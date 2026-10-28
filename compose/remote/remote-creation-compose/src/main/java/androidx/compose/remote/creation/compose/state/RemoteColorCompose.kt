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

import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Creates a [RemoteColor] from a literal [Color] value.
 *
 * @param color The [Color] value.
 * @return A [RemoteColor] representing the constant color.
 */
public fun RemoteColor(color: Color): RemoteColor = color.asRemoteColor()

/**
 * Creates a named [RemoteColor] with an initial value.
 *
 * Named remote colors can be set via AndroidRemoteContext.setNamedColor.
 *
 * @param name A unique name to identify this state within its [domain].
 * @param defaultValue The initial [Color] value for the named remote color.
 * @param domain The domain for the named state. Defaults to [RemoteState.Domain.User].
 * @return A [RemoteColor] representing the named color.
 */
public fun RemoteColor.Companion.createNamedRemoteColor(
    name: String,
    defaultValue: Color,
    domain: RemoteState.Domain = RemoteState.Domain.User,
): RemoteColor = createNamedRemoteColor(name, defaultValue.toArgb(), domain)

/**
 * Creates a named [RemoteColor] with an initial value.
 *
 * @param name A unique name to identify this state within its [domain].
 * @param defaultValue The initial [Color] value for the named remote color.
 * @param domain The domain for the named state. Defaults to [RemoteState.Domain.User].
 * @return A [RemoteColor] representing the named color.
 */
public fun createNamedRemoteColor(
    name: String,
    defaultValue: Color,
    domain: RemoteState.Domain = RemoteState.Domain.User,
): RemoteColor = RemoteColor.createNamedRemoteColor(name, defaultValue, domain)

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
 */
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

/**
 * Remembers a named remote color expression.
 *
 * @param name The unique name for this remote color.
 * @param initialValue The initial ARGB integer value.
 * @param domain The domain of the named color (defaults to [RemoteState.Domain.User]).
 * @return A [RemoteColor] representing the named remote color expression.
 */
@Composable
@RemoteComposable
public fun rememberNamedRemoteColor(
    name: String,
    @ColorInt initialValue: Int,
    domain: RemoteState.Domain = RemoteState.Domain.User,
): RemoteColor {
    return remember(name, domain) {
        RemoteColor.createNamedRemoteColor(name, initialValue, domain)
    }
}

/**
 * Remembers a named remote color expression.
 *
 * @param name The unique name for this remote color.
 * @param initialValue The initial [Color] value.
 * @param domain The domain of the named color (defaults to [RemoteState.Domain.User]).
 * @return A [RemoteColor] representing the named remote color expression.
 */
@Composable
@RemoteComposable
public fun rememberNamedRemoteColor(
    name: String,
    initialValue: Color,
    domain: RemoteState.Domain = RemoteState.Domain.User,
): RemoteColor = rememberNamedRemoteColor(name, initialValue.toArgb(), domain)

/** Extension property to convert a [Color] to a [RemoteColor]. */
public val Color.rc: RemoteColor
    get() = asRemoteColor()

/** Extension function to convert a [Color] to a [RemoteColor]. */
public fun Color.asRemoteColor(): RemoteColor = RemoteColor(this.toArgb())

/** Converts this [RemoteColor] to a [Color] if it has a constant value. */
public fun RemoteColor.asConstantColor(): Color = Color(constantValue)

/** Converts this [RemoteColor] to a [Color] if it has a constant value, or null otherwise. */
public fun RemoteColor.asConstantColorOrNull(): Color? = constantValueOrNull?.let { Color(it) }
