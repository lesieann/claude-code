package com.lesieann.passvault.util

import com.lesieann.passvault.data.VaultEntry
import java.net.URI

/** RFC 4180 CSV：支援引號、欄位內的逗號／換行、"" 跳脫、CRLF 與 UTF-8 BOM。 */
object CsvParser {
    fun parse(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = if (text.startsWith('﻿')) 1 else 0
        while (i < text.length) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') { field.append('"'); i++ } else inQuotes = false
                } else {
                    field.append(c)
                }
            } else when (c) {
                '"' -> inQuotes = true
                ',' -> { row.add(field.toString()); field.clear() }
                '\r' -> {}
                '\n' -> { row.add(field.toString()); field.clear(); rows.add(row); row = mutableListOf() }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) { row.add(field.toString()); rows.add(row) }
        // 去掉空白行
        return rows.filterNot { r -> r.all { it.isBlank() } }
    }
}

/**
 * 匯入 Google 密碼管理工具／Chrome 匯出的 CSV（name,url,username,password,note）。
 * 依標題列對應欄位，所以欄位順序不同或多了欄位也能讀；也接受常見的別名。
 */
object PasswordCsvImporter {
    class Result(val entries: List<VaultEntry>, val skippedRows: Int)

    private val TITLE = setOf("name", "title")
    private val URL = setOf("url", "website", "login_uri", "uri")
    private val USERNAME = setOf("username", "login_username", "login", "email", "user")
    private val PASSWORD = setOf("password", "login_password")
    private val NOTE = setOf("note", "notes", "comment", "extra")

    /** 檔案格式不對（沒有標題列或沒有 password 欄）時丟出 [IllegalArgumentException]。 */
    fun parse(text: String): Result {
        val rows = CsvParser.parse(text)
        require(rows.isNotEmpty()) { "檔案是空的" }
        val header = rows.first().map { it.trim().lowercase() }
        fun col(names: Set<String>) = header.indexOfFirst { it in names }
        val iPassword = col(PASSWORD)
        require(iPassword >= 0) { "找不到 password 欄位，這不是密碼匯出檔" }
        val iTitle = col(TITLE)
        val iUrl = col(URL)
        val iUser = col(USERNAME)
        val iNote = col(NOTE)

        var skipped = 0
        val entries = rows.drop(1).mapNotNull { r ->
            fun get(i: Int) = if (i >= 0 && i < r.size) r[i] else ""
            val password = get(iPassword)
            val url = get(iUrl).trim()
            val username = get(iUser).trim()
            if (password.isEmpty() && username.isEmpty()) { skipped++; return@mapNotNull null }
            val title = get(iTitle).trim().ifEmpty { hostOf(url) }.ifEmpty { username }.ifEmpty { "未命名" }
            VaultEntry(title = title, username = username, password = password, url = url, notes = get(iNote))
        }
        return Result(entries, skipped)
    }

    /** 已經存在（網址、帳號、密碼都相同）的項目不重複匯入；檔案內重複的也只留一筆。 */
    fun withoutDuplicates(incoming: List<VaultEntry>, existing: List<VaultEntry>): List<VaultEntry> {
        val seen = existing.mapTo(HashSet()) { key(it) }
        return incoming.filter { seen.add(key(it)) }
    }

    private fun key(e: VaultEntry) = Triple(e.url.trim().lowercase(), e.username.trim(), e.password)

    fun hostOf(url: String): String = runCatching {
        val withScheme = if ("://" in url) url else "https://$url"
        URI(withScheme).host.orEmpty().removePrefix("www.")
    }.getOrDefault("")
}
