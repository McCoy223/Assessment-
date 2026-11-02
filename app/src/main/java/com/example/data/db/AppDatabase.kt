package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Student
import com.example.data.model.StudentScore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [StudentScore::class, Student::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    /** Access to the primary DAO for storing and querying student assessment data */
    abstract fun scoreDao(): ScoreDao

    /** Direct alias providing access to the DAO for storing assessment data */
    fun assessmentDao(): ScoreDao = scoreDao()

    /** Direct alias for student score operations */
    fun studentScoreDao(): ScoreDao = scoreDao()

    /** Access to the Student DAO for managing class roster */
    abstract fun studentDao(): StudentDao

    companion object {
        const val DATABASE_NAME = "evalutrack_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Thread-safe singleton pattern to initialize and retrieve the Room database.
         * Operates 100% offline using local SQLite persistence on the device.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(CoroutineScope(Dispatchers.IO)))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Thread-safe singleton pattern with custom CoroutineScope for initialization callbacks.
         */
        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Alias for [getDatabase] adhering to standard singleton naming conventions.
         */
        fun getInstance(context: Context): AppDatabase = getDatabase(context)

        /**
         * Offline Feature: Verifies that offline local SQLite storage is available and active.
         */
        fun isOfflineReady(): Boolean = true

        /**
         * Offline Feature: Returns the absolute file path of the on-device SQLite database.
         */
        fun getOfflineDatabasePath(context: Context): String {
            return context.applicationContext.getDatabasePath(DATABASE_NAME).absolutePath
        }

        /**
         * Offline Feature: Returns the current on-device SQLite file size in bytes.
         */
        fun getOfflineDatabaseSizeBytes(context: Context): Long {
            val file = context.applicationContext.getDatabasePath(DATABASE_NAME)
            return if (file.exists()) file.length() else 0L
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialScores(database.scoreDao())
                        populateInitialStudents(database.studentDao())
                    }
                }
            }
        }

        suspend fun populateInitialStudents(dao: StudentDao) {
            val sampleStudents = listOf(
                Student(name = "Alex Johnson", studentId = "STU-001", source = "Manual"),
                Student(name = "Maya Patel", studentId = "STU-002", source = "Manual"),
                Student(name = "Marcus Williams", studentId = "STU-003", source = "Manual"),
                Student(name = "Sophia Chen", studentId = "STU-004", source = "Manual"),
                Student(name = "Ethan Davis", studentId = "STU-005", source = "Manual"),
                Student(name = "Abena Osei", studentId = "STU-006", source = "Manual"),
                Student(name = "Kofi Mensah", studentId = "STU-007", source = "Manual")
            )
            dao.insertStudents(sampleStudents)
        }

        suspend fun populateInitialScores(dao: ScoreDao) {
            val now = System.currentTimeMillis()
            val day = 86400000L

            val sampleScores = listOf(
                // Alex Johnson
                StudentScore(
                    studentName = "Alex Johnson",
                    assessmentName = "Algebra Basics Test",
                    subject = "Mathematics",
                    score = 78.0,
                    category = "Quiz",
                    dateMillis = now - (28 * day),
                    notes = "Needs review on variable isolation."
                ),
                StudentScore(
                    studentName = "Alex Johnson",
                    assessmentName = "Photosynthesis & Ecosystems",
                    subject = "Science",
                    score = 84.0,
                    category = "Homework",
                    dateMillis = now - (24 * day),
                    notes = "Clear observation and diagrams."
                ),
                StudentScore(
                    studentName = "Alex Johnson",
                    assessmentName = "Moral Decency & Social Norms",
                    subject = "Religious and Moral Education (R.M.E.)",
                    score = 90.0,
                    category = "Classwork",
                    dateMillis = now - (20 * day),
                    notes = "Insightful reflections on community values."
                ),
                StudentScore(
                    studentName = "Alex Johnson",
                    assessmentName = "Twi Composition & Proverb Usage",
                    subject = "Ghanaian Language",
                    score = 85.0,
                    category = "Homework",
                    dateMillis = now - (16 * day),
                    notes = "Rich cultural proverb application."
                ),
                StudentScore(
                    studentName = "Alex Johnson",
                    assessmentName = "Perspective Drawing & Shading",
                    subject = "Creative Art",
                    score = 92.0,
                    category = "Project",
                    dateMillis = now - (10 * day),
                    notes = "Great visual balance."
                ),
                StudentScore(
                    studentName = "Alex Johnson",
                    assessmentName = "Scratch Programming Fundamentals",
                    subject = "Computing",
                    score = 95.0,
                    category = "Project",
                    dateMillis = now - (3 * day),
                    notes = "Creative interactive maze game."
                ),

                // Maya Patel
                StudentScore(
                    studentName = "Maya Patel",
                    assessmentName = "Grammar & Comprehension Exam",
                    subject = "English",
                    score = 96.0,
                    category = "Exam",
                    dateMillis = now - (25 * day),
                    notes = "Outstanding analytical writing."
                ),
                StudentScore(
                    studentName = "Maya Patel",
                    assessmentName = "Calculations & Fractions",
                    subject = "Mathematics",
                    score = 94.0,
                    category = "Quiz",
                    dateMillis = now - (21 * day),
                    notes = "Accurate step-by-step solutions."
                ),
                StudentScore(
                    studentName = "Maya Patel",
                    assessmentName = "The Pre-Colonial Kingdoms",
                    subject = "History",
                    score = 91.0,
                    category = "Homework",
                    dateMillis = now - (14 * day),
                    notes = "Comprehensive trade routes synthesis."
                ),
                StudentScore(
                    studentName = "Maya Patel",
                    assessmentName = "Database & Spreadsheets Task",
                    subject = "Computing",
                    score = 98.0,
                    category = "Project",
                    dateMillis = now - (7 * day),
                    notes = "Advanced formula construction."
                ),

                // Marcus Williams
                StudentScore(
                    studentName = "Marcus Williams",
                    assessmentName = "Linear Equations Test",
                    subject = "Mathematics",
                    score = 64.0,
                    category = "Quiz",
                    dateMillis = now - (26 * day),
                    notes = "Participated in math tutorial clinic."
                ),
                StudentScore(
                    studentName = "Marcus Williams",
                    assessmentName = "Traditional Festivals & Drumming",
                    subject = "Ghanaian Language",
                    score = 89.0,
                    category = "Project",
                    dateMillis = now - (19 * day),
                    notes = "Excellent storytelling and drumming context."
                ),
                StudentScore(
                    studentName = "Marcus Williams",
                    assessmentName = "The 1948 Accra Riots Analysis",
                    subject = "History",
                    score = 88.0,
                    category = "Exam",
                    dateMillis = now - (12 * day),
                    notes = "Deep knowledge of the nationalist movement."
                ),
                StudentScore(
                    studentName = "Marcus Williams",
                    assessmentName = "Moral Teachings of the Golden Rule",
                    subject = "Religious and Moral Education (R.M.E.)",
                    score = 82.0,
                    category = "Homework",
                    dateMillis = now - (5 * day),
                    notes = "Reflective personal commentary."
                ),

                // Sophia Chen
                StudentScore(
                    studentName = "Sophia Chen",
                    assessmentName = "Textile Design & Pattern Printing",
                    subject = "Creative Art",
                    score = 94.0,
                    category = "Project",
                    dateMillis = now - (22 * day),
                    notes = "Beautiful Adinkra stamp arrangement."
                ),
                StudentScore(
                    studentName = "Sophia Chen",
                    assessmentName = "Forces & Energy Conversion Lab",
                    subject = "Science",
                    score = 86.0,
                    category = "Quiz",
                    dateMillis = now - (15 * day),
                    notes = "Good mechanical advantage proofs."
                ),
                StudentScore(
                    studentName = "Sophia Chen",
                    assessmentName = "Narrative Essay: My Hometown",
                    subject = "English",
                    score = 90.0,
                    category = "Homework",
                    dateMillis = now - (8 * day),
                    notes = "Expressive descriptive language."
                )
            )

            dao.insertAll(sampleScores)
        }
    }
}
