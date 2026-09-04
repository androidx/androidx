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

package androidx.compose.remote.integration.macrobenchmark.target

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.ReportDrawnWhen
import androidx.activity.compose.setContent
import androidx.collection.mutableIntObjectMapOf
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.remote.core.Limits
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteImage
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteImageBitmap
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.embedded.RcImageLoader
import androidx.compose.remote.player.core.platform.BitmapLoader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Target activity for benchmarking image loading with simulated network latency.
 *
 * Compares the performance of the embedded Compose player ([RcPlayer]) with [RcImageLoader] vs the
 * Java player ([RemoteComposePlayer]) with [BitmapLoader] when loading remote images subject to
 * network latency.
 */
class RemotePlayerImageLoadingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Limits.ENABLE_IMAGE_URLS = true
        val mode = intent.getStringExtra(BENCHMARK_MODE_ARG) ?: MODE_EMBEDDED_PLAYER

        setContent {
            ImageLoadingScreen(mode = mode)
        }
    }

    @Composable
    private fun ImageLoadingScreen(mode: String) {
        val coroutineScope = rememberCoroutineScope()
        var allImagesLoaded by remember {
            mutableStateOf(mode == MODE_JAVA_PLAYER)
        }
        ReportDrawnWhen { allImagesLoaded }

        val imageLoader =
            remember(coroutineScope) {
                LatencySimulatingRcImageLoader(
                    latencyMs = 50L,
                    scope = coroutineScope,
                    onAllImagesLoaded = { allImagesLoaded = true },
                )
            }

        val bitmapLoader = remember { LatencySimulatingBitmapLoader(latencyMs = 50L) }

        var documentBytes by remember { mutableStateOf<ByteArray?>(null) }
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            documentBytes =
                captureSingleRemoteDocument(
                        profile = RcPlatformProfiles.ANDROIDX,
                        context = context,
                    ) {
                        val colors =
                            listOf(
                                "E53935",
                                "D81B60",
                                "8E24AA",
                                "1E88E5",
                                "43A047",
                                "FB8C00",
                            )

                        RemoteColumn(
                            horizontalAlignment = RemoteAlignment.CenterHorizontally,
                            verticalArrangement = RemoteArrangement.Center,
                            modifier =
                                RemoteModifier.fillMaxSize().padding(16.rdp).semantics {
                                    contentDescription = IMAGE_GRID_CONTENT_DESCRIPTION.rs
                                },
                        ) {
                            RemoteText(
                                text = "Image Loading Benchmark".rs,
                                fontSize = 24.rsp,
                                modifier = RemoteModifier.padding(bottom = 16.rdp),
                            )

                            // Row 1
                            RemoteRow(
                                horizontalArrangement = RemoteArrangement.Center,
                                modifier = RemoteModifier.padding(bottom = 12.rdp),
                            ) {
                                for (i in 0 until 3) {
                                    val hex = colors[i]
                                    val bitmap =
                                        rememberNamedRemoteImageBitmap(
                                            name = "img_$i",
                                            url = "color://$hex",
                                        )
                                    RemoteImage(
                                        remoteBitmap = bitmap,
                                        contentDescription = "Image $i".rs,
                                        modifier =
                                            RemoteModifier.size(100.rdp, 100.rdp).padding(4.rdp),
                                    )
                                }
                            }

                            // Row 2
                            RemoteRow(horizontalArrangement = RemoteArrangement.Center) {
                                for (i in 3 until 6) {
                                    val hex = colors[i]
                                    val bitmap =
                                        rememberNamedRemoteImageBitmap(
                                            name = "img_$i",
                                            url = "color://$hex",
                                        )
                                    RemoteImage(
                                        remoteBitmap = bitmap,
                                        contentDescription = "Image $i".rs,
                                        modifier =
                                            RemoteModifier.size(100.rdp, 100.rdp).padding(4.rdp),
                                    )
                                }
                            }
                        }
                    }
                    .bytes
        }

        documentBytes?.let { bytes ->
            RemotePlayerHost(
                playerMode = mode,
                remoteDocumentBytes = bytes,
                modifier = Modifier.fillMaxSize(),
                imageLoader = imageLoader,
                bitmapLoader = bitmapLoader,
            )
        }
    }

    private class LatencySimulatingRcImageLoader(
        private val latencyMs: Long,
        private val scope: CoroutineScope,
        private val onAllImagesLoaded: () -> Unit,
    ) : RcImageLoader {
        private val states = mutableIntObjectMapOf<MutableState<Drawable?>>()
        private val loadedCount = AtomicInteger(0)
        private val requestedCount = AtomicInteger(0)

        override fun loadImage(bitmapId: Int): State<Drawable?> =
            states.getOrPut(bitmapId) {
                val state = mutableStateOf<Drawable?>(null)
                requestedCount.incrementAndGet()
                scope.launch(Dispatchers.IO) {
                    delay(latencyMs)
                    val bitmap =
                        Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply {
                            eraseColor(
                                AndroidColor.HSVToColor(
                                    floatArrayOf((bitmapId * 60f) % 360f, 0.8f, 0.9f)
                                )
                            )
                        }
                    val drawable = BitmapDrawable(Resources.getSystem(), bitmap)
                    withContext(Dispatchers.Main) {
                        state.value = drawable
                        if (loadedCount.incrementAndGet() >= 6) {
                            onAllImagesLoaded()
                        }
                    }
                }
                state
            }
    }

    private class LatencySimulatingBitmapLoader(private val latencyMs: Long) : BitmapLoader {
        @Suppress("BanThreadSleep") // Simulating network latency on background I/O thread
        override fun loadBitmap(url: String): InputStream {
            Thread.sleep(latencyMs)
            val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            val color =
                try {
                    if (url.startsWith("color://")) {
                        AndroidColor.parseColor("#" + url.removePrefix("color://"))
                    } else {
                        AndroidColor.BLUE
                    }
                } catch (e: Exception) {
                    AndroidColor.BLUE
                }
            bitmap.eraseColor(color)
            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)
            return ByteArrayInputStream(bos.toByteArray())
        }
    }
}
