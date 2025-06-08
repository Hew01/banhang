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
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope // For launching coroutines
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.banhangs.Adapter.CartAdapter
import com.example.banhangs.R
import com.example.banhangs.Repository.CartRepository // Assuming you have this
import com.example.banhangs.ViewModel.CartViewModel
import com.example.banhangs.ViewModel.CartViewModelFactory
import com.example.banhangs.databinding.ActivityCartBinding
import com.example.banhangs.Model.VNPayModel // Your simple VNPayModel
import com.example.banhangs.Network.ApiService // Your Retrofit ApiService
import com.example.banhangs.Network.RetrofitClient // Or your way to get ApiService instance
import com.example.banhangs.Utilities.VNPayUtils
import kotlinx.coroutines.launch // For coroutines
import java.net.InetAddress
import java.text.NumberFormat
import java.util.Locale
import java.util.TreeMap // For sorting params for VNPay if not done in VNPayUtils

class CartActivity : BaseActivity() {

    private lateinit var binding: ActivityCartBinding
    private lateinit var cartAdapter: CartAdapter

    // Setup for API backend
    private val apiService: ApiService by lazy { RetrofitClient.instance } // Or your DI method
    private val cartRepository: CartRepository by lazy { CartRepository(apiService) }
    private val viewModel: CartViewModel by viewModels {
        CartViewModelFactory(cartRepository)
    }

    private var taxRate: Double = 0.02 // Example tax rate (2%)
    private var deliveryFee: Double = 15000.0 // Example delivery fee (15,000 VND)
    private var selectedPaymentMethod: Int = 1 // 1: Cash, 2: VNPay

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        binding.metodIc1.imageTintList = ColorStateList.valueOf(if (method == 1) greenColor else blackColor)
        binding.methodtitle1.setTextColor(if (method == 1) greenColor else blackColor)
        binding.methodSubTitle1.setTextColor(if (method == 1) greenColor else greyColor)

