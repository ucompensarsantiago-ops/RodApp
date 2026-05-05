package com.example.rodapp

import android.app.Application

class RodApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SupabaseClient.init(this)
    }
}
