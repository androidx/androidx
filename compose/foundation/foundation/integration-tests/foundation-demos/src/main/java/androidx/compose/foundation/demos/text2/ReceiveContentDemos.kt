/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.demos.text2

import android.content.ClipData
import android.content.Context
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.content.MediaType
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.foundation.content.consumeEach
import androidx.compose.foundation.content.hasMediaType
import androidx.compose.foundation.content.receiveContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReceiveContentBasicTextField2() {
    val state = remember { TextFieldState() }
    val context = LocalContext.current

    ReceiveContentShowcase(
        "Everything Consumer",
        MediaType.All, {
            // consume everything here
            null
        },
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        var images by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
        ReceiveContentShowcase(
            "Image Consumer",
            MediaType.Image, { transferableContent ->
                if (!transferableContent.hasMediaType(MediaType.Image)) {
                    transferableContent
                } else {
                    val newImages = mutableListOf<ImageBitmap>()
                    transferableContent.consumeEach { item ->
                        // only consume this item if we can read
                        item.readImageBitmap(context)?.let { newImages += it; true } ?: false
                    }.also {
                        images = newImages
                    }
                }
            }
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                images.forEach {
                    Image(it, contentDescription = null, Modifier.size(100.dp))
                }
            }
            ReceiveContentShowcase(
                "Text Consumer",
                MediaType.Text, {
                    it.consumeEach { item ->
                        val text = item.coerceToText(context)
                        // only consume if it has text in it.
                        !text.isNullOrBlank() && item.uri == null
                    }
                }
            ) {
                BasicTextField2(
                    state = state,
                    modifier = demoTextFieldModifiers,
                    textStyle = LocalTextStyle.current
                )
            }
        }
    }
}

/**
 * Wraps the given [content] composable with a content receiver that shows all the received content.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReceiveContentShowcase(
    title: String,
    acceptedMediaType: MediaType,
    onReceive: (TransferableContent) -> TransferableContent?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val transferableContentState = remember { mutableStateOf<TransferableContent?>(null) }
    Column(
        modifier
            .receiveContent(acceptedMediaType) {
                transferableContentState.value = it
                onReceive(it)
            }
            .padding(8.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val transferableContent = transferableContentState.value
                if (transferableContent == null) {
                    Text(
                        "$title - Haven't received anything yet!",
                        style = MaterialTheme.typography.h6
                    )
                } else {
                    Text("$title - Summary", style = MaterialTheme.typography.h6)
                    KeyValueEntry(
                        "Item count",
                        "${transferableContent.clipEntry.clipData.itemCount}"
                    )
                    KeyValueEntry("Source", "${transferableContent.source}")
                    KeyValueEntry(
                        "linkUri",
                        "${transferableContent.platformTransferableContent?.linkUri}"
                    )

                    Text("Items", style = MaterialTheme.typography.h6)

                    for (i in 0 until transferableContent.clipEntry.clipData.itemCount) {
                        val item = transferableContent.clipEntry.clipData.getItemAt(i)
                        KeyValueEntry("Uri", "${item.uri}")
                        KeyValueEntry("Text", "${item.text}")
                        KeyValueEntry("Intent", "${item.intent}")
                    }
                }
            }
        }
        content()
    }
}

@Composable
private fun KeyValueEntry(
    key: String,
    value: String
) {
    Row {
        Text(key, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(8.dp))
        Text(value)
    }
}

@Suppress("ClassVerificationFailure", "DEPRECATION")
private fun ClipData.Item.readImageBitmap(context: Context): ImageBitmap? {
    val imageUri = uri ?: return null
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, imageUri))
    } else {
        MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
    }.asImageBitmap()
}
