package com.tasha.pdbuilder

import android.os.Build.VERSION_CODES.M
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
//import com.PDFBuilder.*
import java.io.File
import java.io.FileOutputStream

class ExamplePDFBuilder : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Build a sample PDF — the result will look IDENTICAL on
        // a phone, a tablet, or any other Android device.
        val pdf = PDFBuilder(
            pageSize = PDFPageSize.A4,
            margins = PDFMargins(top = 72f, bottom = 72f, left = 72f, right = 72f),
            showPageNumbers = true,
            header = "ACME Corp — Confidential",
            footer = "© 2024 ACME Corp"
        )
            // ── Page 1: Cover section ──────────────────────────────────
            .title("Annual Report 2024", level = 1)
            .spacer(8f)
            .body("Prepared by the Finance Department", italic = true)
            .divider()
            .spacer(16f)

            // ── Executive Summary ──────────────────────────────────────
            .title("Executive Summary", level = 2)
            .body(
                """
                This report presents the consolidated financial results and key
                operational highlights for the fiscal year ending December 31, 2024.
                Revenue grew by 18 % year-over-year, driven primarily by strong
                performance in our cloud division and international expansion.
                """.trimIndent()
            )
            .spacer()

            // ── Key Metrics Table ──────────────────────────────────────
            .title("Key Metrics", level = 2)
            .tableRow(listOf("Metric", "2023", "2024", "Δ"), isHeader = true)
            .tableRow(listOf("Revenue ($M)", "142.5", "168.1", "+18%"))
            .tableRow(listOf("Gross Profit ($M)", "89.3", "107.2", "+20%"))
            .tableRow(listOf("EBITDA ($M)", "34.1", "42.6", "+25%"))
            .tableRow(listOf("Employees", "1,240", "1,480", "+19%"))
            .spacer()

            // ── Bullet highlights ──────────────────────────────────────
            .title("Highlights", level = 2)
            .bulletList(
                listOf(
                    "Launched three new product lines in Q2 exceeding initial targets.",
                    "Expanded into five new international markets.",
                    "Reduced operational costs by 8% through process automation.",
                    "Achieved highest Net Promoter Score in company history: 72."
                )
            )

            // Long body text — auto-flows to new page as needed
            .spacer()
            .title("Detailed Analysis", level = 2)
            .body(
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do " +
                        "eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim " +
                        "ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut " +
                        "aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit " +
                        "in voluptate velit esse cillum dolore eu fugiat nulla pariatur. ".repeat(8)
            )

            // Force a new page for the appendix
            .pageBreak()
            .title("Appendix A — Regional Breakdown", level = 2)
            .tableRow(listOf("Region", "Revenue ($M)", "Growth"), isHeader = true)
            .tableRow(listOf("North America", "82.4", "+14%"))
            .tableRow(listOf("Europe", "51.6", "+22%"))
            .tableRow(listOf("Asia-Pacific", "28.1", "+31%"))
            .tableRow(listOf("Other", "6.0", "+8%"))

        // Write to Downloads folder
        val file = File(
            getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "annual_report_2024.pdf"
        )
        FileOutputStream(file).use { pdf.buildToStream(it) }

        // In a real app you'd open the file or share it here
    }
}