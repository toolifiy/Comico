package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.BookEditorScreen
import com.example.ui.ComicStudioViewModel
import com.example.ui.FullscreenPageDialog
import com.example.ui.HomeScreen
import com.example.ui.PdfExportDialog
import com.example.ui.ScreenState
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ComicStudioApp()
            }
        }
    }
}

@Composable
fun ComicStudioApp(
    viewModel: ComicStudioViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val editorViewMode by viewModel.editorViewMode.collectAsStateWithLifecycle()
    val books by viewModel.books.collectAsStateWithLifecycle()
    val activeBook by viewModel.activeBook.collectAsStateWithLifecycle()
    val activePages by viewModel.activePages.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val previewIndex by viewModel.previewIndex.collectAsStateWithLifecycle()
    val exportSheetOpen by viewModel.exportSheetOpen.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState, modifier = Modifier.navigationBarsPadding()) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = currentScreen, label = "screen_crossfade") { screen ->
                when (screen) {
                    is ScreenState.Home -> {
                        HomeScreen(
                            books = books,
                            onOpenBook = { bookId -> viewModel.openBook(bookId) },
                            onCreateBook = { title, desc -> viewModel.createBook(title, desc) },
                            onQuickImportCreate = { uris -> viewModel.quickCreateWithImages(uris) },
                            onDuplicateBook = { bookId -> viewModel.duplicateBook(bookId) },
                            onDeleteBook = { bookId -> viewModel.deleteBook(bookId) },
                            onRenameBook = { bookId, newTitle -> viewModel.renameBookById(bookId, newTitle) }
                        )
                    }

                    is ScreenState.Editor -> {
                        activeBook?.let { book ->
                            BookEditorScreen(
                                book = book,
                                pages = activePages,
                                isBusy = isBusy,
                                viewMode = editorViewMode,
                                onToggleViewMode = { viewModel.toggleEditorViewMode() },
                                onBack = { viewModel.navigateHome() },
                                onUpdateTitle = { newTitle -> viewModel.updateBookTitle(newTitle) },
                                onAddPagesToEnd = { uris -> viewModel.addPagesToEnd(uris) },
                                onInsertPagesAt = { targetIndex, uris ->
                                    viewModel.setPendingInsertIndex(targetIndex)
                                    viewModel.insertPagesAtPending(uris)
                                },
                                onReplacePage = { pageId, uri ->
                                    viewModel.setPendingReplacePageId(pageId)
                                    viewModel.replacePage(uri)
                                },
                                onDeletePage = { pageId -> viewModel.deletePage(pageId) },
                                onMovePage = { from, to -> viewModel.movePage(from, to) },
                                onReverseOrder = { viewModel.reversePageOrder() },
                                onPreviewPage = { idx -> viewModel.showPreview(idx) },
                                onOpenExport = { viewModel.openExportSheet() }
                            )
                        }
                    }
                }
            }

            // Fullscreen Page Preview Dialog
            previewIndex?.let { index ->
                FullscreenPageDialog(
                    initialIndex = index,
                    pages = activePages,
                    onDismiss = { viewModel.hidePreview() }
                )
            }

            // PDF Export Modal Dialog
            if (exportSheetOpen) {
                activeBook?.let { book ->
                    PdfExportDialog(
                        comicTitle = book.title,
                        pageCount = activePages.size,
                        exportState = exportState,
                        onDismiss = { viewModel.closeExportSheet() },
                        onStartExport = { config -> viewModel.exportToPdf(config) }
                    )
                }
            }
        }
    }
}
