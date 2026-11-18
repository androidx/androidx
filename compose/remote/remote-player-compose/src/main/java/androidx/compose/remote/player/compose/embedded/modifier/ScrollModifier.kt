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

package androidx.compose.remote.player.compose.embedded.modifier

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.TouchExpression
import androidx.compose.remote.core.operations.layout.modifiers.ScrollModifierOperation
import androidx.compose.remote.player.compose.embedded.LocalCoreDocument
import androidx.compose.remote.player.compose.embedded.LocalGraphContext
import androidx.compose.remote.player.compose.embedded.LocalRemoteContext
import androidx.compose.remote.player.compose.embedded.doc.GraphContext
import androidx.compose.remote.player.compose.embedded.doc.SnapshotRemoteComposeState
import androidx.compose.remote.player.compose.embedded.state.MaybeVariableFloat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * The even-notch count of a scroll, or 0 for free scrolling. The scroll's `TouchExpression` (a
 * child op of the scroll modifier) carries `STOP_NOTCHES_EVEN` + `stopSpec = [notches, notchMax]`;
 * the count is a constant in the bytes (the range is taken from Compose's scrollState, since the
 * document's notchMax is a layout-computed variable the embedded player doesn't populate).
 */
private fun evenNotchCount(op: ScrollModifierOperation): Int {
    val touch = op.list.fastFirstOrNull { it is TouchExpression } as? TouchExpression ?: return 0
    if (touch.stopMode != TouchExpression.STOP_NOTCHES_EVEN) return 0
    val spec = touch.stopSpec ?: return 0
    return spec.firstOrNull()?.toInt()?.coerceAtLeast(0) ?: 0
}

/**
 * Resolves the initial scroll offset in pixels from the document's scroll-position state variable
 * (or literal expression), clamped to `>= 0`.
 */
private fun initialScrollPx(
    op: ScrollModifierOperation,
    document: CoreDocument,
    remoteContext: RemoteContext,
    graphContext: GraphContext?,
): Int = Snapshot.withoutReadObservation {
    val raw = MaybeVariableFloat(op.positionExpression)
    val px =
        if (raw.isReference) {
            val id = raw.id
            val snapshotState = remoteContext.mRemoteComposeState as? SnapshotRemoteComposeState
            val expr =
                if (snapshotState?.isFloatOverridden(id) == true) {
                    null
                } else {
                    document.getFloatExpressions()[id]
                }
            expr?.evaluate(graphContext ?: remoteContext) ?: remoteContext.getFloat(id)
        } else {
            op.positionExpression
        }
    if (px.isFinite()) px.roundToInt().coerceAtLeast(0) else 0
}

@Composable
internal fun Modifier.scroll(op: ScrollModifierOperation): Modifier {
    val document = LocalCoreDocument.current
    val remoteContext = LocalRemoteContext.current
    val graphContext = LocalGraphContext.current
    val scrollState =
        rememberSaveable(op, saver = ScrollState.Saver) {
            ScrollState(initial = initialScrollPx(op, document, remoteContext, graphContext))
        }

    // The document binds the scroll offset to a variable (mPositionExpression, the output of the
    // scroll's TouchExpression). The embedded player drives scroll with Compose's native gesture +
    // fling rather than the core touch engine — which hit-tests against core-layout bounds the
    // embedded player doesn't populate — and reproduces the relevant TouchExpression semantics in
    // pure Compose: the live offset is published back to the bound variable (so expressions reading
    // scroll position — progress indicators, parallax — react), and an even-notch stop spec is
    // honored
    // with a snapping fling. The core works in pixels and scrollState.value is px, so it's written
    // directly.
    val positionId =
        remember(op) {
            val raw = MaybeVariableFloat(op.positionExpression)
            if (raw.isReference) raw.id else -1
        }
    if (positionId > 0) {
        LaunchedEffect(scrollState, positionId) {
            snapshotFlow { scrollState.value }
                .collect { px -> remoteContext.overrideFloat(positionId, px.toFloat()) }
        }
    }

    // Even-notch scrolling: snap the fling to one of `notches` evenly-spaced positions over the
    // scrollable range (Compose's scrollState.maxValue). Unlike core `STOP_NOTCHES_EVEN`, which
    // settles to the nearest notch from the final offset, `calculateSnapOffset` intentionally
    // advances or retreats at least one notch in the fling direction whenever release velocity
    // exceeds `SnapVelocityThresholdDpPerSec` (while `calculateApproachOffset` caps decay to at
    // most one step), so wheel flicks reliably progress without multi-notch overshoot.
    val notches = remember(op) { evenNotchCount(op) }
    val velocityThresholdPxPerSec =
        with(LocalDensity.current) { SnapVelocityThresholdDpPerSec.toPx() }
    val flingBehavior =
        if (notches > 0) {
            val provider =
                remember(scrollState, notches, velocityThresholdPxPerSec) {
                    object : SnapLayoutInfoProvider {
                        override fun calculateApproachOffset(
                            velocity: Float,
                            decayOffset: Float,
                        ): Float {
                            val max = scrollState.maxValue
                            if (max <= 0) return 0f
                            val step = max.toFloat() / notches
                            if (step <= 0f || abs(decayOffset) <= step) return 0f
                            return (abs(decayOffset) - step).coerceAtLeast(0f) * sign(decayOffset)
                        }

                        override fun calculateSnapOffset(velocity: Float): Float {
                            val max = scrollState.maxValue
                            if (max <= 0) return 0f
                            val step = max.toFloat() / notches
                            if (step <= 0f) return 0f
                            val current = scrollState.value.toFloat()
                            val posInSteps = current / step
                            val targetStep =
                                when {
                                    velocity > velocityThresholdPxPerSec -> {
                                        val next = ceil(posInSteps).toInt()
                                        if (next - posInSteps < 0.02f) next + 1 else next
                                    }
                                    velocity < -velocityThresholdPxPerSec -> {
                                        val prev = floor(posInSteps).toInt()
                                        if (posInSteps - prev < 0.02f) prev - 1 else prev
                                    }
                                    else -> posInSteps.roundToInt()
                                }.coerceIn(0, notches)
                            return targetStep * step - current
                        }
                    }
                }
            rememberSnapFlingBehavior(provider)
        } else null

    val isVertical = op.isVerticalScroll
    // Keep leftover same-axis scroll and fling at the remote scroller's edge so a drag inside it
    // does not scroll an enclosing host list. A scroller whose content already fits
    // (maxValue == 0) has nothing to scroll and passes everything through, like any other
    // non-scrolling content.
    val consumeScrollOnAxis =
        remember(isVertical, scrollState) {
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset =
                    when {
                        scrollState.maxValue <= 0 -> Offset.Zero
                        isVertical -> Offset(0f, available.y)
                        else -> Offset(available.x, 0f)
                    }

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity,
                ): Velocity =
                    when {
                        scrollState.maxValue <= 0 -> Velocity.Zero
                        isVertical -> Velocity(0f, available.y)
                        else -> Velocity(available.x, 0f)
                    }
            }
        }

    return if (isVertical) {
        this.nestedScroll(consumeScrollOnAxis)
            .verticalScroll(scrollState, flingBehavior = flingBehavior)
    } else {
        this.nestedScroll(consumeScrollOnAxis)
            .horizontalScroll(
                scrollState,
                flingBehavior = flingBehavior,
                reverseScrolling = LocalLayoutDirection.current == LayoutDirection.Rtl,
            )
    }
}

private val SnapVelocityThresholdDpPerSec = 150.dp
