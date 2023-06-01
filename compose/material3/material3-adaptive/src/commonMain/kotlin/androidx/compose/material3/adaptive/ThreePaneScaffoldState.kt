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

package androidx.compose.material3.adaptive

/**
 * The adapted state of [ThreePaneScaffold]. It contains each pane's adapted state.
 * [ThreePaneScaffold] will use the adapted states to decide which panes should be displayed
 * and how they should be displayed. With other input parameters of [ThreePaneScaffold] fixed,
 * each possible instance of this class should represent a unique state of [ThreePaneScaffold]
 * and developers can compare two [ThreePaneScaffoldState] to decide if there is a layout structure
 * change.
 *
 * For a Material-opinionated layout, it's suggested to use [calculateThreePaneScaffoldState] to
 * calculate the current scaffold state.
 */
@ExperimentalMaterial3AdaptiveApi
class ThreePaneScaffoldState(
    /** [PaneAdaptedState] of the primary pane of [ThreePaneScaffold]. */
    val primaryPaneAdaptedState: PaneAdaptedState,
    /** [PaneAdaptedState] of the secondary pane of [ThreePaneScaffold]. */
    val secondaryPaneAdaptedState: PaneAdaptedState,
    /** [PaneAdaptedState] of the tertiary pane of [ThreePaneScaffold]. */
    val tertiaryPaneAdaptedState: PaneAdaptedState
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ThreePaneScaffoldState) return false
        if (primaryPaneAdaptedState != other.primaryPaneAdaptedState) return false
        if (secondaryPaneAdaptedState != other.secondaryPaneAdaptedState) return false
        if (tertiaryPaneAdaptedState != other.tertiaryPaneAdaptedState) return false
        return true
    }

    override fun hashCode(): Int {
        var result = primaryPaneAdaptedState.hashCode()
        result = 31 * result + secondaryPaneAdaptedState.hashCode()
        result = 31 * result + tertiaryPaneAdaptedState.hashCode()
        return result
    }
}