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

package androidx.wear.compose.remote.material3

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

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
        ProvideRemoteTextStyle(value = typography.bodyLarge, content = content)
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

internal val LocalRemoteColorScheme = staticCompositionLocalOf { RemoteColorScheme() }

internal val LocalRemoteTypography = staticCompositionLocalOf { RemoteTypography() }

internal val LocalRemoteTextStyle = staticCompositionLocalOf { RemoteTextStyle() }
