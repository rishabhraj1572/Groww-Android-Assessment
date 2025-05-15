package com.example.stockapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class StockData(
    val symbol: String,
    val price: String,
    val change:String
) : Parcelable
