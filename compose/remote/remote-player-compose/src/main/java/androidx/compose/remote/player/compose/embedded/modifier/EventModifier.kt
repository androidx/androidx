/*
 * Copyright 2026 The Android Open Source Project
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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded.modifier

import androidx.collection.IntSet
import androidx.collection.MutableIntSet
import androidx.collection.MutableLongIntMap
import androidx.collection.MutableLongList
import androidx.collection.MutableLongObjectMap
import androidx.collection.MutableLongSet
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.WireBuffer
import androidx.compose.remote.core.events.Event
import androidx.compose.remote.core.events.EventManager
import androidx.compose.remote.core.input.MetadataUtil
import androidx.compose.remote.core.input.PointerInputEvent
import androidx.compose.remote.core.input.PointerInputRouter
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.Theme
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.modifiers.EventHandlerModifierOperation
import androidx.compose.remote.player.compose.embedded.LocalCoreDocument
import androidx.compose.remote.player.compose.embedded.LocalGraphContext
import androidx.compose.remote.player.compose.embedded.LocalRcIsMeasureProbe
import androidx.compose.remote.player.compose.embedded.LocalRemoteActionHandler
import androidx.compose.remote.player.compose.embedded.LocalRemoteContext
import androidx.compose.remote.player.compose.embedded.LocalRemoteNamedActionHandler
import androidx.compose.remote.player.compose.embedded.doc.GraphContext
import androidx.compose.remote.player.compose.embedded.doc.getOperationsList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach

internal class RcEvent(
    private val type: Int,
    private val metadata: Int,
    private val data: FloatArray?,
) : Event {
    override fun getType(): Int = type

    override fun getMetadata(): Int = metadata

    override fun getData(): FloatArray? = data
}

/**
 * The key input event constants the embedded player dispatches to [EventHandlerModifierOperation]s,
 * and how Compose key input maps onto them.
 *
 * `remote-core` defines pointer input types in [PointerInputRouter], [PointerInputEvent], and
 * [MetadataUtil], but has no key event types or router yet. The embedded player attaches each
 * component's handlers as Compose input modifiers rather than routing through core's
 * `EventManager`, because Compose already handles key delivery to the focused node and pointer hit
 * testing / per-pointer streams.
 *
 * These key constants are provisional until they move into `remote-core` alongside a key router for
 * the View player.
 */
internal object RcPlayerEvents {
    /** Key events, as proposed in go/rc-eventhandler. */
    const val EVENT_TYPE_KEY: Int = 2

    /**
     * The tunneling pass, from the root down to the target (`onPreviewKeyEvent` for keys). Pass and
     * subtype values follow the event data layout in go/rc-eventhandler.
     */
    const val PASS_TUNNELING: Int = 1

    /** The bubbling pass, from the target up to the root (`onKeyEvent` for keys). */
    const val PASS_BUBBLING: Int = 2

    /** A key was pressed, including key repeats. */
    const val KEY_SUBTYPE_DOWN: Int = 1

    /** A key was released. */
    const val KEY_SUBTYPE_UP: Int = 2

    /** Handler flag: consume a key event and stop propagation. */
    const val STATUS_CONSUMED: Int = 1

    /** Packs a key pass and subtype into an event metadata (and handler filter) value. */
    fun metadata(pass: Int, subtype: Int): Int = (pass shl 8) or subtype

    /** Returns the pass packed into a key event metadata (or handler filter) value. */
    fun passOf(metadata: Int): Int = metadata shr 8
}

// Match EventHandlerModifierOperation.CommonFlagsUtil until aosp/4337994 lands.
private const val EVENT_HANDLER_FLAG_UNCONDITIONAL = 1
private const val EVENT_HANDLER_FLAG_NO_DATA = 1 shl 1

/**
 * Decoded wire fields of an [EventHandlerModifierOperation] alongside the [op] whose nested actions
 * run.
 *
 * TODO: Replace wire re-parsing with generated `EventHandlerModifierOperationNode` once
 *   aosp/4337994 lands.
 */
internal class ParsedEventHandler(
    val op: EventHandlerModifierOperation,
    val type: Int,
    val filter: Int,
    val flags: Int,
    val dataIds: IntArray?,
    val condition: FloatArray?,
) {
    /**
     * Reusable [FloatExpression] for evaluating [condition] without allocating per event;
     * `FloatExpression.updateVariables` resolves variables into `mPreCalcValue` without mutating
     * `mSrcValue`.
     */
    val conditionExpression: FloatExpression? = condition?.let { FloatExpression(-1, it, null) }
}

