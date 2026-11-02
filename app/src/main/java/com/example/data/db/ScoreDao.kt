package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.StudentScore
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreDao {
    @Query("SELECT * FROM student_scores ORDER BY dateMillis DESC")
    fun getAllScores(): Flow<List<StudentScore>>

    @Query("SELECT * FROM student_scores WHERE studentName = :studentName ORDER BY dateMillis ASC")
    fun getScoresForStudentChronological(studentName: String): Flow<List<StudentScore>>

    @Query("SELECT DISTINCT studentName FROM student_scores ORDER BY studentName ASC")
    fun getAllStudentNames(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: StudentScore): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(scores: List<StudentScore>)

    @Update
    suspend fun updateScore(score: StudentScore)

    @Delete
    suspend fun deleteScore(score: StudentScore)

    @Query("DELETE FROM student_scores WHERE id = :id")
    suspend fun deleteScoreById(id: Long)

    @Query("SELECT COUNT(*) FROM student_scores")
    suspend fun getScoreCount(): Int

    @Query("SELECT * FROM student_scores ORDER BY dateMillis DESC")
    suspend fun getAllScoresList(): List<StudentScore>

    @Query("DELETE FROM student_scores")
    suspend fun deleteAllScores()
}
