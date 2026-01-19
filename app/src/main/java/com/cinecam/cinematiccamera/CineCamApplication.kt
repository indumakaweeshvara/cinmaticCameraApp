package com.cinecam.cinematiccamera

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * CineCam Application - Professional Cinematic Camera
 * 
 * Main application class with Hilt dependency injection.
 * Initializes core components for camera processing and AI segmentation.
 */
@HiltAndroidApp
class CineCamApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Initialize any app-wide components here
    }
}
