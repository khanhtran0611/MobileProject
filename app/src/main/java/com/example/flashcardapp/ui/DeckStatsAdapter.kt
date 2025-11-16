package com.example.flashcardapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcardapp.R
import com.example.flashcardapp.model.Deck

/**
 * Adapter for statistics screen that shows one deck per card using deck_stat_card.xml
 */
class DeckStatsAdapter(
    private var items: List<Deck> = emptyList()
) : RecyclerView.Adapter<DeckStatsAdapter.StatVH>() {

    class StatVH(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvStatDeckName)
        val percent: TextView = view.findViewById(R.id.tvDeckProgressPercent)
        val progressBar: ProgressBar = view.findViewById(R.id.progressDeck)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.deck_stat_card, parent, false)
        return StatVH(v)
    }

    override fun getItemCount(): Int = items.size

    @Suppress("SetTextI18n")
    override fun onBindViewHolder(holder: StatVH, position: Int) {
        val deck = items[position]
        holder.name.text = deck.name
        val total = deck.total ?: 0
        val progress = deck.progress ?: 0
        if (total > 0) {
            val pct = (progress * 100.0 / total).coerceIn(0.0, 100.0).toInt()
            holder.percent.text = "$pct%"
            holder.progressBar.max = total
            holder.progressBar.progress = progress.coerceAtMost(total)
        } else {
            holder.percent.text = "0%"
            holder.progressBar.max = 1
            holder.progressBar.progress = 0
        }
    }

    fun submitList(newItems: List<Deck>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = items.size
            override fun getNewListSize(): Int = newItems.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items[oldItemPosition].id == newItems[newItemPosition].id
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val o = items[oldItemPosition]
                val n = newItems[newItemPosition]
                return o.name == n.name && o.progress == n.progress && o.total == n.total && o.progress_today == n.progress_today
            }
        })
        items = newItems
        diff.dispatchUpdatesTo(this)
    }
}
