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
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.pillStar
import androidx.graphics.shapes.star
import kotlin.math.min
import kotlinx.coroutines.launch

val shapeColor1 = Color.Blue
val shapeColor2 = Color.Red

class SimpleMorph : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent(parent = null) { MaterialTheme { ShapeViewer() } }
    }
}

@Composable
fun ShapeViewer() {
    val shape1 = RoundedPolygon.star(5)
    val shape2 =
        RoundedPolygon.pillStar(
            numVerticesPerRadius = 8,
            width = 2f,
            height = .4f,
            rounding = CornerRounding(.5f)
        )
    val morph = Morph(shape1, shape2)
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Column {
        Row {
            ShapeView(
                shape1,
                modifier =
                    Modifier.weight(1f).aspectRatio(1f).padding(horizontal = 5.dp).clickable {
                        scope.launch { doAnimation(progress, reverse = true) }
                    },
                shapeColor1
            )
            ShapeView(
                shape2,
                modifier =
                    Modifier.weight(1f).aspectRatio(1f).padding(horizontal = 5.dp).clickable {
                        scope.launch { doAnimation(progress) }
                    },
                shapeColor2
            )
        }
        Row {
            MorphView(
                modifier = Modifier.weight(1f).aspectRatio(1f).padding(horizontal = 5.dp),
                morph = morph,
                progress = progress.value
            )
        }
        Row {
            Box {
                val hexagon = remember {
                    RoundedPolygon.star(numVerticesPerRadius = 6, rounding = CornerRounding(0.2f))
                }
                val clip = remember(hexagon) { RoundedPolygonShape(polygon = hexagon) }
                Box(
                    modifier =
                        Modifier.clip(clip)
                            .background(MaterialTheme.colorScheme.secondary)
                            .size(200.dp)
                ) {
                    Text(
                        "Hello Compose",
                        color = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            Box {
                val clip = MorphShape(morph = morph, progress = progress.value)
                Box(
                    modifier =
                        Modifier.clip(clip)
                            .background(MaterialTheme.colorScheme.secondary)
                            .size(200.dp)
                ) {
                    Text(
                        "Hello Compose",
                        color = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
fun ShapeView(shape: RoundedPolygon, modifier: Modifier, color: Color) {
    Box(
        modifier.drawWithContent {
            drawContent()
            drawRoundedPolygon(shape, color)
        }
    )
}

fun DrawScope.drawMorph(morph: Morph, progress: Float) {
    val path = morph.toPath(progress)
    val scale = min(size.width, size.height) / 2
    val translator = Matrix()
    val matrix = Matrix()
    translator.translate(1f * scale, 1f * scale)
    matrix.scale(scale, scale)
    matrix *= translator
    path.transform(matrix)
    drawPath(path, SolidColor(lerp(shapeColor1, shapeColor2, progress)))
}

fun DrawScope.drawRoundedPolygon(shape: RoundedPolygon, color: Color) {
    val path = shape.toPath()
    val scale = min(size.width, size.height) / 2
    val translator = Matrix()
    val matrix = Matrix()
    translator.translate(1f * scale, 1f * scale)
    matrix.scale(scale, scale)
    matrix *= translator
    path.transform(matrix)
    drawPath(path, color)
}

@Composable
fun MorphView(morph: Morph, modifier: Modifier = Modifier, progress: Float) {
    Box(
        modifier.drawWithContent {
            drawContent()
            drawMorph(morph, progress)
        }
    )
}

private suspend fun doAnimation(
    progress: Animatable<Float, AnimationVector1D>,
    reverse: Boolean = false
) {
    val startValue = if (reverse) 1f else 0f
    val endValue = if (reverse) 0f else 1f
    progress.snapTo(startValue)
    progress.animateTo(endValue, animationSpec = spring(0.6f, 50f))
}
