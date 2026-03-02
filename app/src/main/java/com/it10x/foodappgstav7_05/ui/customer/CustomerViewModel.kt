package com.it10x.foodappgstav7_05.ui.customer

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import com.it10x.foodappgstav7_05.data.pos.entities.PosCustomerEntity
import com.it10x.foodappgstav7_05.data.pos.repository.CustomerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CustomerViewModel(
    private val repository: CustomerRepository
) : ViewModel() {

    private val _customers = MutableStateFlow<List<PosCustomerEntity>>(emptyList())
    val customers: StateFlow<List<PosCustomerEntity>> = _customers

    fun search(query: String) {
        viewModelScope.launch {
            _customers.value = repository.search(query)
        }
    }

    fun loadAll() {
        Log.e("CREDIT", "Load all called")
        viewModelScope.launch {
            _customers.value = repository.search("")
        }
    }
}
