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

/**
 * The adapted state of a pane. It gives clues to pane scaffolds about if a certain pane should be
 * composed and how.
 */
@ExperimentalMaterial3AdaptiveApi
@Stable
sealed interface PaneAdaptedValue {
    private class Simple(private val description: String) : PaneAdaptedValue {
        override fun toString() = "PaneAdaptedValue[$description]"
    }

    /**
     * Indicates that the associated pane should be reflowed to its [reflowUnder], i.e., it will be
     * displayed under the target pane.
     *
     * @param reflowUnder the target pane of the reflowing, i.e., the pane that the reflowed pane
     *   will be put under.
     */
    @Immutable
    class Reflowed(val reflowUnder: PaneScaffoldRole) : PaneAdaptedValue {
        override fun toString() = "PaneAdaptedValue[Reflowed to $reflowUnder]"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Reflowed) return false
            return reflowUnder == other.reflowUnder
        }

        override fun hashCode(): Int {
            return reflowUnder.hashCode()
        }
    }

    /**
     * Indicates that the associated pane should be levitated with the specified [alignment].
     *
     * @param alignment the alignment of the levitated pane relative to the pane scaffold; the
     *   alignment can also be provided as anchoring to a certain alignment line or a certain
     *   element in the window. See [Alignment] for more information.
     * @param scrim the scrim to show when the levitated pane is shown to block user interaction
     *   with the underlying layout and emphasize the levitated pane; by default it will be `null`
     *   and no scrim will show.
     */
    @Immutable
    class Levitated(val alignment: Alignment, val scrim: Scrim? = null) : PaneAdaptedValue {
        override fun toString() = "PaneAdaptedValue[Levitated with $alignment and scrim=$scrim]"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Levitated) return false
            if (alignment != other.alignment) return false
            if (scrim != other.scrim) return false
            return true
        }

        override fun hashCode(): Int {
            var result = alignment.hashCode()
            result = 31 * result + scrim.hashCode()
            return result
        }
    }

    companion object {
        /** Indicates that the associated pane should be displayed in its full width and height. */
        val Expanded: PaneAdaptedValue = Simple("Expanded")
        /** Indicates that the associated pane should be hidden. */
        val Hidden: PaneAdaptedValue = Simple("Hidden")
    }
}
