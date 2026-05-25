package com.noted.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class NoteType { LIST }

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: NoteType = NoteType.LIST,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
