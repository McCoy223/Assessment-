package com.example.data.repository

import com.example.data.db.ScoreDao
import com.example.data.db.StudentDao
import com.example.data.model.Student
import com.example.data.model.StudentScore
import kotlinx.coroutines.flow.Flow

class ScoreRepository(
    private val scoreDao: ScoreDao,
    private val studentDao: StudentDao
) {
    val allScores: Flow<List<StudentScore>> = scoreDao.getAllScores()
    val allStudentNamesFromScores: Flow<List<String>> = scoreDao.getAllStudentNames()

    val allStudents: Flow<List<Student>> = studentDao.getAllStudents()
    val allRosterStudentNames: Flow<List<String>> = studentDao.getAllStudentNames()

    fun getScoresForStudent(studentName: String): Flow<List<StudentScore>> {
        return scoreDao.getScoresForStudentChronological(studentName)
    }

    suspend fun insertScore(score: StudentScore): Long {
        return scoreDao.insertScore(score)
    }

    suspend fun updateScore(score: StudentScore) {
        scoreDao.updateScore(score)
    }

    suspend fun deleteScore(score: StudentScore) {
        scoreDao.deleteScore(score)
    }

    suspend fun deleteScoreById(id: Long) {
        scoreDao.deleteScoreById(id)
    }

    suspend fun insertStudent(student: Student): Long {
        return studentDao.insertStudent(student)
    }

    suspend fun insertStudents(students: List<Student>) {
        studentDao.insertStudents(students)
    }

    suspend fun deleteStudent(student: Student) {
        studentDao.deleteStudent(student)
    }

    suspend fun getAllScoresList(): List<StudentScore> = scoreDao.getAllScoresList()

    suspend fun getAllStudentsList(): List<Student> = studentDao.getAllStudentsList()

    suspend fun insertScores(scores: List<StudentScore>) {
        scoreDao.insertAll(scores)
    }

    suspend fun clearDatabase() {
        scoreDao.deleteAllScores()
        studentDao.deleteAllStudents()
    }

    suspend fun restoreDatabase(students: List<Student>, scores: List<StudentScore>, replaceExisting: Boolean) {
        if (replaceExisting) {
            clearDatabase()
        }
        if (students.isNotEmpty()) {
            studentDao.insertStudents(students)
        }
        if (scores.isNotEmpty()) {
            scoreDao.insertAll(scores)
        }
    }

    suspend fun seedInitialDataIfNeeded() {
        if (scoreDao.getScoreCount() == 0) {
            com.example.data.db.AppDatabase.populateInitialScores(scoreDao)
        }
        if (studentDao.getStudentCount() == 0) {
            com.example.data.db.AppDatabase.populateInitialStudents(studentDao)
        }
    }
}
