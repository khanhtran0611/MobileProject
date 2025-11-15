package com.example.flashcardapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import com.example.flashcardapp.databinding.DeckAddInfoBinding
import com.example.flashcardapp.model.Card
import com.example.flashcardapp.model.Deck
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class DeckAddingActivity : AppCompatActivity() {

    private lateinit var binding: DeckAddInfoBinding

    companion object {
        private const val TAG = "DeckAddingActivity"
        const val EXTRA_USER_ID = "extra_user_id"          // Provided when adding a deck
        const val EXTRA_MODE = "extra_mode"                // "deck" or "card"
        const val EXTRA_DECK_ID = "extra_deck_id"          // Provided when adding a card
        const val MODE_DECK = "deck"
        const val MODE_CARD = "card"
    }

    private var currentMode: String = MODE_DECK
    private var userId: String? = null
    private var deckIdForCard: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DeckAddInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        readIntentExtras()
        setupUiForMode()
        setupListeners()
    }

    private fun readIntentExtras() {
        currentMode = intent.getStringExtra(EXTRA_MODE) ?: MODE_DECK
        userId = intent.getStringExtra(EXTRA_USER_ID) ?: UserSession.userId
        deckIdForCard = intent.getIntExtra(EXTRA_DECK_ID, -1).takeIf { it != -1 }

        Log.d(TAG, "Mode=$currentMode userId=$userId deckIdForCard=$deckIdForCard")
    }

    private fun setupUiForMode() {
        if (currentMode == MODE_DECK) {
            // Show deck inputs
            binding.tvAddDeckTitle.visibility = View.VISIBLE
            binding.etDeckName.visibility = View.VISIBLE
            binding.etDeckDescription.visibility = View.VISIBLE
            binding.btnCreateDeck.visibility = View.VISIBLE
            // Hide card inputs
            binding.tvAddCardTitle.visibility = View.GONE
            binding.etFrontSide.visibility = View.GONE
            binding.etBackSide.visibility = View.GONE
            binding.btnConfirmAdd.visibility = View.GONE
        } else {
            // Adding a card
            binding.tvAddDeckTitle.visibility = View.GONE
            binding.etDeckName.visibility = View.GONE
            binding.etDeckDescription.visibility = View.GONE
            binding.btnCreateDeck.visibility = View.GONE
            binding.tvAddCardTitle.visibility = View.VISIBLE
            binding.etFrontSide.visibility = View.VISIBLE
            binding.etBackSide.visibility = View.VISIBLE
            binding.btnConfirmAdd.visibility = View.VISIBLE
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnCreateDeck.setOnClickListener {
            if (currentMode != MODE_DECK) return@setOnClickListener
            val name = binding.etDeckName.text?.toString()?.trim() ?: ""
            val description = binding.etDeckDescription.text?.toString()?.trim()?.takeIf { it.isNotBlank() }

            if (name.isEmpty()) {
                toast("Please enter deck name")
                return@setOnClickListener
            }
            val uid = userId
            if (uid.isNullOrEmpty()) {
                toast("Missing user id")
                return@setOnClickListener
            }
            hideKeyboard()
            lifecycleScope.launch {
                try {
                    Log.d(TAG, "Creating deck: name=$name user_id=$uid")
                    val newDeck = Deck(
                        id = null,
                        user_id = uid,
                        progress = 0,
                        description = description,
                        name = name,
                        total = 0
                    )
                    // Request returning representation so we get id without extra query
                    val inserted = SupabaseProvider.client.from("Deck").insert(newDeck) { select() }.decodeSingle<Deck>()
                    Log.d(TAG, "Deck created successfully id=${inserted.id}")
                    toast("Deck created")
                    val result = android.content.Intent().apply {
                        putExtra("deck_created", true)
                        putExtra("deck_id", inserted.id ?: -1)
                        putExtra("deck_name", inserted.name)
                        putExtra("deck_description", inserted.description ?: "")
                    }
                    setResult(RESULT_OK, result)
                    finish()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create deck", e)
                    toast("Error: ${e.message}")
                }
            }
        }

        binding.btnConfirmAdd.setOnClickListener {
            if (currentMode != MODE_CARD) return@setOnClickListener
            val front = binding.etFrontSide.text?.toString()?.trim() ?: ""
            val back = binding.etBackSide.text?.toString()?.trim() ?: ""
            val deckId = deckIdForCard

            if (front.isEmpty() || back.isEmpty()) {
                toast("Please fill both sides")
                return@setOnClickListener
            }
            if (deckId == null) {
                toast("Missing deck id")
                return@setOnClickListener
            }
            hideKeyboard()
            lifecycleScope.launch {
                try {
                    Log.d(TAG, "Creating card for deck=$deckId front='$front' back='$back'")
                    val newCard = Card(
                        id = null,
                        front = front,
                        back = back,
                        deck_id = deckId,
                        learned = false
                    )
                    SupabaseProvider.client.from("Card").insert(newCard)
                    Log.d(TAG, "Card created successfully")
                    toast("Card added")
                    setResult(RESULT_OK)
                    finish()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create card", e)
                    toast("Error: ${e.message}")
                }
            }
        }
    }

    private fun hideKeyboard() {
        try {
            val imm = getSystemService<InputMethodManager>()
            currentFocus?.let { view -> imm?.hideSoftInputFromWindow(view.windowToken, 0) }
        } catch (e: Exception) {
            Log.w(TAG, "hideKeyboard failed: ${e.message}")
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
