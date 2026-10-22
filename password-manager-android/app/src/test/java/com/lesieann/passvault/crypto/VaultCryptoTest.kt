package com.lesieann.passvault.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import javax.crypto.AEADBadTagException

class VaultCryptoTest {
    private val salt = VaultCrypto.randomBytes(VaultCrypto.SALT_BYTES)
    private val iterations = 1_000 // 測試用，正式環境使用 DEFAULT_ITERATIONS

    @Test
    fun roundTrip() {
        val key = VaultCrypto.deriveKey("correct horse".toCharArray(), salt, iterations)
        val plaintext = "秘密資料 secret".toByteArray()
        val sealed = VaultCrypto.encrypt(key, plaintext)
        assertArrayEquals(plaintext, VaultCrypto.decrypt(key, sealed))
    }

    @Test
    fun derivationIsDeterministic() {
        val a = VaultCrypto.deriveKey("pw".toCharArray(), salt, iterations)
        val b = VaultCrypto.deriveKey("pw".toCharArray(), salt, iterations)
        assertEquals(VaultCrypto.KEY_BYTES, a.size)
        assertArrayEquals(a, b)
    }

    @Test
    fun freshIvEveryEncryption() {
        val key = VaultCrypto.deriveKey("pw".toCharArray(), salt, iterations)
        val a = VaultCrypto.encrypt(key, byteArrayOf(1, 2, 3))
        val b = VaultCrypto.encrypt(key, byteArrayOf(1, 2, 3))
        assertFalse(a.iv.contentEquals(b.iv))
    }

    @Test(expected = AEADBadTagException::class)
    fun wrongPasswordFails() {
        val key = VaultCrypto.deriveKey("right".toCharArray(), salt, iterations)
        val wrong = VaultCrypto.deriveKey("wrong".toCharArray(), salt, iterations)
        VaultCrypto.decrypt(wrong, VaultCrypto.encrypt(key, "data".toByteArray()))
    }

    @Test(expected = AEADBadTagException::class)
    fun tamperedCiphertextFails() {
        val key = VaultCrypto.deriveKey("pw".toCharArray(), salt, iterations)
        val sealed = VaultCrypto.encrypt(key, "data".toByteArray())
        sealed.ciphertext[0] = (sealed.ciphertext[0].toInt() xor 1).toByte()
        VaultCrypto.decrypt(key, sealed)
    }
}
