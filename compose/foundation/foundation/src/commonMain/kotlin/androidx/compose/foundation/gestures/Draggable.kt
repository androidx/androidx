/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.foundation.gestures

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.DragEvent.DragCancelled
import androidx.compose.foundation.gestures.DragEvent.DragDelta
import androidx.compose.foundation.gestures.DragEvent.DragStarted
import androidx.compose.foundation.gestures.DragEvent.DragStopped
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.internal.JvmDefaultWithCompatibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.sign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * State of [draggable]. Allows for a granular control of how deltas are consumed by the user as
 * well as to write custom drag methods using [drag] suspend function.
 */
@JvmDefaultWithCompatibility
interface DraggableState {
    /**
     * Call this function to take control of drag logic.
     *
     * All actions that change the logical drag position must be performed within a [drag]
     * block (even if they don't call any other methods on this object) in order to guarantee
     * that mutual exclusion is enforced.
     *
     * If [drag] is called from elsewhere with the [dragPriority] higher or equal to ongoing
     * drag, ongoing drag will be canceled.
     *
     * @param dragPriority of the drag operation
     * @param block to perform drag in
     */
    suspend fun drag(
        dragPriority: MutatePriority = MutatePriority.Default,
        block: suspend DragScope.() -> Unit
    )

    /**
     * Dispatch drag delta in pixels avoiding all drag related priority mechanisms.
     *
     * **NOTE:** unlike [drag], dispatching any delta with this method will bypass scrolling of
     * any priority. This method will also ignore `reverseDirection` and other parameters set in
     * [draggable].
     *
     * This method is used internally for low level operations, allowing implementers of
     * [DraggableState] influence the consumption as suits them, e.g. introduce nested scrolling.
     * Manually dispatching delta via this method will likely result in a bad user experience,
     * you must prefer [drag] method over this one.
     *
     * @param delta amount of scroll dispatched in the nested drag process
     */
    fun dispatchRawDelta(delta: Float)
}

/**
 * Scope used for suspending drag blocks
 */
interface DragScope {
    /**
     * Attempts to drag by [pixels] px.
     */
    fun dragBy(pixels: Float)
}

/**
 * Default implementation of [DraggableState] interface that allows to pass a simple action that
 * will be invoked when the drag occurs.
 *
 * This is the simplest way to set up a [draggable] modifier. When constructing this
 * [DraggableState], you must provide a [onDelta] lambda, which will be invoked whenever
 * drag happens (by gesture input or a custom [DraggableState.drag] call) with the delta in
 * pixels.
 *
 * If you are creating [DraggableState] in composition, consider using [rememberDraggableState].
 *
 * @param onDelta callback invoked when drag occurs. The callback receives the delta in pixels.
 */
fun DraggableState(onDelta: (Float) -> Unit): DraggableState =
    DefaultDraggableState(onDelta)

/**
 * Create and remember default implementation of [DraggableState] interface that allows to pass a
 * simple action that will be invoked when the drag occurs.
 *
 * This is the simplest way to set up a [draggable] modifier. When constructing this
 * [DraggableState], you must provide a [onDelta] lambda, which will be invoked whenever
 * drag happens (by gesture input or a custom [DraggableState.drag] call) with the delta in
 * pixels.
 *
 * @param onDelta callback invoked when drag occurs. The callback receives the delta in pixels.
 */
@Composable
fun rememberDraggableState(onDelta: (Float) -> Unit): DraggableState {
    val onDeltaState = rememberUpdatedState(onDelta)
    return remember { DraggableState { onDeltaState.value.invoke(it) } }
}

