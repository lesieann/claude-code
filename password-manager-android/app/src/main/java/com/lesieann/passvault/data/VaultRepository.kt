package com.lesieann.passvault.data

import android.content.Context
import android.util.AtomicFile
import java.io.File

class VaultRepository(context: Context) {
    private val file = AtomicFile(File(context.filesDir, "vault.json"))

    fun exists(): Boolean = file.baseFile.exists()

    fun read(): VaultFile = VaultFile.fromJson(String(file.readFully(), Charsets.UTF_8))

    fun readRaw(): ByteArray = file.readFully()

    /** 以原子方式寫入，避免寫到一半當機造成保險箱損毀。 */
    fun write(vault: VaultFile) {
        val out = file.startWrite()
        try {
            out.write(vault.toJson().toByteArray(Charsets.UTF_8))
            file.finishWrite(out)
        } catch (e: Exception) {
            file.failWrite(out)
            throw e
        }
    }
}
