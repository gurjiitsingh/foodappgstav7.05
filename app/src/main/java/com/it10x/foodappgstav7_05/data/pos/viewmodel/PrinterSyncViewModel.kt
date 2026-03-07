package com.it10x.foodappgstav7_05.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.it10x.foodappgstav7_05.data.online.sync.PrinterSyncRepository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PrinterSyncViewModel(
    private val repo: PrinterSyncRepository
) : ViewModel() {

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing

    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status

    fun syncPrinters() {

        viewModelScope.launch {

            try {

                _syncing.value = true
                _status.value = "Syncing printers..."

                repo.downloadPrinters()

                _status.value = "Printers synced"

            } catch (e: Exception) {

                _status.value = "Printer sync failed: ${e.message}"

            } finally {

                _syncing.value = false
            }
        }
    }
}
