/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.graphics.shapes.testcompose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.DurationBasedAnimationSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.TransformResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
private fun MorphView(
    morph: Morph,
    progress: Animatable<Float, AnimationVector1D>,
    modifier: Modifier = Modifier,
    fillColor: Color = MaterialTheme.colorScheme.primary,
    isDebug: Boolean = false,
    stroked: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier.fillMaxSize()) {
        Box(
            modifier =
                modifier.aspectRatio(1f).fillMaxSize().align(Alignment.Center).drawWithContent {
                    drawContent()
                    val path = morph.toPath(progress.value)
                    fitToViewport(path, morph.getBounds(), size)
                    if (isDebug) {
                        val scale = size.minDimension
                        drawPath(path, fillColor, style = Stroke(2f))
                        morph.forEachCubic(progress.value) { cubic ->
                            cubic.transform { x, y -> TransformResult(x * scale, y * scale) }
                            debugDrawCubic(cubic, scheme)
                        }
                    } else {
                        val style = if (stroked) Stroke(size.width / 10f) else Fill
                        drawPath(path, fillColor, style = style)
                    }
                }
        )
    }
}

@Composable
fun AnimatedMorphView(
    shapes: List<RoundedPolygon>,
    selectedStartShape: Int,
    selectedEndShape: Int,
    baseAnimation: DurationBasedAnimationSpec<Float> = tween(1400),
) {
    var selectedDisplayIndex by remember { mutableIntStateOf(0) }
    val debug = selectedDisplayIndex != 0
    val morphed =
        remember(selectedStartShape, selectedEndShape, debug) {
            Morph(shapes[selectedStartShape], shapes[selectedEndShape])
        }

    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Column {
        AnimatedMorphViewHeader(selectedDisplayIndex) { index -> selectedDisplayIndex = index }

        AnimationControls(progress, scope, baseAnimation)

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            MorphView(morphed, progress, isDebug = debug)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedMorphViewHeader(selectedIndex: Int, onSelectedModeIndexChanged: (Int) -> Unit) {
    val options = listOf("Output", "Debug")

    TopAppBar(
        title = { Text("Morph Preview") },
        actions = {
            SingleChoiceSegmentedButtonRow {
                options.forEachIndexed { index, label ->
                    SegmentedButton(
                        shape =
                            SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        onClick = { onSelectedModeIndexChanged(index) },
                        selected = index == selectedIndex,
                        icon = {},
                    ) {
                        Text(label)
                    }
                }
            }
        },
    )
}

@Composable
private fun AnimationControls(
    progress: Animatable<Float, AnimationVector1D>,
    scope: CoroutineScope,
    baseAnimation: DurationBasedAnimationSpec<Float>,
) {
    val selectedColors = IconButtonDefaults.filledIconButtonColors()
    val unselectedColors = IconButtonDefaults.iconButtonColors()

    var playBackwards by remember { mutableStateOf(false) }
    var isRepeat by remember { mutableStateOf(false) }

    Row(
        Modifier.padding(horizontal = 5.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        IconButton(
            onClick = {
                scope.launch { restartOrPause(progress, isRepeat, playBackwards, baseAnimation) }
            }
        ) {
            if (animationCanBeStopped(progress, isRepeat)) {
                Icon(Icons.Default.Pause, contentDescription = "Pause Animation")
            } else {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play Animation")
            }
        }

        Slider(
            value = progress.value.coerceIn(0f, 1f),
            onValueChange = { scope.launch { progress.snapTo(it) } },
            Modifier.fillMaxWidth(0.75f),
        )

        IconButton(
            onClick = {
                scope.launch {
                    playBackwards = !playBackwards
                    restartAnimation(progress, isRepeat, playBackwards, baseAnimation)
                }
            },
            colors = if (playBackwards) selectedColors else unselectedColors,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(Icons.AutoMirrored.Default.Undo, contentDescription = "Play Backwards")
        }

        IconButton(
            onClick = {
                scope.launch {
                    isRepeat = !isRepeat
                    if (!isRepeat) {
                        scope.launch { progress.stop() }
                    } else {
                        restartAnimation(progress, true, playBackwards, baseAnimation)
                    }
                }
            },
            colors = if (isRepeat) selectedColors else unselectedColors,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(Icons.Default.Repeat, contentDescription = "Repeat Animation")
        }
    }
}

private suspend fun restartAnimation(
    progress: Animatable<Float, AnimationVector1D>,
    isRepeat: Boolean,
    isReverse: Boolean,
    baseAnimation: DurationBasedAnimationSpec<Float>,
) {
    val startValue = if (isReverse) 1f else 0f
    val endValue = if (isReverse) 0f else 1f

    val animationSpec: AnimationSpec<Float> =
        if (isRepeat)
            infiniteRepeatable(
                animation = baseAnimation,
                repeatMode = if (isReverse) RepeatMode.Reverse else RepeatMode.Restart,
            )
        else spring(dampingRatio = 0.65f, stiffness = 50f)

    progress.snapTo(startValue)
    progress.animateTo(endValue, animationSpec = animationSpec)
}

private suspend fun restartOrPause(
    progress: Animatable<Float, AnimationVector1D>,
    isRepeat: Boolean,
    isReverse: Boolean,
    baseAnimation: DurationBasedAnimationSpec<Float>,
) {
    if (animationCanBeStopped(progress, isRepeat)) {
        progress.stop()
    } else {
        restartAnimation(progress, isRepeat, isReverse, baseAnimation)
    }
}

private fun animationCanBeStopped(
    progress: Animatable<Float, AnimationVector1D>,
    isRepeat: Boolean,
): Boolean {
    return isRepeat && progress.isRunning
}
