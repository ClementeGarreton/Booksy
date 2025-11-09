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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.booksy.AppDependencies
import com.example.booksy.data.Book
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    goProfile: () -> Unit   // 👈 agregado
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
                actions = {
                    TextButton(onClick = goProfile) { Text("Perfil") } // 👈 botón a Profile
                }
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
            Column(Modifier.padding(horizontal = 24.dp)) {
                Text("¡Sesión iniciada!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                if (!email.isNullOrBlank()) Text("Bienvenido, $email")
                Spacer(Modifier.height(8.dp))
                Text("Tu biblioteca persiste por usuario (SQLite).", fontSize = 13.sp)
            }

            val catalog = remember {
                listOf(
                    BookCard("El Hobbit", "J. R. R. Tolkien", "book_hobbit"),
                    BookCard("1984", "George Orwell", "book_1984"),
                    BookCard("Dune", "Frank Herbert", "book_dune"),
                    BookCard("Fahrenheit 451", "Ray Bradbury", "book_f451"),
                    BookCard("Naked Lunch", "William Burroughs", "book_naked_lunch")
                )
            }
            CatalogRow(books = catalog)

            HorizontalDivider()

            Column(Modifier.padding(horizontal = 24.dp)) {
                Text("Agregar libro a tu cuenta", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = author, onValueChange = { author = it }, label = { Text("Autor") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (title.isBlank() || author.isBlank()) { error = "Completa título y autor"; return@Button }
                        scope.launch {
                            dao.insert(Book(userId = userId!!, title = title.trim(), author = author.trim()))
                            savedBooks = dao.getAll(userId!!)
                            title = ""; author = ""; error = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Agregar a mi cuenta") }

                error?.let { Spacer(Modifier.height(6.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(savedBooks, key = { it.id }) { b ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
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

data class BookCard(val title: String, val author: String, val coverName: String)

@Composable
fun CatalogRow(books: List<BookCard>) {
    val context = LocalContext.current
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(books) { book ->
            ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.width(160.dp).height(260.dp)) {
                val resId = remember(book.coverName) {
                    context.resources.getIdentifier(book.coverName, "drawable", context.packageName)
                }
                val painter = if (resId != 0) painterResource(id = resId) else painterResource(id = android.R.drawable.ic_menu_report_image)
                Image(
                    painter = painter,
                    contentDescription = book.title,
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop
                )
                Column(Modifier.padding(12.dp)) {
                    Text(book.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    Spacer(Modifier.height(4.dp))
                    Text(book.author, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
            }
        }
    }
}
