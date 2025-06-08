package com.example.banhangs.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.banhangs.Adapter.OrderAdapter
import com.example.banhangs.Model.OrderData // Use the new API model
import com.example.banhangs.Model.UserData // Assuming this is your user model from login
import com.example.banhangs.R
import com.example.banhangs.ViewModel.MyOrderViewModel // Create this ViewModel
import com.example.banhangs.databinding.ActivityMyOrderBinding
import com.example.banhangs.Helper.SessionManager // Assuming you have this

class MyOrderActivity : BaseActivity() {
    private lateinit var binding: ActivityMyOrderBinding
    private val viewModel: MyOrderViewModel by viewModels()
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var sessionManager: SessionManager
    private val TAG = "MyOrderActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "onCreate started")

        sessionManager = SessionManager(this)

        setupToolbar() // Replaces old backBtn logic
        initOrderList()
        observeViewModel()

        if (sessionManager.fetchAuthToken() == null) {
            Toast.makeText(this, "Please log in to view orders.", Toast.LENGTH_LONG).show()
            // Optionally redirect to LoginActivity
            // startActivity(Intent(this, LoginActivity::class.java))
            // finish()
            binding.emptyOrdersText.text = "Please log in to view your orders."
            binding.emptyOrdersText.visibility = View.VISIBLE
            return
        }

        loadProfileDataFromSession()
        viewModel.loadOrders()
        Log.d(TAG, "Initial data load triggered")
    }

    private fun setupToolbar() {
        // Assuming your ActivityMyOrderBinding has a Toolbar with id 'toolbar'
        // If not, you need to add <androidx.appcompat.widget.Toolbar android:id="@+id/toolbar" ... />
        // to your activity_my_order.xml
        setSupportActionBar(binding.toolbar) // Use the toolbar from binding
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "My Orders" // Or use a string resource R.string.my_orders

        binding.toolbar.setNavigationOnClickListener {
            // This will mimic the back button press
            onBackPressedDispatcher.onBackPressed()
        }
        // The old binding.backBtn can be removed from the XML or hidden
        binding.backBtn.visibility = View.GONE
    }


    private fun loadProfileDataFromSession() {
        // Fetch user details stored during login by SessionManager
        // This assumes SessionManager has methods to get individual fields or a UserData object
        val userName = sessionManager.fetchUserFullName() ?: "N/A"
        val userAddress = sessionManager.fetchUserAddress() ?: "Chưa cập nhật"
        val userPhone = sessionManager.fetchUserPhoneNumber() ?: "Chưa cập nhật"

        binding.nameTxt.text = "Tên: $userName"
        binding.addressTxt.text = "Địa chỉ: $userAddress"
        binding.phoneTxt.text = "Số điện thoại: $userPhone"
        Log.d(TAG, "Profile loaded from SessionManager: Name=$userName, Address=$userAddress, Phone=$userPhone")

        if (userAddress == "Chưa cập nhật" || userPhone == "Chưa cập nhật") {
            Toast.makeText(this, "Vui lòng cập nhật địa chỉ và số điện thoại trong hồ sơ", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initOrderList() {
        orderAdapter = OrderAdapter(mutableListOf()) // Initialize with empty list
        binding.orderRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.orderRecyclerView.adapter = orderAdapter
        Log.d(TAG, "Order RecyclerView initialized")
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this, Observer { isLoading ->
            Log.d(TAG, "isLoading changed: $isLoading")
        })

        viewModel.errorMessage.observe(this, Observer { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                binding.emptyOrdersText.text = it // Show error message in the empty text view
                binding.emptyOrdersText.visibility = View.VISIBLE
                binding.orderRecyclerView.visibility = View.GONE
                Log.e(TAG, "Error observed: $it")
            }
        })

        viewModel.orders.observe(this, Observer { orders ->
            if (orders.isNullOrEmpty()) {
                if (!viewModel.isLoading.value!! && viewModel.errorMessage.value == null) { // Only show if not loading and no error
                    binding.emptyOrdersText.text = "You have no orders yet."
                    binding.emptyOrdersText.visibility = View.VISIBLE
                    binding.orderRecyclerView.visibility = View.GONE
                    Log.d(TAG, "Orders list is empty or null")
                }
            } else {
                orderAdapter.updateOrders(orders) // OrderAdapter needs an updateOrders method
                binding.emptyOrdersText.visibility = View.GONE
                binding.orderRecyclerView.visibility = View.VISIBLE
                Log.d(TAG, "Orders updated in adapter: ${orders.size} orders")
            }
        })
    }

    // Make sure your activity_my_order.xml has a ProgressBar with id 'progressBar'
    // and a TextView with id 'emptyOrdersText'
}