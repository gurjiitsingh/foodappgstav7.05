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

    fun createNew(type: String, srno: Int) {
        viewModelScope.launch {

            val prefix = if (type == "TAKEAWAY") "TA" else "DL"

            val newRow = VirtualTableEntity(
                id = UUID.randomUUID().toString(),
                tableName = "$prefix-$srno",
                orderType = type,
                status = TableStatus.AVAILABLE,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            dao.insert(newRow)
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            dao.deleteById(id)
        }
    }
}