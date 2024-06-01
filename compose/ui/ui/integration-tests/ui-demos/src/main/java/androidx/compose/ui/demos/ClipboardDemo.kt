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

package androidx.compose.ui.demos

import android.content.ClipData
import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.firstUriOrNull
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ClipboardDemo() {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var selectedUri: Uri? by remember { mutableStateOf(null) }
    var pastedUri: Uri? by remember { mutableStateOf(null) }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                selectedUri = uri
            }
        }

    Box(Modifier.fillMaxSize().padding(16.dp)) {
        Column {
            Text(
                "First load an image from local files.\n" +
                    "Copy option will be available when a photo is selected."
            )

            if (selectedUri != null) {
                UriImage(uri = selectedUri!!)
                Button(
                    onClick = {
                        clipboardManager.setClip(
                            ClipData.newUri(context.contentResolver, "Image", selectedUri!!)
                                .toClipEntry()
                        )
                    }
                ) {
                    Text("Copy")
                }
            } else {
                ImagePlaceholder()
            }
            Button(
                onClick = {
                    launcher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            ) {
                Text("Load")
            }

            if (pastedUri != null) {
                UriImage(uri = pastedUri!!)
            } else {
                ImagePlaceholder()
            }

            Text("Or paste an image if one is available in the clipboard.")

            Button(
                onClick = {
                    if (clipboardManager.hasImage()) {
                        pastedUri = clipboardManager.getClip()?.firstUriOrNull()
                    } else {
                        Toast.makeText(
                                context,
                                "There are no images in the clipboard",
                                Toast.LENGTH_SHORT
                            )
                            .show()
                    }
                }
            ) {
                Text("Paste")
            }

            Button(
                onClick = {
                    selectedUri = null
                    pastedUri = null
                }
            ) {
                Text("Clear")
            }
        }
    }
}

@Composable
fun UriImage(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val imageBitmap by
        produceState<ImageBitmap?>(null, uri) {
            withContext(Dispatchers.IO) { value = uri.readImageBitmap(context) }
        }

    imageBitmap?.let {
        Image(bitmap = it, contentDescription = "image from file", modifier = modifier)
    }
}

@Composable
fun ImagePlaceholder() {
    Box(
        modifier =
            Modifier.size(200.dp).drawBehind {
                drawRect(Color.Blue, alpha = 0.4f, style = Stroke(width = 1.dp.toPx()))
                drawLine(
                    Color.Blue,
                    alpha = 0.4f,
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
                drawLine(
                    Color.Blue,
                    alpha = 0.4f,
                    start = Offset(size.width, 0f),
                    end = Offset(0f, size.height)
                )
            }
    ) {
        Text("No Image", Modifier.align(Alignment.Center))
    }
}

fun ClipboardManager.hasImage(): Boolean {
    val clipMetadata = getClip()?.clipMetadata ?: return false

    return clipMetadata.clipDescription.hasMimeType("image/*")
}

@Suppress("ClassVerificationFailure", "DEPRECATION")
fun Uri.readImageBitmap(context: Context): ImageBitmap {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, this))
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, this)
        }
        .asImageBitmap()
}
