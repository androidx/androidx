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

import androidx.compose.foundation.ComposeFoundationFlags.isNonSuspendingPointerInputInDraggableEnabled
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.ExperimentalIndirectTouchTypeApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.input.indirect.IndirectTouchEvent
import androidx.compose.ui.input.indirect.IndirectTouchEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.input.indirect.IndirectTouchEventType
import androidx.compose.ui.input.indirect.IndirectTouchInputModifierNode
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
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastMap
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.sign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
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
     * All actions that change the logical drag position must be performed within a [drag] block
     * (even if they don't call any other methods on this object) in order to guarantee that mutual
     * exclusion is enforced.
     *
     * If [drag] is called from elsewhere with the [dragPriority] higher or equal to ongoing drag,
     * ongoing drag will be canceled.
     *
     * @param dragPriority of the drag operation
     * @param block to perform drag in
     */
    suspend fun drag(
        dragPriority: MutatePriority = MutatePriority.Default,
        block: suspend DragScope.() -> Unit,
    )

    /**
     * Dispatch drag delta in pixels avoiding all drag related priority mechanisms.
     *
     * **NOTE:** unlike [drag], dispatching any delta with this method will bypass scrolling of any
     * priority. This method will also ignore `reverseDirection` and other parameters set in
     * [draggable].
     *
     * This method is used internally for low level operations, allowing implementers of
     * [DraggableState] influence the consumption as suits them, e.g. introduce nested scrolling.
     * Manually dispatching delta via this method will likely result in a bad user experience, you
     * must prefer [drag] method over this one.
     *
     * @param delta amount of scroll dispatched in the nested drag process
     */
    fun dispatchRawDelta(delta: Float)
}

/** Scope used for suspending drag blocks */
interface DragScope {
    /** Attempts to drag by [pixels] px. */
    fun dragBy(pixels: Float)
}

/**
 * Default implementation of [DraggableState] interface that allows to pass a simple action that
 * will be invoked when the drag occurs.
 *
 * This is the simplest way to set up a [draggable] modifier. When constructing this
 * [DraggableState], you must provide a [onDelta] lambda, which will be invoked whenever drag
 * happens (by gesture input or a custom [DraggableState.drag] call) with the delta in pixels.
 *
 * If you are creating [DraggableState] in composition, consider using [rememberDraggableState].
 *
 * @param onDelta callback invoked when drag occurs. The callback receives the delta in pixels.
 */
fun DraggableState(onDelta: (Float) -> Unit): DraggableState = DefaultDraggableState(onDelta)

/**
 * Create and remember default implementation of [DraggableState] interface that allows to pass a
 * simple action that will be invoked when the drag occurs.
 *
 * This is the simplest way to set up a [draggable] modifier. When constructing this
 * [DraggableState], you must provide a [onDelta] lambda, which will be invoked whenever drag
 * happens (by gesture input or a custom [DraggableState.drag] call) with the delta in pixels.
 *
 * @param onDelta callback invoked when drag occurs. The callback receives the delta in pixels.
 */
@Composable
fun rememberDraggableState(onDelta: (Float) -> Unit): DraggableState {
    val onDeltaState = rememberUpdatedState(onDelta)
    return remember { DraggableState { onDeltaState.value.invoke(it) } }
}

