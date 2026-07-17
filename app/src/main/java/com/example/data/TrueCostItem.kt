package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "true_costs")
data class TrueCostItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, // Bubble wrap, boxes, envelopes, etc.
    val cost: Double,
    val dateBought: Long = System.currentTimeMillis(),
    val notes: String = ""
)
