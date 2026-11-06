/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:JvmName("BitmapUtils")

package androidx.xr.testutils

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.util.Log
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

private const val TAG = "BitmapUtils"

/** Default color distance tolerance for detecting foreground content against a solid background. */
public const val DEFAULT_FOREGROUND_COLOR_TOLERANCE: Int = 48

/**
 * Automatically detects the background color (from the frame corners) and crops [bitmap] to its
 * foreground content bounds (plus optional [padding]), removing the surrounding empty background.
 *
 * This is particularly useful for capturing scenes containing 3D entities (such as GLTF models,
 * meshes, and particle systems) that do not have 2D surface bounds in CPM.
 *
 * @param bitmap Source [Bitmap] from the XR compositor.
 * @param padding Extra padding in pixels to preserve around the cropped content bounds.
 * @param backgroundColor Optional explicit background color. If null, the background color is
 *   automatically detected by sampling the corner pixels.
 * @return A software-backed [Bitmap.Config.ARGB_8888] [Bitmap] containing the cropped foreground.
 */
@JvmOverloads
public fun cropForeground(
    bitmap: Bitmap,
    padding: Int = 0,
    backgroundColor: Int? = null,
): Bitmap {
    val softwareBitmap = toSoftwareBitmap(bitmap)
    val width = softwareBitmap.width
    val height = softwareBitmap.height
    val pixels = toPixelArray(softwareBitmap)
    val bgColor = backgroundColor ?: detectBackgroundColor(pixels, width, height)
    val fullBounds = Rect(0, 0, width, height)
    val contentBounds =
        findForegroundBounds(pixels, width, height, fullBounds, bgColor) ?: return softwareBitmap

    val safePadding = max(0, padding)
    val finalBounds =
        Rect(
            (contentBounds.left - safePadding).coerceAtLeast(0),
            (contentBounds.top - safePadding).coerceAtLeast(0),
            (contentBounds.right + safePadding).coerceAtMost(width),
            (contentBounds.bottom + safePadding).coerceAtMost(height),
        )

    val cropped =
        try {
            crop(softwareBitmap, finalBounds)
        } catch (t: Throwable) {
            if (softwareBitmap !== bitmap && !softwareBitmap.isRecycled) {
                softwareBitmap.recycle()
            }
            throw t
        }
    if (cropped !== softwareBitmap && softwareBitmap !== bitmap && !softwareBitmap.isRecycled) {
        softwareBitmap.recycle()
    }
    return cropped
}

/**
 * Crops [bitmap] to [cropBounds]. If [cropBounds] extends outside the bounds of [bitmap], it is
 * clamped to the bitmap's dimensions.
 */
public fun crop(bitmap: Bitmap, cropBounds: Rect): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val clamped = Rect(cropBounds)
    require(clamped.intersect(0, 0, width, height)) {
        "cropBounds $cropBounds does not overlap the ${width}x${height} screenshot"
    }
    if (clamped != cropBounds) {
        Log.w(
            TAG,
            "cropBounds $cropBounds clamped to $clamped to fit the ${width}x${height} screenshot",
        )
    }
    return Bitmap.createBitmap(bitmap, clamped.left, clamped.top, clamped.width(), clamped.height())
}

/** Returns whether [bitmap] is backed by a GPU hardware buffer (`Bitmap.Config.HARDWARE`). */
public fun isHardwareBitmap(bitmap: Bitmap): Boolean =
    Build.VERSION.SDK_INT >= 26 && bitmap.config == Bitmap.Config.HARDWARE

/**
 * Converts [bitmap] to a software-backed [Bitmap.Config.ARGB_8888] bitmap if it is currently a
 * hardware bitmap. If [bitmap] is already a software bitmap, returns [bitmap] unmodified.
 */
public fun toSoftwareBitmap(bitmap: Bitmap): Bitmap =
    if (isHardwareBitmap(bitmap)) {
        checkNotNull(bitmap.copy(Bitmap.Config.ARGB_8888, false)) {
            "Failed to copy HARDWARE screenshot bitmap to ARGB_8888"
        }
    } else {
        bitmap
    }

