
package com.electrical.calculationspro.ui.reports

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import java.io.IOException
import kotlin.math.ceil

/**
 * Creates a multipage engineering PDF report using Android APIs only.
 *
 * The supplied report lines remain the source of truth:
 * this class formats and exports them without recalculating
 * engineering values or changing project data.
 */
object EngineeringReportPdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    private const val MARGIN_LEFT = 40f
    private const val MARGIN_RIGHT = 40f
    private const val HEADER_BASELINE = 30f
    private const val CONTENT_TOP = 52f
    private const val FOOTER_BASELINE = 818f

    private const val BODY_TEXT_SIZE = 10.5f
    private const val TITLE_TEXT_SIZE = 11f
    private const val FOOTER_TEXT_SIZE = 8f

    /**
     * Writes the report to the URI returned by
     * ActivityResultContracts.CreateDocument.
     *
     * Exceptions are intentionally propagated to the caller so the
     * UI can display the actual export failure.
     */
    @Throws(IOException::class)
    fun write(
        context: Context,
        uri: Uri,
        title: String,
        lines: List<String>
    ) {
        require(uri.scheme == "content" || uri.scheme == "file") {
            "Unsupported PDF destination URI."
        }

        val resolver = context.contentResolver
        val outputStream = resolver.openOutputStream(uri, "w")
            ?: throw IOException("Could not open the selected PDF destination.")

        val document = PdfDocument()

        try {
            val pageContentWidth =
                PAGE_WIDTH - MARGIN_LEFT.toInt() - MARGIN_RIGHT.toInt()

            val contentBottom = FOOTER_BASELINE - 24f
            val contentHeight = (contentBottom - CONTENT_TOP).toInt()

            require(pageContentWidth > 0 && contentHeight > 0) {
                "Invalid PDF page dimensions."
            }

            val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = BODY_TEXT_SIZE
                typeface = android.graphics.Typeface.create(
                    android.graphics.Typeface.DEFAULT,
                    android.graphics.Typeface.NORMAL
                )
            }

            val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = TITLE_TEXT_SIZE
                typeface = android.graphics.Typeface.create(
                    android.graphics.Typeface.DEFAULT,
                    android.graphics.Typeface.BOLD
                )
            }

            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.DKGRAY
                textSize = FOOTER_TEXT_SIZE
                textAlign = Paint.Align.CENTER
            }

            val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.LTGRAY
                strokeWidth = 0.8f
                style = Paint.Style.STROKE
            }

            val reportText = lines.joinToString(separator = "\n")
                .ifBlank { "No report data available." }

            val layout = StaticLayout.Builder
                .obtain(
                    reportText,
                    0,
                    reportText.length,
                    bodyPaint,
                    pageContentWidth
                )
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
                .setIncludePad(true)
                .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
                .setHyphenationFrequency(
                    Layout.HYPHENATION_FREQUENCY_NONE
                )
                .build()

            val pageCount = maxOf(
                1,
                ceil(layout.height.toDouble() / contentHeight).toInt()
            )

            for (pageNumber in 0 until pageCount) {
                val pageInfo = PdfDocument.PageInfo.Builder(
                    PAGE_WIDTH,
                    PAGE_HEIGHT,
                    pageNumber + 1
                ).create()

                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                canvas.drawColor(Color.WHITE)

                canvas.drawText(
                    title,
                    MARGIN_LEFT,
                    HEADER_BASELINE,
                    headingPaint
                )

                canvas.drawLine(
                    MARGIN_LEFT,
                    39f,
                    PAGE_WIDTH - MARGIN_RIGHT,
                    39f,
                    rulePaint
                )

                canvas.save()
                canvas.clipRect(
                    MARGIN_LEFT,
                    CONTENT_TOP,
                    PAGE_WIDTH - MARGIN_RIGHT,
                    contentBottom
                )
                canvas.translate(
                    MARGIN_LEFT,
                    CONTENT_TOP - pageNumber * contentHeight
                )
                layout.draw(canvas)
                canvas.restore()

                canvas.drawLine(
                    MARGIN_LEFT,
                    800f,
                    PAGE_WIDTH - MARGIN_RIGHT,
                    800f,
                    rulePaint
                )

                canvas.drawText(
                    "Page ${pageNumber + 1} of $pageCount",
                    PAGE_WIDTH / 2f,
                    FOOTER_BASELINE,
                    footerPaint
                )

                document.finishPage(page)
            }

            outputStream.use { stream ->
                document.writeTo(stream)
                stream.flush()
            }
        } finally {
            document.close()
        }
    }
}
