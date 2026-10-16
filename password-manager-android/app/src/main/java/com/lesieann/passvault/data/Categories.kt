package com.lesieann.passvault.data

object Categories {
    /** 編輯時預設提供的分類；使用者也可以輸入自己的分類名稱。 */
    val PRESETS = listOf("社群", "金融", "工作", "購物", "娛樂", "遊戲")

    const val UNCATEGORIZED_LABEL = "未分類"

    /** 目前有項目使用中的分類：預設分類依固定順序在前，自訂分類依名稱排序在後。 */
    fun inUse(entries: List<VaultEntry>): List<String> =
        sort(entries.map { it.category }.filter { it.isNotBlank() }.distinct())

    /** 編輯畫面的建議清單：預設分類加上已使用過的自訂分類。 */
    fun suggestions(entries: List<VaultEntry>): List<String> =
        sort((PRESETS + entries.map { it.category }).filter { it.isNotBlank() }.distinct())

    fun sort(names: List<String>): List<String> {
        val (preset, custom) = names.partition { it in PRESETS }
        return preset.sortedBy { PRESETS.indexOf(it) } + custom.sortedBy { it.lowercase() }
    }
}
