package com.cyberpulse.evolutionlearning

/**
 * Sideloadable release build: Firebase remains available without forcing a
 * Play-Store-only App Check provider. Backend App Check enforcement can be
 * enabled later for a store-signed release.
 */
object AppCheckConfig {
    fun install() = Unit
}
