package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExportResult
import com.example.data.PdfExportConfig
import com.example.data.PdfQuality
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanDark
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.Gray100
import com.example.ui.theme.Gray200
import com.example.ui.theme.Gray400
import com.example.ui.theme.Gray500
import com.example.ui.theme.Gray600
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StudioCanvas

@Composable
fun PdfExportDialog(
    comicTitle: String,
    pageCount: Int,
    exportState: ExportProgressState,
    onDismiss: () -> Unit,
    onStartExport: (PdfExportConfig) -> Unit
) {
    val context = LocalContext.current
    var selectedQuality by remember { mutableStateOf(PdfQuality.HIGH) }
    var fitOriginalAspect by remember { mutableStateOf(true) }
    var includePageNumbers by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            if (exportState !is ExportProgressState.Progress) {
                onDismiss()
            }
        },
        containerColor = PureWhite,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = PitchBlack,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Export PDF Book",
                        fontFamily = OutfitFontFamily,
                        color = PitchBlack,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp
                    )
                    Text(
                        text = "$pageCount Pages • \"$comicTitle\"",
                        color = Gray500,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        text = {
            AnimatedContent(targetState = exportState, label = "export_state_anim") { state ->
                when (state) {
                    is ExportProgressState.Idle -> {
                        ExportSettingsContent(
                            selectedQuality = selectedQuality,
                            onQualityChange = { selectedQuality = it },
                            fitOriginalAspect = fitOriginalAspect,
                            onFitAspectChange = { fitOriginalAspect = it },
                            includePageNumbers = includePageNumbers,
                            onIncludePageNumbersChange = { includePageNumbers = it }
                        )
                    }

                    is ExportProgressState.Progress -> {
                        ExportProgressContent(
                            current = state.current,
                            total = state.total
                        )
                    }

                    is ExportProgressState.Success -> {
                        ExportSuccessContent(
                            result = state.result,
                            context = context
                        )
                    }

                    is ExportProgressState.Error -> {
                        ExportErrorContent(
                            message = state.message,
                            onRetry = {
                                onStartExport(
                                    PdfExportConfig(
                                        title = comicTitle,
                                        quality = selectedQuality,
                                        includePageNumbers = includePageNumbers,
                                        fitToOriginalAspect = fitOriginalAspect
                                    )
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            when (exportState) {
                is ExportProgressState.Idle -> {
                    Button(
                        onClick = {
                            onStartExport(
                                PdfExportConfig(
                                    title = comicTitle,
                                    quality = selectedQuality,
                                    includePageNumbers = includePageNumbers,
                                    fitToOriginalAspect = fitOriginalAspect
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PitchBlack,
                            contentColor = ElectricCyan
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_start_export_pdf")
                    ) {
                        Text(
                            text = "Generate PDF",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                is ExportProgressState.Success -> {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PitchBlack,
                            contentColor = PureWhite
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }

                is ExportProgressState.Error -> {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Gray100,
                            contentColor = PitchBlack
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                }

                is ExportProgressState.Progress -> {
                    // Busy exporting
                }
            }
        },
        dismissButton = {
            if (exportState is ExportProgressState.Idle) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Gray500, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    )
}

@Composable
private fun ExportSettingsContent(
    selectedQuality: PdfQuality,
    onQualityChange: (PdfQuality) -> Unit,
    fitOriginalAspect: Boolean,
    onFitAspectChange: (Boolean) -> Unit,
    includePageNumbers: Boolean,
    onIncludePageNumbersChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Select Export Quality",
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Bold,
            color = PitchBlack,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        PdfQuality.entries.forEach { quality ->
            val isSelected = selectedQuality == quality
            Surface(
                color = if (isSelected) PitchBlack else StudioCanvas,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) PitchBlack else Gray200
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clickable { onQualityChange(quality) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onQualityChange(quality) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = ElectricCyan,
                            unselectedColor = Gray400
                        ),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = quality.label,
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isSelected) PureWhite else PitchBlack
                        )
                        Text(
                            text = quality.description,
                            fontSize = 11.sp,
                            color = if (isSelected) ElectricCyan else Gray500
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Fit to Image Aspect Ratio",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PitchBlack
                )
                Text(
                    text = "Zero white borders; true comic panels",
                    fontSize = 11.sp,
                    color = Gray500
                )
            }
            Switch(
                checked = fitOriginalAspect,
                onCheckedChange = onFitAspectChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ElectricCyan,
                    checkedTrackColor = PitchBlack,
                    uncheckedThumbColor = Gray400,
                    uncheckedTrackColor = Gray200
                )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Stamp Page Numbers",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PitchBlack
                )
                Text(
                    text = "Small page badge at bottom corner",
                    fontSize = 11.sp,
                    color = Gray500
                )
            }
            Switch(
                checked = includePageNumbers,
                onCheckedChange = onIncludePageNumbersChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ElectricCyan,
                    checkedTrackColor = PitchBlack,
                    uncheckedThumbColor = Gray400,
                    uncheckedTrackColor = Gray200
                )
            )
        }
    }
}

@Composable
private fun ExportProgressContent(
    current: Int,
    total: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = PitchBlack,
            modifier = Modifier.size(52.dp),
            strokeWidth = 4.dp
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Rendering PDF Book...",
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.ExtraBold,
            color = PitchBlack,
            fontSize = 17.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        val progressFraction = if (total > 0) current.toFloat() / total.toFloat() else 0f
        Text(
            text = "Page $current of $total (${(progressFraction * 100).toInt()}%)",
            color = Gray600,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(14.dp))

        LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = PitchBlack,
            trackColor = Gray200
        )
    }
}

@Composable
private fun ExportSuccessContent(
    result: ExportResult,
    context: Context
) {
    val formattedSize = remember(result.fileSizeBytes) {
        val kb = result.fileSizeBytes / 1024.0
        if (kb > 1024) {
            String.format(java.util.Locale.US, "%.1f MB", kb / 1024.0)
        } else {
            String.format(java.util.Locale.US, "%.0f KB", kb)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = EmeraldGreen.copy(alpha = 0.14f),
            shape = CircleShape,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldGreen,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "PDF Ready!",
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            color = PitchBlack
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${result.fileName} ($formattedSize)",
            fontSize = 12.sp,
            color = Gray500
        )

        Text(
            text = "Saved to Downloads/ComicMaker",
            fontSize = 12.sp,
            color = ElectricCyanDark,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(result.uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Open Comic PDF"))
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioCanvas,
                    contentColor = PitchBlack
                ),
                border = BorderStroke(1.dp, Gray200),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_open_pdf_viewer")
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, result.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Comic PDF"))
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PitchBlack,
                    contentColor = ElectricCyan
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_share_pdf_external")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ExportErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Export Failed",
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Bold,
            color = PitchBlack,
            fontSize = 17.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = message,
            color = Gray500,
            fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onRetry,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PitchBlack),
            border = BorderStroke(1.dp, PitchBlack),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Try Again", fontWeight = FontWeight.Bold)
        }
    }
}
