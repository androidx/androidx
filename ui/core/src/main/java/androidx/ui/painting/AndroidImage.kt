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

package androidx.ui.painting

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.DisplayMetrics
import androidx.annotation.RequiresApi
import androidx.ui.graphics.ColorSpace

fun imageFromResource(res: Resources, resId: Int): Image {
    return AndroidImage(BitmapFactory.decodeResource(res, resId))
}

/* actual */ fun Image(
    width: Int,
    height: Int,
    config: ImageConfig = ImageConfig.Argb8888,
    hasAlpha: Boolean = true,
    colorSpace: ColorSpace = ColorSpace.get(ColorSpace.Named.Srgb)
): Image {
    val bitmapConfig = config.toBitmapConfig()
    val bitmap: Bitmap
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // Note intentionally ignoring density in all cases
        bitmap = Bitmap.createBitmap(
            null,
            width,
            height,
            bitmapConfig,
            hasAlpha,
            colorSpace.toFrameworkColorSpace()
        )
    } else {
        bitmap = Bitmap.createBitmap(
            null as DisplayMetrics?,
            width,
            height,
            bitmapConfig
        )
    }
    return AndroidImage(bitmap)
}

// TODO njawad expand API surface with other alternatives for Image creation?
internal class AndroidImage(val bitmap: Bitmap) : Image {

    /**
     * @see Image.width
     */
    override val width: Int
        get() = bitmap.width

    /**
     * @see Image.height
     */
    override val height: Int
        get() = bitmap.height

    override val config: ImageConfig
        get() = bitmap.config.toImageConfig()

    /**
     * @see Image.colorSpace
     */
    override val colorSpace: ColorSpace
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            bitmap.colorSpace?.toComposeColorSpace() ?: ColorSpace.Named.Srgb.colorSpace
        } else {
            ColorSpace.Named.Srgb.colorSpace
        }

    /**
     * @see Image.hasAlpha
     */
    override val hasAlpha: Boolean
        get() = bitmap.hasAlpha()

    /**
     * @see Image.nativeImage
     */
    override val nativeImage: NativeImage
        get() = bitmap

    /**
     * @see
     */
    override fun prepareToDraw() {
        bitmap.prepareToDraw()
    }
}

internal fun ImageConfig.toBitmapConfig(): Bitmap.Config =
    when (this) {
        ImageConfig.Argb8888 -> Bitmap.Config.ARGB_8888
        ImageConfig.Alpha8 -> Bitmap.Config.ALPHA_8
        ImageConfig.Rgb565 -> Bitmap.Config.RGB_565
        else -> Bitmap.Config.ARGB_8888
    }

