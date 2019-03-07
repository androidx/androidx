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
import androidx.ui.core.OnPositioned
import androidx.ui.core.PxPosition
import androidx.ui.core.PxSize
import androidx.ui.core.div
import androidx.ui.core.dp
import androidx.ui.core.minus
import androidx.ui.core.plus
import androidx.ui.core.px
import androidx.ui.core.round
import androidx.ui.core.times
import androidx.ui.core.toDp
import androidx.ui.core.toIntPx
import androidx.ui.core.toPx
import androidx.ui.core.unaryMinus
import androidx.ui.layout.Center
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.CrossAxisAlignment
import androidx.ui.layout.FlexColumn
import androidx.ui.layout.FlexRow
import androidx.ui.layout.Row
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
class FlexTest : LayoutTest() {
    @Test
    fun testRow() {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx(density)

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOf(PxSize(-1.px, -1.px), PxSize(-1.px, -1.px))
        val childPosition = arrayOf(PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px))
        show @Composable {
            <Center>
                <Row>
                    <Container width=sizeDp height=sizeDp>
                        <OnPositioned> coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        </OnPositioned>
                    </Container>

                    <Container width=(sizeDp * 2) height=(sizeDp * 2)>
                        <OnPositioned> coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        </OnPositioned>
                    </Container>
                </Row>
            </Center>
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidCraneView()
        waitForDraw(root)

        assertEquals(PxSize(size, size), childSize[0])
        assertEquals(
            PxSize((sizeDp.toPx(density) * 2).round(), (sizeDp.toPx(density) * 2).round()),
            childSize[1]
        )
        assertEquals(
            PxPosition(0.px, (root.height.px / 2 - (size.toPx() - 1.px) / 2).round().toPx()),
            childPosition[0]
        )
        assertEquals(
            PxPosition(size.toPx(), (root.height.px / 2 - size.toPx()).round().toPx()),
            childPosition[1]
        )
    }

    @Test
    fun testRowFlex_withExpandedChildren() {
        val heightDp = 50.dp
        val childrenHeight = 50.dp.toIntPx(density)

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOf(PxSize(-1.px, -1.px), PxSize(-1.px, -1.px))
        val childPosition = arrayOf(PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px))
        show @Composable {
            <Center>
                <FlexRow>
                    val widthDp = 50.px.toDp(density)

                    expanded(flex=1f) {
                        <Container width=widthDp height=heightDp>
                            <OnPositioned> coordinates ->
                                childSize[0] = coordinates.size
                                childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                                drawLatch.countDown()
                            </OnPositioned>
                        </Container>
                    }

                    expanded(flex=2f) {
                        <Container width=widthDp height=(heightDp * 2)>
                            <OnPositioned> coordinates ->
                                childSize[1] = coordinates.size
                                childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                                drawLatch.countDown()
                            </OnPositioned>
                        </Container>
                    }
                </FlexRow>
            </Center>
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidCraneView()
        waitForDraw(root)

        assertEquals(PxSize(root.width.px / 3, childrenHeight.toPx()), childSize[0])
        assertEquals(
            PxSize(2 * root.width.px / 3, (heightDp.toPx(density) * 2).round().toPx()),
            childSize[1]
        )
        assertEquals(
            PxPosition(0.px,
                (root.height.px / 2 - (childrenHeight.toPx() - 1.px) / 2).round().toPx()),
            childPosition[0]
        )
        assertEquals(
            PxPosition((root.width.px / 3).round().toPx(),
                (root.height.px / 2).round().toPx() - childrenHeight.toPx()),
            childPosition[1]
        )
    }

