// app/src/main/java/com/example/booksy/network/BookService.kt

package com.example.booksy.network

import com.example.booksy.data.RemoteBook
import retrofit2.http.GET

interface BookService {

    @GET("books")
    suspend fun getCatalog(): List<RemoteBook>
}