internal fun Bitmap.Config.toImageConfig(): ImageConfig {
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
        @Suppress("DEPRECATION")
        return when (this) {
            Bitmap.Config.ALPHA_8 -> ImageConfig.Alpha8
            Bitmap.Config.RGB_565 -> ImageConfig.Rgb565
            Bitmap.Config.ARGB_4444 -> ImageConfig.Argb8888 // Always upgrade to Argb_8888
            Bitmap.Config.ARGB_8888 -> ImageConfig.Argb8888
            Bitmap.Config.RGBA_F16 -> ImageConfig.F16
            Bitmap.Config.HARDWARE -> ImageConfig.Gpu
            else -> ImageConfig.Argb8888
        }
    } else {
        // Re implement when statement for older OS versions that do not support hardware
        // bitmaps to work around NoSuchFieldExceptions not being caught in Kotlin:
        // https://youtrack.jetbrains.com/issue/KT-30473
        @Suppress("DEPRECATION")
        return when (this) {
            Bitmap.Config.ALPHA_8 -> ImageConfig.Alpha8
            Bitmap.Config.RGB_565 -> ImageConfig.Rgb565
            Bitmap.Config.ARGB_4444 -> ImageConfig.Argb8888 // Always upgrade to Argb_8888
            Bitmap.Config.ARGB_8888 -> ImageConfig.Argb8888
            else -> ImageConfig.Argb8888
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun ColorSpace.toFrameworkColorSpace(): android.graphics.ColorSpace {
    val frameworkNamedSpace = when (this) {
        ColorSpace.Named.Srgb.colorSpace -> android.graphics.ColorSpace.Named.SRGB
        ColorSpace.Named.Aces.colorSpace -> android.graphics.ColorSpace.Named.ACES
        ColorSpace.Named.Acescg.colorSpace -> android.graphics.ColorSpace.Named.ACESCG
        ColorSpace.Named.AdobeRgb.colorSpace -> android.graphics.ColorSpace.Named.ADOBE_RGB
        ColorSpace.Named.Bt2020.colorSpace -> android.graphics.ColorSpace.Named.BT2020
        ColorSpace.Named.Bt709.colorSpace -> android.graphics.ColorSpace.Named.BT709
        ColorSpace.Named.CieLab.colorSpace -> android.graphics.ColorSpace.Named.CIE_LAB
        ColorSpace.Named.CieXyz.colorSpace -> android.graphics.ColorSpace.Named.CIE_XYZ
        ColorSpace.Named.DciP3.colorSpace -> android.graphics.ColorSpace.Named.DCI_P3
        ColorSpace.Named.DisplayP3.colorSpace -> android.graphics.ColorSpace.Named.DISPLAY_P3
        ColorSpace.Named.ExtendedSrgb.colorSpace -> android.graphics.ColorSpace.Named.EXTENDED_SRGB
        ColorSpace.Named.LinearExtendedSrgb.colorSpace ->
            android.graphics.ColorSpace.Named.LINEAR_EXTENDED_SRGB
        ColorSpace.Named.LinearSrgb.colorSpace -> android.graphics.ColorSpace.Named.LINEAR_SRGB
        ColorSpace.Named.Ntsc1953.colorSpace -> android.graphics.ColorSpace.Named.NTSC_1953
        ColorSpace.Named.ProPhotoRgb.colorSpace -> android.graphics.ColorSpace.Named.PRO_PHOTO_RGB
        ColorSpace.Named.SmpteC.colorSpace -> android.graphics.ColorSpace.Named.SMPTE_C
        else -> android.graphics.ColorSpace.Named.SRGB
    }
    return android.graphics.ColorSpace.get(frameworkNamedSpace)
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun android.graphics.ColorSpace.toComposeColorSpace(): ColorSpace {
    return when (this) {
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SRGB)
            -> ColorSpace.Named.Srgb.colorSpace
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.ACES)
            -> ColorSpace.Named.Aces.colorSpace
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.ACESCG)
            -> ColorSpace.Named.Acescg.colorSpace
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.ADOBE_RGB)
            -> ColorSpace.Named.AdobeRgb.colorSpace
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.BT2020)
            -> ColorSpace.Named.Bt2020.colorSpace
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.BT709)
            -> ColorSpace.Named.Bt709.colorSpace
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.CIE_LAB)
            -> ColorSpace.Named.CieLab.colorSpace
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.CIE_XYZ)
            -> ColorSpace.Named.CieXyz.colorSpace
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.DCI_P3)
            -> ColorSpace.Named.DciP3.colorSpace
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.DISPLAY_P3)
            -> ColorSpace.Named.DisplayP3.colorSpace
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.EXTENDED_SRGB)
            -> ColorSpace.Named.ExtendedSrgb.colorSpace
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.LINEAR_EXTENDED_SRGB)
            -> ColorSpace.Named.LinearExtendedSrgb.colorSpace
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.LINEAR_SRGB)
            -> ColorSpace.Named.LinearSrgb.colorSpace
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.NTSC_1953)
            -> ColorSpace.Named.Ntsc1953.colorSpace
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.PRO_PHOTO_RGB)
            -> ColorSpace.Named.ProPhotoRgb.colorSpace
        android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SMPTE_C)
            -> ColorSpace.Named.SmpteC.colorSpace
        else -> ColorSpace.Named.Srgb.colorSpace
    }
}