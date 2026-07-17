package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loose_parts")
data class LoosePartItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, // Game title
    val platform: String, // PS1, PS2, PSP, etc.
    val partType: String, // "Manual", "Artwork", "Box"
    val notes: String = "",
    val dateAdded: Long = System.currentTimeMillis()
)
