package com.lesieann.passvault.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * 保險箱加密：主密碼經 PBKDF2-HMAC-SHA256 衍生出 256-bit 金鑰，
 * 資料以 AES-256-GCM 加密（GCM 驗證標籤同時用來確認主密碼是否正確）。
 */
object VaultCrypto {
    const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
    const val DEFAULT_ITERATIONS = 210_000
    const val KEY_BYTES = 32
    const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val GCM_TAG_BITS = 128
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    /** 綁定到每份密文的附加驗證資料，避免密文被拿去別的情境使用。 */
    val VAULT_AAD = "passvault:v1".toByteArray()

    private val random = SecureRandom()

    class Sealed(val iv: ByteArray, val ciphertext: ByteArray)

    fun randomBytes(size: Int): ByteArray = ByteArray(size).also(random::nextBytes)

    fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BYTES * 8)
        try {
            return SecretKeyFactory.getInstance(KDF_ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun encrypt(key: ByteArray, plaintext: ByteArray, aad: ByteArray = VAULT_AAD): Sealed {
        val iv = randomBytes(IV_BYTES)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(aad)
        return Sealed(iv, cipher.doFinal(plaintext))
    }

    /** 金鑰錯誤或資料遭竄改時會丟出 [javax.crypto.AEADBadTagException]。 */
    fun decrypt(key: ByteArray, sealed: Sealed, aad: ByteArray = VAULT_AAD): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, sealed.iv))
        cipher.updateAAD(aad)
        return cipher.doFinal(sealed.ciphertext)
    }
}