/**
 * Configure touch dragging for the UI element in a single [Orientation]. The drag distance
 * reported to [DraggableState], allowing users to react on the drag delta and update their state.
 *
 * The common usecase for this component is when you need to be able to drag something
 * inside the component on the screen and represent this state via one float value
 *
 * If you need to control the whole dragging flow, consider using [pointerInput] instead with the
 * helper functions like [detectDragGestures].
 *
 * If you want to enable dragging in 2 dimensions, consider using [draggable2D].
 *
 * If you are implementing scroll/fling behavior, consider using [scrollable].
 *
 * @sample androidx.compose.foundation.samples.DraggableSample
 *
 * @param state [DraggableState] state of the draggable. Defines how drag events will be
 * interpreted by the user land logic.
 * @param orientation orientation of the drag
 * @param enabled whether or not drag is enabled
 * @param interactionSource [MutableInteractionSource] that will be used to emit
 * [DragInteraction.Start] when this draggable is being dragged.
 * @param startDragImmediately when set to true, draggable will start dragging immediately and
 * prevent other gesture detectors from reacting to "down" events (in order to block composed
 * press-based gestures).  This is intended to allow end users to "catch" an animating widget by
 * pressing on it. It's useful to set it when value you're dragging is settling / animating.
 * @param onDragStarted callback that will be invoked when drag is about to start at the starting
 * position, allowing user to suspend and perform preparation for drag, if desired. This suspend
 * function is invoked with the draggable scope, allowing for async processing, if desired. Note
 * that the scope used here is the one provided by the draggable node, for long running work that
 * needs to outlast the modifier being in the composition you should use a scope that fits the
 * lifecycle needed.
 * @param onDragStopped callback that will be invoked when drag is finished, allowing the
 * user to react on velocity and process it. This suspend function is invoked with the draggable
 * scope, allowing for async processing, if desired.  Note that the scope used here is the one
 * provided by the draggable node, for long running work that needs to outlast the modifier being
 * in the composition you should use a scope that fits the lifecycle needed.
 * @param reverseDirection reverse the direction of the scroll, so top to bottom scroll will
 * behave like bottom to top and left to right will behave like right to left.
 */
@Stable
fun Modifier.draggable(
    state: DraggableState,
    orientation: Orientation,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    startDragImmediately: Boolean = false,
    onDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit = NoOpOnDragStarted,
    onDragStopped: suspend CoroutineScope.(velocity: Float) -> Unit = NoOpOnDragStopped,
    reverseDirection: Boolean = false
): Modifier = this then DraggableElement(
    state = state,
    orientation = orientation,
    enabled = enabled,
    interactionSource = interactionSource,
    startDragImmediately = startDragImmediately,
    onDragStarted = onDragStarted,
    onDragStopped = onDragStopped,
    reverseDirection = reverseDirection
)

internal class DraggableElement(
    private val state: DraggableState,
    private val orientation: Orientation,
    private val enabled: Boolean,
    private val interactionSource: MutableInteractionSource?,
    private val startDragImmediately: Boolean,
    private val onDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit,
    private val onDragStopped: suspend CoroutineScope.(velocity: Float) -> Unit,
    private val reverseDirection: Boolean
) : ModifierNodeElement<DraggableNode>() {
    override fun create(): DraggableNode = DraggableNode(
        state,
        CanDrag,
        orientation,
        enabled,
        interactionSource,
        if (startDragImmediately) StartDragImmediately else DoNotStartDragImmediately,
        onDragStarted,
        onDragStopped,
        reverseDirection
    )

    override fun update(node: DraggableNode) {
        node.update(
            state,
            CanDrag,
            orientation,
            enabled,
            interactionSource,
            if (startDragImmediately) StartDragImmediately else DoNotStartDragImmediately,
            onDragStarted,
            onDragStopped,
            reverseDirection
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other === null) return false
        if (this::class != other::class) return false

        other as DraggableElement

        if (state != other.state) return false
        if (orientation != other.orientation) return false
        if (enabled != other.enabled) return false
        if (interactionSource != other.interactionSource) return false
        if (startDragImmediately != other.startDragImmediately) return false
        if (onDragStarted != other.onDragStarted) return false
        if (onDragStopped != other.onDragStopped) return false
        if (reverseDirection != other.reverseDirection) return false

        return true
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + orientation.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + (interactionSource?.hashCode() ?: 0)
        result = 31 * result + startDragImmediately.hashCode()
        result = 31 * result + onDragStarted.hashCode()
        result = 31 * result + onDragStopped.hashCode()
        result = 31 * result + reverseDirection.hashCode()
        return result
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "draggable"
        properties["orientation"] = orientation
        properties["enabled"] = enabled
        properties["reverseDirection"] = reverseDirection
        properties["interactionSource"] = interactionSource
        properties["startDragImmediately"] = startDragImmediately
        properties["onDragStarted"] = onDragStarted
        properties["onDragStopped"] = onDragStopped
        properties["state"] = state
    }

    companion object {
        val StartDragImmediately = { true }
        val DoNotStartDragImmediately = { false }
        val CanDrag: (PointerInputChange) -> Boolean = { true }
    }
}

