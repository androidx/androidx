/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.wear.compose.prototypes.windowinsets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.Text

@Composable
fun MenuScreen(backStack: NavBackStack<NavKey>) {
    val listState = rememberScalingLazyListState()
    ScalingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            ListHeader {
                Text(
                    text = "Wear Windowinsets Prototypes",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            Button(
                onClick = { backStack.add(Screen.GlobalStatusBarSandbox) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Global Status Bar Sandbox") },
            )
        }
        item {
            Button(
                onClick = { backStack.add(Screen.HorizontalPager) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Horizontal Pager") },
            )
        }
        item {
            Button(
                onClick = { backStack.add(Screen.VerticalPager) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Vertical Pager") },
            )
        }
        item {
            Button(
                onClick = { backStack.add(Screen.SelfRenderedSandbox) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Self Rendered Sandbox") },
            )
        }
    }
}

/**
 * Placeholder screen for the Global Status Bar Sandbox demo.
 *
 * TODO: Add Global Status Bar demo content and transition tests later.
 */
@Composable
fun GlobalStatusBarSandboxScreen(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Placeholder Screen")
            Button(onClick = onBack, label = { Text("Back") })
        }
    }
}

/**
 * Placeholder screen for the Horizontal Pager demo.
 *
 * TODO: Add horizontal pager demo content and transitions later.
 */
@Composable
fun HorizontalPagerScreen(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Placeholder Screen")
            Button(onClick = onBack, label = { Text("Back") })
        }
    }
}

/**
 * Placeholder screen for the Vertical Pager demo.
 *
 * TODO: Add vertical pager demo content and transitions later.
 */
@Composable
fun VerticalPagerScreen(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Placeholder Screen")
            Button(onClick = onBack, label = { Text("Back") })
        }
    }
}

/**
 * Placeholder screen for the Self-Rendered Sandbox demo.
 *
 * TODO: Add self-rendered status bar demo content later.
 */
@Composable
fun SelfRenderedSandboxScreen(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Placeholder Screen")
            Button(onClick = onBack, label = { Text("Back") })
        }
    }
}
