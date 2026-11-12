package com.example.storage

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class SharedFileItem(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val isAppCacheFile: Boolean = false
)

class StorageManagerHelper(private val context: Context) {

    fun getAppCacheSize(): Long {
        var size = getFolderSize(context.cacheDir)
        context.externalCacheDir?.let {
            size += getFolderSize(it)
        }
        return size
    }

    private fun getFolderSize(file: File?): Long {
        if (file == null || !file.exists()) return 0L
        var length: Long = 0
        val files = file.listFiles() ?: return 0L
        for (f in files) {
            length += if (f.isFile) f.length() else getFolderSize(f)
        }
        return length
    }

    suspend fun clearAppCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            deleteDir(context.cacheDir)
            context.externalCacheDir?.let { deleteDir(it) }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list() ?: return true
            for (child in children) {
                val success = deleteDir(File(dir, child))
                if (!success) return false
            }
            return dir.delete()
        } else if (dir != null && dir.isFile) {
            return dir.delete()
        }
        return false
    }

    suspend fun resolveFileInfo(uri: Uri): SharedFileItem? = withContext(Dispatchers.IO) {
        try {
            var name = "Unknown File"
            var size = 0L
            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex) ?: name
                    }
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
            SharedFileItem(
                uri = uri,
                name = name,
                sizeBytes = size,
                mimeType = mime,
                isAppCacheFile = false
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun deleteSharedFile(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            DocumentsContract.deleteDocument(context.contentResolver, uri)
        } catch (_: Exception) {
            try {
                context.contentResolver.delete(uri, null, null) > 0
            } catch (_: Exception) {
                false
            }
        }
    }
}
