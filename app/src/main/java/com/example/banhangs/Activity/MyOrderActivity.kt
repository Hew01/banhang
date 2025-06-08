package com.example.banhangs.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.banhangs.Adapter.OrderAdapter
import com.example.banhangs.Helper.TinyDB
import com.example.banhangs.Model.OrderModel
import com.example.banhangs.Model.UserModel
import com.example.banhangs.databinding.ActivityMyOrderBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MyOrderActivity : BaseActivity() {
    private lateinit var binding: ActivityMyOrderBinding
    private lateinit var tinyDB: TinyDB
    private val orders = mutableListOf<OrderModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MyOrderActivity", "onCreate started")
        binding = ActivityMyOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("MyOrderActivity", "Binding initialized: ${binding.backBtn != null}")

        tinyDB = TinyDB(this)

        // Kiểm tra trạng thái nút Back
        binding.backBtn.post {
            Log.d("MyOrderActivity", "Back button state: isShown=${binding.backBtn.isShown}, isEnabled=${binding.backBtn.isEnabled}, isClickable=${binding.backBtn.isClickable}")
        }

        // Xử lý nút Back
        binding.backBtn.setOnClickListener {
            Log.d("MyOrderActivity", "Back button clicked")
            Toast.makeText(this, "Quay lại màn hình chính", Toast.LENGTH_SHORT).show()
            try {
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
                Log.d("MyOrderActivity", "Navigated to MainActivity")
            } catch (e: Exception) {
                Log.e("MyOrderActivity", "Error starting MainActivity: ${e.message}")
                Toast.makeText(this, "Lỗi mở MainActivity: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Thêm sự kiện touch để debug
        binding.backBtn.setOnTouchListener { _, event ->
            Log.d("MyOrderActivity", "Back button touched: ${event.action}")
            false // Cho phép sự kiện click tiếp tục
        }

        loadProfile()
        loadOrders()
        initOrderList()
    }

    private fun loadProfile() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().reference

        database.child("Users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val profileName = snapshot.child("profile_name").getValue(String::class.java)
                        ?: tinyDB.getString("profile_name") ?: "Quang Huy"
                    val address = snapshot.child("address").getValue(String::class.java)
                        ?: tinyDB.getString("profile_address") ?: "Chưa cập nhật"
                    val phone = snapshot.child("phone").getValue(String::class.java)
                        ?: tinyDB.getString("profile_phone") ?: "Chưa cập nhật"

                    val user = UserModel(
                        name = profileName,
                        address = address,
                        phone = phone
                    )

                    binding.nameTxt.text = "Tên: ${user.name}"
                    binding.addressTxt.text = "Địa chỉ: ${user.address}"
                    binding.phoneTxt.text = "Số điện thoại: ${user.phone}"
                    Log.d("MyOrderActivity", "Profile loaded: $user")

                    if (user.address == "Chưa cập nhật" && user.phone == "Chưa cập nhật") {
                        Toast.makeText(this@MyOrderActivity, "Vui lòng cập nhật địa chỉ và số điện thoại trong hồ sơ", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MyOrderActivity", "Failed to load profile: ${error.message}")
                    val profileName = tinyDB.getString("profile_name") ?: "Quang Huy"
                    val address = tinyDB.getString("profile_address") ?: "Chưa cập nhật"
                    val phone = tinyDB.getString("profile_phone") ?: "Chưa cập nhật"
                    binding.nameTxt.text = "Tên: $profileName"
                    binding.addressTxt.text = "Địa chỉ: $address"
                    binding.phoneTxt.text = "Số điện thoại: $phone"
                    Toast.makeText(this@MyOrderActivity, "Lỗi tải hồ sơ: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadOrders() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().reference
        database.child("Users").child(userId).child("orders")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    orders.clear()
                    for (orderSnapshot in snapshot.children) {
                        val order = orderSnapshot.getValue(OrderModel::class.java)
                        order?.let { orders.add(it) }
                    }
                    binding.orderRecyclerView.adapter?.notifyDataSetChanged()
                    Log.d("MyOrderActivity", "Orders loaded: ${orders.size}")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MyOrderActivity", "Failed to load orders: ${error.message}")
                    Toast.makeText(this@MyOrderActivity, "Lỗi tải đơn hàng!", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun initOrderList() {
        binding.orderRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.orderRecyclerView.adapter = OrderAdapter(orders)
    }
}