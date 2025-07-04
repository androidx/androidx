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

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutBoundsHolder
import androidx.compose.ui.layout.layoutBounds
import androidx.compose.ui.layout.onFirstVisible
import androidx.compose.ui.layout.onVisibilityChanged

@Sampled
@Composable
private fun OnVisibilityChangedAutoplaySample() {
    @Composable
    fun VideoFeed(feedData: List<Video>) {
        LazyColumn {
            items(feedData) { video ->
                VideoRow(
                    video,
                    Modifier.onVisibilityChanged(minDurationMs = 500, minFractionVisible = 1f) {
                        visible ->
                        if (visible) video.play() else video.pause()
                    },
                )
            }
        }
    }
}

@Sampled
@Composable
private fun OnVisibilityChangedAutoplayWithViewportSample() {
    @Composable
    fun VideoFeed(feedData: List<Video>) {
        val viewport = remember { LayoutBoundsHolder() }
        LazyColumn(Modifier.layoutBounds(viewport)) {
            items(feedData) { video ->
                VideoRow(
                    video,
                    Modifier.onVisibilityChanged(
                        minDurationMs = 500,
                        minFractionVisible = 1f,
                        viewportBounds = viewport,
                    ) { visible ->
                        if (visible) video.play() else video.pause()
                    },
                )
            }
        }
    }
}

@Sampled
@Composable
private fun OnVisibilityChangedDurationLoggingSample() {
    @Composable
    fun VideoFeed(feedData: List<Video>, logger: Logger) {
        LazyColumn {
            items(feedData) { video ->
                var startTime by remember { mutableLongStateOf(-1) }
                VideoRow(
                    video,
                    Modifier.onVisibilityChanged(minDurationMs = 500, minFractionVisible = 1f) {
                        visible ->
                        if (visible) {
                            startTime = System.currentTimeMillis()
                        } else if (startTime >= 0) {
                            val durationMs = System.currentTimeMillis() - startTime
                            logger.logImpression(video.id, durationMs)
                            startTime = -1
                        }
                    },
                )
            }
        }
    }
}

@Sampled
@Composable
private fun OnFirstVisibleImpressionLoggingSample() {
    @Composable
    fun VideoFeed(feedData: List<Video>, logger: Logger) {
        LazyColumn {
            items(feedData) { video ->
                VideoRow(
                    video,
                    Modifier.onFirstVisible(minDurationMs = 500, minFractionVisible = 1f) {
                        logger.logImpression(video.id)
                    },
                )
            }
        }
    }
}

@Sampled
@Composable
private fun OnFirstVisibleImpressionLoggingWithViewportSample() {
    @Composable
    fun VideoFeed(feedData: List<Video>, logger: Logger) {
        val viewport = remember { LayoutBoundsHolder() }
        LazyColumn(Modifier.layoutBounds(viewport)) {
            items(feedData) { video ->
                VideoRow(
                    video,
                    Modifier.onFirstVisible(
                        minDurationMs = 500,
                        minFractionVisible = 1f,
                        viewportBounds = viewport,
                    ) {
                        logger.logImpression(video.id)
                    },
                )
            }
        }
    }
}

@Composable fun VideoRow(video: Video, modifier: Modifier = Modifier) {}

class Logger {
    fun logImpression(id: Int) {}

    fun logImpression(id: Int, durationMs: Long) {}
}

class Video(val id: Int) {
    fun play() {}

    fun pause() {}
}