/**
 * Extracts [EventHandlerModifierOperation]'s wire fields by re-parsing `op.write(WireBuffer())`.
 *
 * TODO: Replace wire re-parsing with generated `EventHandlerModifierOperationNode` once
 *   aosp/4337994 lands.
 */
internal fun EventHandlerModifierOperation.parseEventHandler(): ParsedEventHandler {
    val buffer = WireBuffer(256)
    write(buffer)
    buffer.setIndex(0)
    val opcode = buffer.readOperationType()
    require(opcode == Operations.MODIFIER_EVENT_HANDLER) {
        "Unexpected opcode for EventHandlerModifierOperation: $opcode"
    }
    val version = buffer.readInt()
    require(version == 0) { "Unsupported EventHandlerModifierOperation version: $version" }
    val type = buffer.readInt()
    val filter = buffer.readInt()
    val commonFlags = buffer.readShort()
    val flags = buffer.readShort()
    val dataIds =
        if ((commonFlags and EVENT_HANDLER_FLAG_NO_DATA) == 0) {
            val dataLen = buffer.readInt()
            IntArray(dataLen) { buffer.readInt() }
        } else {
            null
        }
    val condition =
        if ((commonFlags and EVENT_HANDLER_FLAG_UNCONDITIONAL) == 0) {
            val len = buffer.readInt()
            FloatArray(len) { buffer.readNanId() }
        } else {
            null
        }
    return ParsedEventHandler(
        op = this,
        type = type,
        filter = filter,
        flags = flags,
        dataIds = dataIds,
        condition = condition,
    )
}

/**
 * Runs [handlers] that match [event], outer-to-inner for tunneling/final passes and inner-to-outer
 * for bubbling (`POINTER_PASS_MAIN` / `PASS_BUBBLING`) passes, the same way
 * `PointerInputRouter.dispatchToModifiers` and `EventHandlerModifierOperation.onEvent` do: the
 * handler's filter must equal the event metadata, the event data is written to the handler's data
 * ids, and the nested actions run if the handler's condition holds. The nested actions run through
 * the player's action dispatch, so host actions reach the `RcPlayer` callbacks.
 *
 * For pointer events, consumption marks the event consumed so later handlers in the pass observe
 * `consumed = 1`. For key events, consumption stops dispatching to subsequent handlers on this
 * component in the same pass.
 *
 * @return the bitwise OR of the flags of every handler whose actions ran, or
 *   [EventManager.STATUS_UNHANDLED] if none did.
 */
internal fun dispatchEventActions(
    handlers: List<ParsedEventHandler>,
    event: Event,
    coreDocument: CoreDocument,
    remoteContext: RemoteContext,
    onAction: (Int, String?) -> Unit,
    onNamedAction: (String, Any?) -> Unit,
    graphContext: GraphContext? = null,
): Int {
    val isBubbling =
        when (event.type) {
            PointerInputRouter.EVENT_TYPE_POINTER ->
                MetadataUtil.unpackPass(event.metadata) == PointerInputRouter.POINTER_PASS_MAIN
            RcPlayerEvents.EVENT_TYPE_KEY ->
                RcPlayerEvents.passOf(event.metadata) == RcPlayerEvents.PASS_BUBBLING
            else -> false
        }
    val evalContext = graphContext ?: remoteContext
    var status = EventManager.STATUS_UNHANDLED
    var wroteData = false
    var ranActions = false
    for (i in handlers.indices) {
        val handler = handlers[if (isBubbling) handlers.lastIndex - i else i]
        if (handler.type != event.type || handler.filter != event.metadata) continue
        val dataIds = handler.dataIds
        val data = event.data
        var wroteHandlerData = false
        if (dataIds != null && data != null) {
            for (j in 0 until minOf(dataIds.size, data.size)) {
                // An id of 0 means the creator doesn't want this field.
                if (dataIds[j] != 0) {
                    remoteContext.overrideFloat(dataIds[j], data[j])
                    wroteHandlerData = true
                    wroteData = true
                }
            }
        }
        val conditionExpr = handler.conditionExpression
        if (conditionExpr != null) {
            if (wroteHandlerData && graphContext == null) {
                coreDocument.updateVariables(
                    remoteContext,
                    Theme.SYSTEM,
                    coreDocument.getOperationsList(),
                )
            }
            if (conditionExpr.evaluate(evalContext) == 0f) {
                continue
            }
        }
        handler.op.list.fastForEach { action ->
            applyClickAction(
                action = action,
                coreDocument = coreDocument,
                remoteContext = remoteContext,
                graphContext = graphContext,
                onAction = onAction,
                onNamedAction = onNamedAction,
            )
        }
        ranActions = true
        status = status or handler.flags
        if (
            event is PointerInputEvent &&
                (handler.flags and PointerInputRouter.POINTER_STATUS_CONSUMED) != 0
        ) {
            event.markConsumed()
        }
        if (
            event.type == RcPlayerEvents.EVENT_TYPE_KEY &&
                (handler.flags and RcPlayerEvents.STATUS_CONSUMED) != 0
        ) {
            break
        }
    }
    if (ranActions || wroteData) {
        coreDocument.updateVariables(
            remoteContext,
            Theme.SYSTEM,
            coreDocument.getOperationsList(),
        )
    }
    return status
}

