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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.frontend.layout

import androidx.annotation.RestrictTo
import androidx.compose.remote.frontend.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.frontend.capture.NoRemoteCompose
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Allows to execute a normal composable within RemoteCompose, and only capturing the resulting draw
 * instructions (any layout code would execute during the draw, but wouldn't be captured).
 *
 * Essentially, it's like taking a screenshot of the passed composable, capturing what it display as
 * draw commands, but once recorded in the document the composable will not be run again, and not
 * able to react to things (eg user input, animations, etc.).
 *
 * This is useful as a tool to capture composables that are of fixed dimensions and not updated at
 * runtime.
 */
@RemoteComposable
@Composable
public fun CaptureAsDraw(content: @Composable () -> Unit) {
    Box {
        CompositionLocalProvider(LocalRemoteComposeCreationState provides NoRemoteCompose()) {
            content.invoke()
        }
    }
}
