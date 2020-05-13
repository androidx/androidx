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

import androidx.test.filters.SmallTest
import androidx.ui.core.Modifier
import androidx.ui.foundation.Box
import androidx.ui.foundation.Text
import androidx.ui.layout.Column
import androidx.ui.layout.padding
import androidx.ui.unit.Density
import androidx.ui.unit.dp
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class BoundsTest : ToolingTest() {
    fun Group.all(): Collection<Group> =
        listOf(this) + this.children.flatMap { it.all() }

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
                it.position?.contains("BoundsTest.kt") == true && it.box.right.value > 0
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
                        10.dp.toIntPx()), // Column
// TODO(aelias): Uncomment this line when Semantics is no longer using PassThroughLayout
//                        15.dp.toIntPx()), // Text
                    boundingBoxes
                )
            }
        }
    }
}