/**
 * Attaches [component]'s key [EventHandlerModifierOperation]s as Compose key input modifiers.
 *
 * Compose delivers key events to the focused node, so the handlers run when focus is on, or within,
 * this component: tunneling-pass handlers from `onPreviewKeyEvent` (root to focused node) and
 * bubbling-pass handlers from `onKeyEvent` (focused node to root). Place this outside the
 * component's other modifiers so it is an ancestor of any focus target they add (e.g. a clickable).
 *
 * Key downs (including repeats) and key ups are dispatched with the pass and subtype as the
 * metadata, and `[keyCode, metaState, repeatCount]` as the data, where the key code is the Android
 * key code. A handler matches a specific key with a condition on the key code it receives through
 * its data ids. A handler returning [RcPlayerEvents.STATUS_CONSUMED] consumes the key event.
 *
 * TODO: `RemoteComposeView` only dispatches key downs, in a single pass with the key code as the
 *   metadata. It should dispatch both passes and key ups in this layout so documents behave the
 *   same in both players.
 */
@Composable
internal fun Modifier.keyEventActions(component: LayoutComponent): Modifier {
    if (LocalRcIsMeasureProbe.current) return this
    val handlers = rememberEventActions(component, RcPlayerEvents.EVENT_TYPE_KEY)
    if (handlers.isEmpty()) return this
    val dispatch by rememberEventDispatcher(handlers)
    var modifier = this
    if (handlers.fastAny { RcPlayerEvents.passOf(it.filter) == RcPlayerEvents.PASS_TUNNELING }) {
        modifier = modifier.onPreviewKeyEvent {
            dispatchKeyEvent(it, RcPlayerEvents.PASS_TUNNELING, dispatch)
        }
    }
    if (handlers.fastAny { RcPlayerEvents.passOf(it.filter) == RcPlayerEvents.PASS_BUBBLING }) {
        modifier = modifier.onKeyEvent {
            dispatchKeyEvent(it, RcPlayerEvents.PASS_BUBBLING, dispatch)
        }
    }
    return modifier
}

private fun dispatchKeyEvent(keyEvent: KeyEvent, pass: Int, dispatch: (Event) -> Int): Boolean {
    val subtype =
        when (keyEvent.type) {
            KeyEventType.KeyDown -> RcPlayerEvents.KEY_SUBTYPE_DOWN
            KeyEventType.KeyUp -> RcPlayerEvents.KEY_SUBTYPE_UP
            else -> return false
        }
    val nativeEvent = keyEvent.nativeKeyEvent
    val status =
        dispatch(
            RcEvent(
                RcPlayerEvents.EVENT_TYPE_KEY,
                RcPlayerEvents.metadata(pass, subtype),
                floatArrayOf(
                    keyEvent.key.nativeKeyCode.toFloat(),
                    nativeEvent.metaState.toFloat(),
                    nativeEvent.repeatCount.toFloat(),
                ),
            )
        )
    return status and RcPlayerEvents.STATUS_CONSUMED != 0
}

