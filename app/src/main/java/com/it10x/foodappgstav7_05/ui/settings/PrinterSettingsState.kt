package com.it10x.foodappgstav7_05.ui.settings

import com.it10x.foodappgstav7_05.data.PrinterConfig
import com.it10x.foodappgstav7_05.data.PrinterRole

data class PrinterSettingsState(
    val printers: Map<PrinterRole, PrinterConfig> = emptyMap()
)
