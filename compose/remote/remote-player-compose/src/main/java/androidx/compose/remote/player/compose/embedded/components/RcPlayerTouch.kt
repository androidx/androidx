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

@file:Suppress("RestrictedApiAndroidX", "PrimitiveInCollection")

package androidx.compose.remote.player.compose.embedded.components

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.TouchExpression
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.managers.LayoutManager
import androidx.compose.remote.player.compose.embedded.doc.DocumentPreprocessResult
import androidx.compose.remote.player.compose.embedded.doc.GraphContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs

internal class ComponentPlacementHolder {
    var rootOffset: Offset = Offset.Zero
    var posInParent: Offset = Offset.Zero
    var width: Float = Float.NaN
    var height: Float = Float.NaN
    var boundsInitialized: Boolean = false
    /** The component's outermost coordinates (its full, padding-inclusive box). */
    var outerCoords: LayoutCoordinates? = null
    /** The coordinates of the TouchExpression pointerInput, inside the component's modifiers. */
    var touchCoords: LayoutCoordinates? = null

    /**
     * The offset of the TouchExpression pointerInput within the component's outer box, e.g. its
     * padding. Core reports touch positions relative to the outer box (and hit-tests against the
     * full component size), so pointer positions are shifted by this before reaching core.
     */
    fun touchOrigin(): Offset {
        val outer = outerCoords ?: return Offset.Zero
        val touch = touchCoords ?: return Offset.Zero
        if (!outer.isAttached || !touch.isAttached) return Offset.Zero
        return outer.localPositionOf(touch, Offset.Zero)
    }
}

/**
 * Root-level touch forwarding: updates `ID_TOUCH_POS_X`/`Y` and drives the [TouchExpression]s
 * outside any [LayoutComponent] (`preprocessed.rootTouchExpressions`). Component touch handlers and
 * component-scoped [TouchExpression]s are dispatched by their own Compose modifiers, so the
 * [CoreDocument] touch methods (which also re-traverse the component tree) are deliberately not
 * used here.
 */
internal fun Modifier.rootTouchExpressions(
    document: CoreDocument,
    remoteContext: RemoteContext,
    preprocessed: DocumentPreprocessResult,
    graphContext: GraphContext,
): Modifier =
    pointerInput(document, remoteContext, preprocessed, graphContext) {
        val rootTouchExpressions = preprocessed.rootTouchExpressions
        // Publish the touch position before resolving the expressions' variables, so
        // their precalculated inputs see this event's position.
        fun loadRootTouchPosition(pos: Offset) {
            remoteContext.loadFloat(RemoteContext.ID_TOUCH_POS_X, pos.x)
            remoteContext.loadFloat(RemoteContext.ID_TOUCH_POS_Y, pos.y)
            rootTouchExpressions.fastForEach { te -> te.updateVariables(graphContext) }
        }
        fun releaseRootTouch(pos: Offset, velocityX: Float, velocityY: Float) {
            loadRootTouchPosition(pos)
            rootTouchExpressions.fastForEach { te ->
                te.touchUp(remoteContext, pos.x, pos.y, velocityX, velocityY)
            }
            rootTouchExpressions.fastForEach { te -> te.apply(remoteContext) }
        }
        val velocityTracker = VelocityTracker()
        awaitPointerEventScope {
            while (true) {
                val downEvent = awaitPointerEvent(PointerEventPass.Initial)
                val down =
                    downEvent.changes.fastFirstOrNull { !it.previousPressed && it.pressed }
                        ?: continue
                val pointerId = down.id
                var released = false
                var lastPos = down.position
                velocityTracker.resetTracking()
                velocityTracker.addPointerInputChange(down)
                loadRootTouchPosition(down.position)
                rootTouchExpressions.fastForEach { te ->
                    // Core binds these to the RootLayoutComponent; apply refreshes
                    // their hit bounds from its size (TouchExpression.updateBounds)
                    // before the touchDown hit test, as the component handler does.
                    te.apply(remoteContext)
                    te.touchDown(remoteContext, down.position.x, down.position.y)
                }
                rootTouchExpressions.fastForEach { te -> te.apply(remoteContext) }
                try {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        val change = event.changes.fastFirstOrNull { it.id == pointerId } ?: break
                        if (change.isConsumed) {
                            // A child took the gesture: stop forwarding and release
                            // at the last forwarded position, without a fling.
                            released = true
                            releaseRootTouch(lastPos, 0f, 0f)
                            break
                        }
                        val pos = change.position
                        velocityTracker.addPointerInputChange(change)
                        if (!change.pressed) {
                            val velocity = velocityTracker.calculateVelocity()
                            released = true
                            releaseRootTouch(pos, velocity.x, velocity.y)
                            break
                        }
                        if (event.type == PointerEventType.Move) {
                            lastPos = pos
                            loadRootTouchPosition(pos)
                            rootTouchExpressions.fastForEach { te ->
                                te.touchDrag(remoteContext, pos.x, pos.y)
                            }
                        }
                    }
                } finally {
                    // The pointer disappeared or the handler was cancelled or
                    // restarted mid-gesture. TouchExpression has no cancel, so
                    // release at the last position with no velocity and let it settle
                    // according to its stop mode (as the component handler does).
                    if (!released) {
                        releaseRootTouch(lastPos, 0f, 0f)
                    }
                }
            }
        }
    }

