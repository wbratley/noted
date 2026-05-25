package com.noted.data

import androidx.room.Embedded
import androidx.room.Relation

data class NoteWithItems(
    @Embedded val note: Note,
    @Relation(
        parentColumn = "id",
        entityColumn = "noteId"
    )
    val items: List<Item>
)