/**
 * Attaches [component]'s pointer [EventHandlerModifierOperation]s as a Compose
 * [PointerInputModifierNode] matching [PointerInputRouter]'s wire contract
 * ([PointerInputRouter.EVENT_TYPE_POINTER], [MetadataUtil]-packed metadata, and `[pointerId, x, y,
 * velocityX, velocityY]` data with `pointerId` mapped into `0..31` active slots).
 *
 * Compose hit tests pointer downs and keeps delivering each pointer's stream to the components it
 * went down in, so the handlers see the pointers that went down within this component. Placed
 * outside the component's own modifiers so hit testing covers its outer bounds and `(x, y)` is
 * relative to those outer bounds (matching `PointerInputRouter.convertToLocalCoordinates`).
 *
 * Each change is dispatched in the [PointerInputRouter.POINTER_PASS_INITIAL]
 * ([PointerEventPass.Initial]), [PointerInputRouter.POINTER_PASS_MAIN] ([PointerEventPass.Main]),
 * and [PointerInputRouter.POINTER_PASS_FINAL] ([PointerEventPass.Final]) passes. Only the passes
 * that have handlers are dispatched. A handler returning
 * [PointerInputRouter.POINTER_STATUS_CONSUMED] consumes the change (except in `FINAL`) and marks
 * later handlers in the pass as seeing it consumed. A `MAIN`-pass cancel is dispatched when a
 * descendant or ancestor consumes a tracked pointer (even without movement) or when a tracked
 * pointer disappears from the event stream; `MotionEvent.ACTION_CANCEL` and node detach cancel
 * tracked pointers through [PointerInputModifierNode.onCancelPointerInput] without synthesizing a
 * release (`UP`) event.
 */
@Composable
internal fun Modifier.pointerEventActions(component: LayoutComponent): Modifier {
    if (LocalRcIsMeasureProbe.current) return this
    val handlers = rememberEventActions(component, PointerInputRouter.EVENT_TYPE_POINTER)
    if (handlers.isEmpty()) return this
    val dispatchState = rememberEventDispatcher(handlers)
    val passes =
        remember(handlers) {
            MutableIntSet().apply {
                handlers.fastForEach { add(MetadataUtil.unpackPass(it.filter)) }
            }
        }
    return this then PointerEventActionsElement(component, passes, dispatchState)
}

private class PointerEventActionsElement(
    private val component: LayoutComponent,
    private val passes: IntSet,
    private val dispatchState: State<(Event) -> Int>,
) : ModifierNodeElement<PointerEventActionsNode>() {
    override fun create(): PointerEventActionsNode =
        PointerEventActionsNode(component, passes, dispatchState)

    override fun update(node: PointerEventActionsNode) {
        if (node.component !== component) {
            node.cancelAllTrackedPointers()
            node.component = component
        }
        node.passes = passes
        node.dispatchState = dispatchState
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "pointerEventActions"
        properties["componentId"] = component.getId()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PointerEventActionsElement) return false
        return component === other.component &&
            passes == other.passes &&
            dispatchState == other.dispatchState
    }

    override fun hashCode(): Int {
        var result = System.identityHashCode(component)
        result = 31 * result + passes.hashCode()
        result = 31 * result + dispatchState.hashCode()
        return result
    }
}

