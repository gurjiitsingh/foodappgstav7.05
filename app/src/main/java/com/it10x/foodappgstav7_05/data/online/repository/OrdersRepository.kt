package com.it10x.foodappgstav7_05.data.online.models.repository

import com.it10x.foodappgstav7_05.data.online.models.OrderMasterData
import com.it10x.foodappgstav7_05.data.online.models.OrderProductData
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Date
import com.it10x.foodappgstav7_05.utils.createdAtMillis
class OrdersRepository {

    private val db = FirebaseFirestore.getInstance()

    // -----------------------------
    // PAGINATION STATE
    // -----------------------------
    private val pageAnchors = mutableListOf<DocumentSnapshot>()
    private var lastDocument: DocumentSnapshot? = null

    fun resetPagination() {
        pageAnchors.clear()
        lastDocument = null
    }

    // -----------------------------
    // ORDER MASTER
    // -----------------------------
    suspend fun getFirstPage(limit: Long = 10): List<OrderMasterData> {
        return try {
            // 🧠 Try the fast, indexed query first
            val snapshot = db.collection("orderMaster")
                .whereIn("source", listOf("WEB", "APP", "ONLINE"))
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()

            val docs = snapshot.documents
            if (docs.isNotEmpty()) {
                pageAnchors.clear()
                pageAnchors.add(docs.first())
                lastDocument = docs.last()
            }

            docs.mapNotNull { it.toObject(OrderMasterData::class.java)?.copy(id = it.id) }

        } catch (e: Exception) {
            // ⚠️ Fallback if index is missing or Firestore fails
            android.util.Log.w("ORDER_FETCH", "Indexed query failed, falling back: ${e.message}")

            val snapshot = db.collection("orderMaster")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit * 3)
                .get()
                .await()

            val allOrders = snapshot.documents.mapNotNull {
                it.toObject(OrderMasterData::class.java)?.copy(id = it.id)
            }

            // ✅ Local filter for online/web orders
            val filteredOrders = allOrders.filter {
                it.source?.uppercase() in listOf("WEB", "APP", "ONLINE")
            }.take(limit.toInt())

            filteredOrders
        }
    }


    suspend fun getNextPage(limit: Long = 10): List<OrderMasterData> {
        if (lastDocument == null) return emptyList()

        return try {
            val snapshot = db.collection("orderMaster")
                .whereIn("source", listOf("WEB", "APP", "ONLINE"))
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .startAfter(lastDocument!!)
                .limit(limit)
                .get()
                .await()

            val docs = snapshot.documents
            if (docs.isNotEmpty()) {
                pageAnchors.add(docs.first())
                lastDocument = docs.last()
            }

            docs.mapNotNull { it.toObject(OrderMasterData::class.java)?.copy(id = it.id) }

        } catch (e: Exception) {
            android.util.Log.w("ORDER_FETCH", "Next page fallback: ${e.message}")

            val snapshot = db.collection("orderMaster")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .startAfter(lastDocument!!)
                .limit(limit * 3)
                .get()
                .await()

            val allOrders = snapshot.documents.mapNotNull {
                it.toObject(OrderMasterData::class.java)?.copy(id = it.id)
            }

            val filteredOrders = allOrders.filter {
                it.source?.uppercase() in listOf("WEB", "APP", "ONLINE")
            }.take(limit.toInt())

            filteredOrders
        }
    }


    suspend fun getPrevPage(limit: Long = 10): List<OrderMasterData> {
        if (pageAnchors.size < 2) return emptyList()

        // Move one page back
        pageAnchors.removeLast()
        val prevAnchor = pageAnchors.last()

        return try {
            // 🧠 Try indexed query first
            val snapshot = db.collection("orderMaster")
                .whereIn("source", listOf("WEB", "APP", "ONLINE"))
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .startAt(prevAnchor)
                .limit(limit)
                .get()
                .await()

            val docs = snapshot.documents
            if (docs.isNotEmpty()) {
                lastDocument = docs.last()
            }

            docs.mapNotNull { it.toObject(OrderMasterData::class.java)?.copy(id = it.id) }

        } catch (e: Exception) {
            // ⚠️ Fallback: no index → fetch more + filter locally
            android.util.Log.w("ORDER_FETCH", "Prev page fallback: ${e.message}")

            val snapshot = db.collection("orderMaster")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .startAt(prevAnchor)
                .limit(limit * 3)
                .get()
                .await()

            val allOrders = snapshot.documents.mapNotNull {
                it.toObject(OrderMasterData::class.java)?.copy(id = it.id)
            }

            // ✅ Local filter: only show online/web/app orders
            val filteredOrders = allOrders.filter {
                it.source?.uppercase() in listOf("WEB", "APP", "ONLINE")
            }.take(limit.toInt())

            if (filteredOrders.isNotEmpty()) {
                lastDocument = snapshot.documents.lastOrNull()
            }

            filteredOrders
        }
    }


    // -----------------------------
    // ORDER PRODUCTS (ITEMS) ✅ NEW
    // -----------------------------
    suspend fun getOrderProducts(orderMasterId: String): List<OrderProductData> {

     //  Log.d("ORDER_REPO", "Fetching items for orderId=$orderMasterId")

        val snapshot = db.collection("orderProducts")
            .whereEqualTo("orderMasterId", orderMasterId)
            .get()
            .await()

        return snapshot.documents.mapNotNull {
            it.toObject(OrderProductData::class.java)?.copy(id = it.id)
        }
    }

    suspend fun markOrderAsPrinted(orderId: String) {
        db.collection("orderMaster")
            .document(orderId)
            .update("printed", true)
            .await()
    }



    suspend fun searchOrdersByDate(
        startMillis: Long,
        endMillis: Long,
        limit: Long = 20
    ): List<OrderMasterData> {

        return try {

            val startTimestamp = com.google.firebase.Timestamp(Date(startMillis))
            val endTimestamp = com.google.firebase.Timestamp(Date(endMillis))

            val snapshot = db.collection("orderMaster")
                .whereIn("source", listOf("WEB", "APP", "ONLINE"))
                .whereGreaterThanOrEqualTo("createdAt", startTimestamp)
                .whereLessThanOrEqualTo("createdAt", endTimestamp)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()

            snapshot.documents.mapNotNull {
                it.toObject(OrderMasterData::class.java)?.copy(id = it.id)
            }

        } catch (e: Exception) {

            android.util.Log.e("ORDER_SEARCH", "Date search failed", e)
            emptyList()

        }
    }





    suspend fun getFirstPagePOS(limit: Long = 10): List<OrderMasterData> {

        return try {

            val snapshot = db.collection("orderMaster")
                .whereEqualTo("source", "POS")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()

            val docs = snapshot.documents

            if (docs.isNotEmpty()) {
                pageAnchors.clear()
                pageAnchors.add(docs.first())
                lastDocument = docs.last()
            }

            docs.mapNotNull {
                it.toObject(OrderMasterData::class.java)?.copy(id = it.id)
            }

        } catch (e: Exception) {

            android.util.Log.e("POS_HISTORY", "First page failed", e)
            emptyList()

        }
    }



    suspend fun getNextPagePOS(limit: Long = 10): List<OrderMasterData> {

        if (lastDocument == null) return emptyList()

        return try {

            val snapshot = db.collection("orderMaster")
                .whereEqualTo("source", "POS")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .startAfter(lastDocument!!)
                .limit(limit)
                .get()
                .await()

            val docs = snapshot.documents

            if (docs.isNotEmpty()) {
                pageAnchors.add(docs.first())
                lastDocument = docs.last()
            }

            docs.mapNotNull {
                it.toObject(OrderMasterData::class.java)?.copy(id = it.id)
            }

        } catch (e: Exception) {

            android.util.Log.e("POS_HISTORY", "Next page failed", e)
            emptyList()

        }
    }


    suspend fun getPrevPagePOS(limit: Long = 10): List<OrderMasterData> {

        if (pageAnchors.size < 2) return emptyList()

        pageAnchors.removeLast()
        val prevAnchor = pageAnchors.last()

        return try {

            val snapshot = db.collection("orderMaster")
                .whereEqualTo("source", "POS")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .startAt(prevAnchor)
                .limit(limit)
                .get()
                .await()

            val docs = snapshot.documents

            if (docs.isNotEmpty()) {
                lastDocument = docs.last()
            }

            docs.mapNotNull {
                it.toObject(OrderMasterData::class.java)?.copy(id = it.id)
            }

        } catch (e: Exception) {

            android.util.Log.e("POS_HISTORY", "Prev page failed", e)
            emptyList()

        }
    }

    suspend fun searchPOSOrdersByDateLocal(
        startMillis: Long,
        endMillis: Long
    ): List<OrderMasterData> {

        return try {

            val snapshot = db.collection("orderMaster")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            android.util.Log.d("POS_HISTORY", "Total docs fetched: ${snapshot.size()}")

            val orders = snapshot.documents.mapNotNull {
                it.toObject(OrderMasterData::class.java)?.copy(id = it.id)
            }

//            val filtered = orders.filter {
//
//                val created = it.createdAtMillis()
//
//                created in startMillis..endMillis &&
//                        it.source == "POS"
//            }

            val filtered = orders.filter {

                val created = it.createdAtMillis()

                created in startMillis..endMillis &&
                        it.source == "POS"
            }

            android.util.Log.d("POS_HISTORY", "Filtered orders: ${filtered.size}")

            filtered

        } catch (e: Exception) {

            android.util.Log.e("POS_HISTORY", "Local date search failed", e)
            emptyList()

        }
    }


    // -----------------------------
