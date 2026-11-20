package studylock.app.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

/**
 * Copies a user-chosen picture into app-private storage. Nothing leaves the
 * phone and the source URI is not retained after import.
 */
class ProfilePhotoStore(private val context: Context) {

    private val directory: File
        get() = File(context.filesDir, PHOTO_DIR).apply { if (!exists()) mkdirs() }

    private val photoFile: File get() = File(directory, PHOTO_NAME)

    fun save(uri: Uri): String? {
        return try {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val drawable = ImageDecoder.decodeDrawable(source) { decoder, info, _ ->
            val scale = min(MAX_SIZE.toFloat() / info.size.width, MAX_SIZE.toFloat() / info.size.height)
            val scaled = min(scale, 1f)
            decoder.setTargetSize(
                (info.size.width * scaled).toInt().coerceAtLeast(1),
                (info.size.height * scaled).toInt().coerceAtLeast(1)
            )
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: return null
        FileOutputStream(photoFile).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
        }
        PHOTO_NAME
        } catch (e: Exception) {
            null
        }
    }

    fun file(fileName: String?): File? {
        if (fileName != PHOTO_NAME) return null
        return photoFile.takeIf { it.exists() }
    }

    fun delete(fileName: String?) {
        if (fileName == PHOTO_NAME) photoFile.delete()
    }

    fun decode(fileName: String?): Bitmap? = file(fileName)?.let { BitmapFactory.decodeFile(it.path) }

    private companion object {
        const val PHOTO_DIR = "profile"
        const val PHOTO_NAME = "avatar.jpg"
        const val MAX_SIZE = 512
        const val JPEG_QUALITY = 85
    }
}
