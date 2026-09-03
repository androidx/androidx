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
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText

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
                onClick = { backStack.add(Screen.ScaffoldDemo) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Scaffold Demo") },
            )
        }
        item {
            Button(
                onClick = { backStack.add(Screen.NoScaffoldDemo) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("No Scaffold Demo") },
            )
        }
    }
}

@Composable
fun ScaffoldDemoScreen(onBack: () -> Unit) {
    // Scaffold is local ONLY to this screen
    AppScaffold(timeText = { TimeText() }) {
        val scrollState = rememberScalingLazyListState()
        ScreenScaffold(scrollState = scrollState) { contentPadding ->
            ScalingLazyColumn(
                state = scrollState,
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize(),
            ) {
                item { Text("Inside Scaffold Screen") }
                item { Button(onClick = onBack, label = { Text("Back") }) }
                items(20) { index -> Text("Scroll Item $index") }
            }
        }
    }
}

@Composable
fun NoScaffoldDemoScreen(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No Scaffold Screen")
            Button(onClick = onBack, label = { Text("Back") })
        }
    }
}
