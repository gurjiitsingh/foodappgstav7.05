package com.it10x.foodappgstav7_05.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.it10x.foodappgstav7_05.data.PrinterRole
import com.it10x.foodappgstav7_05.data.online.models.OrderMasterData
import com.it10x.foodappgstav7_05.data.online.models.OrderProductData
import com.it10x.foodappgstav7_05.data.online.models.formattedTime
import com.it10x.foodappgstav7_05.data.online.models.repository.OrdersRepository
import com.it10x.foodappgstav7_05.printer.PrinterManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.it10x.foodappgstav7_05.printer.FirestorePrintMapper
import com.it10x.foodappgstav7_05.printer.ReceiptFormatter
import com.it10x.foodappgstav7_05.data.pos.AppDatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.it10x.foodappgstav7_05.data.mapper.OnlineOrderMapper
import com.it10x.foodappgstav7_05.data.online.models.createdAtMillis

class OnlineOrdersViewModel(
    private val printerManager: PrinterManager
) : ViewModel() {

    private val repo = OrdersRepository()


    suspend fun getOrderItems(orderId: String): List<OrderProductData> {
        return repo.getOrderProducts(orderId)
    }

    private val _orderItems = MutableStateFlow<Map<String, List<OrderProductData>>>(emptyMap())
    val orderItems: StateFlow<Map<String, List<OrderProductData>>> = _orderItems

    fun loadOrderItems(orderId: String) {
        viewModelScope.launch {
            val items = repo.getOrderProducts(orderId)
            _orderItems.value = _orderItems.value + (orderId to items)
        }
    }
    val pageIndex = MutableStateFlow(0)
    private val _orders = MutableStateFlow<List<OrderMasterData>>(emptyList())
    val orders: StateFlow<List<OrderMasterData>> = _orders

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val limit = 10

    // -----------------------------
    // PRINT ORDER (KITCHEN + BILLING)
    // -----------------------------
    fun printOrder(order: OrderMasterData) {

     //   Log.d("PRINT_SOURCE", "🔥 OrdersViewModel.printOrder CALLED")
        viewModelScope.launch {

            val items = repo.getOrderProducts(order.id)
            Log.e("PRINT", "Order No. ${order.srno}")
            if (items.isEmpty()) {
                Log.e("PRINT", "No items for order ${order.srno}")
                return@launch
            }

            //BILL PRINT ONLINE ORDER WHEN BUTTON PRESSED
            val printOrder = FirestorePrintMapper.map(order, items)
            printerManager.printTextNew(PrinterRole.BILLING, printOrder)

            kotlinx.coroutines.delay(2_000)


            //KITCHEN PRINT ONLINE ORDER WHEN BUTTON PRESSED
            val kotItems = OnlineOrderMapper.toKotItems(items)

            printerManager.printTextKitchen(
                PrinterRole.KITCHEN,
                sessionKey = order.srno.toString(),
                orderType = "Online order",
                kotItems ){
                Log.d("PRINT", "Kitchen print success=$it")
            }


        }
    }




fun loadFirstPage() {
    viewModelScope.launch {
        _loading.value = true

        pageIndex.value = 0

        _orders.value = repo.getNextPage(limit.toLong())
            .sortedByDescending { it.createdAtMillis() }

        _loading.value = false
    }
}

fun loadNextPage() {
    viewModelScope.launch {
        _loading.value = true

        pageIndex.value++

        _orders.value = repo.getNextPage(limit.toLong())
            .sortedByDescending { it.createdAtMillis() }

        _loading.value = false
    }
}

fun loadPrevPage() {
    viewModelScope.launch {
        _loading.value = true

        if (pageIndex.value > 0)
            pageIndex.value--

        _orders.value = repo.getNextPage(limit.toLong())
            .sortedByDescending { it.createdAtMillis() }

        _loading.value = false
    }
}














    // -----------------------------
    // HELPERS
    // -----------------------------
    private fun totalLine(label: String, value: Double): String {
        if (value == 0.0) return "" // skip zero values
        val left = label.padEnd(14)
        val right = formatAmount(value).padStart(18)
        return left + right
    }
    private fun formatAmount(value: Double?): String =
        "%.2f".format(value ?: 0.0)

    private fun toDouble(value: Any?): Double =
        when (value) {
            is Double -> value
            is Long -> value.toDouble()
            is Int -> value.toDouble()
            is Float -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }

    private fun btSafe(text: String, max: Int): String {
        return text
            .replace(Regex("[^A-Za-z0-9 ]"), "") // remove Unicode
            .trim()
            .take(max)
    }


}
