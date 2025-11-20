/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.integration.macrobenchmark.target

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop

class PaginatedLazyColumnActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialEntries = List(50) { Entry("Item $it") }
        setContent { BenchmarkScreen(initialEntries) }

        launchIdlenessTracking()
    }
}

@Composable
private fun ListRow(entry: Entry) {
    Card(modifier = Modifier.padding(8.dp)) {
        Row {
            Text(text = entry.contents, modifier = Modifier.padding(16.dp))
            Spacer(modifier = Modifier.weight(1f, fill = true))
            Checkbox(checked = false, onCheckedChange = {}, modifier = Modifier.padding(16.dp))
        }
    }
}

@Preview
@Composable
private fun BenchmarkScreen(initialEntries: List<Entry> = List(100) { Entry("Item $it") }) {
    val state = rememberLazyListState()
    var data by remember { mutableStateOf(initialEntries) }
    var showIndicator by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        snapshotFlow { state.layoutInfo.visibleItemsInfo.lastOrNull() }
            .drop(1)
            .collect { lastVisible ->
                val lastVisible = lastVisible ?: return@collect
                if (lastVisible.index == state.layoutInfo.totalItemsCount - 1) {
                    showIndicator = true
                    delay(3_000L) // simulate network request
                    data = data + initialEntries
                    showIndicator = false
                }
            }
    }
    Box {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "IamLazy" },
        ) {
            items(data) { ListRow(it) }
        }

        if (showIndicator) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
            )
        }
    }
}
