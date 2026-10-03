package com.lesieann.passvault.util

import java.security.SecureRandom
import kotlin.math.log2

object PasswordGenerator {
    private const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LOWER = "abcdefghijklmnopqrstuvwxyz"
    private const val DIGITS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()-_=+[]{};:,.?/~"
    private const val AMBIGUOUS = "Il1O0o"

    data class Options(
        val length: Int = 20,
        val upper: Boolean = true,
        val lower: Boolean = true,
        val digits: Boolean = true,
        val symbols: Boolean = true,
        val avoidAmbiguous: Boolean = false,
    )

    /** 產生密碼，並保證每一種勾選的字元類別至少出現一次。 */
    fun generate(options: Options, random: SecureRandom = SecureRandom()): String {
        val groups = buildList {
            if (options.upper) add(UPPER)
            if (options.lower) add(LOWER)
            if (options.digits) add(DIGITS)
            if (options.symbols) add(SYMBOLS)
        }.map { group ->
            if (options.avoidAmbiguous) group.filterNot { it in AMBIGUOUS } else group
        }
        require(groups.isNotEmpty()) { "至少要選一種字元類別" }
        require(options.length >= groups.size) { "長度不足以包含所有字元類別" }

        val all = groups.joinToString("")
        val chars = CharArray(options.length)
        groups.forEachIndexed { i, group -> chars[i] = group[random.nextInt(group.length)] }
        for (i in groups.size until chars.size) chars[i] = all[random.nextInt(all.length)]
        // Fisher–Yates 洗牌，讓必含字元不會固定出現在開頭
        for (i in chars.lastIndex downTo 1) {
            val j = random.nextInt(i + 1)
            val t = chars[i]; chars[i] = chars[j]; chars[j] = t
        }
        return String(chars)
    }

    enum class Strength(val label: String) { VERY_WEAK("非常弱"), WEAK("弱"), FAIR("普通"), STRONG("強"), VERY_STRONG("非常強") }

    /** 依字元集大小粗估熵（bits），不考慮字典攻擊。 */
    fun estimateBits(password: String): Double {
        if (password.isEmpty()) return 0.0
        var pool = 0
        if (password.any { it.isLowerCase() }) pool += 26
        if (password.any { it.isUpperCase() }) pool += 26
        if (password.any { it.isDigit() }) pool += 10
        if (password.any { !it.isLetterOrDigit() }) pool += 33
        return password.length * log2(pool.coerceAtLeast(1).toDouble())
    }

    fun strength(password: String): Strength {
        val bits = estimateBits(password)
        return when {
            bits < 28 -> Strength.VERY_WEAK
            bits < 45 -> Strength.WEAK
            bits < 60 -> Strength.FAIR
            bits < 90 -> Strength.STRONG
            else -> Strength.VERY_STRONG
        }
    }
}
