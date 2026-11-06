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

package androidx.xr.glimmer.remote

import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.xr.glimmer.remote.RemoteGlimmerTheme.Companion.colors
import androidx.xr.glimmer.remote.RemoteGlimmerTheme.Companion.iconSizes
import androidx.xr.glimmer.remote.RemoteGlimmerTheme.Companion.shapes

/**
 * Remote Compose Glimmer contains different theme subsystems to allow visual customization across a
 * Remote Compose document.
 *
 * Components use properties provided here when retrieving default values.
 *
 * Any values that are not set will inherit the current value from the theme, falling back to the
 * defaults if there is no parent RemoteGlimmerTheme. This allows using a RemoteGlimmerTheme at the
 * top of your document, and then separate RemoteGlimmerTheme(s) for different parts of your UI,
 * overriding only the parts of the theme definition that need to change.
 *
 * @param typography [RemoteTypography] used by components within this hierarchy
 * @param componentSpacingValues [RemoteComponentSpacingValues] used by components within this
 *   hierarchy
 * @param content The content that can retrieve values from this theme
 */
@RemoteComposable
@Composable
public fun RemoteGlimmerTheme(
    typography: RemoteTypography = RemoteGlimmerTheme.typography,
    componentSpacingValues: RemoteComponentSpacingValues =
        RemoteGlimmerTheme.componentSpacingValues,
    content: @RemoteComposable @Composable () -> Unit,
) {
    val theme = RemoteGlimmerTheme(typography, componentSpacingValues)
    CompositionLocalProvider(
        _localRemoteGlimmerTheme provides theme,
        LocalRemoteIconSize provides theme.iconSizes.medium,
        content = content,
    )
}

/**
 * Remote Compose Glimmer contains different theme subsystems to allow visual customization across a
 * Remote Compose document.
 *
 * Components use properties provided here when retrieving default values.
 *
 * @property typography [RemoteTypography] used by Remote Compose Glimmer components
 * @property componentSpacingValues [RemoteComponentSpacingValues] used by Remote Compose Glimmer
 *   components
 * @property colors [RemoteColors] used by Remote Compose Glimmer components
 * @property shapes [RemoteShapes] used by Remote Compose Glimmer components
 * @property iconSizes [RemoteIconSizes] used by icons
 */
@Immutable
public class RemoteGlimmerTheme(
    public val typography: RemoteTypography = RemoteTypography(),
    public val componentSpacingValues: RemoteComponentSpacingValues =
        RemoteComponentSpacingValues(),
) {
    // TODO: b/568385462 - Provide colors customization.
    public val colors: RemoteColors = _colors
    public val shapes: RemoteShapes = _shapes
    public val iconSizes: RemoteIconSizes = _iconSizes

    public companion object {
        /** Retrieves the current [RemoteColors] at the call site's position in the hierarchy. */
        public val colors: RemoteColors
            @Composable @ReadOnlyComposable get() = LocalRemoteGlimmerTheme.current.colors

        /**
         * Retrieves the current [RemoteTypography] at the call site's position in the hierarchy.
         */
        public val typography: RemoteTypography
            @Composable @ReadOnlyComposable get() = LocalRemoteGlimmerTheme.current.typography

        /**
         * Retrieves the current [RemoteComponentSpacingValues] at the call site's position in the
         * hierarchy.
         */
        public val componentSpacingValues: RemoteComponentSpacingValues
            @Composable
            @ReadOnlyComposable
            get() = LocalRemoteGlimmerTheme.current.componentSpacingValues

        /** Retrieves the current [RemoteShapes] at the call site's position in the hierarchy. */
        public val shapes: RemoteShapes
            @Composable @ReadOnlyComposable get() = LocalRemoteGlimmerTheme.current.shapes

        /** Retrieves the current [RemoteIconSizes] at the call site's position in the hierarchy. */
        public val iconSizes: RemoteIconSizes
            @Composable @ReadOnlyComposable get() = LocalRemoteGlimmerTheme.current.iconSizes

        /**
         * [CompositionLocal] providing [RemoteGlimmerTheme] throughout the hierarchy. You can use
         * properties in the companion object to access specific subsystems, for example [colors].
         * To provide a new value for this, use [RemoteGlimmerTheme]. In most cases you should use
         * [colors] and other properties directly.
         */
        public val LocalRemoteGlimmerTheme: CompositionLocal<RemoteGlimmerTheme>
            get() = _localRemoteGlimmerTheme

        /**
         * Cached [RemoteColors] instance to be used across [RemoteGlimmerTheme] instances - colors
         * are not user-configurable.
         */
        private val _colors = RemoteColors()

        /**
         * Cached [RemoteShapes] instance to be used across [RemoteGlimmerTheme] instances -
         * currently shapes are not user-configurable.
         */
        private val _shapes = RemoteShapes()

        /**
         * Cached [RemoteIconSizes] instance to be used across [RemoteGlimmerTheme] instances -
         * currently icon sizes are not user-configurable.
         */
        internal val _iconSizes = RemoteIconSizes()
    }
}

/** Use [RemoteGlimmerTheme.LocalRemoteGlimmerTheme] to access this publicly. */
@Suppress("CompositionLocalNaming")
private val _localRemoteGlimmerTheme: ProvidableCompositionLocal<RemoteGlimmerTheme> =
    staticCompositionLocalOf {
        RemoteGlimmerTheme()
    }
