package com.example.domain

import com.example.data.model.StudentScore
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class GradeDistribution(
    val aCount: Int = 0,
    val bCount: Int = 0,
    val cCount: Int = 0,
    val dCount: Int = 0,
    val fCount: Int = 0
) {
    val total: Int = aCount + bCount + cCount + dCount + fCount

    fun percentA(): Float = if (total > 0) (aCount.toFloat() / total) * 100f else 0f
    fun percentB(): Float = if (total > 0) (bCount.toFloat() / total) * 100f else 0f
    fun percentC(): Float = if (total > 0) (cCount.toFloat() / total) * 100f else 0f
    fun percentD(): Float = if (total > 0) (dCount.toFloat() / total) * 100f else 0f
    fun percentF(): Float = if (total > 0) (fCount.toFloat() / total) * 100f else 0f
}

data class CategoryStat(
    val category: String,
    val averageScore: Double,
    val count: Int
)

data class SubjectStat(
    val subject: String,
    val averageScore: Double,
    val count: Int
)

data class StudentProgressSummary(
    val studentName: String,
    val assessmentCount: Int,
    val currentAverage: Double,
    val highestScore: Double,
    val lowestScore: Double,
    val trendDelta: Double,
    val trendType: TrendType,
    val letterGrade: String
)

enum class TrendType {
    IMPROVING,
    STABLE,
    DECLINING
}

data class ClassGradeAnalysis(
    val totalAssessments: Int,
    val totalStudents: Int,
    val averageScore: Double,
    val medianScore: Double,
    val standardDeviation: Double,
    val highestScore: Double,
    val lowestScore: Double,
    val passingRate: Double,
    val masteryRate: Double,
    val distribution: GradeDistribution,
    val categoryStats: List<CategoryStat>,
    val subjectStats: List<SubjectStat>,
    val studentSummaries: List<StudentProgressSummary>
)

object GradeAnalysisTools {

    fun computeClassAnalysis(scores: List<StudentScore>): ClassGradeAnalysis {
        if (scores.isEmpty()) {
            return ClassGradeAnalysis(
                totalAssessments = 0,
                totalStudents = 0,
                averageScore = 0.0,
                medianScore = 0.0,
                standardDeviation = 0.0,
                highestScore = 0.0,
                lowestScore = 0.0,
                passingRate = 0.0,
                masteryRate = 0.0,
                distribution = GradeDistribution(),
                categoryStats = emptyList(),
                subjectStats = emptyList(),
                studentSummaries = emptyList()
            )
        }

        val percentages = scores.map { it.percentage }
        val avg = percentages.average()
        val sortedPercentages = percentages.sorted()
        val median = if (sortedPercentages.size % 2 == 1) {
            sortedPercentages[sortedPercentages.size / 2]
        } else {
            (sortedPercentages[sortedPercentages.size / 2 - 1] + sortedPercentages[sortedPercentages.size / 2]) / 2.0
        }

        // Standard Deviation
        val variance = if (percentages.size > 1) {
            percentages.sumOf { (it - avg).pow(2) } / (percentages.size - 1)
        } else 0.0
        val stdDev = sqrt(variance)

        val max = sortedPercentages.last()
        val min = sortedPercentages.first()
        val passingCount = sortedPercentages.count { it >= 60.0 }
        val masteryCount = sortedPercentages.count { it >= 80.0 }
        val passingRate = (passingCount.toDouble() / sortedPercentages.size) * 100.0
        val masteryRate = (masteryCount.toDouble() / sortedPercentages.size) * 100.0

        // Distribution
        var a = 0
        var b = 0
        var c = 0
        var d = 0
        var f = 0
        for (score in scores) {
            when (score.letterGrade) {
                "A" -> a++
                "B" -> b++
                "C" -> c++
                "D" -> d++
                else -> f++
            }
        }
        val distribution = GradeDistribution(a, b, c, d, f)

        // Categories
        val categoryStats = scores.groupBy { it.category }
            .map { (cat, catScores) ->
                CategoryStat(
                    category = cat,
                    averageScore = (catScores.map { it.percentage }.average() * 10).roundToInt() / 10.0,
                    count = catScores.size
                )
            }.sortedByDescending { it.count }

        // Subjects
        val subjectStats = scores.groupBy { it.subject }
            .map { (subj, subjScores) ->
                SubjectStat(
                    subject = subj,
                    averageScore = (subjScores.map { it.percentage }.average() * 10).roundToInt() / 10.0,
                    count = subjScores.size
                )
            }.sortedByDescending { it.count }

        // Per-student summaries & progress
        val studentGroups = scores.groupBy { it.studentName }
        val studentSummaries = studentGroups.map { (student, studentScores) ->
            val sortedByDate = studentScores.sortedBy { it.dateMillis }
            val studentAvg = sortedByDate.map { it.percentage }.average()
            val studentMax = sortedByDate.maxOf { it.percentage }
            val studentMin = sortedByDate.minOf { it.percentage }

            // Trend calculation: compare second half vs first half
            val trendDelta = if (sortedByDate.size >= 2) {
                val half = sortedByDate.size / 2
                val firstHalfAvg = sortedByDate.take(half).map { it.percentage }.average()
                val secondHalfAvg = sortedByDate.takeLast(sortedByDate.size - half).map { it.percentage }.average()
                secondHalfAvg - firstHalfAvg
            } else {
                0.0
            }

            val trendType = when {
                trendDelta >= 2.0 -> TrendType.IMPROVING
                trendDelta <= -2.0 -> TrendType.DECLINING
                else -> TrendType.STABLE
            }

            val letterGrade = when {
                studentAvg >= 90.0 -> "A"
                studentAvg >= 80.0 -> "B"
                studentAvg >= 70.0 -> "C"
                studentAvg >= 60.0 -> "D"
                else -> "F"
            }

            StudentProgressSummary(
                studentName = student,
                assessmentCount = studentScores.size,
                currentAverage = (studentAvg * 10).roundToInt() / 10.0,
                highestScore = studentMax,
                lowestScore = studentMin,
                trendDelta = (trendDelta * 10).roundToInt() / 10.0,
                trendType = trendType,
                letterGrade = letterGrade
            )
        }.sortedByDescending { it.currentAverage }

        return ClassGradeAnalysis(
            totalAssessments = scores.size,
            totalStudents = studentGroups.keys.size,
            averageScore = (avg * 10).roundToInt() / 10.0,
            medianScore = (median * 10).roundToInt() / 10.0,
            standardDeviation = (stdDev * 10).roundToInt() / 10.0,
            highestScore = max,
            lowestScore = min,
            passingRate = (passingRate * 10).roundToInt() / 10.0,
            masteryRate = (masteryRate * 10).roundToInt() / 10.0,
            distribution = distribution,
            categoryStats = categoryStats,
            subjectStats = subjectStats,
            studentSummaries = studentSummaries
        )
    }
}
