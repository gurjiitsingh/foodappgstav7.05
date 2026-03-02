package com.it10x.foodappgstav7_05.data.pos.repository

import android.util.Log
import com.it10x.foodappgstav7_05.data.pos.dao.KotBatchDao
import com.it10x.foodappgstav7_05.data.pos.dao.KotItemDao
import com.it10x.foodappgstav7_05.data.pos.dao.TableDao
import com.it10x.foodappgstav7_05.data.pos.entities.PosKotItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class KotRepository(
    private val batchDao: KotBatchDao,
    private val kotItemDao: KotItemDao,
    private val tableDao: TableDao
) {

    fun getRunningKotsForTable(tableNo: String): Flow<Pair<List<Any>, List<Any>>> {
        return combine(
            batchDao.getBatchesForTable(tableNo),
            kotItemDao.getItemsForTable(tableNo)
        ) { batches, items ->
            batches to items
        }
    }


    suspend fun insertItemsInBill(
        tableNo: String,
        items: List<PosKotItemEntity>
    ) {
        kotItemDao.insertAll(items)

    }





    suspend fun markDoneAll(tableNo: String) {
        kotItemDao.markAllDone(tableNo)
         }
    suspend fun markPrinted(tableNo: String) {
        kotItemDao.markAllPrinted(tableNo)
    }

    private suspend fun syncBillCounters(tableNo: String) {
       // val billCount = kotItemDao.countDoneItems(tableNo) ?: 0
        val billCount = kotItemDao.getBillQtyCount(tableNo) ?: 0
        val billAmount = kotItemDao.sumDoneAmount(tableNo) ?: 0.0

        tableDao.updateBill(tableNo, billCount, billAmount)
    }

    private suspend fun syncKitchenCount(tableNo: String) {
        val count = kotItemDao.countBillDone(tableNo) ?: 0
        tableDao.setKitchenCount(tableNo, count)
    }

    suspend fun syncBillCount(tableNo: String) {
          syncBillCounters(tableNo)
    }

    //THIS FUNCITON IS CALLED IN TABLE GRID
    suspend fun syncKinchenCount(tableNo: String) {
            syncKitchenCount(tableNo)
    }


}
