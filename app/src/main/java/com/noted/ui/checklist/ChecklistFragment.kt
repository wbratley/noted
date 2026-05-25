package com.noted.ui.checklist

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.noted.R
import com.noted.data.ApiKeyStore
import com.noted.databinding.FragmentChecklistBinding
import com.noted.viewmodel.ImportState
import com.noted.viewmodel.NoteViewModel

class ChecklistFragment : Fragment() {
    private var _binding: FragmentChecklistBinding? = null
    private val binding get() = _binding!!
    private val vm: NoteViewModel by activityViewModels()
    private val args: ChecklistFragmentArgs by navArgs()
    private var aiPasteItem: MenuItem? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChecklistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)

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

        vm.importState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ImportState.Loading -> aiPasteItem?.isEnabled = false
                is ImportState.Success -> {
                    aiPasteItem?.isEnabled = true
                    Snackbar.make(binding.root, "Added ${state.count} items", Snackbar.LENGTH_SHORT).show()
                    vm.importState.value = ImportState.Idle
                }
                is ImportState.Error -> {
                    aiPasteItem?.isEnabled = true
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    vm.importState.value = ImportState.Idle
                }
                else -> aiPasteItem?.isEnabled = true
            }
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.checklist_menu, menu)
        aiPasteItem = menu.findItem(R.id.action_ai_paste)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.action_ai_paste)?.isVisible = ApiKeyStore(requireContext()).isEnabled()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_ai_paste -> { triggerAiPaste(); true }
            R.id.action_rename -> { binding.noteTitle.requestFocus(); true }
            R.id.action_settings -> {
                findNavController().navigate(ChecklistFragmentDirections.actionChecklistToSettings())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun triggerAiPaste() {
        val clipManager = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipManager.primaryClip

        if (clip == null || clip.itemCount == 0) {
            Snackbar.make(binding.root, getString(R.string.no_clipboard), Snackbar.LENGTH_SHORT).show()
            return
        }

        val clipItem = clip.getItemAt(0)
        val description = clip.description

        // Image in clipboard — pass URI to ViewModel for decoding
        if (description.hasMimeType("image/*") ||
            description.hasMimeType("image/jpeg") ||
            description.hasMimeType("image/png")) {
            val uri = clipItem.uri
            if (uri != null) {
                vm.importFromContent(args.noteId, null, uri)
                return
            }
        }

        // Text fallback
        val text = clipItem.coerceToText(requireContext())?.toString()?.trim()
        if (!text.isNullOrEmpty()) {
            vm.importFromContent(args.noteId, text, null)
        } else {
            Snackbar.make(binding.root, getString(R.string.no_clipboard), Snackbar.LENGTH_SHORT).show()
        }
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