// SIMPLE HISTORY TEST (NO FILTER)
// -----------------------------
    suspend fun getHistoryOrdersTest(limit: Long = 20): List<OrderMasterData> {

        return try {

            android.util.Log.d("HISTORY_TEST", "Fetching history orders...")

            val snapshot = db.collection("orderMaster")
                .limit(limit)
                .get()
                .await()

            android.util.Log.d("HISTORY_TEST", "Documents found: ${snapshot.size()}")

            snapshot.documents.forEach { doc ->
                android.util.Log.d(
                    "HISTORY_TEST",
                    "DocID=${doc.id} | createdAt=${doc.get("createdAt")} | source=${doc.get("source")}"
                )
            }

            val orders = snapshot.documents.mapNotNull {
                it.toObject(OrderMasterData::class.java)?.copy(id = it.id)
            }

            android.util.Log.d("HISTORY_TEST", "Mapped orders count: ${orders.size}")

            orders

        } catch (e: Exception) {

            android.util.Log.e("HISTORY_TEST", "History fetch failed", e)
            emptyList()

        }
    }


    suspend fun searchPOSOrdersByDate(
        startMillis: Long,
        endMillis: Long,
        limit: Long = 50
    ): List<OrderMasterData> {

        return try {

            android.util.Log.d("POS_HISTORY", "Fetching orders without Firestore date filter")

            val snapshot = db.collection("orderMaster")
                .whereEqualTo("source", "POS")
                .limit(limit)
                .get()
                .await()

            val orders = snapshot.documents.mapNotNull {
                it.toObject(OrderMasterData::class.java)?.copy(id = it.id)
            }

            // Filter locally (NO Firestore index needed)
            val filtered = orders.filter {

                val created = it.createdAtMillis()

                created in startMillis..endMillis
            }

            android.util.Log.d(
                "POS_HISTORY",
                "Orders fetched: ${orders.size} | Filtered: ${filtered.size}"
            )

            filtered

        } catch (e: Exception) {

            android.util.Log.e("POS_HISTORY", "POS Date search failed", e)

            emptyList()
        }
    }

}
