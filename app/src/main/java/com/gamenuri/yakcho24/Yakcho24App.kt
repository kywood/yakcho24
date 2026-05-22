package com.gamenuri.yakcho24

import android.app.Application
import com.gamenuri.yakcho24.App.AppManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
//class Yakcho24App : Application()
class Yakcho24App : Application() {

    companion object {
        // 전역 어디서든 Yakcho24App.appManager로 접근 가능
        lateinit var appManager: AppManager
            private set
    }

    override fun onCreate() {
        super.onCreate()
        // 여기서 초기화
        appManager = AppManager(this)
    }
}