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

    // True Costs (Expenses) Queries
    @Query("SELECT * FROM true_costs ORDER BY dateBought DESC")
    fun getAllTrueCosts(): Flow<List<TrueCostItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrueCost(item: TrueCostItem): Long

    @Query("DELETE FROM true_costs WHERE id = :id")
    suspend fun deleteTrueCostById(id: Long)

    // Loose Parts Queries
    @Query("SELECT * FROM loose_parts ORDER BY dateAdded DESC")
    fun getAllLooseParts(): Flow<List<LoosePartItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoosePart(item: LoosePartItem): Long

    @Query("DELETE FROM loose_parts WHERE id = :id")
    suspend fun deleteLoosePartById(id: Long)
}

