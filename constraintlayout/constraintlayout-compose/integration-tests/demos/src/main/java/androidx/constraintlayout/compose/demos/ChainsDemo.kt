/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.constraintlayout.compose.demos

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintLayoutBaseScope
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.ConstraintSetScope
import androidx.constraintlayout.compose.Dimension

/**
 * Shows the usage of `animateChanges = true` with a chain that changes orientation.
 *
 * Also shown here, usage of [ConstraintLayoutBaseScope.withChainParams], [Dimension.ratio] and
 * [ConstraintSetScope.createRefsFor].
 */
@Preview
@Composable
fun ChainsAnimatedOrientationDemo() {
    val boxColors = listOf(Color.Red, Color.Blue, Color.Green)
    var isHorizontal by remember { mutableStateOf(true) }

    Column(Modifier.fillMaxSize()) {
        ConstraintLayout(
            constraintSet = ConstraintSet {
                // Create multiple references using destructuring declaration
                val (box0, box1, box2) = createRefsFor("box0", "box1", "box2")

                // Assign Chain element margins with `withChainParams`
                box1.withChainParams(8.dp, 8.dp, 8.dp, 8.dp)

                // When State value of `isHorizontal` changes, ConstraintLayout will automatically
                // animate ot the resulting ConstraintSet
                if (isHorizontal) {
                    constrain(box0, box1, box2) {
                        width = Dimension.fillToConstraints
                        height = Dimension.value(20.dp)
                        centerVerticallyTo(parent)
                    }
                    constrain(box1) {
                        // Override height to be a ratio
                        height = Dimension.ratio("2:1")
                    }

                    createHorizontalChain(box0, box1, box2)
                } else {
                    constrain(box0, box1, box2) {
                        width = Dimension.value(20.dp)
                        height = Dimension.fillToConstraints
                        centerHorizontallyTo(parent)
                    }
                    constrain(box1) {
                        // Override width to be a ratio
                        width = Dimension.ratio("2:1")
                    }

                    createVerticalChain(box0, box1, box2)
                }
            },
            animateChanges = true, // Set to true, to automatically animate on ConstraintSet changes
            animationSpec = tween(800),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f, true)
        ) {
            boxColors.forEachIndexed { index, color ->
                Box(
                    modifier = Modifier
                        .layoutId("box$index")
                        .background(color)
                )
            }
        }
        Button(onClick = { isHorizontal = !isHorizontal }) {
            Text(text = "Toggle Orientation")
        }
    }
}

@Preview
@Composable
fun ChainsAnimatedOrientationDemo1() {
    val boxColors = listOf(Color.Red, Color.Blue, Color.Green)
    var isHorizontal by remember { mutableStateOf(true) }

    Column(Modifier.fillMaxSize()) {
        ConstraintLayout(
            animateChanges = true, // Set to true, to automatically animate on ConstraintSet changes
            animationSpec = tween(800),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f, true)
        ) {
            val (box0, box1, box2) = createRefs()

            if (isHorizontal) {
                createHorizontalChain(
                    box0,
                    box1.withChainParams(8.dp, 8.dp, 8.dp, 8.dp),
                    box2
                )
            } else {
                createVerticalChain(
                    box0,
                    box1.withChainParams(8.dp, 8.dp, 8.dp, 8.dp),
                    box2
                )
            }

            Box(
                modifier = Modifier
                    .constrainAs(box0) {
                        if (isHorizontal) {
                            width = Dimension.fillToConstraints
                            height = Dimension.value(20.dp)
                            centerVerticallyTo(parent)
                        } else {
                            width = Dimension.value(20.dp)
                            height = Dimension.fillToConstraints
                            centerHorizontallyTo(parent)
                        }
                    }
                    .background(boxColors[0])
            )
            Box(
                modifier = Modifier
                    .constrainAs(box1) {
                        if (isHorizontal) {
                            width = Dimension.fillToConstraints
                            height = Dimension.ratio("2:1")
                            centerVerticallyTo(parent)
                        } else {
                            width = Dimension.ratio("2:1")
                            height = Dimension.fillToConstraints
                            centerHorizontallyTo(parent)
                        }
                    }
                    .background(boxColors[1])
            )
            Box(
                modifier = Modifier
                    .constrainAs(box2) {
                        if (isHorizontal) {
                            width = Dimension.fillToConstraints
                            height = Dimension.value(20.dp)
                            centerVerticallyTo(parent)
                        } else {
                            width = Dimension.value(20.dp)
                            height = Dimension.fillToConstraints
                            centerHorizontallyTo(parent)
                        }
                    }
                    .background(boxColors[2])
            )
        }
        Button(onClick = { isHorizontal = !isHorizontal }) {
            Text(text = "Toggle Orientation")
        }
    }
}
