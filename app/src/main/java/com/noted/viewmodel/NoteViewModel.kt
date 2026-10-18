package com.noted.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.noted.data.ClaudeRepository
import com.noted.data.Item
import com.noted.data.ListParser
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

class NoteViewModel(
    app: Application,
    private val repo: NoteRepository
) : AndroidViewModel(app) {

    val allNotesWithItems: LiveData<List<NoteWithItems>> = repo.allNotesWithItems

    private val noteCache = mutableMapOf<Long, LiveData<NoteWithItems>>()

    private val _importState = MutableLiveData<ImportState>(ImportState.Idle)
    val importState: LiveData<ImportState> = _importState

    private val _pendingShare = MutableLiveData<PendingShare?>()
    val pendingShare: LiveData<PendingShare?> = _pendingShare

    fun getNoteWithItems(id: Long): LiveData<NoteWithItems> = noteCache.getOrPut(id) { repo.getNoteWithItems(id) }

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

    fun resetImportState() {
        _importState.value = ImportState.Idle
    }

    fun setPendingShare(share: PendingShare?) {
        _pendingShare.value = share
    }

    fun importFromContent(noteId: Long, text: String?, imageUri: Uri?) {
        _importState.value = ImportState.Loading
        viewModelScope.launch {
            try {
                // Text that is already a list parses locally — no network, works without Claude.
                val local = if (text != null && imageUri == null) ListParser.parse(text) else null
                val items =
                    if (local != null) {
                        local.items
                    } else {
                        val bitmap = decodeImage(imageUri)
                        ClaudeRepository(getApplication()).generateItems(text, bitmap)
                    }
                renameIfNewList(noteId, local?.title)
                repo.addItems(noteId, items)
                _importState.postValue(ImportState.Success(items.size))
            } catch (e: Exception) {
                _importState.postValue(ImportState.Error(e.message ?: "Import failed"))
            }
        }
    }

    /** A heading pasted before the bullets becomes the name of a brand-new list. */
    private suspend fun renameIfNewList(noteId: Long, title: String?) {
        if (title == null) return
        val note = repo.getNoteWithItems(noteId).value?.note
        if (note != null && note.name == NEW_LIST_NAME) {
            repo.updateNote(note.copy(name = title))
        }
    }

    private suspend fun decodeImage(uri: Uri?): Bitmap? = uri?.let { u ->
        withContext(Dispatchers.IO) {
            try {
                getApplication<Application>().contentResolver
                    .openInputStream(u)?.use { BitmapFactory.decodeStream(it) }
            } catch (e: Exception) {
                null
            }
        }
    }

    companion object {
        const val NEW_LIST_NAME = "New list"

        fun factory(app: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = NoteViewModel(
                app,
                NoteRepository(NoteDatabase.getDatabase(app).noteDao())
            ) as T
        }
    }
}