    @Test
    fun testRowFlex_withFlexibleChildren() {
        val childrenWidthDp = 50.dp
        val childrenWidth = childrenWidthDp.toIntPx(density)
        val childrenHeightDp = 50.dp
        val childrenHeight = childrenHeightDp.toIntPx(density)

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOf(PxSize(-1.px, -1.px), PxSize(-1.px, -1.px))
        val childPosition = arrayOf(PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px))
        show @Composable {
            <Center>
                <FlexRow>
                    flexible(flex=1f) {
                        <Container width=childrenWidthDp height=childrenHeightDp>
                            <OnPositioned> coordinates ->
                                childSize[0] = coordinates.size
                                childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                                drawLatch.countDown()
                            </OnPositioned>
                        </Container>
                    }

                    flexible(flex=2f) {
                        <Container width=childrenWidthDp height=(childrenHeightDp * 2)>
                            <OnPositioned> coordinates ->
                                childSize[1] = coordinates.size
                                childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                                drawLatch.countDown()
                            </OnPositioned>
                        </Container>
                    }
                </FlexRow>
            </Center>
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidCraneView()
        waitForDraw(root)

        assertEquals(PxSize(childrenWidth.toPx(), childrenHeight.toPx()), childSize[0])
        assertEquals(
            PxSize(childrenWidth.toPx(), (childrenHeightDp.toPx(density) * 2).round().toPx()),
            childSize[1]
        )
        assertEquals(
            PxPosition(0.px, ((root.height.px - childrenHeight.toPx() + 1.px) / 2).round().toPx()),
            childPosition[0]
        )
        assertEquals(
            PxPosition(childrenWidth.toPx(),
                (root.height.px / 2 - childrenHeight.toPx()).round().toPx()),
            childPosition[1]
        )
    }

    @Test
    fun testColumn() {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx(density)

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOf(PxSize(-1.px, -1.px), PxSize(-1.px, -1.px))
        val childPosition = arrayOf(PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px))
        show @Composable {
            <Center>
                <Column>
                    <Container width=sizeDp height=sizeDp>
                        <OnPositioned> coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        </OnPositioned>
                    </Container>
                    <Container width=(sizeDp * 2) height=(sizeDp * 2)>
                        <OnPositioned> coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        </OnPositioned>
                    </Container>
                </Column>
            </Center>
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidCraneView()
        waitForDraw(root)

        assertEquals(PxSize(size, size), childSize[0])
        assertEquals(
            PxSize((sizeDp.toPx(density) * 2).round(), (sizeDp.toPx(density) * 2).round()),
            childSize[1]
        )
        assertEquals(
            PxPosition((root.width.px / 2 - size.toPx() / 2).round().toPx(), 0.px),
            childPosition[0]
        )
        assertEquals(
            PxPosition((root.width.px / 2 - size.toPx()).round().toPx(), size.toPx()),
            childPosition[1]
        )
    }

    @Test
    fun testColumnFlex_withExpandedChildren() {
        val widthDp = 50.dp
        val childrenWidth = widthDp.toIntPx(density)

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOf(PxSize(-1.px, -1.px), PxSize(-1.px, -1.px))
        val childPosition = arrayOf(PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px))
        show @Composable {
            <Center>
                <FlexColumn>
                    val heightDp = 50.px.toDp(density)

                    expanded(flex=1f) {
                        <Container width=widthDp height=heightDp>
                            <OnPositioned> coordinates ->
                                childSize[0] = coordinates.size
                                childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                                drawLatch.countDown()
                            </OnPositioned>
                        </Container>
                    }

                    expanded(flex=2f) {
                        <Container width=(widthDp * 2) height=heightDp>
                            <OnPositioned> coordinates ->
                                childSize[1] = coordinates.size
                                childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                                drawLatch.countDown()
                            </OnPositioned>
                        </Container>
                    }
                </FlexColumn>
            </Center>
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidCraneView()
        waitForDraw(root)

        assertEquals(
            PxSize(childrenWidth.toPx(), (root.height.px / 3).round().toPx()),
            childSize[0]
        )
        assertEquals(
            PxSize((widthDp.toPx(density) * 2).round(), (2 * root.height.px / 3).round()),
            childSize[1]
        )
        assertEquals(
            PxPosition((root.width.px / 2 - childrenWidth.toPx() / 2).round().toPx(), 0.px),
            childPosition[0]
        )
        assertEquals(
            PxPosition((root.width.px / 2 - childrenWidth.toPx()).round().toPx(),
                (root.height.px / 3).round().toPx()),
            childPosition[1]
        )
    }