/**
 * Configure touch dragging for the UI element in a single [Orientation]. The drag distance reported
 * to [DraggableState], allowing users to react on the drag delta and update their state.
 *
 * The common usecase for this component is when you need to be able to drag something inside the
 * component on the screen and represent this state via one float value
 *
 * If you need to control the whole dragging flow, consider using [pointerInput] instead with the
 * helper functions like [detectDragGestures].
 *
 * If you want to enable dragging in 2 dimensions, consider using [draggable2D].
 *
 * If you are implementing scroll/fling behavior, consider using [scrollable].
 *
 * @sample androidx.compose.foundation.samples.DraggableSample
 * @param state [DraggableState] state of the draggable. Defines how drag events will be interpreted
 *   by the user land logic.
 * @param orientation orientation of the drag
 * @param enabled whether or not drag is enabled
 * @param interactionSource [MutableInteractionSource] that will be used to emit
 *   [DragInteraction.Start] when this draggable is being dragged.
 * @param startDragImmediately when set to true, draggable will start dragging immediately and
 *   prevent other gesture detectors from reacting to "down" events (in order to block composed
 *   press-based gestures). This is intended to allow end users to "catch" an animating widget by
 *   pressing on it. It's useful to set it when value you're dragging is settling / animating.
 * @param onDragStarted callback that will be invoked when drag is about to start at the starting
 *   position, allowing user to suspend and perform preparation for drag, if desired. This suspend
 *   function is invoked with the draggable scope, allowing for async processing, if desired. Note
 *   that the scope used here is the one provided by the draggable node, for long running work that
 *   needs to outlast the modifier being in the composition you should use a scope that fits the
 *   lifecycle needed.
 * @param onDragStopped callback that will be invoked when drag is finished, allowing the user to
 *   react on velocity and process it. This suspend function is invoked with the draggable scope,
 *   allowing for async processing, if desired. Note that the scope used here is the one provided by
 *   the draggable node, for long running work that needs to outlast the modifier being in the
 *   composition you should use a scope that fits the lifecycle needed.
 * @param reverseDirection reverse the direction of the scroll, so top to bottom scroll will behave
 *   like bottom to top and left to right will behave like right to left.
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
    reverseDirection: Boolean = false,
): Modifier =
    this then
        DraggableElement(
            state = state,
            orientation = orientation,
            enabled = enabled,
            interactionSource = interactionSource,
            startDragImmediately = startDragImmediately,
            onDragStarted = onDragStarted,
            onDragStopped = onDragStopped,
            reverseDirection = reverseDirection,
        )

internal class DraggableElement(
    private val state: DraggableState,
    private val orientation: Orientation,
    private val enabled: Boolean,
    private val interactionSource: MutableInteractionSource?,
    private val startDragImmediately: Boolean,
    private val onDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit,
    private val onDragStopped: suspend CoroutineScope.(velocity: Float) -> Unit,
    private val reverseDirection: Boolean,
) : ModifierNodeElement<DraggableNode>() {
    override fun create(): DraggableNode =
        DraggableNode(
            state,
            CanDrag,
            orientation,
            enabled,
            interactionSource,
            startDragImmediately,
            onDragStarted,
            onDragStopped,
            reverseDirection,
        )

    override fun update(node: DraggableNode) {
        node.update(
            state,
            CanDrag,
            orientation,
            enabled,
            interactionSource,
            startDragImmediately,
            onDragStarted,
            onDragStopped,
            reverseDirection,
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
        val CanDrag: (PointerInputChange) -> Boolean = { true }
    }
}

internal class DraggableNode(
    private var state: DraggableState,
    canDrag: (PointerInputChange) -> Boolean,
    private var orientation: Orientation,
    enabled: Boolean,
    interactionSource: MutableInteractionSource?,
    private var startDragImmediately: Boolean,
    private var onDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit,
    private var onDragStopped: suspend CoroutineScope.(velocity: Float) -> Unit,
    private var reverseDirection: Boolean,
) :
    DragGestureNode(
        canDrag = canDrag,
        enabled = enabled,
        interactionSource = interactionSource,
        orientationLock = orientation,
    ) {

    override suspend fun drag(forEachDelta: suspend ((dragDelta: DragDelta) -> Unit) -> Unit) {
        state.drag(MutatePriority.UserInput) {
            forEachDelta { dragDelta ->
                dragBy(dragDelta.delta.reverseIfNeeded().toFloat(orientation))
            }
        }
    }

    override fun onDragStarted(startedPosition: Offset) {
        if (!isAttached || onDragStarted == NoOpOnDragStarted) return
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            this@DraggableNode.onDragStarted(this, startedPosition)
        }
    }

    override fun onDragStopped(event: DragStopped) {
        if (!isAttached || onDragStopped == NoOpOnDragStopped) return
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            this@DraggableNode.onDragStopped(
                this,
                event.velocity.reverseIfNeeded().toFloat(orientation),
            )
        }
    }

    override fun startDragImmediately(): Boolean = startDragImmediately

    fun update(
        state: DraggableState,
        canDrag: (PointerInputChange) -> Boolean,
        orientation: Orientation,
        enabled: Boolean,
        interactionSource: MutableInteractionSource?,
        startDragImmediately: Boolean,
        onDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit,
        onDragStopped: suspend CoroutineScope.(velocity: Float) -> Unit,
        reverseDirection: Boolean,
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
        if (this.reverseDirection != reverseDirection) {
            this.reverseDirection = reverseDirection
            resetPointerInputHandling = true
        }

        this.onDragStarted = onDragStarted
        this.onDragStopped = onDragStopped
        this.startDragImmediately = startDragImmediately

        update(canDrag, enabled, interactionSource, orientation, resetPointerInputHandling)
    }

    private fun Velocity.reverseIfNeeded() = if (reverseDirection) this * -1f else this * 1f

    private fun Offset.reverseIfNeeded() = if (reverseDirection) this * -1f else this * 1f
}

// TODO(levima) Remove once ExperimentalIndirectTouchTypeApi stable b/426155641
/** A node that performs drag gesture recognition and event propagation. */
@OptIn(ExperimentalIndirectTouchTypeApi::class, ExperimentalFoundationApi::class)
internal abstract class DragGestureNode(
    canDrag: (PointerInputChange) -> Boolean,
    enabled: Boolean,
    interactionSource: MutableInteractionSource?,
    private var orientationLock: Orientation?,
) :
    DelegatingNode(),
    PointerInputModifierNode,
    IndirectTouchInputModifierNode,
    CompositionLocalConsumerModifierNode {

    protected var canDrag = canDrag
        private set

    protected var enabled = enabled
        private set

    protected var interactionSource = interactionSource
        private set

    // Use wrapper lambdas here to make sure that if these properties are updated while we suspend,
    // we point to the new reference when we invoke them. startDragImmediately is a lambda since we
    // need the most recent value passed to it from Scrollable.
    private val _canDrag: (PointerInputChange) -> Boolean = { this.canDrag(it) }
    private var channel: Channel<DragEvent>? = null
    private var dragInteraction: DragInteraction.Start? = null
    private var isListeningForEvents = false
    private var indirectTouchEventProcessor: IndirectTouchEventProcessor? = null

    /** Store non-initialized states for re-use */
    private var _awaitDownState: DragDetectionState.AwaitDown? = null
    private val awaitDownState: DragDetectionState.AwaitDown
        get() = _awaitDownState ?: DragDetectionState.AwaitDown().also { _awaitDownState = it }

    private var _draggingState: DragDetectionState.Dragging? = null
    private val draggingState: DragDetectionState.Dragging
        get() = _draggingState ?: DragDetectionState.Dragging().also { _draggingState = it }

    private var _awaitTouchSlopState: DragDetectionState.AwaitTouchSlop? = null
    private val awaitTouchSlopState: DragDetectionState.AwaitTouchSlop
        get() =
            _awaitTouchSlopState
                ?: DragDetectionState.AwaitTouchSlop().also { _awaitTouchSlopState = it }

    private var _awaitGesturePickupState: DragDetectionState.AwaitGesturePickup? = null
    private val awaitGesturePickupState: DragDetectionState.AwaitGesturePickup
        get() =
            _awaitGesturePickupState
                ?: DragDetectionState.AwaitGesturePickup().also { _awaitGesturePickupState = it }

    private var currentDragState: DragDetectionState? = null
    private var velocityTracker: VelocityTracker? = null
    private var previousPositionOnScreen = Offset.Unspecified
    private var touchSlopDetector: TouchSlopDetector? = null

    /**
     * Accumulated position offset of this [Modifier.Node] that happened during a drag cycle. This
     * is used to correct the pointer input events that are added to the Velocity Tracker. If this
     * Node is static during the drag cycle, nothing will happen. On the other hand, if the position
     * of this node changes during the drag cycle, we need to correct the Pointer Input used for the
     * drag events, this is because Velocity Tracker doesn't have the knowledge about changes in the
     * position of the container that uses it, and because each Pointer Input event is related to
     * the container's root.
     */
    private var nodeOffset = Offset.Zero

    /**
     * Responsible for the dragging behavior between the start and the end of the drag. It
     * continually invokes `forEachDelta` to process incoming events. In return, `forEachDelta`
     * calls `dragBy` method to process each individual delta.
     */
    abstract suspend fun drag(forEachDelta: suspend ((dragDelta: DragDelta) -> Unit) -> Unit)

    /**
     * Passes the action needed when a drag starts. This gives the ability to pass the desired
     * behavior from other nodes implementing AbstractDraggableNode
     */
    abstract fun onDragStarted(startedPosition: Offset)

    /**
     * Passes the action needed when a drag stops. This gives the ability to pass the desired
     * behavior from other nodes implementing AbstractDraggableNode
     */
    abstract fun onDragStopped(event: DragStopped)

    /**
     * If touch slop recognition should be skipped. If this is true, this node will start
     * recognizing drag events immediately without waiting for touch slop.
     */
    abstract fun startDragImmediately(): Boolean

    private fun requireVelocityTracker(): VelocityTracker =
        requireNotNull(velocityTracker) { "Velocity Tracker not initialized." }

    private fun requireChannel(): Channel<DragEvent> =
        requireNotNull(channel) { "Events channel not initialized." }

    private fun requireTouchSlopDetector(): TouchSlopDetector =
        requireNotNull(touchSlopDetector) { "Touch slop detector not initialized." }

    @OptIn(ExperimentalFoundationApi::class)
    private fun startListeningForEvents() {
        isListeningForEvents = true

        if (channel == null) {
            channel = Channel(capacity = Channel.UNLIMITED)
        }

        /**
         * To preserve the original behavior we had (before the Modifier.Node migration) we need to
         * scope the DragStopped and DragCancel methods to the node's coroutine scope instead of
         * using the one provided by the pointer input modifier, this is to ensure that even when
         * the pointer input scope is reset we will continue any coroutine scope scope that we
         * started from these methods while the pointer input scope was active.
         */
        coroutineScope.launch {
            while (isActive) {
                var event = channel?.receive()
                if (event !is DragStarted) continue
                processDragStart(event)
                try {
                    drag { processDelta ->
                        while (event !is DragStopped && event !is DragCancelled) {
                            (event as? DragDelta)?.let(processDelta)
                            event = channel?.receive()
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

    private var pointerInputNode: SuspendingPointerInputModifierNode? = null

    override fun onDetach() {
        isListeningForEvents = false
        disposeInteractionSource()
        nodeOffset = Offset.Zero
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        if (isNonSuspendingPointerInputInDraggableEnabled) {
            if (enabled) {
                // initialize current state
                if (currentDragState == null) currentDragState = awaitDownState
                processRawPointerEvent(pointerEvent, pass)
            }
        } else {
            if (enabled && pointerInputNode == null) {
                pointerInputNode = delegate(initializePointerInputNode())
            }
            pointerInputNode?.onPointerEvent(pointerEvent, pass, bounds)
        }
    }

    override fun onIndirectTouchEvent(event: IndirectTouchEvent): Boolean {
        if (!enabled) return false
        val orientation = orientationLock
        if (orientation == null) return false

        if (indirectTouchEventProcessor == null) {
            indirectTouchEventProcessor =
                IndirectTouchEventProcessor(
                    startGestureTrigger = { if (!isListeningForEvents) startListeningForEvents() },
                    onDragEvent = { channel?.trySend(it) },
                )
        }

        /**
         * TODO(levima) Get the touchslop from device aware ViewConfiguration once it lands
         *   b/370720522
         */
        return indirectTouchEventProcessor!!.processIndirectTouchEvent(
            event,
            orientation,
            currentValueOf(LocalViewConfiguration),
        )
    }

    /** Draggable will consume during the main pass. */
    override fun onPreIndirectTouchEvent(event: IndirectTouchEvent): Boolean = false

    @OptIn(ExperimentalFoundationApi::class)
    private fun initializePointerInputNode(): SuspendingPointerInputModifierNode {
        return SuspendingPointerInputModifierNode {
            // re-create tracker when pointer input block restarts. This lazily creates the tracker
            // only when it is need.
            val suspendingPointerInputVelocityTracker = VelocityTracker()
            var previousPositionOnScreen = requireLayoutCoordinates().positionOnScreen()
            val onDragStart:
                (
                    down: PointerInputChange,
                    slopTriggerChange: PointerInputChange,
                    postSlopOffset: Offset,
                ) -> Unit =
                { down, slopTriggerChange, postSlopOffset ->
                    nodeOffset = Offset.Zero // restart node offset
                    if (canDrag.invoke(down)) {
                        if (!isListeningForEvents) startListeningForEvents()
                        suspendingPointerInputVelocityTracker.addPointerInputChange(down)
                        val dragStartedOffset = slopTriggerChange.position - postSlopOffset
                        // the drag start event offset is the down event + touch slop value
                        // or in this case the event that triggered the touch slop minus
                        // the post slop offset
                        channel?.trySend(DragStarted(dragStartedOffset))
                    }
                }

            val onDragEnd: (change: PointerInputChange) -> Unit = { upEvent ->
                suspendingPointerInputVelocityTracker.addPointerInputChange(upEvent)
                val maximumVelocity = viewConfiguration.maximumFlingVelocity
                val velocity =
                    suspendingPointerInputVelocityTracker.calculateVelocity(
                        Velocity(maximumVelocity, maximumVelocity)
                    )
                suspendingPointerInputVelocityTracker.resetTracking()
                channel?.trySend(
                    DragStopped(velocity.toValidVelocity(), isIndirectTouchEvent = false)
                )
            }

            val onDragCancel: () -> Unit = { channel?.trySend(DragCancelled) }

            val shouldAwaitTouchSlop: () -> Boolean = { !startDragImmediately() }

            val onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit =
                { change, delta ->
                    val currentPositionOnScreen = requireLayoutCoordinates().positionOnScreen()
                    // container changed positions
                    if (currentPositionOnScreen != previousPositionOnScreen) {
                        val delta = currentPositionOnScreen - previousPositionOnScreen
                        nodeOffset += delta
                    }
                    previousPositionOnScreen = currentPositionOnScreen
                    suspendingPointerInputVelocityTracker.addPointerInputChange(
                        event = change,
                        offset = nodeOffset,
                    )
                    channel?.trySend(DragDelta(delta, isIndirectTouchEvent = false))
                }

            coroutineScope {
                try {
                    detectDragGestures(
                        orientationLock = orientationLock,
                        onDragStart = onDragStart,
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragCancel,
                        shouldAwaitTouchSlop = shouldAwaitTouchSlop,
                        onDrag = onDrag,
                    )
                } catch (cancellation: CancellationException) {
                    channel?.trySend(DragCancelled)
                    if (!isActive) throw cancellation
                }
            }
        }
    }

    override fun onCancelPointerInput() {
        indirectTouchEventProcessor?.resetProcessor()
        pointerInputNode?.onCancelPointerInput()
        if (isNonSuspendingPointerInputInDraggableEnabled) resetDragDetectionState()
    }

    private suspend fun processDragStart(event: DragStarted) {
        dragInteraction?.let { oldInteraction ->
            interactionSource?.emit(DragInteraction.Cancel(oldInteraction))
        }
        val interaction = DragInteraction.Start()
        interactionSource?.emit(interaction)
        dragInteraction = interaction
        onDragStarted(event.startPoint)
    }

    private suspend fun processDragStop(event: DragStopped) {
        dragInteraction?.let { interaction ->
            interactionSource?.emit(DragInteraction.Stop(interaction))
            dragInteraction = null
        }
        onDragStopped(event)
    }

    private suspend fun processDragCancel() {
        dragInteraction?.let { interaction ->
            interactionSource?.emit(DragInteraction.Cancel(interaction))
            dragInteraction = null
        }
        onDragStopped(DragStopped(Velocity.Zero, isIndirectTouchEvent = false))
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
        orientationLock: Orientation? = this.orientationLock,
        shouldResetPointerInputHandling: Boolean = false,
    ) {
        var resetPointerInputHandling = shouldResetPointerInputHandling

        this.canDrag = canDrag
        if (this.enabled != enabled) {
            this.enabled = enabled
            if (!enabled) {
                disposeInteractionSource()
                pointerInputNode?.let { undelegate(it) }
                pointerInputNode = null
            }
            resetPointerInputHandling = true
        }
        if (this.interactionSource != interactionSource) {
            disposeInteractionSource()
            this.interactionSource = interactionSource
        }

        if (this.orientationLock != orientationLock) {
            this.orientationLock = orientationLock
            resetPointerInputHandling = true
        }

        if (resetPointerInputHandling) {
            if (isNonSuspendingPointerInputInDraggableEnabled) resetDragDetectionState()
            indirectTouchEventProcessor?.resetProcessor()
            pointerInputNode?.resetPointerInputHandler()
        }
    }

    private fun processRawPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass) {
        when (
            val state = requireNotNull(currentDragState) { "currentDragState should not be null" }
        ) {
            is DragDetectionState.AwaitDown -> processInitialDownState(pointerEvent, pass, state)
            is DragDetectionState.AwaitTouchSlop -> processAwaitTouchSlop(pointerEvent, pass, state)
            is DragDetectionState.AwaitGesturePickup ->
                processAwaitGesturePickup(pointerEvent, pass, state)
            is DragDetectionState.Dragging -> processDraggingState(pointerEvent, pass, state)
        }
    }

    private fun resetDragDetectionState() {
        moveToAwaitDownState()
        if (isListeningForEvents) sendDragCancelled()
        velocityTracker = null
    }

    private fun moveToAwaitTouchSlopState(
        initialDown: PointerInputChange,
        pointerId: PointerId,
        initialTouchSlopPositionChange: Offset = Offset.Zero,
    ) {
        currentDragState =
            awaitTouchSlopState.apply {
                this.initialDown = initialDown
                this.pointerId = pointerId
                if (touchSlopDetector == null) {
                    touchSlopDetector = TouchSlopDetector(orientationLock)
                } else {
                    touchSlopDetector?.orientation = orientationLock
                    touchSlopDetector?.reset(initialTouchSlopPositionChange)
                }
            }
    }

    private fun moveToDraggingState(pointerId: PointerId) {
        currentDragState = draggingState.apply { this.pointerId = pointerId }
    }

    private fun moveToAwaitDownState() {
        currentDragState =
            awaitDownState.apply {
                awaitTouchSlop = DragDetectionState.AwaitDown.AwaitTouchSlop.NotInitialized
                consumedOnInitial = false
            }
    }

    private fun moveToAwaitGesturePickupState(
        initialDown: PointerInputChange,
        pointerId: PointerId,
        touchSlopDetector: TouchSlopDetector,
    ) {
        currentDragState =
            awaitGesturePickupState.apply {
                this.initialDown = initialDown
                this.pointerId = pointerId
                this.touchSlopDetector = touchSlopDetector.also { it.reset() }
            }
    }

    private fun processInitialDownState(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        state: DragDetectionState.AwaitDown,
    ) {
        /** Wait for a down event in any pass. */
        if (pointerEvent.changes.isEmpty()) return
        if (!pointerEvent.isChangedToDown(requireUnconsumed = false)) return

        val firstDown = pointerEvent.changes.first()
        val awaitTouchSlop =
            when (state.awaitTouchSlop) {
                DragDetectionState.AwaitDown.AwaitTouchSlop.NotInitialized -> {
                    if (!startDragImmediately()) {
                        DragDetectionState.AwaitDown.AwaitTouchSlop.Yes
                    } else {
                        DragDetectionState.AwaitDown.AwaitTouchSlop.No
                    }
                }
                else -> state.awaitTouchSlop
            }

        // update the touch slop in the current state
        state.awaitTouchSlop = awaitTouchSlop

        if (pass == PointerEventPass.Initial) {
            // If we shouldn't await touch slop, we consume the event immediately.
            if (awaitTouchSlop == DragDetectionState.AwaitDown.AwaitTouchSlop.No) {
                firstDown.consume()

                // Change state properties so we dispatch only later, this aligns with the previous
                // behavior where dispatching only happened during the main pass
                state.consumedOnInitial = true
            }
        }

        if (pass == PointerEventPass.Main) {
            /**
             * At this point we detected a Down event, if we should await the slop we move to the
             * next state. If we shouldn't await the slop and we already consumed the event we
             * dispatch the drag start events and start the dragging state.
             */
            if (awaitTouchSlop == DragDetectionState.AwaitDown.AwaitTouchSlop.Yes) {
                moveToAwaitTouchSlopState(firstDown, firstDown.id)
            } else if (state.consumedOnInitial) {
                sendDragStart(firstDown, firstDown, Offset.Zero)
                sendDragEvent(firstDown, Offset.Zero)
                moveToDraggingState(firstDown.id)
            }
        }
    }

    private fun processAwaitTouchSlop(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        state: DragDetectionState.AwaitTouchSlop,
    ) {
        /** Slop detection only happens during the main pass */
        if (pass != PointerEventPass.Main) return
        val dragEvent = pointerEvent.changes.fastFirstOrNull { it.id == state.pointerId } ?: return

        /** Do we have unconsumed events for this pointer? */
        if (!dragEvent.isConsumed) {
            if (dragEvent.changedToUpIgnoreConsumed()) {
                /** The pointer lifted, look for another pointer */
                val otherDown = pointerEvent.changes.fastFirstOrNull { it.pressed }
                if (otherDown == null) {
                    // There are no other pointers down, reset the state
                    moveToAwaitDownState()
                } else {
                    // a new pointer was found, update the current state.
                    state.pointerId = otherDown.id
                }
            } else {
                // this is a regular event (MOVE)
                val touchSlop = currentValueOf(LocalViewConfiguration).pointerSlop(dragEvent.type)

                // add data to the slop detector
                val postSlopOffset =
                    requireTouchSlopDetector().addPointerInputChange(dragEvent, touchSlop)

                // slop was crossed, dispatch the drag start event and change to dragging state
                if (postSlopOffset.isSpecified) {
                    dragEvent.consume()
                    sendDragStart(state.initialDown!!, dragEvent, postSlopOffset)
                    sendDragEvent(dragEvent, postSlopOffset)
                    moveToDraggingState(dragEvent.id)
                }
            }
        } else {
            // This draggable "lost" the event as it was consumed by someone else, enter the
            // gesture pickup state if the feature is enabled.
            // Someone consumed this gesture, move this to the await pickup state.
            moveToAwaitGesturePickupState(
                requireNotNull(state.initialDown) {
                    "AwaitTouchSlop.initialDown was not initialized"
                },
                state.pointerId,
                requireNotNull(touchSlopDetector) {
                    "AwaitTouchSlop.touchSlopDetector was not initialized"
                },
            )
        }
    }

    private fun processAwaitGesturePickup(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        state: DragDetectionState.AwaitGesturePickup,
    ) {
        /**
         * Drag pickup only happens during the final pass so we're sure nobody else was interested
         * in this gesture.
         */
        if (pass != PointerEventPass.Final) return
        val hasUnconsumedDrag = pointerEvent.changes.fastAll { !it.isConsumed }
        val hasDownPointers = pointerEvent.changes.fastAny { it.pressed }
        // all pointers are up, reset
        if (!hasDownPointers || pointerEvent.changes.isEmpty()) {
            moveToAwaitDownState()
        } else if (hasUnconsumedDrag) {
            // has pointers down with unconsumed events, a chance to pick up this gesture,
            // move to the touch slop detection phase
            val initialPositionChange =
                pointerEvent.changes.first().position - state.initialDown!!.position

            // await touch slop again, using the initial down as starting point.
            // For most cases this should return immediately since we probably moved
            // far enough from the initial down event.
            moveToAwaitTouchSlopState(
                requireNotNull(state.initialDown) {
                    "AwaitGesturePickup.initialDown was not initialized."
                },
                state.pointerId,
                initialPositionChange,
            )
        }
    }

    private fun processDraggingState(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        state: DragDetectionState.Dragging,
    ) {
        if (pass != PointerEventPass.Main) return

        val pointer = state.pointerId
        val dragEvent = pointerEvent.changes.fastFirstOrNull { it.id == pointer } ?: return
        if (dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = pointerEvent.changes.fastFirstOrNull { it.pressed }
            if (otherDown == null) {
                // This is the last "up"
                if (!dragEvent.isConsumed && dragEvent.changedToUpIgnoreConsumed()) {
                    sendDragStopped(dragEvent)
                } else {
                    sendDragCancelled()
                }
                moveToAwaitDownState()
            } else {
                state.pointerId = otherDown.id
            }
        } else {
            if (dragEvent.isConsumed) {
                sendDragCancelled()
            } else {
                val positionChange = dragEvent.positionChangeIgnoreConsumed()

                /**
                 * During the gesture pickup we can pickup events at any direction so disable the
                 * orientation lock.
                 */
                val motionChange = positionChange.getDistance()
                if (motionChange != 0.0f) {
                    val positionChange = dragEvent.positionChange()
                    sendDragEvent(dragEvent, positionChange)
                    dragEvent.consume()
                }
            }
        }
    }

    private fun sendDragStart(
        down: PointerInputChange,
        slopTriggerChange: PointerInputChange,
        overSlopOffset: Offset,
    ) {
        if (velocityTracker == null) velocityTracker = VelocityTracker()
        requireVelocityTracker().addPointerInputChange(down)
        val dragStartedOffset = slopTriggerChange.position - overSlopOffset
        // the drag start event offset is the down event + touch slop value
        // or in this case the event that triggered the touch slop minus
        // the post slop offset
        nodeOffset = Offset.Zero // restart node offset
        if (canDrag(down)) {
            if (!isListeningForEvents) {
                if (channel == null) {
                    channel = Channel(capacity = Channel.UNLIMITED)
                }
                startListeningForEvents()
            }
            previousPositionOnScreen = requireLayoutCoordinates().positionOnScreen()
            requireChannel().trySend(DragStarted(dragStartedOffset))
        }
    }

    private fun sendDragEvent(change: PointerInputChange, dragAmount: Offset) {
        val currentPositionOnScreen = node.requireLayoutCoordinates().positionOnScreen()
        // container changed positions
        if (
            previousPositionOnScreen != Offset.Unspecified &&
                currentPositionOnScreen != previousPositionOnScreen
        ) {
            val delta = currentPositionOnScreen - previousPositionOnScreen
            nodeOffset += delta
        }
        previousPositionOnScreen = currentPositionOnScreen
        requireVelocityTracker().addPointerInputChange(event = change, offset = nodeOffset)
        requireChannel().trySend(DragDelta(dragAmount, false))
    }

    private fun sendDragStopped(change: PointerInputChange) {
        requireVelocityTracker().addPointerInputChange(change)
        val maximumVelocity = currentValueOf(LocalViewConfiguration).maximumFlingVelocity
        val velocity =
            requireVelocityTracker().calculateVelocity(Velocity(maximumVelocity, maximumVelocity))
        requireVelocityTracker().resetTracking()
        requireChannel().trySend(DragStopped(velocity.toValidVelocity(), false))
    }

    private fun sendDragCancelled() {
        requireChannel().trySend(DragCancelled)
    }
}

private class DefaultDraggableState(val onDelta: (Float) -> Unit) : DraggableState {

    private val dragScope: DragScope =
        object : DragScope {
            override fun dragBy(pixels: Float): Unit = onDelta(pixels)
        }

    private val scrollMutex = MutatorMutex()

    override suspend fun drag(
        dragPriority: MutatePriority,
        block: suspend DragScope.() -> Unit,
    ): Unit = coroutineScope { scrollMutex.mutateWith(dragScope, dragPriority, block) }

    override fun dispatchRawDelta(delta: Float) {
        return onDelta(delta)
    }
}

internal sealed class DragEvent {
    class DragStarted(val startPoint: Offset) : DragEvent()

    class DragStopped(val velocity: Velocity, val isIndirectTouchEvent: Boolean) : DragEvent()

    object DragCancelled : DragEvent()

    class DragDelta(val delta: Offset, val isIndirectTouchEvent: Boolean) : DragEvent()
}

private fun Offset.toFloat(orientation: Orientation) =
    if (orientation == Orientation.Vertical) this.y else this.x

private fun Velocity.toFloat(orientation: Orientation) =
    if (orientation == Orientation.Vertical) this.y else this.x

private fun Velocity.toValidVelocity() =
    Velocity(if (this.x.isNaN()) 0f else this.x, if (this.y.isNaN()) 0f else this.y)

private val NoOpOnDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit = {}
private val NoOpOnDragStopped: suspend CoroutineScope.(velocity: Float) -> Unit = {}

private sealed class DragDetectionState {
    /**
     * Starter state for any drag gesture cycle. At this state we're waiting for a Down event to
     * indicate that a drag gesture may start. Since drag gesture start at the initial pass we have
     * the option to indicate if we consumed the event during the initial pass using
     * [consumedOnInitial]. We also save the [awaitTouchSlop] between passes so we don't call the
     * [DragGestureNode.startDragImmediately] as often.
     */
    class AwaitDown(
        var awaitTouchSlop: AwaitTouchSlop = AwaitTouchSlop.NotInitialized,
        var consumedOnInitial: Boolean = false,
    ) : DragDetectionState() {

        enum class AwaitTouchSlop {
            Yes,
            No,
            NotInitialized,
        }
    }

    /**
     * If drag should wait for touch slop, after the initial down recognition we move to this state.
     * Here we will collect drag events until touch slop is crossed.
     */
    class AwaitTouchSlop(
        var initialDown: PointerInputChange? = null,
        var pointerId: PointerId = PointerId(Long.MAX_VALUE),
    ) : DragDetectionState()

    /**
     * Alternative state that implements the gesture pick up feature. If a draggable loses an event
     * because someone else consumed it, it can still pick it up later if the consumer "gives up" on
     * that gesture. Once a gesture is lost the draggable will pass on to this state until all
     * fingers are up.
     */
    class AwaitGesturePickup(
        var initialDown: PointerInputChange? = null,
        var pointerId: PointerId = PointerId(Long.MAX_VALUE),
        var touchSlopDetector: TouchSlopDetector? = null,
    ) : DragDetectionState()

    /** State where dragging is happening. */
    class Dragging(var pointerId: PointerId = PointerId(Long.MAX_VALUE)) : DragDetectionState()
}

// TODO(levima) Remove once ExperimentalIndirectTouchTypeApi stable b/426155641
@OptIn(ExperimentalIndirectTouchTypeApi::class)
private class IndirectTouchEventProcessor(
    val startGestureTrigger: () -> Unit,
    val onDragEvent: (DragEvent) -> Unit,
) {
    private var velocityTracker: VelocityTracker? = null
    private var hasCrossedTouchSlop = false
    private var previousIndirectTouchPosition = Offset.Zero
    private var positionAccumulator = Offset.Zero
    private var startEventPosition = Offset.Zero
    private var touchInputEventSmoother = TouchInputEventSmoother()

    private fun requireVelocityTracker() =
        requireNotNull(velocityTracker) { "VelocityTracker was not initialized." }

    fun processIndirectTouchEvent(
        event: IndirectTouchEvent,
        orientation: Orientation,
        viewConfiguration: ViewConfiguration,
    ): Boolean {
        if (velocityTracker == null) velocityTracker = VelocityTracker()
        // Reduce noise and account for primary axis
        val smoothedEventPosition = touchInputEventSmoother.smoothEventPosition(event, orientation)

        return when (event.type) {
            IndirectTouchEventType.Press -> {
                resetProcessor()
                requireVelocityTracker().addPosition(event.uptimeMillis, smoothedEventPosition)
                previousIndirectTouchPosition = smoothedEventPosition
                startEventPosition = smoothedEventPosition
                false // just saved the press, but didn't consume.
            }

            IndirectTouchEventType.Move -> {
                var delta = smoothedEventPosition - previousIndirectTouchPosition
                var consumed = false
                positionAccumulator += delta

                /** Haven't crossed the slop yet but just crossed it. */
                if (
                    !hasCrossedTouchSlop &&
                        abs(positionAccumulator.toFloat(orientation)) > viewConfiguration.touchSlop
                ) {
                    hasCrossedTouchSlop = true
                    startGestureTrigger.invoke() // signals the start of a drag cycle
                    val postSlopDelta =
                        (abs(positionAccumulator.toFloat(orientation)) -
                            viewConfiguration.touchSlop) *
                            positionAccumulator.toFloat(orientation).sign
                    delta =
                        if (orientation == Orientation.Horizontal) Offset(x = postSlopDelta, y = 0f)
                        else Offset(x = 0f, y = postSlopDelta)
                    onDragEvent(DragStarted(startEventPosition))
                    consumed = true
                }

                /** Have crossed the slop and the delta is large enough to trigger a drag event. */
                if (
                    hasCrossedTouchSlop &&
                        delta.toFloat(orientation).absoluteValue > PixelSensitivity
                ) {
                    requireVelocityTracker().addPosition(event.uptimeMillis, smoothedEventPosition)
                    consumed = true // regular move, consume it
                    onDragEvent(DragDelta(delta, isIndirectTouchEvent = true))
                }
                previousIndirectTouchPosition = smoothedEventPosition
                consumed
            }
            IndirectTouchEventType.Release -> {
                val consumed =
                    if (hasCrossedTouchSlop) {
                        val maxVelocity = viewConfiguration.maximumFlingVelocity
                        val event =
                            DragStopped(
                                requireVelocityTracker()
                                    .calculateVelocity(Velocity(maxVelocity, maxVelocity)),
                                isIndirectTouchEvent = true,
                            )
                        onDragEvent(event)
                        true // gesture finished, consume it
                    } else {
                        false
                    }
                resetProcessor()
                consumed
            }
            else -> {
                onDragEvent(DragCancelled)
                resetProcessor()
                false
            }
        }
    }

    fun resetProcessor() {
        velocityTracker?.resetTracking()
        hasCrossedTouchSlop = false
        previousIndirectTouchPosition = Offset.Zero
        positionAccumulator = Offset.Zero
        startEventPosition = Offset.Zero
    }

    /**
     * TODO(levima): Remove this once b/413645371 lands and events are dispatched less frequently.
     */
    companion object {
        private const val PixelSensitivity = 2
    }
}

// TODO(levima) Remove once ExperimentalIndirectTouchTypeApi stable b/426155641
/**
 * Smoothes touch input events that are too frequent and noisy
 *
 * TODO(levima): Remove this once b/413645371 lands and events are dispatched less frequently.
 */
@OptIn(ExperimentalIndirectTouchTypeApi::class)
internal class TouchInputEventSmoother() {
    private var rotatingIndex = 0
    private var rotatingArray = mutableListOf<IndirectTouchEvent>()

    /**
     * Smooths [event]'s position and additionally locks it to the provided [orientation] if a
     * [IndirectTouchEventPrimaryDirectionalMotionAxis] is defined.
     */
    fun smoothEventPosition(event: IndirectTouchEvent, orientation: Orientation?): Offset {
        val primaryAxisPosition = event.primaryAxisPosition(orientation)

        var xPosition = primaryAxisPosition.x
        var yPosition = primaryAxisPosition.y

        if (event.type == IndirectTouchEventType.Press) {
            rotatingIndex = 0
            rotatingArray.clear()
        }

        if (event.type == IndirectTouchEventType.Move) {
            if (rotatingArray.size == SmoothingFactor) {
                rotatingArray[rotatingIndex] = event
            } else {
                rotatingArray.add(event)
            }

            if (rotatingIndex == SmoothingFactor) {
                rotatingIndex = 0
            }
            xPosition =
                rotatingArray.fastMap { it.primaryAxisPosition(orientation).x }.average().toFloat()
            yPosition =
                rotatingArray.fastMap { it.primaryAxisPosition(orientation).y }.average().toFloat()
        }

        return Offset(xPosition, yPosition)
    }

    /**
     * Returns a modified position for this [IndirectTouchEvent] accounting for
     * [IndirectTouchEvent.primaryDirectionalMotionAxis]. When we no longer need to smooth
     * positions, we should instead only use the primary axis to resolve delta changes, as changing
     * the entire event in this way will affect the start position we report to onDragStarted. Until
     * we can remove smoothing logic, it's complicated to manage primary axis as well as smoothed
     * positions, so we just make the change here for simplicity.
     */
    private fun IndirectTouchEvent.primaryAxisPosition(orientation: Orientation?): Offset {
        if (orientation == null) return position
        val delta =
            when (primaryDirectionalMotionAxis) {
                IndirectTouchEventPrimaryDirectionalMotionAxis.X -> position.x
                IndirectTouchEventPrimaryDirectionalMotionAxis.Y -> position.y
                // No primary axis, so don't change the offset
                else -> return position
            }
        return if (orientation == Orientation.Horizontal) {
            Offset(x = delta, y = 0f)
        } else {
            Offset(x = 0f, y = delta)
        }
    }

    /**
     * TODO(levima): Remove this once b/413645371 lands and events are dispatched less frequently.
     */
    companion object {
        private const val SmoothingFactor = 3
    }
}
