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
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow

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
    fun GridContent(
        innerContentList: List<Pair<String, @RemoteComposable @Composable () -> Unit>>
    ) {
        val chunkedContents = innerContentList.chunked(itemsPerRow)
        RemoteColumn {
            for (row in chunkedContents) {
                RemoteRow {
                    for ((label, content) in row) {
                        RemoteColumn(modifier = RemoteModifier.width(ContainerSize)) {
                            RemoteText(
                                label,
                                modifier = RemoteModifier.width(ContainerSize).height(20.rdp),
                                fontSize = 8.rsp,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 2,
                            )
                            outerContainer(ContainerSize, RemoteModifier) { content() }
                        }
                        RemoteBox(modifier = RemoteModifier.width(Padding))
                    }
                }
                RemoteBox(modifier = RemoteModifier.height(Padding))
            }
        }
    }

    companion object {
        val DefaultContainerSize = 100.rdp

        fun @RemoteComposable @Composable () -> Unit.toInput():
            Pair<String, @RemoteComposable @Composable () -> Unit> =
            Pair((this@toInput as? kotlin.reflect.KCallable<*>)?.name ?: "", this)

        fun List<@RemoteComposable @Composable () -> Unit>.toInput():
            List<Pair<String, @RemoteComposable @Composable () -> Unit>> = this.map { it.toInput() }
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
        contentAlignment = RemoteAlignment.CenterStart,
        content = content,
    )
}
