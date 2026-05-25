package com.noted.viewmodel

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.noted.data.ClaudeRepository
import com.noted.data.Item
import com.noted.data.Note
import com.noted.data.NoteDatabase
import com.noted.data.NoteRepository
import com.noted.data.NoteWithItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ImportState {
    object Idle : ImportState()
    object Loading : ImportState()
    data class Success(val count: Int) : ImportState()
    data class Error(val message: String) : ImportState()
}

data class PendingShare(val text: String?, val imageUri: Uri?)

class NoteViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = NoteRepository(NoteDatabase.getDatabase(app).noteDao())

    val allNotesWithItems: LiveData<List<NoteWithItems>> = repo.allNotesWithItems

    private val noteCache = mutableMapOf<Long, LiveData<NoteWithItems>>()
    fun getNoteWithItems(id: Long): LiveData<NoteWithItems> =
        noteCache.getOrPut(id) { repo.getNoteWithItems(id) }

    val importState = MutableLiveData<ImportState>(ImportState.Idle)
    val pendingShare = MutableLiveData<PendingShare?>()

    fun createNote(name: String) = viewModelScope.launch { repo.createNote(name) }

    suspend fun createAndGetNoteId(name: String): Long = repo.createNote(name)

    fun renameNote(note: Note, newName: String) = viewModelScope.launch {
        repo.updateNote(note.copy(name = newName))
    }

    fun deleteNote(note: Note) = viewModelScope.launch { repo.deleteNote(note) }

    fun deleteTickedItems(noteId: Long) = viewModelScope.launch { repo.deleteTickedItems(noteId) }

    fun addItem(noteId: Long, text: String) = viewModelScope.launch { repo.addItem(noteId, text) }

    fun addItems(noteId: Long, texts: List<String>) = viewModelScope.launch { repo.addItems(noteId, texts) }

    fun toggleItem(item: Item) = viewModelScope.launch { repo.toggleItem(item) }

    fun updateItemText(item: Item, text: String) = viewModelScope.launch { repo.updateItemText(item, text) }

    fun deleteItem(item: Item) = viewModelScope.launch { repo.deleteItem(item) }

    fun importFromContent(noteId: Long, text: String?, imageUri: Uri?) {
        importState.value = ImportState.Loading
        viewModelScope.launch {
            try {
                val bitmap = imageUri?.let { uri ->
                    withContext(Dispatchers.IO) {
                        try {
                            getApplication<Application>().contentResolver
                                .openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                        } catch (e: Exception) { null }
                    }
                }
                val items = ClaudeRepository(getApplication()).generateItems(text, bitmap)
                repo.addItems(noteId, items)
                importState.postValue(ImportState.Success(items.size))
            } catch (e: Exception) {
                importState.postValue(ImportState.Error(e.message ?: "Import failed"))
            }
        }
    }
}
