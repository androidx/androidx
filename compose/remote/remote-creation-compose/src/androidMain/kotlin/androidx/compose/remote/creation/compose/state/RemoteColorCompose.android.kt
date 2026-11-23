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

@file:JvmName("RemoteColorComposeKt")

package androidx.compose.remote.creation.compose.state

import androidx.annotation.ColorRes
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Creates a [RemoteColor] from a literal [Color] value.
 *
 * @param color The [Color] value.
 * @return A [RemoteColor] representing the constant color.
 */
@Stable public fun RemoteColor(color: Color): RemoteColor = RemoteColor(color.toArgb())

/**
 * Creates a themed [RemoteColor] that resolves to different Android system colors in light and dark
 * mode.
 *
 * @param lightColorRes The [android.R.color] resource ID for light mode.
 * @param darkColorRes The [android.R.color] resource ID for dark mode.
 * @param lightFallback The fallback [Color] value to use in light mode if the theme color cannot be
 *   resolved.
 * @param darkFallback The fallback [Color] value to use in dark mode if the theme color cannot be
 *   resolved.
 * @return A [RemoteColor] representing the themed color.
 * @throws IllegalArgumentException if [lightColorRes] or [darkColorRes] is not a supported
 *   [android.R.color] system color resource.
 */
@Stable
public fun RemoteColor.Companion.createThemedRemoteColor(
    @ColorRes lightColorRes: Int,
    @ColorRes darkColorRes: Int,
    lightFallback: Color,
    darkFallback: Color,
): RemoteColor =
    createThemedRemoteColor(
        lightColorRes = lightColorRes,
        darkColorRes = darkColorRes,
        lightFallback = lightFallback.toArgb(),
        darkFallback = darkFallback.toArgb(),
    )

/** Extension property to convert a [Color] to a [RemoteColor]. */
@Stable
public val Color.rc: RemoteColor
    get() = RemoteColor(this.toArgb())

/**
 * Converts this [RemoteColor] to a [Color] if it has a constant value.
 *
 * @return the constant [Color] value of this [RemoteColor]
 * @throws IllegalStateException if this state does not have a constant value.
 */
@Stable public fun RemoteColor.toConstantColor(): Color = Color(constantValue)

/**
 * Converts this [RemoteColor] to a [Color] if it has a constant value, or null otherwise.
 *
 * @return the constant [Color] value of this [RemoteColor], or `null` if it is dynamic
 */
@Stable
public fun RemoteColor.toConstantColorOrNull(): Color? = constantValueOrNull?.let { Color(it) }
