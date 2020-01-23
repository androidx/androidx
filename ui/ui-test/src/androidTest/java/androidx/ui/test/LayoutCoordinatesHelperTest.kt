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

package androidx.ui.test

import androidx.test.filters.MediumTest
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.OnPositioned
import androidx.ui.core.WithDensity
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.LayoutGravity
import androidx.ui.layout.LayoutWidth
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import androidx.ui.unit.px
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(JUnit4::class)
class LayoutCoordinatesHelperTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @Test
    fun positionInParent_noOffset() {
        val latch = CountDownLatch(2)
        var parentCoordinates: LayoutCoordinates? = null
        var childCoordinates: LayoutCoordinates? = null
        composeTestRule.setContent {
            Column {
                OnPositioned { coordinates ->
                    parentCoordinates = coordinates
                    latch.countDown()
                }
                OnChildPositioned(onPositioned = { coordinates ->
                    childCoordinates = coordinates
                    latch.countDown()
                }) {
                    Container(width = 10.dp, height = 10.dp, modifier = LayoutGravity.Start) {}
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(PxPosition.Origin,
            parentCoordinates!!.childToLocal(childCoordinates!!, PxPosition.Origin))
    }

    @Test
    fun positionInParent_centered() {
        val latch = CountDownLatch(2)
        var parentCoordinates: LayoutCoordinates? = null
        var childCoordinates: LayoutCoordinates? = null
        composeTestRule.setContent {
            WithDensity {
                Container(LayoutWidth(40.ipx.toDp())) {
                    Column(LayoutWidth(20.ipx.toDp())) {
                        OnPositioned { coordinates ->
                            parentCoordinates = coordinates
                            latch.countDown()
                        }
                        OnChildPositioned(onPositioned = { coordinates ->
                            childCoordinates = coordinates
                            latch.countDown()
                        }) {
                            Container(
                                width = 10.ipx.toDp(),
                                height = 10.ipx.toDp(),
                                modifier = LayoutGravity.Center
                            ) {}
                        }
                    }
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(PxPosition(5.px, 0.px),
            parentCoordinates!!.childToLocal(childCoordinates!!, PxPosition.Origin))
    }
}
