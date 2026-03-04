package com.it10x.foodappgstav7_05.data.pos.dao

import androidx.room.*
import com.it10x.foodappgstav7_05.data.pos.entities.VirtualTableEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VirtualTableDao {

    @Query("SELECT * FROM virtual_tables WHERE orderType = :type ORDER BY createdAt ASC")
    fun observeByType(type: String): Flow<List<VirtualTableEntity>>

    @Query("SELECT * FROM virtual_tables WHERE orderType = :type ORDER BY createdAt ASC")
    suspend fun getByType(type: String): List<VirtualTableEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(table: VirtualTableEntity)

    @Update
    suspend fun update(table: VirtualTableEntity)

    @Query("DELETE FROM virtual_tables WHERE id = :id")
    suspend fun deleteById(id: String)
}