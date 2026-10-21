package com.example.data

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class BookRepository(
    private val bookDao: BookDao,
    private val fileManager: ImageFileManager
) {

    val allBooks: Flow<List<BookEntity>> = bookDao.getAllBooks()

    fun getBook(bookId: Long): Flow<BookEntity?> = bookDao.getBookById(bookId)

    fun getPages(bookId: Long): Flow<List<BookPageEntity>> = bookDao.getPagesForBook(bookId)

    suspend fun createBook(title: String, description: String = ""): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val book = BookEntity(
            title = title.ifBlank { "Untitled Comic" },
            description = description,
            createdAt = now,
            updatedAt = now,
            pageCount = 0,
            coverImagePath = null
        )
        bookDao.insertBook(book)
    }

    suspend fun createBookWithImages(title: String, uris: List<Uri>): Long = withContext(Dispatchers.IO) {
        val bookId = createBook(title, "Quick imported project")
        if (uris.isNotEmpty()) {
            addPagesToEnd(bookId, uris)
        }
        bookId
    }

    suspend fun duplicateBook(sourceBookId: Long): Long? = withContext(Dispatchers.IO) {
        val sourceBook = bookDao.getBookByIdSync(sourceBookId) ?: return@withContext null
        val sourcePages = bookDao.getPagesForBookSync(sourceBookId)
        val now = System.currentTimeMillis()

        val newBookId = bookDao.insertBook(
            BookEntity(
                title = "${sourceBook.title} (Copy)",
                description = sourceBook.description,
                createdAt = now,
                updatedAt = now,
                pageCount = 0,
                coverImagePath = null
            )
        )

        val clonedPages = mutableListOf<BookPageEntity>()
        for (page in sourcePages) {
            val newPath = fileManager.copyExistingImage(page.imagePath, newBookId) ?: continue
            clonedPages.add(
                BookPageEntity(
                    bookId = newBookId,
                    pageOrder = clonedPages.size,
                    imagePath = newPath,
                    width = page.width,
                    height = page.height,
                    addedAt = now
                )
            )
        }

        if (clonedPages.isNotEmpty()) {
            bookDao.insertPages(clonedPages)
            updateBookStats(newBookId)
        }
        newBookId
    }

    suspend fun updateBookTitle(bookId: Long, newTitle: String) = withContext(Dispatchers.IO) {
        val book = bookDao.getBookByIdSync(bookId) ?: return@withContext
        bookDao.updateBook(book.copy(title = newTitle.ifBlank { "Untitled Comic" }, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteBook(bookId: Long) = withContext(Dispatchers.IO) {
        fileManager.deleteBookDirectory(bookId)
        bookDao.deleteBookById(bookId)
    }

    suspend fun addPagesToEnd(bookId: Long, uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        val currentPages = bookDao.getPagesForBookSync(bookId)
        var nextOrder = currentPages.size
        var addedCount = 0

        val newEntities = mutableListOf<BookPageEntity>()
        for (uri in uris) {
            val savedInfo = fileManager.saveImageFromUri(bookId, uri) ?: continue
            newEntities.add(
                BookPageEntity(
                    bookId = bookId,
                    pageOrder = nextOrder++,
                    imagePath = savedInfo.filePath,
                    width = savedInfo.width,
                    height = savedInfo.height
                )
            )
            addedCount++
        }

        if (newEntities.isNotEmpty()) {
            bookDao.insertPages(newEntities)
            updateBookStats(bookId)
        }
        addedCount
    }

    suspend fun insertPagesAt(bookId: Long, targetOrderIndex: Int, uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext 0

        val currentPages = bookDao.getPagesForBookSync(bookId)
        val clampedIndex = targetOrderIndex.coerceIn(0, currentPages.size)

        // Save new images
        val savedList = mutableListOf<SavedImageInfo>()
        for (uri in uris) {
            val saved = fileManager.saveImageFromUri(bookId, uri)
            if (saved != null) {
                savedList.add(saved)
            }
        }
        if (savedList.isEmpty()) return@withContext 0

        val countToInsert = savedList.size

        // Shift pages that are at or after clampedIndex
        bookDao.shiftPagesOrder(bookId, clampedIndex, countToInsert)

        // Insert new pages at clampedIndex
        val newEntities = savedList.mapIndexed { i, saved ->
            BookPageEntity(
                bookId = bookId,
                pageOrder = clampedIndex + i,
                imagePath = saved.filePath,
                width = saved.width,
                height = saved.height
            )
        }
        bookDao.insertPages(newEntities)
        updateBookStats(bookId)
        countToInsert
    }

    suspend fun replacePage(pageId: Long, newUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val oldPage = bookDao.getPageByIdSync(pageId) ?: return@withContext false
        val saved = fileManager.saveImageFromUri(oldPage.bookId, newUri) ?: return@withContext false

        // Delete old image file
        fileManager.deleteImageFile(oldPage.imagePath)

        // Update page record
        val updatedPage = oldPage.copy(
            imagePath = saved.filePath,
            width = saved.width,
            height = saved.height,
            addedAt = System.currentTimeMillis()
        )
        bookDao.updatePage(updatedPage)
        updateBookStats(oldPage.bookId)
        true
    }

    suspend fun deletePage(pageId: Long) = withContext(Dispatchers.IO) {
        val page = bookDao.getPageByIdSync(pageId) ?: return@withContext
        val bookId = page.bookId

        // Delete image file and DB record
        fileManager.deleteImageFile(page.imagePath)
        bookDao.deletePageById(pageId)

        // Re-index remaining pages
        val remaining = bookDao.getPagesForBookSync(bookId)
        val reordered = remaining.mapIndexed { index, p ->
            p.copy(pageOrder = index)
        }
        bookDao.updatePages(reordered)
        updateBookStats(bookId)
    }

    suspend fun movePage(bookId: Long, fromIndex: Int, toIndex: Int) = withContext(Dispatchers.IO) {
        val current = bookDao.getPagesForBookSync(bookId).toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
            val moved = current.removeAt(fromIndex)
            current.add(toIndex, moved)
            bookDao.reorderPages(bookId, current)
            updateBookStats(bookId)
        }
    }

    suspend fun reversePages(bookId: Long) = withContext(Dispatchers.IO) {
        val current = bookDao.getPagesForBookSync(bookId)
        if (current.size > 1) {
            val reversed = current.reversed()
            bookDao.reorderPages(bookId, reversed)
            updateBookStats(bookId)
        }
    }

    private suspend fun updateBookStats(bookId: Long) {
        val book = bookDao.getBookByIdSync(bookId) ?: return
        val pages = bookDao.getPagesForBookSync(bookId)
        val coverPath = pages.firstOrNull()?.imagePath
        bookDao.updateBook(
            book.copy(
                pageCount = pages.size,
                coverImagePath = coverPath,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
