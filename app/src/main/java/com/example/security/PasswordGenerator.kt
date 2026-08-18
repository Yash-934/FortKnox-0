package com.example.security

import java.security.SecureRandom
import kotlin.math.log2
import kotlin.math.roundToInt

/**
 * Military-grade Password Generator & Cryptographic Strength Analyzer.
 */
object PasswordGenerator {

    private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private const val DIGITS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?"
    private const val AMBIGUOUS = "0Ool1I|`~"

    private val secureRandom = SecureRandom()

    enum class StrengthRating(val label: String, val score: Int) {
        VERY_WEAK("Vulnerable", 1),
        WEAK("Weak", 2),
        FAIR("Moderate", 3),
        STRONG("Strong", 4),
        MILITARY_GRADE("Military-Grade", 5)
    }

    data class StrengthAnalysis(
        val rating: StrengthRating,
        val entropyBits: Double,
        val crackTimeEstimate: String,
        val scorePercent: Float,
        val warnings: List<String>
    )

    /**
     * Generates a strong cryptographically random password according to configured rules.
     */
    fun generatePassword(
        length: Int = 20,
        includeUppercase: Boolean = true,
        includeLowercase: Boolean = true,
        includeDigits: Boolean = true,
        includeSymbols: Boolean = true,
        excludeAmbiguous: Boolean = false
    ): String {
        var charPool = StringBuilder()
        val guaranteedChars = mutableListOf<Char>()

        fun addGroup(chars: String) {
            val filtered = if (excludeAmbiguous) {
                chars.filter { it !in AMBIGUOUS }
            } else {
                chars
            }
            if (filtered.isNotEmpty()) {
                charPool.append(filtered)
                guaranteedChars.add(filtered[secureRandom.nextInt(filtered.length)])
            }
        }

        if (includeUppercase) addGroup(UPPERCASE)
        if (includeLowercase) addGroup(LOWERCASE)
        if (includeDigits) addGroup(DIGITS)
        if (includeSymbols) addGroup(SYMBOLS)

        if (charPool.isEmpty()) {
            charPool.append(LOWERCASE + DIGITS)
        }

        val poolStr = charPool.toString()
        val remainingLength = maxOf(0, length - guaranteedChars.size)
        val passwordChars = ArrayList(guaranteedChars)

        for (i in 0 until remainingLength) {
            passwordChars.add(poolStr[secureRandom.nextInt(poolStr.length)])
        }

        // Shuffle securely
        for (i in passwordChars.size - 1 downTo 1) {
            val j = secureRandom.nextInt(i + 1)
            val temp = passwordChars[i]
            passwordChars[i] = passwordChars[j]
            passwordChars[j] = temp
        }

        return passwordChars.joinToString("")
    }

    /**
     * Calculates Shannon entropy and estimates resistance to brute force attacks.
     */
    fun evaluateStrength(password: String): StrengthAnalysis {
        if (password.isEmpty()) {
            return StrengthAnalysis(
                rating = StrengthRating.VERY_WEAK,
                entropyBits = 0.0,
                crackTimeEstimate = "Instant",
                scorePercent = 0.0f,
                warnings = listOf("Password cannot be empty")
            )
        }

        var poolSize = 0
        var hasUpper = false
        var hasLower = false
        var hasDigit = false
        var hasSymbol = false

        for (c in password) {
            when {
                c.isUpperCase() -> hasUpper = true
                c.isLowerCase() -> hasLower = true
                c.isDigit() -> hasDigit = true
                else -> hasSymbol = true
            }
        }

        if (hasUpper) poolSize += 26
        if (hasLower) poolSize += 26
        if (hasDigit) poolSize += 10
        if (hasSymbol) poolSize += 32

        if (poolSize == 0) poolSize = 10

        val entropy = password.length * log2(poolSize.toDouble())
        val warnings = mutableListOf<String>()

        if (password.length < 12) {
            warnings.add("Length is under 12 characters")
        }
        if (!hasUpper || !hasLower) {
            warnings.add("Missing mixed-case characters")
        }
        if (!hasDigit) {
            warnings.add("Missing numeric digits")
        }
        if (!hasSymbol) {
            warnings.add("Missing special symbols")
        }

        val (rating, crackTime) = when {
            entropy < 35 -> StrengthRating.VERY_WEAK to "< 1 second"
            entropy < 55 -> StrengthRating.WEAK to "A few hours"
            entropy < 75 -> StrengthRating.FAIR to "Several months"
            entropy < 95 -> StrengthRating.STRONG to "Centuries"
            else -> StrengthRating.MILITARY_GRADE to "100+ Trillion Years"
        }

        val scorePercent = (entropy / 110.0).coerceIn(0.0, 1.0).toFloat()

        return StrengthAnalysis(
            rating = rating,
            entropyBits = (entropy * 10).roundToInt() / 10.0,
            crackTimeEstimate = crackTime,
            scorePercent = scorePercent,
            warnings = warnings
        )
    }
}