internal class DraggableNode(
    private var state: DraggableState,
    canDrag: (PointerInputChange) -> Boolean,
    private var orientation: Orientation,
    enabled: Boolean,
    interactionSource: MutableInteractionSource?,
    startDragImmediately: () -> Boolean,
    private var onDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit,
    private var onDragStopped: suspend CoroutineScope.(velocity: Float) -> Unit,
    reverseDirection: Boolean
) : AbstractDraggableNode(
    canDrag,
    enabled,
    interactionSource,
    startDragImmediately,
    reverseDirection
) {

    override suspend fun drag(forEachDelta: suspend ((dragDelta: DragDelta) -> Unit) -> Unit) {
        state.drag(MutatePriority.UserInput) {
            forEachDelta { dragDelta ->
                dragBy(dragDelta.delta.toFloat(orientation))
            }
        }
    }

    override val pointerDirectionConfig = orientation.toPointerDirectionConfig()

    override suspend fun CoroutineScope.onDragStarted(startedPosition: Offset) =
        this@DraggableNode.onDragStarted(this, startedPosition)

    override suspend fun CoroutineScope.onDragStopped(velocity: Velocity) =
        this@DraggableNode.onDragStopped(this, velocity.toFloat(orientation))

    fun update(
        state: DraggableState,
        canDrag: (PointerInputChange) -> Boolean,
        orientation: Orientation,
        enabled: Boolean,
        interactionSource: MutableInteractionSource?,
        startDragImmediately: () -> Boolean,
        onDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit,
        onDragStopped: suspend CoroutineScope.(velocity: Float) -> Unit,
        reverseDirection: Boolean
    ) {
        var resetPointerInputHandling = false
        if (this.state != state) {
            this.state = state
            resetPointerInputHandling = true
        }
        if (this.orientation != orientation) {
            this.orientation = orientation
            resetPointerInputHandling = true
        }
        this.onDragStarted = onDragStarted
        this.onDragStopped = onDragStopped

        update(
            canDrag,
            enabled,
            interactionSource,
            startDragImmediately,
            reverseDirection,
            resetPointerInputHandling
        )
    }
}

