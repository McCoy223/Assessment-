package com.example.data.model

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.ui.theme.GradeBlue
import com.example.ui.theme.GradeGreen
import com.example.ui.theme.GradeOrange
import com.example.ui.theme.GradeRed
import com.example.ui.theme.GradeYellow

@Entity(tableName = "student_scores")
data class StudentScore(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentName: String,
    val assessmentName: String,
    val subject: String = "Mathematics", // Mathematics, Science, English, History, etc.
    val score: Double,
    val maxScore: Double = 100.0,
    val category: String = "Quiz", // Quiz, Exam, Homework, Project, Midterm, Final
    val dateMillis: Long = System.currentTimeMillis(),
    val notes: String = ""
) {
    val percentage: Double
        get() = if (maxScore > 0) (score / maxScore) * 100.0 else 0.0

    val letterGrade: String
        get() = when {
            percentage >= 90.0 -> "A"
            percentage >= 80.0 -> "B"
            percentage >= 70.0 -> "C"
            percentage >= 60.0 -> "D"
            else -> "F"
        }

    val gradeColor: Color
        get() = when {
            percentage >= 90.0 -> GradeGreen
            percentage >= 80.0 -> GradeBlue
            percentage >= 70.0 -> GradeYellow
            percentage >= 60.0 -> GradeOrange
            else -> GradeRed
        }

    val gpaPoint: Double
        get() = when {
            percentage >= 90.0 -> 4.0
            percentage >= 80.0 -> 3.0
            percentage >= 70.0 -> 2.0
            percentage >= 60.0 -> 1.0
            else -> 0.0
        }
}
