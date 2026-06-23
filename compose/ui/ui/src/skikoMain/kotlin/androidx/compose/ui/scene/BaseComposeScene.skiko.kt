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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
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
import androidx.compose.ui.input.rotary.RotaryScrollEvent
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.platform.ProvidePlatformCompositionLocals
import androidx.compose.ui.util.trace
import kotlin.concurrent.Volatile

/**
 * BaseComposeScene is an internal abstract class that implements the ComposeScene interface.
 * It provides a base implementation for managing composition, input events, and rendering.
 *
 * @property composeSceneContext the object that used to share "context" between multiple scenes
 * on the screen. Also, it provides a way for platform interaction that is required within a scene.
 */
@OptIn(InternalComposeUiApi::class)
internal abstract class BaseComposeScene(
    protected val frameRecomposer: FrameRecomposer,
    private val invalidateLayout: () -> Unit,
    private val invalidateDraw: () -> Unit,
) : ComposeScene {
    protected val inputHandler: ComposeSceneInputHandler =
        ComposeSceneInputHandler(
            prepareForPointerInputEvent = ::doMeasureAndLayout,
            processPointerInputEvent = ::onPointerInputEvent,
            cancelPointerInput = ::processCancelPointerInput,
            processKeyEvent = ::processKeyEvent,
        )

    private var composition: Composition? = null

    abstract val composeSceneContext: ComposeSceneContext

    protected var isClosed = false
        private set

    private var isInvalidationDisabled = false
    private inline fun <T> postponeInvalidation(traceTag: String, crossinline block: () -> T): T =
        trace(traceTag) {
            check(!isClosed) { "postponeInvalidation called after ComposeScene is closed" }
            if (isInvalidationDisabled) return block()
            isInvalidationDisabled = true
            return try {
                block()
            } finally {
                isInvalidationDisabled = false
            }.also {
                invokeInvalidationCallbacks()
            }
        }

    @Volatile
    protected var hasForcedLayout: Boolean = false
        private set

    @Volatile
    protected var hasForcedDraw: Boolean = false
        private set

    protected fun invokeInvalidationCallbacks(
        forceLayout: Boolean = false,
        forceDraw: Boolean = false,
    ) {
        hasForcedLayout = hasForcedLayout || forceLayout
        hasForcedDraw = hasForcedDraw || forceDraw
        if (isInvalidationDisabled || isClosed || composition == null) return
        if (hasForcedLayout || hasPendingMeasureOrLayout) {
            invalidateLayout()
        }
        if (hasForcedDraw || hasPendingDraw) {
            invalidateDraw()
        }
    }

    override var compositionLocalContext: CompositionLocalContext? by mutableStateOf(null)

    /**
     * The last known position of pointer cursor position or `null` if cursor is not inside a scene.
     *
     * TODO: Move it to PlatformContext
     */
    val lastKnownPointerPosition by inputHandler::lastKnownPointerPosition

    override fun close() {
        check(!isClosed) { "ComposeScene is already closed" }
        isClosed = true

        composition?.dispose()
    }

    override fun setContent(
        parentCompositionContext: CompositionContext?,
        content: @Composable () -> Unit,
    ) = postponeInvalidation("BaseComposeScene:setContent") {
            check(!isClosed) { "setContent called after ComposeScene is closed" }
            inputHandler.onChangeContent()

            /*
             * This is usually a no-op for the first composition, but it must drain any stale
             * host work from the previous content before replacing the composition. Otherwise,
             * changed parameters can be applied in a separate turn and trigger double
             * recomposition when new content is installed.
             */
            frameRecomposer.performFrameDispatch()
            composition?.dispose()
            composition = createComposition(
                parentCompositionContext = parentCompositionContext ?: frameRecomposer.compositionContext,
            ) {
                ProvidePlatformCompositionLocals(
                    @Suppress("DEPRECATION")
                    LocalComposeScene provides this,
                    LocalComposeSceneContext provides composeSceneContext,
                    platformContext = composeSceneContext.platformContext,
                    content = content
                )
            }
            frameRecomposer.performFrameDispatch()
        }

    override fun measureAndLayout() {
        if (isClosed) return
        hasForcedLayout = false

        postponeInvalidation("BaseComposeScene:measureAndLayout") {
            doMeasureAndLayout()

            // Schedule synthetic events to be sent after measure/layout completes.
            if (inputHandler.needUpdatePointerPosition) {
                frameRecomposer.dispatch {
                    inputHandler.updatePointerPosition()
                }
            }
        }
    }

    override fun draw(canvas: Canvas) {
        if (isClosed) return
        hasForcedDraw = false

        postponeInvalidation("BaseComposeScene:draw") {
            // FIXME: Remove applying the global snapshot here.
            //  Android never applies the snapshot *between* the layout and draw phases
            //  (applies happen once per frame on the main looper, not between phases).
            //  This between-phase apply is a temporary workaround kept only to preserve current
            //  behavior for OffsetToFocusedRect (iOS FocusableAboveKeyboard).
            Snapshot.sendApplyNotifications()

            // AndroidComposeView.dispatchDraw() begins with measureAndLayout() so layout changes
            // discovered after the host layout traversal are still settled before drawing. Keep
            // that trailing layout pass here even though measureAndLayout() is also a public phase.
            doMeasureAndLayout()

            // Advance the global snapshot before drawing so writes made since the last pass
            // including state objects created during a prior draw are recorded as modified and
            // visible to this draw. Lighter than sendApplyNotifications, matches what Android does.
            Snapshot.notifyObjectsInitialized()

            doDraw(canvas)
        }
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
        button: PointerButton?,
        scaleGestureFactor: Float,
        panGestureOffset: Offset
    ): PointerEventResult = postponeInvalidation(
        "BaseComposeScene:sendPointerEvent"
    ) {
        inputHandler.onPointerEvent(
            eventType = eventType,
            position = position,
            scrollDelta = scrollDelta,
            timeMillis = timeMillis,
            type = type,
            buttons = buttons,
            keyboardModifiers = keyboardModifiers,
            nativeEvent = nativeEvent,
            button = button,
            scaleGestureFactor = scaleGestureFactor,
            panGestureOffset = panGestureOffset,
        ).also {
            frameRecomposer.performTrampolineDispatch()
        }
    }

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
        scaleGestureFactor: Float,
        panGestureOffset: Offset,
    ): PointerEventResult = postponeInvalidation(
        "BaseComposeScene:sendPointerEvent"
    ) {
        inputHandler.onPointerEvent(
            eventType = eventType,
            pointers = pointers,
            buttons = buttons,
            keyboardModifiers = keyboardModifiers,
            scrollDelta = scrollDelta,
            timeMillis = timeMillis,
            nativeEvent = nativeEvent,
            button = button,
            scaleGestureFactor = scaleGestureFactor,
            panGestureOffset = panGestureOffset,
        ).also {
            frameRecomposer.performTrampolineDispatch()
        }
    }

    override fun cancelPointerInput() {
        inputHandler.cancelPointerInput()
    }

    override fun sendKeyEvent(keyEvent: KeyEvent): Boolean =
        postponeInvalidation("BaseComposeScene:sendKeyEvent") {
            inputHandler.onKeyEvent(keyEvent).also {
                frameRecomposer.performTrampolineDispatch()
            }
        }

    override fun sendRotaryScrollEvent(
        verticalScrollPixels: Float,
        horizontalScrollPixels: Float,
        timeMillis: Long
    ): Boolean = postponeInvalidation("BaseComposeScene:sendRotaryScrollEvent") {
        val event = RotaryScrollEvent(
            verticalScrollPixels = verticalScrollPixels,
            horizontalScrollPixels = horizontalScrollPixels,
            uptimeMillis = timeMillis
        )
        processRotaryScrollEvent(event).also {
            frameRecomposer.performTrampolineDispatch()
        }
    }

    protected abstract fun createComposition(
        parentCompositionContext: CompositionContext,
        content: @Composable () -> Unit
    ): Composition

    private fun onPointerInputEvent(event: PointerInputEvent) = processPointerInputEvent(event)
        .also {
            if (composeSceneContext.platformContext.isClearFocusOnMouseDownEnabled) {
                val isDown = event.eventType == PointerEventType.Press
                val pointer = event.pointers.singleOrNull()
                val isFromMouse = pointer?.type == PointerType.Mouse
                if (isDown && isFromMouse) {
                    focusManager.clearFocusIfOutsideOfActiveFocusTargetNode(pointer.position)
                }
            }
        }

    protected abstract fun processPointerInputEvent(event: PointerInputEvent): PointerEventResult

    protected abstract fun processCancelPointerInput()

    protected abstract fun processKeyEvent(keyEvent: KeyEvent): Boolean

    protected abstract fun processRotaryScrollEvent(event: RotaryScrollEvent): Boolean

    protected abstract fun doMeasureAndLayout()

    protected abstract fun doDraw(canvas: Canvas)
}

internal val BaseComposeScene.semanticsOwnerListener
    get() = composeSceneContext.platformContext.semanticsOwnerListener

// TODO: Remove the cast once there is a way to obtain it from [PlatformContext]
internal val ComposeScene.lastKnownPointerPosition: Offset?
    get() {
        this as BaseComposeScene
        return lastKnownPointerPosition
    }
