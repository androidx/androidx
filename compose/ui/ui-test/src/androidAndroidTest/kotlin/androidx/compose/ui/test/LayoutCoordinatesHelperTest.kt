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

package androidx.compose.ui.test

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class LayoutCoordinatesHelperTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun positionInParent_noOffset() {
        val latch = CountDownLatch(2)
        var parentCoordinates: LayoutCoordinates? = null
        var childCoordinates: LayoutCoordinates? = null
        rule.setContent {
            Column(
                Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                    parentCoordinates = coordinates
                    latch.countDown()
                }
            ) {
                Box(
                    Modifier.size(10.dp)
                        .align(Alignment.Start)
                        .onGloballyPositioned { coordinates ->
                            childCoordinates = coordinates
                            latch.countDown()
                        }
                )
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(
            Offset.Zero,
            parentCoordinates!!.localPositionOf(childCoordinates!!, Offset.Zero)
        )
    }

    @Test
    fun positionInParent_centered() {
        val latch = CountDownLatch(2)
        var parentCoordinates: LayoutCoordinates? = null
        var childCoordinates: LayoutCoordinates? = null
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.width(40.toDp()), contentAlignment = Alignment.Center) {
                    Column(
                        Modifier.width(20.toDp())
                            .onGloballyPositioned { coordinates: LayoutCoordinates ->
                                parentCoordinates = coordinates
                                latch.countDown()
                            }
                    ) {
                        Box(
                            Modifier.size(10.toDp())
                                .align(Alignment.CenterHorizontally)
                                .onGloballyPositioned { coordinates ->
                                    childCoordinates = coordinates
                                    latch.countDown()
                                }
                        )
                    }
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(
            Offset(5f, 0f),
            parentCoordinates!!.localPositionOf(childCoordinates!!, Offset.Zero)
        )
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun onPlaced_alignmentChange() {
        var offset by mutableStateOf(IntOffset(0, 0))
        var alignment by mutableStateOf(Alignment.Center)
        var positionInRoot by mutableStateOf(Offset.Zero)
        rule.setContent {
            CompositionLocalProvider(LocalDensity.provides(Density(1f))) {
                Box(Modifier.offset(200.dp, 100.dp).size(300.dp)) {
                    Box(Modifier.onPlaced {
                        offset = it.placementInParent()
                        positionInRoot = it.positionInRoot()
                    }.align(alignment = alignment).size(100.dp))
                }
            }
        }
        val parentOffset = Offset(200f, 100f)
        rule.runOnIdle {
            assertEquals(IntOffset(100, 100), offset)
            assertEquals(parentOffset + Offset(100f, 100f), positionInRoot)
            alignment = Alignment.TopStart
        }
        rule.runOnIdle {
            assertEquals(IntOffset(0, 0), offset)
            assertEquals(parentOffset + Offset(0f, 0f), positionInRoot)
            alignment = Alignment.BottomEnd
        }
        rule.runOnIdle {
            assertEquals(IntOffset(200, 200), offset)
            assertEquals(parentOffset + Offset(200f, 200f), positionInRoot)
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun onPlaced_invocation() {
        var additionalOffset by mutableStateOf(IntOffset.Zero)
        var alignment by mutableStateOf(Alignment.Center)
        val invocations = mutableListOf(0, 0, 0)
        rule.setContent {
            CompositionLocalProvider(LocalDensity.provides(Density(1f))) {
                Box(Modifier.offset(200.dp, 100.dp).size(300.dp)) {
                    Box(Modifier.align(alignment = alignment)
                        .offset { additionalOffset }.onPlaced {
                            assertEquals(additionalOffset, it.placementInParent())
                            invocations[0] = invocations[0] + 1
                        }.clickable { }.onPlaced {
                            assertEquals(additionalOffset, it.placementInParent())
                            invocations[1] = invocations[1] + 1
                        }.testTag("Test").onPlaced {
                            assertEquals(additionalOffset, it.placementInParent())
                            invocations[2] = invocations[2] + 1
                        }
                        .size(100.dp))
                }
            }
        }
        rule.runOnIdle {
            invocations.forEach {
                assertEquals(1, it)
            }
            alignment = Alignment.TopStart
        }
        rule.runOnIdle {
            invocations.forEach {
                assertEquals(2, it)
            }
            additionalOffset = IntOffset(0, 10)
        }
        rule.runOnIdle {
            invocations.forEach {
                assertEquals(3, it)
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun LayoutCoordinates.placementInParent() =
        parentCoordinates!!.localPositionOf(this, Offset.Zero).round()
}
