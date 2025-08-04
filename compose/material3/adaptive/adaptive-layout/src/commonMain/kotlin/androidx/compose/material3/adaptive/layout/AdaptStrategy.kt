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

package androidx.compose.material3.adaptive.layout

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import kotlin.jvm.JvmInline

/**
 * Provides the information about how the associated pane should be adapted if not all panes can be
 * displayed in the [PaneAdaptedValue.Expanded] state.
 */
@ExperimentalMaterial3AdaptiveApi
@Stable
sealed interface AdaptStrategy {
    /** Override this function to provide the resulted adapted state. */
    @Deprecated(
        "This function is deprecated in favor of directly using the info carried by the " +
            "strategy instances to make adaptation decisions."
    )
    fun adapt(): PaneAdaptedValue = PaneAdaptedValue.Hidden

    @Immutable
    private class Simple(private val description: String) : AdaptStrategy {
        override fun toString() = "AdaptStrategy[$description]"
    }

    /**
     * Indicate the associated pane should be reflowed when certain conditions are met. With the
     * default calculation functions [calculateThreePaneScaffoldValue] we provide, when it's a
     * single pane layout, a pane with a reflow strategy will be adapted to either:
     * 1. [PaneAdaptedValue.Reflowed], when either the reflowed pane or the target pane it's
     *    supposed to be reflowed to is the current destination; or
     * 2. [PaneAdaptedValue.Hidden] otherwise.
     *
     * Note that if the current layout can have more than one horizontal partition, the pane will
     * never be reflowed.
     *
     * To provide custom adapt strategies, see the following sample:
     *
     * @sample androidx.compose.material3.adaptive.samples.reflowAdaptStrategySample
     * @param reflowUnder the pane that the reflowed pane will be put under; within the context of
     *   three pane scaffolds, the type of this parameter is supposed to be [ThreePaneScaffoldRole].
     */
    @Immutable
    class Reflow(internal val reflowUnder: Any) : AdaptStrategy {
        override fun toString() = "AdaptStrategy[Reflow to $reflowUnder]"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Reflow) return false
            return reflowUnder == other.reflowUnder
        }

        override fun hashCode(): Int {
            return reflowUnder.hashCode()
        }
    }

    /**
     * Indicate the associated pane should be levitated when certain conditions are met. A levitated
     * pane will be rendered above other panes in the pane scaffold like a pop-up, may or may not
     * cast a scrim to block interaction with the underlying panes.
     *
     * With the default calculation functions [calculateThreePaneScaffoldValue] we provide. A pane
     * with a levitate strategy will be adapted to either:
     * 1. [PaneAdaptedValue.Levitated] with specified [alignment], when the levitated pane is the
     *    current destination, and the provided [Strategy] is [Strategy.Always] or it's a
     *    single-pane layout;
     * 2. [PaneAdaptedValue.Expanded], when the levitated pane is one of the most recent
     *    destinations, and the provided [Strategy] is [Strategy.SinglePaneOnly] and it's not a
     *    single-pane layout; or
     * 3. [PaneAdaptedValue.Hidden] otherwise.
     *
     * @sample androidx.compose.material3.adaptive.samples.levitateAdaptStrategySample
     * @sample androidx.compose.material3.adaptive.samples.SupportingPaneScaffoldSampleWithExtraPaneLevitatedAsBottomSheet
     * @param strategy the strategy that specifies when the associated pane should be levitated; see
     *   [Strategy] for more detailed descriptions.
     * @param alignment the alignment of the associated pane when it's levitated, relatively to the
     *   pane scaffold.
     * @param scrim the scrim to show when the pane is levitated to block user interaction with the
     *   underlying layout and emphasize the levitated pane; by default it will be `null` and no
     *   scrim will show.
     */
    @Immutable
    class Levitate(
        internal val strategy: Strategy = Strategy.Always,
        internal val alignment: Alignment = Alignment.Center,
        internal val scrim: Scrim? = null,
    ) : AdaptStrategy {
        override fun toString() =
            "AdaptStrategy[Levitate, type=$strategy, alignment=$alignment, scrim=$scrim]"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Levitate) return false
            if (strategy != other.strategy) return false
            if (alignment != other.alignment) return false
            if (scrim != other.scrim) return false
            return true
        }

        override fun hashCode(): Int {
            var result = strategy.hashCode()
            result = 31 * result + alignment.hashCode()
            result = 31 * result + scrim.hashCode()
            return result
        }

        /**
         * The strategy that specifies when the associated pane should be levitated. Currently two
         * strategies are supported - [Always] and [SinglePaneOnly]. A pane with [Always] strategy
         * is supposed to be levitated whenever it's the current destination; on the other hand, one
         * with [SinglePaneOnly] strategy is only supposed to be levitated when it's a single-pane
         * layout.
         */
        @JvmInline
        value class Strategy private constructor(private val description: String) {
            override fun toString() = description

            companion object {
                /**
                 * Specifies that the associated pane should always be levitated when it's the
                 * current navigation destination, no matter it's a single-pane or multi-pane
                 * layout.
                 */
                val Always = Strategy("Always")

                /**
                 * Specifies that the associated pane should only be levitated when it's a
                 * single-pane layout and the associated pane is the current navigation destination.
                 */
                val SinglePaneOnly = Strategy("SinglePaneOnly")
            }
        }
    }

    companion object {
        /**
         * The default [AdaptStrategy] that suggests the layout to hide the associated pane when it
         * has to be adapted, i.e., cannot be displayed in its [PaneAdaptedValue.Expanded] state.
         */
        val Hide: AdaptStrategy = Simple("Hide")
    }
}
