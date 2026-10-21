package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BookEntity
import com.example.data.BookPageEntity
import com.example.data.BookRepository
import com.example.data.ExportResult
import com.example.data.ImageFileManager
import com.example.data.PdfExportConfig
import com.example.data.PdfExporter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ScreenState {
    data object Home : ScreenState
    data class Editor(val bookId: Long) : ScreenState
}

enum class EditorViewMode {
    GRID_2_COL,
    STORYBOARD_STRIP
}

sealed interface ExportProgressState {
    data object Idle : ExportProgressState
    data class Progress(val current: Int, val total: Int) : ExportProgressState
    data class Success(val result: ExportResult) : ExportProgressState
    data class Error(val message: String) : ExportProgressState
}

class ComicStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val fileManager = ImageFileManager(application)
    private val repository = BookRepository(database.bookDao(), fileManager)
    private val pdfExporter = PdfExporter(application)

    val books: StateFlow<List<BookEntity>> = repository.allBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentScreen = MutableStateFlow<ScreenState>(ScreenState.Home)
    val currentScreen: StateFlow<ScreenState> = _currentScreen.asStateFlow()

    private val _editorViewMode = MutableStateFlow(EditorViewMode.GRID_2_COL)
    val editorViewMode: StateFlow<EditorViewMode> = _editorViewMode.asStateFlow()

    private val _activeBook = MutableStateFlow<BookEntity?>(null)
    val activeBook: StateFlow<BookEntity?> = _activeBook.asStateFlow()

    private val _activePages = MutableStateFlow<List<BookPageEntity>>(emptyList())
    val activePages: StateFlow<List<BookPageEntity>> = _activePages.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _pendingInsertIndex = MutableStateFlow<Int?>(null)
    val pendingInsertIndex: StateFlow<Int?> = _pendingInsertIndex.asStateFlow()

    private val _pendingReplacePageId = MutableStateFlow<Long?>(null)
    val pendingReplacePageId: StateFlow<Long?> = _pendingReplacePageId.asStateFlow()

    private val _previewIndex = MutableStateFlow<Int?>(null)
    val previewIndex: StateFlow<Int?> = _previewIndex.asStateFlow()

    private val _exportSheetOpen = MutableStateFlow(false)
    val exportSheetOpen: StateFlow<Boolean> = _exportSheetOpen.asStateFlow()

    private val _exportState = MutableStateFlow<ExportProgressState>(ExportProgressState.Idle)
    val exportState: StateFlow<ExportProgressState> = _exportState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private var bookObservationJob: Job? = null
    private var pagesObservationJob: Job? = null

    fun openBook(bookId: Long) {
        bookObservationJob?.cancel()
        pagesObservationJob?.cancel()
        _currentScreen.value = ScreenState.Editor(bookId)

        bookObservationJob = viewModelScope.launch {
            repository.getBook(bookId).collect { book ->
                _activeBook.value = book
            }
        }
        pagesObservationJob = viewModelScope.launch {
            repository.getPages(bookId).collect { pages ->
                _activePages.value = pages
            }
        }
    }

    fun navigateHome() {
        bookObservationJob?.cancel()
        pagesObservationJob?.cancel()
        _currentScreen.value = ScreenState.Home
        _activeBook.value = null
        _activePages.value = emptyList()
        _pendingInsertIndex.value = null
        _pendingReplacePageId.value = null
    }

    fun toggleEditorViewMode() {
        _editorViewMode.value = when (_editorViewMode.value) {
            EditorViewMode.GRID_2_COL -> EditorViewMode.STORYBOARD_STRIP
            EditorViewMode.STORYBOARD_STRIP -> EditorViewMode.GRID_2_COL
        }
    }

    fun setEditorViewMode(mode: EditorViewMode) {
        _editorViewMode.value = mode
    }

    fun createBook(title: String, description: String = "", openImmediately: Boolean = true) {
        viewModelScope.launch {
            _isBusy.value = true
            val finalTitle = title.ifBlank { "Comic Issue #${books.value.size + 1}" }
            val id = repository.createBook(finalTitle, description)
            _isBusy.value = false
            _toastMessage.value = "Created \"$finalTitle\""
            if (openImmediately) {
                openBook(id)
            }
        }
    }

    fun quickCreateWithImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isBusy.value = true
            val autoTitle = "Comic Issue #${books.value.size + 1}"
            val id = repository.createBookWithImages(autoTitle, uris)
            _isBusy.value = false
            _toastMessage.value = "Created \"$autoTitle\" with ${uris.size} pages"
            openBook(id)
        }
    }

    fun duplicateBook(bookId: Long) {
        viewModelScope.launch {
            _isBusy.value = true
            val newId = repository.duplicateBook(bookId)
            _isBusy.value = false
            if (newId != null) {
                _toastMessage.value = "Project duplicated!"
            }
        }
    }

    fun renameBookById(bookId: Long, newTitle: String) {
        viewModelScope.launch {
            repository.updateBookTitle(bookId, newTitle)
            _toastMessage.value = "Project renamed"
        }
    }

    fun updateBookTitle(newTitle: String) {
        val book = _activeBook.value ?: return
        viewModelScope.launch {
            repository.updateBookTitle(book.id, newTitle)
        }
    }

    fun deleteBook(bookId: Long) {
        viewModelScope.launch {
            repository.deleteBook(bookId)
            if (_activeBook.value?.id == bookId) {
                navigateHome()
            }
            _toastMessage.value = "Project deleted"
        }
    }

    fun addPagesToEnd(uris: List<Uri>) {
        val book = _activeBook.value ?: return
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isBusy.value = true
            val added = repository.addPagesToEnd(book.id, uris)
            _isBusy.value = false
            _toastMessage.value = "Added $added page${if (added != 1) "s" else ""}"
        }
    }

    fun setPendingInsertIndex(index: Int) {
        _pendingInsertIndex.value = index
    }

    fun insertPagesAtPending(uris: List<Uri>) {
        val book = _activeBook.value ?: return
        val targetIndex = _pendingInsertIndex.value ?: return
        if (uris.isEmpty()) return

        viewModelScope.launch {
            _isBusy.value = true
            val added = repository.insertPagesAt(book.id, targetIndex, uris)
            _pendingInsertIndex.value = null
            _isBusy.value = false
            _toastMessage.value = "Inserted $added page${if (added != 1) "s" else ""} at Page #${targetIndex + 1}"
        }
    }

    fun setPendingReplacePageId(pageId: Long) {
        _pendingReplacePageId.value = pageId
    }

    fun replacePage(uri: Uri) {
        val pageId = _pendingReplacePageId.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val success = repository.replacePage(pageId, uri)
            _pendingReplacePageId.value = null
            _isBusy.value = false
            _toastMessage.value = if (success) "Page replaced!" else "Failed to replace page"
        }
    }

    fun deletePage(pageId: Long) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.deletePage(pageId)
            _isBusy.value = false
            _toastMessage.value = "Page removed"
        }
    }

    fun movePage(fromIndex: Int, toIndex: Int) {
        val book = _activeBook.value ?: return
        viewModelScope.launch {
            repository.movePage(book.id, fromIndex, toIndex)
        }
    }

    fun reversePageOrder() {
        val book = _activeBook.value ?: return
        viewModelScope.launch {
            repository.reversePages(book.id)
            _toastMessage.value = "Page sequence reversed"
        }
    }

    fun showPreview(index: Int) {
        _previewIndex.value = index
    }

    fun hidePreview() {
        _previewIndex.value = null
    }

    fun openExportSheet() {
        _exportState.value = ExportProgressState.Idle
        _exportSheetOpen.value = true
    }

    fun closeExportSheet() {
        _exportSheetOpen.value = false
        _exportState.value = ExportProgressState.Idle
    }

    fun exportToPdf(config: PdfExportConfig) {
        val book = _activeBook.value ?: return
        val pages = _activePages.value
        if (pages.isEmpty()) {
            _toastMessage.value = "Add at least 1 page before exporting PDF!"
            return
        }

        viewModelScope.launch {
            _exportState.value = ExportProgressState.Progress(0, pages.size)
            try {
                val result = pdfExporter.generatePdf(
                    book = book,
                    pages = pages,
                    config = config,
                    onProgress = { current, total ->
                        _exportState.value = ExportProgressState.Progress(current, total)
                    }
                )
                _exportState.value = ExportProgressState.Success(result)
            } catch (e: Exception) {
                e.printStackTrace()
                _exportState.value = ExportProgressState.Error(e.message ?: "Failed to generate PDF")
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
