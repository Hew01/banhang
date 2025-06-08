package com.example.banhangs.Activity

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.launch
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.banhangs.Helper.SessionManager
import com.example.banhangs.Model.LoginApiResponse
import com.example.banhangs.R
import com.example.banhangs.Network.RetrofitClient // Your Retrofit client
import com.example.banhangs.Model.LoginRequest
import kotlinx.coroutines.launch
import java.io.IOException

    class LoginActivity : AppCompatActivity() {
        private lateinit var edtlogin: EditText
        private lateinit var edtpass: EditText
        private lateinit var btnlogin: Button
        private lateinit var btnChange: ImageButton
        private val apiService by lazy {
            RetrofitClient.instance // Or whatever you named your Retrofit client access
        }
        private lateinit var sessionManager: SessionManager // Declare SessionManager

        @SuppressLint("MissingInflatedId")
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            enableEdgeToEdge()
            setContentView(R.layout.activity_login)

            sessionManager = SessionManager(this)

            // If user is already logged in, redirect to MainActivity
            if (sessionManager.isLoggedIn()) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                return // Important to prevent rest of onCreate from running
            }

            edtlogin = findViewById(R.id.edtemail_login)
            edtpass = findViewById(R.id.edtpassword_login)
            btnlogin = findViewById(R.id.btn_Login)
            btnChange = findViewById(R.id.btn_Change_signUp)
            // Remove mAuth initialization
            // mAuth = FirebaseAuth.getInstance()

            btnlogin.setOnClickListener {
                val loginIdentifier = edtlogin.text.toString().trim() // Use "loginIdentifier"
                val pass = edtpass.text.toString().trim()

                if (loginIdentifier.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(
                        this,
                        "Vui lòng nhập email/tên đăng nhập và mật khẩu",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }
                lifecycleScope.launch {
                    try {
                        if (LoginApiResponse != null && LoginApiResponse.retCode == 0 && LoginApiResponse.data != null) {
                            val token = LoginApiResponse.data.token
                            val user = LoginApiResponse.data.user // Assuming user is your UserModel

                            sessionManager.saveAuthToken(token)

                            val userName = "${user.firstName ?: ""} ${user.lastName ?: ""}".trim()
                            if (userName.isEmpty()) {
                                // Fallback if names are not available, or use email/username
                                sessionManager.saveUserDetails(
                                    user.userId,
                                    user.email ?: loginIdentifier
                                )
                            } else {
                                sessionManager.saveUserDetails(user.userId, userName)
                            }
                        }
                        val loginRequest = LoginRequest(loginIdentifier, pass)
                        val response = apiService.login(loginRequest) // Assuming apiService.login() is your suspend function

                        // Hide loading indicator
                        // binding.progressBar.visibility = View.GONE

                        if (response.isSuccessful) {
                            val loginApiResponse = response.body()
                            if (loginApiResponse != null && loginApiResponse.retCode == 0 && loginApiResponse.data != null) { // Check retCode for success
                                val token = loginApiResponse.data.token
                                val user = loginApiResponse.data.user

                                sessionManager.saveAuthToken(token)

                                val userName = "${user.firstName ?: ""} ${user.lastName ?: ""}".trim()
                                if (userName.isEmpty()) {
                                    sessionManager.saveUserDetails(
                                        user.userId,
                                        user.email ?: loginIdentifier // Fallback to email or loginIdentifier
                                    )
                                } else {
                                    sessionManager.saveUserDetails(user.userId, userName)
                                }

                                Toast.makeText(this@LoginActivity, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                                Log.i("LoginActivity", "Login successful. Token: $token, UserID: ${user.userId}")

                                // Navigate to MainActivity or another appropriate activity
                                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish() // Finish LoginActivity so user can't go back to it with back button

                            } else {
                                // Handle API-specific errors (e.g., wrong credentials, user not found)
                                val errorMessage = loginApiResponse?.systemMessage ?: "Đăng nhập thất bại. Vui lòng thử lại."
                                Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_LONG).show()
                                Log.e("LoginActivity", "Login failed: ${loginApiResponse?.retCode} - ${loginApiResponse?.systemMessage} - ${response.errorBody()?.string()}")
                            }
                        } else {
                            // Handle HTTP errors (e.g., 401, 404, 500)
                            Toast.makeText(this@LoginActivity, "Lỗi kết nối: ${response.code()}", Toast.LENGTH_LONG).show()
                            Log.e("LoginActivity", "Login HTTP error: ${response.code()} - ${response.message()} - ${response.errorBody()?.string()}")
                        }
                    } catch (e: IOException) {
                        // Hide loading indicator
                        // binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@LoginActivity, "Lỗi mạng. Vui lòng kiểm tra kết nối.", Toast.LENGTH_LONG).show()
                        Log.e("LoginActivity", "Network error during login", e)
                    } catch (e: Exception) {
                        // Hide loading indicator
                        // binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@LoginActivity, "Đã xảy ra lỗi không mong muốn.", Toast.LENGTH_LONG).show()
                        Log.e("LoginActivity", "Unexpected error during login", e)
                    }
                }
            }

            btnChange.setOnClickListener {
                startActivity(Intent(this, SignUpActivity::class.java))
            }
        }
    }

