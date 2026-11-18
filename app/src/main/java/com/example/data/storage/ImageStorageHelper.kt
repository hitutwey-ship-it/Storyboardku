package com.example.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class ImageStorageHelper(private val context: Context) {

    companion object {
        const val MAX_IMAGE_BYTES = 5L * 1024L * 1024L // 5 MB limit per image
        private const val MAX_DIMENSION_PX = 1280
    }

    private val imageDir: File
        get() = File(context.filesDir, "storyboard_images").apply { mkdirs() }

    /**
     * Imports a user-selected image Uri, checks size/dimensions, compresses it cleanly to JPEG <= 5 MB,
     * and stores it in internal app storage for fast offline access.
     */
    suspend fun importAndCompressImage(uri: Uri, prefix: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("Tidak bisa membaca file gambar.")

            if (rawBytes.size > 20L * 1024L * 1024L) {
                error("Ukuran file terlalu besar (> 20 MB). Pilih gambar di bawah 5 MB.")
            }

            // Decode bounds first to prevent OOM on large phone camera photos
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOpts)

            var sampleSize = 1
            while ((boundsOpts.outWidth / sampleSize) > MAX_DIMENSION_PX * 2 ||
                (boundsOpts.outHeight / sampleSize) > MAX_DIMENSION_PX * 2
            ) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts)
                ?: error("Format gambar tidak didukung.")

            val scaled = scaleDownIfNeeded(decoded, MAX_DIMENSION_PX)
            if (scaled !== decoded) decoded.recycle()

            var quality = 88
            var compressedBytes: ByteArray
            do {
                val baos = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                compressedBytes = baos.toByteArray()
                quality -= 10
            } while (compressedBytes.size > MAX_IMAGE_BYTES && quality >= 30)

            scaled.recycle()

            if (compressedBytes.size > MAX_IMAGE_BYTES) {
                error("Gambar masih melebihi batas 5 MB setelah kompresi.")
            }

            val targetFile = File(imageDir, "${prefix}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(targetFile).use { it.write(compressedBytes) }
            targetFile.absolutePath
        }
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val ratio = minOf(maxDim.toFloat() / w, maxDim.toFloat() / h)
        val newW = (w * ratio).toInt().coerceAtLeast(1)
        val newH = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    suspend fun getCacheAndImageSizeBytes(referencedPaths: Set<String>): Pair<Long, Long> =
        withContext(Dispatchers.IO) {
            val cacheBytes = dirSize(context.cacheDir)
            val orphanImageBytes = imageDir.listFiles()
                ?.filter { it.absolutePath !in referencedPaths }
                ?.sumOf { it.length() } ?: 0L
            cacheBytes to orphanImageBytes
        }

    suspend fun clearUnusedCache(referencedPaths: Set<String>): Long = withContext(Dispatchers.IO) {
        var freed = 0L
        context.cacheDir.listFiles()?.forEach { file ->
            val len = fileSize(file)
            if (file.deleteRecursively()) freed += len
        }
        imageDir.listFiles()?.forEach { file ->
            if (file.absolutePath !in referencedPaths) {
                val len = file.length()
                if (file.delete()) freed += len
            }
        }
        freed
    }

    private fun dirSize(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        return file.listFiles()?.sumOf { dirSize(it) } ?: 0L
    }

    private fun fileSize(file: File): Long = dirSize(file)
}
