package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.StudentScore
import com.example.domain.GradeAnalysisTools
import com.example.ui.viewmodel.CURRICULUM_SUBJECTS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("EvaluTrack", appName)
  }

  @Test
  fun `verify curriculum subjects list`() {
    val expected = listOf(
      "English",
      "Mathematics",
      "Religious and Moral Education (R.M.E.)",
      "Ghanaian Language",
      "Science",
      "Creative Art",
      "History",
      "Computing"
    )
    assertEquals(expected, CURRICULUM_SUBJECTS)
  }

  @Test
  fun `verify letter grade calculation`() {
    val scoreA = StudentScore(studentName = "Alex", assessmentName = "Quiz", score = 95.0, subject = "Mathematics")
    val scoreB = StudentScore(studentName = "Alex", assessmentName = "Quiz", score = 84.0, subject = "Science")
    val scoreC = StudentScore(studentName = "Alex", assessmentName = "Quiz", score = 72.0, subject = "History")
    val scoreD = StudentScore(studentName = "Alex", assessmentName = "Quiz", score = 61.0, subject = "English")
    val scoreF = StudentScore(studentName = "Alex", assessmentName = "Quiz", score = 45.0, subject = "Creative Art")

    assertEquals("A", scoreA.letterGrade)
    assertEquals("B", scoreB.letterGrade)
    assertEquals("C", scoreC.letterGrade)
    assertEquals("D", scoreD.letterGrade)
    assertEquals("F", scoreF.letterGrade)
    assertEquals("Mathematics", scoreA.subject)
  }

  @Test
  fun `verify grade analysis computation and subject breakdown`() {
    val scores = listOf(
      StudentScore(studentName = "Alice", assessmentName = "Test 1", score = 90.0, subject = "Mathematics"),
      StudentScore(studentName = "Bob", assessmentName = "Test 1", score = 80.0, subject = "Mathematics"),
      StudentScore(studentName = "Charlie", assessmentName = "Lab 1", score = 70.0, subject = "Science")
    )

    val analysis = GradeAnalysisTools.computeClassAnalysis(scores)
    assertEquals(3, analysis.totalAssessments)
    assertEquals(80.0, analysis.averageScore, 0.1)
    assertEquals(80.0, analysis.medianScore, 0.1)
    assertEquals(100.0, analysis.passingRate, 0.1)

    val mathStat = analysis.subjectStats.find { it.subject == "Mathematics" }
    val scienceStat = analysis.subjectStats.find { it.subject == "Science" }
    assertEquals(85.0, mathStat?.averageScore ?: 0.0, 0.1)
    assertEquals(2, mathStat?.count)
    assertEquals(70.0, scienceStat?.averageScore ?: 0.0, 0.1)
    assertEquals(1, scienceStat?.count)
  }

  @Test
  fun `verify database backup and csv filename formats`() {
    val backupName = com.example.data.backup.DatabaseBackupManager.generateBackupFileName()
    val csvName = com.example.data.backup.DatabaseBackupManager.generateCsvFileName()

    assertTrue(backupName.startsWith("evalutrack_backup_"))
    assertTrue(backupName.endsWith(".json"))
    assertTrue(csvName.startsWith("evalutrack_gradebook_"))
    assertTrue(csvName.endsWith(".csv"))
  }

  @Test
  fun `verify Room database abstract class singleton pattern and dao access`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db1 = com.example.data.db.AppDatabase.getDatabase(context)
    val db2 = com.example.data.db.AppDatabase.getDatabase(context)
    val db3 = com.example.data.db.AppDatabase.getInstance(context)

    // Verify thread-safe singleton returns identical instance
    org.junit.Assert.assertNotNull(db1)
    org.junit.Assert.assertSame(db1, db2)
    org.junit.Assert.assertSame(db1, db3)

    // Verify DAO access for storing assessment data
    val scoreDao = db1.scoreDao()
    val assessmentDao = db1.assessmentDao()
    val studentScoreDao = db1.studentScoreDao()
    val studentDao = db1.studentDao()

    org.junit.Assert.assertNotNull(scoreDao)
    org.junit.Assert.assertNotNull(assessmentDao)
    org.junit.Assert.assertNotNull(studentScoreDao)
    org.junit.Assert.assertNotNull(studentDao)

    // Verify aliases reference the same underlying DAO
    org.junit.Assert.assertSame(scoreDao, assessmentDao)
    org.junit.Assert.assertSame(scoreDao, studentScoreDao)

    // Verify offline capability helpers
    org.junit.Assert.assertTrue(com.example.data.db.AppDatabase.isOfflineReady())
    val path = com.example.data.db.AppDatabase.getOfflineDatabasePath(context)
    org.junit.Assert.assertTrue(path.contains("evalutrack_database"))

    // Verify data.local compatibility alias
    val localDb = com.example.data.local.ScoreDatabase.getDatabase(context)
    org.junit.Assert.assertSame(db1, localDb)
  }

  @Test
  fun `verify offline storage CRUD operations on assessment data`() = kotlinx.coroutines.test.runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    // Use an in-memory database for isolated offline CRUD verification
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.db.AppDatabase::class.java
    ).allowMainThreadQueries().build()

    val dao = db.assessmentDao()
    val assessment = StudentScore(
      studentName = "Kwame Mensah",
      assessmentName = "End of Term Examination",
      subject = "Mathematics",
      score = 92.5,
      category = "Exam",
      notes = "Outstanding analytical ability"
    )

    val id = dao.insertScore(assessment)
    org.junit.Assert.assertTrue(id > 0)

    val allScores = dao.getAllScoresList()
    org.junit.Assert.assertEquals(1, allScores.size)
    org.junit.Assert.assertEquals("Kwame Mensah", allScores[0].studentName)
    org.junit.Assert.assertEquals("Mathematics", allScores[0].subject)
    org.junit.Assert.assertEquals(92.5, allScores[0].score, 0.01)
    org.junit.Assert.assertEquals("A", allScores[0].letterGrade)

    db.close()
  }
}

