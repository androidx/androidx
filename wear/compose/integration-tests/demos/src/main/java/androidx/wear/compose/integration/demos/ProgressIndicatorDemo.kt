package androidx.wear.compose.integration.demos

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
public fun IndeterminateProgress() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
public fun ProgressWithCustomAngles() {
    var startAngle by remember { mutableFloatStateOf(292.5f) }
    var endAngle by remember { mutableFloatStateOf(247.5f) }
    var progress by remember { mutableFloatStateOf(0.5f) }
    val animatedProgress: Float by animateFloatAsState(targetValue = progress)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ScalingLazyColumnWithRSB(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Text("Start Angle:$startAngle") }
            item {
                DefaultInlineSlider(
                    value = startAngle,
                    onValueChange = { startAngle = it },
                    steps = 15,
                    valueRange = 0f..360f
                )
            }
            item { Text("End Angle:$endAngle") }
            item {
                DefaultInlineSlider(
                    value = endAngle,
                    onValueChange = { endAngle = it },
                    steps = 15,
                    valueRange = 0f..360f
                )
            }
            item { Text("Progress:$progress") }
            item {
                DefaultInlineSlider(
                    value = progress,
                    onValueChange = { progress = it },
                    steps = 4,
                    valueRange = 0f..1f
                )
            }
        }
        CircularProgressIndicator(
            startAngle = startAngle,
            endAngle = endAngle,
            progress = animatedProgress,
            modifier = Modifier.fillMaxSize().padding(all = 1.dp)
        )
    }
}

@Composable
public fun ProgressWithMedia() {
    var status by remember { mutableStateOf(Status.Loading) }

    val playerState = remember { PlayerState() }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(50.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        scope.launch {
                            playerState.prevSong()
                            startPlaying(playerState.progress)
                        }
                    },
                    colors = ButtonDefaults.iconButtonColors()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_skip_previous),
                        contentDescription = "Previous",
                        modifier = Modifier.size(24.dp).wrapContentSize(align = Alignment.Center),
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Box(modifier = Modifier.size(ButtonDefaults.LargeButtonSize)) {
                    Button(
                        onClick = {
                            when (status) {
                                Status.Stopped -> {
                                    status = Status.Playing
                                    scope.launch { startPlaying(playerState.progress) }
                                }
                                Status.Playing -> {
                                    status = Status.Stopped
                                    scope.launch { pausePlaying(playerState.progress) }
                                }
                                Status.Loading -> {
                                    status = Status.Playing
                                    scope.launch {
                                        playerState.spinToNext()
                                        startPlaying(playerState.progress)
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.secondaryButtonColors(),
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        Icon(
                            painter = painterResource(
                                if (status == Status.Playing) R.drawable.ic_pause
                                else R.drawable.ic_play
                            ),
                            contentDescription = "Play",
                            modifier = Modifier.size(24.dp)
                                .wrapContentSize(align = Alignment.Center),
                        )
                    }
                    if (status == Status.Loading)
                        CircularProgressIndicator(
                            modifier = Modifier.fillMaxSize(),
                            startAngle = playerState.startOffsetAngle,
                            indicatorColor = MaterialTheme.colors.secondary,
                            trackColor = MaterialTheme.colors.onBackground.copy(alpha = 0.1f),
                            strokeWidth = 4.dp
                        )
                    else
                        CircularProgressIndicator(
                            progress = playerState.progress.value,
                            modifier = Modifier.fillMaxSize(),
                            startAngle = playerState.startOffsetAngle,
                            indicatorColor = MaterialTheme.colors.secondary,
                            trackColor = MaterialTheme.colors.onBackground.copy(alpha = 0.1f),
                            strokeWidth = 4.dp
                        )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Button(
                    onClick = {
                        scope.launch {
                            playerState.nextSong()
                            startPlaying(playerState.progress)
                        }
                    },
                    colors = ButtonDefaults.iconButtonColors()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_skip_next),
                        contentDescription = "Next",
                        modifier = Modifier.size(24.dp).wrapContentSize(align = Alignment.Center),
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            CompactChip(
                modifier = Modifier.width(110.dp),
                label = { Text(text = "Start loading") },
                onClick = {
                    status = Status.Loading
                })
        }
    }
}

/**
 * This is an example of possible smooth transition between indeterminate progress state
 * to determinate
 */
@Composable
public fun TransformingCustomProgressIndicator() {
    val transformState = remember { TransformingState() }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(ButtonDefaults.LargeButtonSize)) {
                    Button(
                        onClick = {
                            scope.launch { transformState.stopPlayingStartLoading() }
                        },
                        colors = ButtonDefaults.secondaryButtonColors(),
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_play),
                            contentDescription = "Play",
                            modifier = Modifier.size(24.dp)
                                .wrapContentSize(align = Alignment.Center),
                        )
                    }
                    CircularProgressIndicator(
                        progress = transformState.progress.value,
                        modifier = Modifier.fillMaxSize(),
                        startAngle = transformState.startOffsetAngle,
                        indicatorColor = MaterialTheme.colors.secondary,
                        trackColor = MaterialTheme.colors.onBackground.copy(alpha = 0.1f),
                        strokeWidth = 4.dp
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Button(
                    onClick = {
                        scope.launch {
                            transformState.stopLoadingStartPlaying()
                            startPlaying(transformState.progress)
                        }
                    },
                    colors = ButtonDefaults.iconButtonColors()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_skip_next),
                        contentDescription = "Next",
                        modifier = Modifier.size(24.dp).wrapContentSize(align = Alignment.Center),
                    )
                }
            }
        }
    }
}

