package com.example.banhangs.Activity

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.compose.ui.semantics.text
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope // For launching coroutines
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.banhangs.Adapter.CartAdapter
import com.example.banhangs.Model.OrderCreateModel
import com.example.banhangs.Model.OrderItemRequestData
import com.example.banhangs.R
import com.example.banhangs.Repository.CartRepository // Assuming you have this
import com.example.banhangs.ViewModel.CartViewModel
import com.example.banhangs.ViewModel.CartViewModelFactory
import com.example.banhangs.databinding.ActivityCartBinding
import com.example.banhangs.Model.VNPayModel // Your simple VNPayModel
import com.example.banhangs.Network.ApiResponse
import com.example.banhangs.Network.ApiService // Your Retrofit ApiService
import com.example.banhangs.Network.RetrofitClient // Or your way to get ApiService instance
import com.example.banhangs.Repository.UserPreferencesRepository
import com.example.banhangs.Utilities.VNPayUtils
import kotlinx.coroutines.launch // For coroutines
import retrofit2.Response
import java.net.InetAddress
import java.text.NumberFormat
import java.util.Locale
import kotlin.text.firstOrNull
import kotlin.text.isBlank
import kotlin.text.isNotBlank
import kotlinx.coroutines.flow.firstOrNull

class CartActivity : BaseActivity() {

    private lateinit var binding: ActivityCartBinding
    private lateinit var cartAdapter: CartAdapter

    private lateinit var userPreferencesRepository: UserPreferencesRepository

    // Setup for API backend
    private val apiService: ApiService by lazy { RetrofitClient.instance } // Or your DI method
    private val cartRepository: CartRepository by lazy {
        // Ensure userPreferencesRepository is initialized before this lazy block is executed for the first time
        CartRepository(apiService, userPreferencesRepository)
    }
    private val viewModel: CartViewModel by viewModels {
        CartViewModelFactory(application)
    }

    private var taxRate: Double = 0.02 // Example tax rate (2%)
    private var deliveryFee: Double = 15000.0 // Example delivery fee (15,000 VND)
    private var selectedPaymentMethod: Int = 1 // 1: Cash, 2: VNPay

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userPreferencesRepository = UserPreferencesRepository(applicationContext)

        // StrictMode for network calls on main thread (VNPay SDK might do this, or some older IP address logic)
        // Ideally, all network calls (including getIpAddress if it involves network) should be off the main thread.
        // For VNPay, the critical part is building the URL; the redirect is an Intent.
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        setupRecyclerView()
        setupUIListeners()
        observeViewModel()

