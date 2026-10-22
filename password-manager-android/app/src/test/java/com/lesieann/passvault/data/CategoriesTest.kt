package com.lesieann.passvault.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoriesTest {
    private fun entry(category: String) = VaultEntry(title = "t", category = category)

    @Test
    fun inUseListsPresetsInFixedOrderThenCustomSorted() {
        val entries = listOf(entry("學校"), entry("金融"), entry(""), entry("社群"), entry("Bank"), entry("金融"))
        assertEquals(listOf("社群", "金融", "Bank", "學校"), Categories.inUse(entries))
    }

    @Test
    fun inUseIgnoresUncategorized() {
        assertTrue(Categories.inUse(listOf(entry(""), entry(" "))).isEmpty())
    }

    @Test
    fun suggestionsIncludeAllPresetsPlusCustom() {
        val suggestions = Categories.suggestions(listOf(entry("學校")))
        assertEquals(Categories.PRESETS + "學校", suggestions)
    }
}
