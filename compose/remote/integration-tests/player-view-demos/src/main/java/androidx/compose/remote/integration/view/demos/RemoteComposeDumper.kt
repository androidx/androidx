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

package androidx.compose.remote.integration.view.demos

import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.v2.captureSingleRemoteDocumentV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import java.io.ByteArrayOutputStream
import java.io.File

private const val TAG = "RemoteComposeDumper"

private class ReusableByteArrayStream(initialSize: Int) : ByteArrayOutputStream(initialSize) {
    fun getInternalBuffer(): ByteArray = buf

    fun ensureCapacity(size: Int) {
        if (size > buf.size) {
            val newSize = maxOf(buf.size * 2, size)
            buf = buf.copyOf(newSize)
        }
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
fun RemoteComposeDumper(
    sample: DumperSample,
    width: Int,
    height: Int,
    onFinished: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val config = LocalConfiguration.current

    val stream = remember { ReusableByteArrayStream(1024 * 1024) }
    var isCaptureFinished by remember { mutableStateOf(false) }

    var currentDataLength by remember { mutableIntStateOf(0) }

    val creationDisplayInfo =
        remember(sample, width, height) {
            RemoteCreationDisplayInfo(width, height, config.densityDpi)
        }
    val virtualDisplay = rememberVirtualDisplay(creationDisplayInfo)

    when (sample) {
        is DumperSample.ComposableSample -> {
            if (!isCaptureFinished) {
                LaunchedEffect(sample) {
                    val doc =
                        captureSingleRemoteDocumentV2(
                            creationDisplayInfo = creationDisplayInfo,
                            context = context,
                        ) {
                            sample.content()
                        }
                    stream.ensureCapacity(doc.bytes.size)
                    doc.bytes.copyInto(stream.getInternalBuffer(), 0, 0, doc.bytes.size)

                    val file = File(context.cacheDir, "${sample.name}_${width}x${height}.rc")
                    file.writeBytes(stream.getInternalBuffer().copyOf(doc.bytes.size))

                    currentDataLength = doc.bytes.size
                    isCaptureFinished = true // Triggers Composable removal
                    onFinished(file.absolutePath)
                }
            }
        }
        is DumperSample.Context -> {
            LaunchedEffect(sample) {
                val rcContext = sample.getContext()
                val buffer = rcContext.buffer.buffer.buffer
                val bufferSize = rcContext.buffer.buffer.size()

                stream.ensureCapacity(bufferSize)
                buffer.copyInto(stream.getInternalBuffer(), 0, 0, bufferSize)

                val file = File(context.cacheDir, "${sample.name}_${width}x${height}.rc")
                file.writeBytes(stream.getInternalBuffer().copyOf(bufferSize))

                currentDataLength = bufferSize
                isCaptureFinished = true
                onFinished(file.absolutePath)
            }
        }
    }
}
