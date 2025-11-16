package com.example.flashcardapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.flashcardapp.databinding.StudyScreenBinding
import com.example.flashcardapp.model.Card
import com.example.flashcardapp.ui.StudyPagerAdapter
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class StudyScreenFragment : Fragment() {

    private var _binding: StudyScreenBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val TAG = "StudyScreenFragment"
        const val ARG_DECK_ID = "arg_deck_id"

        fun newInstance(deckId: Int): StudyScreenFragment = StudyScreenFragment().apply {
            arguments = Bundle().apply { putInt(ARG_DECK_ID, deckId) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = StudyScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val deckId = arguments?.getInt(ARG_DECK_ID, -1) ?: -1
        if (deckId == -1) {
            Log.e(TAG, "Missing deck id in arguments")
            return
        }

        // Fetch cards for this deck and set up the pager
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d(TAG, "Fetching cards for deck=$deckId")
                val cards = SupabaseProvider.client
                    .from("Card")
                    .select {
                        filter { eq("deck_id", deckId) }
                    }
                    .decodeList<Card>()
                Log.d(TAG, "Fetched ${cards.size} cards for study")

                val adapter = StudyPagerAdapter(this@StudyScreenFragment, cards)
                binding.vpStudyCards.adapter = adapter
                binding.vpStudyCards.offscreenPageLimit = 1
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load cards: ${e.message}", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}