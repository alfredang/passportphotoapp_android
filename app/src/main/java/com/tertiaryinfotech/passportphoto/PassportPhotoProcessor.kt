package com.tertiaryinfotech.passportphoto

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class ProcessingException(message: String) : Exception(message)

/**
 * Turns an arbitrary selfie into a compliant passport photo:
 * 1. Isolates the person with an ML Kit selfie-segmentation mask (removes background + distractors).
 * 2. Composites the person onto a pure white background.
 * 3. Crops & scales around the detected face to the chosen spec at 300 DPI.
 *
 * Everything runs on-device; nothing is uploaded.
 */
class PassportPhotoProcessor {

    suspend fun process(source: Bitmap, spec: PhotoSpec): Bitmap = withContext(Dispatchers.Default) {
        val input = downscaleIfNeeded(source, MAX_DIMENSION)
        val composited = removeBackground(input)
        val cropRect = faceAwareCropRect(input, spec)
        renderFinal(composited, cropRect, spec)
    }

    /** Segments the person and composites them over a pure white background. */
    private suspend fun removeBackground(bitmap: Bitmap): Bitmap {
        val segmenter = Segmentation.getClient(
            SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                .build()
        )
        val mask = try {
            segmenter.process(InputImage.fromBitmap(bitmap, 0)).await()
        } catch (e: Exception) {
            throw ProcessingException("We couldn't separate you from the background. Try better lighting.")
        } finally {
            segmenter.close()
        }

        val w = bitmap.width
        val h = bitmap.height
        val maskBuffer = mask.buffer
        maskBuffer.rewind()

        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        // The mask is returned at the input image size in SINGLE_IMAGE_MODE.
        var foregroundConfidenceTotal = 0.0
        for (i in pixels.indices) {
            val confidence = maskBuffer.float
            foregroundConfidenceTotal += confidence
            val p = pixels[i]
            val r = (Color.red(p) * confidence + 255f * (1f - confidence)).roundToInt().coerceIn(0, 255)
            val g = (Color.green(p) * confidence + 255f * (1f - confidence)).roundToInt().coerceIn(0, 255)
            val b = (Color.blue(p) * confidence + 255f * (1f - confidence)).roundToInt().coerceIn(0, 255)
            pixels[i] = Color.rgb(r, g, b)
        }

        // If almost nothing was classified as a person, treat it as "no person detected".
        if (foregroundConfidenceTotal / pixels.size < 0.01) {
            throw ProcessingException("No person was found in the photo. Make sure your face is clearly visible.")
        }

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    /** Computes the crop rectangle in top-left pixel coordinates from face geometry. */
    private suspend fun faceAwareCropRect(bitmap: Bitmap, spec: PhotoSpec): RectF {
        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .build()
        )
        val face = try {
            detector.process(InputImage.fromBitmap(bitmap, 0)).await()
                .maxByOrNull { it.boundingBox.width().toLong() * it.boundingBox.height() }
        } catch (e: Exception) {
            null
        } finally {
            detector.close()
        }
            ?: return centeredCrop(bitmap.width, bitmap.height, spec)

        val bb: Rect = face.boundingBox

        // ML Kit's box covers roughly forehead → below chin; extend to approximate crown → chin.
        val headHeight = bb.height() * 1.35
        val cropHeight = headHeight / spec.headHeightFraction
        val cropWidth = cropHeight * spec.aspectRatio

        // Prefer real eye landmarks for the eye line; fall back to a fraction of the face box.
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
        val eyeY = if (leftEye != null && rightEye != null) {
            (leftEye.y + rightEye.y) / 2.0
        } else {
            bb.top + bb.height() * 0.42
        }
        val faceCenterX = bb.exactCenterX().toDouble()

        val cropTop = eyeY - cropHeight * (1 - spec.eyeLineFromBottom)
        val cropLeft = faceCenterX - cropWidth / 2

        return RectF(
            cropLeft.toFloat(),
            cropTop.toFloat(),
            (cropLeft + cropWidth).toFloat(),
            (cropTop + cropHeight).toFloat(),
        )
    }

    /** Fallback: centered crop to the spec aspect ratio. */
    private fun centeredCrop(width: Int, height: Int, spec: PhotoSpec): RectF {
        val targetAR = spec.aspectRatio
        var cropW = width.toDouble()
        var cropH = cropW / targetAR
        if (cropH > height) {
            cropH = height.toDouble()
            cropW = cropH * targetAR
        }
        val left = (width - cropW) / 2
        val top = (height - cropH) / 2
        return RectF(left.toFloat(), top.toFloat(), (left + cropW).toFloat(), (top + cropH).toFloat())
    }

    /** Draws the composited image into a fixed-size white canvas so the crop rect maps to the full output. */
    private fun renderFinal(image: Bitmap, cropRect: RectF, spec: PhotoSpec): Bitmap {
        val out = Bitmap.createBitmap(spec.pixelWidth, spec.pixelHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE)

        val scale = spec.pixelHeight / cropRect.height()
        val drawLeft = -cropRect.left * scale
        val drawTop = -cropRect.top * scale
        val dst = RectF(drawLeft, drawTop, drawLeft + image.width * scale, drawTop + image.height * scale)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(image, null, dst, paint)
        return out
    }

    private fun downscaleIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val largest = max(bitmap.width, bitmap.height)
        if (largest <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / largest
        val w = min((bitmap.width * scale).roundToInt(), maxDimension)
        val h = min((bitmap.height * scale).roundToInt(), maxDimension)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    private companion object {
        /** Keeps segmentation + pixel blending fast and memory-safe on large camera images. */
        const val MAX_DIMENSION = 2400
    }
}
