package com.lesieann.passvault.data

import org.junit.Assert.assertEquals
import org.junit.Test

class EntryCodecTest {
    @Test
    fun roundTripKeepsCategory() {
        val entries = listOf(
            VaultEntry(id = "1", title = "信箱", username = "me@example.com", password = "p@ss", category = "工作", updatedAt = 42),
            VaultEntry(id = "2", title = "Wi-Fi", password = "secret", updatedAt = 7),
        )
        assertEquals(entries, EntryCodec.decode(EntryCodec.encode(entries)))
    }

    @Test
    fun legacyVaultWithoutCategoryDecodesAsUncategorized() {
        val legacy = """[{"id":"x","title":"舊資料","username":"u","password":"p","url":"","notes":"","updatedAt":1}]"""
        val decoded = EntryCodec.decode(legacy.toByteArray())
        assertEquals(1, decoded.size)
        assertEquals("", decoded[0].category)
        assertEquals("舊資料", decoded[0].title)
    }
}
