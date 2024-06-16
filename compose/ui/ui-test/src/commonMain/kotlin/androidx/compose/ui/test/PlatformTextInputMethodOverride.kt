/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.test

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputSession

/**
 * Installs a custom [PlatformTextInputSession] implementation to run when
 * [PlatformTextInputSession.startInputMethod] is called by text editors inside [content].
 *
 * @param sessionHandler The [PlatformTextInputSession] to use to handle input method requests. This
 *   object does _not_ need to worry about synchronizing calls to
 *   [PlatformTextInputSession.startInputMethod] – this composable will handle the session
 *   management the same way as in production.
 * @param content The composable content for which to override the input method handler.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Deprecated("Use InterceptPlatformTextInput instead")
@ExperimentalTestApi
@Composable
fun PlatformTextInputMethodTestOverride(
    sessionHandler: PlatformTextInputSession,
    content: @Composable () -> Unit
) {
    InterceptPlatformTextInput(
        interceptor = { request, _ ->
            // Don't forward the request, block it, so that tests don't have to deal with the
            // actual IME sending commands.
            sessionHandler.startInputMethod(request)
        },
        content = content
    )
}