private enum class Status {
    Stopped,
    Playing,
    Loading
}

private suspend fun startPlaying(progress: Animatable<Float, AnimationVector1D>) {
    progress.animateTo(
        1f, TweenSpec(durationMillis = 20000, easing = LinearEasing)
    )
}

private suspend fun pausePlaying(progress: Animatable<Float, AnimationVector1D>) {
    progress.snapTo(progress.value)
}

private class PlayerState {
    val progress = Animatable(0f)
    private val _startOffsetAngle = Animatable(0f)

    val startOffsetAngle: Float
        get() = _startOffsetAngle.value

    suspend fun nextSong() {
        if (progress.value < 0.05) {
            _startOffsetAngle.snapTo(-90f)
            progress.animateTo(0f, TweenSpec(easing = LinearOutSlowInEasing))
        } else {
            spinToNext()
        }
    }

    suspend fun prevSong() {
        _startOffsetAngle.snapTo(-90f)
        progress.animateTo(0f, TweenSpec(easing = LinearOutSlowInEasing))
    }

    suspend fun spinToNext() {
        coroutineScope {
            launch {
                _startOffsetAngle.animateTo(
                    270f,
                    animationSpec = TweenSpec(easing = LinearOutSlowInEasing)
                )
                _startOffsetAngle.snapTo(-90f)
            }
            launch {
                progress.animateTo(
                    0f,
                    animationSpec = TweenSpec(easing = LinearOutSlowInEasing)
                )
            }
        }
    }
}

private class TransformingState {
    val progress = Animatable(0f)
    private val _startAngle = Animatable(0f)

    val startOffsetAngle: Float
        get() = _startAngle.value

    suspend fun stopLoadingStartPlaying() {
        // Finalize loading animation by bringing progress to 0 and startAngle into closest top
        // position -270 or 630 degrees. After that startAngle snapped to -90 to
        // make animation easier
        coroutineScope {
            launch {
                progress.animateTo(
                    0f,
                    TweenSpec(easing = LinearOutSlowInEasing)
                )
            }
            launch {
                _startAngle.animateTo(
                    targetValue = if (_startAngle.value < 180) 270f else 630f,
                    animationSpec = TweenSpec(easing = LinearOutSlowInEasing)
                )
                _startAngle.snapTo(-90f)
            }
        }
        // after that start a mock progress animation
        progress.animateTo(1f, TweenSpec(easing = LinearEasing, durationMillis = 20000))
    }

    suspend fun stopPlayingStartLoading() {
        // If progress is less than 5% - then we return progress to 0 and snap startAngle to -90
        // If progress > 5%, then progress animates to 0 and startAngle to 270 degrees
        // so that it'll make a full circle

        if (progress.value < 0.05) {
            _startAngle.snapTo(-90f)
            progress.animateTo(0f, TweenSpec(easing = LinearOutSlowInEasing))
        } else {
            spinToNext()
        }
        // after that an infinite animation starts, 2 in parallel -
        // 1) changes startAngle from -90 to 630 ( making 2 full circles ), then repeats.
        // 2) changes progress from 0 to 0.8 ( leaving some space for a gap),
        // then reverts and then repeats again.
        coroutineScope {
            launch {
                progress.animateTo(
                    targetValue = 0.8f,
                    animationSpec = InfiniteRepeatableSpec(
                        animation = TweenSpec(
                            delay = 200,
                            easing = CubicBezierEasing(0.4f, 0.0f, 0.7f, 1.0f),
                            // This duration is set as a half of startAngle duration because
                            // it reverts
                            durationMillis = 800
                        ),
                        repeatMode = RepeatMode.Reverse,
                        initialStartOffset = StartOffset(200)
                    )
                )
            }
            launch {
                _startAngle.animateTo(
                    targetValue = 630f,
                    animationSpec = InfiniteRepeatableSpec(
                        animation = TweenSpec(
                            delay = 400,
                            easing = CubicBezierEasing(0.7f, 0.0f, 0.75f, 1.0f),
                            durationMillis = 1600
                        ),
                        repeatMode = RepeatMode.Restart
                    )
                )
            }
        }
    }

    suspend fun spinToNext() {
        coroutineScope {
            launch {
                _startAngle.animateTo(
                    270f,
                    animationSpec = TweenSpec(easing = LinearOutSlowInEasing)
                )
                _startAngle.snapTo(-90f)
            }
            launch {
                progress.animateTo(
                    0f,
                    animationSpec = TweenSpec(easing = LinearOutSlowInEasing)
                )
            }
        }
    }
}
