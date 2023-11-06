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

package androidx.compose.ui.platform

import androidx.annotation.RestrictTo
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.requireLayoutNode
import androidx.compose.ui.node.requireOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin

/**
 * A modifier node that can connect to the platform's text input IME system. To initiate a text
 * input session, call [establishTextInputSession].
 *
 * @sample androidx.compose.ui.samples.platformTextInputModifierNodeSample
 */
interface PlatformTextInputModifierNode : DelegatableNode

/**
 * Receiver type for [establishTextInputSession].
 */
expect interface PlatformTextInputSession {
    /**
     * Starts the text input session and suspends until it is closed.
     *
     * On platforms that support software keyboards, calling this method will show the keyboard and
     * attempt to keep it visible until the last session is closed.
     *
     * Calling this method multiple times, within the same [establishTextInputSession] block or from
     * different [establishTextInputSession]s, will restart the session each time.
     *
     * @param request The platform-specific [PlatformTextInputMethodRequest] that will be used to
     * initiate the session.
     */
    suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing
}

/**
 * A [PlatformTextInputSession] that is also a [CoroutineScope]. This type should _only_ be used as
 * the receiver of the function passed to [establishTextInputSession]. Other extension functions
 * that need to get the scope should _not_ use this as their receiver type, instead they should be
 * suspend functions with a [PlatformTextInputSession] receiver. If they need a [CoroutineScope]
 * they should call the [kotlinx.coroutines.coroutineScope] function.
 */
interface PlatformTextInputSessionScope : PlatformTextInputSession, CoroutineScope

/**
 * Starts a new text input session and suspends until the session is closed.
 *
 * The [block] function must call [PlatformTextInputSession.startInputMethod] to actually show and
 * initiate the connection with the input method. If it does not, the session will end when this
 * function returns without showing the input method.
 *
 * If this function is called while another session is active, the sessions will not overlap. The
 * new session will interrupt the old one, which will be cancelled and allowed to finish running any
 * cancellation tasks (e.g. `finally` blocks) before running the new [block] function.
 *
 * The session will be closed when:
 *  - The session function throws an exception.
 *  - The requesting coroutine is cancelled.
 *  - Another session is started via this method, either from the same modifier or a different one.
 *  - The system closes the connection (currently only supported on Android, and there only
 *    depending on OS version).
 *
 * This function should only be called from the modifier node's
 * [coroutineScope][Modifier.Node.coroutineScope]. If it is not, the session will _not_
 * automatically be closed if the modifier is detached.
 *
 * @sample androidx.compose.ui.samples.platformTextInputModifierNodeSample
 *
 * @param block A suspend function that will be called when the session is started and that must
 * call [PlatformTextInputSession.startInputMethod] to actually show and initiate the connection
 * with the input method.
 */
@OptIn(InternalComposeUiApi::class)
suspend fun PlatformTextInputModifierNode.establishTextInputSession(
    block: suspend PlatformTextInputSessionScope.() -> Nothing
): Nothing {
    require(node.isAttached) { "establishTextInputSession called from an unattached node" }
    val override = requireLayoutNode().compositionLocalMap[LocalPlatformTextInputMethodOverride]
    val handler = override ?: requireOwner()
    handler.textInputSession(block)
}

/**
 * Composition local used to override the [PlatformTextInputSessionHandler] used by
 * [establishTextInputSession] for tests. Should only be set by
 * `PlatformTextInputMethodTestOverride` in the ui-test module, and only ready by
 * [establishTextInputSession].
 */
@Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@InternalComposeUiApi
@get:InternalComposeUiApi
val LocalPlatformTextInputMethodOverride =
    staticCompositionLocalOf<PlatformTextInputSessionHandler?> { null }

/**
 * SAM interface used by [textInputSession] to start a text input session.
 */
@InternalComposeUiApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface PlatformTextInputSessionHandler {
    /**
     * Starts a new text input session and suspends until it's closed. For more information see
     * [PlatformTextInputModifierNode.establishTextInputSession].
     *
     * Implementations must ensure that new requests cancel any active request. They must also
     * ensure that the previous request is finished running all cancellation tasks before starting
     * the new session, to ensure that no session code overlaps (e.g. using [Job.cancelAndJoin]).
     */
    suspend fun textInputSession(
        session: suspend PlatformTextInputSessionScope.() -> Nothing
    ): Nothing
}
