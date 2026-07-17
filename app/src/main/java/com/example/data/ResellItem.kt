package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "resell_items")
data class ResellItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "GAME" or "CONSOLE"
    val name: String,
    val platform: String, // "PS1", "PS2", "PS3", "PS4", "PS5", "PSP", "PS Vita", "Switch", "Xbox", "Retro", "Other"
    val barcode: String = "",
    val condition: String = "CIB", // "CIB", "Without Manual", "Loose", "Broken Box", "Not Working"
    val tested: Boolean = false,
    val onSale: Boolean = false,
    val status: String = "Inventory", // "Inventory", "Listed", "Sold", "Keep"
    val priceBought: Double = 0.0,
    val priceSold: Double = 0.0,
    val shippingCost: Double = 0.0,
    val countryOfSale: String = "", // e.g. "Portugal", "Spain", "Italy", etc.
    val gameVersion: String = "Normal", // "Normal", "Platinum", "Essentials", "SteelBook", "ArtBook", etc.
    val photoPath: String = "", // URI or prefilled vector/preset indicator
    val whereBought: String = "",
    val whereSold: String = "",
    val dateBought: Long = System.currentTimeMillis(),
    val dateSold: Long = 0,
    val notes: String = ""
)

