/*
 * Copyright 2025 The Android Open Source Project
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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.wear.material3

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rememberRemoteColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.ProvideTextStyle
import androidx.wear.compose.material3.Shapes
import androidx.wear.compose.material3.Typography

@Composable
@RemoteComposable
public fun RemoteMaterialTheme(
    colorScheme: RemoteColorScheme = RemoteMaterialTheme.colorScheme,
    typography: RemoteTypography = RemoteMaterialTheme.typography,
    shapes: RemoteShapes = RemoteMaterialTheme.shapes,
    content: @RemoteComposable @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalRemoteColorScheme provides colorScheme,
        LocalRemoteShapes provides shapes,
        LocalRemoteTypography provides typography,
    ) {
        ProvideTextStyle(value = typography.typography.bodyLarge, content = content)
    }
}

/**
 * Contains functions to access the current theme values provided at the call site's
 * `RemoteMaterialTheme` context.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteMaterialTheme {
    public val colorScheme: RemoteColorScheme
        @Composable @RemoteComposable get() = LocalRemoteColorScheme.current

    public val typography: RemoteTypography
        @Composable @RemoteComposable get() = LocalRemoteTypography.current

    public val shapes: RemoteShapes
        @Composable @RemoteComposable get() = LocalRemoteShapes.current
}

/**
 * Represents the color scheme for the Wear Material 3 theme in a remote context. This object
 * provides functions to access the various colors defined in the Material 3 color system, such as
 * primary, secondary, surface, background, and error colors. Each function returns a `RemoteColor`
 * which can be used to apply these colors to remote UI elements.
 *
 * The colors are retrieved from the local `ColorScheme` and wrapped in `rememberRemoteColor` to
 * ensure they are correctly managed and updated in the remote UI.
 */
