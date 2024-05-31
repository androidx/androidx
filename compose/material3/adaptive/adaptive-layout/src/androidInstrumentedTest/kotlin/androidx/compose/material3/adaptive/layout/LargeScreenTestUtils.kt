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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.adaptive.currentWindowSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.toSize

internal fun ComposeContentTestRule.setContentWithSimulatedSize(
    simulatedWidth: Dp,
    simulatedHeight: Dp,
    content: @Composable () -> Unit
) {
    setContent {
        val currentDensity = LocalDensity.current
        val windowSize = with(currentDensity) { currentWindowSize().toSize().toDpSize() }

        val simulatedDensity = Density(currentDensity.density * (windowSize.width / simulatedWidth))
        CompositionLocalProvider(LocalDensity provides simulatedDensity) {
            Box(
                Modifier.fillMaxWidth().height(simulatedHeight),
            ) {
                content()
            }
        }
    }
}
