package com.example.booksy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit

// -------------------- DataStore (session persistence) --------------------
private val android.content.Context.dataStore by preferencesDataStore("booksy_prefs")
private val KEY_USER_ID = stringPreferencesKey("current_user_id")

object SessionManager {
    suspend fun saveCurrentUserId(context: android.content.Context, userId: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = userId.toString()
        }
    }

    suspend fun clearCurrentUserId(context: android.content.Context) {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_USER_ID)
        }
    }

    suspend fun loadCurrentUserId(context: android.content.Context): Long? {
        val s = context.dataStore.data.map { it[KEY_USER_ID] }.first()
        return s?.toLongOrNull()
    }
}

// -------------------- ROOM: User + Book entities, DAO, DB --------------------
@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val password: String
)

@Entity(
    tableName = "books",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId")]
)
data class Book(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val title: String,
    val author: String
)

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): User?

    @Insert
    suspend fun insert(user: User): Long

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): User?
}

@Dao
interface BookDao {
    @Query("SELECT * FROM books WHERE userId = :userId ORDER BY id DESC")
    fun getAllByUser(userId: Long): Flow<List<Book>>

    @Insert
    suspend fun insert(book: Book): Long

    @Delete
    suspend fun delete(book: Book)
}

@Database(entities = [User::class, Book::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun bookDao(): BookDao

    companion object {
        @Volatile
        private var INSTANCE: AppDb? = null

        fun get(context: android.content.Context): AppDb =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDb::class.java,
                    "booksy.db"
                ).build().also { INSTANCE = it }
            }
    }
}

// -------------------- UI --------------------
@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot() {
    val ctx = LocalContext.current
    val db = remember { AppDb.get(ctx) }
    val userDao = remember { db.userDao() }
    val scope = rememberCoroutineScope()

    var currentUserId by remember { mutableStateOf<Long?>(null) }
    var currentUserEmail by remember { mutableStateOf<String?>(null) }
    var loadingSession by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val savedId = SessionManager.loadCurrentUserId(ctx)
        if (savedId != null) {
            currentUserId = savedId
            val u = userDao.getById(savedId)
            currentUserEmail = u?.email
        }
        loadingSession = false
    }

    if (loadingSession) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (currentUserId != null) {
        HomeScreenWithPersistence(
            userId = currentUserId!!,
            email = currentUserEmail,
            onLogout = {
                scope.launch {
                    SessionManager.clearCurrentUserId(ctx)
                    currentUserId = null
                    currentUserEmail = null
                }
            }
        )
    } else {
        AuthScreenWithPersistence(
            onSuccess = { user ->
                scope.launch {
                    SessionManager.saveCurrentUserId(ctx, user.id)
                    currentUserId = user.id
                    currentUserEmail = user.email
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreenWithPersistence(onSuccess: (User) -> Unit) {
    val ctx = LocalContext.current
    val db = remember { AppDb.get(ctx) }
    val userDao = remember { db.userDao() }
    val scope = rememberCoroutineScope()

    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isLogin) "Booksy — Login" else "Booksy — Registro") }
            )
        }
    ) { p ->
        Box(
            Modifier.fillMaxSize().padding(p),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (isLogin) ImeAction.Done else ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isLogin) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it },
                        label = { Text("Repite la contraseña") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        message = null
                        if (email.isBlank() || password.isBlank()) {
                            message = "Completa todos los campos"
                            return@Button
                        }
                        if (!email.contains("@") || !email.contains(".")) {
                            message = "Email no válido"
                            return@Button
                        }
                        if (password.length < 4) {
                            message = "Contraseña demasiado corta (4+)"
                            return@Button
                        }
                        if (!isLogin && confirm != password) {
                            message = "Las contraseñas no coinciden"
                            return@Button
                        }

                        scope.launch {
                            try {
                                if (isLogin) {
                                    val u = userDao.findByEmail(email.trim())
                                    if (u == null) {
                                        message = "Usuario no encontrado"
                                    } else if (u.password != password) {
                                        message = "Contraseña incorrecta"
                                    } else {
                                        onSuccess(u)
                                    }
                                } else {
                                    val exists = userDao.findByEmail(email.trim())
                                    if (exists != null) {
                                        message = "Ya existe un usuario con ese email"
                                    } else {
                                        val id = userDao.insert(
                                            User(email = email.trim(), password = password)
                                        )
                                        val u = userDao.getById(id)
                                        if (u != null) onSuccess(u)
                                    }
                                }
                            } catch (e: Exception) {
                                message = "Error: ${e.message}"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isLogin) "Entrar" else "Registrar")
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        isLogin = !isLogin
                        message = null
                        password = ""
                        confirm = ""
                    }
                ) {
                    Text(
                        if (isLogin) "¿No tienes cuenta? Regístrate"
                        else "¿Ya tienes cuenta? Inicia sesión"
                    )
                }
                Spacer(Modifier.height(12.dp))
                if (!message.isNullOrBlank()) {
                    Text(message!!, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenWithPersistence(userId: Long, email: String?, onLogout: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { AppDb.get(ctx) }
    val dao = remember { db.bookDao() }
    val scope = rememberCoroutineScope()

    val savedBooks by dao.getAllByUser(userId).collectAsState(initial = emptyList())

    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Booksy — Home") })
        }
    ) { p ->
        Column(
            Modifier.fillMaxSize().padding(p).padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.padding(horizontal = 24.dp)) {
                Text(
                    "¡Sesión iniciada!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
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
                            dao.insert(
                                Book(
                                    userId = userId,
                                    title = title.trim(),
                                    author = author.trim()
                                )
                            )
                            title = ""
                            author = ""
                            error = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Agregar a mi cuenta")
                }
                if (error != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            }

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
                            OutlinedButton(onClick = { scope.launch { dao.delete(b) } }) {
                                Text("Eliminar")
                            }
                        }
                    }
                }
            }

            Row(Modifier.padding(horizontal = 24.dp)) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            SessionManager.clearCurrentUserId(ctx)
                            onLogout()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cerrar sesión")
                }
            }
        }
    }
}

data class BookCard(val title: String, val author: String, val coverName: String)

@Composable
fun CatalogRow(books: List<BookCard>) {
    val context = LocalContext.current
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(books) { book ->
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.width(160.dp).height(260.dp)
            ) {
                val resId = remember(book.coverName) {
                    context.resources.getIdentifier(
                        book.coverName,
                        "drawable",
                        context.packageName
                    )
                }
                val painter = if (resId != 0) {
                    painterResource(id = resId)
                } else {
                    painterResource(id = android.R.drawable.ic_menu_report_image)
                }
                Image(
                    painter = painter,
                    contentDescription = book.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
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