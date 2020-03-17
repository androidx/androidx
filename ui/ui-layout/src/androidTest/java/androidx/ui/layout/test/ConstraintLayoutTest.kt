/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.layout.test

import androidx.compose.Composable
import androidx.test.filters.SmallTest
import androidx.ui.core.LayoutTag
import androidx.ui.core.Ref
import androidx.ui.layout.LayoutDirectionModifier
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.Stack
import androidx.ui.layout.constraintlayout.ConstraintLayout
import androidx.ui.layout.constraintlayout.ConstraintSet
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.ipx
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class ConstraintLayoutTest : LayoutTest() {
    private val boxSize = 100.ipx
    private val offset = 150.ipx

    @Test
    fun testConstraintLayout() {
        val position = Array(3) { Ref<PxPosition>() }
        val size = Array(3) { Ref<IntPxSize>() }
        val countDownLatch = CountDownLatch(3)

        show {
            TestLayout(size, position, countDownLatch)
        }

        assertTrue(countDownLatch.await(1, TimeUnit.SECONDS))
        val root = findAndroidComposeView()
        waitForDraw(root)
        val rootWidth = root.width.ipx
        val rootHeight = root.height.ipx

        assertEquals(
            PxPosition((rootWidth - boxSize) / 2, (rootHeight - boxSize) / 2),
            position[0].value
        )
        assertEquals(
            PxPosition(rootWidth / 2 + offset, (rootHeight - boxSize) / 2 - boxSize),
            position[1].value
        )
        assertEquals(
            PxPosition(offset, rootHeight - boxSize - offset),
            position[2].value
        )
    }

    @Test
    fun testConstraintLayout_rtl() {
        val position = Array(3) { Ref<PxPosition>() }
        val size = Array(3) { Ref<IntPxSize>() }
        val countDownLatch = CountDownLatch(3)

        show {
            Stack(LayoutSize.Fill + LayoutDirectionModifier.Rtl) {
                TestLayout(size, position, countDownLatch)
            }
        }

        assertTrue(countDownLatch.await(1, TimeUnit.SECONDS))
        val root = findAndroidComposeView()
        waitForDraw(root)
        val rootWidth = root.width.ipx
        val rootHeight = root.height.ipx

        assertEquals(
            PxPosition((rootWidth - boxSize) / 2, (rootHeight - boxSize) / 2),
            position[0].value
        )
        assertEquals(
            PxPosition(rootWidth / 2 - offset - boxSize, (rootHeight - boxSize) / 2 - boxSize),
            position[1].value
        )
        assertEquals(
            PxPosition(rootWidth - offset - boxSize, rootHeight - boxSize - offset),
            position[2].value
        )
    }

    @Composable
    private fun TestLayout(
        size: Array<Ref<IntPxSize>>,
        position: Array<Ref<PxPosition>>,
        countDownLatch: CountDownLatch
    ) = with(density) {
        val boxSize = boxSize.toDp()
        ConstraintLayout(
            ConstraintSet {
                val box1 = tag("box1")
                val box2 = tag("box2")
                val box3 = tag("box3")

                box1.center()

                val half = createGuidelineFromLeft(percent = 0.5f)
                box2.apply {
                    left constrainTo half
                    left.margin = offset.toDp()
                    bottom constrainTo box1.top
                }

                box3.apply {
                    left constrainTo parent.left
                    left.margin = offset.toDp()
                    bottom constrainTo parent.bottom
                    bottom.margin = offset.toDp()
                }
            }
        ) {
            Stack(LayoutTag("box1") + LayoutSize(boxSize, boxSize) +
                saveLayoutInfo(size[0], position[0], countDownLatch)
            ) {
            }
            Stack(LayoutTag("box2") + LayoutSize(boxSize, boxSize) +
                saveLayoutInfo(size[1], position[1], countDownLatch)
            ) {
            }
            Stack(LayoutTag("box3") + LayoutSize(boxSize, boxSize) +
                saveLayoutInfo(size[2], position[2], countDownLatch)
            ) {
            }
        }
    }
}