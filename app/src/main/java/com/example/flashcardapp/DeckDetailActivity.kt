package com.example.flashcardapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.flashcardapp.databinding.DeckDetailsBinding
import com.example.flashcardapp.model.Card
import com.example.flashcardapp.model.Deck
import com.example.flashcardapp.ui.CardRowAdapter
import com.example.flashcardapp.ui.CardRowAdapter.CardSide
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class DeckDetailActivity : AppCompatActivity() {

    private lateinit var binding: DeckDetailsBinding

    companion object {
        private const val TAG = "DeckDetailActivity"
        const val EXTRA_DECK_ID = "extra_deck_id"
    }

    private var editMode: Boolean = false
    private var currentDeck: Deck? = null
    private lateinit var adapter: CardRowAdapter
    private var deckChanged: Boolean = false // track if any field updated

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DeckDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val deckId = intent.getIntExtra(EXTRA_DECK_ID, -1)
        if (deckId == -1) {
            Toast.makeText(this, "Missing deck id", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // RecyclerView setup for cards with cell click support
        adapter = CardRowAdapter(emptyList()) { card, side ->
            if (editMode) {
                when (side) {
                    CardSide.FRONT -> showEditCardDialog(card, isFront = true)
                    CardSide.BACK -> showEditCardDialog(card, isFront = false)
                }
            }
        }
        binding.rvDeckCards.layoutManager = LinearLayoutManager(this)
        binding.rvDeckCards.adapter = adapter

        // Add card button navigates to DeckAddingActivity in card mode (disabled in edit mode)
        binding.btnAdd.setOnClickListener {
            if (editMode) return@setOnClickListener
            val intent = Intent(this, DeckAddingActivity::class.java)
            intent.putExtra(DeckAddingActivity.EXTRA_MODE, DeckAddingActivity.MODE_CARD)
            intent.putExtra(DeckAddingActivity.EXTRA_DECK_ID, deckId)
            startActivity(intent)
        }

        // Edit button toggles edit mode
        binding.btnEdit.setOnClickListener {
            toggleEditMode()
        }

        // Delete button with confirmation (disabled in edit mode)
        binding.btnDelete.setOnClickListener {
            if (editMode) return@setOnClickListener
            confirmAndDeleteDeck()
        }

        // Click on title/description opens edit only in edit mode
        binding.tvDeckNameTop.setOnClickListener {
            if (editMode) showEditTitleDialog()
        }
        binding.tvDeckDescription.setOnClickListener {
            if (editMode) showEditDescriptionDialog()
        }

        // Load deck info and cards
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Fetching deck info for id=$deckId")
                val deckList = SupabaseProvider.client.from("Deck")
                    .select { filter { eq("id", deckId) } }
                    .decodeList<Deck>()
                val deck = deckList.firstOrNull()
                if (deck == null) {
                    Toast.makeText(this@DeckDetailActivity, "Deck not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }
                currentDeck = deck
                // Set title/description
                binding.tvDeckNameTop.text = deck.name
                binding.tvDeckDescription.text = deck.description ?: ""

                Log.d(TAG, "Fetching cards for deck=$deckId")
                val cards = SupabaseProvider.client.from("Card")
                    .select { filter { eq("deck_id", deckId) } }
                    .decodeList<Card>()
                Log.d(TAG, "Fetched ${cards.size} cards")
                adapter.submitList(cards)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading deck details", e)
                Toast.makeText(this@DeckDetailActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Handle system back with dispatcher (instead of deprecated override)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (deckChanged && currentDeck?.id != null) {
                    val resultIntent = Intent().apply {
                        putExtra("deck_updated", true)
                        putExtra("deck_id", currentDeck!!.id!!)
                        putExtra("deck_name", currentDeck!!.name)
                        putExtra("deck_description", currentDeck!!.description ?: "")
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                } else {
                    setResult(Activity.RESULT_CANCELED)
                }
                finish()
            }
        })
    }

    private fun toggleEditMode() {
        editMode = !editMode
        binding.btnAdd.isEnabled = !editMode
        binding.btnDelete.isEnabled = !editMode

        // Provide visual feedback on edit button (optional tint change via alpha)
        binding.btnEdit.alpha = if (editMode) 1.0f else 0.5f

        val msg = if (editMode) "Edit mode ON: tap fields to edit" else "Edit mode OFF"
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun showEditTitleDialog() {
        val deck = currentDeck ?: return
        val input = android.widget.EditText(this).apply {
            setText(deck.name)
            setSelection(text.length)
        }
        AlertDialog.Builder(this)
            .setTitle("Edit deck title")
            .setView(input)
            .setPositiveButton("Save") { dialog, _ ->
                val newTitle = input.text.toString().trim()
                if (newTitle.isNotEmpty() && newTitle != deck.name) {
                    saveDeckField("name", newTitle) {
                        currentDeck = deck.copy(name = newTitle)
                        binding.tvDeckNameTop.text = newTitle
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showEditDescriptionDialog() {
        val deck = currentDeck ?: return
        val input = android.widget.EditText(this).apply {
            setText(deck.description ?: "")
            doOnTextChanged { _, _, _, _ -> }
        }
        AlertDialog.Builder(this)
            .setTitle("Edit description")
            .setView(input)
            .setPositiveButton("Save") { dialog, _ ->
                val newDesc = input.text.toString().trim()
                saveDeckField("description", newDesc) {
                    currentDeck = deck.copy(description = newDesc)
                    binding.tvDeckDescription.text = newDesc
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showEditCardDialog(card: Card, isFront: Boolean) {
        val input = android.widget.EditText(this).apply {
            setText(if (isFront) card.front else card.back)
            setSelection(text.length)
        }
        val title = if (isFront) "Edit front" else "Edit back"
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(input)
            .setPositiveButton("Save") { dialog, _ ->
                val newText = input.text.toString()
                saveCardField(card, isFront, newText)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun saveDeckField(field: String, value: String, onLocalApply: () -> Unit) {
        val deckId = currentDeck?.id ?: return
        lifecycleScope.launch {
            try {
                val body = buildJsonObject { put(field, JsonPrimitive(value)) }
                SupabaseProvider.client.from("Deck").update(
                    body
                ) {
                    filter { eq("id", deckId) }
                }
                Log.d(TAG, "Updated deck $field successfully")
                deckChanged = true // mark change
                onLocalApply()
            } catch (e: Exception) {
                Log.e(TAG, "Failed updating deck $field", e)
                Toast.makeText(this@DeckDetailActivity, "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveCardField(card: Card, isFront: Boolean, newText: String) {
        val id = card.id ?: run {
            Toast.makeText(this, "Card id missing", Toast.LENGTH_SHORT).show()
            return
        }
        val field = if (isFront) "front" else "back"
        lifecycleScope.launch {
            try {
                val body = buildJsonObject { put(field, JsonPrimitive(newText)) }
                SupabaseProvider.client.from("Card").update(
                    body
                ) {
                    filter { eq("id", id) }
                }
                // Refresh list to reflect the latest content
                refreshCards()
            } catch (e: Exception) {
                Log.e(TAG, "Failed updating card ${card.id}", e)
                Toast.makeText(this@DeckDetailActivity, "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun refreshCards() {
        val deckId = currentDeck?.id ?: return
        lifecycleScope.launch {
            try {
                val cards = SupabaseProvider.client.from("Card")
                    .select { filter { eq("deck_id", deckId) } }
                    .decodeList<Card>()
                adapter.submitList(cards)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh cards", e)
            }
        }
    }

    private fun confirmAndDeleteDeck() {
        val deckId = currentDeck?.id ?: run {
            Toast.makeText(this, "Deck id missing", Toast.LENGTH_SHORT).show(); return
        }
        AlertDialog.Builder(this)
            .setTitle("Delete Deck")
            .setMessage("Are you sure you want to delete this deck? All its cards will be removed.")
            .setPositiveButton("Delete") { dialog, _ ->
                dialog.dismiss()
                lifecycleScope.launch {
                    try {
                        Log.d(TAG, "Deleting deck id=$deckId")
                        SupabaseProvider.client.from("Deck").delete {
                            filter { eq("id", deckId) }
                        }
                        Toast.makeText(this@DeckDetailActivity, "Deck deleted", Toast.LENGTH_SHORT).show()
                        // Return deletion result
                        val resultIntent = Intent().apply {
                            putExtra("deck_deleted", true)
                            putExtra("deck_id", deckId)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed deleting deck", e)
                        Toast.makeText(this@DeckDetailActivity, "Delete failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
