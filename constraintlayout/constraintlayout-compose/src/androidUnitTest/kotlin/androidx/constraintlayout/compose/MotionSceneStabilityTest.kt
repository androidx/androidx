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

package androidx.constraintlayout.compose

import androidx.compose.ui.unit.dp
import kotlin.test.assertEquals
import org.junit.Test

@OptIn(ExperimentalMotionApi::class)
class MotionSceneStabilityTest {
    @Test
    fun testMotionSceneDslEquality() {
        val scene = MotionScene {
            val box0 = createRefFor("box0")
            val box1 = createRefFor("box1")
            val box2 = createRefFor("box2")
            defaultTransition(
                from = constraintSet {
                    constrain(box0) {
                        width = Dimension.fillToConstraints
                        height = Dimension.value(20.dp)
                        centerVerticallyTo(parent)
                    }
                    constrain(box1) {
                        width = Dimension.fillToConstraints
                        height = Dimension.ratio("2:1")
                        centerVerticallyTo(parent)
                    }
                    constrain(box2) {
                        width = Dimension.fillToConstraints
                        height = Dimension.value(20.dp)
                        centerVerticallyTo(parent)
                    }

                    createHorizontalChain(
                        box0,
                        box1.withHorizontalChainParams(startMargin = 8.dp, endMargin = 8.dp),
                        box2
                    )
                },
                to = constraintSet {
                    constrain(box0) {
                        height = Dimension.fillToConstraints
                        width = Dimension.value(20.dp)
                        centerHorizontallyTo(parent)
                    }
                    constrain(box1) {
                        height = Dimension.fillToConstraints
                        width = Dimension.ratio("2:1")
                        centerHorizontallyTo(parent)
                    }
                    constrain(box2) {
                        height = Dimension.fillToConstraints
                        width = Dimension.value(20.dp)
                        centerHorizontallyTo(parent)
                    }

                    createVerticalChain(
                        box0,
                        box1.withVerticalChainParams(topMargin = 8.dp, bottomMargin = 8.dp),
                        box2
                    )
                }
            ) {
                keyAttributes(box1) {
                    frame(33) {
                        alpha = 0.25f
                    }
                    frame(66) {
                        alpha = 0.8f
                    }
                }
            }
        }

        // Same content, slightly different syntax
        assertEquals(
            expected = scene,
            actual = MotionScene {
                defaultTransition(
                    from = constraintSet {
                        val box0 = createRefFor("box0")
                        val box1 = createRefFor("box1")
                        val box2 = createRefFor("box2")
                        constrain(box0) {
                            width = Dimension.fillToConstraints
                            height = Dimension.value(20.dp)
                            centerVerticallyTo(parent)
                        }
                        constrain(box1) {
                            width = Dimension.fillToConstraints
                            height = Dimension.ratio("2:1")
                            centerVerticallyTo(parent)
                        }
                        constrain(box2) {
                            width = Dimension.fillToConstraints
                            height = Dimension.value(20.dp)
                            centerVerticallyTo(parent)
                        }

                        createHorizontalChain(
                            box0,
                            box1.withChainParams(startMargin = 8.dp, endMargin = 8.dp),
                            box2
                        )
                    },
                    to = constraintSet {
                        val box0 = createRefFor("box0")
                        val box1 = createRefFor("box1")
                        val box2 = createRefFor("box2")
                        constrain(box0) {
                            height = Dimension.fillToConstraints
                            width = Dimension.value(20.dp)
                            centerHorizontallyTo(parent)
                        }
                        constrain(box1) {
                            height = Dimension.fillToConstraints
                            width = Dimension.ratio("2:1")
                            centerHorizontallyTo(parent)
                        }
                        constrain(box2) {
                            height = Dimension.fillToConstraints
                            width = Dimension.value(20.dp)
                            centerHorizontallyTo(parent)
                        }

                        createVerticalChain(
                            box0,
                            box1.withChainParams(topMargin = 8.dp, bottomMargin = 8.dp),
                            box2
                        )
                    }
                ) {
                    val box1 = createRefFor("box1")
                    keyAttributes(box1) {
                        frame(33) {
                            alpha = 0.25f
                        }
                        frame(66) {
                            alpha = 0.8f
                        }
                    }
                }
            }
        )
    }
}
