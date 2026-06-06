package com.example.primerpro

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ExportModule {

    private fun generateReportText(
        geneName: String,
        forwardSequence: String,
        reverseSequence: String,
        forwardTm: Double,
        reverseTm: Double,
        forwardGc: Double,
        reverseGc: Double,
        productSize: Int
    ): String {
        return """
            |PrimerPro - Primer Design Report
            |=================================
            |Target Gene: $geneName
            |Product Size: $productSize bp
            |
            |Forward Primer:
            |Sequence: 5' - $forwardSequence - 3'
            |Length: ${forwardSequence.length} bp
            |Tm: ${String.format("%.2f", forwardTm)} °C
            |GC%: ${String.format("%.1f", forwardGc)} %
            |
            |Reverse Primer:
            |Sequence: 5' - $reverseSequence - 3'
            |Length: ${reverseSequence.length} bp
            |Tm: ${String.format("%.2f", reverseTm)} °C
            |GC%: ${String.format("%.1f", reverseGc)} %
            |
            |Developed by Shayan Yegan Nejad
        """.trimMargin()
    }

    fun shareAsText(
        context: Context,
        geneName: String,
        forwardSequence: String,
        reverseSequence: String,
        forwardTm: Double,
        reverseTm: Double,
        forwardGc: Double,
        reverseGc: Double,
        productSize: Int
    ) {
        val report = generateReportText(
            geneName, forwardSequence, reverseSequence, forwardTm, reverseTm, forwardGc, reverseGc, productSize
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Primer Design Report - $geneName")
            putExtra(Intent.EXTRA_TEXT, report)
        }
        context.startActivity(Intent.createChooser(intent, "Share Report via"))
    }

    fun shareAsPdf(
        context: Context,
        geneName: String,
        forwardSequence: String,
        reverseSequence: String,
        forwardTm: Double,
        reverseTm: Double,
        forwardGc: Double,
        reverseGc: Double,
        productSize: Int
    ) {
        val pdfr = generateReportText(
            geneName, forwardSequence, reverseSequence, forwardTm, reverseTm, forwardGc, reverseGc, productSize
        )
        
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        
        val canvas: Canvas = page.canvas
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
        }
        
        var yPosition = 50f
        val lines = pdfr.split("\n")
        val paintTitle = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            isFakeBoldText = true
        }

        for ((index, line) in lines.withIndex()) {
            if (index == 0) {
                canvas.drawText(line, 50f, yPosition, paintTitle)
            } else {
                canvas.drawText(line, 50f, yPosition, paint)
            }
            yPosition += 25f
        }
        
        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "PrimerReport_$geneName.pdf")
        val fileOutputStream = FileOutputStream(file)
        pdfDocument.writeTo(fileOutputStream)
        pdfDocument.close()

        val uri: Uri = FileProvider.getUriForFile(
            context,
            context.applicationContext.packageName + ".provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Primer Design Report - $geneName")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share PDF via"))
    }
}
