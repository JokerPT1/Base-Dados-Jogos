package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "resell_items")
data class ResellItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "GAME" or "CONSOLE"
    val name: String,
    val platform: String, // "PS1", "PS2", "PS3", "PS4", "PS5", "Xbox", "Switch", "Retro", "Other"
    val barcode: String = "",
    val condition: String = "Good", // "New", "Like New", "Very Good", "Good", "Acceptable"
    val tested: Boolean = false,
    val onSale: Boolean = false,
    val status: String = "Inventory", // "Inventory", "Listed", "Sold", "Keep"
    val priceBought: Double = 0.0,
    val priceSold: Double = 0.0,
    val whereBought: String = "",
    val whereSold: String = "",
    val dateBought: Long = System.currentTimeMillis(),
    val dateSold: Long = 0,
    val notes: String = ""
)
