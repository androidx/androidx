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

package androidx.ui.core.adapter

import androidx.ui.core.ComplexMeasureBox
import androidx.ui.core.ComplexMeasureOperations
import androidx.ui.core.Constraints
import androidx.ui.core.Dp
import androidx.ui.core.IntrinsicMeasureOperations
import androidx.ui.core.MeasureBoxComposable
import androidx.ui.core.ComplexMeasureBoxComposable
import androidx.ui.core.IntPx
import androidx.ui.core.Measurable
import androidx.ui.core.MeasureOperations
import androidx.ui.core.Placeable
import androidx.ui.core.PositioningBlockReceiver
import androidx.ui.core.Px
import androidx.ui.core.round
import androidx.ui.core.toPx
import com.google.r4a.Children
import com.google.r4a.Composable

/**
 * All this module is needed to work around b/120971484
 *
 * For the original logic:
 * @see androidx.ui.core.MeasureBox
 */
@Composable
@Suppress("PLUGIN_ERROR")
fun MeasureBox(
    @Children(composable = false) block:
        MeasureBoxReceiver.(constraints: Constraints) -> Unit
) {
    MeasureBoxComposable { constraints, measureOperations ->
        val measureBoxReceiver = MeasureBoxReceiver(measureOperations)
        with(measureBoxReceiver) {
            block(constraints)
        }
    }
}

/**
 * All this module is needed to work around b/120971484
 *
 * For the original logic:
 * @see androidx.ui.core.ComplexMeasureBox
 */
@Composable
@Suppress("PLUGIN_ERROR")
fun ComplexMeasureBox(
    @Children(composable = false) block: ComplexMeasureBoxReceiver.() -> Unit
) {
    ComplexMeasureBoxComposable { complexMeasureOperations ->
        val complexMeasureBoxReceiver = ComplexMeasureBoxReceiver(complexMeasureOperations)
        with(complexMeasureBoxReceiver) {
            block()
        }
    }
}

@DslMarker private annotation class LayoutDsl

/**
 * Receiver scope for [ComplexMeasureBox]'s child lambda.
 */
@LayoutDsl
class ComplexMeasureBoxReceiver internal constructor(
    private val complexMeasureOperations: ComplexMeasureOperations
) {
    fun collect(@Children children: () -> Unit) = complexMeasureOperations.collect(children)
    fun layout(block: LayoutBlockReceiver.(Constraints) -> Unit) {
        complexMeasureOperations.layout { constraints, measure, intrinsics, layoutResult ->
            val layoutBlockReceiver =
                LayoutBlockReceiver(measure, intrinsics, layoutResult, this::collect)
            with(layoutBlockReceiver) {
                block(constraints)
            }
        }
    }

    /**
     * Set the min intrinsic width of the current layout. The block is not aware of constraints,
     * and is unable to measure their children.
     */
    fun minIntrinsicWidth(block: IntrinsicMeasurementsReceiver.(IntPx) -> IntPx) {
        complexMeasureOperations.minIntrinsicWidth { h, intrinsics ->
            val intrinsicMeasurementsReceiver =
                    IntrinsicMeasurementsReceiver(this::collect, intrinsics)
            with(intrinsicMeasurementsReceiver) {
                block(h)
            }
        }
    }

    /**
     * Set the max intrinsic width of the current layout. The block is not aware of constraints,
     * and is unable to measure their children.
     */
    fun maxIntrinsicWidth(block: IntrinsicMeasurementsReceiver.(IntPx) -> IntPx) {
        complexMeasureOperations.maxIntrinsicWidth { h, intrinsics ->
            val intrinsicMeasurementsReceiver =
                    IntrinsicMeasurementsReceiver(this::collect, intrinsics)
            with(intrinsicMeasurementsReceiver) {
                block(h)
            }
        }
    }

    /**
     * Set the min intrinsic height of the current layout. The block is not aware of constraints,
     * and is unable to measure their children.
     */
    fun minIntrinsicHeight(block: IntrinsicMeasurementsReceiver.(IntPx) -> IntPx) {
        complexMeasureOperations.minIntrinsicHeight { w, intrinsics ->
            val intrinsicMeasurementsReceiver =
                    IntrinsicMeasurementsReceiver(this::collect, intrinsics)
            with(intrinsicMeasurementsReceiver) {
                block(w)
            }
        }
    }

    /**
     * Set the max intrinsic height of the current layout. The block is not aware of constraints,
     * and is unable to measure their children.
     */
    fun maxIntrinsicHeight(block: IntrinsicMeasurementsReceiver.(IntPx) -> IntPx) {
        complexMeasureOperations.maxIntrinsicHeight { w, intrinsics ->
            val intrinsicMeasurementsReceiver =
                    IntrinsicMeasurementsReceiver(this::collect, intrinsics)
            with(intrinsicMeasurementsReceiver) {
                block(w)
            }
        }
    }

    fun Dp.toPx(): Px = toPx(complexMeasureOperations.density)
    fun Dp.toIntPx(): IntPx = toPx().round()
}

