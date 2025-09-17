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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment

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
    class Reflow(internal val reflowUnder: PaneScaffoldRole) : AdaptStrategy {
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
     * Indicate the associated pane should be levitated when it's the current destination.
     *
     * A levitated pane will be rendered above other panes in the pane scaffold like a pop-up or a
     * sheet (for example, as a bottom sheet or a side sheet.) A [scrim] can be provided to block
     * interaction with the underlying panes.
     *
     * With the default [calculateThreePaneScaffoldValue] we provide, a pane with a levitate
     * strategy will be adapted to either:
     * 1. [PaneAdaptedValue.Levitated] with specified [alignment], when the levitated pane is the
     *    current destination; or
     * 2. [PaneAdaptedValue.Hidden] otherwise.
     *
     * @param alignment the alignment of the associated pane when it's levitated, relatively to the
     *   pane scaffold.
     * @param scrim the scrim to show when the pane is levitated to block user interaction with the
     *   underlying layout and emphasize the levitated pane; by default it will be `null` and no
     *   scrim will show; to display a scrim, we recommend to use [LevitatedPaneScrim] as a default
     *   implementation.
     * @sample androidx.compose.material3.adaptive.samples.levitateAsDialogSample
     * @sample androidx.compose.material3.adaptive.samples.ListDetailPaneScaffoldSampleWithExtraPaneLevitatedAsDialog
     * @see [onlyIf] and [onlyIfSinglePane] for finer control over when the pane should be
     *   levitated.
     */
    @Immutable
    class Levitate(
        internal val alignment: Alignment = Alignment.Center,
        internal val scrim: (@Composable () -> Unit)? = null,
    ) : AdaptStrategy {
        override fun toString() = "AdaptStrategy[Levitate, alignment=$alignment, scrim=$scrim]"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Levitate) return false
            if (alignment != other.alignment) return false
            if (scrim !== other.scrim) return false
            return true
        }

        override fun hashCode(): Int {
            var result = alignment.hashCode()
            result = 31 * result + scrim.hashCode()
            return result
        }

        /**
         * This is a convenient function to only levitate the associated pane when the provided
         * condition is met. If the condition is not met, the pane will be expanded instead, if
         * there's enough room; otherwise it will be hidden.
         *
         * @see onlyIfSinglePane
         */
        @Composable fun onlyIf(condition: Boolean): AdaptStrategy = if (condition) this else Hide

        /**
         * This is a convenient function to only levitate the associated pane when it's a
         * single-pane layout. On multi-pane layouts, the pane will be expanded instead, if it's one
         * of the recent destinations.
         */
        @Composable
        fun onlyIfSinglePane(scaffoldDirective: PaneScaffoldDirective): AdaptStrategy =
            onlyIf(scaffoldDirective.isSinglePaneLayout())
    }

    companion object {
        /**
         * The default [AdaptStrategy] that suggests the layout to hide the associated pane when it
         * has to be adapted, i.e., cannot be displayed in its [PaneAdaptedValue.Expanded] state.
         */
        val Hide: AdaptStrategy = Simple("Hide")
    }
}
