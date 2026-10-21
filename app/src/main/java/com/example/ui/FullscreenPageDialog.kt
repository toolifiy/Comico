package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.BookPageEntity
import com.example.ui.theme.CapCutBlue
import com.example.ui.theme.Gray400
import com.example.ui.theme.Gray800
import com.example.ui.theme.PureWhite
import java.io.File

@Composable
fun FullscreenPageDialog(
    initialIndex: Int,
    pages: List<BookPageEntity>,
    onDismiss: () -> Unit
) {
    if (pages.isEmpty() || initialIndex !in pages.indices) {
        onDismiss()
        return
    }

    var currentIndex by remember { mutableIntStateOf(initialIndex) }
    val currentPage = pages[currentIndex]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Main Comic Page Image
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = File(currentPage.imagePath),
                    contentDescription = "Page ${currentIndex + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top Control Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Page indicator badge
                Surface(
                    color = Gray800.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Page ${currentIndex + 1} of ${pages.size}",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                // Close Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Gray800.copy(alpha = 0.9f), CircleShape)
                        .testTag("btn_close_fullscreen_preview")
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close Preview",
                        tint = PureWhite
                    )
                }
            }

            // Bottom Navigation Prev / Next Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (currentIndex > 0) currentIndex--
                    },
                    enabled = currentIndex > 0,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (currentIndex > 0) Gray800.copy(alpha = 0.9f) else Color.Transparent,
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Page",
                        tint = if (currentIndex > 0) PureWhite else Gray400.copy(alpha = 0.3f)
                    )
                }

                // Dimension details
                if (currentPage.width > 0 && currentPage.height > 0) {
                    Surface(
                        color = Gray800.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${currentPage.width} x ${currentPage.height} px",
                            color = Gray400,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        if (currentIndex < pages.lastIndex) currentIndex++
                    },
                    enabled = currentIndex < pages.lastIndex,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (currentIndex < pages.lastIndex) Gray800.copy(alpha = 0.9f) else Color.Transparent,
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Page",
                        tint = if (currentIndex < pages.lastIndex) PureWhite else Gray400.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}
