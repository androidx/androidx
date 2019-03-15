/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.test.filters.SmallTest
import androidx.ui.core.Constraints
import androidx.ui.core.OnPositioned
import androidx.ui.core.PxPosition
import androidx.ui.core.PxSize
import androidx.ui.core.dp
import androidx.ui.core.ipx
import androidx.ui.core.px
import androidx.ui.core.toPx
import androidx.ui.core.withDensity
import androidx.ui.layout.Center
import androidx.ui.layout.ConstrainedBox
import androidx.ui.layout.Container
import androidx.ui.layout.EdgeInsets
import androidx.ui.layout.Padding
import com.google.r4a.Composable
import com.google.r4a.composer
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class PaddingTest : LayoutTest() {
    @Test
    fun testPaddingIsApplied() = withDensity(density) {
        val size = 50.dp.toIntPx()
        val padding = 10.dp
        val paddingPx = padding.toIntPx()

        val drawLatch = CountDownLatch(1)
        var childSize = PxSize(-1.px, -1.px)
        var childPosition = PxPosition(-1.px, -1.px)
        show @Composable {
            <Center>
                <ConstrainedBox additionalConstraints=Constraints.tightConstraints(size, size)>
                    <Padding padding=EdgeInsets(padding)>
                        <Container
                            padding=null
                            color=null
                            alignment=null
                            margin=null
                            constraints=null
                            width=null
                            height=null>
                            <OnPositioned onPositioned={ coordinates ->
                                childSize = coordinates.size
                                childPosition =
                                    coordinates.localToGlobal(PxPosition(0.px, 0.px))
                                drawLatch.countDown()
                            } />
                        </Container>
                    </Padding>
                </ConstrainedBox>
            </Center>
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidCraneView()
        waitForDraw(root)

        val innerSize = (size - paddingPx * 2)
        assertEquals(PxSize(innerSize, innerSize), childSize)
        val left = ((root.width.ipx - size) / 2) + paddingPx
        val top = ((root.height.ipx - size) / 2) + paddingPx
        assertEquals(
            PxPosition(left.toPx(), top.toPx()),
            childPosition
        )
    }

    @Test
    fun testPaddingIsApplied_withDifferentInsets() = withDensity(density) {
        val size = 50.dp.toIntPx()
        val padding = EdgeInsets(10.dp, 15.dp, 20.dp, 30.dp)

        val drawLatch = CountDownLatch(1)
        var childSize = PxSize(-1.px, -1.px)
        var childPosition = PxPosition(-1.px, -1.px)
        show @Composable {
            <Center>
                <ConstrainedBox additionalConstraints=Constraints.tightConstraints(size, size)>
                    <Padding padding>
                        <Container
                            padding=null
                            color=null
                            alignment=null
                            margin=null
                            constraints=null
                            width=null
                            height=null>
                            <OnPositioned onPositioned={ coordinates ->
                                childSize = coordinates.size
                                childPosition =
                                    coordinates.localToGlobal(PxPosition(0.px, 0.px))
                                drawLatch.countDown()
                            } />
                        </Container>
                    </Padding>
                </ConstrainedBox>
            </Center>
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidCraneView()
        waitForDraw(root)

        val paddingLeft = padding.left.toIntPx()
        val paddingRight = padding.right.toIntPx()
        val paddingTop = padding.top.toIntPx()
        val paddingBottom = padding.bottom.toIntPx()
        assertEquals(
            PxSize(
                size - paddingLeft - paddingRight,
                size - paddingTop - paddingBottom
            ),
            childSize
        )
        val left = ((root.width.ipx - size) / 2) + paddingLeft
        val top = ((root.height.ipx - size) / 2) + paddingTop
        assertEquals(
            PxPosition(left.toPx(), top.toPx()),
            childPosition
        )
    }

    @Test
    fun testPadding_withInsufficientSpace() = withDensity(density) {
        val size = 50.dp.toIntPx()
        val padding = 30.dp
        val paddingPx = padding.toIntPx()

        val drawLatch = CountDownLatch(1)
        var childSize = PxSize(-1.px, -1.px)
        var childPosition = PxPosition(-1.px, -1.px)
        show @Composable {
            <Center>
                <ConstrainedBox additionalConstraints=Constraints.tightConstraints(size, size)>
                    <Padding padding=EdgeInsets(padding)>
                        <Container
                            padding=null
                            color=null
                            alignment=null
                            margin=null
                            constraints=null
                            width=null
                            height=null>
                            <OnPositioned onPositioned={ coordinates ->
                                childSize = coordinates.size
                                childPosition = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                                drawLatch.countDown()
                            } />
                        </Container>
                    </Padding>
                </ConstrainedBox>
            </Center>
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidCraneView()
        waitForDraw(root)

        assertEquals(PxSize(0.px, 0.px), childSize)
        val left = ((root.width.ipx - size) / 2) + paddingPx
        val top = ((root.height.ipx - size) / 2) + paddingPx
        assertEquals(PxPosition(left.toPx(), top.toPx()), childPosition)
    }
}
