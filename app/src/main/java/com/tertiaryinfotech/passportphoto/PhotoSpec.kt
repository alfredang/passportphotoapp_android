package com.tertiaryinfotech.passportphoto

import kotlin.math.roundToInt

/**
 * A passport / ID photo specification for a specific country.
 *
 * Output pixel size is computed from the physical size at 300 DPI, which is the
 * standard print resolution accepted by passport / visa authorities.
 */
data class PhotoSpec(
    val id: String,            // ISO-ish country code
    val country: String,       // "Singapore"
    val flag: String,          // emoji flag
    val widthMM: Double,
    val heightMM: Double,
    /** Target fraction of the photo height that the head (chin → crown) should fill. */
    val headHeightFraction: Double,
    /** Vertical position of the eye line measured from the bottom, as a fraction of height. */
    val eyeLineFromBottom: Double,
) {
    val displayName: String get() = "$flag  $country"

    /** Human-readable physical dimensions, e.g. "35 × 45 mm". */
    val detail: String
        get() {
            val w = if (widthMM % 1.0 == 0.0) widthMM.toInt().toString() else widthMM.toString()
            val h = if (heightMM % 1.0 == 0.0) heightMM.toInt().toString() else heightMM.toString()
            return "$w × $h mm"
        }

    val pixelWidth: Int get() = ((widthMM / MM_PER_INCH) * DPI).roundToInt()
    val pixelHeight: Int get() = ((heightMM / MM_PER_INCH) * DPI).roundToInt()
    val aspectRatio: Double get() = widthMM / heightMM

    companion object {
        const val DPI = 300.0
        private const val MM_PER_INCH = 25.4

        val SINGAPORE = PhotoSpec("SG", "Singapore", "🇸🇬", 35.0, 45.0, 0.72, 0.55)
        val UNITED_STATES = PhotoSpec("US", "United States", "🇺🇸", 51.0, 51.0, 0.60, 0.58)
        val UNITED_KINGDOM = PhotoSpec("GB", "United Kingdom", "🇬🇧", 35.0, 45.0, 0.72, 0.55)
        val SCHENGEN = PhotoSpec("EU", "EU / Schengen", "🇪🇺", 35.0, 45.0, 0.78, 0.52)
        val AUSTRALIA = PhotoSpec("AU", "Australia", "🇦🇺", 35.0, 45.0, 0.72, 0.55)
        val CANADA = PhotoSpec("CA", "Canada", "🇨🇦", 50.0, 70.0, 0.45, 0.55)
        val INDIA = PhotoSpec("IN", "India", "🇮🇳", 35.0, 45.0, 0.72, 0.55)
        val CHINA = PhotoSpec("CN", "China", "🇨🇳", 33.0, 48.0, 0.70, 0.55)
        val JAPAN = PhotoSpec("JP", "Japan", "🇯🇵", 35.0, 45.0, 0.72, 0.55)
        val MALAYSIA = PhotoSpec("MY", "Malaysia", "🇲🇾", 35.0, 50.0, 0.70, 0.55)

        /** All supported countries, Singapore first (default). */
        val ALL = listOf(
            SINGAPORE, UNITED_STATES, UNITED_KINGDOM, SCHENGEN, AUSTRALIA,
            CANADA, INDIA, CHINA, JAPAN, MALAYSIA,
        )

        val DEFAULT = SINGAPORE
    }
}
