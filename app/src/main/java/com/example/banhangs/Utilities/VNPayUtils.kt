package com.example.banhangs.Utilities // Or your preferred package

import android.net.Uri
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object VNPayUtils {
    // --- TODO: REPLACE WITH YOUR ACTUAL VALUES FROM VNPAY PORTAL ---
    const val TMN_CODE = "LMMFFI1R"
    private const val HASH_SECRET = "8M180BJMNXLWKZTUH6HLAHSB4AJU5FIP"
    // --- TODO: CONFIGURE THIS IN ANDROIDMANIFEST.XML ---
    const val RETURN_URL = "app://com.example.banhangs/vnpay_return" // Example, ensure it's unique

    /**
     * Generates a unique transaction reference for VNPay (vnp_TxnRef).
     * Example: "20231027103055ABCDEF"
     */
    fun generateVnpTxnRef(): String {
        val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date())
        // Generate a short random string to append for further uniqueness
        val allowedChars = ('A'..'Z') + ('0'..'9') // VNPay often prefers uppercase
        val randomSuffix = (1..6)
            .map { allowedChars.random() }
            .joinToString("")
        return "$timestamp$randomSuffix"
    }

    /**
     * Builds the final VNPay payment URL including the secure hash.
     * @param paramsMap A map of VNPay parameters (e.g., vnp_Amount, vnp_OrderInfo, etc.)
     *                  This map should NOT contain vnp_CreateDate, vnp_ExpireDate, or vnp_SecureHash yet.
     *                  It should already contain vnp_TmnCode, vnp_Version.
     */
    fun buildPaymentUrl(paramsMap: Map<String, String>): String {
        val vnp_CreateDate = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date())
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, 15) // Payment expires in 15 minutes
        val vnp_ExpireDate = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(calendar.time)

        val finalParams = paramsMap.toMutableMap() // Start with parameters passed in
        finalParams["vnp_CreateDate"] = vnp_CreateDate
        finalParams["vnp_ExpireDate"] = vnp_ExpireDate

        // VNPay requires parameters to be sorted alphabetically for hashing
        val sortedParams = TreeMap(finalParams)
        val hashDataBuilder = StringBuilder()
        val queryBuilder = StringBuilder()

        sortedParams.forEach { (key, value) ->
            if (value.isNotBlank()) {
                // Build hash data: key1=value1&key2=value2...
                hashDataBuilder.append(key)
                hashDataBuilder.append('=')
                hashDataBuilder.append(URLEncoder.encode(value, StandardCharsets.UTF_8.toString()))
                hashDataBuilder.append('&')

                // Build query string for URL
                queryBuilder.append(URLEncoder.encode(key, StandardCharsets.UTF_8.toString()))
                queryBuilder.append('=')
                queryBuilder.append(URLEncoder.encode(value, StandardCharsets.UTF_8.toString()))
                queryBuilder.append('&')
            }
        }

        // Remove the last '&' from hashData and query
        val hashData = if (hashDataBuilder.isNotEmpty()) hashDataBuilder.substring(0, hashDataBuilder.length - 1) else ""
        val queryString = if (queryBuilder.isNotEmpty()) queryBuilder.substring(0, queryBuilder.length - 1) else ""

        val vnp_SecureHash = hmacSHA512(HASH_SECRET, hashData)

        // VNPay URL (use sandbox for testing, production URL is different)
        val paymentBaseUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"
        // For production: "https://vnpayment.vn/paymentv2/vpcpay.html"

        return "$paymentBaseUrl?$queryString&vnp_SecureHash=$vnp_SecureHash"
    }

    private fun hmacSHA512(key: String, data: String): String {
        try {
            val sha512Hmac = Mac.getInstance("HmacSHA512")
            val secretKey = SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), "HmacSHA512")
            sha512Hmac.init(secretKey)
            val bytes = sha512Hmac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
            return bytesToHex(bytes)
        } catch (e: Exception) {
            throw RuntimeException("Failed to generate HMACSHA512: ${e.message}", e)
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = "0123456789abcdef"[v ushr 4]
            hexChars[j * 2 + 1] = "0123456789abcdef"[v and 0x0F]
        }
        return String(hexChars)
    }
}