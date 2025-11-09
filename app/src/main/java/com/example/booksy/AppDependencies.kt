package com.example.booksy

import android.app.Application
import com.example.booksy.data.AppDatabase
import com.example.booksy.data.SessionManager

object AppDependencies {
    lateinit var db: AppDatabase
        private set
    lateinit var session: SessionManager
        private set

    fun init(app: Application) {
        db = AppDatabase.get(app)
        session = SessionManager(app)
    }
}
