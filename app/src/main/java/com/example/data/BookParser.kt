package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.text.Html
import android.util.Xml
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

class BookParser(private val context: Context) {

    init {
        PDFBoxResourceLoader.init(context)
    }

    suspend fun extractTextFromBook(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val format = detectFormat(uri)
            val text = when (format) {
                "epub", "zip" -> parseZip(uri) // Если это ZIP, ищем внутри и HTML, и FB2
                "fb2" -> parseFb2(uri)
                else -> parsePdf(uri)
            }
            
            if (text.isBlank()) {
                Result.failure(Exception("Не удалось извлечь текст. Возможно, это сканы без текстового слоя."))
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun detectFormat(uri: Uri): String {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val buffered = BufferedInputStream(stream)
                buffered.mark(4)
                val header = ByteArray(4)
                if (buffered.read(header) >= 4) {
                    buffered.reset()
                    if (header[0] == 0x25.toByte() && header[1] == 0x50.toByte() && header[2] == 0x44.toByte() && header[3] == 0x46.toByte()) return "pdf"
                    if (header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()) return "zip" // EPUB или ZIP
                    if (header[0] == 0x3C.toByte()) return "fb2"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        val name = getFileName(uri)?.lowercase() ?: ""
        return when {
            name.endsWith(".epub") || name.endsWith(".zip") -> "zip"
            name.endsWith(".fb2") -> "fb2"
            else -> "pdf"
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) name = cursor.getString(nameIndex)
                }
            }
        } catch (e: Exception) {
            name = uri.lastPathSegment
        }
        return name
    }

    private fun parsePdf(uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri) ?: throw Exception("Не удалось открыть PDF")
        inputStream.use { stream ->
            val document = PDDocument.load(stream)
            val textStripper = PDFTextStripper()
            val text = textStripper.getText(document)
            document.close()
            return text
        }
    }

    private fun parseFb2(uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri) ?: throw Exception("Не удалось открыть FB2")
        return inputStream.use { parseFb2Stream(it) }
    }

    // Безопасный парсер XML, который не ломается на вложенных тегах
    private fun parseFb2Stream(stream: InputStream): String {
        val textBuilder = StringBuilder()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(stream, null)

        var eventType = parser.eventType
        var insideBody = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name.equals("body", ignoreCase = true)) {
                        insideBody = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideBody) {
                        val text = parser.text
                        if (text != null && text.isNotBlank()) {
                            textBuilder.append(text.trim()).append(" ")
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.equals("body", ignoreCase = true)) {
                        insideBody = false
                    }
                    // Делаем отступ после каждого абзаца
                    if (insideBody && (parser.name.equals("p", ignoreCase = true) || parser.name.equals("subtitle", ignoreCase = true))) {
                        textBuilder.append("\n\n")
                    }
                }
            }
            eventType = parser.next()
        }
        return textBuilder.toString()
    }

    // Ищем внутри ZIP и HTML (EPUB), и FB2 (на всякий случай)
    private fun parseZip(uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri) ?: throw Exception("Не удалось открыть архив")
        val textBuilder = StringBuilder()
        
        inputStream.use { stream ->
            val zipInputStream = ZipInputStream(BufferedInputStream(stream))
            var entry = zipInputStream.nextEntry
            val textFiles = mutableListOf<Pair<String, String>>()

            while (entry != null) {
                val name = entry.name.lowercase()
                if (name.endsWith(".html") || name.endsWith(".xhtml") || name.endsWith(".htm") || name.endsWith(".fb2")) {
                    try {
                        val content = zipInputStream.readBytes().toString(Charsets.UTF_8)
                        textFiles.add(entry.name to content)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }

            textFiles.sortBy { it.first }

            for ((fileName, fileContent) in textFiles) {
                if (fileName.endsWith(".fb2")) {
                    fileContent.byteInputStream().use { 
                        textBuilder.append(parseFb2Stream(it)).append("\n\n")
                    }
                } else {
                    val cleanText = Html.fromHtml(fileContent, Html.FROM_HTML_MODE_LEGACY).toString()
                    if (cleanText.isNotBlank()) textBuilder.append(cleanText.trim()).append("\n\n")
                }
            }
        }
        return textBuilder.toString()
    }
}
