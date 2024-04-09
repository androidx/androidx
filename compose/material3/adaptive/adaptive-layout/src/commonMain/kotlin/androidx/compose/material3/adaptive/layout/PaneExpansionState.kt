/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

@ExperimentalMaterial3AdaptiveApi
@Immutable
internal class PaneExpansionState {
    private var firstPaneWidthState by mutableIntStateOf(UnspecifiedWidth)
    private var firstPanePercentageState by mutableFloatStateOf(Float.NaN)

    var firstPaneWidth: Int
        set(value) {
            firstPanePercentageState = Float.NaN
            firstPaneWidthState = value
        }
        get() = firstPaneWidthState

    var firstPanePercentage: Float
        set(value) {
            require(value in 0f..1f) { "Percentage value needs to be in [0, 1]" }
            firstPaneWidthState = UnspecifiedWidth
            firstPanePercentageState = value
        }
        get() = firstPanePercentageState

    fun isUnspecified(): Boolean =
        firstPaneWidthState == UnspecifiedWidth && firstPanePercentage.isNaN()

    companion object {
        const val UnspecifiedWidth = -1
    }
}
