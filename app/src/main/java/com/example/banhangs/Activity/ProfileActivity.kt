package com.example.banhangs.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.banhangs.Activity.BaseActivity
import com.example.banhangs.Helper.TinyDB
import com.example.banhangs.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ProfileActivity : BaseActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var tinyDB: TinyDB
    private lateinit var auth: FirebaseAuth
    private val TAG = "ProfileActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize TinyDB
        tinyDB = TinyDB(this)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Check authentication
        if (auth.currentUser == null || !auth.currentUser!!.isEmailVerified) {
            // Redirect to LoginActivity if not logged in or email not verified
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        } else {
            // Display current email
            binding.currentEmailTxt.text = auth.currentUser?.email ?: "No email"
            loadProfileInfo()
        }

        // Back button click
        binding.backBtn.setOnClickListener {
            finish()
        }

        // Save button click
        binding.saveBtn.setOnClickListener {
            val name = binding.nameEditTxt.text.toString().trim()
            val email = binding.emailAddress.text.toString().trim()
            val phone = binding.phoneEditTxt.text.toString().trim()
            if (name.isNotEmpty()) {
                saveProfileInfo(name, email, phone)
            } else {
                Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadProfileInfo() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            FirebaseDatabase.getInstance().getReference("Users/$userId")
                .get().addOnSuccessListener { snapshot ->
                    val name = snapshot.child("profile_name").getValue(String::class.java) ?: tinyDB.getString("profile_name") ?: "Quang Huy"
                    val address = snapshot.child("address").getValue(String::class.java) ?: tinyDB.getString("profile_address") ?: ""
                    val phone = snapshot.child("phone").getValue(String::class.java) ?: tinyDB.getString("profile_phone") ?: ""
                    binding.nameEditTxt.setText(name)
                    binding.emailAddress.setText(address)
                    binding.phoneEditTxt.setText(phone)
                    Log.d(TAG, "Profile loaded: name=$name, address=$address, phone=$phone")
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Failed to load profile: ${e.message}")
                    binding.nameEditTxt.setText(tinyDB.getString("profile_name") ?: "Quang Huy")
                    binding.emailAddress.setText(tinyDB.getString("profile_address") ?: "")
                    binding.phoneEditTxt.setText(tinyDB.getString("profile_phone") ?: "")
                    Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
        } else {
            binding.nameEditTxt.setText(tinyDB.getString("profile_name") ?: "Quang Huy")
            binding.emailAddress.setText(tinyDB.getString("profile_address") ?: "")
            binding.phoneEditTxt.setText(tinyDB.getString("profile_phone") ?: "")
        }
    }

    private fun saveProfileInfo(name: String, address: String, phone: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userData = mapOf(
                "profile_name" to name,
                "address" to address,
                "phone" to phone,
                "auth_email" to auth.currentUser?.email
            )
            FirebaseDatabase.getInstance().getReference("Users/$userId")
                .setValue(userData)
                .addOnSuccessListener {
                    tinyDB.putString("profile_name", name)
                    tinyDB.putString("profile_address", address)
                    tinyDB.putString("profile_phone", phone)
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
                    finish()
                    Log.d(TAG, "Profile saved to Firebase: name=$name, address=$address, phone=$phone")
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Failed to save profile: ${e.message}")
                    Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            tinyDB.putString("profile_name", name)
            tinyDB.putString("profile_address", address)
            tinyDB.putString("profile_phone", phone)
            Toast.makeText(this, "Profile saved locally", Toast.LENGTH_SHORT).show()
            finish()
            Log.d(TAG, "Profile saved to TinyDB: name=$name, address=$address, phone=$phone")
        }
    }
}