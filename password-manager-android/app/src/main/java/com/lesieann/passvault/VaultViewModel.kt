package com.lesieann.passvault

import android.app.Application
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lesieann.passvault.crypto.BiometricKeyStore
import com.lesieann.passvault.crypto.VaultCrypto
import com.lesieann.passvault.data.EntryCodec
import com.lesieann.passvault.data.VaultEntry
import com.lesieann.passvault.data.VaultFile
import com.lesieann.passvault.data.VaultRepository
import com.lesieann.passvault.util.PasswordCsvImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher

sealed interface Screen {
    data object Setup : Screen
    data object Locked : Screen
    data object List : Screen
    data class Edit(val entryId: String?) : Screen
    data object Settings : Screen
}

class VaultViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = VaultRepository(app)
    private val biometricStore = BiometricKeyStore()

    var screen: Screen by mutableStateOf(if (repo.exists()) Screen.Locked else Screen.Setup)
        private set
    var entries: List<VaultEntry> by mutableStateOf(emptyList())
        private set
    var busy by mutableStateOf(false)
        private set
    var biometricEnabled by mutableStateOf(false)
        private set

    /** 要顯示給使用者的一次性訊息（Snackbar）。 */
    var message: String? by mutableStateOf(null)

    // 只在解鎖期間存在於記憶體中
    private var key: ByteArray? = null
    private var vault: VaultFile? = null
    private var backgroundedAt = 0L

    init {
        refreshBiometricFlag()
    }

    // ---- 建立 / 解鎖 / 上鎖 ----

    fun create(password: String, confirm: String) {
        validateNewPassword(password, confirm)?.let { message = it; return }
        runBusy {
            val (k, v) = withContext(Dispatchers.Default) {
                val salt = VaultCrypto.randomBytes(VaultCrypto.SALT_BYTES)
                val k = VaultCrypto.deriveKey(password.toCharArray(), salt, VaultCrypto.DEFAULT_ITERATIONS)
                k to seal(k, salt, VaultCrypto.DEFAULT_ITERATIONS, emptyList(), biometric = null)
            }
            repo.write(v)
            open(k, v, emptyList())
        }
    }

    fun unlock(password: String) {
        if (password.isEmpty()) return
        runBusy {
            val v = repo.read()
            val (k, list) = withContext(Dispatchers.Default) {
                val k = VaultCrypto.deriveKey(password.toCharArray(), v.salt, v.iterations)
                k to EntryCodec.decode(VaultCrypto.decrypt(k, v.sealed))
            }
            open(k, v, list)
        }
    }

    /** 回傳待生物辨識授權的 Cipher；若生物辨識已失效則回傳 null 並停用。 */
    fun biometricUnlockCipher(): Cipher? {
        val wrap = runCatching { repo.read().biometric }.getOrNull() ?: return null
        val cipher = biometricStore.decryptCipherOrNull(wrap.iv)
        if (cipher == null) {
            runCatching { repo.write(repo.read().copy(biometric = null)) }
            biometricEnabled = false
            message = "生物辨識資料已變更，請用主密碼解鎖後重新啟用"
        }
        return cipher
    }

    fun completeBiometricUnlock(cipher: Cipher) {
        runBusy {
            val v = repo.read()
            val wrap = v.biometric ?: error("尚未啟用生物辨識")
            val k = cipher.doFinal(wrap.wrappedKey)
            val list = withContext(Dispatchers.Default) { EntryCodec.decode(VaultCrypto.decrypt(k, v.sealed)) }
            open(k, v, list)
        }
    }

    fun lock() {
        pendingImport = null
        key?.fill(0)
        key = null
        vault = null
        entries = emptyList()
        screen = Screen.Locked
        refreshBiometricFlag()
    }

    fun onBackground() {
        backgroundedAt = SystemClock.elapsedRealtime()
    }

    /** App 自己開的系統檔案選擇器顯示期間不自動上鎖，回來時呼叫 [endFilePicker]。 */
    private var filePickerOpen = false

    fun beginFilePicker() { filePickerOpen = true }
    fun endFilePicker() { filePickerOpen = false }

    fun onForeground() {
        if (key != null && backgroundedAt != 0L && !filePickerOpen &&
            SystemClock.elapsedRealtime() - backgroundedAt > AUTO_LOCK_MS
        ) lock()
        backgroundedAt = 0L
    }

    // ---- 導覽 ----

    fun openEntry(id: String?) { if (key != null) screen = Screen.Edit(id) }
    fun openSettings() { if (key != null) screen = Screen.Settings }
    fun backToList() { if (key != null) screen = Screen.List }

    // ---- 項目 CRUD ----

    fun entry(id: String): VaultEntry? = entries.firstOrNull { it.id == id }

    fun save(entry: VaultEntry) {
        if (entry.title.isBlank()) { message = "請輸入名稱"; return }
        val updated = entry.copy(category = entry.category.trim(), updatedAt = System.currentTimeMillis())
        val list = entries.filterNot { it.id == entry.id } + updated
        if (persist(list)) screen = Screen.List
    }

    fun delete(id: String) {
        if (persist(entries.filterNot { it.id == id })) {
            screen = Screen.List
            message = "已刪除"
        }
    }

    // ---- 分類 ----

    /** 把某個分類下的所有項目改到新名稱；新名稱已存在時等於合併兩個分類。 */
    fun renameCategory(old: String, new: String) {
        val name = new.trim()
        if (name.isEmpty()) { message = "請輸入分類名稱"; return }
        if (name == old) return
        val list = entries.map { if (it.category == old) it.copy(category = name) else it }
        if (persist(list)) message = "已將「$old」改為「$name」"
    }

    /** 刪除分類只會把項目移到「未分類」，不會刪除項目本身。 */
    fun removeCategory(name: String) {
        val list = entries.map { if (it.category == name) it.copy(category = "") else it }
        if (persist(list)) message = "已刪除分類「$name」，項目已移到未分類"
    }

    // ---- 設定 ----

    fun biometricEnrollCipher(): Cipher = biometricStore.newEncryptCipher()

    fun completeBiometricEnroll(cipher: Cipher) {
        val k = key ?: return
        val v = vault ?: return
        runCatching {
            val wrapped = cipher.doFinal(k)
            val nv = v.copy(biometric = VaultFile.BiometricWrap(cipher.iv, wrapped))
            repo.write(nv)
            vault = nv
            biometricEnabled = true
            message = "已啟用生物辨識解鎖"
        }.onFailure { message = "啟用失敗：${it.message}" }
    }

    fun disableBiometric() {
        val v = vault ?: return
        val nv = v.copy(biometric = null)
        repo.write(nv)
        vault = nv
        biometricStore.deleteKey()
        biometricEnabled = false
    }

    fun changeMasterPassword(current: String, new: String, confirm: String) {
        val oldKey = key ?: return
        val v = vault ?: return
        validateNewPassword(new, confirm)?.let { message = it; return }
        runBusy {
            val (newKey, nv) = withContext(Dispatchers.Default) {
                val check = VaultCrypto.deriveKey(current.toCharArray(), v.salt, v.iterations)
                if (!MessageDigest.isEqual(check, oldKey)) throw AEADBadTagException()
                val salt = VaultCrypto.randomBytes(VaultCrypto.SALT_BYTES)
                val nk = VaultCrypto.deriveKey(new.toCharArray(), salt, VaultCrypto.DEFAULT_ITERATIONS)
                // 舊的生物辨識包裝的是舊金鑰，變更後必須重新啟用
                nk to seal(nk, salt, VaultCrypto.DEFAULT_ITERATIONS, entries, biometric = null)
            }
            repo.write(nv)
            biometricStore.deleteKey()
            biometricEnabled = false
            oldKey.fill(0)
            key = newKey
            vault = nv
            message = "主密碼已變更"
        }
    }

    // ---- 備份 ----

    /** 匯出的檔案仍以主密碼加密；不含僅限本機的生物辨識資料。 */
    fun exportBackup(): ByteArray? = vault?.copy(biometric = null)?.toJson()?.toByteArray(Charsets.UTF_8)

    /** 以備份檔取代目前的保險箱（需要備份當時的主密碼）。 */
    fun importBackup(bytes: ByteArray, password: String) {
        runBusy {
            val (k, v, list) = withContext(Dispatchers.Default) {
                val v = VaultFile.fromJson(String(bytes, Charsets.UTF_8)).copy(biometric = null)
                val k = VaultCrypto.deriveKey(password.toCharArray(), v.salt, v.iterations)
                Triple(k, v, EntryCodec.decode(VaultCrypto.decrypt(k, v.sealed)))
            }
            repo.write(v)
            biometricStore.deleteKey()
            biometricEnabled = false
            key?.fill(0)
            open(k, v, list)
            message = "已從備份還原 ${list.size} 筆資料"
        }
    }

    // ---- 從 Google 密碼管理工具匯入 ----

    /** 等待使用者確認的匯入內容。 */
    class PendingImport(val entries: List<VaultEntry>, val duplicates: Int, val skippedRows: Int)

    var pendingImport: PendingImport? by mutableStateOf(null)
        private set

    fun prepareCsvImport(bytes: ByteArray) {
        if (key == null) { message = "保險箱已上鎖，請解鎖後再匯入"; return }
        runCatching {
            val result = PasswordCsvImporter.parse(String(bytes, Charsets.UTF_8))
            val fresh = PasswordCsvImporter.withoutDuplicates(result.entries, entries)
            if (fresh.isEmpty()) {
                message = if (result.entries.isEmpty()) "檔案裡沒有可匯入的密碼" else "全部 ${result.entries.size} 筆都已經在保險箱裡了"
            } else {
                pendingImport = PendingImport(fresh, result.entries.size - fresh.size, result.skippedRows)
            }
        }.onFailure { message = "無法讀取：${it.message}" }
    }

    fun confirmCsvImport(category: String) {
        val pending = pendingImport ?: return
        pendingImport = null
        val now = System.currentTimeMillis()
        val imported = pending.entries.map { it.copy(category = category.trim(), updatedAt = now) }
        if (persist(entries + imported)) message = "已匯入 ${imported.size} 筆密碼。記得刪除手機裡的 CSV 檔，它沒有加密。"
    }

    fun cancelCsvImport() {
        pendingImport = null
    }

    // ---- 內部 ----

    private fun open(k: ByteArray, v: VaultFile, list: List<VaultEntry>) {
        key = k
        vault = v
        entries = list
        biometricEnabled = v.biometric != null
        screen = Screen.List
    }

    private fun persist(list: List<VaultEntry>): Boolean {
        val k = key ?: return false
        val v = vault ?: return false
        return runCatching {
            val sealed = VaultCrypto.encrypt(k, EntryCodec.encode(list))
            val nv = v.copy(iv = sealed.iv, data = sealed.ciphertext)
            repo.write(nv)
            vault = nv
            entries = list
        }.onFailure { message = "儲存失敗：${it.message}" }.isSuccess
    }

    private fun seal(
        k: ByteArray,
        salt: ByteArray,
        iterations: Int,
        list: List<VaultEntry>,
        biometric: VaultFile.BiometricWrap?,
    ): VaultFile {
        val sealed = VaultCrypto.encrypt(k, EntryCodec.encode(list))
        return VaultFile(iterations, salt, sealed.iv, sealed.ciphertext, biometric)
    }

    private fun refreshBiometricFlag() {
        biometricEnabled = runCatching { repo.exists() && repo.read().biometric != null }.getOrDefault(false)
    }

    private fun validateNewPassword(password: String, confirm: String): String? = when {
        password.length < MIN_MASTER_LENGTH -> "主密碼至少需要 $MIN_MASTER_LENGTH 個字元"
        password != confirm -> "兩次輸入的密碼不一致"
        else -> null
    }

    private fun runBusy(block: suspend () -> Unit) {
        if (busy) return
        viewModelScope.launch {
            busy = true
            try {
                block()
            } catch (e: AEADBadTagException) {
                message = "密碼錯誤"
            } catch (e: Exception) {
                message = "發生錯誤：${e.message ?: e.javaClass.simpleName}"
            } finally {
                busy = false
            }
        }
    }

    override fun onCleared() {
        key?.fill(0)
    }

    companion object {
        const val AUTO_LOCK_MS = 30_000L
        const val MIN_MASTER_LENGTH = 8
    }
}
