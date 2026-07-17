package com.example.data

import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class ResellRepository(private val resellDao: ResellDao) {
    val allItems: Flow<List<ResellItem>> = resellDao.getAllItems()

    suspend fun getItemById(id: Long): ResellItem? {
        return resellDao.getItemById(id)
    }

    suspend fun getItemByBarcode(barcode: String): ResellItem? {
        return resellDao.getItemByBarcode(barcode)
    }

    suspend fun insertItem(item: ResellItem): Long {
        return resellDao.insertItem(item)
    }

    suspend fun updateItem(item: ResellItem) {
        resellDao.updateItem(item)
    }

    suspend fun deleteItemById(id: Long) {
        resellDao.deleteItemById(id)
    }

    // True Costs
    val allTrueCosts: Flow<List<TrueCostItem>> = resellDao.getAllTrueCosts()
    suspend fun insertTrueCost(item: TrueCostItem): Long = resellDao.insertTrueCost(item)
    suspend fun deleteTrueCostById(id: Long) = resellDao.deleteTrueCostById(id)

    // Loose Parts
    val allLooseParts: Flow<List<LoosePartItem>> = resellDao.getAllLooseParts()
    suspend fun insertLoosePart(item: LoosePartItem): Long = resellDao.insertLoosePart(item)
    suspend fun deleteLoosePartById(id: Long) = resellDao.deleteLoosePartById(id)

    // Export entire database as a JSON string for cloud backup and multi-device sync
    suspend fun exportDatabaseAsJson(itemsList: List<ResellItem>): String {
        val jsonArray = JSONArray()
        for (item in itemsList) {
            val jsonObject = JSONObject().apply {
                put("type", item.type)
                put("name", item.name)
                put("platform", item.platform)
                put("barcode", item.barcode)
                put("condition", item.condition)
                put("tested", item.tested)
                put("onSale", item.onSale)
                put("status", item.status)
                put("priceBought", item.priceBought)
                put("priceSold", item.priceSold)
                put("shippingCost", item.shippingCost)
                put("countryOfSale", item.countryOfSale)
                put("gameVersion", item.gameVersion)
                put("photoPath", item.photoPath)
                put("whereBought", item.whereBought)
                put("whereSold", item.whereSold)
                put("dateBought", item.dateBought)
                put("dateSold", item.dateSold)
                put("notes", item.notes)
            }
            jsonArray.put(jsonObject)
        }
        val envelope = JSONObject().apply {
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("items", jsonArray)
        }
        return envelope.toString(2)
    }

    // Import and sync database from an exported JSON string
    suspend fun importDatabaseFromJson(jsonString: String): Result<Int> {
        return try {
            val envelope = JSONObject(jsonString)
            val itemsArray = envelope.getJSONArray("items")
            var importCount = 0
            for (i in 0 until itemsArray.length()) {
                val obj = itemsArray.getJSONObject(i)
                val barcode = obj.optString("barcode", "")
                
                // Create item
                val item = ResellItem(
                    type = obj.optString("type", "GAME"),
                    name = obj.optString("name", "Unknown Game"),
                    platform = obj.optString("platform", "Other"),
                    barcode = barcode,
                    condition = obj.optString("condition", "CIB"),
                    tested = obj.optBoolean("tested", false),
                    onSale = obj.optBoolean("onSale", false),
                    status = obj.optString("status", "Inventory"),
                    priceBought = obj.optDouble("priceBought", 0.0),
                    priceSold = obj.optDouble("priceSold", 0.0),
                    shippingCost = obj.optDouble("shippingCost", 0.0),
                    countryOfSale = obj.optString("countryOfSale", ""),
                    gameVersion = obj.optString("gameVersion", "Normal"),
                    photoPath = obj.optString("photoPath", ""),
                    whereBought = obj.optString("whereBought", ""),
                    whereSold = obj.optString("whereSold", ""),
                    dateBought = obj.optLong("dateBought", System.currentTimeMillis()),
                    dateSold = obj.optLong("dateSold", 0),
                    notes = obj.optString("notes", "")
                )

                // Check conflict: if item with same name & platform & barcode exists, update it, otherwise insert new
                // For simplicity of syncing, we insert or replace
                resellDao.insertItem(item)
                importCount++
            }
            Result.success(importCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
