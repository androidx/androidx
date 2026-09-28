/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import kotlinx.coroutines.launch

@Sampled
@Composable
fun ReadTextFromClipboardSample() {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    var textFromClipboard by remember { mutableStateOf("") }

    Column {
        Text("Text from Clipboard: '$textFromClipboard'")

        Button(
            onClick = {
                coroutineScope.launch {
                    val clipEntry = clipboard.getClipEntry() ?: return@launch
                    // First, check if the entry contains a text representation: plain text, HTML or
                    // URL
                    if (clipEntry.clipMetadata.hasText()) {
                        // readText() value can be either the plain text, HTML, URL string or null
                        textFromClipboard = clipEntry.readText() ?: "null"
                    }
                }
            }
        ) {
            Text("Read Clipboard")
        }
    }
}

@Sampled
@Composable
fun ReadDistinctRepresentationsFromClipboardSample() {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    var allRepresentationsText by remember { mutableStateOf("") }

    Column {
        Text("Clipboard content: '$allRepresentationsText'")

        Button(
            onClick = {
                coroutineScope.launch {
                    val clipEntry = clipboard.getClipEntry() ?: return@launch

                    allRepresentationsText = buildString {
                        append("Plain text = ")
                        if (clipEntry.clipMetadata.hasPlainText()) {
                            // For example: `compose_logo.png`
                            append(clipEntry.readPlainText() ?: "null")
                        } else {
                            append("N/A")
                        }
                        append("\n")

                        append("HTML = ")
                        if (clipEntry.clipMetadata.hasHtml()) {
                            // For example: `<a href='http://localhost:8080/compose_logo.png'></a>`
                            append(clipEntry.readHtml() ?: "null")
                        } else {
                            append("N/A")
                        }
                        append("\n")

                        append("URL = ")
                        if (clipEntry.clipMetadata.hasUrl()) {
                            // For example: `http://localhost:8080/compose_logo.png`
                            append(clipEntry.readUrl() ?: "null")
                        } else {
                            append("N/A")
                        }
                    }
                }
            }
        ) {
            Text("Read Clipboard")
        }
    }
}

@Sampled
@Composable
fun WritePlainTextToClipboard() {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()

    var copyCounter by remember { mutableIntStateOf(0) }
    val textToCopy = "Copyable Text $copyCounter"

    Column {
        Text("Text to copy: $textToCopy")
        Button(
            onClick = {
                val clipEntry = ClipEntry.withText(plainText = textToCopy)
                coroutineScope.launch {
                    clipboard.setClipEntry(clipEntry)
                }
                copyCounter++
            }
        ) {
            Text("Copy")
        }
    }
}

@Sampled
@Composable
fun WriteHtmlToClipboard() {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()

    var copyCounter by remember { mutableIntStateOf(0) }
    val textToCopy = buildAnnotatedString {
        append("Copy counter = ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append("$copyCounter")
        }
    }

    Column {
        Text(textToCopy)
        Button(
            onClick = {
                val clipEntry =
                    ClipEntry.withText(
                        plainText = textToCopy.text,
                        html = "Copy counter = <b>$copyCounter</b>",
                    )
                coroutineScope.launch {
                    clipboard.setClipEntry(clipEntry)
                }
                copyCounter++
            }
        ) {
            Text("Copy")
        }
    }
}

@Sampled
@Composable
fun WriteUrlToClipboard() {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val url = "https://developer.android.com/jetpack/compose"
    val linkText = "Jetpack Compose"
    val linkHtml = "<a href='$url'>$linkText</a>"

    Column {
        Text("Link to copy: $linkText")
        Button(
            onClick = {
                val clipEntry = ClipEntry.withUrl(url = url, plainText = linkText, html = linkHtml)
                coroutineScope.launch {
                    clipboard.setClipEntry(clipEntry)
                }
            }
        ) {
            Text("Copy link")
        }
    }
}
