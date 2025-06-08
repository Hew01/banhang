package com.example.banhangs // Your base package

import android.app.Application
import com.example.banhangs.Network.RetrofitClient

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.initialize(this) // Initialize RetrofitClient here
    }
}