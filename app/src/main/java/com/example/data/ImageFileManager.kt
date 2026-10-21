package com.example.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class SavedImageInfo(
    val filePath: String,
    val width: Int,
    val height: Int
)

class ImageFileManager(private val context: Context) {

    private fun getBookDir(bookId: Long): File {
        val dir = File(context.filesDir, "books/$bookId")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun saveImageFromUri(bookId: Long, uri: Uri): SavedImageInfo? = withContext(Dispatchers.IO) {
        try {
            val bookDir = getBookDir(bookId)
            val fileName = "page_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
            val targetFile = File(bookDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return@withContext null

            // Read image dimensions without loading entire bitmap to memory
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(targetFile.absolutePath, options)
            val width = if (options.outWidth > 0) options.outWidth else 1080
            val height = if (options.outHeight > 0) options.outHeight else 1920

            SavedImageInfo(
                filePath = targetFile.absolutePath,
                width = width,
                height = height
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun copyExistingImage(sourcePath: String, targetBookId: Long): String? = withContext(Dispatchers.IO) {
        try {
            val srcFile = File(sourcePath)
            if (!srcFile.exists()) return@withContext null
            val targetDir = getBookDir(targetBookId)
            val fileName = "page_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
            val destFile = File(targetDir, fileName)
            srcFile.copyTo(destFile, overwrite = true)
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun deleteImageFile(filePath: String) = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteBookDirectory(bookId: Long) = withContext(Dispatchers.IO) {
        try {
            val dir = getBookDir(bookId)
            if (dir.exists()) {
                dir.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