    @Test
    fun testColumnFlex_withFlexibleChildren() {
        val childrenWidthDp = 50.dp
        val childrenWidth = childrenWidthDp.toIntPx(density)
        val childrenHeightDp = 50.dp
        val childrenHeight = childrenHeightDp.toIntPx(density)

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOf(PxSize(-1.px, -1.px), PxSize(-1.px, -1.px))
        val childPosition = arrayOf(PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px))
        show @Composable {
            <Center>
                <FlexColumn>
                    flexible(flex=1f) {
                        <Container width=childrenWidthDp height=childrenHeightDp>
                            <OnPositioned> coordinates ->
                                childSize[0] = coordinates.size
                                childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                                drawLatch.countDown()
                            </OnPositioned>
                        </Container>
                    }

                    flexible(flex=2f) {
                        <Container width=(childrenWidthDp * 2) height=childrenHeightDp>
                            <OnPositioned> coordinates ->
                                childSize[1] = coordinates.size
                                childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                                drawLatch.countDown()
                            </OnPositioned>
                        </Container>
                    }
                </FlexColumn>
            </Center>
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidCraneView()
        waitForDraw(root)

        assertEquals(PxSize(childrenWidth.toPx(), childrenHeight.toPx()), childSize[0])
        assertEquals(
            PxSize((childrenWidthDp.toPx(density) * 2).round(), childrenHeight),
            childSize[1]
        )
        assertEquals(
            PxPosition((root.width.px / 2 - childrenWidth.toPx() / 2).round().toPx(), 0.px),
            childPosition[0]
        )
        assertEquals(
            PxPosition((root.width.px / 2 - childrenWidth.toPx()).round().toPx(),
                childrenHeight.toPx()),
            childPosition[1]
        )
    }

    @Test
    fun testRow_withStartCrossAxisAlignment() {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx(density)

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOf(PxSize(-1.px, -1.px), PxSize(-1.px, -1.px))
        val childPosition = arrayOf(PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px))
        show @Composable {
            <Center>
                <Row crossAxisAlignment=CrossAxisAlignment.Start>
                    <Container width=sizeDp height=sizeDp>
                        <OnPositioned> coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        </OnPositioned>
                    </Container>

                    <Container width=(sizeDp * 2) height=(sizeDp * 2)>
                        <OnPositioned> coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        </OnPositioned>
                    </Container>
                </Row>
            </Center>
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidCraneView()
        waitForDraw(root)

        assertEquals(PxSize(size, size), childSize[0])
        assertEquals(
            PxSize((sizeDp.toPx(density) * 2).round(), (sizeDp.toPx(density) * 2).round()),
            childSize[1]
        )
        assertEquals(
            PxPosition(0.px, (root.height.px / 2 - size.toPx()).round().toPx()),
            childPosition[0]
        )
        assertEquals(
            PxPosition(size.toPx(), (root.height.px / 2 - size.toPx()).round().toPx()),
            childPosition[1]
        )
    }

    @Test
    fun testRow_withEndCrossAxisAlignment() {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx(density)

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOf(PxSize(-1.px, -1.px), PxSize(-1.px, -1.px))
        val childPosition = arrayOf(PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px))
        show @Composable {
            <Center>
                <Row crossAxisAlignment=CrossAxisAlignment.End>
                    <Container width=sizeDp height=sizeDp>
                        <OnPositioned> coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        </OnPositioned>
                    </Container>

                    <Container width=(sizeDp * 2) height=(sizeDp * 2)>
                        <OnPositioned> coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        </OnPositioned>
                    </Container>
                </Row>
            </Center>
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidCraneView()
        waitForDraw(root)

        assertEquals(PxSize(size, size), childSize[0])
        assertEquals(
            PxSize((sizeDp.toPx(density) * 2).round(), (sizeDp.toPx(density) * 2).round()),
            childSize[1]
        )
        assertEquals(
            PxPosition(0.px, ((root.height.px + (2 * sizeDp.toPx(density))
                .round().toPx()) / 2 - size.toPx()).round().toPx()),
            childPosition[0]
        )
        assertEquals(
            PxPosition(size.toPx(), (root.height.px / 2 - size.toPx()).round().toPx()),
            childPosition[1]
        )
    }

    @Test
    fun testRow_withStretchCrossAxisAlignment() {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx(density)

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOf(PxSize(-1.px, -1.px), PxSize(-1.px, -1.px))
        val childPosition = arrayOf(PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px))
        show @Composable {
            <Center>
                <Row crossAxisAlignment=CrossAxisAlignment.Stretch>
                    <Container width=sizeDp height=sizeDp>
                        <OnPositioned> coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        </OnPositioned>
                    </Container>

                    <Container width=(sizeDp * 2) height=(sizeDp * 2)>
                        <OnPositioned> coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        </OnPositioned>
                    </Container>
                </Row>
            </Center>
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidCraneView()
        waitForDraw(root)

        assertEquals(PxSize(size.toPx(), root.height.px), childSize[0])
        assertEquals(
            PxSize((sizeDp.toPx(density) * 2).round().toPx(), root.height.px),
            childSize[1]
        )
        assertEquals(PxPosition(0.px, 0.px), childPosition[0])
        assertEquals(PxPosition(size.toPx(), 0.px), childPosition[1])
    }

