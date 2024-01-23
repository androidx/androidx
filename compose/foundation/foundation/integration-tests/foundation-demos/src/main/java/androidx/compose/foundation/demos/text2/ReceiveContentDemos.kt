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

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.content.MediaType
import androidx.compose.foundation.content.ReceiveContentListener
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.foundation.content.consumeEach
import androidx.compose.foundation.content.hasMediaType
import androidx.compose.foundation.content.receiveContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReceiveContentBasicTextField2() {
    val state = remember { TextFieldState() }
    val context = LocalContext.current

    Column {
        var descriptionToggle by remember { mutableStateOf(false) }
        Text(
            if (descriptionToggle) Description else "Click to see the description...",
            Modifier
                .padding(8.dp)
                .clickable { descriptionToggle = !descriptionToggle }
        )
        Spacer(Modifier.height(8.dp))
        ReceiveContentShowcase(
            "Everything Consumer",
            MediaType.All, {
                // consume everything here
                null
            },
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            val coroutineScope = rememberCoroutineScope()
            var images by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
            ReceiveContentShowcase(
                title = "Image Consumer",
                hintMediaType = MediaType.Image,
                onReceive = { transferableContent ->
                    if (!transferableContent.hasMediaType(MediaType.Image)) {
                        transferableContent
                    } else {
                        var uri: Uri? = null
                        transferableContent.consumeEach { item ->
                            // only consume this item if we can read
                            if (item.uri != null && uri == null) {
                                uri = item.uri
                                true
                            } else {
                                false
                            }
                        }.also {
                            coroutineScope.launch(Dispatchers.IO) {
                                uri?.readImageBitmap(context)?.let { images = listOf(it) }
                            }
                        }
                    }
                },
                onClear = { images = emptyList() }
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
}

/**
 * Wraps the given [content] composable with a content receiver that shows all the received content.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReceiveContentShowcase(
    title: String,
    hintMediaType: MediaType,
    onReceive: (TransferableContent) -> TransferableContent?,
    modifier: Modifier = Modifier,
    onClear: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val transferableContentState = remember { mutableStateOf<TransferableContent?>(null) }
    val receiveContentState = remember {
        ReceiveContentState(setOf(hintMediaType)) {
            transferableContentState.value = it
            onReceive(it)
        }
    }
    Column(
        modifier
            .dropReceiveContent(receiveContentState)
            .padding(8.dp)
    ) {
        Card(
            Modifier
                .fillMaxWidth()
                .clickable {
                    transferableContentState.value = null
                    onClear()
                },
            elevation = 4.dp,
            backgroundColor = if (receiveContentState.hovering) {
                MaterialTheme.colors.secondary
            } else {
                MaterialTheme.colors.surface
            }
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val transferableContent = transferableContentState.value
                if (transferableContent == null) {
                    Text(
                        "$title - Hasn't received anything yet!",
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
                        if (item.uri != null) KeyValueEntry("Uri", "${item.uri}")
                        if (item.text != null) KeyValueEntry("Text", "${item.text}")
                        if (item.intent != null) KeyValueEntry("Intent", "${item.intent}")
                        Divider(Modifier.fillMaxWidth())
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
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
private fun Uri.readImageBitmap(context: Context): ImageBitmap? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, this))
    } else {
        MediaStore.Images.Media.getBitmap(context.contentResolver, this)
    }.asImageBitmap()
}

@OptIn(ExperimentalFoundationApi::class)
class ReceiveContentState(
    var hintMediaTypes: Set<MediaType>,
    private val onReceive: (TransferableContent) -> TransferableContent?
) {
    internal var hovering by mutableStateOf(false)
    internal var dragging by mutableStateOf(false)

    internal val listener = object : ReceiveContentListener {
        override fun onDragEnter() {
            hovering = true
        }

        override fun onDragEnd() {
            hovering = false
            dragging = false
        }

        override fun onDragStart() {
            dragging = true
        }

        override fun onDragExit() {
            hovering = false
        }

        override fun onReceive(transferableContent: TransferableContent): TransferableContent? {
            dragging = false
            hovering = false
            return this@ReceiveContentState.onReceive(transferableContent)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.dropReceiveContent(
    state: ReceiveContentState
) = composed {
    receiveContent(state.hintMediaTypes, state.listener)
        .background(
            color = if (state.hovering) {
                MaterialTheme.colors.secondary
            } else if (state.dragging) {
                MaterialTheme.colors.primary
            } else {
                MaterialTheme.colors.surface
            },
            shape = RoundedCornerShape(8.dp)
        )
}

private const val Description = "Below setup works as follows;\n" +
    "  - There are 3 nested receiveContent nodes.\n" +
    "  - The outermost one consumes everything that's passed to it.\n" +
    "  - The middle one only consumes image content.\n" +
    "  - The innermost one only consumes text content.\n" +
    "  - BasicTextField2 that's nested the deepest would delegate whatever it receives " +
    "to all 3 parents in order of proximity.\n" +
    "  - Each node shows all the items it receives, not just what it consumes.\n\n" +
    "ReceiveContent works with keyboard, paste, and drag/drop.\n" +
    "Click on any card to clear its internal state.\n" +
    "Click on this description to hide it."
