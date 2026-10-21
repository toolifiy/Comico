package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.BookEntity
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricAzure
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanDark
import com.example.ui.theme.ElectricCyanSoft
import com.example.ui.theme.Gray100
import com.example.ui.theme.Gray200
import com.example.ui.theme.Gray300
import com.example.ui.theme.Gray400
import com.example.ui.theme.Gray50
import com.example.ui.theme.Gray500
import com.example.ui.theme.Gray600
import com.example.ui.theme.Gray700
import com.example.ui.theme.Gray900
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StudioCanvas
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class HistoryLayoutMode {
    LIST_ROWS,
    COVER_GRID
}

private enum class SortMode(val label: String) {
    RECENT("Recent"),
    MOST_PAGES("Most Pages")
}

@Composable
fun HomeScreen(
    books: List<BookEntity>,
    onOpenBook: (Long) -> Unit,
    onCreateBook: (String, String) -> Unit,
    onQuickImportCreate: (List<Uri>) -> Unit = {},
    onDuplicateBook: (Long) -> Unit = {},
    onDeleteBook: (Long) -> Unit,
    onRenameBook: (Long, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var bookToRename by remember { mutableStateOf<BookEntity?>(null) }
    var bookToDelete by remember { mutableStateOf<BookEntity?>(null) }

    var searchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var layoutMode by remember { mutableStateOf(HistoryLayoutMode.LIST_ROWS) }
    var sortMode by remember { mutableStateOf(SortMode.RECENT) }

    // Direct Multi-Photo Picker for "Quick Import"
    val quickImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onQuickImportCreate(uris)
        }
    }

    val filteredBooks = remember(books, searchQuery, sortMode) {
        val searched = if (searchQuery.isBlank()) {
            books
        } else {
            books.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true)
            }
        }
        when (sortMode) {
            SortMode.RECENT -> searched.sortedByDescending { it.updatedAt }
            SortMode.MOST_PAGES -> searched.sortedByDescending { it.pageCount }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureWhite)
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            // 1. Pro Studio Top Bar (CapCut / VN style)
            item(key = "studio_top_bar") {
                StudioTopBar(
                    searchActive = searchVisible,
                    onToggleSearch = {
                        searchVisible = !searchVisible
                        if (!searchVisible) searchQuery = ""
                    }
                )
            }

            // Expandable Search Bar
            item(key = "search_bar") {
                AnimatedVisibility(
                    visible = searchVisible,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search saved comics or books...", fontSize = 14.sp, color = Gray400) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PitchBlack) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Gray500)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PitchBlack,
                            unfocusedBorderColor = Gray200,
                            focusedContainerColor = StudioCanvas,
                            unfocusedContainerColor = StudioCanvas,
                            cursorColor = PitchBlack,
                            focusedTextColor = PitchBlack,
                            unfocusedTextColor = PitchBlack
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .testTag("home_search_input")
                    )
                }
            }

            // 2. CapCut Style Large Gradient Hero Card with Big '+' Icon
            item(key = "capcut_hero_card") {
                CapCutGradientHeroCard(
                    onNewProjectClick = { showCreateDialog = true }
                )
            }

            // 3. 4-Column Studio Shortcut Strip (Quick Import, Blank Draft, Layout, Sort)
            item(key = "studio_shortcut_strip") {
                StudioQuickActionStrip(
                    onQuickImport = {
                        quickImportLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onNewBlank = { showCreateDialog = true },
                    layoutMode = layoutMode,
                    onToggleLayout = {
                        layoutMode = if (layoutMode == HistoryLayoutMode.LIST_ROWS) {
                            HistoryLayoutMode.COVER_GRID
                        } else {
                            HistoryLayoutMode.LIST_ROWS
                        }
                    },
                    sortMode = sortMode,
                    onToggleSort = {
                        sortMode = if (sortMode == SortMode.RECENT) SortMode.MOST_PAGES else SortMode.RECENT
                    }
                )
            }

            // 4. "Projects" History Header
            item(key = "history_section_header") {
                HistorySectionHeader(
                    totalCount = filteredBooks.size,
                    sortLabel = sortMode.label,
                    isGrid = layoutMode == HistoryLayoutMode.COVER_GRID,
                    onToggleLayout = {
                        layoutMode = if (layoutMode == HistoryLayoutMode.LIST_ROWS) {
                            HistoryLayoutMode.COVER_GRID
                        } else {
                            HistoryLayoutMode.LIST_ROWS
                        }
                    }
                )
            }

            // 5. Saved Projects History (List Rows or 2-Column Cover Grid)
            if (filteredBooks.isEmpty()) {
                item(key = "empty_history") {
                    CapCutEmptyProjectsState(
                        isSearching = searchQuery.isNotBlank(),
                        onNewProjectClick = { showCreateDialog = true },
                        onQuickImportClick = {
                            quickImportLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                }
            } else if (layoutMode == HistoryLayoutMode.LIST_ROWS) {
                items(filteredBooks, key = { it.id }) { book ->
                    VnProjectRowCard(
                        book = book,
                        onClick = { onOpenBook(book.id) },
                        onDuplicate = { onDuplicateBook(book.id) },
                        onRename = { bookToRename = book },
                        onDelete = { bookToDelete = book }
                    )
                }
            } else {
                // 2-Column Cover Grid inside LazyColumn
                val chunkedBooks = filteredBooks.chunked(2)
                items(chunkedBooks, key = { pair -> pair.first().id }) { pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (book in pair) {
                            Box(modifier = Modifier.weight(1f)) {
                                CapCutGridCoverCard(
                                    book = book,
                                    onClick = { onOpenBook(book.id) },
                                    onDuplicate = { onDuplicateBook(book.id) },
                                    onRename = { bookToRename = book },
                                    onDelete = { bookToDelete = book }
                                )
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    // Create Project Dialog
    if (showCreateDialog) {
        CreateBookDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, desc ->
                onCreateBook(title, desc)
                showCreateDialog = false
            }
        )
    }

    // Rename Dialog
    bookToRename?.let { book ->
        RenameBookDialog(
            currentTitle = book.title,
            onDismiss = { bookToRename = null },
            onConfirm = { newTitle ->
                onRenameBook(book.id, newTitle)
                bookToRename = null
            }
        )
    }

    // Delete Confirmation Dialog
    bookToDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            containerColor = PureWhite,
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    text = "Delete \"${book.title}\"?",
                    fontFamily = OutfitFontFamily,
                    color = PitchBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Text(
                    text = "This will permanently remove this project and all ${book.pageCount} saved comic pages from your device.",
                    color = Gray600,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteBook(book.id)
                        bookToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralRed,
                        contentColor = PureWhite
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) {
                    Text("Cancel", color = Gray600, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Composable
private fun StudioTopBar(
    searchActive: Boolean,
    onToggleSearch: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Pitch Black Studio Badge with Electric Cyan Mark
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PitchBlack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoStories,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ComicCut",
                        fontFamily = OutfitFontFamily,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PitchBlack,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = PitchBlack,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "STUDIO",
                            fontFamily = OutfitFontFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ElectricCyan,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "Book & Comic PDF Creator",
                    fontSize = 11.sp,
                    color = Gray500,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Search Toggle Button
        Surface(
            color = if (searchActive) PitchBlack else StudioCanvas,
            shape = CircleShape,
            border = BorderStroke(1.dp, if (searchActive) PitchBlack else Gray200),
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .clickable(onClick = onToggleSearch)
                .testTag("btn_toggle_search")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (searchActive) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "Search Projects",
                    tint = if (searchActive) PureWhite else PitchBlack,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CapCutGradientHeroCard(
    onNewProjectClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF00E5FF),
                            Color(0xFF00B4FF),
                            Color(0xFF0066FF)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(900f, 450f)
                    )
                )
                .drawBehind {
                    // Decorative geometric light rings like CapCut's New Project banner
                    drawCircle(
                        color = Color.White.copy(alpha = 0.16f),
                        radius = size.height * 0.85f,
                        center = Offset(size.width * 0.88f, size.height * 0.2f),
                        style = Stroke(width = 28f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.10f),
                        radius = size.height * 0.55f,
                        center = Offset(size.width * 0.12f, size.height * 0.95f)
                    )
                }
                .clickable(onClick = onNewProjectClick)
                .testTag("capcut_hero_new_project"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Iconic Big Pitch-Black Plus Circle Button
                Surface(
                    color = PitchBlack,
                    shape = CircleShape,
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Project",
                            tint = ElectricCyan,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "New project",
                    fontFamily = OutfitFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PitchBlack,
                    letterSpacing = (-0.3).sp
                )

                Text(
                    text = "Arrange gallery images into a comic or book PDF",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PitchBlack.copy(alpha = 0.78f)
                )
            }
        }
    }
}

@Composable
private fun StudioQuickActionStrip(
    onQuickImport: () -> Unit,
    onNewBlank: () -> Unit,
    layoutMode: HistoryLayoutMode,
    onToggleLayout: () -> Unit,
    sortMode: SortMode,
    onToggleSort: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickToolTile(
            icon = Icons.Default.AddPhotoAlternate,
            label = "Quick Import",
            accent = true,
            onClick = onQuickImport,
            modifier = Modifier
                .weight(1f)
                .testTag("btn_quick_import_photos")
        )
        QuickToolTile(
            icon = Icons.Default.CreateNewFolder,
            label = "Blank Book",
            accent = false,
            onClick = onNewBlank,
            modifier = Modifier.weight(1f)
        )
        QuickToolTile(
            icon = if (layoutMode == HistoryLayoutMode.LIST_ROWS) Icons.Default.GridView else Icons.Default.ViewAgenda,
            label = if (layoutMode == HistoryLayoutMode.LIST_ROWS) "Grid View" else "List View",
            accent = false,
            onClick = onToggleLayout,
            modifier = Modifier.weight(1f)
        )
        QuickToolTile(
            icon = Icons.Default.SwapVert,
            label = sortMode.label,
            accent = false,
            onClick = onToggleSort,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickToolTile(
    icon: ImageVector,
    label: String,
    accent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(StudioCanvas)
            .border(1.dp, Gray200, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp)
    ) {
        Surface(
            color = if (accent) PitchBlack else PureWhite,
            shape = RoundedCornerShape(12.dp),
            border = if (accent) null else BorderStroke(1.dp, Gray200),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (accent) ElectricCyan else PitchBlack,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = PitchBlack,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HistorySectionHeader(
    totalCount: Int,
    sortLabel: String,
    isGrid: Boolean,
    onToggleLayout: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Projects",
                fontFamily = OutfitFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PitchBlack
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = PitchBlack,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "$totalCount",
                    fontFamily = OutfitFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Surface(
            color = StudioCanvas,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Gray200),
            modifier = Modifier.clickable(onClick = onToggleLayout)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Icon(
                    imageVector = if (isGrid) Icons.Default.GridView else Icons.Default.ViewAgenda,
                    contentDescription = null,
                    tint = PitchBlack,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "$sortLabel • ${if (isGrid) "Grid" else "List"}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Gray700
                )
            }
        }
    }
}

@Composable
private fun VnProjectRowCard(
    book: BookEntity,
    onClick: () -> Unit,
    onDuplicate: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val formattedDate = remember(book.updatedAt) {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date(book.updatedAt))
    }

    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Gray200),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable(onClick = onClick)
            .testTag("project_item_${book.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover Thumbnail (CapCut / VN style rounded square with page count badge)
            Box(
                modifier = Modifier
                    .size(width = 76.dp, height = 84.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioCanvas)
                    .border(1.dp, Gray200, RoundedCornerShape(12.dp))
            ) {
                if (!book.coverImagePath.isNullOrEmpty()) {
                    AsyncImage(
                        model = File(book.coverImagePath),
                        contentDescription = "Cover for ${book.title}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Collections,
                            contentDescription = null,
                            tint = Gray400,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Pitch Black + Electric Cyan Page Count Pill
                Surface(
                    color = PitchBlack.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(5.dp)
                ) {
                    Text(
                        text = "${book.pageCount}P",
                        fontFamily = OutfitFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Project Metadata
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Text(
                    text = book.title,
                    fontFamily = OutfitFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PitchBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (book.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = book.description,
                        fontSize = 12.sp,
                        color = Gray600,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = StudioCanvas,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${book.pageCount} ${if (book.pageCount == 1) "Page" else "Pages"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PitchBlack,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = Gray500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Options Menu Button
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Project Options",
                        tint = PitchBlack,
                        modifier = Modifier.size(20.dp)
                    )
                }

                ProjectContextMenu(
                    expanded = menuExpanded,
                    onDismiss = { menuExpanded = false },
                    onOpen = onClick,
                    onDuplicate = onDuplicate,
                    onRename = onRename,
                    onDelete = onDelete
                )
            }
        }
    }
}

@Composable
private fun CapCutGridCoverCard(
    book: BookEntity,
    onClick: () -> Unit,
    onDuplicate: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val formattedDate = remember(book.updatedAt) {
        val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
        sdf.format(Date(book.updatedAt))
    }

    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Gray200),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
                    .background(StudioCanvas)
            ) {
                if (!book.coverImagePath.isNullOrEmpty()) {
                    AsyncImage(
                        model = File(book.coverImagePath),
                        contentDescription = book.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Collections,
                            contentDescription = null,
                            tint = Gray400,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                // Bottom-left Pitch Black Page Count Badge
                Surface(
                    color = PitchBlack.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "${book.pageCount} Pages",
                        fontFamily = OutfitFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Top-right 3-dot button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Surface(
                        color = PitchBlack.copy(alpha = 0.7f),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(30.dp)
                            .clickable { menuExpanded = true }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "More",
                                tint = PureWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    ProjectContextMenu(
                        expanded = menuExpanded,
                        onDismiss = { menuExpanded = false },
                        onOpen = onClick,
                        onDuplicate = onDuplicate,
                        onRename = onRename,
                        onDelete = onDelete
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = book.title,
                    fontFamily = OutfitFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PitchBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Edited $formattedDate",
                    fontSize = 11.sp,
                    color = Gray500
                )
            }
        }
    }
}

@Composable
private fun ProjectContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(PureWhite)
    ) {
        DropdownMenuItem(
            text = { Text("Open in Studio", color = PitchBlack, fontWeight = FontWeight.SemiBold) },
            leadingIcon = { Icon(Icons.Default.AutoStories, null, tint = PitchBlack) },
            onClick = {
                onDismiss()
                onOpen()
            }
        )
        DropdownMenuItem(
            text = { Text("Duplicate Draft", color = PitchBlack, fontWeight = FontWeight.Medium) },
            leadingIcon = { Icon(Icons.Default.ContentCopy, null, tint = ElectricCyanDark) },
            onClick = {
                onDismiss()
                onDuplicate()
            }
        )
        DropdownMenuItem(
            text = { Text("Rename", color = PitchBlack, fontWeight = FontWeight.Medium) },
            leadingIcon = { Icon(Icons.Default.Edit, null, tint = Gray600) },
            onClick = {
                onDismiss()
                onRename()
            }
        )
        DropdownMenuItem(
            text = { Text("Delete", color = CoralRed, fontWeight = FontWeight.SemiBold) },
            leadingIcon = { Icon(Icons.Default.Delete, null, tint = CoralRed) },
            onClick = {
                onDismiss()
                onDelete()
            }
        )
    }
}

@Composable
private fun CapCutEmptyProjectsState(
    isSearching: Boolean,
    onNewProjectClick: () -> Unit,
    onQuickImportClick: () -> Unit
) {
    Surface(
        color = StudioCanvas,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Gray200),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 36.dp)
        ) {
            Surface(
                color = PitchBlack,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Collections,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isSearching) "No matching projects found" else "No saved drafts yet",
                fontFamily = OutfitFontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PitchBlack
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isSearching) {
                    "Try a different keyword or clear the search filter."
                } else {
                    "Create a project or import comic pages straight from your gallery. Every change is auto-saved offline."
                },
                fontSize = 13.sp,
                color = Gray500,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )

            if (!isSearching) {
                Spacer(modifier = Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onQuickImportClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PitchBlack,
                            contentColor = PureWhite
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Quick Import", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onNewProjectClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PureWhite,
                            contentColor = PitchBlack
                        ),
                        border = BorderStroke(1.dp, Gray300),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("empty_state_create_button")
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Blank", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CreateBookDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PureWhite,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(
                text = "New Comic / Book Project",
                fontFamily = OutfitFontFamily,
                color = PitchBlack,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Give your project a title. You can add, reorder, and insert pages anytime.",
                    fontSize = 13.sp,
                    color = Gray600,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Project Title") },
                    placeholder = { Text("e.g., Manga Chapter 01") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PitchBlack,
                        unfocusedBorderColor = Gray300,
                        focusedLabelColor = PitchBlack,
                        cursorColor = PitchBlack,
                        focusedTextColor = PitchBlack,
                        unfocusedTextColor = PitchBlack
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_comic_title")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Notes / Subtitle (Optional)") },
                    placeholder = { Text("e.g., Full color panels") },
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PitchBlack,
                        unfocusedBorderColor = Gray300,
                        focusedLabelColor = PitchBlack,
                        cursorColor = PitchBlack,
                        focusedTextColor = PitchBlack,
                        unfocusedTextColor = PitchBlack
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title.trim(), description.trim()) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PitchBlack,
                    contentColor = ElectricCyan
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("submit_create_comic_button")
            ) {
                Text("Create Project", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Gray500, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
fun RenameBookDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (newTitle: String) -> Unit
) {
    var title by remember { mutableStateOf(currentTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PureWhite,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(
                text = "Rename Project",
                fontFamily = OutfitFontFamily,
                color = PitchBlack,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
            )
        },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PitchBlack,
                    unfocusedBorderColor = Gray300,
                    focusedLabelColor = PitchBlack,
                    cursorColor = PitchBlack,
                    focusedTextColor = PitchBlack,
                    unfocusedTextColor = PitchBlack
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title.trim()) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PitchBlack,
                    contentColor = ElectricCyan
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Gray500, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}
