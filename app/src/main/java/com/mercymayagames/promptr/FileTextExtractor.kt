package com.mercymayagames.promptr

import android.content.Context
import android.net.Uri
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * FileTextExtractor – yanks pure text from .txt / .pdf / .docx
 * …and politely ignores any images or fancy styling.
 */
object FileTextExtractor {

    fun extractText(ctx: Context, uri: Uri): String =
        when (ctx.contentResolver.getType(uri)) {
            "text/plain" -> readTxt(ctx, uri)
            "application/pdf" -> readPdf(ctx, uri)
            else -> readDocx(ctx, uri)  // default to DOCX
        }

    private fun readTxt(ctx: Context, uri: Uri): String =
        ctx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { br ->
            br.readText()
        } ?: ""

    private fun readPdf(ctx: Context, uri: Uri): String {
        val sb = StringBuilder()
        ctx.contentResolver.openInputStream(uri)?.use { stream ->
            val reader = PdfReader(stream)
            (1..reader.numberOfPages).forEach { page ->
                sb.append(PdfTextExtractor.getTextFromPage(reader, page)).append("\n\n")
            }
            reader.close()
        }
        return sb.toString()
    }

    private fun readDocx(ctx: Context, uri: Uri): String {
        ctx.contentResolver.openInputStream(uri)?.use { stream ->
            val doc = XWPFDocument(stream)
            val extractor = XWPFWordExtractor(doc)
            val text = extractor.text
            extractor.close(); doc.close()
            return text
        }
        return ""
    }
}
