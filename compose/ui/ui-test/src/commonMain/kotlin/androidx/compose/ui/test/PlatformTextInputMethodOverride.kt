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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.SessionMutex
import androidx.compose.ui.platform.LocalPlatformTextInputMethodOverride
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputModifierNode
import androidx.compose.ui.platform.PlatformTextInputSession
import androidx.compose.ui.platform.PlatformTextInputSessionHandler
import androidx.compose.ui.platform.PlatformTextInputSessionScope
import androidx.compose.ui.platform.establishTextInputSession
import androidx.compose.ui.test.PlatformTextInputMethodOverride.OverrideSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job

/**
 * Installs a custom [PlatformTextInputSession] implementation to run when
 * [PlatformTextInputSession.startInputMethod] is called by text editors inside [content].
 *
 * @param sessionHandler The [PlatformTextInputSession] to use to handle input method requests.
 * This object does _not_ need to worry about synchronizing calls to
 * [PlatformTextInputSession.startInputMethod] – this composable will handle the session management
 * the same way as in production.
 * @param content The composable content for which to override the input method handler.
 */
@OptIn(InternalComposeUiApi::class)
@ExperimentalTestApi
@Composable
fun PlatformTextInputMethodTestOverride(
    sessionHandler: PlatformTextInputSession,
    content: @Composable () -> Unit
) {
    val override = remember(sessionHandler) { PlatformTextInputMethodOverride(sessionHandler) }
    CompositionLocalProvider(
        LocalPlatformTextInputMethodOverride provides override,
        content = content
    )
}

/**
 * A [PlatformTextInputSessionHandler] that manages [OverrideSession]s with a [SessionMutex] and
 * cancels the last session's [Job] when forgotten from the composition.
 *
 * Note: This class implements [RememberObserver], and MUST NOT be exposed publicly where it could
 * be remembered externally in a composition. It should ONLY be exposed as the receiver to
 * [PlatformTextInputModifierNode.establishTextInputSession].
 */
@OptIn(InternalComposeUiApi::class)
@Stable
private class PlatformTextInputMethodOverride(
    private val sessionHandler: PlatformTextInputSession
) : PlatformTextInputSessionHandler, RememberObserver {
    private val sessionMutex = SessionMutex<PlatformTextInputSessionScope>()

    override fun onRemembered() {}
    override fun onAbandoned() {}

    override fun onForgotten() {
        sessionMutex.currentSession?.coroutineContext?.job?.cancel()
    }

    override suspend fun textInputSession(
        session: suspend PlatformTextInputSessionScope.() -> Nothing
    ): Nothing {
        sessionMutex.withSessionCancellingPrevious<Nothing>(
            sessionInitializer = { coroutineScope ->
                OverrideSession(sessionHandler, coroutineScope)
            },
            session = session
        )
    }

    private class OverrideSession(
        private val session: PlatformTextInputSession,
        coroutineScope: CoroutineScope
    ) : PlatformTextInputSessionScope,
        // For platform-specific stuff.
        PlatformTextInputSession by session,
        CoroutineScope by coroutineScope {
        private val inputMethodMutex = SessionMutex<Unit>()

        override suspend fun startInputMethod(
            request: PlatformTextInputMethodRequest
        ): Nothing {
            inputMethodMutex.withSessionCancellingPrevious<Nothing>(
                sessionInitializer = {},
                session = { session.startInputMethod(request) }
            )
        }
    }
}
