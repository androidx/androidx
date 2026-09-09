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

import android.media.MediaPlayer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.integration.a2ui.icons.PauseIcon
import androidx.compose.material3.integration.a2ui.icons.PlayArrowIcon
import androidx.compose.material3.integration.a2ui.icons.VolumeUpIcon
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

/**
 * An audio player renderer using Android's [MediaPlayer] with Material 3 playback controls for
 * [androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1.AudioPlayer].
 */
@Composable
internal fun AudioPlayerRenderer(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onError: (Throwable?) -> Unit = {},
) {
    var isPlaying by remember(url) { mutableStateOf(false) }
    var isPrepared by remember(url) { mutableStateOf(false) }
    var hasError by remember(url) { mutableStateOf(false) }
    var durationMs by remember(url) { mutableIntStateOf(0) }
    var currentPosMs by remember(url) { mutableIntStateOf(0) }
    var sliderPosition by remember(url) { mutableFloatStateOf(0f) }

    val mediaPlayer = remember(url) { MediaPlayer() }

    DisposableEffect(url) {
        isPrepared = false
        isPlaying = false
        hasError = false
        currentPosMs = 0
        durationMs = 0
        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(url)
            mediaPlayer.setOnPreparedListener { mp ->
                isPrepared = true
                hasError = false
                durationMs = mp.duration.coerceAtLeast(1)
            }
            mediaPlayer.setOnCompletionListener {
                isPlaying = false
                currentPosMs = durationMs
                sliderPosition = 1f
            }
            mediaPlayer.setOnErrorListener { _, what, extra ->
                hasError = true
                isPlaying = false
                onError(RuntimeException("Audio playback error (what=$what, extra=$extra)"))
                true
            }
            mediaPlayer.prepareAsync()
        } catch (e: Exception) {
            hasError = true
            onError(e)
        }

        onDispose {
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.release()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(isPlaying, isPrepared) {
        while (isPlaying && isPrepared) {
            try {
                if (mediaPlayer.isPlaying) {
                    currentPosMs = mediaPlayer.currentPosition
                    if (durationMs > 0) {
                        sliderPosition =
                            (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    }
                }
            } catch (_: Exception) {}
            delay(200.milliseconds)
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            AudioPlayerHeaderRow(
                title =
                    contentDescription?.takeIf { it.isNotBlank() }
                        ?: url.substringAfterLast('/').ifEmpty { "Audio Track" },
                isPlaying = isPlaying,
                isPrepared = isPrepared,
                hasError = hasError,
                currentPosMs = currentPosMs,
                durationMs = durationMs,
                onTogglePlayPause = {
                    if (isPrepared) {
                        if (isPlaying) {
                            mediaPlayer.pause()
                            isPlaying = false
                        } else {
                            mediaPlayer.start()
                            isPlaying = true
                        }
                    }
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            val sliderState = rememberSliderState(value = sliderPosition, trackRange = 0f..1f)
            sliderState.value = sliderPosition
            Slider(
                state = sliderState,
                onValueChange = { newValue ->
                    sliderPosition = newValue
                    if (isPrepared && durationMs > 0) {
                        val seekTarget = (newValue * durationMs).toInt()
                        mediaPlayer.seekTo(seekTarget)
                        currentPosMs = seekTarget
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isPrepared && !hasError,
            )
        }
    }
}

@Composable
private fun AudioPlayerHeaderRow(
    title: String,
    isPlaying: Boolean,
    isPrepared: Boolean,
    hasError: Boolean,
    currentPosMs: Int,
    durationMs: Int,
    onTogglePlayPause: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = VolumeUpIcon,
                    contentDescription = "Audio track",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            val statusText =
                when {
                    hasError -> "Playback error"
                    !isPrepared -> "Buffering audio..."
                    isPlaying ->
                        "Playing • ${formatDuration(currentPosMs)} / ${formatDuration(durationMs)}"
                    else ->
                        "Ready • ${formatDuration(currentPosMs)} / ${formatDuration(durationMs)}"
                }
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color =
                    if (hasError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        FilledIconButton(
            onClick = onTogglePlayPause,
            enabled = isPrepared && !hasError,
            shape = CircleShape,
            modifier = Modifier.size(44.dp),
        ) {
            if (!isPrepared && !hasError) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Icon(
                    imageVector = if (isPlaying) PauseIcon else PlayArrowIcon,
                    contentDescription = if (isPlaying) "Pause audio" else "Play audio",
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

private fun formatDuration(durationMs: Int): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
