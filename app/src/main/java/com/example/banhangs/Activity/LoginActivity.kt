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
import com.example.banhangs.Model.UserData
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
                        "Vui lòng nhập email và mật khẩu",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }
                lifecycleScope.launch {
                    try {
                        val loginRequest = LoginRequest(loginIdentifier, pass)
                        val response = apiService.login(loginRequest) // apiService.login() returns Response<LoginApiResponse>

                        if (response.isSuccessful) {
                            val loginApiResponse = response.body() // This is your LoginApiResponse INSTANCE

                            // NOW, check the properties of the 'loginApiResponse' INSTANCE
                            if (loginApiResponse != null && (loginApiResponse.retCode == 0 || loginApiResponse.retCode == 3) && loginApiResponse.data != null) {
                                val token = loginApiResponse.data.token
                                val userFromApi = loginApiResponse.data.user // This is your UserData object from the API

                                sessionManager.saveAuthToken(token)
                                sessionManager.saveUserDetails(userFromApi) // Pass the UserData object directly

                                Toast.makeText(this@LoginActivity, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                                Log.i("LoginActivity", "Login successful. Token: $token, UserID: ${userFromApi.userId}")

                                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            } else {
                                val errorMessage = loginApiResponse?.systemMessage ?: "Đăng nhập thất bại. Vui lòng thử lại."
                                Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_LONG).show()
                                Log.e(
                                    "LoginActivity",
                                    "Login API error: RetCode=${loginApiResponse?.retCode}, Message='${loginApiResponse?.systemMessage}', ErrorBody='${
                                        response.errorBody()?.string()
                                    }'"
                                )
                            }
                        } else {
                            val errorBody = response.errorBody()?.string()
                            Toast.makeText(this@LoginActivity, "Lỗi kết nối: ${response.code()}", Toast.LENGTH_LONG).show()
                            Log.e("LoginActivity", "Login HTTP error: ${response.code()} - ${response.message()} - ErrorBody: '$errorBody'")
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

