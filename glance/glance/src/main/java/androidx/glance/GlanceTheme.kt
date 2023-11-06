/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.glance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.glance.color.ColorProviders

/**
 * Object that provides access to the current theme.
 */
object GlanceTheme {
    val colors: ColorProviders
        @GlanceComposable @Composable
        @ReadOnlyComposable
        get() = LocalColors.current
}

/**
 * A top level theme for Glance code. Unlike a standard compose theme, this only provides
 * color. Like a standard compose theme, it should be at the top level of a compose hierarchy with
 * all themed UI provided as [content].
 */
@Composable
fun GlanceTheme(
    colors: ColorProviders = LocalColors.current,
    content: @GlanceComposable @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalColors provides colors,
        content = content
    )
}
