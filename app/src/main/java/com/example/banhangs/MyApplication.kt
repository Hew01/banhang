package com.example.banhangs // Your base package

import android.app.Application
import com.example.banhangs.Network.RetrofitClient
import com.example.banhangs.Repository.UserPreferencesRepository

class MyApplication : Application() {
    lateinit var userPreferencesRepository: UserPreferencesRepository
        private set
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.initialize(this)
        userPreferencesRepository = UserPreferencesRepository(applicationContext)
    }
}