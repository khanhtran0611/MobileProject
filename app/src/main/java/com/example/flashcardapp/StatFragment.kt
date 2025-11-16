package com.example.flashcardapp

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import com.example.flashcardapp.databinding.StatScreenBinding
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import com.example.flashcardapp.model.Deck
import com.example.flashcardapp.model.User
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.flashcardapp.ui.DeckStatsAdapter

class StatFragment : Fragment(){
    private var _binding: StatScreenBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View? {
        _binding = StatScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val userId = arguments?.getString(MainLayoutActivity.ARG_USER_ID) ?: UserSession.userId
        val username = arguments?.getString(MainLayoutActivity.ARG_USERNAME) ?: UserSession.username
        Log.d("StatFragment", "userId=$userId username=$username")

        // Set labels per requirement using string resources
        binding.statALabel.text = getString(R.string.statA_label)
        binding.statBLabel.text = getString(R.string.statB_label)

        if (userId.isNullOrEmpty()) {
            binding.statAValue.text = "0"
            binding.statBValue.text = "0"
            return
        }

        // Setup RecyclerView for deck stats
        val statsAdapter = DeckStatsAdapter()
        binding.rvStatsList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStatsList.adapter = statsAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val decks: List<Deck> = SupabaseProvider.client
                    .from("Deck")
                    .select { filter { eq("user_id", userId) } }
                    .decodeList()
                val totalCompleted = decks.count { d ->
                    val t = d.total
                    val p = d.progress
                    (t != null && t > 0 && p != null && t == p)
                }
                binding.statAValue.text = totalCompleted.toString()
                // Submit decks to adapter for list display
                statsAdapter.submitList(decks)
            } catch (e: Exception) {
                Log.e("StatFragment", "Failed computing total completed: ${e.message}", e)
                binding.statAValue.text = "0"
            }

            try {
                val users: List<User> = SupabaseProvider.client
                    .from("User")
                    .select { filter { eq("user_id", userId) } }
                    .decodeList()
                val totalDecks = users.firstOrNull()?.total_created ?: 0
                binding.statBValue.text = totalDecks.toString()
            } catch (e: Exception) {
                Log.e("StatFragment", "Failed loading total_created: ${e.message}", e)
                binding.statBValue.text = "0"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}