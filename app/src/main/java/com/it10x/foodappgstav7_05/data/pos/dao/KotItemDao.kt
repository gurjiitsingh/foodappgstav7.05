package com.it10x.foodappgstav7_05.data.pos.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.it10x.foodappgstav7_05.data.pos.entities.PosKotBatchEntity
import com.it10x.foodappgstav7_05.data.pos.entities.PosKotItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KotItemDao {



    @Dao
    interface KotItemDao {

        @Insert(onConflict = OnConflictStrategy.ABORT)
        suspend fun insertAll(items: List<PosKotItemEntity>)

        // 🔥 Printing
        @Query("""
        SELECT * FROM pos_kot_items
        WHERE tableNo = :tableNo
        AND kitchenPrinted = 0
        AND status = 'ACTIVE'
        ORDER BY createdAt ASC
    """)
        suspend fun getItemsToPrint(tableNo: String): List<PosKotItemEntity>

        @Query("""
        UPDATE pos_kot_items
        SET kitchenPrinted = 1
        WHERE id IN (:ids)
    """)
        suspend fun markPrinted(ids: List<String>)

        // 💳 Billing
        @Query("""
        SELECT * FROM pos_kot_items
        WHERE tableNo = :tableNo
        AND status = 'ACTIVE'
    """)
        suspend fun getBillItems(tableNo: String): List<PosKotItemEntity>

        @Query("""
        SELECT SUM(quantity) FROM pos_kot_items
        WHERE tableNo = :tableNo
        AND productId = :productId
        AND status = 'ACTIVE'
    """)
        suspend fun getTotalQtyForProduct(
            tableNo: String,
            productId: String
        ): Int?
    }


    @Dao
    interface KotBatchDao {

        @Insert(onConflict = OnConflictStrategy.ABORT)
        suspend fun insert(batch: PosKotBatchEntity)

        @Query("""
        SELECT * FROM pos_kot_batch
        WHERE tableNo = :tableNo
        ORDER BY createdAt ASC
    """)
        suspend fun getBatchesForTable(tableNo: String): List<PosKotBatchEntity>

        @Query("""
        SELECT * FROM pos_kot_batch
        WHERE syncStatus != 'DONE'
    """)
        suspend fun getUnsyncedBatches(): List<PosKotBatchEntity>
    }



    // -------------------------
    // INSERT
    // -------------------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PosKotItemEntity>)


    // -------------------------
    // INSERT SINGLE ITEM (NEW)
    // -------------------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PosKotItemEntity)

    // -------------------------
    // FETCH (LIVE BILL)
    // -------------------------
    @Query("""
        SELECT * FROM pos_kot_items
        WHERE tableNo = :tableNo
        ORDER BY createdAt ASC
    """)
    fun getItemsForTable(tableNo: String): Flow<List<PosKotItemEntity>>

    // -------------------------
    // FETCH (FINAL BILL)
    // -------------------------
    @Query("""
        SELECT * FROM pos_kot_items
        WHERE tableNo = :tableNo
    """)
    suspend fun getItemsForTableSync(tableNo: String?): List<PosKotItemEntity>

    // -------------------------
    // CLEANUP AFTER PAYMENT
    // -------------------------
    @Query("""
        DELETE FROM pos_kot_items
        WHERE tableNo = :tableNo
    """)
    suspend fun clearForTable(tableNo: String)

    @Query("""
        DELETE FROM pos_kot_items       
    """)
    suspend fun clearForTableAll()


    @Query("""
    UPDATE pos_kot_items
    SET status = :status
    WHERE id = :itemId
""")
    suspend fun updateStatus(
        itemId: String,
        status: String
    )


    @Query("SELECT * FROM pos_kot_items WHERE id = :itemId LIMIT 1")
    suspend fun getItemByIdSync(itemId: String): PosKotItemEntity?
    @Query("""
    SELECT * FROM pos_kot_items
    WHERE tableNo = :tableNo
      AND status = 'PENDING'
    ORDER BY createdAt ASC
""")
    fun getPendingItemsForTable(tableNo: String): Flow<List<PosKotItemEntity>>

    @Query("""
    SELECT * FROM pos_kot_items
    WHERE tableNo = :tableNo
      AND status = 'DONE'
    ORDER BY createdAt ASC
""")
    fun getDoneItemsForTable(tableNo: String): Flow<List<PosKotItemEntity>>

    @Query("DELETE FROM pos_kot_items")
    suspend fun deleteAllKotItems()


    @Query("SELECT * FROM pos_kot_items")
    fun getTotalKotItems(): Flow<List<PosKotItemEntity>>



    @Query("""
SELECT * FROM pos_kot_items
WHERE kotBatchId = :sessionKey
AND status = 'PENDING'
""")
    fun getPendingItemsForSession(sessionKey: String): Flow<List<PosKotItemEntity>>

    @Query("SELECT * FROM pos_kot_items ORDER BY createdAt ASC")
    fun getAllKotItems(): Flow<List<PosKotItemEntity>>

    @Query("SELECT * FROM pos_kot_items")
    fun getAllKotItems1(): Flow<List<PosKotItemEntity>>

    @Query("SELECT quantity FROM pos_kot_items WHERE productId = :itemId LIMIT 1")
    suspend fun getItemQtyById1(itemId: String): Int?

    @Query("UPDATE pos_kot_items SET quantity = :qty WHERE productId = :id")
    suspend fun updateQuantity1(id: String, qty: Int)

//    @Query("DELETE FROM pos_kot_items WHERE productId = :itemId")
//    suspend fun deleteItemById(itemId: String)

    @Query("DELETE FROM pos_kot_items WHERE id = :id")
    suspend fun deleteItemById(id: String)
    // -------------------------
// PRINT FLAG
// -------------------------
//    @Query("""
//    UPDATE pos_kot_items
//    SET print = 1
//    WHERE id = :itemId
//""")
//    suspend fun markPrinted(itemId: String)



    // ✅ Get quantity for a specific table + product (unique key)
    @Query("SELECT quantity FROM pos_kot_items WHERE id = :id LIMIT 1")
    suspend fun getItemQtyById(id: String): Int?

    // ✅ Update quantity for a specific table + product
    @Query("UPDATE pos_kot_items SET quantity = :qty WHERE id = :id")
    suspend fun updateQuantity(id: String, qty: Int)

    // ✅ Check if an item exists for table + product
    @Query("SELECT EXISTS(SELECT 1 FROM pos_kot_items WHERE id = :id)")
    suspend fun isItemAlreadyExists(id: String): Boolean

    // ✅ Insert a single item (will REPLACE if id already exists)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert1(item: PosKotItemEntity)


    // -------------------------
// FETCH UNPRINTED ITEMS
// -------------------------
    @Query("""
    SELECT * FROM pos_kot_items
    WHERE tableNo = :tableNo
      AND status = 'PENDING'
      AND kitchenPrinted = 0
    ORDER BY createdAt ASC
""")
    suspend fun getUnprintedPendingItems(tableNo: String): List<PosKotItemEntity>


    @Query("""
    SELECT * FROM pos_kot_items
    WHERE tableNo = :tableNo
      AND status = 'PENDING'
      AND kitchenPrintReq = 1
      AND kitchenPrinted = 0
    ORDER BY createdAt ASC
""")
    suspend fun getItemsToPrintForKitchen(tableNo: String): List<PosKotItemEntity>

    @Query("""
    SELECT * FROM pos_kot_items
    WHERE tableNo = :tableNo
    ORDER BY createdAt ASC
""")
    suspend fun getItemsAll(tableNo: String): List<PosKotItemEntity>

    @Query("""
    UPDATE pos_kot_items
    SET kitchenPrinted = 1
    WHERE id IN (:ids)
""")
    suspend fun markKitchenPrinted(ids: List<String>)


    @Query("""
    SELECT * FROM pos_kot_items
    WHERE tableNo = :tableNo
      AND status = 'PENDING'
      ORDER BY createdAt ASC
""")
    suspend fun getPendingItems(tableNo: String): List<PosKotItemEntity>

    @Query("""
    UPDATE pos_kot_items
    SET kitchenPrinted = 1
    WHERE tableNo = :tableNo
      AND kitchenPrinted = 0
""")
    suspend fun markAllPrintedForTable(tableNo: String)

    @Query("""
    UPDATE pos_kot_items
    SET kitchenPrinted = 1
    WHERE id = :itemId
""")
    suspend fun markPrinted(itemId: String)

    @Query("""
    UPDATE pos_kot_items
    SET kitchenPrinted = 1
    WHERE id IN (:itemIds)
""")
    suspend fun markPrintedBatch(itemIds: List<String>)

    @Query("""
    UPDATE pos_kot_items
    SET status = 'DONE'
    WHERE tableNo = :tableNo
      AND status = 'PENDING'
""")
    suspend fun markAllDone(tableNo: String)

    @Query("""
    UPDATE pos_kot_items
    SET kitchenPrinted = 1
    WHERE tableNo = :tableNo
      AND kitchenPrinted = 0
""")
    suspend fun markAllPrinted(tableNo: String)

    @Query("""
    SELECT * FROM pos_kot_items
    WHERE tableNo = :tableNo
    AND kitchenPrintReq = 1
      AND kitchenPrinted = 0
    ORDER BY createdAt ASC
""")
    suspend fun getUnprintedItems(tableNo: String): List<PosKotItemEntity>

    @Query("""
    UPDATE pos_kot_items
    SET kitchenPrinted = 1
    WHERE kotBatchId = :batchId
""")
    suspend fun markBatchKitchenPrintedBatch(batchId: String)

    @Query("""
    SELECT * FROM pos_kot_items
    WHERE kotBatchId = :batchId
    AND kitchenPrintReq = 1
      AND kitchenPrinted = 0
    ORDER BY createdAt ASC
""")
    suspend fun getItemsByBatchId(batchId: String): List<PosKotItemEntity>
    @Query("""
    SELECT * FROM pos_kot_items
    WHERE tableNo = :tableNo
      
    ORDER BY createdAt ASC
""")
    suspend fun getAllItems(tableNo: String): List<PosKotItemEntity>
    @Query("""
    SELECT COUNT(*) 
    FROM pos_kot_items 
    WHERE sessionId = :sessionId
""")
    suspend fun cartItemCount(sessionId: String): Int


    @Query("""
    SELECT EXISTS(
        SELECT 1 FROM pos_kot_items WHERE sessionId = :sessionId
    )
""")
    suspend fun hasCartItems(sessionId: String): Boolean



    // -------------------------
// TABLE GRID – COUNTS
// -------------------------

    @Query("""
    SELECT COUNT(*) 
    FROM pos_kot_items
    WHERE tableNo = :tableNo
      AND status = 'PENDING'
""")
    suspend fun countKitchenPending(tableNo: String): Int


    @Query("""
    SELECT COUNT(*) 
    FROM pos_kot_items
    WHERE tableNo = :tableNo
      AND status = 'DONE'
""")
    suspend fun countBillDone(tableNo: String): Int

//    @Query("SELECT COUNT(*) FROM pos_kot_items WHERE tableNo = :tableNo AND status = 'DONE'")
//    suspend fun countDoneItems(tableNo: String): Int?

    @Query("""
    SELECT COUNT(*) 
    FROM pos_kot_items 
    WHERE tableNo = :tableNo 
    AND status = 'DONE'
""")
    suspend fun getBillLineCount(tableNo: String): Int?

    @Query("""
    SELECT SUM(quantity) 
    FROM pos_kot_items 
    WHERE tableNo = :tableNo 
    AND status = 'DONE'
""")
    suspend fun getBillQtyCount(tableNo: String): Int?

    @Query("""
SELECT SUM(basePrice * quantity)
FROM pos_kot_items
WHERE tableNo = :tableNo AND status = 'DONE'
""")
    suspend fun sumDoneAmount(tableNo: String): Double?


    // -------------------------
// TABLE GRID – BILL AMOUNT
// -------------------------
    @Query("""
    SELECT 
        IFNULL(SUM(
            (basePrice * quantity) +
            CASE 
                WHEN taxType = 'exclusive'
                THEN (basePrice * quantity * taxRate / 100)
                ELSE 0
            END
        ), 0)
    FROM pos_kot_items
    WHERE tableNo = :tableNo
      AND status = 'DONE'
""")
    suspend fun billAmountForTable(tableNo: String): Double


    @Query("""
    SELECT IFNULL(SUM(
        (basePrice * quantity) +
        CASE 
            WHEN taxType = 'exclusive' 
            THEN (basePrice * quantity * taxRate / 100)
            ELSE 0
        END
    ), 0)
    FROM pos_kot_items
    WHERE tableNo = :tableNo
      AND status IN ('PENDING', 'DONE')
""")
    suspend fun billAmountForTableIncludingKitchen(tableNo: String): Double


    @Query("SELECT COUNT(*) > 0 FROM pos_kot_items WHERE id = :orderId")
    suspend fun isOrderAlreadyProcessed(orderId: String): Boolean


    @Query("SELECT * FROM pos_kot_items")
    suspend fun getTotalKotItemsOnce(): List<PosKotItemEntity>

    @Query("DELETE FROM pos_kot_items")
    suspend fun deleteAll()
}
