package com.example.spacebridgeapp

import android.app.Application
import android.content.Context
import com.example.spacebridgeapp.di.AppContainer
import com.example.spacebridgeapp.di.DefaultAppContainer

class SpaceBridgeApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as SpaceBridgeApplication).container
