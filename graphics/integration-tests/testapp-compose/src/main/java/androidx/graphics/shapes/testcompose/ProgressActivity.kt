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

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.fragment.app.FragmentActivity
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.pillStar
import androidx.graphics.shapes.toPath
import kotlin.math.min
import kotlinx.coroutines.launch

/**
 * This is a simple app showing how to use the startLocation parameter to change where a pillStar
 * shape starts from, then animating a stroked path from that starting point.
 */
class ProgressActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent(parent = null) { MaterialTheme { ProgressHolder() } }
    }
}

@Composable
fun ProgressHolder() {
    val progress = remember { Animatable(1f) }
    val startLocation = remember { mutableFloatStateOf(0f) }
    val pillStar =
        RoundedPolygon.pillStar(
            numVerticesPerRadius = 16,
            width = 1.8f,
            height = .4f,
            rounding = CornerRounding(1f),
            startLocation = startLocation.floatValue
        )
    val scope = rememberCoroutineScope()
    val pathMeasure = PathMeasure()
    val pathSegment = Path()

    Column {
        Slider(
            value = startLocation.floatValue.coerceIn(0f, 1f),
            onValueChange = { startLocation.floatValue = it }
        )
        Box(
            Modifier.clickable { scope.launch { doAnimation(progress) } }
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    val scale = min(size.width, size.height) / 2
                    val m = Matrix()
                    m.translate(size.width / 2, size.height / 2, 0f)
                    m.scale(scale, scale)

                    val pillStarPath = pillStar.toPath().asComposePath()
                    pillStarPath.transform(m)
                    pathMeasure.setPath(pillStarPath, false)
                    val pathLength = pathMeasure.length
                    pathSegment.rewind()
                    pathMeasure.getSegment(0f, progress.value * pathLength, pathSegment, true)
                    drawPath(path = pathSegment, style = Stroke(20f), color = Color.Blue)
                }
        )
    }
}

private suspend fun doAnimation(progress: Animatable<Float, AnimationVector1D>) {
    progress.snapTo(0f)
    progress.animateTo(
        1f,
        infiniteRepeatable(tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Reverse)
    )
}
