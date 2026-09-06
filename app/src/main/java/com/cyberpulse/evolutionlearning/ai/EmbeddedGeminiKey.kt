package com.cyberpulse.evolutionlearning.ai

/**
 * Embedded Gemini API credential for the offline APK build.
 *
 * This is intentionally obfuscated rather than stored as a readable string.
 * Obfuscation only prevents casual discovery; a determined person can still
 * recover any secret that is shipped inside an APK.
 */
internal object EmbeddedGeminiKey {
    private const val MASK = 0x5A

    // Original bytes XORed with MASK and stored in reverse order.
    private val encoded = intArrayOf(
        45, 10, 57, 28, 29, 28, 15, 109, 19, 47, 18, 30, 9, 15, 107, 30, 54, 60,
        49, 43, 2, 19, 31, 5, 29, 41, 41, 106, 10, 23, 16, 63, 13, 106, 99, 46,
        119, 107, 46, 53, 106, 17, 57, 19, 108, 20, 8, 98, 56, 27, 116, 11, 27
    )

    fun value(): String {
        val decoded = ByteArray(encoded.size)
        for (i in encoded.indices) {
            decoded[i] = (encoded[encoded.lastIndex - i] xor MASK).toByte()
        }
        return decoded.toString(Charsets.UTF_8)
    }
}