private class PointerEventActionsNode(
    var component: LayoutComponent,
    var passes: IntSet,
    var dispatchState: State<(Event) -> Int>,
) : Modifier.Node(), PointerInputModifierNode {
    // Pointers this component saw go down unconsumed externally and hasn't released/cancelled.
    private val trackedPointers = MutableLongObjectMap<TrackedPointerState>()

    // Per-pointer velocity trackers updated across the stream and read on UP.
    private val velocityEntries = MutableLongObjectMap<PointerVelocityEntry>()

    // Active Compose pointer IDs mapped to reusable 0..31 slot IDs (matching PointerInputRouter).
    private val pointerSlots = MutableLongIntMap()
    private var usedSlotMask = 0

    // Per-event transient sets cleared at the start of Initial.
    private val consumedBySelfInInitial = MutableLongSet()
    private val consumedBySelfInMain = MutableLongSet()
    private val downInCurrentEvent = MutableLongSet()
    private val upInCurrentEvent = MutableLongSet()
    private val seenInMain = MutableLongSet()

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        val dispatch = dispatchState.value
        when (pass) {
            PointerEventPass.Initial -> handleInitialPass(pointerEvent, dispatch)
            PointerEventPass.Main -> handleMainPass(pointerEvent, dispatch)
            PointerEventPass.Final -> handleFinalPass(pointerEvent, dispatch)
        }
    }

    override fun onCancelPointerInput() {
        cancelAllTrackedPointers()
    }

    override fun onDetach() {
        cancelAllTrackedPointers()
    }

    private fun slotFor(pointerId: Long): Int {
        val existing = pointerSlots.getOrDefault(pointerId, -1)
        if (existing >= 0) return existing
        val freeBits = usedSlotMask.inv()
        val slot =
            if (freeBits != 0) {
                Integer.numberOfTrailingZeros(freeBits)
            } else {
                (pointerId and 31L).toInt()
            }
        usedSlotMask = usedSlotMask or (1 shl slot)
        pointerSlots[pointerId] = slot
        return slot
    }

    private fun releasePointerResources(pointerId: Long) {
        velocityEntries.remove(pointerId)
        val slot = pointerSlots.getOrDefault(pointerId, -1)
        if (slot >= 0) {
            pointerSlots.remove(pointerId)
            usedSlotMask = usedSlotMask and (1 shl slot).inv()
        }
    }

    private fun handleInitialPass(pointerEvent: PointerEvent, dispatch: (Event) -> Int) {
        consumedBySelfInInitial.clear()
        consumedBySelfInMain.clear()
        downInCurrentEvent.clear()
        upInCurrentEvent.clear()

        pointerEvent.changes.fastForEach { change ->
            val pointerId = change.id.value
            val isDown = change.changedToDownIgnoreConsumed()
            val isUp = change.changedToUpIgnoreConsumed()
            if (isDown) {
                downInCurrentEvent.add(pointerId)
                val slot = slotFor(pointerId)
                val entry = velocityEntries.getOrPut(pointerId) { PointerVelocityEntry() }
                entry.tracker.resetTracking()
                entry.releaseVelocityX = 0f
                entry.releaseVelocityY = 0f
                entry.tracker.addPointerInputChange(change)

                if (PointerInputRouter.POINTER_PASS_INITIAL in passes) {
                    val wasConsumedBefore = change.isConsumed
                    val status =
                        dispatchPointerChange(
                            pass = PointerInputRouter.POINTER_PASS_INITIAL,
                            subtype = PointerInputRouter.POINTER_SUBTYPE_DOWN,
                            consumed = wasConsumedBefore,
                            pointerSlot = slot,
                            x = change.position.x,
                            y = change.position.y,
                            velocityX = 0f,
                            velocityY = 0f,
                            dispatch = dispatch,
                        )
                    if (status and PointerInputRouter.POINTER_STATUS_CONSUMED != 0) {
                        change.consume()
                        if (!wasConsumedBefore) {
                            consumedBySelfInInitial.add(pointerId)
                        }
                    }
                }
                return@fastForEach
            }

            val state = trackedPointers[pointerId] ?: return@fastForEach
            if (isUp) {
                val entry = velocityEntries[pointerId]
                if (entry != null) {
                    entry.tracker.addPointerInputChange(change)
                    val velocity = entry.tracker.calculateVelocity()
                    entry.releaseVelocityX = velocity.x
                    entry.releaseVelocityY = velocity.y
                }
            } else if (change.pressed) {
                velocityEntries[pointerId]?.tracker?.addPointerInputChange(change)
            }

            if (PointerInputRouter.POINTER_PASS_INITIAL !in passes) return@fastForEach
            val subtype =
                when {
                    isUp -> PointerInputRouter.POINTER_SUBTYPE_UP
                    change.pressed && change.positionChangeIgnoreConsumed() != Offset.Zero ->
                        PointerInputRouter.POINTER_SUBTYPE_MOVE
                    else -> return@fastForEach
                }
            val vx: Float
            val vy: Float
            if (subtype == PointerInputRouter.POINTER_SUBTYPE_UP) {
                val entry = velocityEntries[pointerId]
                vx = entry?.releaseVelocityX ?: 0f
                vy = entry?.releaseVelocityY ?: 0f
            } else {
                vx = 0f
                vy = 0f
            }
            val wasConsumedBefore = change.isConsumed
            val status =
                dispatchPointerChange(
                    pass = PointerInputRouter.POINTER_PASS_INITIAL,
                    subtype = subtype,
                    consumed = wasConsumedBefore,
                    pointerSlot = state.slot,
                    x = change.position.x,
                    y = change.position.y,
                    velocityX = vx,
                    velocityY = vy,
                    dispatch = dispatch,
                )
            if (status and PointerInputRouter.POINTER_STATUS_CONSUMED != 0) {
                change.consume()
                if (!wasConsumedBefore) {
                    consumedBySelfInInitial.add(pointerId)
                }
            }
        }
    }

    private fun handleMainPass(pointerEvent: PointerEvent, dispatch: (Event) -> Int) {
        seenInMain.clear()
        val hasMainHandlers = PointerInputRouter.POINTER_PASS_MAIN in passes
        pointerEvent.changes.fastForEach { change ->
            val pointerId = change.id.value
            seenInMain.add(pointerId)
            val wasConsumedBefore = change.isConsumed
            val consumedExternally = wasConsumedBefore && pointerId !in consumedBySelfInInitial
            val isDown = change.changedToDownIgnoreConsumed()
            if (isDown) {
                val slot = slotFor(pointerId)
                val status =
                    if (hasMainHandlers) {
                        dispatchPointerChange(
                            pass = PointerInputRouter.POINTER_PASS_MAIN,
                            subtype = PointerInputRouter.POINTER_SUBTYPE_DOWN,
                            consumed = wasConsumedBefore,
                            pointerSlot = slot,
                            x = change.position.x,
                            y = change.position.y,
                            velocityX = 0f,
                            velocityY = 0f,
                            dispatch = dispatch,
                        )
                    } else {
                        EventManager.STATUS_UNHANDLED
                    }
                if (!consumedExternally) {
                    trackedPointers[pointerId] =
                        TrackedPointerState(slot, change.position.x, change.position.y)
                }
                if (status and PointerInputRouter.POINTER_STATUS_CONSUMED != 0) {
                    change.consume()
                    if (!wasConsumedBefore) {
                        consumedBySelfInMain.add(pointerId)
                    }
                }
                return@fastForEach
            }

            val state = trackedPointers[pointerId] ?: return@fastForEach
            if (consumedExternally) {
                // A descendant (in Main) or ancestor (in Initial) claimed this stream: cancel
                // immediately, even when this change had no position delta.
                trackedPointers.remove(pointerId)
                releasePointerResources(pointerId)
                if (hasMainHandlers) {
                    dispatchPointerChange(
                        pass = PointerInputRouter.POINTER_PASS_MAIN,
                        subtype = PointerInputRouter.POINTER_SUBTYPE_CANCEL,
                        consumed = false,
                        pointerSlot = state.slot,
                        x = change.position.x,
                        y = change.position.y,
                        velocityX = 0f,
                        velocityY = 0f,
                        dispatch = dispatch,
                    )
                }
                return@fastForEach
            }

            val subtype =
                when {
                    change.changedToUpIgnoreConsumed() -> PointerInputRouter.POINTER_SUBTYPE_UP
                    change.pressed && change.positionChangeIgnoreConsumed() != Offset.Zero ->
                        PointerInputRouter.POINTER_SUBTYPE_MOVE
                    else -> return@fastForEach
                }
            val vx: Float
            val vy: Float
            if (subtype == PointerInputRouter.POINTER_SUBTYPE_UP) {
                // Keep trackedPointers and velocityEntries until after Final so Final still sees UP
                // with its slot and computed velocity.
                upInCurrentEvent.add(pointerId)
                val entry = velocityEntries[pointerId]
                vx = entry?.releaseVelocityX ?: 0f
                vy = entry?.releaseVelocityY ?: 0f
            } else {
                state.lastX = change.position.x
                state.lastY = change.position.y
                vx = 0f
                vy = 0f
            }
            val status =
                if (hasMainHandlers) {
                    dispatchPointerChange(
                        pass = PointerInputRouter.POINTER_PASS_MAIN,
                        subtype = subtype,
                        consumed = wasConsumedBefore,
                        pointerSlot = state.slot,
                        x = change.position.x,
                        y = change.position.y,
                        velocityX = vx,
                        velocityY = vy,
                        dispatch = dispatch,
                    )
                } else {
                    EventManager.STATUS_UNHANDLED
                }
            if (status and PointerInputRouter.POINTER_STATUS_CONSUMED != 0) {
                change.consume()
                if (!wasConsumedBefore) {
                    consumedBySelfInMain.add(pointerId)
                }
            }
        }

        if (trackedPointers.isNotEmpty()) {
            var disappeared: MutableLongList? = null
            trackedPointers.forEachKey { id ->
                if (id !in seenInMain) {
                    if (disappeared == null) disappeared = MutableLongList(1)
                    disappeared.add(id)
                }
            }
            disappeared?.forEach { id ->
                val state = trackedPointers.remove(id)
                releasePointerResources(id)
                if (state != null && hasMainHandlers) {
                    dispatchPointerChange(
                        pass = PointerInputRouter.POINTER_PASS_MAIN,
                        subtype = PointerInputRouter.POINTER_SUBTYPE_CANCEL,
                        consumed = false,
                        pointerSlot = state.slot,
                        x = state.lastX,
                        y = state.lastY,
                        velocityX = 0f,
                        velocityY = 0f,
                        dispatch = dispatch,
                    )
                }
            }
        }
    }

    private fun handleFinalPass(pointerEvent: PointerEvent, dispatch: (Event) -> Int) {
        val hasFinalHandlers = PointerInputRouter.POINTER_PASS_FINAL in passes
        val hasMainHandlers = PointerInputRouter.POINTER_PASS_MAIN in passes
        pointerEvent.changes.fastForEach { change ->
            val pointerId = change.id.value
            val isDown = pointerId in downInCurrentEvent
            val isUp = pointerId in upInCurrentEvent
            val state = trackedPointers[pointerId]
            if (state == null && !isDown) {
                return@fastForEach
            }

            val slot = state?.slot ?: slotFor(pointerId)
            if (hasFinalHandlers) {
                val subtype =
                    when {
                        isDown -> PointerInputRouter.POINTER_SUBTYPE_DOWN
                        isUp -> PointerInputRouter.POINTER_SUBTYPE_UP
                        change.pressed && change.positionChangeIgnoreConsumed() != Offset.Zero ->
                            PointerInputRouter.POINTER_SUBTYPE_MOVE
                        else -> null
                    }
                if (subtype != null) {
                    val vx: Float
                    val vy: Float
                    if (subtype == PointerInputRouter.POINTER_SUBTYPE_UP) {
                        val entry = velocityEntries[pointerId]
                        vx = entry?.releaseVelocityX ?: 0f
                        vy = entry?.releaseVelocityY ?: 0f
                    } else {
                        vx = 0f
                        vy = 0f
                    }
                    dispatchPointerChange(
                        pass = PointerInputRouter.POINTER_PASS_FINAL,
                        subtype = subtype,
                        consumed = change.isConsumed,
                        pointerSlot = slot,
                        x = change.position.x,
                        y = change.position.y,
                        velocityX = vx,
                        velocityY = vy,
                        dispatch = dispatch,
                    )
                }
            }

            if (state != null && !isDown && !isUp && change.pressed) {
                val consumedBySelf =
                    pointerId in consumedBySelfInInitial || pointerId in consumedBySelfInMain
                if (change.isConsumed && !consumedBySelf) {
                    // An ancestor (e.g. a scrollable container) consumed this pointer on Main
                    // after our Main pass ran: stop tracking and cancel the stream.
                    trackedPointers.remove(pointerId)
                    releasePointerResources(pointerId)
                    if (hasMainHandlers) {
                        dispatchPointerChange(
                            pass = PointerInputRouter.POINTER_PASS_MAIN,
                            subtype = PointerInputRouter.POINTER_SUBTYPE_CANCEL,
                            consumed = false,
                            pointerSlot = state.slot,
                            x = change.position.x,
                            y = change.position.y,
                            velocityX = 0f,
                            velocityY = 0f,
                            dispatch = dispatch,
                        )
                    }
                    return@fastForEach
                }
            }

            if (isUp) {
                trackedPointers.remove(pointerId)
                releasePointerResources(pointerId)
            } else if (isDown && state == null) {
                // Externally-consumed DOWN was not tracked; free its slot/velocity after Final.
                releasePointerResources(pointerId)
            }
        }
    }

    fun cancelAllTrackedPointers() {
        if (trackedPointers.isEmpty()) {
            velocityEntries.clear()
            pointerSlots.clear()
            usedSlotMask = 0
            consumedBySelfInInitial.clear()
            consumedBySelfInMain.clear()
            downInCurrentEvent.clear()
            upInCurrentEvent.clear()
            seenInMain.clear()
            return
        }
        // Snapshot and clear before dispatching so a CANCEL action that detaches this node cannot
        // re-enter and double-dispatch CANCEL.
        val toCancel = ArrayList<TrackedPointerState>(trackedPointers.size)
        trackedPointers.forEachValue { toCancel.add(it) }
        trackedPointers.clear()
        velocityEntries.clear()
        pointerSlots.clear()
        usedSlotMask = 0
        consumedBySelfInInitial.clear()
        consumedBySelfInMain.clear()
        downInCurrentEvent.clear()
        upInCurrentEvent.clear()
        seenInMain.clear()

        val dispatch = dispatchState.value
        toCancel.fastForEach { state ->
            var consumed = false
            if (PointerInputRouter.POINTER_PASS_INITIAL in passes) {
                val status =
                    dispatchPointerChange(
                        pass = PointerInputRouter.POINTER_PASS_INITIAL,
                        subtype = PointerInputRouter.POINTER_SUBTYPE_CANCEL,
                        consumed = false,
                        pointerSlot = state.slot,
                        x = state.lastX,
                        y = state.lastY,
                        velocityX = 0f,
                        velocityY = 0f,
                        dispatch = dispatch,
                    )
                if (status and PointerInputRouter.POINTER_STATUS_CONSUMED != 0) {
                    consumed = true
                }
            }
            if (PointerInputRouter.POINTER_PASS_MAIN in passes) {
                val status =
                    dispatchPointerChange(
                        pass = PointerInputRouter.POINTER_PASS_MAIN,
                        subtype = PointerInputRouter.POINTER_SUBTYPE_CANCEL,
                        consumed = consumed,
                        pointerSlot = state.slot,
                        x = state.lastX,
                        y = state.lastY,
                        velocityX = 0f,
                        velocityY = 0f,
                        dispatch = dispatch,
                    )
                if (status and PointerInputRouter.POINTER_STATUS_CONSUMED != 0) {
                    consumed = true
                }
            }
            if (PointerInputRouter.POINTER_PASS_FINAL in passes) {
                dispatchPointerChange(
                    pass = PointerInputRouter.POINTER_PASS_FINAL,
                    subtype = PointerInputRouter.POINTER_SUBTYPE_CANCEL,
                    consumed = consumed,
                    pointerSlot = state.slot,
                    x = state.lastX,
                    y = state.lastY,
                    velocityX = 0f,
                    velocityY = 0f,
                    dispatch = dispatch,
                )
            }
        }
    }
}