private fun touchExpressionAxes(touchExpressions: List<TouchExpression>): Pair<Boolean, Boolean> {
    var usesX = false
    var usesY = false
    touchExpressions.fastForEach { te ->
        val exp = te.mSrcExp ?: return@fastForEach
        for (v in exp) {
            if (v.isNaN()) {
                when (Utils.idFromNan(v)) {
                    RemoteContext.ID_TOUCH_POS_X -> usesX = true
                    RemoteContext.ID_TOUCH_POS_Y -> usesY = true
                }
            }
        }
    }
    if (!usesX && !usesY) {
        return true to true
    }
    return usesX to usesY
}

@Composable
internal fun Modifier.componentTouchExpressions(
    component: LayoutComponent,
    touchExpressions: List<TouchExpression>,
    placement: ComponentPlacementHolder,
    remoteContext: RemoteContext,
    graphContext: GraphContext?,
): Modifier {
    val (usesX, usesY) = remember(touchExpressions) { touchExpressionAxes(touchExpressions) }
    return this.onPlaced { placement.touchCoords = it }
        .pointerInput(
            component,
            touchExpressions,
            remoteContext,
            graphContext,
        ) {
            val velocityTracker = VelocityTracker()
            val touchSlop = viewConfiguration.touchSlop
            awaitPointerEventScope {
                while (true) {
                    val downEvent = awaitPointerEvent(PointerEventPass.Main)
                    val down =
                        downEvent.changes.fastFirstOrNull { !it.previousPressed && it.pressed }
                            ?: continue
                    val pointerId = down.id
                    val downPos = down.position
                    var dragLocked = false
                    var dragRejected = false
                    val isLegacyTouch = remoteContext.touchVersion != LayoutManager.FIX_TOUCH_EVENT
                    // Pointer positions are relative to this pointerInput, which sits inside the
                    // component's modifiers (e.g. padding). Shift them into the component's outer
                    // box, the space core uses for touch positions and the hit bounds (the
                    // component's full size, published by onPlaced).
                    val touchOrigin = placement.touchOrigin()
                    velocityTracker.resetTracking()
                    velocityTracker.addPointerInputChange(down)
                    val downLocal = downPos + touchOrigin
                    val downRootX = placement.rootOffset.x + downLocal.x
                    val downRootY = placement.rootOffset.y + downLocal.y
                    val downTouchX = if (isLegacyTouch) downRootX else downLocal.x
                    val downTouchY = if (isLegacyTouch) downRootY else downLocal.y
                    remoteContext.loadFloat(
                        RemoteContext.ID_TOUCH_POS_X,
                        downRootX,
                    )
                    remoteContext.loadFloat(
                        RemoteContext.ID_TOUCH_POS_Y,
                        downRootY,
                    )
                    touchExpressions.fastForEach { te ->
                        te.updateVariables(graphContext ?: remoteContext)
                        // Bounds are only read by the touchDown hit test, so refresh them here
                        // (apply runs updateBounds) rather than on every placement. Legacy bounds
                        // depend on the ancestors' positions, which change while scrolling.
                        te.apply(remoteContext)
                        te.touchDown(remoteContext, downTouchX, downTouchY)
                        te.apply(remoteContext)
                    }
                    var ended = false
                    var lastTouchX = downTouchX
                    var lastTouchY = downTouchY
                    try {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change =
                                event.changes.fastFirstOrNull { it.id == pointerId } ?: break
                            val pos = change.position
                            val local = pos + touchOrigin
                            val rootX = placement.rootOffset.x + local.x
                            val rootY = placement.rootOffset.y + local.y
                            val touchX = if (isLegacyTouch) rootX else local.x
                            val touchY = if (isLegacyTouch) rootY else local.y
                            lastTouchX = touchX
                            lastTouchY = touchY
                            if (!change.pressed) {
                                ended = true
                                val velocity =
                                    if (!dragRejected && !change.isConsumed) {
                                        velocityTracker.addPointerInputChange(change)
                                        velocityTracker.calculateVelocity()
                                    } else {
                                        null
                                    }
                                if (!dragRejected && !change.isConsumed) {
                                    remoteContext.loadFloat(
                                        RemoteContext.ID_TOUCH_POS_X,
                                        rootX,
                                    )
                                    remoteContext.loadFloat(
                                        RemoteContext.ID_TOUCH_POS_Y,
                                        rootY,
                                    )
                                }
                                touchExpressions.fastForEach { te ->
                                    te.updateVariables(graphContext ?: remoteContext)
                                    te.touchUp(
                                        remoteContext,
                                        touchX,
                                        touchY,
                                        velocity?.x ?: 0f,
                                        velocity?.y ?: 0f,
                                    )
                                    te.apply(remoteContext)
                                }
                                break
                            }
                            if (event.type == PointerEventType.Move) {
                                if (!dragLocked && !dragRejected) {
                                    val dx = abs(pos.x - downPos.x)
                                    val dy = abs(pos.y - downPos.y)
                                    if (usesX && !usesY) {
                                        if (dy > touchSlop && dy > dx) {
                                            dragRejected = true
                                        } else if (dx > touchSlop) {
                                            dragLocked = true
                                        }
                                    } else if (usesY && !usesX) {
                                        if (dx > touchSlop && dx > dy) {
                                            dragRejected = true
                                        } else if (dy > touchSlop) {
                                            dragLocked = true
                                        }
                                    } else if (dx * dx + dy * dy > touchSlop * touchSlop) {
                                        dragLocked = true
                                    }
                                }
                                if (!dragRejected) {
                                    velocityTracker.addPointerInputChange(change)
                                    remoteContext.loadFloat(
                                        RemoteContext.ID_TOUCH_POS_X,
                                        rootX,
                                    )
                                    remoteContext.loadFloat(
                                        RemoteContext.ID_TOUCH_POS_Y,
                                        rootY,
                                    )
                                    touchExpressions.fastForEach { te ->
                                        te.updateVariables(graphContext ?: remoteContext)
                                        te.touchDrag(
                                            remoteContext,
                                            touchX,
                                            touchY,
                                        )
                                    }
                                    if (dragLocked) {
                                        change.consume()
                                    }
                                }
                            }
                        }
                    } finally {
                        // The pointer disappeared or the handler was cancelled or restarted
                        // mid-gesture. TouchExpression has no cancel, so release at the last
                        // position with no velocity and let it settle according to its stop mode.
                        if (!ended) {
                            touchExpressions.fastForEach { te ->
                                te.updateVariables(graphContext ?: remoteContext)
                                te.touchUp(
                                    remoteContext,
                                    lastTouchX,
                                    lastTouchY,
                                    0f,
                                    0f,
                                )
                                te.apply(remoteContext)
                            }
                        }
                    }
                }
            }
        }
}
