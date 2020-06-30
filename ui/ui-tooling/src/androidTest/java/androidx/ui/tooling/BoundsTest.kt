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

package androidx.ui.tooling

import androidx.compose.InternalComposeApi
import androidx.compose.getValue
import androidx.compose.key
import androidx.compose.mutableStateOf
import androidx.compose.resetSourceInfo
import androidx.compose.setValue
import androidx.test.filters.SmallTest
import androidx.ui.core.Modifier
import androidx.ui.core.WithConstraints
import androidx.compose.foundation.Box
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.ui.unit.Density
import androidx.ui.unit.dp
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class BoundsTest : ToolingTest() {
    fun Group.all(): Collection<Group> =
        listOf(this) + this.children.flatMap { it.all() }

    @Before
    fun reset() {
        @OptIn(InternalComposeApi::class)
        resetSourceInfo()
    }

    @Test
    fun testBounds() {
        val slotTableRecord = SlotTableRecord.create()
        show {
            Inspectable(slotTableRecord) {
                Box {
                    Column(Modifier.padding(10.dp)) {
                        Text("Hello", Modifier.padding(5.dp))
                    }
                }
            }
        }

        activityTestRule.runOnUiThread {
            val tree = slotTableRecord.store.first().asTree()
            val boundingBoxes = tree.firstOrNull {
                it.position?.contains("BoundsTest.kt") == true && it.box.right > 0
            }!!
                .all()
                .filter {
                    val name = it.position
                    name != null && name.contains("BoundsTest.kt")
                }
                .map {
                    it.box.left
                }
                .distinct()
                .sorted()
                .toTypedArray()

            with(Density(activityTestRule.activity)) {
                println(boundingBoxes.contentDeepToString())
                Assert.assertArrayEquals(
                    arrayOf(
                        0.dp.toIntPx(), // Root
                        10.dp.toIntPx(), // Column
                        15.dp.toIntPx()), // Text
                    boundingBoxes
                )
            }
        }
    }

    @Test
    fun testBoundWithConstraints() {
        val slotTableRecord = SlotTableRecord.create()
        show {
            Inspectable(slotTableRecord) {
                WithConstraints {
                    Column {
                        Box {
                            Text("Hello")
                        }
                        Box {
                            Text("Hello")
                        }
                    }
                }
            }
        }

        activityTestRule.runOnUiThread {
            val store = slotTableRecord.store
            Assert.assertTrue(store.size > 1)
            val trees = slotTableRecord.store.map { it.asTree() }
            val boundingBoxes = trees.map {
                it.all().filter {
                    it.box.right > 0 && it.location?.sourceFile == "BoundsTest.kt"
                }
            }.flatten().groupBy { it.location }

            Assert.assertTrue(boundingBoxes.size >= 6)
        }
    }

    @Test
    fun testDisposeWithComposeTables() {
        val slotTableRecord = SlotTableRecord.create()
        var value by mutableStateOf(0)
        var latch = CountDownLatch(1)
        show {
            Inspectable(slotTableRecord) {
                key(value) {
                    WithConstraints {
                        Text("Hello")
                    }
                }
            }
            latch.countDown()
        }

        activityTestRule.runOnUiThread {
            latch = CountDownLatch(1)
            value = 1
        }
        latch.await(1, TimeUnit.SECONDS)

        activityTestRule.runOnUiThread {
            latch = CountDownLatch(1)
            value = 2
        }
        latch.await(1, TimeUnit.SECONDS)

        Assert.assertTrue(slotTableRecord.store.size < 3)
    }
}