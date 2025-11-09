package com.example.booksy.data

import android.content.Context
import androidx.room.*

@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val title: String,
    val author: String
)

@Dao
interface BookDao {
    @Query("SELECT * FROM books WHERE userId = :userId")
    suspend fun getAll(userId: Long): List<Book>

    @Insert
    suspend fun insert(book: Book)

    @Delete
    suspend fun delete(book: Book)
}
@Database(entities = [Book::class], version = 2) // ← sube de 1 a 2
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "booksy.db"
                )
                    .fallbackToDestructiveMigration() // ← recrea la DB si el schema cambió
                    .build().also { instance = it }
            }
    }
}
