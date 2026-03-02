package com.it10x.foodappgstav7_05.data.pos.repository

import com.it10x.foodappgstav7_05.data.pos.dao.OutletDao
import com.it10x.foodappgstav7_05.data.print.OutletInfo
import com.it10x.foodappgstav7_05.data.print.OutletMapper

class OutletRepository(
    private val outletDao: OutletDao
) {

    suspend fun getOutletInfo(): OutletInfo {
        val outletEntity = outletDao.getOutlet()
        return OutletMapper.fromEntity(outletEntity)
    }

    suspend fun clearOutlet() {
        outletDao.deleteOutlet()
    }
}
