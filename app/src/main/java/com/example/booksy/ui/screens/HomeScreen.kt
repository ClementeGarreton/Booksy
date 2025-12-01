package com.example.booksy.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.booksy.AppDependencies
import com.example.booksy.data.Book
import com.example.booksy.data.RemoteBook
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    goProfile: () -> Unit
) {
    val ctx = LocalContext.current
    val dao = remember { AppDependencies.db.bookDao() }
    val scope = rememberCoroutineScope()

    var userId by remember { mutableStateOf<Long?>(null) }
    var email by remember { mutableStateOf<String?>(null) }
    var savedBooks by remember { mutableStateOf<List<Book>>(emptyList()) }
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        userId = AppDependencies.session.loadUserId()
        email = AppDependencies.session.loadUserEmail()
        userId?.let { savedBooks = dao.getAll(it) }
        loading = false
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (userId == null) {
        Text("No hay sesión iniciada", Modifier.padding(16.dp))
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Booksy — Home") },
                actions = { TextButton(onClick = goProfile) { Text("Perfil") } }
            )
        }
    ) { p ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(p)
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Header
            Column(Modifier.padding(horizontal = 24.dp)) {
                Text("¡Sesión iniciada!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                if (!email.isNullOrBlank()) Text("Bienvenido, $email")
                Spacer(Modifier.height(8.dp))
                Text("Tu biblioteca persiste por usuario (SQLite).", fontSize = 13.sp)
            }

            // ========================
            // REMOTE BOOK CATALOG WITH FILTERS
            // ========================
            var remoteBooks by remember { mutableStateOf<List<RemoteBook>>(emptyList()) }
            var allRemoteBooks by remember { mutableStateOf<List<RemoteBook>>(emptyList()) }
            var loadingCatalog by remember { mutableStateOf(true) }
            var selectedGenre by remember { mutableStateOf<String?>(null) }
            var availableGenres by remember { mutableStateOf<Set<String>>(emptySet()) }

            LaunchedEffect(Unit) {
                try {
                    val books = AppDependencies.bookService.getCatalog()
                    allRemoteBooks = books
                    remoteBooks = books
                    availableGenres = books.map { it.genre }.toSet()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    loadingCatalog = false
                }
            }

            if (loadingCatalog) {
                CircularProgressIndicator(Modifier.padding(16.dp))
            } else {
                Column {
                    // Genre filter chips
                    Text(
                        "Catálogo de libros",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(Modifier.height(8.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedGenre == null,
                                onClick = {
                                    selectedGenre = null
                                    remoteBooks = allRemoteBooks
                                },
                                label = { Text("Todos") }
                            )
                        }
                        items(availableGenres.sorted()) { genre ->
                            FilterChip(
                                selected = selectedGenre == genre,
                                onClick = {
                                    selectedGenre = genre
                                    remoteBooks = allRemoteBooks.filter { it.genre == genre }
                                },
                                label = { Text(genre) }
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    RemoteCatalogRow(books = remoteBooks)
                }
            }

            HorizontalDivider()

            // ========================
            // ADD BOOK LOCAL
            // ========================
            Column(Modifier.padding(horizontal = 24.dp)) {
                Text("Agregar libro a tu cuenta", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Autor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (title.isBlank() || author.isBlank()) {
                            error = "Completa título y autor"
                            return@Button
                        }
                        scope.launch {
                            dao.insert(Book(userId = userId!!, title = title.trim(), author = author.trim()))
                            savedBooks = dao.getAll(userId!!)
                            title = ""
                            author = ""
                            error = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Agregar a mi cuenta") }

                error?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }

            // ========================
            // LOCAL BOOK LIST
            // ========================
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(savedBooks, key = { it.id }) { b ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(b.title, style = MaterialTheme.typography.titleSmall)
                                Text(b.author, style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedButton(onClick = {
                                scope.launch {
                                    dao.delete(b)
                                    savedBooks = dao.getAll(userId!!)
                                }
                            }) { Text("Eliminar") }
                        }
                    }
                }
            }

            // ========================
            // LOGOUT BUTTON
            // ========================
            Row(Modifier.padding(horizontal = 24.dp)) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            AppDependencies.session.clear()
                            onLogout()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Cerrar sesión") }
            }
        }
    }
}


// ======================================
// REMOTE CATALOG ROW
// ======================================
@Composable
fun RemoteCatalogRow(books: List<RemoteBook>) {
    if (books.isEmpty()) {
        Text(
            "No hay libros en este género",
            modifier = Modifier.padding(horizontal = 24.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(books) { book ->
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .width(160.dp)
                    .height(280.dp)
            ) {
                Column {
                    Image(
                        painter = rememberAsyncImagePainter(book.imageUrl),
                        contentDescription = book.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            book.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            book.author,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            book.genre,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}