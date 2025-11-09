package com.example.booksy.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.booksy.AppDependencies
import com.example.booksy.data.Book
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val email: String? = null,
    val title: String = "",
    val author: String = "",
    val books: List<Book> = emptyList(),
    val error: String? = null
)

class HomeViewModel : ViewModel() {
    private val _ui = MutableStateFlow(HomeUiState())
    val ui = _ui.asStateFlow()

    fun load(ctx: Context) = viewModelScope.launch {
        val session = AppDependencies.session
        val userId = session.loadUserId() ?: return@launch
        val email = session.loadUserEmail()
        val books = AppDependencies.db.bookDao().getAll(userId)
        _ui.value = _ui.value.copy(email = email, books = books)
    }

    fun setTitle(v: String) { _ui.value = _ui.value.copy(title = v) }
    fun setAuthor(v: String) { _ui.value = _ui.value.copy(author = v) }

    fun add(ctx: Context) = viewModelScope.launch {
        val s = _ui.value
        val userId = AppDependencies.session.loadUserId() ?: return@launch
        if (s.title.isBlank() || s.author.isBlank()) return@launch
        AppDependencies.db.bookDao().insert(Book(userId = userId, title = s.title, author = s.author))
        load(ctx)
        _ui.value = _ui.value.copy(title = "", author = "")
    }

    fun delete(ctx: Context, book: Book) = viewModelScope.launch {
        AppDependencies.db.bookDao().delete(book)
        load(ctx)
    }

    suspend fun logout(ctx: Context) {
        AppDependencies.session.clear()
    }
}
