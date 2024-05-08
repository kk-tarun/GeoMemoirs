package com.example.geomemoirs.ui.notes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.geomemoirs.Note
import com.example.geomemoirs.R

class NoteAdapter(private val noteList: List<Note>) : RecyclerView.Adapter<NoteAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewLocation: TextView = itemView.findViewById(R.id.textViewLocation)
        val textViewNote: TextView = itemView.findViewById(R.id.textViewNote)
//        val textViewUserName: TextView = itemView.findViewById(R.id.textViewUserName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = noteList[position]
        val latitudeFormatted = String.format("%.4f", currentItem.latitude)
        val longitudeFormatted = String.format("%.4f", currentItem.longitude)
        holder.textViewLocation.text = "($latitudeFormatted, $longitudeFormatted)"
        holder.textViewNote.text = currentItem.text
//        holder.textViewUserName.text = "${currentItem.name}"
    }

    override fun getItemCount() = noteList.size
}
