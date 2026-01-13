package com.example.flashcardapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LearningLogs(
    @SerialName("id")
    val id: Int? = null,

    @SerialName("user_id")
    val user_id: String,

    @SerialName("total_learned")
    val total_learned: Int? = null,

    @SerialName("day")
    val day: String? = null
)

