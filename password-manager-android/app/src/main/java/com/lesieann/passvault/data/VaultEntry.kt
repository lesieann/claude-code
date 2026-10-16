package com.lesieann.passvault.data

import java.util.UUID

data class VaultEntry(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val notes: String = "",
    /** 空字串代表「未分類」。 */
    val category: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
)
