package com.example.quanlysachcanhan.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageStorageHelper {

    /**
     * Copy ảnh được chọn vào thư mục external files của app.
     * Repository/Activity chỉ cần lưu đường dẫn String trả về vào SQLite.
     */
    fun copyImageToAppStorage(
        context: Context,
        sourceUri: Uri
    ): String? {
        val directory = File(
            context.getExternalFilesDir(null),
            "covers"
        )

        if (!directory.exists()) {
            directory.mkdirs()
        }

        val targetFile = File(
            directory,
            "cover_${System.currentTimeMillis()}.jpg"
        )

        return runCatching {
            context.contentResolver.openInputStream(sourceUri).use { input ->
                FileOutputStream(targetFile).use { output ->
                    requireNotNull(input)
                    input.copyTo(output)
                }
            }
            targetFile.absolutePath
        }.getOrNull()
    }

    fun createTempCameraUri(context: Context): Pair<File, Uri>? {
        return runCatching {
            val cacheDir = File(context.cacheDir, "camera")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val tempFile = File(cacheDir, "capture_${System.currentTimeMillis()}.jpg")
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            Pair(tempFile, uri)
        }.getOrNull()
    }

    fun deleteImage(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }
}
