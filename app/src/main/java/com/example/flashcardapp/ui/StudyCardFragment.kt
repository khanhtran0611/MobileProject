package com.example.flashcardapp.ui

import android.animation.Animator
import android.animation.AnimatorInflater
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.flashcardapp.R
import com.example.flashcardapp.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonPrimitive
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class StudyCardFragment : Fragment() {

    companion object {
        private const val ARG_CARD_ID = "arg_card_id"
        private const val ARG_DECK_ID = "arg_deck_id"
        private const val ARG_FRONT = "arg_front"
        private const val ARG_BACK = "arg_back"
        private const val ARG_LEARNED = "arg_learned"
        private const val ARG_LEARNED_TODAY = "arg_learned_today"
        private const val TAG = "StudyCardFragment"

        fun newInstance(
            cardId: Int,
            deckId: Int,
            front: String,
            back: String,
            learned: Boolean,
            learnedToday: Boolean
        ): StudyCardFragment = StudyCardFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_CARD_ID, cardId)
                putInt(ARG_DECK_ID, deckId)
                putString(ARG_FRONT, front)
                putString(ARG_BACK, back)
                putBoolean(ARG_LEARNED, learned)
                putBoolean(ARG_LEARNED_TODAY, learnedToday)
            }
        }
    }

    private var isFrontVisible = true
    private var updatedFlagsThisSession = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.item_study_card, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val front = arguments?.getString(ARG_FRONT).orEmpty()
        val back = arguments?.getString(ARG_BACK).orEmpty()
        val cardId = arguments?.getInt(ARG_CARD_ID) ?: -1
        val deckId = arguments?.getInt(ARG_DECK_ID) ?: -1
        val alreadyLearned = arguments?.getBoolean(ARG_LEARNED) ?: false
        val alreadyLearnedToday = arguments?.getBoolean(ARG_LEARNED_TODAY) ?: false

        val card = view as CardView
        // Improve 3D flip look
        card.cameraDistance = 8000f

        val tvFront = view.findViewById<TextView>(R.id.tvFrontText)
        val tvBack = view.findViewById<TextView>(R.id.tvBackText)
        tvFront.text = if (front.isBlank()) "(front empty)" else front
        tvBack.text = if (back.isBlank()) "(back empty)" else back

        // Prepare simple flip animation (use Animator, not AnimatorSet)
        val flipOut: Animator = AnimatorInflater.loadAnimator(requireContext(), R.animator.flip_out)
        val flipIn: Animator = AnimatorInflater.loadAnimator(requireContext(), R.animator.flip_in)
        val interpolator = AccelerateDecelerateInterpolator()
        flipOut.interpolator = interpolator
        flipIn.interpolator = interpolator

        card.setOnClickListener {
            val flippingToBack = isFrontVisible

            if (isFrontVisible) {
                flipOut.setTarget(tvFront)
                flipIn.setTarget(tvBack)
                tvBack.visibility = View.VISIBLE
                flipOut.start()
                flipIn.start()
                tvFront.visibility = View.GONE
            } else {
                flipOut.setTarget(tvBack)
                flipIn.setTarget(tvFront)
                tvFront.visibility = View.VISIBLE
                flipOut.start()
                flipIn.start()
                tvBack.visibility = View.GONE
            }
            isFrontVisible = !isFrontVisible

            // When flipping to the back side for the first time, mark learned & learned_today
            if (flippingToBack && !updatedFlagsThisSession) {
                // Only update if we have a valid id and at least one flag is false
                if (cardId > 0 && (!alreadyLearned || !alreadyLearnedToday)) {
                    updatedFlagsThisSession = true
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            val payload = buildJsonObject {
                                if (!alreadyLearned) put("learned", JsonPrimitive(true))
                                if (!alreadyLearnedToday) put("learned_today", JsonPrimitive(true))
                            }
                            if (payload.isEmpty()) {
                                Log.d(TAG, "No flag changes needed for cardId=$cardId")
                            } else {
                                Log.d(TAG, "Updating cardId=$cardId with $payload")
                                SupabaseProvider.client.from("Card").update(payload) {
                                    filter { eq("id", cardId) }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to update learned flags for cardId=$cardId: ${e.message}", e)
                            // Allow retry if update failed
                            updatedFlagsThisSession = false
                        }
                    }
                } else {
                    Log.d(TAG, "Skip update: invalid card id or already learned flags (cardId=$cardId, learned=$alreadyLearned, learned_today=$alreadyLearnedToday)")
                }
            }
        }
    }
}
