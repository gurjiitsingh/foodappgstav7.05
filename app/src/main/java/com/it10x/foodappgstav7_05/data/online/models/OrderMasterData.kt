package com.it10x.foodappgstav7_05.data.online.models

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * OrderMasterData (NEW SYSTEM)
 *
 * Only standard, clean fields.
 * Used for POS printing, Firestore, and Web orders.
 */
data class OrderMasterData(

    // =====================================================
    // CORE IDENTIFIERS
    // =====================================================
    var id: String = "",
    var srno: Int = 0,
    var source: String? = null,

    // =====================================================
    // CUSTOMER
    // =====================================================
    var customerId: String? = null,   // ✅ NEW
    var customerName: String = "",
    var email: String = "",
    var addressId: String = "",
    var customerPhone: String? = null,

    // =====================================================
    // CUSTOMER ADDRESS (PRINT SNAPSHOT)
    // =====================================================
    var dAddressLine1: String? = null,
    var dAddressLine2: String? = null,
    var dCity: String? = null,
    var dState: String? = null,
    var dZipcode: String? = null,
    var dLandmark: String? = null,

    // =====================================================
    // ORDER TYPE
    // =====================================================
    var orderType: String? = null,
    var tableNo: String? = null,

    // =====================================================
    // AMOUNTS
    // =====================================================
    var itemTotal: Double = 0.0,
    var subTotal: Double? = null,
    var discountTotal: Double? = null,
    var taxTotal: Double? = null,
    var deliveryFee: Double? = null,
    var grandTotal: Double? = null,

    // =====================================================
    // PAYMENT
    // =====================================================
    var paymentType: String = "",
    var paymentStatus: String? = null,

    // =====================================================
    // ORDER FLOW
    // =====================================================
    var orderStatus: String? = null,

    // =====================================================
    // OUTLET (MULTI-LOCATION)
    // =====================================================
    var outletId: String? = null,
    var outletName: String? = null,

    // =====================================================
    // POS HELPERS
    // =====================================================
    var productsCount: Int? = null,
    var notes: String? = null,

    // =====================================================
    // TIMESTAMPS
    // =====================================================
  //  var createdAt: Timestamp? = null,
    val createdAt: Any? = null,
    var localCreatedAt: Long? = null,
    // =====================================================
    // AUTOMATION
    // =====================================================
    var printed: Boolean? = null,
    var acknowledged: Boolean? = null,

    // =====================================================
    // SYNC CONTROL
    // =====================================================
    var syncStatus: String? = null,

    // =====================================================
    // LEGACY (KEEP)
    // =====================================================

   var couponFlat: Double? = null,
   var pickUpDiscount: Double? = null,
   var couponPercent: Double? = null,
)







// Extension to format timestamp for printing
fun OrderMasterData.formattedTime(): String {
    val millis = createdAtMillis()
    if (millis == 0L) return "--"

    val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
    return sdf.format(Date(millis))
}



fun OrderMasterData.createdAtMillis(): Long {
    return when (val value = createdAt) {
        is Timestamp -> value.toDate().time
        is Long -> value
        is Double -> value.toLong()
        else -> localCreatedAt ?: 0L
    }
}







fun OrderMasterData.fullDeliveryAddress(): String? {
    val parts = listOfNotNull(
        dAddressLine1,
        dAddressLine2,
        dLandmark,
        dCity,
        dState,
        dZipcode
    ).filter { it.isNotBlank() }

    return if (parts.isEmpty()) null else parts.joinToString(", ")
}
