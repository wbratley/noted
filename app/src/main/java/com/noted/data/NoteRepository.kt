package com.noted.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val dao: NoteDao) {
    val allNotesWithItems: Flow<List<NoteWithItems>> = dao.getAllNotesWithItems()

    fun getNoteWithItems(id: Long): Flow<NoteWithItems> = dao.getNoteWithItems(id)

    suspend fun createNote(name: String): Long = dao.insertNote(Note(name = name))

    suspend fun updateNote(note: Note) = dao.updateNote(note.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteNote(note: Note) = dao.deleteNote(note)

    suspend fun addItem(noteId: Long, text: String): Long = dao.insertItem(Item(noteId = noteId, text = text))

    suspend fun toggleItem(item: Item) = dao.updateItem(item.copy(ticked = !item.ticked))

    suspend fun updateItemText(item: Item, text: String) = dao.updateItem(item.copy(text = text))

    suspend fun deleteItem(item: Item) = dao.deleteItem(item)
}
