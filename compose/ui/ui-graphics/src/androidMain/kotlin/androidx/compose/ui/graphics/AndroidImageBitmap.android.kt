/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.graphics

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.DisplayMetrics
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces

/**
 * Create an [ImageBitmap] from the given [Bitmap]. Note this does not create a copy of the original
 * [Bitmap] and changes to it will modify the returned [ImageBitmap]
 */
fun Bitmap.asImageBitmap(): ImageBitmap = AndroidImageBitmap(this)

internal actual fun createImageBitmap(bytes: ByteArray): ImageBitmap {
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
}

internal actual fun ActualImageBitmap(
    width: Int,
    height: Int,
    config: ImageBitmapConfig,
    hasAlpha: Boolean,
    colorSpace: ColorSpace
): ImageBitmap {
    val bitmapConfig = config.toBitmapConfig()
    val bitmap: Bitmap
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        bitmap = Api26Bitmap.createBitmap(width, height, config, hasAlpha, colorSpace)
    } else {
        bitmap = Bitmap.createBitmap(null as DisplayMetrics?, width, height, bitmapConfig)
        bitmap.setHasAlpha(hasAlpha)
    }
    return AndroidImageBitmap(bitmap)
}

/**
 * @Throws UnsupportedOperationException if this [ImageBitmap] is not backed by an
 *   android.graphics.Bitmap
 */
fun ImageBitmap.asAndroidBitmap(): Bitmap =
    when (this) {
        is AndroidImageBitmap -> bitmap
        else -> throw UnsupportedOperationException("Unable to obtain android.graphics.Bitmap")
    }

internal class AndroidImageBitmap(internal val bitmap: Bitmap) : ImageBitmap {

    override val width: Int
        get() = bitmap.width

    override val height: Int
        get() = bitmap.height

    override val config: ImageBitmapConfig
        get() = bitmap.config!!.toImageConfig()

    override val colorSpace: ColorSpace
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                with(Api26Bitmap) { bitmap.composeColorSpace() }
            } else {
                ColorSpaces.Srgb
            }

    override fun readPixels(
        buffer: IntArray,
        startX: Int,
        startY: Int,
        width: Int,
        height: Int,
        bufferOffset: Int,
        stride: Int
    ) {
        // Internal Android implementation that copies the pixels from the underlying
        // android.graphics.Bitmap if the configuration supports it
        val androidBitmap = asAndroidBitmap()
        var recycleTarget = false
        val targetBitmap =
            if (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                    androidBitmap.config != Bitmap.Config.HARDWARE
            ) {
                androidBitmap
            } else {
                // Because we are creating a copy for the purposes of reading pixels out of it
                // be sure to recycle this temporary bitmap when we are finished with it.
                recycleTarget = true

                // Pixels of a hardware bitmap cannot be queried directly so make a copy
                // of it into a configuration that can be queried
                // Passing in false for the isMutable parameter as we only intend to read pixel
                // information from the bitmap
                androidBitmap.copy(Bitmap.Config.ARGB_8888, false)
            }

        targetBitmap.getPixels(buffer, bufferOffset, stride, startX, startY, width, height)
        // Recycle the target if we are done with it
        if (recycleTarget) {
            targetBitmap.recycle()
        }
    }

    override val hasAlpha: Boolean
        get() = bitmap.hasAlpha()

    override fun prepareToDraw() {
        bitmap.prepareToDraw()
    }
}

internal fun ImageBitmapConfig.toBitmapConfig(): Bitmap.Config {
    // Cannot utilize when statements with enums that may have different sets of supported
    // values between the compiled SDK and the platform version of the device.
    // As a workaround use if/else statements
    // See https://youtrack.jetbrains.com/issue/KT-30473 for details
    return if (this == ImageBitmapConfig.Argb8888) {
        Bitmap.Config.ARGB_8888
    } else if (this == ImageBitmapConfig.Alpha8) {
        Bitmap.Config.ALPHA_8
    } else if (this == ImageBitmapConfig.Rgb565) {
        Bitmap.Config.RGB_565
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && this == ImageBitmapConfig.F16) {
        Bitmap.Config.RGBA_F16
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && this == ImageBitmapConfig.Gpu) {
        Bitmap.Config.HARDWARE
    } else {
        Bitmap.Config.ARGB_8888
    }
}

internal fun Bitmap.Config.toImageConfig(): ImageBitmapConfig {
    // Cannot utilize when statements with enums that may have different sets of supported
    // values between the compiled SDK and the platform version of the device.
    // As a workaround use if/else statements
    // See https://youtrack.jetbrains.com/issue/KT-30473 for details
    @Suppress("DEPRECATION")
    return if (this == Bitmap.Config.ALPHA_8) {
        ImageBitmapConfig.Alpha8
    } else if (this == Bitmap.Config.RGB_565) {
        ImageBitmapConfig.Rgb565
    } else if (this == Bitmap.Config.ARGB_4444) {
        ImageBitmapConfig.Argb8888 // Always upgrade to Argb_8888
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && this == Bitmap.Config.RGBA_F16) {
        ImageBitmapConfig.F16
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && this == Bitmap.Config.HARDWARE) {
        ImageBitmapConfig.Gpu
    } else {
        ImageBitmapConfig.Argb8888
    }
}

/**
 * Make Lint happy Separate class to contain all API calls that require API level 26 to assist in
 * dead code elimination during compilation time
 */
@RequiresApi(Build.VERSION_CODES.O)
internal object Api26Bitmap {
    @JvmStatic
    internal fun createBitmap(
        width: Int,
        height: Int,
        bitmapConfig: ImageBitmapConfig,
        hasAlpha: Boolean,
        colorSpace: ColorSpace
    ): Bitmap {
        // Note intentionally ignoring density in all cases
        return Bitmap.createBitmap(
            null,
            width,
            height,
            bitmapConfig.toBitmapConfig(),
            hasAlpha,
            colorSpace.toAndroidColorSpace()
        )
    }

    @JvmStatic
    internal fun Bitmap.composeColorSpace() = colorSpace?.toComposeColorSpace() ?: ColorSpaces.Srgb
}
