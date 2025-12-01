package com.example.booksy

import android.app.Application
import com.example.booksy.data.AppDatabase
import com.example.booksy.data.SessionManager
import com.example.booksy.network.BookService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object AppDependencies {

    lateinit var db: AppDatabase
        private set

    lateinit var session: SessionManager
        private set

    fun init(app: Application) {
        db = AppDatabase.get(app)
        session = SessionManager(app)
    }

    // 🔹 Especificamos el tipo Retrofit
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://booksyserver-production-596f.up.railway.app") // cambia por la IP de tu backend
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // 🔹 Servicio que usarás en HomeScreen
    val bookService: BookService by lazy {
        retrofit.create(BookService::class.java)
    }
}
