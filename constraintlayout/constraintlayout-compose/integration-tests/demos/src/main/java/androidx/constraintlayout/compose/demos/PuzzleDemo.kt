/*
 * Copyright 2023 The Android Open Source Project
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

@file:OptIn(ExperimentalMotionApi::class)

package androidx.constraintlayout.compose.demos

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.compose.Arc
import androidx.constraintlayout.compose.ConstraintLayoutBaseScope
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.ExperimentalMotionApi
import androidx.constraintlayout.compose.MotionLayout
import androidx.constraintlayout.compose.MotionScene
import androidx.constraintlayout.compose.Wrap

/**
 * Shows how to animate moving pieces of a puzzle using MotionLayout.
 *
 * &nbsp;
 *
 * The [PuzzlePiece]s are laid out using the [ConstraintLayoutBaseScope.createFlow] helper.
 *
 * And the animation is achieved by creating two ConstraintSets. One providing ordered IDs to Flow,
 * and the other providing a shuffled list of the same IDs.
 *
 * @see PuzzlePiece
 */
@Preview
@Composable
fun AnimatedPuzzlePiecesDemo() {
    val grid = 5
    val blocks = grid * grid

    var animateToEnd by remember { mutableStateOf(true) }

    val index = remember { Array(blocks) { it }.apply { shuffle() } }
    val refId = remember { Array(blocks) { "W$it" } }

    // Recreate scene when order changes (which is driven by toggling `animateToEnd`)
    val scene = remember(animateToEnd) {
        MotionScene {
            val ordered = refId.map { createRefFor(it) }.toTypedArray()
            val shuffle = index.map { ordered[it] }.toTypedArray()
            val set1 = constraintSet {
                val flow = createFlow(
                    elements = ordered,
                    maxElement = grid,
                    wrapMode = Wrap.Aligned,
                )
                constrain(flow) {
                    centerTo(parent)
                    width = Dimension.ratio("1:1")
                    height = Dimension.ratio("1:1")
                }
                ordered.forEach {
                    constrain(it) {
                        width = Dimension.percent(1f / grid)
                        height = Dimension.ratio("1:1")
                    }
                }
            }
            val set2 = constraintSet {
                val flow = createFlow(
                    elements = shuffle,
                    maxElement = grid,
                    wrapMode = Wrap.Aligned,
                )
                constrain(flow) {
                    centerTo(parent)
                    width = Dimension.ratio("1:1")
                    height = Dimension.ratio("1:1")
                }
                ordered.forEach {
                    constrain(it) {
                        width = Dimension.percent(1f / grid)
                        height = Dimension.ratio("1:1")
                    }
                }
            }
            transition(set1, set2, "default") {
                motionArc = Arc.StartHorizontal
                keyAttributes(*ordered) {
                    frame(40) {
                        // alpha = 0.0f
                        rotationZ = -90f
                        scaleX = 0.1f
                        scaleY = 0.1f
                    }
                    frame(70) {
                        rotationZ = 90f
                        scaleX = 0.1f
                        scaleY = 0.1f
                    }
                }
            }
        }
    }

    val progress by animateFloatAsState(
        targetValue = if (animateToEnd) 1f else 0f,
        animationSpec = tween(800)
    )

    MotionLayout(
        motionScene = scene,
        modifier = Modifier
            .clickable {
                animateToEnd = !animateToEnd
                index.shuffle()
            }
            .background(Color.Red)
            .fillMaxSize(),
        progress = progress
    ) {
        val painter = rememberVectorPainter(image = Icons.Default.Face)
        index.forEachIndexed { i, id ->
            PuzzlePiece(
                x = i % grid,
                y = i / grid,
                gridSize = grid,
                painter = painter,
                modifier = Modifier.layoutId(refId[id])
            )
        }
    }
}

/**
 * Composable that displays a fragment of the given surface (provided through [painter]) based on
 * the given position ([x], [y]) of a square grid of size [gridSize].
 */
@Composable
fun PuzzlePiece(
    x: Int,
    y: Int,
    gridSize: Int,
    painter: Painter,
    modifier: Modifier = Modifier
) {
    Canvas(modifier.fillMaxSize()) {
        clipRect {
            translate(
                left = -x * size.width,
                top = -y * size.height
            ) {
                with(painter) {
                    draw(size.times(gridSize.toFloat()))
                }
            }
        }
    }
}
