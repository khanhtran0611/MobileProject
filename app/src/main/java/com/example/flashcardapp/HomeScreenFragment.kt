package com.example.flashcardapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.flashcardapp.databinding.HomeScreenBinding
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import com.example.flashcardapp.model.Deck
import com.example.flashcardapp.model.User
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.flashcardapp.ui.RecentDecksAdapter

class HomeScreenFragment : Fragment(){
    private var _binding : HomeScreenBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = HomeScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val userId = arguments?.getString(MainLayoutActivity.ARG_USER_ID) ?: UserSession.userId
        val username = arguments?.getString(MainLayoutActivity.ARG_USERNAME) ?: UserSession.username
        Log.d("HomeScreenFragment", "userId=$userId username=$username")
        // Use resources for labels
        binding.stat1Label.text = getString(R.string.stat1_label)
        binding.stat2Label.text = getString(R.string.stat2_label)

        if (userId.isNullOrEmpty()) {
            Log.w("HomeScreenFragment", "No userId provided; showing zeros")
            binding.stat1Number.text = "0"
            binding.stat2Number.text = "0"
            return
        }

        // Setup recent decks recycler view
        val recentAdapter = RecentDecksAdapter(onDeckClick = { deck ->
            Log.d("HomeScreenFragment", "Recent deck clicked: ${deck.name} id=${deck.id}")
            // Optional: navigate to deck detail
        })
        binding.decksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.decksRecyclerView.adapter = recentAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val decks: List<Deck> = SupabaseProvider.client
                    .from("Deck")
                    .select { filter { eq("user_id", userId) } }
                    .decodeList()
                val todayCompleted = decks.count { deck ->
                    val t = deck.total
                    val pt = deck.progress_today
                    (t != null && t > 0 && pt != null && t == pt)
                }
                binding.stat1Number.text = todayCompleted.toString()
            } catch (e: Exception) {
                Log.e("HomeScreenFragment", "Failed loading decks for stats: ${e.message}", e)
                binding.stat1Number.text = "0"
            }

            try {
                val users: List<User> = SupabaseProvider.client
                    .from("User")
                    .select { filter { eq("user_id", userId) } }
                    .decodeList()
                val user = users.firstOrNull()
                val streak = user?.streak ?: 0
                binding.stat2Number.text = streak.toString()
            } catch (e: Exception) {
                Log.e("HomeScreenFragment", "Failed loading user streak: ${e.message}", e)
                binding.stat2Number.text = "0"
            }

            try {
                // Fetch user for recent1/recent2 deck ids
                val users: List<User> = SupabaseProvider.client
                    .from("User")
                    .select { filter { eq("user_id", userId) } }
                    .decodeList()
                val user = users.firstOrNull()
                val recent1Id = user?.recent1
                val recent2Id = user?.recent2
                var deckLeft: Deck? = null
                var deckRight: Deck? = null
                if (recent1Id != null && recent1Id > 0) {
                    deckLeft = SupabaseProvider.client.from("Deck")
                        .select { filter { eq("id", recent1Id) } }
                        .decodeList<Deck>()
                        .firstOrNull()
                }
                if (recent2Id != null && recent2Id > 0) {
                    deckRight = SupabaseProvider.client.from("Deck")
                        .select { filter { eq("id", recent2Id) } }
                        .decodeList<Deck>()
                        .firstOrNull()
                }
                recentAdapter.update(deckLeft, deckRight)
            } catch (e: Exception) {
                Log.e("HomeScreenFragment", "Failed loading recent decks: ${e.message}", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}