    @Test
    fun testColumn_withStartCrossAxisAlignment() {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx(density)

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOf(PxSize(-1.px, -1.px), PxSize(-1.px, -1.px))
        val childPosition = arrayOf(PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px))
        show @Composable {
            <Center>
                <Column crossAxisAlignment=CrossAxisAlignment.Start>
                    <Container width=sizeDp height=sizeDp>
                        <OnPositioned> coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        </OnPositioned>
                    </Container>

                    <Container width=(sizeDp * 2) height=(sizeDp * 2)>
                        <OnPositioned> coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        </OnPositioned>
                    </Container>
                </Column>
            </Center>
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidCraneView()
        waitForDraw(root)

        assertEquals(PxSize(size, size), childSize[0])
        assertEquals(
            PxSize((sizeDp.toPx(density) * 2).round(), (sizeDp.toPx(density) * 2).round()),
            childSize[1]
        )
        assertEquals(
            PxPosition((root.width.px / 2 - size.toPx()).round().toPx(), 0.px),
            childPosition[0]
        )
        assertEquals(
            PxPosition((root.width.px / 2 - size.toPx()).round().toPx(), size.toPx()),
            childPosition[1]
        )
    }

    @Test
    fun testColumn_withEndCrossAxisAlignment() {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx(density)

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOf(PxSize(-1.px, -1.px), PxSize(-1.px, -1.px))
        val childPosition = arrayOf(PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px))
        show @Composable {
            <Center>
                <Column crossAxisAlignment=CrossAxisAlignment.End>
                    <Container width=sizeDp height=sizeDp>
                        <OnPositioned> coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        </OnPositioned>
                    </Container>

                    <Container width=(sizeDp * 2) height=(sizeDp * 2)>
                        <OnPositioned> coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        </OnPositioned>
                    </Container>
                </Column>
            </Center>
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidCraneView()
        waitForDraw(root)

        assertEquals(PxSize(size, size), childSize[0])
        assertEquals(
            PxSize((sizeDp.toPx(density) * 2).round(), (sizeDp.toPx(density) * 2).round()),
            childSize[1]
        )
        assertEquals(
            PxPosition(
                (((root.width.px + (2 * sizeDp.toPx(density))
                    .round().toPx()) / 2).round() - size).toPx(),
                0.px
            ),
            childPosition[0]
        )
        assertEquals(
            PxPosition((root.width.px / 2 - size.toPx()).round().toPx(), size.toPx()),
            childPosition[1]
        )
    }

    @Test
    fun testColumn_withStretchCrossAxisAlignment() {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx(density)

        val drawLatch = CountDownLatch(2)
        val childSize = arrayOf(PxSize(-1.px, -1.px), PxSize(-1.px, -1.px))
        val childPosition = arrayOf(PxPosition(-1.px, -1.px), PxPosition(-1.px, -1.px))
        show @Composable {
            <Center>
                <Column crossAxisAlignment=CrossAxisAlignment.Stretch>
                    <Container width=sizeDp height=sizeDp>
                        <OnPositioned> coordinates ->
                            childSize[0] = coordinates.size
                            childPosition[0] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        </OnPositioned>
                    </Container>

                    <Container width=(sizeDp * 2) height=(sizeDp * 2)>
                        <OnPositioned> coordinates ->
                            childSize[1] = coordinates.size
                            childPosition[1] = coordinates.localToGlobal(PxPosition(0.px, 0.px))
                            drawLatch.countDown()
                        </OnPositioned>
                    </Container>
                </Column>
            </Center>
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        val root = findAndroidCraneView()
        waitForDraw(root)

        assertEquals(PxSize(root.width.px, size.toPx()), childSize[0])
        assertEquals(
            PxSize(root.width.px, (sizeDp.toPx(density) * 2).round().toPx()),
            childSize[1]
        )
        assertEquals(PxPosition(0.px, 0.px), childPosition[0])
        assertEquals(PxPosition(0.px, size.toPx()), childPosition[1])
    }
}
