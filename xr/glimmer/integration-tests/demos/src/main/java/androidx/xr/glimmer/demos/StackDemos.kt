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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.xr.glimmer.Card
import androidx.xr.glimmer.CardDefaults
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.LocalTextStyle
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.samples.VerticalStackSample
import androidx.xr.glimmer.stack.VerticalStack

internal val StackDemos =
    listOf(
        ComposableDemo("VerticalStack fixed size") { VerticalStackFixedItemSizeDemo() },
        ComposableDemo("VerticalStack varying size") { VerticalStackVaryingItemSizeDemo() },
        ComposableDemo("VerticalStack various content") { VerticalStackVariousContentDemo() },
    )

@Composable
internal fun VerticalStackFixedItemSizeDemo() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        VerticalStackSample()
    }
}

@Composable
internal fun VerticalStackVaryingItemSizeDemo() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Vertical stack with items of different sizes.
        VerticalStack(modifier = Modifier.fillMaxWidth().height(364.dp)) {
            items(10) { index ->
                Card(
                    modifier =
                        Modifier.fillMaxHeight(if (index % 2 == 0) 0.5f else 1f)
                            .itemDecoration(CardDefaults.shape)
                ) {
                    Text(
                        "Item-$index",
                        style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated),
                    )
                }
            }
        }
    }
}

@Composable
internal fun VerticalStackVariousContentDemo() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        VerticalStack(modifier = Modifier.fillMaxWidth().height(364.dp)) {
            item {
                Card(modifier = Modifier.itemDecoration(CardDefaults.shape)) {
                    Text(
                        "This is a card",
                        style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated),
                    )
                }
            }
            item {
                Card(
                    trailingIcon = { Icon(Icons.FavoriteIcon, "Localized description") },
                    modifier = Modifier.itemDecoration(CardDefaults.shape),
                ) {
                    Text(
                        "This is a card with a trailing icon",
                        style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated),
                    )
                }
            }
            item {
                Card(
                    title = {
                        Text(
                            "Title",
                            style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated),
                        )
                    },
                    leadingIcon = { Icon(Icons.FavoriteIcon, "Localized description") },
                    modifier = Modifier.itemDecoration(CardDefaults.shape),
                ) {
                    Text(
                        "This is a card with a title and leading icon",
                        style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated),
                    )
                }
            }
            item {
                Card(modifier = Modifier.itemDecoration(CardDefaults.shape)) {
                    Text(
                        "This is a card with a lot of text that will wrap to multiple lines. The maximum recommend number of lines of text for a card is 10.",
                        overflow = TextOverflow.Ellipsis,
                        style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated),
                    )
                }
            }
            item {
                Card(
                    title = {
                        Text(
                            "Title",
                            style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated),
                        )
                    },
                    subtitle = {
                        Text(
                            "Subtitle",
                            style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated),
                        )
                    },
                    leadingIcon = { Icon(Icons.FavoriteIcon, "Localized description") },
                    modifier = Modifier.itemDecoration(CardDefaults.shape),
                ) {
                    Text(
                        "This is a card with a lot of text that will wrap to multiple lines.",
                        style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated),
                    )
                }
            }
        }
    }
}
