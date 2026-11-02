package com.example.data.backup

import android.content.Context
import android.net.Uri
import com.example.data.model.Student
import com.example.data.model.StudentScore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupMetadata(
    val version: Int = 1,
    val appName: String = "EvaluTrack",
    val exportedAt: Long = System.currentTimeMillis(),
    val totalStudents: Int,
    val totalScores: Int
)

data class DatabaseRestoreResult(
    val success: Boolean,
    val studentsRestored: Int = 0,
    val scoresRestored: Int = 0,
    val message: String
)

object DatabaseBackupManager {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val fileTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())

    fun generateBackupFileName(): String {
        val stamp = fileTimestampFormat.format(Date())
        return "evalutrack_backup_$stamp.json"
    }

    fun generateCsvFileName(): String {
        val stamp = fileTimestampFormat.format(Date())
        return "evalutrack_gradebook_$stamp.csv"
    }

    suspend fun exportDatabaseToJson(
        context: Context,
        uri: Uri,
        students: List<Student>,
        scores: List<StudentScore>
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject()
            root.put("version", 1)
            root.put("appName", "EvaluTrack")
            root.put("storageType", "PhoneStorage_GoogleDrive_Sync")
            root.put("exportedAt", System.currentTimeMillis())
            root.put("exportedAtFormatted", dateFormat.format(Date()))
            root.put("studentsCount", students.size)
            root.put("scoresCount", scores.size)

            val studentsArray = JSONArray()
            for (student in students) {
                val sObj = JSONObject()
                sObj.put("id", student.id)
                sObj.put("name", student.name)
                sObj.put("studentId", student.studentId)
                sObj.put("source", student.source)
                sObj.put("dateAdded", student.dateAdded)
                studentsArray.put(sObj)
            }
            root.put("students", studentsArray)

            val scoresArray = JSONArray()
            for (score in scores) {
                val scObj = JSONObject()
                scObj.put("id", score.id)
                scObj.put("studentName", score.studentName)
                scObj.put("assessmentName", score.assessmentName)
                scObj.put("subject", score.subject)
                scObj.put("score", score.score)
                scObj.put("maxScore", score.maxScore)
                scObj.put("category", score.category)
                scObj.put("dateMillis", score.dateMillis)
                scObj.put("notes", score.notes)
                scoresArray.put(scObj)
            }
            root.put("scores", scoresArray)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(root.toString(2))
                    writer.flush()
                }
            } ?: return@withContext Result.failure(Exception("Failed to open output stream for destination"))

            Result.success(scores.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importDatabaseFromJson(
        context: Context,
        uri: Uri
    ): Result<Pair<List<Student>, List<StudentScore>>> = withContext(Dispatchers.IO) {
        try {
            val contentBuilder = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        contentBuilder.append(line).append("\n")
                        line = reader.readLine()
                    }
                }
            } ?: return@withContext Result.failure(Exception("Cannot open selected backup file"))

            val jsonString = contentBuilder.toString().trim()
            if (jsonString.isEmpty()) {
                return@withContext Result.failure(Exception("The selected file is empty"))
            }

            val root = JSONObject(jsonString)
            val studentsList = mutableListOf<Student>()
            val scoresList = mutableListOf<StudentScore>()

            if (root.has("students")) {
                val studentsArray = root.getJSONArray("students")
                for (i in 0 until studentsArray.length()) {
                    val item = studentsArray.getJSONObject(i)
                    studentsList.add(
                        Student(
                            id = 0L, // Reset ID so Room assigns fresh autoincrement ID or maintains integrity
                            name = item.optString("name", "Unknown").trim(),
                            studentId = item.optString("studentId", "").trim(),
                            source = item.optString("source", "Restored Backup"),
                            dateAdded = item.optLong("dateAdded", System.currentTimeMillis())
                        )
                    )
                }
            }

            if (root.has("scores")) {
                val scoresArray = root.getJSONArray("scores")
                for (i in 0 until scoresArray.length()) {
                    val item = scoresArray.getJSONObject(i)
                    scoresList.add(
                        StudentScore(
                            id = 0L, // Fresh ID
                            studentName = item.optString("studentName", "").trim(),
                            assessmentName = item.optString("assessmentName", "").trim(),
                            subject = item.optString("subject", "General").trim(),
                            score = item.optDouble("score", 0.0),
                            maxScore = item.optDouble("maxScore", 100.0),
                            category = item.optString("category", "General").trim(),
                            dateMillis = item.optLong("dateMillis", System.currentTimeMillis()),
                            notes = item.optString("notes", "").trim()
                        )
                    )
                }
            }

            // Fallback: If JSON is simply an array of scores
            if (!root.has("scores") && !root.has("students") && jsonString.startsWith("[")) {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    if (item.has("studentName") && item.has("score")) {
                        scoresList.add(
                            StudentScore(
                                id = 0L,
                                studentName = item.optString("studentName", ""),
                                assessmentName = item.optString("assessmentName", "Imported Assessment"),
                                subject = item.optString("subject", "General"),
                                score = item.optDouble("score", 0.0),
                                maxScore = item.optDouble("maxScore", 100.0),
                                category = item.optString("category", "Quiz"),
                                dateMillis = item.optLong("dateMillis", System.currentTimeMillis()),
                                notes = item.optString("notes", "")
                            )
                        )
                    }
                }
            }

            Result.success(Pair(studentsList, scoresList))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportGradebookToCsv(
        context: Context,
        uri: Uri,
        scores: List<StudentScore>,
        students: List<Student>
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val studentIdMap = students.associate { it.name.lowercase().trim() to it.studentId }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    // UTF-8 BOM so Excel opens it with proper character encoding
                    writer.write("\uFEFF")
                    // Header
                    writer.write("Student Name,Student ID,Subject,Assessment,Category,Score,Max Score,Percentage,Grade,Date,Notes\n")

                    for (sc in scores) {
                        val stuId = studentIdMap[sc.studentName.lowercase().trim()] ?: ""
                        val row = listOf(
                            escapeCsv(sc.studentName),
                            escapeCsv(stuId),
                            escapeCsv(sc.subject),
                            escapeCsv(sc.assessmentName),
                            escapeCsv(sc.category),
                            sc.score.toString(),
                            sc.maxScore.toString(),
                            String.format(Locale.US, "%.1f%%", sc.percentage),
                            sc.letterGrade,
                            escapeCsv(dateFormat.format(Date(sc.dateMillis))),
                            escapeCsv(sc.notes)
                        )
                        writer.write(row.joinToString(",") + "\n")
                    }
                    writer.flush()
                }
            } ?: return@withContext Result.failure(Exception("Cannot open CSV file output"))

            Result.success(scores.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\"", "\"\"")
        return if (clean.contains(",") || clean.contains("\"") || clean.contains("\n")) {
            "\"$clean\""
        } else {
            clean
        }
    }
}
