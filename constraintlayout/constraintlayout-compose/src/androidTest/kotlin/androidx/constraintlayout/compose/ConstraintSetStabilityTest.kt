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
import kotlin.test.assertNotEquals
import org.junit.Test

class ConstraintSetStabilityTest {
    @Test
    fun testConstraintSetDslEquality() {
        val constraintSet = ConstraintSet {
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
                box1.withHorizontalChainParams(startMargin = 8.dp, endMargin = 8.dp),
                box2
            )
        }

        // Instance should be equivalent to the re-declaration
        assertEquals(
            expected = constraintSet,
            actual = ConstraintSet {
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
            }
        )

        // Different order in chain, should not be equal
        assertNotEquals(
            illegal = constraintSet,
            actual = ConstraintSet {
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
                    box2,
                    box1.withChainParams(startMargin = 8.dp, endMargin = 8.dp),
                    box0
                )
            }
        )
    }

    @Test
    fun testInheritedConstraintSetEquality() {
        val constraintSetA = ConstraintSet {
            val box0 = createRefFor("box0")
            val box1 = createRefFor("box1")
            val box2 = createRefFor("box2")

            constrain(box0) {
                width = Dimension.fillToConstraints
                height = Dimension.value(20.dp)
                centerVerticallyTo(parent)

                alpha = 0f
                horizontalBias = 0f
                verticalBias = 0f
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
        }

        val constraintSetB = ConstraintSet(constraintSetA) {
            val box0 = createRefFor("box0")
            // It's ok to set Chain params before creating the chain
            val box1 = createRefFor("box1").withChainParams(startMargin = 8.dp, endMargin = 8.dp)
            val box2 = createRefFor("box2")

            createHorizontalChain(
                box0,
                box1,
                box2
            )

            // Chain params set chain declarations should have no effect
            box0.withChainParams(1.dp, 2.dp, 3.dp, 4.dp)

            constrain(box0) {
                // Set values back to default, the set should not be ignored, otherwise they would
                // stay at 0f
                alpha = 1f
                horizontalBias = 0.5f
                verticalBias = 0.5f
            }
        }

        // ConstraintSetB should internally reflect the inherited constraints, and so, should be
        // equal to a redeclaration with ALL constraints
        assertEquals(
            expected = constraintSetB,
            actual = ConstraintSet {
                val box0 = createRefFor("box0")
                val box1 = createRefFor("box1")
                val box2 = createRefFor("box2")

                constrain(box0) {
                    width = Dimension.fillToConstraints
                    height = Dimension.value(20.dp)
                    centerVerticallyTo(parent)

                    // We also need to re-set the values here, its absence in the underlying
                    // structure will cause a failure, we are checking for equality, not equivalency
                    alpha = 1f
                    horizontalBias = 0.5f
                    verticalBias = 0.5f
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
            }
        )
    }
}
