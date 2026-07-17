package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ResellDao {
    @Query("SELECT * FROM resell_items ORDER BY dateBought DESC")
    fun getAllItems(): Flow<List<ResellItem>>

    @Query("SELECT * FROM resell_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): ResellItem?

    @Query("SELECT * FROM resell_items WHERE barcode = :barcode LIMIT 1")
    suspend fun getItemByBarcode(barcode: String): ResellItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ResellItem): Long

    @Update
    suspend fun updateItem(item: ResellItem)

    @Query("DELETE FROM resell_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)
}
