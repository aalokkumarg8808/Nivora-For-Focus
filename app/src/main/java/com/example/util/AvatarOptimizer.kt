package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Utility for optimizing, resizing, and compressing user profile avatars.
 * - Maximum resolution: 512x512 (maintaining aspect ratio)
 * - Correct EXIF orientation handling
 * - JPEG compression @ 82% quality (target size < 300 KB)
 * - Strict Supabase Storage path: <user-uuid>/avatar.jpg
 */
object AvatarOptimizer {

    const val MAX_AVATAR_DIMENSION = 512
    const val JPEG_COMPRESSION_QUALITY = 82
    const val STORAGE_BUCKET_AVATARS = "avatars"

    /**
     * Returns the strict Supabase Storage relative object path for a given user ID.
     * Format: <user-uuid>/avatar.jpg
     */
    fun getStrictAvatarPath(userId: String): String {
        return "$userId/avatar.jpg"
    }

    /**
     * Compresses and resizes a picked image Uri from the gallery into an internal storage file.
     * Overwrites any existing local avatar file for this user cleanly.
     */
    fun compressAndSaveAvatar(context: Context, sourceUri: Uri, userId: String): File? {
        var originalBitmap: Bitmap? = null
        var rotatedBitmap: Bitmap? = null
        var scaledBitmap: Bitmap? = null

        try {
            // 1. Read EXIF orientation
            val orientation = getExifOrientation(context, sourceUri)

            // 2. Decode image bounds
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            // 3. Calculate optimal sample size to reduce memory usage during decode
            options.inSampleSize = calculateInSampleSize(origWidth, origHeight, MAX_AVATAR_DIMENSION * 2, MAX_AVATAR_DIMENSION * 2)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            // 4. Decode sampled bitmap
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                originalBitmap = BitmapFactory.decodeStream(input, null, options)
            }
            val loadedBitmap = originalBitmap ?: return null

            // 5. Rotate according to EXIF
            rotatedBitmap = rotateBitmap(loadedBitmap, orientation)

            // 6. Scale to max 512x512 maintaining aspect ratio
            val currentWidth = rotatedBitmap.width
            val currentHeight = rotatedBitmap.height
            val scaleFactor = min(1.0f, MAX_AVATAR_DIMENSION.toFloat() / max(currentWidth, currentHeight))

            val targetWidth = (currentWidth * scaleFactor).roundToInt().coerceAtLeast(1)
            val targetHeight = (currentHeight * scaleFactor).roundToInt().coerceAtLeast(1)

            scaledBitmap = if (targetWidth != currentWidth || targetHeight != currentHeight) {
                Bitmap.createScaledBitmap(rotatedBitmap, targetWidth, targetHeight, true)
            } else {
                rotatedBitmap
            }

            // 7. Compress into JPEG @ 82% quality
            val avatarsDir = File(context.filesDir, "profile_avatars").apply { mkdirs() }
            val destFile = File(avatarsDir, "avatar_${userId}.jpg")

            val byteStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_COMPRESSION_QUALITY, byteStream)
            val compressedBytes = byteStream.toByteArray()

            FileOutputStream(destFile).use { fos ->
                fos.write(compressedBytes)
                fos.flush()
            }

            return destFile
        } catch (e: Exception) {
            return null
        } finally {
            // Clean up intermediate Bitmaps
            try {
                if (originalBitmap != null && originalBitmap != rotatedBitmap && originalBitmap != scaledBitmap) {
                    originalBitmap?.recycle()
                }
                if (rotatedBitmap != null && rotatedBitmap != scaledBitmap) {
                    rotatedBitmap?.recycle()
                }
                if (scaledBitmap != null) {
                    scaledBitmap?.recycle()
                }
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }

    private fun getExifOrientation(context: Context, uri: Uri): Int {
        var inputStream: InputStream? = null
        return try {
            inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val exif = ExifInterface(inputStream)
                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } else {
                ExifInterface.ORIENTATION_NORMAL
            }
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        } finally {
            try {
                inputStream?.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return try {
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            rotated
        } catch (e: Exception) {
            bitmap
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
