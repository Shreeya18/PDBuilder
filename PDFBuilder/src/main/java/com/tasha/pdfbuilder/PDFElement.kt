package com.tasha.pdbuilder

import android.graphics.Paint
import android.graphics.Typeface

/**
 * Represents a single element to be rendered in the PDF.
 * Each element can measure its own height and draw itself.
 */
sealed class PDFElement {

    /** Measures how tall this element will be on the page (in points). */
    abstract fun measure(availableWidth: Float, painter: PDFPainter): Float

    /** Renders this element starting at (x, y) on the current canvas. */
    abstract fun draw(x: Float, y: Float, availableWidth: Float, painter: PDFPainter)

    /** Whether this element must stay on the same page as the next element. */
    open val keepWithNext: Boolean = false

    // ─────────────────────────────────────────────────────────────
    // CONCRETE ELEMENTS
    // ─────────────────────────────────────────────────────────────

    data class Title(
        val text: String,
        val level: Int = 1,               // 1–4
        override val keepWithNext: Boolean = true
    ) : PDFElement() {

        private fun paint() = Paint().apply {
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
            textSize = when (level) { 1 -> 24f; 2 -> 20f; 3 -> 16f; else -> 14f }
        }

        override fun measure(availableWidth: Float, painter: PDFPainter): Float {
            val p = paint()
            val lines = painter.breakTextIntoLines(text, p, availableWidth)
            val lineHeight = p.fontSpacing
            return lines.size * lineHeight + lineHeight * 0.4f  // + bottom margin
        }

        override fun draw(x: Float, y: Float, availableWidth: Float, painter: PDFPainter) {
            val p = paint()
            val lines = painter.breakTextIntoLines(text, p, availableWidth)
            var curY = y + p.textSize
            lines.forEach { line ->
                painter.canvas.drawText(line, x, curY, p)
                curY += p.fontSpacing
            }
        }
    }

    data class Body(
        val text: String,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val fontSize: Float = 11f,
        val lineSpacing: Float = 1.4f
    ) : PDFElement() {

        private fun paint() = Paint().apply {
            isAntiAlias = true
            typeface = when {
                bold && italic -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
                bold           -> Typeface.DEFAULT_BOLD
                italic         -> Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                else           -> Typeface.DEFAULT
            }
            textSize = fontSize
        }

        override fun measure(availableWidth: Float, painter: PDFPainter): Float {
            val p = paint()
            val lines = painter.breakTextIntoLines(text, p, availableWidth)
            return lines.size * p.fontSpacing * lineSpacing + p.fontSpacing * 0.3f
        }

        override fun draw(x: Float, y: Float, availableWidth: Float, painter: PDFPainter) {
            val p = paint()
            val lines = painter.breakTextIntoLines(text, p, availableWidth)
            var curY = y + p.textSize
            lines.forEach { line ->
                painter.canvas.drawText(line, x, curY, p)
                curY += p.fontSpacing * lineSpacing
            }
        }
    }

    data class Spacer(val heightPt: Float = 16f) : PDFElement() {
        override fun measure(availableWidth: Float, painter: PDFPainter) = heightPt
        override fun draw(x: Float, y: Float, availableWidth: Float, painter: PDFPainter) { /* no-op */ }
    }

    data class Divider(
        val thickness: Float = 1f,
        val color: Int = 0xFF888888.toInt(),
        val margin: Float = 8f
    ) : PDFElement() {
        override fun measure(availableWidth: Float, painter: PDFPainter) = thickness + margin * 2

        override fun draw(x: Float, y: Float, availableWidth: Float, painter: PDFPainter) {
            val p = Paint().apply { color = this@Divider.color; strokeWidth = thickness; isAntiAlias = true }
            val lineY = y + margin + thickness / 2
            painter.canvas.drawLine(x, lineY, x + availableWidth, lineY, p)
        }
    }

