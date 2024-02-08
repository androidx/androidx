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

package androidx.compose.foundation.samples

import android.content.ClipData
import androidx.annotation.Sampled
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.content.MediaType
import androidx.compose.foundation.content.ReceiveContentListener
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.foundation.content.consumeEach
import androidx.compose.foundation.content.hasMediaType
import androidx.compose.foundation.content.receiveContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicTextField2
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

@OptIn(ExperimentalFoundationApi::class)
@Sampled
@Composable
fun ReceiveContentBasicSample() {
    val state = rememberTextFieldState()
    var images by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    Column {
        Row {
            images.forEach {
                Image(bitmap = it, contentDescription = null)
            }
        }
        BasicTextField2(
            state = state,
            modifier = Modifier.receiveContent(setOf(MediaType.Image)) { transferableContent ->
                if (!transferableContent.hasMediaType(MediaType.Image)) {
                    return@receiveContent transferableContent
                }
                val newImages = mutableListOf<ImageBitmap>()
                transferableContent.consumeEach { item ->
                    // only consume this item if we can read an imageBitmap
                    item.readImageBitmap()?.let { newImages += it; true } ?: false
                }.also {
                    images = newImages
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Sampled
@Composable
fun ReceiveContentFullSample() {
    val state = rememberTextFieldState()
    var images by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var dragging by remember { mutableStateOf(false) }
    var hovering by remember { mutableStateOf(false) }
    Column {
        Row {
            images.forEach {
                Image(bitmap = it, contentDescription = null)
            }
        }
        BasicTextField2(
            state = state,
            modifier = Modifier
                .background(
                    when {
                        dragging -> Color.Red
                        hovering -> Color.Green
                        else -> MaterialTheme.colors.background
                    }
                )
                .receiveContent(
                    hintMediaTypes = setOf(MediaType.Image),
                    receiveContentListener = object : ReceiveContentListener {
                        override fun onDragStart() {
                            dragging = true
                        }

                        override fun onDragEnd() {
                            hovering = false
                            dragging = false
                        }

                        override fun onDragEnter() {
                            hovering = true
                        }

                        override fun onDragExit() {
                            hovering = false
                        }

                        override fun onReceive(
                            transferableContent: TransferableContent
                        ): TransferableContent? {
                            if (!transferableContent.hasMediaType(MediaType.Image)) {
                                return transferableContent
                            }
                            val newImages = mutableListOf<ImageBitmap>()
                            return transferableContent
                                .consumeEach { item ->
                                    // only consume this item if we can read an imageBitmap
                                    item
                                        .readImageBitmap()
                                        ?.let { newImages += it; true } ?: false
                                }
                                .also {
                                    images = newImages
                                }
                        }
                    }
                )
        )
    }
}

private fun ClipData.Item.readImageBitmap(): ImageBitmap? =
    TODO("Reads an ImageBitmap from ClipData.Item#uri")
