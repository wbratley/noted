package com.noted.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.noted.data.Item
import com.noted.data.Note
import com.noted.data.NoteDatabase
import com.noted.data.NoteRepository
import com.noted.data.NoteWithItems
import kotlinx.coroutines.launch

class NoteViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = NoteRepository(NoteDatabase.getDatabase(app).noteDao())

    val allNotesWithItems: LiveData<List<NoteWithItems>> = repo.allNotesWithItems

    private val noteCache = mutableMapOf<Long, LiveData<NoteWithItems>>()
    fun getNoteWithItems(id: Long): LiveData<NoteWithItems> =
        noteCache.getOrPut(id) { repo.getNoteWithItems(id) }

    fun createNote(name: String) = viewModelScope.launch { repo.createNote(name) }

    suspend fun createAndGetNoteId(name: String): Long = repo.createNote(name)

    fun renameNote(note: Note, newName: String) = viewModelScope.launch {
        repo.updateNote(note.copy(name = newName))
    }

    fun deleteNote(note: Note) = viewModelScope.launch { repo.deleteNote(note) }

    fun addItem(noteId: Long, text: String) = viewModelScope.launch { repo.addItem(noteId, text) }

    fun toggleItem(item: Item) = viewModelScope.launch { repo.toggleItem(item) }

    fun updateItemText(item: Item, text: String) = viewModelScope.launch { repo.updateItemText(item, text) }

    fun deleteItem(item: Item) = viewModelScope.launch { repo.deleteItem(item) }
}
