/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer.demos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.xr.glimmer.Card
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.samples.VerticalStackSample
import androidx.xr.glimmer.stack.VerticalStack

internal val StackDemos =
    listOf(
        ComposableDemo("VerticalStack fixed size") { VerticalStackFixedItemSizeDemo() },
        ComposableDemo("VerticalStack varying size") { VerticalStackVaryingItemSizeDemo() },
    )

@Composable
internal fun VerticalStackFixedItemSizeDemo() {
    Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        VerticalStackSample()
    }
}

@Composable
internal fun VerticalStackVaryingItemSizeDemo() {
    Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        VerticalStack(modifier = Modifier.height(300.dp)) {
            items(10) { index ->
                Card(modifier = Modifier.fillMaxHeight(if (index % 2 == 0) 0.5f else 1f)) {
                    Text("Item-$index")
                }
            }
        }
    }
}
