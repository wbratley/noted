package com.noted

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.noted.data.Item
import com.noted.data.Note
import com.noted.data.NoteDatabase
import com.noted.data.NoteDao
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteDaoTest {

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    private lateinit var db: NoteDatabase
    private lateinit var dao: NoteDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NoteDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.noteDao()
    }

    @After
    fun closeDb() = db.close()

    @Test
    fun insertNoteAndRetrieve() = runBlocking {
        val id = dao.insertNote(Note(name = "Groceries"))
        val result = dao.getAllNotesWithItems().getOrAwaitValue()
        assertEquals(1, result.size)
        assertEquals("Groceries", result[0].note.name)
        assertEquals(id, result[0].note.id)
    }

    @Test
    fun insertItemsAndRetrieveWithNote() = runBlocking {
        val noteId = dao.insertNote(Note(name = "Todo"))
        dao.insertItem(Item(noteId = noteId, text = "first"))
        dao.insertItem(Item(noteId = noteId, text = "second"))
        val result = dao.getNoteWithItems(noteId).getOrAwaitValue()
        assertEquals(2, result.items.size)
    }

    @Test
    fun deleteTickedItemsLeavesUnticked() = runBlocking {
        val noteId = dao.insertNote(Note(name = "Mixed"))
        dao.insertItem(Item(noteId = noteId, text = "done", ticked = true))
        dao.insertItem(Item(noteId = noteId, text = "todo", ticked = false))
        dao.deleteTickedItems(noteId)
        val result = dao.getNoteWithItems(noteId).getOrAwaitValue()
        assertEquals(1, result.items.size)
        assertEquals("todo", result.items[0].text)
    }

    @Test
    fun deleteTickedItemsWhenNoneLeavesAll() = runBlocking {
        val noteId = dao.insertNote(Note(name = "All unticked"))
        dao.insertItem(Item(noteId = noteId, text = "a"))
        dao.insertItem(Item(noteId = noteId, text = "b"))
        dao.deleteTickedItems(noteId)
        val result = dao.getNoteWithItems(noteId).getOrAwaitValue()
        assertEquals(2, result.items.size)
    }

    @Test
    fun deleteNoteCascadesItems() = runBlocking {
        val noteId = dao.insertNote(Note(name = "To delete"))
        dao.insertItem(Item(noteId = noteId, text = "orphan"))
        val note = dao.getNoteWithItems(noteId).getOrAwaitValue().note
        dao.deleteNote(note)
        val all = dao.getAllNotesWithItems().getOrAwaitValue()
        assertTrue(all.isEmpty())
    }

    @Test
    fun insertItemsBulk() = runBlocking {
        val noteId = dao.insertNote(Note(name = "Bulk"))
        dao.insertItems(
            listOf(
                Item(noteId = noteId, text = "one"),
                Item(noteId = noteId, text = "two"),
                Item(noteId = noteId, text = "three"),
            ),
        )
        val result = dao.getNoteWithItems(noteId).getOrAwaitValue()
        assertEquals(3, result.items.size)
    }
}
