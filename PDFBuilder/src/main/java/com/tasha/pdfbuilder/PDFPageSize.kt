package com.tasha.pdbuilder


/**
 * Standard page sizes in points (1 point = 1/72 inch).
 * These are FIXED regardless of device screen size/density.
 */
enum class PDFPageSize(val widthPt: Float, val heightPt: Float) {
    A4(595.28f, 841.89f),
    LETTER(612f, 792f),
    LEGAL(612f, 1008f),
    A3(841.89f, 1190.55f),
    A5(419.53f, 595.28f);

    val widthPx: Int get() = widthPt.toInt()
    val heightPx: Int get() = heightPt.toInt()
}

data class PDFMargins(
    val top: Float = 56f,      // ~0.78 inch
    val bottom: Float = 56f,
    val left: Float = 72f,     // 1 inch
    val right: Float = 72f
)