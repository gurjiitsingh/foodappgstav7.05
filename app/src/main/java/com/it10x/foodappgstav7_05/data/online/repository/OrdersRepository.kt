package com.it10x.foodappgstav7_05.data.online.models.repository

import com.it10x.foodappgstav7_05.data.online.models.OrderMasterData
import com.it10x.foodappgstav7_05.data.online.models.OrderProductData
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

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


}
