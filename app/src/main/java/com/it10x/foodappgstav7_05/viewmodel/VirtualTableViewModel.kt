package com.it10x.foodappgstav7_05.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.it10x.foodappgstav7_05.data.pos.AppDatabaseProvider
import com.it10x.foodappgstav7_05.data.pos.entities.VirtualTableEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class VirtualTableViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabaseProvider.get(app).virtualTableDao()

    // 🔥 Current selected order type
    private val selectedType = MutableStateFlow<String?>(null)

    private val cartDao = AppDatabaseProvider.get(app).cartDao()
    private val kotDao = AppDatabaseProvider.get(app).kotItemDao()


    // 🔥 Automatically reacts when type changes
    val tables: StateFlow<List<VirtualTableEntity>> =
        selectedType
            .filterNotNull()
            .flatMapLatest { type ->
                dao.observeByType(type)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // 🔥 Just set type, no manual collecting
    fun observe(type: String) {
        selectedType.value = type
    }

    fun createNew(type: String, srno: Int): VirtualTableEntity {

        val prefix = if (type == "TAKEAWAY") "TA" else "DL"

        val newRow = VirtualTableEntity(
            id = UUID.randomUUID().toString(),
            tableName = "$prefix-$srno",
            orderType = type,
            status = TableStatus.AVAILABLE,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            dao.insert(newRow)
        }

        return newRow   // 🔥 return immediately
    }

    fun delete(id: String) {
        viewModelScope.launch {
            dao.deleteById(id)
        }
    }


    fun syncCartCount(tableId: String) {
        viewModelScope.launch {
            val count = cartDao.getCartCountForTable(tableId) ?: 0
            dao.setCartCount(
                tableId = tableId,
                count = count,
                time = System.currentTimeMillis()
            )
        }
    }

    fun syncBillData(tableId: String) {
        viewModelScope.launch {

            val billCount = kotDao.getBillQtyCount(tableId) ?: 0
            val billAmount = kotDao.sumDoneAmount(tableId) ?: 0.0

            dao.setBillData(
                tableId = tableId,
                count = billCount,
                amount = billAmount,
                time = System.currentTimeMillis()
            )
        }
    }

    fun syncKitchenCount(tableId: String) {
        viewModelScope.launch {
            val count = kotDao.getKitchenCountForTable(tableId) ?: 0
            dao.setKitchenCount(
                tableId = tableId,
                count = count,
                time = System.currentTimeMillis()
            )
        }
    }
}