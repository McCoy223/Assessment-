package com.example.util

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.BufferedReader
import java.io.InputStreamReader

object RosterParser {

    /**
     * Parses student names from unstructured or structured text (e.g., CSV, numbered lists, comma/tab separated).
     */
    fun parseStudentNamesFromText(rawText: String): List<String> {
        if (rawText.isBlank()) return emptyList()

        val lines = rawText.lines()
        val detectedNames = mutableListOf<String>()

        val ignoredHeaderKeywords = setOf(
            "name", "names", "student", "students", "score", "grade", "no.", "no", "sn", "s/n",
            "roll", "roll no", "index", "index number", "gender", "class", "subject", "remarks",
            "total", "average", "attendance", "date", "term", "academic", "school"
        )

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue

            // If line is comma-delimited (CSV style), take the first or second column that looks like a name
            val tokens = if (trimmedLine.contains(",") || trimmedLine.contains("\t") || trimmedLine.contains(";")) {
                trimmedLine.split(Regex("[,;\t]")).map { it.trim() }.filter { it.isNotEmpty() }
            } else {
                listOf(trimmedLine)
            }

            for (token in tokens) {
                // Remove leading numbering like "1.", "1)", "01.", "[1]", "-", "*", "•"
                val cleaned = token
                    .replace(Regex("^[0-9]+[.\\)\\-\\]]+\\s*"), "")
                    .replace(Regex("^[\\-*•#]+\\s*"), "")
                    .trim()

                // Skip header terms or pure numbers/symbols
                val lower = cleaned.lowercase()
                if (lower.isEmpty()) continue
                if (ignoredHeaderKeywords.contains(lower)) continue
                if (cleaned.all { it.isDigit() || it.isWhitespace() || it == '.' || it == '-' }) continue
                if (cleaned.length < 2) continue

                // Check if it has at least some alphabetic characters
                if (cleaned.any { it.isLetter() }) {
                    detectedNames.add(cleaned)
                }
            }
        }

        // Deduplicate while preserving insertion order
        return detectedNames.distinct()
    }

    /**
     * Reads a document (plain text, CSV, TSV) from a content URI.
     */
    fun parseDocumentUri(context: Context, uri: Uri): List<String> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            val content = reader.use { it.readText() }
            parseStudentNamesFromText(content)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Extracts text from an image (photo of attendance sheet, roster, printed list) using on-device ML Kit Text Recognition.
     */
    fun parseImageUri(
        context: Context,
        uri: Uri,
        onSuccess: (List<String>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text
                    val names = parseStudentNamesFromText(text)
                    onSuccess(names)
                }
                .addOnFailureListener { exception ->
                    onError(exception)
                }
        } catch (e: Exception) {
            onError(e)
        }
    }
}
