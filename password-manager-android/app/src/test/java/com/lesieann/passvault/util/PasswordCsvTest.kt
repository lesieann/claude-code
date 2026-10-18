package com.lesieann.passvault.util

import com.lesieann.passvault.data.VaultEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class PasswordCsvTest {
    // 與 Google 密碼管理工具匯出的格式相同（含 BOM、CRLF、Android App 的網址）
    private val googleExport = "﻿name,url,username,password,note\r\n" +
        "accounts.example.com,https://accounts.example.com/signin,mei@example.com,\"p,a\"\"ss\",\r\n" +
        "bank.example.com,https://bank.example.com/,mei0812,S3cret!,\"第一行\n第二行\"\r\n" +
        ",https://www.shop.example.com/login,buyer,hunter2,\r\n" +
        "Example App,android://AbCd==@com.example.app/,app-user,apppass,\r\n" +
        "Empty site,https://empty.example.com,,,\r\n" +
        ",,,,\r\n"

    @Test
    fun parsesQuotesCommasAndNewlines() {
        val rows = CsvParser.parse("a,\"b,c\",\"d\"\"e\"\r\n\"multi\nline\",x,\n")
        assertEquals(listOf(listOf("a", "b,c", "d\"e"), listOf("multi\nline", "x", "")), rows)
    }

    @Test
    fun importsGoogleExport() {
        val result = PasswordCsvImporter.parse(googleExport)
        assertEquals(4, result.entries.size)
        assertEquals(1, result.skippedRows)

        val first = result.entries[0]
        assertEquals("accounts.example.com", first.title)
        assertEquals("mei@example.com", first.username)
        assertEquals("p,a\"ss", first.password)
        assertEquals("https://accounts.example.com/signin", first.url)

        assertEquals("第一行\n第二行", result.entries[1].notes)
        // 沒有名稱時用網址的主機名稱當標題
        assertEquals("shop.example.com", result.entries[2].title)
        assertEquals("Example App", result.entries[3].title)
        // 匯入的項目預設未分類
        assertEquals("", first.category)
    }

    @Test
    fun columnOrderDoesNotMatter() {
        val csv = "password,username,url,name\nsecret,me,https://site.example.com,Site\n"
        val e = PasswordCsvImporter.parse(csv).entries.single()
        assertEquals("Site", e.title)
        assertEquals("me", e.username)
        assertEquals("secret", e.password)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsFileWithoutPasswordColumn() {
        PasswordCsvImporter.parse("name,phone\nMei,0912\n")
    }

    @Test
    fun skipsEntriesAlreadyInVaultAndDuplicatesInFile() {
        val existing = listOf(VaultEntry(title = "Bank", url = "https://bank.example.com/", username = "mei0812", password = "S3cret!"))
        val incoming = PasswordCsvImporter.parse(googleExport).entries
        val doubled = incoming + incoming.first().copy(id = "dup")
        val fresh = PasswordCsvImporter.withoutDuplicates(doubled, existing)
        assertEquals(listOf("accounts.example.com", "shop.example.com", "Example App"), fresh.map { it.title })
    }
}
