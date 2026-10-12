package com.example.data

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PdfParser(private val context: Context) {

    init {
        // Инициализация движка PDFBox при создании класса
        PDFBoxResourceLoader.init(context)
    }

    /**
     * Читает PDF-файл по ссылке (Uri) и возвращает весь текст.
     * Работает только с текстовыми PDF (не сканами).
     */
    suspend fun extractTextFromPdf(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Не удалось открыть файл"))

            inputStream.use { stream ->
                val document = PDDocument.load(stream)
                val textStripper = PDFTextStripper()
                val text = textStripper.getText(document)
                document.close()
                Result.success(text)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}