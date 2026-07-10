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

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.player.view.RemoteComposePlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import kotlinx.coroutines.delay

@Suppress(
    "RestrictedApiAndroidX",
    "COMPOSE_APPLIER_CALL_MISMATCH",
    "COMPOSE_APPLIER_PARAMETER_MISMATCH",
)
class DumperActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        RideShareHolder.init(resources)

        val demoName = intent.getStringExtra("demo")
        val mode = intent.getStringExtra("mode") ?: "remote"
        val width = intent.getIntExtra("width", 480)
        val height = intent.getIntExtra("height", 480)
        val density = intent.getIntExtra("density", 160)
        val fontScale = intent.getFloatExtra("fontScale", 2.0f)
        val fps = intent.getIntExtra("fps", 30)
        val durationSeconds = intent.getLongExtra("duration", 30L)
        val durationMillis = durationSeconds * 1000L
        val bitrate = intent.getIntExtra("bitrate", 200000)

        val isVideo =
            mode.equals("video", ignoreCase = true) ||
                mode.equals("remote_video", ignoreCase = true)

        val name = demoName ?: "Clock"
        val sample = AllSamples.find { it.name.equals(name, ignoreCase = true) }
        if (sample == null) {
            Log.e("DumperActivity", "Unknown demo: $name. Available: ${AllSamples.map { it.name }}")
            finish()
            return
        }

        Log.i(
            "DumperActivity",
            "Starting: demo=$name, mode=$mode, ${width}x${height}, durationSeconds=$durationSeconds",
        )

        setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isVideo) {
                    val result =
                        mediaH264Preview(
                            context = this@DumperActivity,
                            sample = sample,
                            width = width,
                            height = height,
                            durationMillis = durationMillis,
                            fps = fps,
                            bitrate = bitrate,
                            densityDpi = density,
                            fontScale = fontScale,
                        )
                    LaunchedEffect(result) {
                        result?.let {
                            Log.i("DumperActivity", "Video dump finished: ${it.filePath}")
                            finish()
                        }
                    }
                } else {
                    RemoteComposePlayAndDump(
                        sample = sample,
                        width = width,
                        height = height,
                        durationMillis = durationMillis,
                        densityDpi = density,
                        fontScale = fontScale,
                        onFinished = { outputPath ->
                            Log.i(
                                "DumperActivity",
                                "Remote compose play and dump finished: $outputPath",
                            )
                            finish()
                        },
                    )
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
fun RemoteComposePlayAndDump(
    sample: DumperSample,
    width: Int,
    height: Int,
    durationMillis: Long,
    densityDpi: Int = LocalConfiguration.current.densityDpi,
    fontScale: Float = LocalConfiguration.current.fontScale,
    onFinished: (String) -> Unit = {},
) {
    val context = LocalContext.current
    var bytes by remember { mutableStateOf<ByteArray?>(null) }
    var outputPath by remember { mutableStateOf<String?>(null) }

    val creationDisplayInfo =
        remember(sample, width, height, densityDpi, fontScale) {
            RemoteCreationDisplayInfo(width, height, densityDpi, fontScale)
        }

    LaunchedEffect(sample) {
        val generatedBytes =
            when (sample) {
                is DumperSample.ComposableSample -> {
                    val doc =
                        captureSingleRemoteDocument(
                            creationDisplayInfo = creationDisplayInfo,
                            context = context,
                        ) {
                            sample.content()
                        }
                    doc.bytes
                }
                is DumperSample.Context -> {
                    val rcContext = sample.getContext()
                    val wireBuffer = rcContext.buffer.buffer
                    wireBuffer.getBuffer().copyOf(wireBuffer.size())
                }
                is DumperSample.FileSample -> {
                    sample.file.readBytes()
                }
            }

        val file = File(context.cacheDir, "${sample.name}_${width}x${height}.rc")
        file.writeBytes(generatedBytes)
        Log.i("DumperActivity", "Remote compose dump finished: ${file.absolutePath}")

        bytes = generatedBytes
        outputPath = file.absolutePath
    }

    Box(modifier = Modifier.fillMaxSize()) {
        bytes?.let { activeBytes ->
            AndroidView(
                factory = { ctx ->
                    RemoteComposePlayer(ctx).apply {
                        setShaderControl { true }
                        setDocument(activeBytes)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            LaunchedEffect(durationMillis) {
                delay(durationMillis)
                Log.i("DumperActivity", "Remote compose play finished after $durationMillis ms")
                onFinished(outputPath ?: "")
            }
        }
    }
}
