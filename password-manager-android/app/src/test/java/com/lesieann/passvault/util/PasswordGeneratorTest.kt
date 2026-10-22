package com.lesieann.passvault.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordGeneratorTest {
    @Test
    fun respectsLengthAndContainsEveryClass() {
        repeat(200) {
            val pw = PasswordGenerator.generate(PasswordGenerator.Options(length = 12))
            assertEquals(12, pw.length)
            assertTrue(pw.any { it.isUpperCase() })
            assertTrue(pw.any { it.isLowerCase() })
            assertTrue(pw.any { it.isDigit() })
            assertTrue(pw.any { !it.isLetterOrDigit() })
        }
    }

    @Test
    fun onlyDigits() {
        val pw = PasswordGenerator.generate(
            PasswordGenerator.Options(length = 30, upper = false, lower = false, symbols = false)
        )
        assertTrue(pw.all { it.isDigit() })
    }

    @Test
    fun avoidsAmbiguousCharacters() {
        repeat(100) {
            val pw = PasswordGenerator.generate(PasswordGenerator.Options(length = 64, avoidAmbiguous = true))
            assertTrue(pw.none { it in "Il1O0o" })
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNoCharacterClasses() {
        PasswordGenerator.generate(PasswordGenerator.Options(upper = false, lower = false, digits = false, symbols = false))
    }

    @Test
    fun strengthOrdering() {
        assertEquals(PasswordGenerator.Strength.VERY_WEAK, PasswordGenerator.strength("abc"))
        assertEquals(PasswordGenerator.Strength.VERY_STRONG, PasswordGenerator.strength("k#9Lm!2qP@x7Zr\$5wT&"))
    }
}
