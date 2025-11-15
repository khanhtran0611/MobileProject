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
 * Adapter that displays decks in rows of 2 using deck_card.xml
 * The data source is a flat list of Decks from Supabase.
 */
class DeckRowAdapter(
    private var items: List<Deck>,
    private val onDeckClick: (Deck) -> Unit = {}
) : RecyclerView.Adapter<DeckRowAdapter.RowVH>() {

    class RowVH(view: View) : RecyclerView.ViewHolder(view) {
        val cardLeft: CardView = view.findViewById(R.id.cardLeft)
        val nameLeft: TextView = view.findViewById(R.id.tvDeckNameLeft)
        val cardRight: CardView = view.findViewById(R.id.cardRight)
        val nameRight: TextView = view.findViewById(R.id.tvDeckNameRight)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.deck_card, parent, false)
        return RowVH(v)
    }

    override fun getItemCount(): Int {
        // number of rows = ceil(items.size / 2.0)
        return (items.size + 1) / 2
    }

    override fun onBindViewHolder(holder: RowVH, position: Int) {
        val leftIndex = position * 2
        val rightIndex = leftIndex + 1

        val left = items.getOrNull(leftIndex)
        val right = items.getOrNull(rightIndex)

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
            // If odd count, hide right card to keep spacing consistent
            holder.cardRight.visibility = View.INVISIBLE
        }
    }

    fun submitList(newItems: List<Deck>) {
        items = newItems
        notifyDataSetChanged()
    }
}

