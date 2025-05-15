package com.example.stockapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecentAdapter(private val stocks: List<String>, private val onItemClick: (String) -> Unit) : RecyclerView.Adapter<RecentAdapter.RecentViewHolder>() {

    class RecentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSymbol: TextView = view.findViewById(R.id.symbol)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recent_item, parent, false)
        return RecentViewHolder(view)
    }

    override fun getItemCount(): Int = stocks.size

    override fun onBindViewHolder(holder: RecentViewHolder, position: Int) {
        val stock = stocks[position]
        holder.tvSymbol.text = stock
        holder.itemView.setOnClickListener {
            onItemClick(stock)
        }
    }
}
