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
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.OnPositioned
import androidx.ui.core.PxPosition
import androidx.ui.core.PxSize
import androidx.ui.core.Ref
import androidx.ui.core.dp
import androidx.ui.core.px
import androidx.ui.core.withDensity
import androidx.ui.layout.Align
import androidx.ui.layout.Alignment
import androidx.ui.layout.ConstrainedBox
import androidx.ui.layout.Container
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.Wrap
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
class WrapTest : LayoutTest() {
    @Test
    fun testWrap() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()

        val positionedLatch = CountDownLatch(2)
        val wrapSize = Ref<PxSize>()
        val childSize = Ref<PxSize>()
        val childPosition = Ref<PxPosition>()
        show @Composable {
            <Align alignment=Alignment.TopLeft>
                <OnChildPositioned onPositioned = { coordinates ->
                    wrapSize.value = coordinates.size
                    positionedLatch.countDown()
                }>
                    <Wrap>
                        <Container width=sizeDp height=sizeDp>
                            <SaveLayoutInfo size=childSize position=childPosition positionedLatch />
                        </Container>
                    </Wrap>
                </OnChildPositioned>
            </Align>
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        assertEquals(PxSize(size, size), wrapSize.value)
        assertEquals(PxSize(size, size), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testWrap_respectsMinConstraints() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()
        val doubleSizeDp = sizeDp * 2
        val doubleSize = doubleSizeDp.toIntPx()

        val positionedLatch = CountDownLatch(2)
        val wrapSize = Ref<PxSize>()
        val childSize = Ref<PxSize>()
        val childPosition = Ref<PxPosition>()
        show @Composable {
            <Align alignment=Alignment.TopLeft>
                <OnChildPositioned onPositioned = { coordinates ->
                    wrapSize.value = coordinates.size
                    positionedLatch.countDown()
                }>
                    val constraints =
                        DpConstraints(minWidth = doubleSizeDp, minHeight = doubleSizeDp)
                    <ConstrainedBox constraints>
                        <Wrap>
                            <Container width=sizeDp height=sizeDp>
                                <SaveLayoutInfo
                                    size=childSize
                                    position=childPosition
                                    positionedLatch />
                            </Container>
                        </Wrap>
                    </ConstrainedBox>
                </OnChildPositioned>
            </Align>
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        assertEquals(PxSize(doubleSize, doubleSize), wrapSize.value)
        assertEquals(PxSize(size, size), childSize.value)
        assertEquals(PxPosition(0.px, 0.px), childPosition.value)
    }

    @Test
    fun testWrap_respectsMinConstraintsAndAlignment() = withDensity(density) {
        val sizeDp = 50.dp
        val size = sizeDp.toIntPx()
        val doubleSizeDp = sizeDp * 2
        val doubleSize = doubleSizeDp.toIntPx()

        val positionedLatch = CountDownLatch(2)
        val wrapSize = Ref<PxSize>()
        val childSize = Ref<PxSize>()
        val childPosition = Ref<PxPosition>()
        show @Composable {
            <Align alignment=Alignment.TopLeft>
                <OnChildPositioned onPositioned = { coordinates ->
                    wrapSize.value = coordinates.size
                    positionedLatch.countDown()
                }>
                    val constraints = DpConstraints(
                        minWidth = doubleSizeDp,
                        minHeight = doubleSizeDp
                    )
                    <ConstrainedBox constraints>
                        <Wrap alignment=Alignment.Center>
                            <Container width=sizeDp height=sizeDp>
                                <SaveLayoutInfo
                                    size=childSize
                                    position=childPosition
                                    positionedLatch />
                            </Container>
                        </Wrap>
                    </ConstrainedBox>
                </OnChildPositioned>
            </Align>
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        assertEquals(PxSize(doubleSize, doubleSize), wrapSize.value)
        assertEquals(PxSize(size, size), childSize.value)
        assertEquals(PxPosition(size / 2, size / 2), childPosition.value)
    }

    @Composable
    fun SaveLayoutInfo(
        size: Ref<PxSize>,
        position: Ref<PxPosition>,
        positionedLatch: CountDownLatch
    ) {
        <OnPositioned onPositioned = { coordinates ->
            size.value = PxSize(coordinates.size.width, coordinates.size.height)
            position.value = coordinates.localToGlobal(PxPosition(0.px, 0.px))
            positionedLatch.countDown()
        } />
    }
}