internal abstract class AbstractDraggableNode(
    private var canDrag: (PointerInputChange) -> Boolean,
    private var enabled: Boolean,
    private var interactionSource: MutableInteractionSource?,
    private var startDragImmediately: () -> Boolean,
    private var reverseDirection: Boolean
) : DelegatingNode(), PointerInputModifierNode, CompositionLocalConsumerModifierNode {

    // Use wrapper lambdas here to make sure that if these properties are updated while we suspend,
    // we point to the new reference when we invoke them. startDragImmediately is a lambda since we
    // need the most recent value passed to it from Scrollable.
    private val _canDrag: (PointerInputChange) -> Boolean = { canDrag(it) }
    private val _startDragImmediately: () -> Boolean = { startDragImmediately() }
    private val velocityTracker = VelocityTracker()
    private var isListeningForEvents = false

    /**
     * Responsible for the dragging behavior between the start and the end of the drag. It
     * continually invokes `forEachDelta` to process incoming events. In return, `forEachDelta`
     * calls `dragBy` method to process each individual delta.
     */
    abstract suspend fun drag(forEachDelta: suspend ((dragDelta: DragDelta) -> Unit) -> Unit)

    /**
     * Returns the pointerDirectionConfig which specifies the main and cross axis deltas. This is
     * important when observing the delta change for Draggable, as we want to observe the change
     * in the main axis only.
     */
    abstract val pointerDirectionConfig: PointerDirectionConfig

    /**
     * Passes the action needed when a drag starts. This gives the ability to pass the desired
     * behavior from other nodes implementing AbstractDraggableNode
     */
    abstract suspend fun CoroutineScope.onDragStarted(startedPosition: Offset)

    /**
     * Passes the action needed when a drag stops. This gives the ability to pass the desired
     * behavior from other nodes implementing AbstractDraggableNode
     */
    abstract suspend fun CoroutineScope.onDragStopped(velocity: Velocity)

    private fun startListeningForEvents() {
        isListeningForEvents = true

        /**
         * To preserve the original behavior we had (before the Modifier.Node migration) we need to
         * scope the DragStopped and DragCancel methods to the node's coroutine scope instead of using
         * the one provided by the pointer input modifier, this is to ensure that even when the pointer
         * input scope is reset we will continue any coroutine scope scope that we started from these
         * methods while the pointer input scope was active.
         */
        coroutineScope.launch {
            while (isActive) {
                var event = channel.receive()
                if (event !is DragStarted) continue
                processDragStart(event)
                try {
                    drag { processDelta ->
                        while (event !is DragStopped && event !is DragCancelled) {
                            (event as? DragDelta)?.let(processDelta)
                            event = channel.receive()
                        }
                    }
                    if (event is DragStopped) {
                        processDragStop(event as DragStopped)
                    } else if (event is DragCancelled) {
                        processDragCancel()
                    }
                } catch (c: CancellationException) {
                    processDragCancel()
                }
            }
        }
    }

    private val pointerInputNode = delegate(SuspendingPointerInputModifierNode {
        // TODO: conditionally undelegate when aosp/2462416 lands?
        if (!enabled) return@SuspendingPointerInputModifierNode
        coroutineScope {
            try {
                awaitPointerEventScope {
                    while (isActive) {
                        awaitDownAndSlop(
                            _canDrag,
                            _startDragImmediately,
                            velocityTracker,
                            pointerDirectionConfig
                        )?.let {
                            /**
                             * The gesture crossed the touch slop, events are now relevant
                             * and should be propagated
                             */
                            if (!isListeningForEvents) {
                                startListeningForEvents()
                            }
                            var isDragSuccessful = false
                            try {
                                isDragSuccessful = awaitDrag(
                                    it.first,
                                    it.second,
                                    velocityTracker,
                                    channel,
                                    reverseDirection
                                ) { event ->
                                    pointerDirectionConfig.calculateDeltaChange(
                                        event.positionChangeIgnoreConsumed()
                                    ) != 0f
                                }
                            } catch (cancellation: CancellationException) {
                                isDragSuccessful = false
                                if (!isActive) throw cancellation
                            } finally {
                                val maximumVelocity = currentValueOf(LocalViewConfiguration)
                                    .maximumFlingVelocity
                                val event = if (isDragSuccessful) {
                                    val velocity = velocityTracker.calculateVelocity(
                                        Velocity(maximumVelocity, maximumVelocity)
                                    )
                                    velocityTracker.resetTracking()
                                    DragStopped(velocity * if (reverseDirection) -1f else 1f)
                                } else {
                                    DragCancelled
                                }
                                channel.trySend(event)
                            }
                        }
                    }
                }
            } catch (exception: CancellationException) {
                if (!isActive) {
                    throw exception
                }
            }
        }
    })

    private val channel = Channel<DragEvent>(capacity = Channel.UNLIMITED)
    private var dragInteraction: DragInteraction.Start? = null

    override fun onDetach() {
        isListeningForEvents = false
        disposeInteractionSource()
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        pointerInputNode.onPointerEvent(pointerEvent, pass, bounds)
    }

    override fun onCancelPointerInput() {
        pointerInputNode.onCancelPointerInput()
    }

    private suspend fun CoroutineScope.processDragStart(event: DragStarted) {
        dragInteraction?.let { oldInteraction ->
            interactionSource?.emit(DragInteraction.Cancel(oldInteraction))
        }
        val interaction = DragInteraction.Start()
        interactionSource?.emit(interaction)
        dragInteraction = interaction
        with(this) { onDragStarted(event.startPoint) }
    }

    private suspend fun CoroutineScope.processDragStop(event: DragStopped) {
        dragInteraction?.let { interaction ->
            interactionSource?.emit(DragInteraction.Stop(interaction))
            dragInteraction = null
        }
        with(this) { onDragStopped(event.velocity) }
    }

    private suspend fun CoroutineScope.processDragCancel() {
        dragInteraction?.let { interaction ->
            interactionSource?.emit(DragInteraction.Cancel(interaction))
            dragInteraction = null
        }
        with(this) { onDragStopped(Velocity.Zero) }
    }

    fun disposeInteractionSource() {
        dragInteraction?.let { interaction ->
            interactionSource?.tryEmit(DragInteraction.Cancel(interaction))
            dragInteraction = null
        }
    }

    fun update(
        canDrag: (PointerInputChange) -> Boolean = this.canDrag,
        enabled: Boolean = this.enabled,
        interactionSource: MutableInteractionSource? = this.interactionSource,
        startDragImmediately: () -> Boolean = this.startDragImmediately,
        reverseDirection: Boolean = this.reverseDirection,
        isResetPointerInputHandling: Boolean = false
    ) {
        var resetPointerInputHandling = isResetPointerInputHandling
        this.canDrag = canDrag
        if (this.enabled != enabled) {
            this.enabled = enabled
            if (!enabled) {
                disposeInteractionSource()
            }
            resetPointerInputHandling = true
        }
        if (this.interactionSource != interactionSource) {
            disposeInteractionSource()
            this.interactionSource = interactionSource
        }
        this.startDragImmediately = startDragImmediately
        if (this.reverseDirection != reverseDirection) {
            this.reverseDirection = reverseDirection
            resetPointerInputHandling = true
        }
        if (resetPointerInputHandling) {
            pointerInputNode.resetPointerInputHandler()
        }
    }
}

