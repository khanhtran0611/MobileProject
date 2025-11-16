package com.example.flashcardapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcardapp.R
import com.example.flashcardapp.model.Deck

/**
 * Adapter for home screen recent decks section.
 * Always renders a single row (deck_card.xml) with left = recent1, right = recent2.
 */
class RecentDecksAdapter(
    private var leftDeck: Deck? = null,
    private var rightDeck: Deck? = null,
    private val onDeckClick: (Deck) -> Unit = {}
) : RecyclerView.Adapter<RecentDecksAdapter.RecentVH>() {

    class RecentVH(view: View) : RecyclerView.ViewHolder(view) {
        val cardLeft: CardView = view.findViewById(R.id.cardLeft)
        val nameLeft: TextView = view.findViewById(R.id.tvDeckNameLeft)
        val cardRight: CardView = view.findViewById(R.id.cardRight)
        val nameRight: TextView = view.findViewById(R.id.tvDeckNameRight)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.deck_card, parent, false)
        return RecentVH(v)
    }

    override fun getItemCount(): Int = 1 // single row

    override fun onBindViewHolder(holder: RecentVH, position: Int) {
        val left = leftDeck
        val right = rightDeck

        if (left != null) {
            holder.cardLeft.visibility = View.VISIBLE
            holder.nameLeft.text = left.name
            holder.cardLeft.setOnClickListener { onDeckClick(left) }
        } else {
            holder.cardLeft.visibility = View.INVISIBLE
        }

        if (right != null) {
            holder.cardRight.visibility = View.VISIBLE
            holder.nameRight.text = right.name
            holder.cardRight.setOnClickListener { onDeckClick(right) }
        } else {
            holder.cardRight.visibility = View.INVISIBLE
        }
    }

    fun update(left: Deck?, right: Deck?) {
        leftDeck = left
        rightDeck = right
        notifyItemChanged(0)
    }
}

