package com.tasha.pdbuilder

import android.graphics.Canvas
import android.graphics.Paint

/**
 * Thin wrapper around Android Canvas that provides utility methods
 * used by PDFElement implementations. Works at FIXED point coordinates.
 */
class PDFPainter(val canvas: Canvas) {

    /**
     * Breaks [text] into lines that fit within [maxWidth] using the given [paint].
     * Respects existing newlines in the source text.
     */
    fun breakTextIntoLines(text: String, paint: Paint, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        text.split("\n").forEach { paragraph ->
            val words = paragraph.split(" ")
            var line = StringBuilder()
            words.forEach { word ->
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    line = StringBuilder(candidate)
                } else {
                    if (line.isNotEmpty()) result.add(line.toString())
                    // Handle words wider than the column
                    line = if (paint.measureText(word) > maxWidth) {
                        result.add(word); StringBuilder()
                    } else {
                        StringBuilder(word)
                    }
                }
            }
            if (line.isNotEmpty()) result.add(line.toString())
        }
        return result.ifEmpty { listOf("") }
    }
}
