package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("SELECT * FROM books ORDER BY updatedAt DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun getBookById(id: Long): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookByIdSync(id: Long): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteBookById(bookId: Long)

    @Query("SELECT * FROM book_pages WHERE bookId = :bookId ORDER BY pageOrder ASC")
    fun getPagesForBook(bookId: Long): Flow<List<BookPageEntity>>

    @Query("SELECT * FROM book_pages WHERE bookId = :bookId ORDER BY pageOrder ASC")
    suspend fun getPagesForBookSync(bookId: Long): List<BookPageEntity>

    @Query("SELECT * FROM book_pages WHERE id = :pageId")
    suspend fun getPageByIdSync(pageId: Long): BookPageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: BookPageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<BookPageEntity>): List<Long>

    @Update
    suspend fun updatePage(page: BookPageEntity)

    @Update
    suspend fun updatePages(pages: List<BookPageEntity>)

    @Delete
    suspend fun deletePage(page: BookPageEntity)

    @Query("DELETE FROM book_pages WHERE id = :pageId")
    suspend fun deletePageById(pageId: Long)

    @Query("DELETE FROM book_pages WHERE bookId = :bookId")
    suspend fun deletePagesForBook(bookId: Long)

    @Query("UPDATE book_pages SET pageOrder = pageOrder + :shiftBy WHERE bookId = :bookId AND pageOrder >= :fromOrder")
    suspend fun shiftPagesOrder(bookId: Long, fromOrder: Int, shiftBy: Int)

    @Transaction
    suspend fun reorderPages(bookId: Long, pagesInNewOrder: List<BookPageEntity>) {
        val updated = pagesInNewOrder.mapIndexed { index, page ->
            page.copy(pageOrder = index)
        }
        updatePages(updated)
    }
}
