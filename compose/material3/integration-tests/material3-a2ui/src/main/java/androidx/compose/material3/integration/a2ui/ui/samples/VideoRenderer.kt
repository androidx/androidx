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

package androidx.compose.material3.integration.a2ui.ui.samples

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.integration.a2ui.icons.PauseIcon
import androidx.compose.material3.integration.a2ui.icons.PlayArrowIcon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

/**
 * A video renderer using Android's [VideoView] with Material 3 playback controls for
 * [androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1.Video].
 */
@Composable
internal fun VideoRenderer(
    url: String,
    modifier: Modifier = Modifier,
    onError: (Throwable?) -> Unit = {},
) {
    var isPlaying by remember(url) { mutableStateOf(false) }
    var isPrepared by remember(url) { mutableStateOf(false) }
    var hasError by remember(url) { mutableStateOf(false) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var currentPosMs by remember(url) { mutableIntStateOf(0) }
    var durationMs by remember(url) { mutableIntStateOf(0) }

    LaunchedEffect(isPlaying, isPrepared) {
        while (isPlaying && isPrepared) {
            videoViewRef?.let { view ->
                if (view.isPlaying) {
                    currentPosMs = view.currentPosition
                    durationMs = view.duration.coerceAtLeast(1)
                }
            }
            delay(250)
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(16.dp)),
        color = Color.Black,
        shape = RoundedCornerShape(16.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    VideoView(context).apply {
                        setOnPreparedListener { mp ->
                            isPrepared = true
                            hasError = false
                            durationMs = mp.duration.coerceAtLeast(1)
                            mp.isLooping = true
                        }
                        setOnErrorListener { _, what, extra ->
                            hasError = true
                            isPlaying = false
                            onError(
                                RuntimeException("Video playback error (what=$what, extra=$extra)")
                            )
                            true
                        }
                        setOnCompletionListener { isPlaying = false }
                        videoViewRef = this
                    }
                },
                update = { view ->
                    videoViewRef = view
                    if (view.tag != url) {
                        view.tag = url
                        isPrepared = false
                        hasError = false
                        isPlaying = false
                        try {
                            view.setVideoURI(Uri.parse(url))
                        } catch (e: Exception) {
                            hasError = true
                            onError(e)
                        }
                    }
                },
            )

            VideoPlaybackOverlay(
                url = url,
                isPlaying = isPlaying,
                isPrepared = isPrepared,
                hasError = hasError,
                currentPosMs = currentPosMs,
                durationMs = durationMs,
                onTogglePlayPause = {
                    val view = videoViewRef
                    if (view != null && isPrepared) {
                        if (isPlaying) {
                            view.pause()
                            isPlaying = false
                        } else {
                            view.start()
                            isPlaying = true
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun VideoPlaybackOverlay(
    url: String,
    isPlaying: Boolean,
    isPrepared: Boolean,
    hasError: Boolean,
    currentPosMs: Int,
    durationMs: Int,
    onTogglePlayPause: () -> Unit,
) {
    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = if (isPlaying) 0.15f else 0.45f))
                .clickable(onClick = onTogglePlayPause),
        contentAlignment = Alignment.Center,
    ) {
        when {
            hasError -> VideoErrorState()
            !isPrepared -> VideoLoadingState()
            else ->
                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    colors =
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor =
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                ) {
                    Icon(
                        imageVector = if (isPlaying) PauseIcon else PlayArrowIcon,
                        contentDescription = if (isPlaying) "Pause video" else "Play video",
                        modifier = Modifier.size(28.dp),
                    )
                }
        }

        VideoBottomInfoBar(
            url = url,
            isPrepared = isPrepared,
            currentPosMs = currentPosMs,
            durationMs = durationMs,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun VideoErrorState() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "⚠️", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Unable to play video stream",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
        )
    }
}

@Composable
private fun VideoLoadingState() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(
            color = Color.White,
            modifier = Modifier.size(36.dp),
            strokeWidth = 3.dp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Loading video...",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
        )
    }
}

@Composable
private fun VideoBottomInfoBar(
    url: String,
    isPrepared: Boolean,
    currentPosMs: Int,
    durationMs: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = url.substringAfterLast('/').ifEmpty { "Video Stream" },
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (isPrepared && durationMs > 0) {
                Text(
                    text = "${formatDuration(currentPosMs)} / ${formatDuration(durationMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
        if (isPrepared && durationMs > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = {
                    (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.3f),
            )
        }
    }
}

private fun formatDuration(durationMs: Int): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
