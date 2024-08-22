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

package androidx.compose.material3.adaptive.benchmark

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldPaneScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val singlePaneDirective = PaneScaffoldDirective.Default

val dualPaneDirective =
    PaneScaffoldDirective.Default.copy(
        maxHorizontalPartitions = 2,
        horizontalPartitionSpacerSize = 24.dp,
    )

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal abstract class ThreePaneScaffoldTestCase(private val animated: Boolean) :
    LayeredComposeTestCase(), ToggleableTestCase {
    var currentScaffoldDirective by mutableStateOf(singlePaneDirective)
    abstract var currentDestination: ThreePaneScaffoldDestinationItem<Int>

    override fun toggleState() {}

    @Composable
    fun ThreePaneScaffoldPaneScope.TestPane(color: Color) {
        val content = @Composable { Box(modifier = Modifier.fillMaxSize().background(color)) }
        if (animated) {
            AnimatedPane(Modifier) { content() }
        } else {
            content()
        }
    }
}
