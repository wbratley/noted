package com.noted.ui.checklist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.noted.data.Item
import com.noted.databinding.ItemChecklistRowBinding

class ItemAdapter(
    private val onTick: (Item) -> Unit,
    private val onDelete: (Item) -> Unit,
    private val onEdit: (Item, String) -> Unit
) : ListAdapter<Item, ItemAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemChecklistRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Item) {
            binding.checkbox.setOnCheckedChangeListener(null)
            binding.checkbox.isChecked = item.ticked
            binding.itemText.setText(item.text)

            binding.checkbox.setOnCheckedChangeListener { _, _ -> onTick(item) }
            binding.deleteButton.setOnClickListener { onDelete(item) }
            binding.itemText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val text = binding.itemText.text.toString().trim()
                    if (text.isNotEmpty() && text != item.text) onEdit(item, text)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemChecklistRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Item>() {
            override fun areItemsTheSame(a: Item, b: Item) = a.id == b.id
            override fun areContentsTheSame(a: Item, b: Item) = a == b
        }
    }
}
