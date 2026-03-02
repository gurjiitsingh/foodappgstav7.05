package com.it10x.foodappgstav7_05.ui.payment

data class PaymentInput(
    val mode: String,       // CASH | UPI | CARD | CREDIT
    val amount: Double
)


