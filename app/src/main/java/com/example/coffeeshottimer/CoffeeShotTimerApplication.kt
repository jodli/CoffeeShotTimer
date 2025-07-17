package com.example.coffeeshottimer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for the Espresso Shot Tracker app.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
class CoffeeShotTimerApplication : Application()