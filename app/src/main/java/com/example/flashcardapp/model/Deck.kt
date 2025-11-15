package com.example.flashcardapp.model
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Deck(

    @SerialName("id")
    val id : Int? = null,

    @SerialName("user_id")
    val user_id : String, // changed from Int to String to match Supabase auth user id (UUID)

    @SerialName("progress")
    val progress : Int? = null,

    @SerialName("description")
    val description : String? = null,

    @SerialName("name")
    val name : String,

    @SerialName("total")
    val total : Int? = null
)
