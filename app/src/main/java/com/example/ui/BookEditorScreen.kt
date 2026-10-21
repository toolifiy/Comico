package com.example.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.BookEntity
import com.example.data.BookPageEntity
import com.example.ui.theme.CoralRed
import com.example.ui.theme.DarkElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanDark
import com.example.ui.theme.Gray100
import com.example.ui.theme.Gray200
import com.example.ui.theme.Gray300
import com.example.ui.theme.Gray400
import com.example.ui.theme.Gray500
import com.example.ui.theme.Gray600
import com.example.ui.theme.Gray700
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StudioCanvas
import java.io.File
import java.util.Locale

@Composable
fun BookEditorScreen(
    book: BookEntity,
    pages: List<BookPageEntity>,
    isBusy: Boolean,
    viewMode: EditorViewMode = EditorViewMode.GRID_2_COL,
    onToggleViewMode: () -> Unit = {},
    onBack: () -> Unit,
    onUpdateTitle: (String) -> Unit,
    onAddPagesToEnd: (List<Uri>) -> Unit,
    onInsertPagesAt: (targetIndex: Int, List<Uri>) -> Unit,
    onReplacePage: (pageId: Long, Uri) -> Unit,
    onDeletePage: (pageId: Long) -> Unit,
    onMovePage: (fromIndex: Int, toIndex: Int) -> Unit,
    onReverseOrder: () -> Unit,
    onPreviewPage: (pageIndex: Int) -> Unit,
    onOpenExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var isTitleEditing by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var pageToDelete by remember { mutableStateOf<BookPageEntity?>(null) }
    var targetInsertIndexForPicker by remember { mutableStateOf<Int?>(null) }
    var targetReplacePageIdForPicker by remember { mutableStateOf<Long?>(null) }

    // Multi-image picker for appending to the end
    val appendLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onAddPagesToEnd(uris)
        }
    }

    // Multi-image picker for inserting between two pages
    val insertBetweenLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        val targetIdx = targetInsertIndexForPicker
        if (uris.isNotEmpty() && targetIdx != null) {
            onInsertPagesAt(targetIdx, uris)
        }
        targetInsertIndexForPicker = null
    }

    // Single-image picker for replacing an image
    val replaceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val pageId = targetReplacePageIdForPicker
        if (uri != null && pageId != null) {
            onReplacePage(pageId, uri)
        }
        targetReplacePageIdForPicker = null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StudioCanvas)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Clean White Studio Top Bar
            EditorStudioHeader(
                bookTitle = book.title,
                pageCount = pages.size,
                viewMode = viewMode,
                onToggleViewMode = onToggleViewMode,
                onBack = onBack,
                onEditTitle = { isTitleEditing = true },
                onExportClick = onOpenExport,
                onMenuClick = { menuExpanded = true }
            )

            // Header Dropdown Menu
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(PureWhite)
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename Project", color = PitchBlack, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Icons.Default.Edit, null, tint = PitchBlack) },
                        onClick = {
                            menuExpanded = false
                            isTitleEditing = true
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (viewMode == EditorViewMode.GRID_2_COL) "Switch to Storyboard Strip" else "Switch to 2-Column Grid",
                                color = PitchBlack,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (viewMode == EditorViewMode.GRID_2_COL) Icons.Default.ViewAgenda else Icons.Default.GridView,
                                null,
                                tint = ElectricCyanDark
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onToggleViewMode()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Reverse Page Order", color = PitchBlack, fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(Icons.Default.SwapVert, null, tint = Gray600) },
                        onClick = {
                            menuExpanded = false
                            onReverseOrder()
                        },
                        enabled = pages.size > 1
                    )
                }
            }

            // 2. Main Editor Canvas (Empty State, 2-Column Grid, or Storyboard Strip)
            if (pages.isEmpty()) {
                EmptyEditorView(
                    onAddPagesClick = {
                        appendLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                )
            } else {
                // Mode Switcher Sub-bar
                EditorSubToolbar(
                    viewMode = viewMode,
                    onToggleViewMode = onToggleViewMode,
                    onInsertAtFirst = {
                        targetInsertIndexForPicker = 0
                        insertBetweenLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onReverseOrder = onReverseOrder,
                    canReverse = pages.size > 1
                )

                if (viewMode == EditorViewMode.GRID_2_COL) {
                    // 2-COLUMN COMIC GRID VIEW
                    val indexedPages = pages.mapIndexed { idx, p -> idx to p }
                    val rows = indexedPages.chunked(2)

                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 112.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .testTag("pages_grid")
                    ) {
                        itemsIndexed(rows, key = { _, row -> row.first().second.id }) { _, rowPair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                for ((pageIndex, pageEntity) in rowPair) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ComicGridPageCard(
                                            page = pageEntity,
                                            displayIndex = pageIndex + 1,
                                            isFirst = pageIndex == 0,
                                            isLast = pageIndex == pages.lastIndex,
                                            onMovePrev = { onMovePage(pageIndex, pageIndex - 1) },
                                            onMoveNext = { onMovePage(pageIndex, pageIndex + 1) },
                                            onReplace = {
                                                targetReplacePageIdForPicker = pageEntity.id
                                                replaceLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            onInsertAfter = {
                                                targetInsertIndexForPicker = pageIndex + 1
                                                insertBetweenLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            onPreview = { onPreviewPage(pageIndex) },
                                            onDelete = { pageToDelete = pageEntity }
                                        )
                                    }
                                }
                                if (rowPair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    // 1-COLUMN STORYBOARD STRIP VIEW (with Between-Page '+' Connectors)
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 6.dp,
                            bottom = 112.dp
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .testTag("pages_list")
                    ) {
                        item(key = "top_insert") {
                            InsertionDivider(
                                label = "Insert before Page #1",
                                onClick = {
                                    targetInsertIndexForPicker = 0
                                    insertBetweenLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            )
                        }

                        itemsIndexed(pages, key = { _, page -> page.id }) { index, page ->
                            PageStripItem(
                                page = page,
                                displayIndex = index + 1,
                                isFirst = index == 0,
                                isLast = index == pages.lastIndex,
                                onMoveUp = { onMovePage(index, index - 1) },
                                onMoveDown = { onMovePage(index, index + 1) },
                                onReplace = {
                                    targetReplacePageIdForPicker = page.id
                                    replaceLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onInsertAfter = {
                                    targetInsertIndexForPicker = index + 1
                                    insertBetweenLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onPreview = { onPreviewPage(index) },
                                onDelete = { pageToDelete = page }
                            )

                            InsertionDivider(
                                label = if (index == pages.lastIndex) {
                                    "Insert after Page #${index + 1}"
                                } else {
                                    "Insert between #${index + 1} & #${index + 2}"
                                },
                                onClick = {
                                    targetInsertIndexForPicker = index + 1
                                    insertBetweenLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // 3. Floating Pitch-Black Studio Bottom Dock (CapCut Pro Style)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Surface(
                color = PitchBlack,
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "${pages.size} ${if (pages.size == 1) "Page" else "Pages"}",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            color = PureWhite,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Saved offline",
                            color = ElectricCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Add Pages Button
                        Button(
                            onClick = {
                                appendLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkElevated,
                                contentColor = PureWhite
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("btn_add_pages_bottom")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Pages", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        // Export PDF Primary CTA (Electric Cyan on Pitch Black)
                        Button(
                            onClick = onOpenExport,
                            enabled = pages.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = PitchBlack,
                                disabledContainerColor = DarkElevated,
                                disabledContentColor = Gray500
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("btn_export_pdf_bottom")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export PDF", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Processing Overlay
        if (isBusy) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PitchBlack.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = PureWhite,
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = PitchBlack, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Updating comic pages...",
                            fontFamily = OutfitFontFamily,
                            color = PitchBlack,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }

    // Rename Title Dialog
    if (isTitleEditing) {
        RenameBookDialog(
            currentTitle = book.title,
            onDismiss = { isTitleEditing = false },
            onConfirm = { newTitle ->
                onUpdateTitle(newTitle)
                isTitleEditing = false
            }
        )
    }

    // Delete Page Dialog
    pageToDelete?.let { page ->
        AlertDialog(
            onDismissRequest = { pageToDelete = null },
            containerColor = PureWhite,
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    text = "Remove Page #${page.pageOrder + 1}?",
                    fontFamily = OutfitFontFamily,
                    color = PitchBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Text(
                    text = "This page will be deleted from your project and all following pages will shift up automatically.",
                    color = Gray600,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePage(page.id)
                        pageToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralRed,
                        contentColor = PureWhite
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_delete_page_button")
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pageToDelete = null }) {
                    Text("Cancel", color = Gray500, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Composable
private fun EditorStudioHeader(
    bookTitle: String,
    pageCount: Int,
    viewMode: EditorViewMode,
    onToggleViewMode: () -> Unit,
    onBack: () -> Unit,
    onEditTitle: () -> Unit,
    onExportClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Surface(
        color = PureWhite,
        border = BorderStroke(1.dp, Gray200),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("editor_back_button")) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = PitchBlack
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onEditTitle)
                    .padding(horizontal = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = bookTitle,
                        fontFamily = OutfitFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PitchBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit title",
                        tint = Gray400,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = "$pageCount ${if (pageCount == 1) "Page" else "Pages"} • Tap title to rename",
                    fontSize = 11.sp,
                    color = Gray500,
                    fontWeight = FontWeight.Medium
                )
            }

            // View Mode Toggle (2-Col Grid vs Storyboard Strip)
            Surface(
                color = StudioCanvas,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Gray200),
                modifier = Modifier
                    .clickable(onClick = onToggleViewMode)
                    .testTag("btn_toggle_editor_view_mode")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Icon(
                        imageVector = if (viewMode == EditorViewMode.GRID_2_COL) Icons.Default.GridView else Icons.Default.ViewAgenda,
                        contentDescription = "Switch View",
                        tint = PitchBlack,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (viewMode == EditorViewMode.GRID_2_COL) "Grid" else "Strip",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PitchBlack
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Top Export Button
            Button(
                onClick = onExportClick,
                enabled = pageCount > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PitchBlack,
                    contentColor = ElectricCyan
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("header_export_pdf_button")
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("PDF", fontFamily = OutfitFontFamily, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }

            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = PitchBlack)
            }
        }
    }
}

@Composable
private fun EditorSubToolbar(
    viewMode: EditorViewMode,
    onToggleViewMode: () -> Unit,
    onInsertAtFirst: () -> Unit,
    onReverseOrder: () -> Unit,
    canReverse: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Insert at start quick pill
        Surface(
            color = PureWhite,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Gray200),
            modifier = Modifier
                .clickable(onClick = onInsertAtFirst)
                .testTag("btn_insert_before_first")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = PitchBlack, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Insert at Page #1",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PitchBlack
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (canReverse) {
                Surface(
                    color = PureWhite,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Gray200),
                    modifier = Modifier.clickable(onClick = onReverseOrder)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.SwapVert, contentDescription = null, tint = Gray600, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reverse", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Gray700)
                    }
                }
            }
        }
    }
}

@Composable
private fun ComicGridPageCard(
    page: BookPageEntity,
    displayIndex: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onMovePrev: () -> Unit,
    onMoveNext: () -> Unit,
    onReplace: () -> Unit,
    onInsertAfter: () -> Unit,
    onPreview: () -> Unit,
    onDelete: () -> Unit
) {
    val badgeNum = String.format(Locale.US, "%02d", displayIndex)

    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Gray200),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("page_grid_item_$displayIndex")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Comic Page Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(195.dp)
                    .background(StudioCanvas)
                    .clickable(onClick = onPreview)
            ) {
                AsyncImage(
                    model = File(page.imagePath),
                    contentDescription = "Page $displayIndex",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top-Left Pitch Black + Electric Cyan Page Number Badge
                Surface(
                    color = PitchBlack.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "#$badgeNum",
                        fontFamily = OutfitFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ElectricCyan,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Top-Right Delete Button
                Surface(
                    color = PitchBlack.copy(alpha = 0.72f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(28.dp)
                        .clickable(onClick = onDelete)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Page",
                            tint = CoralRed,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // Bottom-Right Fullscreen Zoom Pill
                Surface(
                    color = PitchBlack.copy(alpha = 0.72f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Fullscreen,
                            contentDescription = "Fullscreen Preview",
                            tint = PureWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Action Controls under Grid Thumbnail
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Row 1: Move Left (<) and Move Right (>)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = if (!isFirst) StudioCanvas else Gray100.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Gray200),
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                            .clickable(enabled = !isFirst, onClick = onMovePrev)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Move Earlier",
                                tint = if (!isFirst) PitchBlack else Gray300,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Surface(
                        color = if (!isLast) StudioCanvas else Gray100.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Gray200),
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                            .clickable(enabled = !isLast, onClick = onMoveNext)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Move Later",
                                tint = if (!isLast) PitchBlack else Gray300,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                // Row 2: Replace & Insert After (+ Between)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = PureWhite,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Gray300),
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                            .clickable(onClick = onReplace)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.FindReplace,
                                contentDescription = "Replace",
                                tint = PitchBlack,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Replace",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PitchBlack
                            )
                        }
                    }

                    Surface(
                        color = PitchBlack,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                            .clickable(onClick = onInsertAfter)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Insert After",
                                tint = ElectricCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "After",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsertionDivider(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Gray200)
        )

        Surface(
            color = PureWhite,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Gray300),
            modifier = Modifier
                .clickable(onClick = onClick)
                .testTag("insertion_divider_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Surface(
                    color = PitchBlack,
                    shape = CircleShape,
                    modifier = Modifier.size(18.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = PitchBlack,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PageStripItem(
    page: BookPageEntity,
    displayIndex: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onReplace: () -> Unit,
    onInsertAfter: () -> Unit,
    onPreview: () -> Unit,
    onDelete: () -> Unit
) {
    val badgeNum = String.format(Locale.US, "%02d", displayIndex)

    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Gray200),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("page_item_$displayIndex")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Page Image Thumbnail
            Box(
                modifier = Modifier
                    .size(width = 88.dp, height = 118.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioCanvas)
                    .border(1.dp, Gray200, RoundedCornerShape(10.dp))
                    .clickable(onClick = onPreview)
            ) {
                AsyncImage(
                    model = File(page.imagePath),
                    contentDescription = "Page $displayIndex",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(5.dp)
                        .size(24.dp)
                        .background(PitchBlack.copy(alpha = 0.78f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Fullscreen,
                        contentDescription = "Zoom",
                        tint = PureWhite,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = PitchBlack,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "PAGE #$badgeNum",
                            fontFamily = OutfitFontFamily,
                            color = ElectricCyan,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }

                    if (page.width > 0 && page.height > 0) {
                        Text(
                            text = "${page.width} × ${page.height}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Gray500
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Up / Down Reorder Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = if (!isFirst) StudioCanvas else Gray100.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Gray200),
                        modifier = Modifier
                            .height(32.dp)
                            .clickable(enabled = !isFirst, onClick = onMoveUp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = "Move Up",
                                tint = if (!isFirst) PitchBlack else Gray300,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Up",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isFirst) PitchBlack else Gray300
                            )
                        }
                    }

                    Surface(
                        color = if (!isLast) StudioCanvas else Gray100.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Gray200),
                        modifier = Modifier
                            .height(32.dp)
                            .clickable(enabled = !isLast, onClick = onMoveDown)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = "Move Down",
                                tint = if (!isLast) PitchBlack else Gray300,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Down",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isLast) PitchBlack else Gray300
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Replace, Insert After, Delete
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = PureWhite,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Gray300),
                        modifier = Modifier
                            .height(32.dp)
                            .clickable(onClick = onReplace)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Icon(Icons.Default.FindReplace, null, tint = PitchBlack, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Replace", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PitchBlack)
                        }
                    }

                    Surface(
                        color = PitchBlack,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .clickable(onClick = onInsertAfter)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Icon(Icons.Default.Add, null, tint = ElectricCyan, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Insert After", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Page",
                            tint = CoralRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyEditorView(
    onAddPagesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = PureWhite,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Gray200),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 40.dp)
            ) {
                Surface(
                    color = PitchBlack,
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Add Comic or Book Pages",
                    fontFamily = OutfitFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PitchBlack
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Pick multiple images from your gallery. You can freely rearrange them, insert missing pages between any two images, or replace panels anytime.",
                    fontSize = 13.sp,
                    color = Gray500,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = onAddPagesClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PitchBlack,
                        contentColor = ElectricCyan
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
                    modifier = Modifier.testTag("empty_editor_add_pages_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Select Images from Gallery",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
