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

import androidx.compose.runtime.Immutable

/**
 * The adapted state of [ThreePaneScaffold]. It contains each pane's adapted state.
 * [ThreePaneScaffold] will use the adapted states to decide which panes should be displayed
 * and how they should be displayed. With other input parameters of [ThreePaneScaffold] fixed,
 * each possible instance of this class should represent a unique state of [ThreePaneScaffold]
 * and developers can compare two [ThreePaneScaffoldValue] to decide if there is a layout structure
 * change.
 *
 * For a Material-opinionated layout, it's suggested to use [calculateThreePaneScaffoldState] to
 * calculate the current scaffold state.
 *
 * @param primary [PaneAdaptedValue] of the primary pane of [ThreePaneScaffold]
 * @param secondary [PaneAdaptedValue] of the secondary pane of [ThreePaneScaffold]
 * @param tertiary [PaneAdaptedValue] of the tertiary pane of [ThreePaneScaffold]
 */
@ExperimentalMaterial3AdaptiveApi
@Immutable
class ThreePaneScaffoldValue(
    val primary: PaneAdaptedValue,
    val secondary: PaneAdaptedValue,
    val tertiary: PaneAdaptedValue
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ThreePaneScaffoldValue) return false
        if (primary != other.primary) return false
        if (secondary != other.secondary) return false
        if (tertiary != other.tertiary) return false
        return true
    }

    override fun hashCode(): Int {
        var result = primary.hashCode()
        result = 31 * result + secondary.hashCode()
        result = 31 * result + tertiary.hashCode()
        return result
    }
}