package com.example.familymap.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.familymap.R
import com.example.familymap.utils.SuopConstants

class SearchResultAdapter(private val items: List<String>) :
    RecyclerView.Adapter<SearchResultViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.search_result, parent, false)
        return SearchResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}

class SearchResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val textViewItem = itemView.findViewById<TextView>(R.id.search_result)

    fun bind(text: String) {
        //Split the data string into its two parts, the text and the icon
        val data = text.split(SuopConstants.STRING_SEPARATOR)
        textViewItem.text = data[0]
        textViewItem.setCompoundDrawablesWithIntrinsicBounds(data[1].toIntOrNull() ?: 0, 0, 0, 0)
    }
}