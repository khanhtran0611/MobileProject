package com.example.flashcardapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcardapp.R
import com.example.flashcardapp.model.Card

/**
 * Adapter that displays ONE card per row using deck_card.xml with two cells:
 * - Left cell shows the FRONT text
 * - Right cell shows the BACK text
 */
class CardRowAdapter(
    private var items: List<Card>,
    private val onCellClick: (card: Card, side: CardSide) -> Unit = { _, _ -> },
    private val onCellLongClick: (card: Card, side: CardSide) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<CardRowAdapter.RowVH>() {

    enum class CardSide { FRONT, BACK }

    class RowVH(view: View) : RecyclerView.ViewHolder(view) {
        val cardLeft: CardView = view.findViewById(R.id.cardLeft)
        val textLeft: TextView = view.findViewById(R.id.tvDeckNameLeft)
        val cardRight: CardView = view.findViewById(R.id.cardRight)
        val textRight: TextView = view.findViewById(R.id.tvDeckNameRight)
        // Hidden card id views
        val cardIdLeft: TextView = view.findViewById(R.id.tvCardIdLeft)
        val cardIdRight: TextView = view.findViewById(R.id.tvCardIdRight)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.deck_card, parent, false)
        return RowVH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RowVH, position: Int) {
        val card = items[position]
        holder.cardLeft.visibility = View.VISIBLE
        holder.cardRight.visibility = View.VISIBLE

        holder.textLeft.text = card.front.ifBlank { "(front is empty)" }
        holder.textRight.text = card.back.ifBlank { "(back is empty)" }

        // Bind ids to hidden views and also as tags for convenience
        val idString = card.id?.toString() ?: ""
        holder.cardIdLeft.text = idString
        holder.cardIdRight.text = idString
        holder.cardLeft.tag = card.id
        holder.cardRight.tag = card.id

        holder.cardLeft.setOnClickListener { onCellClick(card, CardSide.FRONT) }
        holder.cardRight.setOnClickListener { onCellClick(card, CardSide.BACK) }

        holder.cardLeft.setOnLongClickListener {
            onCellLongClick(card, CardSide.FRONT)
            true
        }
        holder.cardRight.setOnLongClickListener {
            onCellLongClick(card, CardSide.BACK)
            true
        }
    }

    fun submitList(newItems: List<Card>) {
        items = newItems
        notifyDataSetChanged()
    }
}
