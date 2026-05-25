package com.noted.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.noted.databinding.FragmentHomeBinding
import com.noted.viewmodel.NoteViewModel
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val vm: NoteViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = NoteAdapter { noteId ->
            findNavController().navigate(HomeFragmentDirections.actionHomeToChecklist(noteId))
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                vm.deleteNote(adapter.getNoteAt(vh.adapterPosition).note)
            }
        }).attachToRecyclerView(binding.recyclerView)

        vm.allNotesWithItems.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.emptyState.visibility = if (list.isNullOrEmpty()) View.VISIBLE else View.GONE
        }

        vm.pendingShare.observe(viewLifecycleOwner) { share ->
            if (share == null) return@observe
            vm.pendingShare.value = null

            viewLifecycleOwner.lifecycleScope.launch {
                val noteId = vm.createAndGetNoteId("New list")
                findNavController().navigate(HomeFragmentDirections.actionHomeToChecklist(noteId))
                // Import runs in viewModelScope — survives fragment navigation
                vm.importFromContent(noteId, share.text, share.imageUri)
            }
        }

        binding.fab.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val noteId = vm.createAndGetNoteId("New list")
                findNavController().navigate(HomeFragmentDirections.actionHomeToChecklist(noteId))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
