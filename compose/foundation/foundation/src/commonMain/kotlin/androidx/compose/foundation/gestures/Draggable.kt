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

import androidx.compose.foundation.ComposeFoundationFlags.isDelayPressesUsingGestureConsumptionEnabled
import androidx.compose.foundation.ComposeFoundationFlags.isNestedDraggablesTouchConflictFixEnabled
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.GestureConnection
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestureNode
import androidx.compose.foundation.gestures.DragEvent.DragCancelled
import androidx.compose.foundation.gestures.DragEvent.DragDelta
import androidx.compose.foundation.gestures.DragEvent.DragStarted
import androidx.compose.foundation.gestures.DragEvent.DragStopped
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.internal.JvmDefaultWithCompatibility
import androidx.compose.foundation.parentGestureConnection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.input.indirect.IndirectPointerEvent
import androidx.compose.ui.input.indirect.IndirectPointerInputChange
import androidx.compose.ui.input.indirect.IndirectPointerInputModifierNode
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import kotlin.coroutines.cancellation.CancellationException
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
        val CanDrag: (PointerType) -> Boolean = { true }
    }
}

internal class DraggableNode(
    private var state: DraggableState,
    canDrag: (PointerType) -> Boolean,
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
        canDrag: (PointerType) -> Boolean,
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

/** A node that performs drag gesture recognition and event propagation. */
@OptIn(ExperimentalFoundationApi::class)
internal abstract class DragGestureNode(
    canDrag: (PointerType) -> Boolean,
    enabled: Boolean,
    interactionSource: MutableInteractionSource?,
    var orientationLock: Orientation?,
) :
    DelegatingNode(),
    PointerInputModifierNode,
    IndirectPointerInputModifierNode,
    CompositionLocalConsumerModifierNode,
    GestureConnection {

    var canDrag = canDrag
        private set

    protected var enabled = enabled
        private set

    protected var interactionSource = interactionSource
        private set

    private var gestureNode: DelegatableNode? = null

    // Use wrapper lambdas here to make sure that if these properties are updated while we suspend,
    // we point to the new reference when we invoke them. startDragImmediately is a lambda since we
    // need the most recent value passed to it from Scrollable.
    private val _canDrag: (PointerType) -> Boolean = { this.canDrag(it) }
    private var channel: Channel<DragEvent>? = null
    private var dragInteraction: DragInteraction.Start? = null
    internal var isListeningForEvents = false
    internal var isListeningForPointerInputEvents = false

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
    private var indirectPointerInputDragCycleDetector: IndirectPointerInputDragCycleDetector? = null

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

    override fun onDetach() {
        isListeningForEvents = false
        disposeInteractionSource()
        nodeOffset = Offset.Zero

        gestureNode?.let { undelegate(it) }
        gestureNode = null
    }

    protected fun initializeGestureCoordination() {
        if (!isDelayPressesUsingGestureConsumptionEnabled) return
        if (gestureNode == null) {
            gestureNode = delegate(gestureNode(this))
        }
    }

    override fun isInterested(event: IndirectPointerInputChange): Boolean {
        // for now, if this is a down event it may become a drag so we're
        // interested.
        return event.changedToDownIgnoreConsumed() && enabled
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        isListeningForPointerInputEvents = true
        initializeGestureCoordination()
        if (enabled) {
            // initialize current state
            if (currentDragState == null) currentDragState = awaitDownState
            processRawPointerEvent(pointerEvent, pass)
        }
    }

    override fun onIndirectPointerEvent(event: IndirectPointerEvent, pass: PointerEventPass) {
        initializeGestureCoordination()
        if (enabled) {
            if (indirectPointerInputDragCycleDetector == null) {
                indirectPointerInputDragCycleDetector = IndirectPointerInputDragCycleDetector(this)
            }
            indirectPointerInputDragCycleDetector?.processIndirectPointerInputEvent(event, pass)
        }
    }

    override fun onCancelIndirectPointerInput() {
        indirectPointerInputDragCycleDetector?.resetDragDetectionState()
    }

    /**
     * Draggable containers will be interested in the following events:
     * 1) DOWN events. They may become a drag gesture later.
     * 2) The touch slop trigger event if the preceding deltas form an angle of interest. The touch
     *    slop trigger event is when, effectively, draggables will start consuming. So at this
     *    point, we look at the collected deltas since the first down event, and we decide if we're
     *    interested based on the angle that those deltas form. We will favor vertical drags over
     *    horizontal drags more because UX-wise there's more freedom and uncertainty when a user
     *    performs a vertical gesture vs. a horizontal gesture.
     */
    override fun isInterested(event: PointerInputChange): Boolean {
        if (event.changedToDownIgnoreConsumed()) return enabled
        if (!isNestedDraggablesTouchConflictFixEnabled) return false
        if (event.changedToUpIgnoreConsumed()) return false

        if (touchSlopDetector == null) {
            touchSlopDetector = TouchSlopDetector(orientationLock)
        }

        val touchSlop = currentValueOf(LocalViewConfiguration).touchSlop
        val positionChange = event.positionChange()

        return with(requireTouchSlopDetector()) {
            getPostSlopOffset(positionChange, touchSlop, false) != Offset.Unspecified &&
                isDeltaAtAngleOfInterest(positionChange)
        }
    }

    override fun onCancelPointerInput() {
        if (isListeningForPointerInputEvents) resetDragDetectionState()
        isListeningForPointerInputEvents = false
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
        onDragStopped(DragStopped(Velocity.Zero, isIndirectPointerEvent = false))
    }

    fun disposeInteractionSource() {
        dragInteraction?.let { interaction ->
            interactionSource?.tryEmit(DragInteraction.Cancel(interaction))
            dragInteraction = null
        }
    }

    fun update(
        canDrag: (PointerType) -> Boolean = this.canDrag,
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
                indirectPointerInputDragCycleDetector = null
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
            if (isListeningForPointerInputEvents) resetDragDetectionState()
            indirectPointerInputDragCycleDetector?.resetDragDetectionState()
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
        verifyConsumptionInFinalPass: Boolean = false,
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
                this.verifyConsumptionInFinalPass = verifyConsumptionInFinalPass
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
        /** Slop detection only cares about the main and final passes */
        if (pass == PointerEventPass.Initial) return
        val eventFromPointerId = pointerEvent.changes.fastFirstOrNull { it.id == state.pointerId }

        /**
         * We lost this pointer, try to replace it. This is to cover the case where multiple
         * pointers were down, but the original one we tracked (state.pointerId) is no longer down,
         * try to move tracking to a different pointer
         */
        val dragEvent =
            if (eventFromPointerId == null) {
                val otherDown = pointerEvent.changes.fastFirstOrNull { it.pressed }
                if (otherDown == null) {
                    // There are no other pointers down, reset the state
                    moveToAwaitDownState()
                    return
                } else {
                    // a new pointer was found, update the current state.
                    state.pointerId = otherDown.id
                }
                otherDown
            } else {
                eventFromPointerId
            }

        /**
         * Slop detection routines happens during the Main pass. Do we have unconsumed events for
         * this pointer?
         */
        if (pass == PointerEventPass.Main) {
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
                    val touchSlop =
                        currentValueOf(LocalViewConfiguration).pointerSlop(dragEvent.type)

                    // add data to the slop detector
                    val postSlopOffset =
                        requireTouchSlopDetector()
                            .getPostSlopOffset(dragEvent.positionChangeIgnoreConsumed(), touchSlop)

                    /**
                     * Here we use the [gestureNode] and [GestureConnection] APIs to make a
                     * decision. About this gesture. At this point we have all the triggers to start
                     * a recognizing a gesture in this current
                     * [androidx.compose.foundation.gestures.DragGestureNode]. This is the moment
                     * that touch slop is recognized here in this node. During this time, before we
                     * start consuming drag events we check the interested of the parent and our
                     * self-interest. If the parent is interested and we're not (for this specific
                     * event), we will give the parent a chance to do something by postponing the
                     * remaining consumption to the final pass.
                     */
                    if (isNestedDraggablesTouchConflictFixEnabled) {
                        if (postSlopOffset.isSpecified) {
                            val isSelfInterested = isInterested(dragEvent)
                            val isParentInterested =
                                parentGestureConnection?.isInterested(dragEvent) == true
                            if (!isSelfInterested && isParentInterested) {
                                state.verifyConsumptionInFinalPass = true
                            } else {
                                dragEvent.consume()
                                sendDragStart(state.initialDown!!, dragEvent, postSlopOffset)
                                sendDragEvent(dragEvent, postSlopOffset)
                                moveToDraggingState(dragEvent.id)
                            }
                        } else {
                            state.verifyConsumptionInFinalPass = true
                        }
                    } else {
                        if (postSlopOffset.isSpecified) {
                            dragEvent.consume()
                            sendDragStart(state.initialDown!!, dragEvent, postSlopOffset)
                            sendDragEvent(dragEvent, postSlopOffset)
                            moveToDraggingState(dragEvent.id)
                        } else {
                            state.verifyConsumptionInFinalPass = true
                        }
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

        /**
         * This checks 2 cases: 1) A parent consumed in the main pass and this child can only see
         * that consumption during the final pass. 2) The parent actually consumed during the final
         * pass.
         */
        if (pass == PointerEventPass.Final && state.verifyConsumptionInFinalPass) {
            if (dragEvent.isConsumed) {
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
            } else {
                /**
                 * Self and nobody consumed dragEvent. We will only get here if self didn't consume
                 * in the main pass OR if self wasn't interested during the main pass. In this case
                 * we remain in the awaitTouchSlop state and wait for more information (events).
                 */
                state.verifyConsumptionInFinalPass = false
            }
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
        if (canDrag(down.type)) {
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
        isListeningForPointerInputEvents = false
    }

    private fun sendDragCancelled() {
        requireChannel().trySend(DragCancelled)
    }

    fun onDragEvent(event: DragEvent) {
        if (event is DragStarted && !isListeningForEvents) {
            isListeningForEvents = true
            startListeningForEvents()
        }
        requireChannel().trySend(event)
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

    class DragStopped(val velocity: Velocity, val isIndirectPointerEvent: Boolean) : DragEvent()

    object DragCancelled : DragEvent()

    class DragDelta(val delta: Offset, val isIndirectPointerEvent: Boolean) : DragEvent()
}

internal fun Offset.toFloat(orientation: Orientation) =
    if (orientation == Orientation.Vertical) this.y else this.x

private fun Velocity.toFloat(orientation: Orientation) =
    if (orientation == Orientation.Vertical) this.y else this.x

internal fun Velocity.toValidVelocity() =
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
        var verifyConsumptionInFinalPass: Boolean = false,
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
