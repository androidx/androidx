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
import androidx.ui.core.DensityAmbient
import androidx.ui.core.DrawLayerModifier
import androidx.ui.core.DrawModifier
import androidx.ui.core.LayoutModifier
import androidx.ui.core.Modifier
import androidx.ui.core.OwnedLayer
import androidx.ui.core.drawLayer
import androidx.ui.core.positionInRoot
import androidx.ui.foundation.Box
import androidx.ui.foundation.drawBackground
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.padding
import androidx.ui.layout.size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class ModifierInfoTest : ToolingTest() {
    fun Group.all(): Collection<Group> =
        listOf(this) + this.children.flatMap { it.all() }

    @Test
    fun testBounds() {
        val slotTableRecord = SlotTableRecord.create()
        show {
            Inspectable(slotTableRecord) {
                with(DensityAmbient.current) {
                    val px10 = 10f.toDp()
                    val px5 = 5f.toDp()
                    Box {
                        Column(Modifier.padding(px10).drawLayer().drawBackground(Color.Blue)) {
                            Box(Modifier.padding(px5).size(px5))
                        }
                    }
                }
            }
        }

        activityTestRule.runOnUiThread {
            val tree = slotTableRecord.store.first().asTree()
            val firstGroup = tree.firstOrNull {
                it.position?.contains("ModifierInfoTest.kt") == true && it.box.right.value > 0
            }!!
            val modifierInfoItems = firstGroup.all()
                .filter { it.modifierInfo.isNotEmpty() }
                .sortedBy { it.modifierInfo.size }

            val modifierInfo = modifierInfoItems.map {
                it.modifierInfo
            }

            assertEquals(2, modifierInfo.size)

            val boxModifierInfo = modifierInfo[0]
            assertEquals(2, boxModifierInfo.size)
            assertTrue("Box should only have LayoutModifiers, but the first was " +
                "${boxModifierInfo[0].modifier}", boxModifierInfo[0].modifier is LayoutModifier)
            assertEquals(10f, boxModifierInfo[0].coordinates.positionInRoot.x)

            assertTrue("Box should only have LayoutModifiers, but the second was " +
                    "${boxModifierInfo[1].modifier}",
                boxModifierInfo[1].modifier is LayoutModifier
            )
            assertEquals(15f, boxModifierInfo[1].coordinates.positionInRoot.x)

            val columnModifierInfo = modifierInfo[1]
            assertEquals(3, columnModifierInfo.size)
            assertTrue(
                "The first modifier in the column should be a LayoutModifier" +
                        "but was ${columnModifierInfo[0].modifier}",
                columnModifierInfo[0].modifier is LayoutModifier
            )
            assertEquals(0f, columnModifierInfo[0].coordinates.positionInRoot.x)
            assertTrue(
                "The second modifier in the column should be a DrawLayerModifier" +
                        "but was ${columnModifierInfo[1].modifier}",
                columnModifierInfo[1].modifier is DrawLayerModifier
            )
            assertTrue(columnModifierInfo[1].extra is OwnedLayer)
            assertEquals(10f, columnModifierInfo[1].coordinates.positionInRoot.x)
            assertTrue(
                "The third modifier in the column should be a DrawModifier" +
                        "but was ${columnModifierInfo[2].modifier}",
                columnModifierInfo[2].modifier is DrawModifier
            )
            assertEquals(10f, columnModifierInfo[2].coordinates.positionInRoot.x)
        }
    }
}