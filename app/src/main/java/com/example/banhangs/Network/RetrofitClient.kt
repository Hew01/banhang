package com.example.banhangs.Network

import android.content.Context // Required for SessionManager
import com.example.banhangs.Helper.SessionManager // Your SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


object RetrofitClient { // Or class RetrofitClient, depending on your preference

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private const val BASE_URL = "https://techstoreapi-35j6.onrender.com/" // <<<< IMPORTANT
//    private const val BASE_URL = "https://10.0.2.2:7243/"
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Or NONE for production
    }

    private val authInterceptor: Interceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val token = appContext?.let { SessionManager(it).fetchAuthToken() } // Get token

        if (token != null) {
            val newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token") // Common way to send token
                .build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(originalRequest) // Proceed without token if not logged in
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor) // Now this should work
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val instance: ApiService by lazy {
        if (appContext == null) {
            throw IllegalStateException("RetrofitClient must be initialized with Context before use. Call RetrofitClient.initialize(context) in your Application class.")
        }
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(ApiService::class.java)
    }
}