        // VNPay
        binding.method2.setBackgroundResource(if (method == 2) R.drawable.green_bg_selected else R.drawable.grey_bg_selected)
        binding.metodIc2.imageTintList = ColorStateList.valueOf(if (method == 2) greenColor else blackColor)
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
        if (currentSubtotal <= 0 && viewModel.cartItems.value.isNullOrEmpty()) { Toast.makeText(this,  "Your cart is empty or total is zero.", Toast.LENGTH_SHORT). show( );  return }
        val finalTotalForPayment = currentSubtotal + (currentSubtotal * taxRate) + deliveryFee
        if (finalTotalForPayment <=0) {
            Toast.makeText(this, "Cannot proceed with zero or negative total after fees.", Toast.LENGTH_SHORT).show()
            return
        }
        val vnpTxnRef = VNPayUtils.generateVnpTxnRef() // Unique ID for this payment attempt
        val vnpAmount = (finalTotalForPayment * 100).toLong().toString() // VNPay amount is in paisa/cents (Dong * 100)
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
        val amount = data.getQueryParameter("vnp_Amount") // Amount in Dong (e.g., 1000000 for 10,000 VND)
        val transactionNo = data.getQueryParameter("vnp_TransactionNo") // VNPay's transaction number
        // You can get other parameters like vnp_BankCode, vnp_PayDate, etc.
        Log.i("VNPayReturn", "Response Code: $responseCode, TxnRef: $txnRef, VNPay TxNo: $transactionNo, Amount: $amount")
        if ("00" == responseCode) {
            // VNPay client-side indicates success.
            // NOW, THE CRITICAL STEP: VERIFY WITH YOUR BACKEND.
            // Your backend should make a server-to-server call to VNPay to confirm this transaction (using txnRef and other details).
            // This prevents tampering with client-side parameters.
            // For now, we'll assume client-side success means we can proceed to call our order placement API.
            // In a real app, you'd probably show a "Processing..." message,
            // call your backend to verify and finalize the order, then navigate.
            Toast.makeText(this, "VNPay Payment Successful (Client). Verifying with server...", Toast.LENGTH_LONG).show()
            // Call your backend to verify the transaction and finalize the order
            // Pass txnRef, transactionNo, amount, etc., to your backend.
            // Your backend will then use VNPay's API to query the transaction status.
            verifyAndPlaceOrderWithApi("VNPay_Success", txnRef, transactionNo, amount)
        } else {
            // Payment failed or cancelled by user on VNPay page
            Toast.makeText(this, "VNPay Payment Failed or Cancelled (Code: $responseCode) for order $txnRef.", Toast.LENGTH_LONG).show()
            // Optionally, update order status on backend to "Payment Failed" if an order was pre-created.
        }
    }
    /**
     * Placeholder for calling your backend API to create an order (e.g., for Cash payment).
     */
    private fun placeOrderWithApi(paymentMethodName: String, vnpayTxnRef: String? = null) {
        val cartItemsForOrder = viewModel.cartItems.value
        if (cartItemsForOrder.isNullOrEmpty()) {
            Toast.makeText(this, "Cart is empty, cannot place order.", Toast.LENGTH_SHORT).show()
            return
        }
        val subtotal = viewModel.totalAmount.value ?: 0.0
        val orderTax = subtotal * taxRate
        val orderTotal = subtotal + orderTax + deliveryFee
// TODO: Construct your OrderRequest object based on your API's needs
// Example:
// val orderRequest = OrderRequest(
        //     userId = "current_user_id", // Get from your auth system
        //     items = cartItemsForOrder.map { cartItem ->
        //         OrderItemRequest(productId = cartItem.productId, quantity = cartItem.quantity, priceAtOrder = cartItem.priceAtOrderTime)
        //     },
        //     subtotal = subtotal,
        //     tax = orderTax,
        //     deliveryFee = deliveryFee,
        //     totalAmount = orderTotal,
        //     paymentMethod = paymentMethodName,
        //     vnpayTransactionRef = vnpayTxnRef, // Include if it's a VNPay pending order
        //     customerNotes = "Some notes here" // If you have this field
        // )

        lifecycleScope.launch { // Use lifecycleScope for coroutines in Activity
            try {
                // Show loading indicator
                binding.button.isEnabled = false

                // Replace with your actual API call
                // val response = apiService.createOrder(orderRequest) // Assuming createOrder endpoint

                // MOCK DELAY - REMOVE THIS IN REAL APP
                kotlinx.coroutines.delay(2000)
                val mockSuccess = true // Simulate API success
                val mockOrderId = "MOCK_ORDER_${System.currentTimeMillis()}"


                // if (response.isSuccessful && response.body() != null) { // For real API
                if (mockSuccess) { // For mock
                    // val orderConfirmation = response.body()!! // For real API
                    Toast.makeText(this@CartActivity, "Order placed successfully! Order ID: $mockOrderId", Toast.LENGTH_LONG).show()
                    viewModel.clearCart() // Clear cart in ViewModel (which should call repository to clear on backend)

                    val intent = Intent(this@CartActivity, OrderSuccessActivity::class.java)
                    intent.putExtra("orderId", mockOrderId) // Pass order ID to success screen
                    startActivity(intent)
                    finish() // Finish CartActivity
                } else {
                    // val errorBody = response.errorBody()?.string() ?: "Unknown error" // For real API
                    val errorBody = "Failed to place order (mock error)" // For mock
                    Toast.makeText(this@CartActivity, "Failed to place order: $errorBody", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("PlaceOrderAPI", "Error placing order", e)
                Toast.makeText(this@CartActivity, "Error placing order: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                // Hide loading indicator
                binding.button.isEnabled = true
            }
        }
    }

    /**
     * Placeholder for calling your backend API to verify VNPay transaction and then place/finalize the order.
     */
    private fun verifyAndPlaceOrderWithApi(
        paymentMethodName: String,
        vnpayTxnRef: String?,
        vnpayTransactionNo: String?,
        vnpayAmount: String?
    ) {
        val cartItemsForOrder = viewModel.cartItems.value
        if (cartItemsForOrder.isNullOrEmpty() && paymentMethodName.contains("VNPay")) {
            // This might happen if the cart was cleared prematurely or state was lost.
            // For VNPay, the txnRef is key. Your backend should be able to fetch order details
            // if an order was created in a "pending" state using this txnRef.
            Log.w("VerifyOrderAPI", "Cart is empty during VNPay verification for $vnpayTxnRef. Backend should handle.")
        }

        val subtotal = viewModel.totalAmount.value ?: 0.0 // This might be stale if cart was cleared.
        val orderTax = subtotal * taxRate
        val orderTotal = subtotal + orderTax + deliveryFee

        // TODO: Construct your VerifyAndPlaceOrderRequest object
        // This request would go to your backend. Your backend then:
        // 1. Calls VNPay's queryDR endpoint using vnpayTxnRef to confirm the transaction status and amount.
        // 2. If VNPay confirms success and amount matches, your backend finalizes the order (or creates it if not pending).
        // Example:
        // val verifyRequest = VerifyVnpayOrderRequest(
        //     vnpayTransactionReference = vnpayTxnRef,
        //     vnpayTransactionNumber = vnpayTransactionNo,
        //     vnpayAmountReported = vnpayAmount, // Amount from VNPay return (e.g., "1000000")
        //     // Include cart items or original order details if your backend needs them for verification/creation
        //     items = cartItemsForOrder?.map { ... } // if needed
        // )

        lifecycleScope.launch {
            try {
                binding.button.isEnabled = false

                // Replace with your actual API call to your backend verification endpoint
                // val response = apiService.verifyAndFinalizeOrder(verifyRequest)

                // MOCK DELAY & RESPONSE - REMOVE THIS
                kotlinx.coroutines.delay(2500)
                val mockVerificationSuccess = true
                val mockVerifiedOrderId = vnpayTxnRef ?: "MOCK_VNPay_${System.currentTimeMillis()}"

                // if (response.isSuccessful && response.body()?.orderConfirmed == true) { // For real API
                if (mockVerificationSuccess) { // For mock
                    // val orderConfirmation = response.body()!! // For real API
                    Toast.makeText(this@CartActivity, "Order $mockVerifiedOrderId confirmed via VNPay!", Toast.LENGTH_LONG).show()
                    viewModel.clearCart() // Clear cart in ViewModel

                    val intent = Intent(this@CartActivity, OrderSuccessActivity::class.java)
                    intent.putExtra("orderId", mockVerifiedOrderId)
                    startActivity(intent)
                    finish()
                } else {
                    // val errorMsg = response.body()?.message ?: response.errorBody()?.string() ?: "VNPay verification failed on server." // For real API
                    val errorMsg = "VNPay verification failed on server (mock)." // For mock
                    Toast.makeText(this@CartActivity, errorMsg, Toast.LENGTH_LONG).show()
                    // Potentially update order status on backend to "Payment Verification Failed"
                }
            } catch (e: Exception) {
                Log.e("VerifyOrderAPI", "Error verifying/placing VNPay order", e)
                Toast.makeText(this@CartActivity, "Error verifying VNPay order: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.button.isEnabled = true
            }
        }
    }
}