private suspend fun AwaitPointerEventScope.awaitDownAndSlop(
    canDrag: (PointerInputChange) -> Boolean,
    startDragImmediately: () -> Boolean,
    velocityTracker: VelocityTracker,
    pointerDirectionConfig: PointerDirectionConfig
): Pair<PointerInputChange, Offset>? {
    val initialDown =
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
    return if (!canDrag(initialDown)) {
        null
    } else if (startDragImmediately()) {
        initialDown.consume()
        velocityTracker.addPointerInputChange(initialDown)
        // since we start immediately we don't wait for slop and the initial delta is 0
        initialDown to Offset.Zero
    } else {
        val down = awaitFirstDown(requireUnconsumed = false)
        velocityTracker.addPointerInputChange(down)
        var initialDelta = Offset.Zero
        val postPointerSlop = { event: PointerInputChange, offset: Offset ->
            velocityTracker.addPointerInputChange(event)
            event.consume()
            initialDelta = offset
        }

        val afterSlopResult = awaitPointerSlopOrCancellation(
            down.id,
            down.type,
            pointerDirectionConfig = pointerDirectionConfig,
            onPointerSlopReached = postPointerSlop
        )

        if (afterSlopResult != null) afterSlopResult to initialDelta else null
    }
}

private suspend fun AwaitPointerEventScope.awaitDrag(
    startEvent: PointerInputChange,
    initialDelta: Offset,
    velocityTracker: VelocityTracker,
    channel: SendChannel<DragEvent>,
    reverseDirection: Boolean,
    hasDragged: (PointerInputChange) -> Boolean,
): Boolean {

    val overSlopOffset = initialDelta
    val xSign = sign(startEvent.position.x)
    val ySign = sign(startEvent.position.y)
    val adjustedStart = startEvent.position -
        Offset(overSlopOffset.x * xSign, overSlopOffset.y * ySign)
    channel.trySend(DragStarted(adjustedStart))

    channel.trySend(DragDelta(if (reverseDirection) initialDelta * -1f else initialDelta))

    return onDragOrUp(hasDragged, startEvent.id) { event ->
        // Velocity tracker takes all events, even UP
        velocityTracker.addPointerInputChange(event)

        // Dispatch only MOVE events
        if (!event.changedToUpIgnoreConsumed()) {
            val delta = event.positionChange()
            event.consume()
            channel.trySend(DragDelta(if (reverseDirection) delta * -1f else delta))
        }
    }
}

private suspend fun AwaitPointerEventScope.onDragOrUp(
    hasDragged: (PointerInputChange) -> Boolean,
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit
): Boolean {
    return drag(
        pointerId = pointerId,
        onDrag = onDrag,
        hasDragged = hasDragged,
        motionConsumed = { it.isConsumed }
    )?.let(onDrag) != null
}

private class DefaultDraggableState(val onDelta: (Float) -> Unit) : DraggableState {

    private val dragScope: DragScope = object : DragScope {
        override fun dragBy(pixels: Float): Unit = onDelta(pixels)
    }

    private val scrollMutex = MutatorMutex()

    override suspend fun drag(
        dragPriority: MutatePriority,
        block: suspend DragScope.() -> Unit
    ): Unit = coroutineScope {
        scrollMutex.mutateWith(dragScope, dragPriority, block)
    }

    override fun dispatchRawDelta(delta: Float) {
        return onDelta(delta)
    }
}

internal sealed class DragEvent {
    class DragStarted(val startPoint: Offset) : DragEvent()
    class DragStopped(val velocity: Velocity) : DragEvent()
    object DragCancelled : DragEvent()
    class DragDelta(val delta: Offset) : DragEvent()
}

private fun Offset.toFloat(orientation: Orientation) =
    if (orientation == Orientation.Vertical) this.y else this.x

private fun Velocity.toFloat(orientation: Orientation) =
    if (orientation == Orientation.Vertical) this.y else this.x

private val NoOpOnDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit = {}
private val NoOpOnDragStopped: suspend CoroutineScope.(velocity: Float) -> Unit = {}
