package com.it10x.foodappgstav7_05.ui.waiterkitchen

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.compose.foundation.lazy.items

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.it10x.foodappgstav7_05.data.PrinterRole
import com.it10x.foodappgstav7_05.data.pos.AppDatabaseProvider
import com.it10x.foodappgstav7_05.data.pos.entities.PosCartEntity
import com.it10x.foodappgstav7_05.data.pos.entities.PosKotBatchEntity
import com.it10x.foodappgstav7_05.data.pos.entities.PosKotItemEntity
import com.it10x.foodappgstav7_05.data.pos.repository.CartRepository
import com.it10x.foodappgstav7_05.data.pos.repository.POSOrdersRepository
import com.it10x.foodappgstav7_05.data.pos.usecase.KotToBillUseCase
import com.it10x.foodappgstav7_05.printer.PrinterManager
import com.it10x.foodappgstav7_05.ui.cart.CartViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import com.it10x.foodappgstav7_05.data.pos.repository.KotRepository



import com.google.firebase.firestore.FirebaseFirestore
import com.it10x.foodappgstav7_05.data.online.models.waiter.WaiterOrder
import com.it10x.foodappgstav7_05.data.online.models.waiter.WaiterOrderItem

import com.it10x.foodappgstav7_05.data.pos.repository.WaiterKitchenRepository

