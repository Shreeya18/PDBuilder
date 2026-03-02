PDFBuilder 📄
A simple Android library to generate consistent, professional PDF files from Kotlin code. Works identically on all Android devices — phones, tablets, and emulators.

Installation
Step 1 — Add JitPack to your settings.gradle.kts:
kotlindependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
Step 2 — Add the dependency:
groovyimplementation 'com.github.Shreeya18:PDBuilder:1.0.7'

Quick Start
kotlinval pdf = PDFBuilder(pageSize = PDFPageSize.A4)
    .title("My Report")
    .body("This is the first paragraph of my report.")
    .spacer()
    .tableRow(listOf("Item", "Price"), isHeader = true)
    .tableRow(listOf("Consulting Fee", "₹250"))
    .divider()

val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "report.pdf")
FileOutputStream(file).use { pdf.buildToStream(it) }

Features

✅ Consistent layout across all Android devices
✅ Automatic page breaks
✅ Headers and footers on every page
✅ Page numbers
✅ Tables with custom colors and column widths
✅ Images with captions
✅ Bullet lists
✅ Multiple page sizes (A4, Letter, Legal, A3, A5)
✅ Bold, italic, and custom font sizes


Available Elements
MethodDescription.title(text, level)Heading (level 1–4).body(text)Paragraph text.tableRow(cells, isHeader)Table row.bulletList(items)Bulleted list.image(bitmap)Image with optional caption.divider()Horizontal line.spacer()Empty vertical space.pageBreak()Force new page

Page Sizes
kotlinPDFPageSize.A4      // 595 x 842 pt
PDFPageSize.LETTER  // 612 x 792 pt
PDFPageSize.LEGAL   // 612 x 1008 pt
PDFPageSize.A3      // 842 x 1191 pt
PDFPageSize.A5      // 420 x 595 pt

Table with Custom Colors
kotlinpdf.tableRow(
    cells = listOf("Total", "₹3940"),
    isHeader = true,
    headerBgColor = 0xFF1976D2.toInt(),  // blue
    textColor = 0xFFFFFFFF.toInt(),       // white text
    borderColor = 0xFF1976D2.toInt()
)

Full Invoice Example
kotlinval pdf = PDFBuilder(
    pageSize = PDFPageSize.A4,
    margins = PDFMargins(top = 72f, bottom = 72f, left = 72f, right = 72f),
    showPageNumbers = false,
    header = "Shalini Clinic"
)
    .title("Invoice", level = 2)
    .spacer(8f)
    .body("Billed to: Ms. Amira Shah", bold = true)
    .body("Date: 23 Feb 2026")
    .divider()
    .tableRow(listOf("Description", "Amount"), isHeader = true, columnWeights = listOf(3f, 1f))
    .tableRow(listOf("Consulting Fee", "₹250"), columnWeights = listOf(3f, 1f))
    .tableRow(listOf("Medicine", "₹3690"), columnWeights = listOf(3f, 1f))
    .tableRow(listOf("Total", "₹3940"), isHeader = true, columnWeights = listOf(3f, 1f))

val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "invoice.pdf")
FileOutputStream(file).use { pdf.buildToStream(it) }

Requirements

Android API 21+
Kotlin


License
MIT License — free to use in personal and commercial projects.
