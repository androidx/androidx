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

package androidx.glance.unit

import android.content.Context
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo

/** Provider of colors for a glance composable's attributes. */
public interface ColorProvider

/** Returns a [ColorProvider] that always resolves to the [Color]. */
public fun ColorProvider(color: Color): ColorProvider {
    return FixedColorProvider(color)
}

/** Returns a [ColorProvider] that resolves to the color resource. */
public fun ColorProvider(@ColorRes resId: Int): ColorProvider {
    return ResourceColorProvider(resId)
}

/** @suppress */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FixedColorProvider(val color: Color) : ColorProvider

/** @suppress */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ResourceColorProvider(@ColorRes val resId: Int) : ColorProvider

/** @suppress */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("INLINE_CLASS_DEPRECATED")
public fun ResourceColorProvider.resolve(context: Context): Color {
    val androidColor = if (Build.VERSION.SDK_INT >= 23) {
        ColorProviderApi23Impl.getColor(context.applicationContext, resId)
    } else {
        @Suppress("DEPRECATION") // Resources.getColor must be used on < 23.
        context.applicationContext.resources.getColor(resId)
    }
    return Color(
        red = android.graphics.Color.red(androidColor) / 255f,
        green = android.graphics.Color.green(androidColor) / 255f,
        blue = android.graphics.Color.blue(androidColor) / 255f,
        alpha = android.graphics.Color.alpha(androidColor) / 255f
    )
}

@RequiresApi(23)
private object ColorProviderApi23Impl {
    @ColorInt
    @DoNotInline
    fun getColor(context: Context, @ColorRes resId: Int): Int {
        return context.getColor(resId)
    }
}
