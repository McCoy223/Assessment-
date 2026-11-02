package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val studentId: String = "",
    val source: String = "Manual", // Manual, Document, Image, Bulk
    val dateAdded: Long = System.currentTimeMillis()
)
