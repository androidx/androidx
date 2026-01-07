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

package androidx.compose.remote.creation.compose.test.base

import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Class to provide a single UI composed of many other smaller UIs, displayed on a grid. */
class GridScreenshotUI(
    private val itemsPerRow: Int = 3,
    private val Padding: RemoteDp = 24.rdp,
    private val ContainerSize: RemoteDp = DefaultContainerSize,
    private val outerContainer:
        @Composable
        (size: RemoteDp, modifier: RemoteModifier, content: @Composable () -> Unit) -> Unit =
        ::Container,
) {
    @Composable
    @RemoteComposable
    fun GridContent(innerContentList: List<@RemoteComposable @Composable () -> Unit>) {
        val chunkedContents = innerContentList.chunked(itemsPerRow)
        RemoteColumn {
            for (row in chunkedContents) {
                RemoteRow {
                    for (content in row) {
                        outerContainer(ContainerSize, RemoteModifier) { content() }
                        RemoteBox(modifier = RemoteModifier.width(Padding))
                    }
                }
                RemoteBox(modifier = RemoteModifier.height(Padding))
            }
        }
    }

    companion object {
        val DefaultContainerSize = 100.rdp
    }
}

@Composable
@RemoteComposable
private fun Container(
    size: RemoteDp,
    modifier: RemoteModifier,
    content: @RemoteComposable @Composable () -> Unit,
) {
    RemoteBox(
        modifier = modifier.size(size).background(Color(0xFFCFD8DC)),
        horizontalAlignment = RemoteAlignment.Start,
        verticalArrangement = RemoteArrangement.Center,
        content = content,
    )
}
