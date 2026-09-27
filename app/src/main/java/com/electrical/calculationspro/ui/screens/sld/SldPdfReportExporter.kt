package com.electrical.calculationspro.ui.screens.sld

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.OutputStream

object SldPdfReportExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    private const val LEFT = 42f
    private const val RIGHT = 553f
    private const val TOP = 48f
    private const val BOTTOM = 790f

    fun export(
        outputStream: OutputStream,
        title: String,
        reportText: String
    ) {

        val document =
            PdfDocument()

        val titlePaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                textSize = 20f
                isFakeBoldText = true
            }

        val sectionPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                textSize = 13f
                isFakeBoldText = true
            }

        val bodyPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                textSize = 10f
            }

        val footerPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                textSize = 8f
            }

        var pageNumber =
            1

        var page =
            createPage(
                document,
                pageNumber
            )

        var canvas =
            page.canvas

        var y =
            TOP

        canvas.drawText(
            title,
            LEFT,
            y,
            titlePaint
        )

        y += 28f

        canvas.drawText(
            "Electrical Engineering Design Report",
            LEFT,
            y,
            sectionPaint
        )

        y += 28f

        canvas.drawLine(
            LEFT,
            y,
            RIGHT,
            y,
            bodyPaint
        )

        y += 22f

        val lines =
            prepareLines(
                reportText,
                bodyPaint
            )

        for (line in lines) {

            if (
                y >
                    BOTTOM -
                    25f
            ) {

                drawFooter(
                    canvas,
                    pageNumber,
                    footerPaint
                )

                document.finishPage(
                    page
                )

                pageNumber++

                page =
                    createPage(
                        document,
                        pageNumber
                    )

                canvas =
                    page.canvas

                y =
                    TOP
            }

            val paint =
                if (
                    isSectionHeading(
                        line
                    )
                ) {
                    sectionPaint
                } else {
                    bodyPaint
                }

            if (
                isSectionHeading(
                    line
                )
            ) {
                y += 8f
            }

            canvas.drawText(
                line,
                LEFT,
                y,
                paint
            )

            y +=
                if (
                    isSectionHeading(
                        line
                    )
                ) {
                    22f
                } else {
                    16f
                }
        }

        drawFooter(
            canvas,
            pageNumber,
            footerPaint
        )

        document.finishPage(
            page
        )

        document.writeTo(
            outputStream
        )

        document.close()
    }

    private fun createPage(
        document: PdfDocument,
        number: Int
    ): PdfDocument.Page {

        return document.startPage(
            PdfDocument.PageInfo.Builder(
                PAGE_WIDTH,
                PAGE_HEIGHT,
                number
            ).create()
        )
    }

    private fun drawFooter(
        canvas: android.graphics.Canvas,
        pageNumber: Int,
        paint: Paint
    ) {

        canvas.drawLine(
            LEFT,
            805f,
            RIGHT,
            805f,
            paint
        )

        canvas.drawText(
            "ElectricalCalculationsPro",
            LEFT,
            820f,
            paint
        )

        canvas.drawText(
            "Page $pageNumber",
            500f,
            820f,
            paint
        )
    }

    private fun prepareLines(
        text: String,
        paint: Paint
    ): List<String> {

        val result =
            mutableListOf<String>()

        text
            .replace(
                "\r\n",
                "\n"
            )
            .split("\n")
            .forEach { raw ->

                if (
                    raw.isBlank()
                ) {

                    result += ""

                } else {

                    wrapLine(
                        raw,
                        paint
                    ).forEach {
                        result += it
                    }
                }
            }

        return result
    }

    private fun wrapLine(
        text: String,
        paint: Paint
    ): List<String> {

        val maxWidth =
            RIGHT -
                LEFT

        val words =
            text.trim()
                .split(
                    Regex("\\s+")
                )

        val lines =
            mutableListOf<String>()

        var current =
            ""

        for (word in words) {

            val candidate =
                if (
                    current.isEmpty()
                ) {
                    word
                } else {
                    "$current $word"
                }

            if (
                paint.measureText(
                    candidate
                ) <= maxWidth
            ) {

                current =
                    candidate

            } else {

                if (
                    current.isNotEmpty()
                ) {
                    lines +=
                        current
                }

                current =
                    word
            }
        }

        if (
            current.isNotEmpty()
        ) {
            lines +=
                current
        }

        return lines
    }

    private fun isSectionHeading(
        text: String
    ): Boolean {

        val value =
            text.trim()

        if (
            value.isEmpty()
        ) {
            return false
        }

        return value.endsWith(":") ||
            value.startsWith("===") ||
            value.startsWith("---") ||
            value.contains(
                "SUMMARY",
                ignoreCase = true
            ) ||
            value.contains(
                "NETWORK",
                ignoreCase = true
            ) ||
            value.contains(
                "SOURCE",
                ignoreCase = true
            ) ||
            value.contains(
                "FEEDER",
                ignoreCase = true
            ) ||
            value.contains(
                "CABLE",
                ignoreCase = true
            ) ||
            value.contains(
                "BREAKER",
                ignoreCase = true
            ) ||
            value.contains(
                "TRANSFORMER",
                ignoreCase = true
            ) ||
            value.contains(
                "WARNING",
                ignoreCase = true
            )
    }
}
