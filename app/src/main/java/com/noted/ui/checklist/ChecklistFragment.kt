package com.noted.ui.checklist

import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
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
            if (!binding.noteTitle.isFocused) {
                binding.noteTitle.setText(nwi.note.name)
            }
            adapter.submitList(nwi.items.sortedBy { it.id })
        }

        binding.noteTitle.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.noteTitle.clearFocus()
                true
            } else false
        }

        binding.noteTitle.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) saveTitle()
        }

        binding.addItemInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { addItem(); true } else false
        }

        binding.addItemInput.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN &&
                (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_TAB)) {
                addItem(); true
            } else false
        }

        binding.addItemInput.postDelayed({
            binding.addItemInput.requestFocus()
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.addItemInput, InputMethodManager.SHOW_IMPLICIT)
        }, 300)
    }

    private fun addItem() {
        val text = binding.addItemInput.text.toString().trim()
        if (text.isEmpty()) return

        val nwi = vm.getNoteWithItems(args.noteId).value
        if (nwi != null && nwi.items.isEmpty() && nwi.note.name == "New list") {
            vm.renameNote(nwi.note, text)
        }

        vm.addItem(args.noteId, text)
        binding.addItemInput.text?.clear()
    }

    private fun saveTitle() {
        val newName = binding.noteTitle.text.toString().trim()
        val note = vm.getNoteWithItems(args.noteId).value?.note
        if (newName.isNotEmpty() && note != null && newName != note.name) {
            vm.renameNote(note, newName)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
