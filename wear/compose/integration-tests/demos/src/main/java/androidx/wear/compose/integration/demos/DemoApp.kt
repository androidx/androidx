/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.integration.demos

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.integration.demos.common.ActivityDemo
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.integration.demos.common.Demo
import androidx.compose.integration.demos.common.DemoCategory
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

@Composable
fun DemoApp(
    currentDemo: Demo,
    onNavigateToDemo: (Demo) -> Unit,
) {
    DemoContent(currentDemo, onNavigateToDemo)
}

@Composable
private fun DemoContent(
    currentDemo: Demo,
    onNavigate: (Demo) -> Unit,
) {
    Crossfade(currentDemo) { demo ->
        DisplayDemo(demo, onNavigate)
    }
}

@Composable
private fun DisplayDemo(demo: Demo, onNavigate: (Demo) -> Unit) {
    when (demo) {
        is ActivityDemo<*> -> {
            /* should never get here as activity demos are not added to the backstack*/
        }
        is ComposableDemo -> demo.content()
        is DemoCategory -> DisplayDemoList(demo, onNavigate)
    }
}

@Composable
private fun DisplayDemoList(category: DemoCategory, onNavigate: (Demo) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = category.title,
            style = MaterialTheme.typography.caption1,
            color = Color.White
        )
        Spacer(modifier = Modifier.size(4.dp))
        category.demos.forEach { demo ->
            CompactChip(
                onClick = { onNavigate(demo) },
                colors = ChipDefaults.secondaryChipColors(),
                label = {
                    Text(
                        text = demo.title,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                },
                modifier = Modifier.width(100.dp)
            )
        }
    }
}