    data class TableRow(
        val cells: List<String>,
        val isHeader: Boolean = false,
        val columnWeights: List<Float>? = null  // null = equal columns
    ) : PDFElement() {

        private fun paint(isHeader: Boolean) = Paint().apply {
            isAntiAlias = true
            typeface = if (isHeader) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            textSize = if (isHeader) 11f else 10f
        }

        private fun columnWidths(availableWidth: Float): List<Float> {
            val weights = columnWeights ?: List(cells.size) { 1f }
            val total = weights.sum()
            return weights.map { (it / total) * availableWidth }
        }

        override fun measure(availableWidth: Float, painter: PDFPainter): Float {
            val p = paint(isHeader)
            val widths = columnWidths(availableWidth)
            val padding = 8f
            val maxLines = cells.indices.maxOf { i ->
                painter.breakTextIntoLines(cells[i], p, widths[i] - padding * 2).size
            }
            return maxLines * p.fontSpacing + padding * 2
        }

        override fun draw(x: Float, y: Float, availableWidth: Float, painter: PDFPainter) {
            val p = paint(isHeader)
            val widths = columnWidths(availableWidth)
            val padding = 8f
            val rowHeight = measure(availableWidth, painter)

            // Background for header
            if (isHeader) {
                val bgPaint = Paint().apply { color = 0xFFEEEEEE.toInt() }
                painter.canvas.drawRect(x, y, x + availableWidth, y + rowHeight, bgPaint)
            }

            // Row border
            val borderPaint = Paint().apply { color = 0xFFCCCCCC.toInt(); style = Paint.Style.STROKE; strokeWidth = 0.5f }
            painter.canvas.drawRect(x, y, x + availableWidth, y + rowHeight, borderPaint)

            // Cells
            var cellX = x
            cells.forEachIndexed { i, text ->
                val cellW = widths[i]
                // Vertical separator
                if (i > 0) painter.canvas.drawLine(cellX, y, cellX, y + rowHeight, borderPaint)

                val lines = painter.breakTextIntoLines(text, p, cellW - padding * 2)
                var textY = y + padding + p.textSize
                lines.forEach { line ->
                    painter.canvas.drawText(line, cellX + padding, textY, p)
                    textY += p.fontSpacing
                }
                cellX += cellW
            }
        }
    }

    data class BulletList(
        val items: List<String>,
        val bullet: String = "•",
        val indent: Float = 20f,
        val fontSize: Float = 11f
    ) : PDFElement() {

        private fun paint() = Paint().apply { isAntiAlias = true; textSize = fontSize }

        override fun measure(availableWidth: Float, painter: PDFPainter): Float {
            val p = paint()
            var total = 0f
            items.forEach { item ->
                val lines = painter.breakTextIntoLines(item, p, availableWidth - indent)
                total += lines.size * p.fontSpacing * 1.3f
            }
            return total + p.fontSpacing * 0.3f
        }

        override fun draw(x: Float, y: Float, availableWidth: Float, painter: PDFPainter) {
            val p = paint()
            var curY = y + p.textSize
            items.forEach { item ->
                painter.canvas.drawText(bullet, x, curY, p)
                val lines = painter.breakTextIntoLines(item, p, availableWidth - indent)
                lines.forEachIndexed { idx, line ->
                    painter.canvas.drawText(line, x + indent, curY + idx * p.fontSpacing * 1.3f, p)
                }
                curY += lines.size * p.fontSpacing * 1.3f
            }
        }
    }

    /** Forces a page break regardless of remaining space. */
    object PageBreak : PDFElement() {
        override fun measure(availableWidth: Float, painter: PDFPainter) = Float.MAX_VALUE
        override fun draw(x: Float, y: Float, availableWidth: Float, painter: PDFPainter) { }
    }

    data class Image(
        val bitmap: android.graphics.Bitmap,
        val maxHeightPt: Float = 300f,
        val caption: String? = null
    ) : PDFElement() {
        override fun measure(availableWidth: Float, painter: PDFPainter): Float {
            val ratio = availableWidth / bitmap.width
            val scaledH = (bitmap.height * ratio).coerceAtMost(maxHeightPt)
            val captionH = if (caption != null) 20f else 0f
            return scaledH + captionH + 8f
        }

        override fun draw(x: Float, y: Float, availableWidth: Float, painter: PDFPainter) {
            val ratio = availableWidth / bitmap.width
            val scaledH = (bitmap.height * ratio).coerceAtMost(maxHeightPt)
            val dst = android.graphics.RectF(x, y, x + availableWidth, y + scaledH)
            painter.canvas.drawBitmap(bitmap, null, dst, null)
            if (caption != null) {
                val p = Paint().apply {
                    isAntiAlias = true; textSize = 9f
                    color = 0xFF555555.toInt()
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                }
                painter.canvas.drawText(caption, x, y + scaledH + 14f, p)
            }
        }
    }
}