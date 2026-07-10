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

import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.wear.compose.material3.LocalTextConfiguration
import androidx.wear.compose.material3.TextConfiguration

internal fun provideScopeContent(
    contentColor: RemoteColor,
    textStyle: RemoteTextStyle,
    content: (@Composable () -> Unit),
): (@Composable () -> Unit) = {
    CompositionLocalProvider(
        LocalRemoteContentColor provides contentColor,
        LocalRemoteTextStyle provides textStyle,
        content = content,
    )
}

internal fun <T> provideScopeContent(
    contentColor: RemoteColor,
    textStyle: RemoteTextStyle,
    content: (@Composable T.() -> Unit),
): (@Composable T.() -> Unit) = {
    CompositionLocalProvider(
        LocalRemoteContentColor provides contentColor,
        LocalRemoteTextStyle provides textStyle,
    ) {
        content()
    }
}

internal fun <T> provideScopeContent(
    contentColor: RemoteColor,
    textStyle: RemoteTextStyle,
    textConfiguration: TextConfiguration,
    content: (@Composable T.() -> Unit),
): (@Composable T.() -> Unit) = {
    CompositionLocalProvider(
        LocalRemoteContentColor provides contentColor,
        LocalRemoteTextStyle provides textStyle,
        LocalTextConfiguration provides textConfiguration,
    ) {
        content()
    }
}

internal fun <T> provideNullableScopeContent(
    contentColor: RemoteColor,
    textStyle: RemoteTextStyle,
    textConfiguration: TextConfiguration,
    content: (@Composable T.() -> Unit)?,
): (@Composable T.() -> Unit)? =
    content?.let {
        {
            CompositionLocalProvider(
                LocalRemoteContentColor provides contentColor,
                LocalRemoteTextStyle provides textStyle,
                LocalTextConfiguration provides textConfiguration,
            ) {
                content()
            }
        }
    }