private class TrackedPointerState(val slot: Int, var lastX: Float, var lastY: Float)

private class PointerVelocityEntry {
    val tracker = VelocityTracker()
    var releaseVelocityX = 0f
    var releaseVelocityY = 0f
}

/**
 * Returns [component]'s [EventHandlerModifierOperation]s of the given event [type], parsed and in
 * document order.
 */
@Composable
private fun rememberEventActions(
    component: LayoutComponent,
    type: Int,
): List<ParsedEventHandler> =
    remember(component, type) {
        buildList {
            component.componentModifiers.list.fastForEach {
                if (it is EventHandlerModifierOperation) {
                    val parsed = it.parseEventHandler()
                    if (parsed.type == type) add(parsed)
                }
            }
        }
    }

/**
 * Returns a dispatcher for [handlers] that reads the latest document, context and host callbacks,
 * as the input handlers using it outlive recompositions.
 */
@Composable
private fun rememberEventDispatcher(handlers: List<ParsedEventHandler>): State<(Event) -> Int> {
    val coreDocument = LocalCoreDocument.current
    val remoteContext = LocalRemoteContext.current
    val graphContext = LocalGraphContext.current
    val onAction = LocalRemoteActionHandler.current
    val onNamedAction = LocalRemoteNamedActionHandler.current
    return rememberUpdatedState { event: Event ->
        dispatchEventActions(
            handlers = handlers,
            event = event,
            coreDocument = coreDocument,
            remoteContext = remoteContext,
            onAction = onAction,
            onNamedAction = onNamedAction,
            graphContext = graphContext,
        )
    }
}

private fun dispatchPointerChange(
    pass: Int,
    subtype: Int,
    consumed: Boolean,
    pointerSlot: Int,
    x: Float,
    y: Float,
    velocityX: Float,
    velocityY: Float,
    dispatch: (Event) -> Int,
): Int {
    val event = PointerInputEvent.obtain()
    event.setPass(pass)
    event.setSubtype(subtype)
    if (consumed) {
        event.markConsumed()
    }
    val data = event.data
    data[0] = pointerSlot.toFloat()
    data[1] = x
    data[2] = y
    data[3] = velocityX
    data[4] = velocityY
    return dispatch(event)
}
