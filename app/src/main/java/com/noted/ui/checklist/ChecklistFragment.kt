package com.noted.ui.checklist

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.noted.R
import com.noted.databinding.FragmentChecklistBinding
import com.noted.viewmodel.NoteViewModel

class ChecklistFragment : Fragment() {
    private var _binding: FragmentChecklistBinding? = null
    private val binding get() = _binding!!
    private val vm: NoteViewModel by activityViewModels()
    private val args: ChecklistFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChecklistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = ItemAdapter(
            onTick = { vm.toggleItem(it) },
            onDelete = { vm.deleteItem(it) },
            onEdit = { item, text -> vm.updateItemText(item, text) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        vm.getNoteWithItems(args.noteId).observe(viewLifecycleOwner) { nwi ->
            requireActivity().title = nwi.note.name
            adapter.submitList(nwi.items.sortedBy { it.id })
        }

        binding.addItemButton.setOnClickListener {
            val text = binding.addItemInput.text.toString().trim()
            if (text.isNotEmpty()) {
                vm.addItem(args.noteId, text)
                binding.addItemInput.text?.clear()
            }
        }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.checklist_menu, menu)
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == R.id.action_rename) {
                    showRenameDialog()
                    return true
                }
                return false
            }
        }, viewLifecycleOwner)
    }

    private fun showRenameDialog() {
        val note = vm.getNoteWithItems(args.noteId).value?.note ?: return
        val input = EditText(requireContext()).apply {
            setText(note.name)
            setPadding(48, 16, 48, 16)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Rename")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) vm.renameNote(note, name)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
