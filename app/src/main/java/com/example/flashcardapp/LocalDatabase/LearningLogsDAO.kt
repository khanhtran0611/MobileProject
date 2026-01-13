package com.example.flashcardapp.LocalDatabase

import androidx.room.*

@Dao
interface LearningLogsDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(learningLogs: LearningLogs)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(learningLogs: List<LearningLogs>)

    @Update
    suspend fun update(learningLogs: LearningLogs)

    @Delete
    suspend fun delete(learningLogs: LearningLogs)

    @Query("SELECT * FROM learning_logs WHERE user_id = :userId ORDER BY day DESC")
    suspend fun getLearningLogsByUserId(userId: String): List<LearningLogs>

    @Query("SELECT * FROM learning_logs WHERE user_id = :userId AND day = :day LIMIT 1")
    suspend fun getLearningLogByUserIdAndDay(userId: String, day: String): LearningLogs?

    @Query("DELETE FROM learning_logs WHERE user_id = :userId")
    suspend fun deleteAllByUserId(userId: String)

    @Query("DELETE FROM learning_logs")
    suspend fun deleteAll()

    @Query("SELECT * FROM learning_logs")
    suspend fun getAllLearningLogs(): List<LearningLogs>
}

