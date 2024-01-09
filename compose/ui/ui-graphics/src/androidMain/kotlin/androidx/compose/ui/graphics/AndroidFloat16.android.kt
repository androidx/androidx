/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.os.Build
import android.util.Half
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi

// Use the platform version to benefit from ART intrinsics on API 30+
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun floatToHalf(f: Float): Short = if (Build.VERSION.SDK_INT >= 26) {
    Api26Impl.floatToHalf(f)
} else {
    softwareFloatToHalf(f)
}

// Use the platform version to benefit from ART intrinsics on API 30+
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun halfToFloat(h: Short): Float = if (Build.VERSION.SDK_INT >= 26) {
    Api26Impl.halfToFloat(h)
} else {
    softwareHalfToFloat(h)
}

@RequiresApi(26)
internal object Api26Impl {
    @JvmStatic
    @DoNotInline
    @Suppress("HalfFloat")
    fun floatToHalf(f: Float) = Half.toHalf(f)

    @JvmStatic
    @DoNotInline
    @Suppress("HalfFloat")
    fun halfToFloat(h: Short) = Half.toFloat(h)
}
