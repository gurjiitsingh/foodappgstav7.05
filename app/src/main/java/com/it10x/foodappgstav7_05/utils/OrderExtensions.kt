package com.it10x.foodappgstav7_05.utils

import com.it10x.foodappgstav7_05.data.online.models.OrderMasterData

fun OrderMasterData.createdAtMillis(): Long {
    return createdAt?.toDate()?.time ?: 0L
}