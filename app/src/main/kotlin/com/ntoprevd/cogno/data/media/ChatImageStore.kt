package com.ntoprevd.cogno.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class StoredChatImage(
    val path: String,
    val mimeType: String
)

class ChatImageStore(context: Context) {
    private val appContext = context.applicationContext

    suspend fun storeCompressed(uri: Uri): StoredChatImage = withContext(Dispatchers.IO) {
        val source = ImageDecoder.createSource(appContext.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val width = info.size.width
            val height = info.size.height
            val longestSide = maxOf(width, height)
            if (longestSide > MAX_IMAGE_SIDE) {
                val scale = MAX_IMAGE_SIDE.toFloat() / longestSide
                decoder.setTargetSize(
                    (width * scale).toInt().coerceAtLeast(1),
                    (height * scale).toInt().coerceAtLeast(1)
                )
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }

        val imageDirectory = File(appContext.filesDir, IMAGE_DIRECTORY).apply { mkdirs() }
        val outputFile = File(imageDirectory, "${UUID.randomUUID()}.jpg")
        try {
            FileOutputStream(outputFile).use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                    "图片压缩失败"
                }
            }
        } catch (error: Throwable) {
            outputFile.delete()
            throw error
        } finally {
            bitmap.recycle()
        }

        StoredChatImage(
            path = outputFile.absolutePath,
            mimeType = JPEG_MIME_TYPE
        )
    }

    companion object {
        private const val IMAGE_DIRECTORY = "chat_images"
        private const val MAX_IMAGE_SIDE = 1600
        private const val JPEG_QUALITY = 82
        private const val JPEG_MIME_TYPE = "image/jpeg"
    }
}
