package com.example.booksy.data

import android.content.Context
import androidx.room.*

// ----------------------------
// ENTITY
// ----------------------------
@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val title: String,
    val author: String
)

// ----------------------------
// DAO
// ----------------------------
@Dao
interface BookDao {

    @Query("SELECT * FROM books WHERE userId = :userId")
    suspend fun getAll(userId: Long): List<Book>

    @Insert
    suspend fun insert(book: Book)

    @Delete
    suspend fun delete(book: Book)
}

// ----------------------------
// DATABASE
// ----------------------------
@Database(entities = [Book::class], version = 2)  // Version subida a 2
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "booksy.db"
                )
                    // Si el schema cambia, borra y recrea la DB (seguro para apps de clase)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
