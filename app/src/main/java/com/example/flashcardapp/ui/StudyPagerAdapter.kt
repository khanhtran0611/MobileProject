package com.example.flashcardapp.ui

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.flashcardapp.model.Card

class StudyPagerAdapter(
    fragment: Fragment,
    private val cards: List<Card>
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = cards.size

    override fun createFragment(position: Int): Fragment {
        val card = cards[position]
        return StudyCardFragment.newInstance(
            cardId = card.id ?: -1,
            deckId = card.deck_id,
            front = card.front,
            back = card.back,
            learned = card.learned ?: false,
            learnedToday = card.learned_today ?: false
        )
    }
}
