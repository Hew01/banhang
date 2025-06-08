package com.example.banhangs.Activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity // Or your BaseActivity if you have one
import androidx.compose.ui.semantics.text
import com.example.banhangs.databinding.ActivityOrderSuccessBinding

class OrderSuccessActivity : AppCompatActivity() { // Change to BaseActivity if needed

    private lateinit var binding: ActivityOrderSuccessBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderSuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve the order ID passed from CartActivity
        val orderId = intent.getStringExtra("orderId")

        if (orderId != null && orderId.isNotBlank()) {
            binding.successMessageTextView.text =
                "Your order #$orderId has been successfully placed. Thank you for shopping with us!"
        } else {
            binding.successMessageTextView.text =
                "Your order has been successfully placed. Thank you for shopping with us!"
        }

        binding.backToHomeButton.setOnClickListener {
            // Navigate back to your main shopping activity or home screen.
            // Clear the task stack to prevent users from going back to the cart or checkout process.
            val intent = Intent(this, MainActivity::class.java) // Replace MainActivity with your actual main/home activity
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish() // Finish OrderSuccessActivity
        }
    }

    // Optional: Prevent going back to CartActivity with the back button
    override fun onBackPressed() {
        super.onBackPressed()
        // Similar to backToHomeButton, ensure a clean navigation flow
        val intent = Intent(this, MainActivity::class.java) // Replace MainActivity
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}