        // ViewModel's init block should call loadCartItems()
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            mutableListOf(),
            onQuantityChanged = { productId, newQuantity ->
                viewModel.updateItemQuantity(productId, newQuantity)
            },
            onItemRemoved = { productId, currentQuantity ->
                viewModel.removeItem(productId, currentQuantity)
            }
        )
        binding.viewCart.apply {
            layoutManager = LinearLayoutManager(this@CartActivity)
            adapter = cartAdapter
            // itemAnimator = null // Optional: disable default animations if they cause issues
        }
    }

    private fun setupUIListeners() {
        binding.backBtn.setOnClickListener { finish() }

        binding.button.setOnClickListener { // Checkout button
            if (viewModel.cartItems.value.isNullOrEmpty()) {
                Toast.makeText(this, "Your cart is empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedPaymentMethod == 1) { // Cash
                // Call your backend API to place the order with "Cash" payment method
                placeOrderWithApi("Cash")
            } else { // VNPay
                // For VNPay, we typically redirect first, then confirm order on backend upon successful return.
                // Or, you can create a "pending" order on backend first.
                paymentVNPay()
            }
        }

        binding.metod1.setOnClickListener { selectPaymentMethodUI(1) }
        binding.method2.setOnClickListener { selectPaymentMethodUI(2) }
        selectPaymentMethodUI(1) // Default to cash
    }

    private fun selectPaymentMethodUI(method: Int) {
        selectedPaymentMethod = method
        val greenColor = ContextCompat.getColor(this, R.color.green) // Define R.color.green
        val blackColor = ContextCompat.getColor(this, R.color.black)
        val greyColor = ContextCompat.getColor(this, R.color.grey) // Define R.color.grey

        // Cash
        binding.metod1.setBackgroundResource(if (method == 1) R.drawable.green_bg_selected else R.drawable.grey_bg_selected)
        binding.metodIc1.imageTintList =
            ColorStateList.valueOf(if (method == 1) greenColor else blackColor)
        binding.methodtitle1.setTextColor(if (method == 1) greenColor else blackColor)
        binding.methodSubTitle1.setTextColor(if (method == 1) greenColor else greyColor)

        // VNPay
        binding.method2.setBackgroundResource(if (method == 2) R.drawable.green_bg_selected else R.drawable.grey_bg_selected)
        binding.metodIc2.imageTintList =
            ColorStateList.valueOf(if (method == 2) greenColor else blackColor)
        binding.methodtitle2.setTextColor(if (method == 2) greenColor else blackColor)
        binding.methodSubTitle2.setTextColor(if (method == 2) greenColor else greyColor)

        // Ensure you have drawables: green_bg_selected.xml and grey_bg_selected.xml
        // Example green_bg_selected.xml:
        // <shape xmlns:android="http://schemas.android.com/apk/res/android">
        //     <solid android:color="@color/light_green_background"/> <!-- Define this color -->
        //     <stroke android:width="2dp" android:color="@color/green"/>
        //     <corners android:radius="8dp"/>
        // </shape>
    }


    private fun observeViewModel() {
        viewModel.cartItems.observe(this) { items ->
            cartAdapter.updateItems(items)
            // Visibility of empty cart message and scroll view is handled by viewModel.isEmpty
        }

        viewModel.totalAmount.observe(this) { subtotal ->
            updatePriceSummary(subtotal)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.button.isEnabled = !isLoading // Disable checkout while loading cart
        }

        viewModel.isEmpty.observe(this) { isEmpty ->
            binding.emptyTxt.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.scrollView3.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }

        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.onErrorShown() // ViewModel should have a method to clear the error
            }
        }

        viewModel.toastMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.onToastShown() // ViewModel should have a method to clear the toast
            }
        }
    }

    private fun updatePriceSummary(subtotal: Double) {
        val tax = subtotal * taxRate
        val total = subtotal + tax + deliveryFee

        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN")) // VND currency format

        binding.totalFeeTxt.text = formatter.format(subtotal)
        binding.taxTxt.text = formatter.format(tax)
        binding.deliveryTxt.text = formatter.format(deliveryFee)
        binding.totalTxt.text = formatter.format(total)
    }

    private fun paymentVNPay() {
        val currentSubtotal = viewModel.totalAmount.value ?: 0.0
        if (currentSubtotal <= 0 && viewModel.cartItems.value.isNullOrEmpty()) {
            Toast.makeText(this, "Your cart is empty or total is zero.", Toast.LENGTH_SHORT)
                .show(); return
        }
        val finalTotalForPayment = currentSubtotal + (currentSubtotal * taxRate) + deliveryFee
        if (finalTotalForPayment <= 0) {
            Toast.makeText(
                this,
                "Cannot proceed with zero or negative total after fees.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val vnpTxnRef = VNPayUtils.generateVnpTxnRef() // Unique ID for this payment attempt
        val vnpAmount = (finalTotalForPayment * 100).toLong()
            .toString() // VNPay amount is in paisa/cents (Dong * 100)
        val vnpIpAddr = getIpAddress()
        val vnpOrderInfo = "Thanh toan don hang $vnpTxnRef" // Description for VNPay
        // 1. Create the VNPayModel instance (simple data holder)
        val vnPayModelData = VNPayModel(
            vnp_TmnCode = VNPayUtils.TMN_CODE,         // From VNPayUtils
            vnp_Amount = vnpAmount,
            vnp_TxnRef = vnpTxnRef,
            vnp_OrderInfo = vnpOrderInfo,
            vnp_ReturnUrl = VNPayUtils.RETURN_URL,     // From VNPayUtils
            vnp_IpAddr = vnpIpAddr
            // vnp_Version, vnp_Command, vnp_CurrCode, vnp_Locale, vnp_OrderType use defaults from VNPayModel
            // vnp_BankCode can be set here if you have bank selection UI: e.g., vnp_BankCode = "NCB"
        )
        // 2. Manually create the map of parameters for VNPayUtils.buildPaymentUrl
        // This map should only contain parameters that VNPayUtils.buildPaymentUrl expects
        // (i.e., excluding vnp_CreateDate, vnp_ExpireDate, vnp_SecureHash, as the util adds them)
        val paramsMapForUtils = mutableMapOf<String, String>()
        paramsMapForUtils["vnp_Version"] = vnPayModelData.vnp_Version
        paramsMapForUtils["vnp_Command"] = vnPayModelData.vnp_Command
        paramsMapForUtils["vnp_TmnCode"] = vnPayModelData.vnp_TmnCode
        paramsMapForUtils["vnp_Amount"] = vnPayModelData.vnp_Amount
        paramsMapForUtils["vnp_CurrCode"] = vnPayModelData.vnp_CurrCode
        paramsMapForUtils["vnp_TxnRef"] = vnPayModelData.vnp_TxnRef
        paramsMapForUtils["vnp_OrderInfo"] = vnPayModelData.vnp_OrderInfo
        paramsMapForUtils["vnp_OrderType"] = vnPayModelData.vnp_OrderType
        paramsMapForUtils["vnp_Locale"] = vnPayModelData.vnp_Locale
        paramsMapForUtils["vnp_ReturnUrl"] = vnPayModelData.vnp_ReturnUrl
        paramsMapForUtils["vnp_IpAddr"] = vnPayModelData.vnp_IpAddr
        vnPayModelData.vnp_BankCode?.let { // Add bank code only if it's not null and not blank
            if (it.isNotBlank()) paramsMapForUtils["vnp_BankCode"] = it
        }
        // 3. Build the final payment URL using VNPayUtils
        val paymentUrl = VNPayUtils.buildPaymentUrl(paramsMapForUtils)
        Log.d("VNPay", "Payment URL: $paymentUrl")
        // Optional: Before redirecting, you might want to create an order on your backend
        // with a "pending_vnpay_payment" status and associate vnpTxnRef with it.
        // This helps in tracking if the user abandons the VNPay page.
        // placeOrderWithApi("VNPay_Pending", vnpTxnRef) // Example call
        // 4. Redirect to VNPay
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(paymentUrl)
        startActivity(intent)
        // The result of VNPay transaction will be handled in onResume() via the RETURN_URL deep link.
    }

    private fun getIpAddress(): String {
        // This is a simplified IP address getter. For production, consider robustness.
        // Getting IP address can be tricky and might require network permissions.
        // Also, this might get the local network IP, not the public IP if behind NAT.
        // VNPay might also determine IP on their end.
        try {
            val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            val ipAddressInt = wifiManager.connectionInfo.ipAddress
            return InetAddress.getByAddress(
                ByteArray(4) { i -> (ipAddressInt shr (i * 8) and 0xFF).toByte() }
            ).hostAddress ?: "127.0.0.1" // Fallback
        } catch (e: Exception) {
            Log.e("IPAddress", "Failed to get IP Address", e)
            return "127.0.0.1" // Fallback IP
        }
    }

    override fun onResume() {
        super.onResume()
        // Handle VNPay return if this activity is configured for it (via AndroidManifest RETURN_URL)
        val data: Uri? = intent.data
        if (data != null && data.toString().startsWith(VNPayUtils.RETURN_URL)) {
            handleVNPayReturn(data)
            // Clear the intent data to prevent re-processing on subsequent onResume calls without a new intent
            intent.data = null // Very important!
        }
    }

    private fun handleVNPayReturn(data: Uri) {
        val responseCode = data.getQueryParameter("vnp_ResponseCode")
        val txnRef = data.getQueryParameter("vnp_TxnRef") // Your vnp_TxnRef
        val amount =
            data.getQueryParameter("vnp_Amount") // Amount in Dong (e.g., 1000000 for 10,000 VND)
        val transactionNo =
            data.getQueryParameter("vnp_TransactionNo") // VNPay's transaction number
        // You can get other parameters like vnp_BankCode, vnp_PayDate, etc.
        Log.i(
            "VNPayReturn",
            "Response Code: $responseCode, TxnRef: $txnRef, VNPay TxNo: $transactionNo, Amount: $amount"
        )
        if ("00" == responseCode) {
            // VNPay client-side indicates success.
            // NOW, THE CRITICAL STEP: VERIFY WITH YOUR BACKEND.
            // Your backend should make a server-to-server call to VNPay to confirm this transaction (using txnRef and other details).
            // This prevents tampering with client-side parameters.
            // For now, we'll assume client-side success means we can proceed to call our order placement API.
            // In a real app, you'd probably show a "Processing..." message,
            // call your backend to verify and finalize the order, then navigate.
            Toast.makeText(
                this,
                "VNPay Payment Successful (Client). Verifying with server...",
                Toast.LENGTH_LONG
            ).show()
            // Call your backend to verify the transaction and finalize the order
            // Pass txnRef, transactionNo, amount, etc., to your backend.
            // Your backend will then use VNPay's API to query the transaction status.
            verifyAndPlaceOrderWithApi("VNPay_Success", txnRef, transactionNo, amount)
        } else {
            // Payment failed or cancelled by user on VNPay page
            Toast.makeText(
                this,
                "VNPay Payment Failed or Cancelled (Code: $responseCode) for order $txnRef.",
                Toast.LENGTH_LONG
            ).show()
            // Optionally, update order status on backend to "Payment Failed" if an order was pre-created.
        }
    }

    /**
     * Placeholder for calling your backend API to create an order (e.g., for Cash payment).
     */
    private fun placeOrderWithApi(
        paymentMethodIdentifier: String, // e.g., "Cash", "VNPay_Success"
        vnpayTxnRefForLogging: String? = null
    ) {
        val cartItemsForOrder = viewModel.cartItems.value
        if (cartItemsForOrder.isNullOrEmpty()) {
            Toast.makeText(this, "Cart is empty, cannot place order.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val currentToken = userPreferencesRepository.userTokenFlow.firstOrNull()
            val currentUserId = userPreferencesRepository.userIdFlow.firstOrNull()

            if (currentToken == null || currentUserId == null) {
                Toast.makeText(this@CartActivity, "User not authenticated.", Toast.LENGTH_LONG).show()
                return@launch
            }

            // --- Fetch other customer details (ensure these are available) ---
            val customerName = userPreferencesRepository.userFullNameFlow.firstOrNull() ?: "N/A" // Replace with actual logic
            val customerPhoneNumber = userPreferencesRepository.userPhoneNumberFlow.firstOrNull() ?: "N/A" // Replace with actual logic
            val customerEmail = userPreferencesRepository.userEmailFlow.firstOrNull() ?: "N/A" // Replace with actual logic

            // --- Construct Order Items ---
            val orderItemsRequest = cartItemsForOrder.map { cartItem ->
                val currentPrice = cartItem.priceAtOrderTime // Assuming this exists on your CartItem
                val itemDiscount = cartItem.discount ?: 0.0
                val calculatedTotalPrice = (currentPrice * cartItem.quantity) - itemDiscount
                OrderItemRequestData(
                    productId = cartItem.productId,
                    quantity = cartItem.quantity,
                    priceAtOrderTime = currentPrice,
                    discount = itemDiscount,
                    totalPrice = calculatedTotalPrice
                    // orderId = null // Or some placeholder if backend insists, but ideally not needed for creation
                )
            }

            val shippingAddressStr = binding.shippingAddressEdt.text.toString()
            val voucherCodeStr = binding.voucherCodeEdt.text.toString().takeIf { it.isNotBlank() } // Assuming you add an EditText for voucher

            if (shippingAddressStr.isBlank()) {
                Toast.makeText(this@CartActivity, "Please enter a shipping address.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // --- Determine Payment Method Integer ---
            // This needs to map to what your backend expects for "Cash" or "VNPay"
            // Example: Let's say backend expects 0 for Cash, 1 for VNPay (you need to confirm this)
            val paymentMethodInt = when {
                paymentMethodIdentifier.equals("Cash", ignoreCase = true) -> 0 // Example value for Cash
                paymentMethodIdentifier.equals("VNPay_Success", ignoreCase = true) -> 1 // Example value for VNPay
                else -> -1 // Should not happen with current logic, but good for safety
            }

            if (paymentMethodInt == -1) {
                Toast.makeText(this@CartActivity, "Invalid payment method for API.", Toast.LENGTH_LONG).show()
                return@launch
            }


            val orderRequest = OrderCreateModel(
                customerId = currentUserId,
                customerName = customerName, // Ensure this is fetched correctly
                customerPhonenumber = customerPhoneNumber, // Ensure this is fetched correctly
                customerEmail = customerEmail,       // Ensure this is fetched correctly
                shippingAddress = shippingAddressStr,
                voucherCode = voucherCodeStr, // Pass the voucher code (can be null if optional)
                items = orderItemsRequest,
                paymentMethod = paymentMethodInt // Pass the integer payment method
            )

            // --- Determine which API endpoint to call ---
            // This part of your logic might need to change if the backend uses the same
            // endpoint for both COD and Pre-Paid and differentiates by the 'paymentMethod' field
            // in the request body. However, your previous code had distinct endpoints.
            // Let's assume distinct endpoints for now, as per your original structure.

            try {
                binding.button.isEnabled = false // Disable button during API call

                val response: Response<ApiResponse<String>> // Assuming ApiResponse<String> where String is Order ID

                // IMPORTANT: Verify if your backend uses different endpoints for COD vs. Pre-Pay
                // or if it's the same endpoint and the 'paymentMethod' field in OrderCreateModel
                // dictates the type. The provided spec was for "cod-order".
                // If it's the same endpoint for pre-pay, you'd just call that one.

                if (paymentMethodIdentifier.equals("Cash", ignoreCase = true)) {
                    Log.d(
                        "PlaceOrderAPI",
                        "Placing COD order for user $currentUserId: $orderRequest"
                    )
                    // Ensure apiService.createCodOrder expects the new OrderCreateModel
                    response = apiService.createCodOrder(
                        // The userId might be redundant in the path if it's already in OrderCreateModel.
                        // Check your API spec. If customerId in the body is enough,
                        // the {userId} path parameter for createCodOrder might be removable from the API definition.
                        // For now, keeping it as per your previous structure.
                        currentUserId, // Or remove if not needed in path and only in body
                        "Bearer $currentToken",
                        orderRequest
                    )
                } else if (paymentMethodIdentifier.equals("VNPay_Success", ignoreCase = true)) {
                    Log.d(
                        "PlaceOrderAPI",
                        "Placing Pre-Paid order for user $currentUserId (VNPay TxnRef for log: $vnpayTxnRefForLogging): $orderRequest"
                    )
                    // Ensure apiService.createPrePayOrder expects the new OrderCreateModel
                    // You'll need to confirm the request body for pre-pay orders.
                    // It might be identical to COD or slightly different.
                    // For this example, we'll assume it's the same OrderCreateModel.
                    response = apiService.createPrePayOrder( // Or a generic createOrder if endpoints are merged
                        currentUserId, // Or remove if not needed in path
                        "Bearer $currentToken",
                        orderRequest
                    )
                } else {
                    // This case should ideally not be reached if paymentMethodInt check is done correctly
                    Toast.makeText(
                        this@CartActivity,
                        "Unsupported payment method for API call.",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.button.isEnabled = true
                    return@launch // Exit coroutine
                }

                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (apiResponse.retCode == 0 && apiResponse.data != null) { // Assuming retCode 0 is success
                        val newOrderId = apiResponse.data // This is the Order ID string
                        Toast.makeText(
                            this@CartActivity,
                            "Order placed successfully! Order ID: $newOrderId",
                            Toast.LENGTH_LONG
                        ).show()

                        viewModel.clearCart() // This should also trigger backend cart clearing via repository

                        val intent = Intent(this@CartActivity, OrderSuccessActivity::class.java)
                        intent.putExtra("orderId", newOrderId)
                        startActivity(intent)
                        finish() // Finish CartActivity
                    } else {
                        val errorMessage = apiResponse.systemMessage
                            ?: "Failed to place order (API error code: ${apiResponse.retCode})"
                        Log.e(
                            "PlaceOrderAPI",
                            "API Error: $errorMessage - Full Response: $apiResponse"
                        )
                        Toast.makeText(this@CartActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("PlaceOrderAPI", "HTTP Error: ${response.code()} - $errorBody")
                    Toast.makeText(
                        this@CartActivity,
                        "Failed to place order: ${response.message()} ($errorBody)",
                        Toast.LENGTH_LONG
                    ).show()
                    // Consider parsing the errorBody JSON here to show more specific messages
                    // from the backend if it follows the same structure as the 400 validation errors.
                }
            } catch (e: Exception) {
                Log.e("PlaceOrderAPI", "Exception placing order", e)
                Toast.makeText(
                    this@CartActivity,
                    "Error placing order: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.button.isEnabled = true // Re-enable button
            }
        } // End of lifecycleScope.launch
    }

    // Ensure you have this function for the VNPay flow
    private fun verifyAndPlaceOrderWithApi(
        paymentMethodName: String, // Should be "VNPay_Success"
        vnpayTxnRef: String?,
        vnpayTransactionNo: String?, // For server-side verification
        vnpayAmount: String?         // For server-side verification
    ) {
        // CRITICAL: Implement server-side verification of VNPay transaction first.
        // This is a placeholder.
        Log.i(
            "VerifyAndPlaceOrder",
            "VNPay client success for $vnpayTxnRef. Proceeding to place order with backend."
        )
        Log.d(
            "VerifyAndPlaceOrder",
            "VNPay Details - TxnRef: $vnpayTxnRef, TxnNo: $vnpayTransactionNo, Amount: $vnpayAmount"
        )


        // Call the generic placeOrderWithApi function
        // The address and notes details should still be in the EditText fields
        // or retrieved from the ViewModel if you've saved them there before VNPay redirect.
        placeOrderWithApi(
            paymentMethodIdentifier = paymentMethodName, // "VNPay_Success"
            vnpayTxnRefForLogging = vnpayTxnRef
        )
    }
}