/**
 * Receiver scope for [ComplexMeasureBoxReceiver] intrinsic measurements lambdas.
 */
@LayoutDsl
class IntrinsicMeasurementsReceiver internal constructor(
    private val doCollect: (@Composable() () -> Unit) -> List<Measurable>,
    private val intrinsics: IntrinsicMeasureOperations
) {
    fun collect(@Children children: () -> Unit) = doCollect(children)
    fun Measurable.minIntrinsicWidth(h: IntPx) = intrinsics.minIntrinsicWidth(this, h)
    fun Measurable.maxIntrinsicWidth(h: IntPx) = intrinsics.maxIntrinsicWidth(this, h)
    fun Measurable.minIntrinsicHeight(w: IntPx) = intrinsics.minIntrinsicHeight(this, w)
    fun Measurable.maxIntrinsicHeight(w: IntPx) = intrinsics.maxIntrinsicHeight(this, w)
}

/**
 * Receiver scope for [ComplexMeasureBoxReceiver#layout]'s lambda.
 */
@LayoutDsl
class LayoutBlockReceiver internal constructor(
    private val measure: (Measurable, Constraints) -> Placeable,
    private val intrinsics: IntrinsicMeasureOperations,
    private val doLayoutResult: (IntPx, IntPx, () -> Unit) -> Unit,
    private val doCollect: (@Composable() () -> Unit) -> List<Measurable>
) {
    fun Measurable.measure(constraints: Constraints) = measure(this, constraints)
    fun layoutResult(
        width: IntPx,
        height: IntPx,
        block: PositioningBlockReceiver.() -> Unit
    ) {
        val positioningBlockReceiver = PositioningBlockReceiver()
        doLayoutResult(width, height, { with(positioningBlockReceiver) { block } })
    }
    fun collect(@Children children: () -> Unit) = doCollect(children)
    fun Measurable.minIntrinsicWidth(h: IntPx) = intrinsics.minIntrinsicWidth(this, h)
    fun Measurable.maxIntrinsicWidth(h: IntPx) = intrinsics.maxIntrinsicWidth(this, h)
    fun Measurable.minIntrinsicHeight(w: IntPx) = intrinsics.minIntrinsicHeight(this, w)
    fun Measurable.maxIntrinsicHeight(w: IntPx) = intrinsics.maxIntrinsicHeight(this, w)
}

/**
 * Receiver  scope for [ComplexMeasureBox]'s child lambda.
 */
class MeasureBoxReceiver internal constructor(
    private val measureOperations: MeasureOperations
) {
    fun collect(@Children children: () -> Unit) = measureOperations.collect(children)
    fun Measurable.measure(constraints: Constraints) =
            measureOperations.measure(this, constraints)
    fun layout(width: IntPx, height: IntPx, block: PositioningBlockReceiver.() -> Unit) {
        measureOperations.layout(width, height,
            { with(PositioningBlockReceiver()) { block() } })
    }

    fun layout(width: Px, height: Px, block: PositioningBlockReceiver.() -> Unit) {
        layout(width.round(), height.round(), block)
    }

    fun Dp.toPx(): Px = toPx(measureOperations.density)
    fun Dp.toIntPx(): IntPx = toPx().round()
}
