package com.example.booksy.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.booksy.AppDependencies
import com.example.booksy.data.Book
import com.example.booksy.data.RemoteBook
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val email: String? = null,
    val title: String = "",
    val author: String = "",
    val books: List<Book> = emptyList(),

    // 🔎 Catálogo remoto + búsqueda + género
    val remoteBooks: List<RemoteBook> = emptyList(),
    val filteredRemoteBooks: List<RemoteBook> = emptyList(),
    val searchQuery: String = "",
    val availableGenres: Set<String> = emptySet(),
    val selectedGenre: String? = null,

    // Estado general
    val hasSession: Boolean = false,
    val loading: Boolean = true,
    val loadingCatalog: Boolean = false,
    val error: String? = null
)

class HomeViewModel : ViewModel() {

    private val _ui = MutableStateFlow(HomeUiState())
    val ui = _ui.asStateFlow()

    fun load(ctx: Context) = viewModelScope.launch {
        _ui.value = _ui.value.copy(
            loading = true,
            loadingCatalog = true,
            error = null
        )

        val session = AppDependencies.session
        val userId = session.loadUserId()
        if (userId == null) {
            _ui.value = _ui.value.copy(
                hasSession = false,
                loading = false,
                loadingCatalog = false
            )
            return@launch
        }

        val email = session.loadUserEmail()
        val books = AppDependencies.db.bookDao().getAll(userId)

        _ui.value = _ui.value.copy(
            email = email,
            books = books,
            hasSession = true,
            loading = false
        )

        // catálogo remoto
        try {
            val remote = AppDependencies.bookService.getCatalog()
            val genres = remote.map { it.genre }.toSet()
            val filtered = applyFilters(remote, _ui.value.searchQuery, _ui.value.selectedGenre)
            _ui.value = _ui.value.copy(
                remoteBooks = remote,
                filteredRemoteBooks = filtered,
                availableGenres = genres,
                loadingCatalog = false
            )
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(
                loadingCatalog = false,
                error = e.message ?: "Error cargando catálogo remoto"
            )
        }
    }

    // --- filtros ---

    private fun applyFilters(
        list: List<RemoteBook>,
        query: String,
        genre: String?
    ): List<RemoteBook> {
        var result = list

        if (!genre.isNullOrBlank()) {
            result = result.filter { it.genre == genre }
        }

        if (query.isBlank()) return result

        val q = query.trim().lowercase()
        return result.filter { book ->
            book.title.lowercase().contains(q) ||
                    book.author.lowercase().contains(q)
        }
    }

    fun setSearchQuery(q: String) {
        val s = _ui.value
        val filtered = applyFilters(s.remoteBooks, q, s.selectedGenre)
        _ui.value = s.copy(
            searchQuery = q,
            filteredRemoteBooks = filtered
        )
    }

    fun setSelectedGenre(genre: String?) {
        val s = _ui.value
        val filtered = applyFilters(s.remoteBooks, s.searchQuery, genre)
        _ui.value = s.copy(
            selectedGenre = genre,
            filteredRemoteBooks = filtered
        )
    }

    // --- formulario local ---

    fun setTitle(v: String) {
        _ui.value = _ui.value.copy(title = v)
    }

    fun setAuthor(v: String) {
        _ui.value = _ui.value.copy(author = v)
    }

    fun add(ctx: Context) = viewModelScope.launch {
        val s = _ui.value
        val userId = AppDependencies.session.loadUserId() ?: return@launch

        if (s.title.isBlank() || s.author.isBlank()) {
            _ui.value = s.copy(error = "Completa título y autor")
            return@launch
        }

        AppDependencies.db.bookDao().insert(
            Book(
                userId = userId,
                title = s.title.trim(),
                author = s.author.trim()
            )
        )
        val books = AppDependencies.db.bookDao().getAll(userId)
        _ui.value = s.copy(
            books = books,
            title = "",
            author = "",
            error = null
        )
    }

    fun delete(ctx: Context, book: Book) = viewModelScope.launch {
        val userId = AppDependencies.session.loadUserId() ?: return@launch
        AppDependencies.db.bookDao().delete(book)
        val books = AppDependencies.db.bookDao().getAll(userId)
        _ui.value = _ui.value.copy(books = books)
    }

    suspend fun logout(ctx: Context) {
        AppDependencies.session.clear()
    }
}
