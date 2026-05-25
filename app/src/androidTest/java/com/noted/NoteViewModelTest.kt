package com.noted

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.noted.data.Item
import com.noted.data.Note
import com.noted.data.NoteRepository
import com.noted.data.NoteWithItems
import com.noted.viewmodel.ImportState
import com.noted.viewmodel.NoteViewModel
import com.noted.viewmodel.PendingShare
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class NoteViewModelTest {

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    private val repo: NoteRepository = mockk(relaxed = true)
    private lateinit var vm: NoteViewModel

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        vm = NoteViewModel(app, repo)
    }

    @Test
    fun importState_startsIdle() {
        assertEquals(ImportState.Idle, vm.importState.value)
    }

    @Test
    fun resetImportState_setsIdle() {
        vm.resetImportState()
        assertEquals(ImportState.Idle, vm.importState.value)
    }

    @Test
    fun pendingShare_startsNull() {
        assertNull(vm.pendingShare.value)
    }

    @Test
    fun setPendingShare_updatesValue() {
        val share = PendingShare(text = "hello", imageUri = null)
        vm.setPendingShare(share)
        assertEquals(share, vm.pendingShare.getOrAwaitValue())
    }

    @Test
    fun setPendingShare_null_clearsValue() {
        vm.setPendingShare(PendingShare("x", null))
        vm.setPendingShare(null)
        assertNull(vm.pendingShare.value)
    }

    @Test
    fun deleteTickedItems_delegatesToRepo() {
        vm.deleteTickedItems(42L)
        Thread.sleep(100)
        coVerify { repo.deleteTickedItems(42L) }
    }

    @Test
    fun deleteNote_delegatesToRepo() {
        val note = Note(id = 1L, name = "Test")
        vm.deleteNote(note)
        Thread.sleep(100)
        coVerify { repo.deleteNote(note) }
    }

    @Test
    fun addItem_delegatesToRepo() {
        vm.addItem(1L, "task")
        Thread.sleep(100)
        coVerify { repo.addItem(1L, "task") }
    }
}
