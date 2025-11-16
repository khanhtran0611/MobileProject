package com.example.flashcardapp.model
//import androidx.room.ColumnInfo
//import androidx.room.Entity
//import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Card(

    @SerialName("id")
    val id : Int? = null,

    @SerialName("front")
    val front : String,

    @SerialName("back")
    val back : String,

    @SerialName("deck_id")
    val deck_id : Int,

    @SerialName("learned")
    val learned : Boolean? = null,

    @SerialName("learned_today")
    val learned_today : Boolean? = null
)