/** Scales [bitmap] to [targetWidth] x [targetHeight] using bilinear interpolation. */
public fun scaleBilinear(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    if (width == targetWidth && height == targetHeight) return bitmap
    require(targetWidth > 0 && targetHeight > 0) {
        "Target dimensions must be positive: ${targetWidth}x${targetHeight}"
    }
    val srcW = width
    val srcH = height
    val srcPixels = toPixelArray(bitmap)
    val outPixels = IntArray(targetWidth * targetHeight)
    val scaleX = if (targetWidth > 1) (srcW - 1).toFloat() / (targetWidth - 1) else 0f
    val scaleY = if (targetHeight > 1) (srcH - 1).toFloat() / (targetHeight - 1) else 0f
    for (oy in 0 until targetHeight) {
        val sy = (oy * scaleY).coerceIn(0f, (srcH - 1).toFloat())
        val y0 = floor(sy).toInt().coerceIn(0, max(0, srcH - 2))
        val y1 = min(srcH - 1, y0 + 1)
        val fy = sy - y0
        val outRow = oy * targetWidth
        val row0 = y0 * srcW
        val row1 = y1 * srcW
        for (ox in 0 until targetWidth) {
            val sx = (ox * scaleX).coerceIn(0f, (srcW - 1).toFloat())
            val x0 = floor(sx).toInt().coerceIn(0, max(0, srcW - 2))
            val x1 = min(srcW - 1, x0 + 1)
            val fx = sx - x0
            val w00 = (1f - fx) * (1f - fy)
            val w10 = fx * (1f - fy)
            val w01 = (1f - fx) * fy
            val w11 = fx * fy
            val p00 = srcPixels[row0 + x0]
            val p10 = srcPixels[row0 + x1]
            val p01 = srcPixels[row1 + x0]
            val p11 = srcPixels[row1 + x1]
            val a =
                ((p00 ushr 24) * w00 +
                        (p10 ushr 24) * w10 +
                        (p01 ushr 24) * w01 +
                        (p11 ushr 24) * w11 +
                        0.5f)
                    .toInt()
                    .coerceIn(0, 255)
            val r =
                (((p00 shr 16) and 0xFF) * w00 +
                        ((p10 shr 16) and 0xFF) * w10 +
                        ((p01 shr 16) and 0xFF) * w01 +
                        ((p11 shr 16) and 0xFF) * w11 +
                        0.5f)
                    .toInt()
                    .coerceIn(0, 255)
            val g =
                (((p00 shr 8) and 0xFF) * w00 +
                        ((p10 shr 8) and 0xFF) * w10 +
                        ((p01 shr 8) and 0xFF) * w01 +
                        ((p11 shr 8) and 0xFF) * w11 +
                        0.5f)
                    .toInt()
                    .coerceIn(0, 255)
            val b =
                ((p00 and 0xFF) * w00 +
                        (p10 and 0xFF) * w10 +
                        (p01 and 0xFF) * w01 +
                        (p11 and 0xFF) * w11 +
                        0.5f)
                    .toInt()
                    .coerceIn(0, 255)
            outPixels[outRow + ox] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
    return Bitmap.createBitmap(outPixels, targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
}

/**
 * Extracts pixels from [bitmap] into [destination] (or a newly allocated array if null or size does
 * not match).
 */
@JvmOverloads
public fun toPixelArray(bitmap: Bitmap, destination: IntArray? = null): IntArray {
    val width = bitmap.width
    val height = bitmap.height
    val pixelCount = width * height
    val pixels =
        if (destination != null && destination.size == pixelCount) {
            destination
        } else {
            IntArray(pixelCount)
        }
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    return pixels
}

/** Detects the dominant background color of a pixel array by sampling the four corner pixels. */
public fun detectBackgroundColor(pixels: IntArray, width: Int, height: Int): Int {
    if (pixels.isEmpty() || width <= 0 || height <= 0) return 0xFF000000.toInt()
    val corners =
        intArrayOf(
            pixels[0],
            pixels[width - 1],
            pixels[(height - 1) * width],
            pixels[height * width - 1],
        )
    var bestColor = corners[0]
    var bestMatches = 0
    for (candidate in corners) {
        var matches = 0
        for (other in corners) {
            if (!isForegroundPixel(other, candidate)) {
                matches++
            }
        }
        if (matches > bestMatches) {
            bestMatches = matches
            bestColor = candidate
        }
    }
    return bestColor
}

/**
 * Returns whether [pixel] differs from [bgColor] beyond [tolerance] in any channel (A, R, G, B).
 */
@JvmOverloads
public fun isForegroundPixel(
    pixel: Int,
    bgColor: Int,
    tolerance: Int = DEFAULT_FOREGROUND_COLOR_TOLERANCE,
): Boolean {
    val dr = abs(((pixel shr 16) and 0xFF) - ((bgColor shr 16) and 0xFF))
    val dg = abs(((pixel shr 8) and 0xFF) - ((bgColor shr 8) and 0xFF))
    val db = abs((pixel and 0xFF) - (bgColor and 0xFF))
    val da = abs(((pixel ushr 24) and 0xFF) - ((bgColor ushr 24) and 0xFF))
    return dr > tolerance || dg > tolerance || db > tolerance || da > tolerance
}

/**
 * Finds the bounding rectangle of foreground pixels within [searchRect]. Returns `null` if no
 * foreground pixels are found.
 */
public fun findForegroundBounds(
    pixels: IntArray,
    width: Int,
    height: Int,
    searchRect: Rect,
    bgColor: Int,
): Rect? {
    val left = searchRect.left.coerceIn(0, width)
    val top = searchRect.top.coerceIn(0, height)
    val right = searchRect.right.coerceIn(left, width)
    val bottom = searchRect.bottom.coerceIn(top, height)
    var minX = right
    var minY = bottom
    var maxX = -1
    var maxY = -1
    for (y in top until bottom) {
        val rowOffset = y * width
        for (x in left until right) {
            if (isForegroundPixel(pixels[rowOffset + x], bgColor)) {
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }
    }
    return if (maxX >= minX && maxY >= minY) {
        Rect(minX, minY, maxX + 1, maxY + 1)
    } else {
        null
    }
}
