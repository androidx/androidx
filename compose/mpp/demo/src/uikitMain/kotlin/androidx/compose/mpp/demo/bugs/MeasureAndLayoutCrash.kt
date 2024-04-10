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

package androidx.compose.mpp.demo.bugs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.mpp.demo.Screen
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun RowItem(index: Int) {
    Row(modifier = Modifier.padding(end = 30.dp)) {
        var state by remember(index) { mutableStateOf<Unit?>(null) }

        LaunchedEffect(index) {
            delay((100..200).random().toLong())
            state = Unit
        }
        state?.let {
            Text("Item $index")
        }
    }
}

@Composable
fun RowItem(wrapInBox: Boolean, index: Int) {
    if (wrapInBox) {
        Box {
            RowItem(index)
        }
    } else {
        RowItem(index)
    }
}

val MeasureAndLayoutCrash = Screen.Example("MeasureAndLayoutCrash") {
    val lazyColumnState = rememberLazyListState()
    var enableCrash by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.safeContentPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = enableCrash,
                onCheckedChange = { enableCrash = it },
                modifier = Modifier.padding(16.dp),
            )

            Text("Enable crash")
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                state = lazyColumnState,
            ) {
                items(1000) { index ->
                    RowItem(enableCrash, index)
                }
            }
        }
    }
}
