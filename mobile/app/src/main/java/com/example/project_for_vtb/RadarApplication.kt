package com.example.project_for_vtb

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class RadarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // В debug режиме логируем все, в release - только ошибки
        if (applicationInfo != null && (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            Timber.plant(Timber.DebugTree())
            Timber.d("🚀 [RadarApplication] Приложение запущено в DEBUG режиме")
            Timber.d("📱 [RadarApplication] Логирование Timber включено")
        } else {
            // В release режиме логируем только ошибки
            Timber.plant(object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    if (priority >= android.util.Log.ERROR) {
                        android.util.Log.println(priority, tag, message)
                    }
                }
            })
        }
    }
}

