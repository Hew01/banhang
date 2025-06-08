package com.example.banhangs.Model

data class OrderModel(
    val timestamp: Long = 0,
    val items: List<ProductDetailsModel> = emptyList(),
    val total: Double = 0.0,
    val status: String = ""
)