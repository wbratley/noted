package com.noted.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.noted.data.NoteWithItems
import com.noted.databinding.ItemNoteBinding

class NoteAdapter(private val onClick: (Long) -> Unit) :
    ListAdapter<NoteWithItems, NoteAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemNoteBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: NoteWithItems) {
            binding.noteName.text = item.note.name
            val total = item.items.size
            val ticked = item.items.count { it.ticked }
            binding.noteCount.text = when {
                total == 0 -> "empty"
                ticked == total -> "done ✓"
                else -> "$ticked / $total"
            }
            binding.root.setOnClickListener { onClick(item.note.id) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    fun getNoteAt(position: Int): NoteWithItems = getItem(position)

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<NoteWithItems>() {
            override fun areItemsTheSame(a: NoteWithItems, b: NoteWithItems) = a.note.id == b.note.id
            override fun areContentsTheSame(a: NoteWithItems, b: NoteWithItems) = a == b
        }
    }
}
