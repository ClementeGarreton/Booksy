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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.booksy.data.Book
import com.example.booksy.data.RemoteBook
import com.example.booksy.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    goProfile: () -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val ctx = LocalContext.current
    val ui by vm.ui.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.load(ctx)
    }

    if (ui.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (!ui.hasSession) {
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(p),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {

            // HEADER
            item {
                Column(Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        "¡Sesión iniciada!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    ui.email?.let {
                        Text("Bienvenido, $it")
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tu biblioteca persiste por usuario (SQLite).",
                        fontSize = 13.sp
                    )
                }
            }

            // BARRA DE BÚSQUEDA (remoto)
            item {
                OutlinedTextField(
                    value = ui.searchQuery,
                    onValueChange = { vm.setSearchQuery(it) },
                    label = { Text("Buscar por título o autor") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
            }

            // CHIPS DE GÉNERO
            item {
                if (ui.availableGenres.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = ui.selectedGenre == null,
                                onClick = { vm.setSelectedGenre(null) },
                                label = { Text("Todos") }
                            )
                        }
                        items(ui.availableGenres.sorted()) { genre ->
                            FilterChip(
                                selected = ui.selectedGenre == genre,
                                onClick = { vm.setSelectedGenre(genre) },
                                label = { Text(genre) }
                            )
                        }
                    }
                }
            }

            // CATÁLOGO REMOTO FILTRADO
            item {
                if (ui.loadingCatalog) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column {
                        Text(
                            "Catálogo de libros",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(Modifier.height(8.dp))

                        RemoteCatalogRow(books = ui.filteredRemoteBooks)
                    }
                }
            }

            item {
                HorizontalDivider()
            }

            // FORMULARIO: AGREGAR LIBRO LOCAL
            item {
                Column(Modifier.padding(horizontal = 24.dp)) {
                    Text("Agregar libro a tu cuenta", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = ui.title,
                        onValueChange = { vm.setTitle(it) },
                        label = { Text("Título") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = ui.author,
                        onValueChange = { vm.setAuthor(it) },
                        label = { Text("Autor") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { vm.add(ctx) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Agregar a mi cuenta") }

                    ui.error?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // LISTA LOCAL
            items(ui.books, key = { it.id }) { b: Book ->
                ElevatedCard(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(b.title, style = MaterialTheme.typography.titleSmall)
                            Text(b.author, style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(onClick = { vm.delete(ctx, b) }) {
                            Text("Eliminar")
                        }
                    }
                }
            }

            // LOGOUT
            item {
                Row(Modifier.padding(horizontal = 24.dp)) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                vm.logout(ctx)
                                onLogout()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Cerrar sesión") }
                }
            }
        }
    }
}

// CARRUSEL REMOTO
@Composable
fun RemoteCatalogRow(books: List<RemoteBook>) {
    if (books.isEmpty()) {
        Text(
            "No hay libros que coincidan con la búsqueda",
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
