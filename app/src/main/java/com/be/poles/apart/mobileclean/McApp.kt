package com.be.poles.apart.mobileclean

import android.app.Application

class McApp: Application() {
    companion object {
        lateinit var application: McApp
    }
    override fun onCreate() {
        super.onCreate()
        application = this
    }

}