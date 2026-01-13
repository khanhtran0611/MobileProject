package com.example.flashcardapp.LocalDatabase

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "learning_logs")
data class LearningLogs(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    @ColumnInfo(name = "user_id")
    val user_id: String,

    @ColumnInfo(name = "total_learned")
    val total_learned: Int? = null,

    @ColumnInfo(name = "day")
    val day: String? = null
)

