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

import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.remote.core.operations.TouchExpression
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.modifiers.ScrollModifierOperation
import androidx.compose.remote.player.compose.embedded.LocalRemoteContext
import androidx.compose.remote.player.compose.embedded.scrollPosition
import androidx.compose.remote.player.compose.embedded.touchStopMode
import androidx.compose.remote.player.compose.embedded.touchStopSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastFirstOrNull
import kotlin.math.roundToInt

/**
 * The even-notch count of a scroll, or 0 for free scrolling. The scroll's `TouchExpression` (a
 * child op of the scroll modifier) carries `STOP_NOTCHES_EVEN` + `stopSpec = [notches, notchMax]`;
 * the count is a constant in the bytes (the range is taken from Compose's scrollState, since the
 * document's notchMax is a layout-computed variable the embedded player doesn't populate).
 */
private fun evenNotchCount(op: ScrollModifierOperation): Int {
    val touch = op.list.fastFirstOrNull { it is TouchExpression } as? TouchExpression ?: return 0
    if (touchStopMode(touch) != TouchExpression.STOP_NOTCHES_EVEN) return 0
    val spec = touchStopSpec(touch) ?: return 0
    return spec.firstOrNull()?.toInt()?.coerceAtLeast(0) ?: 0
}

@Composable
internal fun Modifier.scroll(op: ScrollModifierOperation): Modifier {
    val remoteContext = LocalRemoteContext.current
    val scrollState = rememberScrollState()

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
            val raw = scrollPosition(op)
            if (Utils.isVariable(raw)) Utils.idFromNan(raw) else -1
        }
    if (positionId > 0) {
        LaunchedEffect(scrollState, positionId) {
            snapshotFlow { scrollState.value }
                .collect { px -> remoteContext.overrideFloat(positionId, px.toFloat()) }
        }
    }

    // Even-notch scrolling: snap the fling to one of `notches` evenly-spaced positions over the
    // scrollable range (Compose's scrollState.maxValue), mirroring the TouchExpression's
    // STOP_NOTCHES_EVEN settle.
    val notches = remember(op) { evenNotchCount(op) }
    val flingBehavior =
        if (notches > 0) {
            val provider =
                remember(scrollState, notches) {
                    object : SnapLayoutInfoProvider {
                        override fun calculateSnapOffset(velocity: Float): Float {
                            val max = scrollState.maxValue
                            if (max <= 0) return 0f
                            val step = max.toFloat() / notches
                            if (step <= 0f) return 0f
                            val current = scrollState.value.toFloat()
                            return (current / step).roundToInt() * step - current
                        }
                    }
                }
            rememberSnapFlingBehavior(provider)
        } else null

    return if (op.isVerticalScroll) {
        this.verticalScroll(scrollState, flingBehavior = flingBehavior)
    } else {
        this.horizontalScroll(scrollState, flingBehavior = flingBehavior)
    }
}
