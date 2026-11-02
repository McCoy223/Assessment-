package com.example.data.local

import com.example.data.db.AppDatabase
import com.example.data.db.ScoreDao
import com.example.data.db.StudentDao

/**
 * [ScoreDatabase] provides access to the Room database for storing assessment data.
 * Aliased to [AppDatabase] to maintain full compatibility across local data packages
 * while delivering 100% offline device-local SQLite storage.
 */
typealias ScoreDatabase = AppDatabase

/**
 * [StudentScoreDao] provides access to student assessment score operations.
 * Aliased to [ScoreDao].
 */
typealias StudentScoreDao = ScoreDao
