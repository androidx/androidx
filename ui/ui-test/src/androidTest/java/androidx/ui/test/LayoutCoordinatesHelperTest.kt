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
import androidx.ui.core.Alignment
import androidx.ui.core.DensityAmbient
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Modifier
import androidx.ui.core.onPositioned
import androidx.ui.foundation.Box
import androidx.ui.foundation.ContentGravity
import androidx.ui.layout.Column
import androidx.ui.layout.preferredSize
import androidx.ui.layout.preferredWidth
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
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
            Column(Modifier.onPositioned { coordinates: LayoutCoordinates ->
                parentCoordinates = coordinates
                latch.countDown()
            }) {
                Box(
                    Modifier.preferredSize(10.dp)
                        .gravity(Alignment.Start)
                        .onPositioned { coordinates ->
                            childCoordinates = coordinates
                            latch.countDown()
                        }
                )
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(
            PxPosition.Origin,
            parentCoordinates!!.childToLocal(childCoordinates!!, PxPosition.Origin)
        )
    }

    @Test
    fun positionInParent_centered() {
        val latch = CountDownLatch(2)
        var parentCoordinates: LayoutCoordinates? = null
        var childCoordinates: LayoutCoordinates? = null
        composeTestRule.setContent {
            with(DensityAmbient.current) {
                Box(Modifier.preferredWidth(40.ipx.toDp()), gravity = ContentGravity.Center) {
                    Column(
                        Modifier.preferredWidth(20.ipx.toDp())
                            .onPositioned { coordinates: LayoutCoordinates ->
                                parentCoordinates = coordinates
                                latch.countDown()
                            }
                    ) {
                        Box(
                            Modifier.preferredSize(10.ipx.toDp())
                                .gravity(Alignment.CenterHorizontally)
                                .onPositioned { coordinates ->
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
            PxPosition(5f, 0f),
            parentCoordinates!!.childToLocal(childCoordinates!!, PxPosition.Origin)
        )
    }
}
