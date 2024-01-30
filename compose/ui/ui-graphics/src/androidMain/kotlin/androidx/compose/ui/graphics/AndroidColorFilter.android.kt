/*
 * Copyright 2021 The Android Open Source Project
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

import android.graphics.BlendModeColorFilter as AndroidBlendModeColorFilter
import android.graphics.ColorMatrix as AndroidColorMatrix
import android.graphics.ColorMatrixColorFilter as AndroidColorMatrixColorFilter
import android.graphics.LightingColorFilter as AndroidLightingColorFilter
import android.graphics.PorterDuffColorFilter as AndroidPorterDuffColorFilter
import android.os.Build
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import java.lang.IllegalArgumentException

internal actual typealias NativeColorFilter = android.graphics.ColorFilter

/**
 * Obtain a [android.graphics.ColorFilter] instance from this [ColorFilter]
 */
fun ColorFilter.asAndroidColorFilter(): android.graphics.ColorFilter = nativeColorFilter

/**
 * Create a [ColorFilter] from the given [android.graphics.ColorFilter] instance
 */
fun android.graphics.ColorFilter.asComposeColorFilter(): ColorFilter {
    return if (Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT &&
        this is AndroidBlendModeColorFilter) {
        BlendModeColorFilterHelper.createBlendModeColorFilter(this)
    } else if (this is AndroidLightingColorFilter && supportsLightingColorFilterQuery()) {
        LightingColorFilter(Color(this.colorMultiply), Color(this.colorAdd), this)
    } else if (this is AndroidColorMatrixColorFilter && supportsColorMatrixQuery()) {
        // Pass in null for the ColorMatrix here as the android.graphics.ColorFilter is
        // the source of truth. This allows for the ColorMatrix instance to be lazily created
        // on first query to ColorMatrixColorFilter#copyColorMatrix without having to do the
        // copies within this conversion method as an optimization.
        // This helps avoid copy overhead in this method in case this is invoked multiple times.
        ColorMatrixColorFilter(null, this)
    } else {
        // PorterDuffColorFilter is not inspectable and superseded by BlendModeColorFilter.
        // ColorMatrixColorFilter and LightingColorFilter were not inspectable until Android O
        // In each of these cases return an opaque ColorFilter implementation
        ColorFilter(this)
    }
}

internal actual fun actualTintColorFilter(color: Color, blendMode: BlendMode): NativeColorFilter {
    val androidColorFilter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        BlendModeColorFilterHelper.BlendModeColorFilter(color, blendMode)
    } else {
        AndroidPorterDuffColorFilter(color.toArgb(), blendMode.toPorterDuffMode())
    }
    return androidColorFilter
}

internal actual fun actualColorMatrixColorFilter(colorMatrix: ColorMatrix): NativeColorFilter =
    AndroidColorMatrixColorFilter(colorMatrix.values)

internal actual fun actualLightingColorFilter(multiply: Color, add: Color): NativeColorFilter =
    AndroidLightingColorFilter(multiply.toArgb(), add.toArgb())

@RequiresApi(Build.VERSION_CODES.Q)
private object BlendModeColorFilterHelper {
    @DoNotInline
    fun BlendModeColorFilter(color: Color, blendMode: BlendMode): AndroidBlendModeColorFilter {
        return AndroidBlendModeColorFilter(color.toArgb(), blendMode.toAndroidBlendMode())
    }

    @DoNotInline
    fun createBlendModeColorFilter(
        androidBlendModeColorFilter: AndroidBlendModeColorFilter
    ): BlendModeColorFilter {
        return BlendModeColorFilter(
            Color(androidBlendModeColorFilter.color),
            androidBlendModeColorFilter.mode.toComposeBlendMode(),
            androidBlendModeColorFilter
        )
    }
}

internal actual fun actualColorMatrixFromFilter(filter: NativeColorFilter): ColorMatrix {
    return if (filter is AndroidColorMatrixColorFilter && supportsColorMatrixQuery()) {
        ColorMatrixFilterHelper.getColorMatrix(filter)
    } else {
        // This method should not be invoked on API levels that do not support querying
        // the underlying ColorMatrix from the ColorMatrixColorFilter
        throw IllegalArgumentException("Unable to obtain ColorMatrix from Android " +
            "ColorMatrixColorFilter. This method was invoked on an unsupported Android version")
    }
}

/**
 * Helper method to determine when the [AndroidColorMatrixColorFilter.getColorMatrix] was
 * available in the platform
 */
internal fun supportsColorMatrixQuery() = Build.VERSION_CODES.O <= Build.VERSION.SDK_INT

/**
 * Helper method to determine when the [AndroidLightingColorFilter.getColorMultiply] and
 * [AndroidLightingColorFilter.getColorAdd] were available in the platform
 */
internal fun supportsLightingColorFilterQuery() = Build.VERSION_CODES.O <= Build.VERSION.SDK_INT

@RequiresApi(Build.VERSION_CODES.O)
private object ColorMatrixFilterHelper {

    @DoNotInline
    fun getColorMatrix(colorFilter: AndroidColorMatrixColorFilter): ColorMatrix {
        val androidColorMatrix = AndroidColorMatrix()
        colorFilter.getColorMatrix(androidColorMatrix)
        return ColorMatrix(androidColorMatrix.array)
    }
}
