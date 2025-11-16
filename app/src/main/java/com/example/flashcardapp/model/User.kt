package com.example.flashcardapp.model

//import androidx.room.ColumnInfo
//import androidx.room.Entity
//import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName


@Serializable
data class User (

    @SerialName("id")
    val id: Int? = null,

    @SerialName("username")
    val username: String,

    @SerialName("today")
    val today : Int? = null,

    @SerialName("total_created")
    val total_created : Int? = null,

    @SerialName("total_learned")
    val total_learned : Int? = null,

    @SerialName("recent1")
    val recent1 : Int? = null,

    @SerialName("recent2")
    val recent2 : Int? = null,

    @SerialName("user_id")
    val user_id : String,

    @SerialName("lastime_access")
    val lastime_access : String? = null,

    @SerialName("streak")
    val streak : Int? = null
)
