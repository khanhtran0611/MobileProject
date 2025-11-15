package com.example.flashcardapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.flashcardapp.databinding.DeckManagementScreenBinding
import com.example.flashcardapp.model.Deck
import com.example.flashcardapp.ui.DeckRowAdapter
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class DeckManagementFragment : Fragment() {
    private var _binding: DeckManagementScreenBinding? = null
    private val binding get() = _binding!!
    private lateinit var deckAdapter: DeckRowAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DeckManagementScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val userId = arguments?.getString(MainLayoutActivity.ARG_USER_ID) ?: UserSession.userId
        val username = arguments?.getString(MainLayoutActivity.ARG_USERNAME) ?: UserSession.username
        Log.d("DeckManagementFragment", "userId=$userId username=$username")

        // RecyclerView setup
        deckAdapter = DeckRowAdapter(emptyList()) { deck ->
            Log.d("DeckManagementFragment", "Deck clicked: ${deck.name} (id=${deck.id})")
            deck.id?.let { id ->
                val intent = Intent(requireContext(), DeckDetailActivity::class.java)
                intent.putExtra(DeckDetailActivity.EXTRA_DECK_ID, id)
                deckDetailLauncher.launch(intent)
            }
        }
        binding.deckRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.deckRecyclerView.adapter = deckAdapter

        // Initial fetch
        fetchDecks(userId)

        // Add deck button
        binding.btnAddDeck.setOnClickListener {
            Log.d("DeckManagementFragment", "Add deck button clicked")
            val userIdToPass = arguments?.getString(MainLayoutActivity.ARG_USER_ID) ?: UserSession.userId
            val intent = Intent(requireContext(), DeckAddingActivity::class.java)
            intent.putExtra(DeckAddingActivity.EXTRA_USER_ID, userIdToPass)
            intent.putExtra(DeckAddingActivity.EXTRA_MODE, DeckAddingActivity.MODE_DECK)
            deckAddLauncher.launch(intent)
        }
    }

    private val deckDetailLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val updated = data?.getBooleanExtra("deck_updated", false) == true
            val deleted = data?.getBooleanExtra("deck_deleted", false) == true
            if (updated || deleted) {
                val userId = arguments?.getString(MainLayoutActivity.ARG_USER_ID) ?: UserSession.userId
                if (!userId.isNullOrEmpty()) fetchDecks(userId)
            }
        }
    }

    private val deckAddLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val created = data?.getBooleanExtra("deck_created", false) == true
            if (created) {
                val userId = arguments?.getString(MainLayoutActivity.ARG_USER_ID) ?: UserSession.userId
                if (!userId.isNullOrEmpty()) fetchDecks(userId)
            }
        }
    }

    private fun fetchDecks(userId: String?) {
        if (userId.isNullOrEmpty()) return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d("DeckManagementFragment", "Fetching decks from Supabase for user_id=$userId")
                val decks: List<Deck> = SupabaseProvider.client
                    .from("Deck")
                    .select { filter { eq("user_id", userId) } }
                    .decodeList<Deck>()
                Log.d("DeckManagementFragment", "Fetched ${decks.size} decks")
                deckAdapter.submitList(decks)
            } catch (e: Exception) {
                Log.e("DeckManagementFragment", "Error fetching decks: ${e.message}", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}