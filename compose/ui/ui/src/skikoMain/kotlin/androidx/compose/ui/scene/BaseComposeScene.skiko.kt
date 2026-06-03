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
import androidx.compose.ui.node.SnapshotInvalidationTracker
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.platform.GlobalSnapshotManager
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
    protected val snapshotInvalidationTracker = SnapshotInvalidationTracker(::updateInvalidations)
    protected val inputHandler: ComposeSceneInputHandler =
        ComposeSceneInputHandler(
            prepareForPointerInputEvent = ::runMeasureAndLayout,
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
                // Keep the same scene-boundary snapshot behavior the previous combined render path had
                // via SnapshotInvalidationTracker.sendAndPerformSnapshotChanges(): first send global
                // apply notifications, then run only this scene's queued owner-observer callbacks.
                // This makes snapshot reads that affect layout/draw visible before the phase starts,
                // but keeps the tracker scene-local;
                Snapshot.sendApplyNotifications()

                // Try to get see the up-to-date state before running block
                // Note that this doesn't guarantee it, if sendApplyNotifications is called concurrently
                // in a different thread than this code.
                snapshotInvalidationTracker.performSnapshotChanges()
                snapshotInvalidationTracker.performSnapshotChangesSynchronously(block)
            } finally {
                // This is the previous wrapper's trailing checkpoint written out explicitly.
                // It lets state writes produced during the phase enqueue layout/draw invalidations
                // before the native platform decides whether another layout or draw pass is needed.
                Snapshot.sendApplyNotifications()
                snapshotInvalidationTracker.performSnapshotChanges()
                isInvalidationDisabled = false
            }.also {
                updateInvalidations()
            }
        }

    protected fun updateInvalidations() {
        hasPendingMeasureOrLayout = snapshotInvalidationTracker.hasPendingMeasureOrLayout
        hasPendingDraw = snapshotInvalidationTracker.hasPendingDraw
        if (!isInvalidationDisabled && !isClosed && composition != null) {
            if (hasPendingMeasureOrLayout) {
                invalidateLayout()
            }
            // Snapshot-observer commands queued on this scene need a future host turn to be
            // performed (they're drained inside measureAndLayout/draw's postponeInvalidation), so
            // request a draw invalidation without flipping the scene's own hasPendingDraw flag.
            if (hasPendingDraw || hasPendingSnapshotCommands) {
                invalidateDraw()
            }
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
    }

    @Volatile
    override var hasPendingMeasureOrLayout: Boolean = true
        protected set

    @Volatile
    override var hasPendingDraw: Boolean = true
        protected set

    override val hasPendingSnapshotCommands: Boolean
        get() = snapshotInvalidationTracker.hasPendingSnapshotCommands

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
            frameRecomposer.performScheduledRecomposerTasks()
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
            frameRecomposer.performScheduledRecomposerTasks()
        }

    override fun measureAndLayout() {
        if (isClosed) return

        postponeInvalidation("BaseComposeScene:measureAndLayout") {
            // Android runs owner measure/layout from AndroidComposeView.measureAndLayout() during
            // the host layout traversal. Skiko exposes that phase imperatively so platforms can
            // call it from their native layout pass instead of hiding it inside draw/render.
            runMeasureAndLayout()

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

        postponeInvalidation("BaseComposeScene:draw") {
            // AndroidComposeView.dispatchDraw() begins with measureAndLayout() so layout changes
            // discovered after the host layout traversal are still settled before drawing. Keep
            // that trailing layout pass here even though measureAndLayout() is also a public phase.
            runMeasureAndLayout()
            snapshotInvalidationTracker.onDraw()
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
            frameRecomposer.performScheduledEffects()
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
            frameRecomposer.performScheduledEffects()
        }
    }

    override fun cancelPointerInput() {
        inputHandler.cancelPointerInput()
    }

    override fun sendKeyEvent(keyEvent: KeyEvent): Boolean =
        postponeInvalidation("BaseComposeScene:sendKeyEvent") {
            inputHandler.onKeyEvent(keyEvent).also {
                frameRecomposer.performScheduledEffects()
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
            frameRecomposer.performScheduledEffects()
        }
    }

    protected fun runMeasureAndLayout() {
        snapshotInvalidationTracker.onMeasureAndLayout()
        doMeasureAndLayout()
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
