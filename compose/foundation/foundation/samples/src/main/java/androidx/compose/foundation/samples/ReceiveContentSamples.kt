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
import androidx.compose.foundation.content.MediaType
import androidx.compose.foundation.content.consumeEach
import androidx.compose.foundation.content.hasMediaType
import androidx.compose.foundation.content.receiveContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
            modifier = Modifier.receiveContent(MediaType.Image) { transferableContent ->
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

private fun ClipData.Item.readImageBitmap(): ImageBitmap? =
    TODO("Reads an ImageBitmap from ClipData.Item#uri")