import kotlinx.coroutines.tasks.await
class WaiterKitchenViewModel(
    app: Application,
    private val tableId: String,
    private val tableName: String,
    private val sessionId: String,
    private val orderType: String,
    private val repository: POSOrdersRepository,
    private val waiterKitchenRepository: WaiterKitchenRepository,
    private val cartViewModel: CartViewModel,
) : AndroidViewModel(app) {

    private var kotPrintJob: Job? = null
    private val pendingKotItems = mutableListOf<PosKotItemEntity>()
    private var pendingBatchId: String? = null
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> get() = _loading
    private val kotItemDao =
        AppDatabaseProvider.get(app).kotItemDao()

    private val _sendSuccess = MutableStateFlow(false)
    val sendSuccess: StateFlow<Boolean> = _sendSuccess

    private val kotToBillUseCase =
        KotToBillUseCase(kotItemDao)

    val kotItems: StateFlow<List<PosKotItemEntity>> =
        kotItemDao.getAllKotItems()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    private var isProcessing = false

    private val kotRepository = KotRepository(
        AppDatabaseProvider.get(app).kotBatchDao(),
        AppDatabaseProvider.get(app).kotItemDao(),
        AppDatabaseProvider.get(app).tableDao()
    )

    private val cartRepository = CartRepository(
        AppDatabaseProvider.get(app).cartDao(),
        AppDatabaseProvider.get(app).tableDao()
    )

    private val printerManager =
        PrinterManager(app.applicationContext)

    fun waiterCartTo_FireStore_Bill(
        cartList: List<PosCartEntity>,
        tableNo: String,
        deviceId: String,
        deviceName: String?,
    ) {
        if (isProcessing) return   // 🔥 Prevent duplicate presses

        viewModelScope.launch {
            // Always get fresh items from DB
            val dao = AppDatabaseProvider.get(getApplication()).cartDao()
            val latestCart = try {
                dao.getCartItemsByTableId(tableNo).first()
            } catch (e: Exception) {
                Log.e("WaiterKitchenVM", "❌ Failed to load cart from DB: ${e.message}", e)
                emptyList()
            }

            if (latestCart.isEmpty()) {
                Log.w("WaiterKitchenVM", "⚠️ No items found in DB for table=$tableNo")
                return@launch
            }

            Log.d("WaiterKitchenVM", "🚀 Send All triggered for table=$tableNo, total=${latestCart.size}")
            latestCart.forEachIndexed { index, item ->
                Log.d(
                    "WaiterKitchenVM",
                    "📦 [$index] name=${item.name}, qty=${item.quantity}, note='${item.note ?: ""}'"
                )
            }

            if (isProcessing) return@launch
            isProcessing = true
            _loading.value = true

            try {

                val success = withContext(Dispatchers.IO) {
                    waiterKitchenRepository.sendOrderToFireStore(
                        cartList = latestCart,
                        tableNo = tableNo,
                        sessionId = sessionId,
                        orderType = orderType,
                        deviceId = deviceId,
                        deviceName = deviceName
                    )
                }

                if (!success) {
                    Log.e("WaiterKitchenVM", "❌ Firestore upload failed")
                    return@launch
                }

                val billSaved = saveCartItemToBillView(
                    orderType = orderType,
                    sessionId = sessionId,
                    tableNo = tableNo,
                    cartItems = latestCart,
                    deviceId = deviceId,
                    deviceName = deviceName,
                    appVersion = "appVersion"
                )

                if (!billSaved) {
                    Log.e("WaiterKitchenVM", "❌ Bill save failed")
                    return@launch
                }

                repository.clearCart(orderType, tableNo)
                cartRepository.syncCartCount(tableNo)

                Log.d("WaiterKitchenVM", "✅ Firestore + Bill saved successfully")

                // ✅ MOVE HERE
                _sendSuccess.value = true

            } catch (e: Exception) {
                Log.e("WaiterKitchenVM", "❌ Error in waiterCartToBill: ${e.message}", e)
            }
        }





    }



    private suspend fun saveCartItemToBillView(
        orderType: String,
        sessionId: String,
        tableNo: String?,
        cartItems: List<PosCartEntity>,
        deviceId: String,
        deviceName: String?,
        appVersion: String?
    ): Boolean = withContext(Dispatchers.IO) {

        val tableNo = tableNo?: "";
        try {
            val db = AppDatabaseProvider.get(printerManager.appContext())
            val kotBatchDao = db.kotBatchDao()
            val kotItemDao = db.kotItemDao()

            val batchId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            repository.markAllSent(tableNo)

            val batch = PosKotBatchEntity(
                id = batchId,
                sessionId = sessionId,
                tableNo = tableNo,
                orderType = orderType,
                deviceId = deviceId,
                deviceName = deviceName,
                appVersion = appVersion,
                createdAt = now,
                sentBy = "WAITRER",
                syncStatus = "DONE",
                lastSyncedAt = null
            )

            kotBatchDao.insert(batch)

            val items = cartItems.map { cart ->
                PosKotItemEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    kotBatchId = batchId,
                    tableNo = tableNo,
                    productId = cart.productId,
                    name = cart.name,
                    categoryId = cart.categoryId,
                    categoryName = cart.categoryName,
                    parentId = cart.parentId,
                    isVariant = cart.isVariant,
                    basePrice = cart.basePrice,
                    quantity = cart.quantity,
                    taxRate = cart.taxRate,
                    taxType = cart.taxType,
                    note = cart.note,
                    modifiersJson = cart.modifiersJson,
                    kitchenPrinted = true,
                    status = "DONE",
                    createdAt = now
                )
            }

            kotRepository.insertItemsInBill(tableNo, items)


                kotRepository.markDoneAll(tableNo)
                kotRepository.syncKinchenCount(tableNo)
                kotRepository.syncBillCount(tableNo)


            true

        } catch (e: Exception) {
            Log.e("KOT", "❌ Failed to save KOT", e)
            false
        }
    }



    fun getPendingItems(orderRef: String, orderType: String): Flow<List<PosKotItemEntity>> {


        return if (orderType == "DINE_IN") {
            kotItemDao.getPendingItemsForTable(orderRef)
        } else {
            kotItemDao.getPendingItemsForTable(orderType)
          //  kotItemDao.getPendingItemsForSession(orderRef)
        }
    }
     // ✅ POS signal: kitchen completed for table



    fun sendSingleItemDirectlyToBill_Print_noPrint(
        cart: PosCartEntity,
        orderType: String,
        tableNo: String,
        sessionId: String,
        print: Boolean
    ) {

        viewModelScope.launch(Dispatchers.IO) {

            val db = AppDatabaseProvider.get(getApplication())
            val kotBatchDao = db.kotBatchDao()
            val kotItemDao = db.kotItemDao()

            val now = System.currentTimeMillis()
            val batchId = UUID.randomUUID().toString()

            // 🔹 Create batch (required for consistency)
            val batch = PosKotBatchEntity(
                id = batchId,
                sessionId = sessionId,
                tableNo = tableNo ?: orderType,
                orderType = orderType,
                deviceId = "dummy",
                deviceName = "dummy",
                appVersion = "dummy",
                createdAt = now,
                sentBy = "dummy",
                syncStatus = "DONE",
                lastSyncedAt = null
            )

            kotBatchDao.insert(batch)

            // 🔹 Create SINGLE KOT item → DONE
            val kotItem = PosKotItemEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                kotBatchId = batchId,
                tableNo = tableNo ?: orderType,
                productId = cart.productId,
                name = cart.name,
                categoryId = cart.categoryId,
                categoryName = cart.categoryName,
                parentId = cart.parentId,
                isVariant = cart.isVariant,
                basePrice = cart.basePrice,
                quantity = cart.quantity,
                taxRate = cart.taxRate,
                taxType = cart.taxType,
                note = cart.note,
                modifiersJson = cart.modifiersJson,
                status = "DONE",
                kitchenPrinted = false,
                createdAt = now
            )

            kotItemDao.insert(kotItem)
            Log.d("TABLE_DEBUG", "Cart to direct bill with print")

            //kotItemDao.getPendingItems(tableNo)

            // 🔹 Remove from cart after sending to bill
            //    cartViewModel.removeFromCart(cart.productId, tableNo)
            cartRepository.remove(cart.productId, tableNo)
            cartRepository.syncCartCount(tableNo)
            kotRepository.syncBillCount(tableNo)

           // logAllKotItems()
            // 🔹 Print if required
            if (print) {
                addItemToDebouncedKitchenPrint(kotItem, orderType)
                }
            kotItemDao.markPrinted(kotItem.id)



        }
    }

    private fun addItemToDebouncedKitchenPrint(
        item: PosKotItemEntity,
        orderType: String
    ) {
        synchronized(this) {
            pendingKotItems.add(item)
            if (pendingBatchId == null) {
                pendingBatchId = item.kotBatchId
            }
        }

        // Cancel previous timer
        kotPrintJob?.cancel()

        // Start / restart 10s timer
        kotPrintJob = viewModelScope.launch {
            delay(5_000) // ⏱️ 10 seconds

            val itemsToPrint: List<PosKotItemEntity>
            val batchId: String?

            synchronized(this@WaiterKitchenViewModel) {
                itemsToPrint = pendingKotItems.toList()
                batchId = pendingBatchId
                pendingKotItems.clear()
                pendingBatchId = null
            }

            if (itemsToPrint.isNotEmpty()) {
                printerManager.printTextKitchen(
                    PrinterRole.KITCHEN,
                    sessionKey = itemsToPrint.first().tableNo ?: batchId!!,
                    orderType = orderType,
                    items = itemsToPrint
                )

                // mark all printed
                val db = AppDatabaseProvider.get(getApplication())
                db.kotItemDao().markPrintedBatch(itemsToPrint.map { it.id })
            }
        }
    }

    fun resetSendSuccess() {
        _sendSuccess.value = false
    }

//    fun closeTable(
//        tableNo: String,
//        paymentType: String,
//        onSuccess: () -> Unit,
//        onError: (String) -> Unit
//    ) {
//        viewModelScope.launch {
//            try {
//
//                // 1️⃣ Take snapshot of bill items (NOT from mutable KOT table)
//                val billingItems = _uiState.value.items
//                if (billingItems.isEmpty()) {
//                    onError("No items in bill")
//                    return@launch
//                }
//
//                // 2️⃣ Save bill to OrderMaster / OrderDetails
//                saveOrder(billingItems, tableNo, paymentType)
//
//                // 3️⃣ Clear KOT items
//                kotItemDao.deleteByTable(tableNo)
//
//                // 4️⃣ Clear UI state
//                _uiState.value = _uiState.value.copy(items = emptyList())
//
//                // 5️⃣ Success → Go back
//                onSuccess()
//
//            } catch (e: Exception) {
//                onError("Error: ${e.message}")
//            }
//        }
//    }


}





