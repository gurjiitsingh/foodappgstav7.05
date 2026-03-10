package com.it10x.foodappgstav7_05.utils


object MoneyUtils {

    fun toPaise(amount: Double): Long {
        return (amount * 100).toLong()
    }

    fun fromPaise(paise: Long): Double {
        return paise / 100.0
    }

}