@Suppress("RestrictedApiAndroidX")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class RemoteColorScheme() {
    private val colorScheme: ColorScheme = ColorScheme()

    private companion object {
        private const val PRIMARY = "WearM3.primary"
        private const val PRIMARY_DIM = "WearM3.primaryDim"
        private const val PRIMARY_CONTAINER = "WearM3.primaryContainer"
        private const val ON_PRIMARY = "WearM3.onPrimary"
        private const val ON_PRIMARY_CONTAINER = "WearM3.onPrimaryContainer"
        private const val SECONDARY = "WearM3.secondary"
        private const val SECONDARY_DIM = "WearM3.secondaryDim"
        private const val SECONDARY_CONTAINER = "WearM3.secondaryContainer"
        private const val ON_SECONDARY = "WearM3.onSecondary"
        private const val ON_SECONDARY_CONTAINER = "WearM3.onSecondaryContainer"
        private const val TERTIARY = "WearM3.tertiary"
        private const val TERTIARY_DIM = "WearM3.tertiaryDim"
        private const val TERTIARY_CONTAINER = "WearM3.tertiaryContainer"
        private const val ON_TERTIARY = "WearM3.onTertiary"
        private const val ON_TERTIARY_CONTAINER = "WearM3.onTertiaryContainer"
        private const val SURFACE_CONTAINER_LOW = "WearM3.surfaceContainerLow"
        private const val SURFACE_CONTAINER = "WearM3.surfaceContainer"
        private const val SURFACE_CONTAINER_HIGH = "WearM3.surfaceContainerHigh"
        private const val ON_SURFACE = "WearM3.onSurface"
        private const val ON_SURFACE_VARIANT = "WearM3.onSurfaceVariant"
        private const val OUTLINE = "WearM3.outline"
        private const val OUTLINE_VARIANT = "WearM3.outlineVariant"
        private const val BACKGROUND = "WearM3.background"
        private const val ON_BACKGROUND = "WearM3.onBackground"
        private const val ERROR = "WearM3.error"
        private const val ERROR_DIM = "WearM3.errorDim"
        private const val ERROR_CONTAINER = "WearM3.errorContainer"
        private const val ON_ERROR = "WearM3.onError"
        private const val ON_ERROR_CONTAINER = "WearM3.onErrorContainer"
    }

    public open val primary: RemoteColor
        @RemoteComposable @Composable get() = rememberRemoteColor(PRIMARY) { colorScheme.primary }

    public open val primaryDim: RemoteColor
        @RemoteComposable
        @Composable
        get() = rememberRemoteColor(PRIMARY_DIM) { colorScheme.primaryDim }

    public open val primaryContainer: RemoteColor
        @RemoteComposable
        @Composable
        get() = rememberRemoteColor(PRIMARY_CONTAINER) { colorScheme.primaryContainer }

    public open val onPrimary: RemoteColor
        @RemoteComposable
        @Composable
        get() = rememberRemoteColor(ON_PRIMARY) { colorScheme.onPrimary }

    public open val onPrimaryContainer: RemoteColor
        @RemoteComposable
        @Composable
        get() = rememberRemoteColor(ON_PRIMARY_CONTAINER) { colorScheme.onPrimaryContainer }

    public open val secondary: RemoteColor
        @RemoteComposable
        @Composable
        get() = rememberRemoteColor(SECONDARY) { colorScheme.secondary }

    public open val secondaryDim: RemoteColor
        @RemoteComposable
        @Composable
        get() = rememberRemoteColor(SECONDARY_DIM) { colorScheme.secondaryDim }

    public open val secondaryContainer: RemoteColor
        @RemoteComposable
        @Composable
        get() = rememberRemoteColor(SECONDARY_CONTAINER) { colorScheme.secondaryContainer }

    public open val onSecondary: RemoteColor
        @RemoteComposable
        @Composable
        get() = rememberRemoteColor(ON_SECONDARY) { colorScheme.onSecondary }

    public open val onSecondaryContainer: RemoteColor
        @RemoteComposable
        @Composable
        get() = rememberRemoteColor(ON_SECONDARY_CONTAINER) { colorScheme.onSecondaryContainer }

    public open val tertiary: RemoteColor
        @RemoteComposable @Composable get() = rememberRemoteColor(TERTIARY) { colorScheme.tertiary }

    public open val tertiaryDim: RemoteColor
        @RemoteComposable
        @Composable
        get() = rememberRemoteColor(TERTIARY_DIM) { colorScheme.tertiaryDim }

    public open val tertiaryContainer: RemoteColor
        @RemoteComposable
        @Composable
        get() = rememberRemoteColor(TERTIARY_CONTAINER) { colorScheme.tertiaryContainer }

    public open val onTertiary: RemoteColor
        @RemoteComposable
        @Composable
        get() = rememberRemoteColor(ON_TERTIARY) { colorScheme.onTertiary }

    public open val onTertiaryContainer: RemoteColor
        @RemoteComposable
        @Composable
        get() = rememberRemoteColor(ON_TERTIARY_CONTAINER) { colorScheme.onTertiaryContainer }

    public open val surfaceContainerLow: RemoteColor
        @RemoteComposable
        @Composable
        get() = rememberRemoteColor(SURFACE_CONTAINER_LOW) { colorScheme.surfaceContainerLow }

    public open val surfaceContainer: RemoteColor
        @RemoteComposable
        @Composable
        get() = rememberRemoteColor(SURFACE_CONTAINER) { colorScheme.surfaceContainer }

    public open val surfaceContainerHigh: RemoteColor
        @RemoteComposable
        @Composable
        get() = rememberRemoteColor(SURFACE_CONTAINER_HIGH) { colorScheme.surfaceContainerHigh }

    public open val onSurface: RemoteColor
        @RemoteComposable
        @Composable
        get() = rememberRemoteColor(ON_SURFACE) { colorScheme.onSurface }

    public open val onSurfaceVariant: RemoteColor
        @RemoteComposable
        @Composable
        get() = rememberRemoteColor(ON_SURFACE_VARIANT) { colorScheme.onSurfaceVariant }

    public open val outline: RemoteColor
        @RemoteComposable @Composable get() = rememberRemoteColor(OUTLINE) { colorScheme.outline }

    public open val outlineVariant: RemoteColor
        @RemoteComposable
        @Composable
        get() = rememberRemoteColor(OUTLINE_VARIANT) { colorScheme.outlineVariant }

    public open val background: RemoteColor
        @RemoteComposable
        @Composable
        get() = rememberRemoteColor(BACKGROUND) { colorScheme.background }

    public open val onBackground: RemoteColor
        @RemoteComposable
        @Composable
        get() = rememberRemoteColor(ON_BACKGROUND) { colorScheme.onBackground }

    public open val error: RemoteColor
        @RemoteComposable @Composable get() = rememberRemoteColor(ERROR) { colorScheme.error }

    public open val errorDim: RemoteColor
        @RemoteComposable
        @Composable
        get() = rememberRemoteColor(ERROR_DIM) { colorScheme.errorDim }

    public open val errorContainer: RemoteColor
        @RemoteComposable
        @Composable
        get() = rememberRemoteColor(ERROR_CONTAINER) { colorScheme.errorContainer }

    public open val onError: RemoteColor
        @RemoteComposable @Composable get() = rememberRemoteColor(ON_ERROR) { colorScheme.onError }

    public open val onErrorContainer: RemoteColor
        @RemoteComposable
        @Composable
        get() = rememberRemoteColor(ON_ERROR_CONTAINER) { colorScheme.onErrorContainer }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteTypography {
    internal val typography: Typography = Typography()
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteShapes {
    internal val shapes: Shapes = Shapes()
}

internal val LocalRemoteColorScheme = staticCompositionLocalOf { RemoteColorScheme() }

internal val LocalRemoteShapes = staticCompositionLocalOf { RemoteShapes }

internal val LocalRemoteTypography = staticCompositionLocalOf { RemoteTypography }
