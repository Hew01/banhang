package com.example.banhangs.Model // Or your model package

/**
 * A simple data class to hold VNPay parameters before they are put into a map
 * for URL construction. This aligns with a more "classic" setup where the Activity
 * handles the parameter preparation.
 */
data class VNPayModel(
    // Fields typically set with defaults or from VNPayUtils
    val vnp_Version: String = "2.1.0",
    val vnp_Command: String = "pay",
    val vnp_TmnCode: String, // To be set from VNPayUtils.TMN_CODE

    // Fields set from cart/order details
    val vnp_Amount: String,    // Amount as String (e.g., "1000000" for 10,000 VND)
    val vnp_CurrCode: String = "VND",
    val vnp_TxnRef: String,    // Unique transaction reference from VNPayUtils.generateVnpTxnRef()
    val vnp_OrderInfo: String, // Order description
    val vnp_OrderType: String = "other", // Default order type

    // Fields related to network and redirection
    val vnp_Locale: String = "vn",
    val vnp_ReturnUrl: String, // To be set from VNPayUtils.RETURN_URL
    val vnp_IpAddr: String,    // Customer's IP address

    // Optional field
    val vnp_BankCode: String? = null // Specific bank code if chosen by user
)
// No toMap() or toQueryString() methods needed here.
// CartActivity will handle creating the map for VNPayUtils.buildPaymentUrl.