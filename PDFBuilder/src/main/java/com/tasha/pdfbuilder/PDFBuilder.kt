package com.tasha.pdbuilder

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.OutputStream

/**
 * UniversalPDFBuilder
 *
 * Cross-device, consistent multipage PDF generation for Android.
 *
 * Key design decisions:
 *  • Page size is FIXED in typographic points (not pixels/dp), so the
 *    physical layout is identical on phones, tablets, and emulators.
 *  • Content is measured before drawing; a new page is opened
 *    automatically whenever an element would overflow the current page.
 *  • The API is a simple builder/DSL — add elements in order, then call build().
 *
 * Usage:
 * ```kotlin
 * val pdf = UniversalPDFBuilder(pageSize = PDFPageSize.A4)
 *     .title("Annual Report 2024")
 *     .body("Introduction paragraph …")
 *     .spacer()
 *     .tableRow(listOf("Name", "Score"), isHeader = true)
 *     .tableRow(listOf("Alice", "95"))
 *     .build()
 *
 * pdf.writeTo(outputStream)
 * pdf.close()
 * ```
 */
class PDFBuilder(
    val pageSize: PDFPageSize = PDFPageSize.A4,
    val margins: PDFMargins = PDFMargins(),
    val showPageNumbers: Boolean = true,
    val header: String? = null,
    val footer: String? = null
) {

    private val elements = mutableListOf<PDFElement>()

    // ─────────────────────────────────────────────────────────────
    // DSL helpers
    // ─────────────────────────────────────────────────────────────

    fun title(text: String, level: Int = 1, keepWithNext: Boolean = true) = apply {
        elements += PDFElement.Title(text, level, keepWithNext)
    }

    fun body(text: String, bold: Boolean = false, italic: Boolean = false, fontSize: Float = 11f) = apply {
        elements += PDFElement.Body(text, bold, italic, fontSize)
    }

    fun spacer(heightPt: Float = 16f) = apply { elements += PDFElement.Spacer(heightPt) }

    fun divider(thickness: Float = 1f) = apply { elements += PDFElement.Divider(thickness) }

    fun bulletList(items: List<String>) = apply { elements += PDFElement.BulletList(items) }

    fun headerRow(
        leftContent: List<PDFElement>,
        rightContent: List<PDFElement>,
        leftWeight: Float = 2f,
        rightWeight: Float = 1f
    ) = apply {
        elements += PDFElement.HeaderRow(leftContent, rightContent, leftWeight, rightWeight)
    }

    fun tableRow(
        cells: List<String>,
        isHeader: Boolean = false,
        columnWeights: List<Float>? = null,
        headerBgColor: Int = 0xFFEEEEEE.toInt(),
        borderColor: Int = 0xFFCCCCCC.toInt(),
        textColor: Int = 0xFF000000.toInt(),
        cellBgColor: Int = 0xFFFFFFFF.toInt()
    ) = apply {
        elements += PDFElement.TableRow(
            cells = cells,
            isHeader = isHeader,
            columnWeights = columnWeights,
            headerBgColor = headerBgColor,
            borderColor = borderColor,
            textColor = textColor,
            cellBgColor = cellBgColor
        )
    }

    fun image(bitmap: android.graphics.Bitmap, maxHeightPt: Float = 300f, caption: String? = null) = apply {
        elements += PDFElement.Image(bitmap, maxHeightPt, caption)
    }

    fun pageBreak() = apply { elements += PDFElement.PageBreak }

    fun addElement(element: PDFElement) = apply { elements += element }

    // ─────────────────────────────────────────────────────────────
    // Build
    // ─────────────────────────────────────────────────────────────

    /**
     * Renders all elements into an Android [PdfDocument].
     * The caller is responsible for calling [PdfDocument.close] and
     * writing to an [OutputStream] via [PdfDocument.writeTo].
     */
    fun build(): PdfDocument {
        val doc = PdfDocument()

        val contentWidth = pageSize.widthPt - margins.left - margins.right
        val contentHeight = pageSize.heightPt - margins.top - margins.bottom

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageSize.widthPx, pageSize.heightPx, pageNumber).create()
        var page = doc.startPage(pageInfo)
        var painter = PDFPainter(page.canvas)
        var curY = margins.top

        fun drawHeaderFooter(canvas: Canvas, pg: Int) {
            val smallPaint = Paint().apply {
                isAntiAlias = true; textSize = 9f; color = 0xFF888888.toInt()
            }
            if (header != null) {
                canvas.drawText(header, margins.left, margins.top - 10f, smallPaint)
            }
            val footerText = buildString {
                if (footer != null) append(footer)
                if (showPageNumbers) {
                    if (footer != null) append("   |   ")
                    append("Page $pg")
                }
            }
            if (footerText.isNotEmpty()) {
                val tw = smallPaint.measureText(footerText)
                canvas.drawText(
                    footerText,
                    (pageSize.widthPt - tw) / 2f,
                    pageSize.heightPt - margins.bottom + 16f,
                    smallPaint
                )
            }
            // Top rule
            val rulePaint = Paint().apply { strokeWidth = 0.5f; color = 0xFFBBBBBB.toInt() }
            if (header != null) canvas.drawLine(margins.left, margins.top - 4f, pageSize.widthPt - margins.right, margins.top - 4f, rulePaint)
            // Bottom rule
            if (showPageNumbers || footer != null) canvas.drawLine(margins.left, pageSize.heightPt - margins.bottom + 4f, pageSize.widthPt - margins.right, pageSize.heightPt - margins.bottom + 4f, rulePaint)
        }

        drawHeaderFooter(page.canvas, pageNumber)

        fun finishAndStartNewPage() {
            doc.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageSize.widthPx, pageSize.heightPx, pageNumber).create()
            page = doc.startPage(pageInfo)
            painter = PDFPainter(page.canvas)
            curY = margins.top
            drawHeaderFooter(page.canvas, pageNumber)
        }

        elements.forEachIndexed { idx, element ->
            // Force page break element
            if (element is PDFElement.PageBreak) {
                finishAndStartNewPage()
                return@forEachIndexed
            }

            val elHeight = element.measure(contentWidth, painter)

            // Look-ahead: if keepWithNext, also account for the next element's height
            val nextHeight = if (element.keepWithNext && idx + 1 < elements.size) {
                elements[idx + 1].measure(contentWidth, painter)
            } else 0f

            val totalNeeded = elHeight + nextHeight

            if (curY + totalNeeded > margins.top + contentHeight && curY > margins.top) {
                finishAndStartNewPage()
            }

            element.draw(margins.left, curY, contentWidth, painter)
            curY += elHeight
        }

        doc.finishPage(page)
        return doc
    }

    /**
     * Convenience: build and write directly to an [OutputStream].
     */
    fun buildToStream(outputStream: OutputStream) {
        val doc = build()
        doc.writeTo(outputStream)
        doc.close()
    }
}
