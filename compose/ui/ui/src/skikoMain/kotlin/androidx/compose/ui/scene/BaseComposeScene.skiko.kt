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

package androidx.compose.ui.scene

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.node.SnapshotInvalidationTracker
import androidx.compose.ui.platform.GlobalSnapshotManager
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.util.trace
import kotlin.coroutines.CoroutineContext
import kotlin.concurrent.Volatile

/**
 * BaseComposeScene is an internal abstract class that implements the ComposeScene interface.
 * It provides a base implementation for managing composition, input events, and rendering.
 *
 * @property composeSceneContext the object that used to share "context" between multiple scenes
 * on the screen. Also, it provides a way for platform interaction that required within a scene.
 */
@OptIn(InternalComposeUiApi::class)
internal abstract class BaseComposeScene(
    coroutineContext: CoroutineContext,
    val composeSceneContext: ComposeSceneContext,
    private val invalidate: () -> Unit,
) : ComposeScene {
    protected val snapshotInvalidationTracker = SnapshotInvalidationTracker(::invalidateIfNeeded)
    protected val inputHandler: ComposeSceneInputHandler =
        ComposeSceneInputHandler(
            prepareForPointerInputEvent = ::doLayout,
            processPointerInputEvent = ::processPointerInputEvent,
            processKeyEvent = ::processKeyEvent
        )

    private val frameClock = BroadcastFrameClock(onNewAwaiters = ::invalidateIfNeeded)
    private val recomposer: ComposeSceneRecomposer =
        ComposeSceneRecomposer(coroutineContext, frameClock)
    private var composition: Composition? = null

    protected val compositionContext: CompositionContext
        get() = recomposer.compositionContext

    protected var isClosed = false
        private set

    private var isInvalidationDisabled = false
    private inline fun <T> postponeInvalidation(traceTag: String, crossinline block: () -> T): T = trace(traceTag) {
        check(!isClosed) { "ComposeScene is closed" }
        isInvalidationDisabled = true
        return try {
            // Try to get see the up-to-date state before running block
            // Note that this doesn't guarantee it, if sendApplyNotifications is called concurrently
            // in a different thread than this code.
            snapshotInvalidationTracker.sendAndPerformSnapshotChanges()
            snapshotInvalidationTracker.performSnapshotChangesSynchronously(block)
        } finally {
            isInvalidationDisabled = false
        }.also {
            invalidateIfNeeded()
        }
    }

    @Volatile
    private var hasPendingDraws = true
    protected fun invalidateIfNeeded() {
        hasPendingDraws = frameClock.hasAwaiters ||
            snapshotInvalidationTracker.hasInvalidations ||
            inputHandler.hasInvalidations
        if (hasPendingDraws && !isInvalidationDisabled && !isClosed && composition != null) {
            invalidate()
        }
    }

    override var compositionLocalContext: CompositionLocalContext? by mutableStateOf(null)

    /**
     * The last known position of pointer cursor position or `null` if cursor is not inside a scene.
     *
     * TODO: Move it to PlatformContext
     */
    val lastKnownPointerPosition by inputHandler::lastKnownPointerPosition

    init {
        GlobalSnapshotManager.ensureStarted()
    }

    override fun close() {
        check(!isClosed) { "ComposeScene is already closed" }
        isClosed = true

        composition?.dispose()
        recomposer.cancel()
    }

    override fun hasInvalidations(): Boolean = hasPendingDraws || recomposer.hasPendingWork

    override fun setContent(content: @Composable () -> Unit) = postponeInvalidation("BaseComposeScene:setContent") {
        check(!isClosed) { "ComposeScene is closed" }
        inputHandler.onChangeContent()

        /*
         * It's required before setting content to apply changed parameters
         * before first recomposition. Otherwise, it can lead to double recomposition.
         */
        recomposer.performScheduledTasks()

        composition?.dispose()
        composition = createComposition {
            CompositionLocalProvider(
                LocalComposeScene provides this,
                content = content
            )
        }

        recomposer.performScheduledTasks()
    }

    override fun render(canvas: Canvas, nanoTime: Long) =
        postponeInvalidation("BaseComposeScene:render") {
            // Note that on Android the order is slightly different:
            // - Recomposition
            // - Layout
            // - Draw
            // - Composition effects
            // - Synthetic events
            // We do this differently in order to be able to observe changes made by synthetic events
            // in the drawing phase, thus reducing the time before they are visible on screen.
            //
            // It is important, however, to run the composition effects before the synthetic events are
            // dispatched, in order to allow registering for these events before they are sent.
            // Otherwise, events like a synthetic mouse-enter sent due to a new element appearing under
            // the pointer would be missed by e.g. InteractionSource.collectHoverAsState
            recomposer.performScheduledTasks()
            frameClock.sendFrame(nanoTime)           // Recomposition
            doLayout()                               // Layout
            recomposer.performScheduledEffects()     // Composition effects (e.g. LaunchedEffect)
            inputHandler.updatePointerPosition()     // Synthetic move event
            snapshotInvalidationTracker.onDraw()
            draw(canvas)                             // Draw
        }

    override fun sendPointerEvent(
        eventType: PointerEventType,
        position: Offset,
        scrollDelta: Offset,
        timeMillis: Long,
        type: PointerType,
        buttons: PointerButtons?,
        keyboardModifiers: PointerKeyboardModifiers?,
        nativeEvent: Any?,
        button: PointerButton?
    ) = postponeInvalidation("BaseComposeScene:sendPointerEvent") {
        inputHandler.onPointerEvent(
            eventType = eventType,
            position = position,
            scrollDelta = scrollDelta,
            timeMillis = timeMillis,
            type = type,
            buttons = buttons,
            keyboardModifiers = keyboardModifiers,
            nativeEvent = nativeEvent,
            button = button
        )
    }

    // TODO(demin): return Boolean (when it is consumed)
    // TODO(demin) verify that pressure is the same on Android and iOS
    override fun sendPointerEvent(
        eventType: PointerEventType,
        pointers: List<ComposeScenePointer>,
        buttons: PointerButtons,
        keyboardModifiers: PointerKeyboardModifiers,
        scrollDelta: Offset,
        timeMillis: Long,
        nativeEvent: Any?,
        button: PointerButton?,
    ) = postponeInvalidation("BaseComposeScene:sendPointerEvent") {
        inputHandler.onPointerEvent(
            eventType = eventType,
            pointers = pointers,
            buttons = buttons,
            keyboardModifiers = keyboardModifiers,
            scrollDelta = scrollDelta,
            timeMillis = timeMillis,
            nativeEvent = nativeEvent,
            button = button
        )
    }

    override fun sendKeyEvent(keyEvent: KeyEvent): Boolean = postponeInvalidation("BaseComposeScene:sendKeyEvent") {
        inputHandler.onKeyEvent(keyEvent)
    }

    private fun doLayout() {
        snapshotInvalidationTracker.onLayout()
        measureAndLayout()
    }

    protected abstract fun createComposition(content: @Composable () -> Unit): Composition

    protected abstract fun processPointerInputEvent(event: PointerInputEvent)

    protected abstract fun processKeyEvent(keyEvent: KeyEvent): Boolean

    protected abstract fun measureAndLayout()

    protected abstract fun draw(canvas: Canvas)
}

internal val BaseComposeScene.semanticsOwnerListener
    get() = composeSceneContext.platformContext.semanticsOwnerListener

// TODO: Remove the cast once there is a way to obtain [PlatformContext] without the scene
internal val ComposeScene.platformContext: PlatformContext
    get() {
        this as BaseComposeScene
        return composeSceneContext.platformContext
    }

// TODO: Remove the cast once there is a way to obtain it from [PlatformContext]
internal val ComposeScene.lastKnownPointerPosition: Offset?
    get() {
        this as BaseComposeScene
        return lastKnownPointerPosition
    }
