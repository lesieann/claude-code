package com.lesieann.passvault.data

import android.util.Base64
import com.lesieann.passvault.crypto.VaultCrypto
import org.json.JSONArray
import org.json.JSONObject

/** 磁碟上的保險箱檔案：只有 KDF 參數為明文，所有項目都在 [data] 密文裡。 */
data class VaultFile(
    val iterations: Int,
    val salt: ByteArray,
    val iv: ByteArray,
    val data: ByteArray,
    val biometric: BiometricWrap? = null,
) {
    /** 以 Keystore 生物辨識金鑰包裝過的保險箱金鑰（僅限本機有效）。 */
    class BiometricWrap(val iv: ByteArray, val wrappedKey: ByteArray)

    val sealed get() = VaultCrypto.Sealed(iv, data)

    fun toJson(): String = JSONObject().apply {
        put("version", VERSION)
        put("kdf", VaultCrypto.KDF_ALGORITHM)
        put("iterations", iterations)
        put("salt", salt.b64())
        put("iv", iv.b64())
        put("data", data.b64())
        biometric?.let {
            put("biometric", JSONObject().put("iv", it.iv.b64()).put("key", it.wrappedKey.b64()))
        }
    }.toString()

    companion object {
        const val VERSION = 1

        fun fromJson(text: String): VaultFile {
            val o = JSONObject(text)
            require(o.getInt("version") == VERSION) { "不支援的保險箱版本" }
            require(o.getString("kdf") == VaultCrypto.KDF_ALGORITHM) { "不支援的金鑰衍生演算法" }
            val bio = o.optJSONObject("biometric")?.let {
                BiometricWrap(it.getString("iv").unb64(), it.getString("key").unb64())
            }
            return VaultFile(
                iterations = o.getInt("iterations"),
                salt = o.getString("salt").unb64(),
                iv = o.getString("iv").unb64(),
                data = o.getString("data").unb64(),
                biometric = bio,
            )
        }

        private fun ByteArray.b64() = Base64.encodeToString(this, Base64.NO_WRAP)
        private fun String.unb64() = Base64.decode(this, Base64.NO_WRAP)
    }
}

object EntryCodec {
    fun encode(entries: List<VaultEntry>): ByteArray = JSONArray().apply {
        entries.forEach { e ->
            put(
                JSONObject()
                    .put("id", e.id)
                    .put("title", e.title)
                    .put("username", e.username)
                    .put("password", e.password)
                    .put("url", e.url)
                    .put("notes", e.notes)
                    .put("category", e.category)
                    .put("updatedAt", e.updatedAt)
            )
        }
    }.toString().toByteArray(Charsets.UTF_8)

    fun decode(bytes: ByteArray): List<VaultEntry> {
        val arr = JSONArray(String(bytes, Charsets.UTF_8))
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            VaultEntry(
                id = o.getString("id"),
                title = o.optString("title"),
                username = o.optString("username"),
                password = o.optString("password"),
                url = o.optString("url"),
                notes = o.optString("notes"),
                // 舊版保險箱沒有這個欄位，讀進來就是未分類
                category = o.optString("category").trim(),
                updatedAt = o.optLong("updatedAt"),
            )
        }
    }
}
