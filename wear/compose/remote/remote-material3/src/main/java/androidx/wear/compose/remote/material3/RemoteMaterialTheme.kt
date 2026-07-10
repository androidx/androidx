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

package androidx.wear.compose.remote.material3

import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Material Theming refers to the customization of your Material Design app to better reflect your
 * product’s brand.
 *
 * Remote Material components such as [RemoteButton] and [RemoteText] use values provided here when
 * retrieving default values.
 *
 * All values may be set by providing this component with the [RemoteColorScheme],
 * [RemoteTypography] attributes. Use this to configure the overall theme of elements within this
 * MaterialTheme.
 *
 * Any values that are not set will inherit the current value from the theme, falling back to the
 * defaults if there is no parent MaterialTheme. This allows using a MaterialTheme at the top of
 * your application, and then separate MaterialTheme(s) for different screens / parts of your UI,
 * overriding only the parts of the theme definition that need to change.
 *
 * @param colorScheme A complete definition of the Material Color theme for this hierarchy
 * @param shapes A set of shapes to be used as this hierarchy's shape system
 * @param typography A set of text styles to be used as this hierarchy's typography system
 * @param content The content inheriting this theme
 */
@Composable
@RemoteComposable
public fun RemoteMaterialTheme(
    colorScheme: RemoteColorScheme = RemoteMaterialTheme.colorScheme,
    typography: RemoteTypography = RemoteMaterialTheme.typography,
    shapes: RemoteShapes = RemoteMaterialTheme.shapes,
    content: @RemoteComposable @Composable () -> Unit,
) {
    val theme = RemoteMaterialTheme.Values(colorScheme, typography, shapes)

    CompositionLocalProvider(LocalRemoteMaterialTheme provides theme) {
        ProvideRemoteTextStyle(value = typography.bodyLarge, content = content)
    }
}

/**
 * Contains functions to access the current theme values provided at the call site's
 * `RemoteMaterialTheme` context.
 */
public object RemoteMaterialTheme {
    public val colorScheme: RemoteColorScheme
        @Composable @RemoteComposable get() = LocalRemoteMaterialTheme.current.colorScheme

    public val typography: RemoteTypography
        @Composable @RemoteComposable get() = LocalRemoteMaterialTheme.current.typography

    public val shapes: RemoteShapes
        @Composable @RemoteComposable get() = LocalRemoteMaterialTheme.current.shapes

    /**
     * Material 3 contains different theme subsystems to allow visual customization across a UI
     * hierarchy.
     *
     * Components use properties provided here when retrieving default values.
     *
     * @property colorScheme [RemoteColorScheme] used by material components
     * @property typography [RemoteTypography] used by material components
     * @property shapes [RemoteShapes] used by material components
     */
    @Immutable
    internal class Values(
        val colorScheme: RemoteColorScheme = RemoteColorScheme(),
        val typography: RemoteTypography = RemoteTypography(),
        val shapes: RemoteShapes = RemoteShapes(),
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Values

            if (colorScheme != other.colorScheme) return false
            if (typography != other.typography) return false
            if (shapes != other.shapes) return false

            return true
        }

        override fun hashCode(): Int {
            var result = colorScheme.hashCode()
            result = 31 * result + typography.hashCode()
            result = 31 * result + shapes.hashCode()
            return result
        }

        override fun toString(): String {
            return "Values(colorScheme=$colorScheme, typography=$typography, shapes=$shapes)"
        }
    }
}

private val LocalRemoteMaterialTheme: ProvidableCompositionLocal<RemoteMaterialTheme.Values> =
    staticCompositionLocalOf {
        RemoteMaterialTheme.Values()
    }
