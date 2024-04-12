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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.SessionMutex
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.Owner
import androidx.compose.ui.node.requireLayoutNode
import androidx.compose.ui.node.requireOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest

/**
 * A modifier node that can connect to the platform's text input IME system. To initiate a text
 * input session, call [establishTextInputSession].
 *
 * @sample androidx.compose.ui.samples.platformTextInputModifierNodeSample
 */
interface PlatformTextInputModifierNode : DelegatableNode

/** Receiver type for [establishTextInputSession]. */
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
     *   initiate the session.
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

/** Single-function interface passed to [InterceptPlatformTextInput]. */
@ExperimentalComposeUiApi
fun interface PlatformTextInputInterceptor {

    /**
     * Called when a function passed to
     * [establishTextInputSession][PlatformTextInputModifierNode.establishTextInputSession] calls
     * [startInputMethod][PlatformTextInputSession.startInputMethod]. The
     * [PlatformTextInputMethodRequest] from the caller is passed to this function as [request], and
     * this function can either respond to the request directly (e.g. recording it for a test), or
     * wrap the request and pass it to [nextHandler]. This function _must_ call into [nextHandler]
     * if it intends for the system to respond to the request. Not calling into [nextHandler] has
     * the effect of blocking the request.
     *
     * This method has the same ordering guarantees as
     * [startInputMethod][PlatformTextInputSession.startInputMethod]. That is, for a given text
     * input modifier, if [startInputMethod][PlatformTextInputSession.startInputMethod] is called
     * multiple times, only one [interceptStartInputMethod] call will be made at a time, and any
     * previous call will be allowed to finish running any `finally` blocks before the new session
     * starts.
     */
    suspend fun interceptStartInputMethod(
        request: PlatformTextInputMethodRequest,
        nextHandler: PlatformTextInputSession
    ): Nothing
}

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
 * - The session function throws an exception.
 * - The requesting coroutine is cancelled.
 * - Another session is started via this method, either from the same modifier or a different one.
 *   The session may remain open when:
 * - The system closes the connection. This behavior currently only exists on Android depending on
 *   OS version. Android platform may intermittently close the active connection to immediately
 *   start it back again. In these cases the session will not be prematurely closed, so that it can
 *   serve the follow-up requests.
 *
 * This function should only be called from the modifier node's
 * [coroutineScope][Modifier.Node.coroutineScope]. If it is not, the session will _not_
 * automatically be closed if the modifier is detached.
 *
 * @sample androidx.compose.ui.samples.platformTextInputModifierNodeSample
 * @param block A suspend function that will be called when the session is started and that must
 *   call [PlatformTextInputSession.startInputMethod] to actually show and initiate the connection
 *   with the input method.
 */
suspend fun PlatformTextInputModifierNode.establishTextInputSession(
    block: suspend PlatformTextInputSessionScope.() -> Nothing
): Nothing {
    require(node.isAttached) { "establishTextInputSession called from an unattached node" }
    val owner = requireOwner()
    val handler = requireLayoutNode().compositionLocalMap[LocalChainedPlatformTextInputInterceptor]
    owner.interceptedTextInputSession(handler, block)
}

/**
 * Intercept all calls to [PlatformTextInputSession.startInputMethod] from below where this
 * composition local is provided with the given [PlatformTextInputInterceptor].
 *
 * If a different interceptor instance is passed between compositions while a text input session is
 * active, the upstream session will be torn down and restarted with the new interceptor. The
 * downstream session (i.e. the call to [PlatformTextInputSession.startInputMethod]) will _not_ be
 * cancelled and the request will be re-used to pass to the new interceptor.
 *
 * @sample androidx.compose.ui.samples.InterceptPlatformTextInputSample
 * @sample androidx.compose.ui.samples.disableSoftKeyboardSample
 */
@ExperimentalComposeUiApi
@Composable
fun InterceptPlatformTextInput(
    interceptor: PlatformTextInputInterceptor,
    content: @Composable () -> Unit
) {
    val parent = LocalChainedPlatformTextInputInterceptor.current
    // We don't need to worry about explicitly cancelling the input session if the parent changes:
    // The only way the parent can change is if the entire subtree of the composition is moved,
    // which means the PlatformTextInputModifierNode would be detached/reattached, and the node
    // should cancel its input session when it's detached.
    val chainedInterceptor =
        remember(parent) { ChainedPlatformTextInputInterceptor(interceptor, parent) }

    // If the interceptor changes while an input session is active, the upstream session will be
    // restarted and the downstream one will not be cancelled.
    chainedInterceptor.updateInterceptor(interceptor)

    CompositionLocalProvider(
        LocalChainedPlatformTextInputInterceptor provides chainedInterceptor,
        content = content
    )
}

private val LocalChainedPlatformTextInputInterceptor =
    staticCompositionLocalOf<ChainedPlatformTextInputInterceptor?> { null }

/** Establishes a new text input session, optionally intercepted by [chainedInterceptor]. */
private suspend fun Owner.interceptedTextInputSession(
    chainedInterceptor: ChainedPlatformTextInputInterceptor?,
    session: suspend PlatformTextInputSessionScope.() -> Nothing
): Nothing {
    if (chainedInterceptor == null) {
        textInputSession(session)
    } else {
        chainedInterceptor.textInputSession(this, session)
    }
}

/**
 * A link in a chain of [PlatformTextInputInterceptor]s. Knows about its [parent] and dispatches
 * [textInputSession] calls up the chain.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Stable
private class ChainedPlatformTextInputInterceptor(
    initialInterceptor: PlatformTextInputInterceptor,
    private val parent: ChainedPlatformTextInputInterceptor?
) {
    private var interceptor by mutableStateOf(initialInterceptor)

    fun updateInterceptor(interceptor: PlatformTextInputInterceptor) {
        this.interceptor = interceptor
    }

    /**
     * Intercepts the text input session with [interceptor] and dispatches up the chain, ultimately
     * terminating at the [Owner].
     *
     * Note that the interceptor chain is assembled across the entire composition, including any
     * subcompositions, but [owner] must be the [Owner] that directly hosts the
     * [PlatformTextInputModifierNode] establishing the session.
     */
    @OptIn(InternalComposeUiApi::class)
    suspend fun textInputSession(
        owner: Owner,
        session: suspend PlatformTextInputSessionScope.() -> Nothing
    ): Nothing {
        owner.interceptedTextInputSession(parent) {
            val parentSession = this
            val inputMethodMutex = SessionMutex<Unit>()

            // Impl by delegation for platform-specific stuff.
            val scope =
                object : PlatformTextInputSessionScope by parentSession {
                    override suspend fun startInputMethod(
                        request: PlatformTextInputMethodRequest
                    ): Nothing {
                        // Explicitly synchronize between calls to our startInputMethod.
                        inputMethodMutex.withSessionCancellingPrevious<Nothing>(
                            sessionInitializer = {},
                            session = {
                                // Restart the upstream session if the interceptor is changed while
                                // the
                                // session is active.
                                snapshotFlow { interceptor }
                                    .collectLatest { interceptor ->
                                        interceptor.interceptStartInputMethod(
                                            request,
                                            parentSession
                                        )
                                    }
                                error("Interceptors flow should never terminate.")
                            }
                        )
                    }
                }
            session.invoke(scope)
        }
    }
}
