/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.glance.adaptive.appwidget.matrix

import androidx.collection.floatListOf
import androidx.compose.ui.unit.dp
import androidx.glance.adaptive.core.action.GlanceWidgetAction
import androidx.glance.adaptive.core.selection.HeightTier
import androidx.glance.adaptive.core.selection.SizeTiers
import androidx.glance.adaptive.core.selection.WidthTier
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SlotComponentsTest {

    @Test
    fun text_rejectsBlankValue() {
        val error = assertThrows(IllegalArgumentException::class.java) { TemplateText(" ") }
        assertThat(error).hasMessageThat().isEqualTo("Text must not be blank.")
    }

    @Test
    fun text_rejectsBlankSensitiveReplacement() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                TemplateText("6 / 8", sensitiveReplacement = "")
            }
        assertThat(error).hasMessageThat().isEqualTo("Sensitive replacement must not be blank.")
    }

    @Test
    fun ring_acceptsNormalizedProgress() {
        assertThat(Ring(progress = 0f).progress).isEqualTo(0f)
        assertThat(Ring(progress = 1f).progress).isEqualTo(1f)
    }

    @Test
    fun ring_rejectsProgressOutsideUnitRange() {
        val error = assertThrows(IllegalArgumentException::class.java) { Ring(progress = 1.5f) }
        assertThat(error).hasMessageThat().isEqualTo("Ring progress must be in [0, 1], was 1.5.")
        assertThrows(IllegalArgumentException::class.java) { Ring(progress = Float.NaN) }
    }

    @Test
    fun graph_rejectsFewerThanTwoValues() {
        val error = assertThrows(IllegalArgumentException::class.java) { Graph(floatListOf(1f)) }
        assertThat(error).hasMessageThat().isEqualTo("A graph needs at least 2 values, had 1.")
    }

    @Test
    fun graph_rejectsNegativeOrNonFiniteValues() {
        assertThrows(IllegalArgumentException::class.java) { Graph(floatListOf(1f, -1f)) }
        assertThrows(IllegalArgumentException::class.java) {
            Graph(floatListOf(1f, Float.POSITIVE_INFINITY))
        }
    }

    @Test
    fun graph_rejectsNonPositiveGoal() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                Graph(floatListOf(1f, 2f), goal = 0f)
            }
        assertThat(error).hasMessageThat().isEqualTo("A graph goal must be positive, was 0.0.")
    }

    @Test
    fun graph_needsOneLabelPerValueOrNone() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                Graph(floatListOf(1f, 2f), labels = listOf("M"))
            }
        assertThat(error)
            .hasMessageThat()
            .isEqualTo("A graph needs one label per value or none, had 1 for 2.")
        assertThat(Graph(floatListOf(1f, 2f)).labelAt(0)).isNull()
    }

    @Test
    fun graph_barSpacingWidensFromW3() {
        assertThat(Graph.barSpacing(SizeTiers(WidthTier.W1, HeightTier.H4))).isEqualTo(8.dp)
        assertThat(Graph.barSpacing(SizeTiers(WidthTier.W2, HeightTier.H4))).isEqualTo(8.dp)
        assertThat(Graph.barSpacing(SizeTiers(WidthTier.W3, HeightTier.H4))).isEqualTo(16.dp)
        assertThat(Graph.barSpacing(SizeTiers(WidthTier.W4, HeightTier.H4))).isEqualTo(16.dp)
    }

    @Test
    fun graph_scalesBarsToTheGoalOrTheLargestValue() {
        val toGoal = Graph(floatListOf(5f, 10f), goal = 20f)
        assertThat(toGoal.fractionAt(0)).isEqualTo(0.25f)
        assertThat(toGoal.fractionAt(1)).isEqualTo(0.5f)
        assertThat(toGoal.meetsGoalAt(1)).isFalse()

        val pastGoal = Graph(floatListOf(5f, 10f, 20f), goal = 10f)
        assertThat(pastGoal.fractionAt(0)).isEqualTo(0.25f)
        assertThat(pastGoal.meetsGoalAt(0)).isFalse()
        assertThat(pastGoal.meetsGoalAt(1)).isTrue()

        val toLargest = Graph(floatListOf(1f, 4f))
        assertThat(toLargest.fractionAt(0)).isEqualTo(0.25f)
        assertThat(toLargest.fractionAt(1)).isEqualTo(1f)
        assertThat(toLargest.meetsGoalAt(1)).isFalse()
    }

    @Test
    fun graph_fillsTheDotsThatReachedTheGoalOrTheLargestValue() {
        val water = Graph(floatListOf(1f, 1f, 0.75f, 0f), dotted = true)
        assertThat((0..3).map { water.isDotFilledAt(it) })
            .containsExactly(true, true, false, false)
            .inOrder()

        val withGoal = Graph(floatListOf(2f, 3f), goal = 2f, dotted = true)
        assertThat(withGoal.isDotFilledAt(0)).isTrue()
    }

    @Test
    fun badge_rejectsBlankLabel() {
        assertThrows(IllegalArgumentException::class.java) { Badge(label = "", outlineColor = 0) }
    }

    @Test
    fun containedRing_rejectsBlankValueAndInvalidProgress() {
        assertThrows(IllegalArgumentException::class.java) {
            ContainedRing(progress = 0.5f, value = "")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ContainedRing(progress = -0.1f, value = "42")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ContainedRing(progress = 0.5f, value = "42", sensitiveReplacement = " ")
        }
    }

    @Test
    fun capabilities_describeWhatEachComponentAdds() {
        assertThat(TemplateText("Steps").capabilities).containsExactly(Capability.Text)
        assertThat(Ring(progress = 0.5f).capabilities).containsExactly(Capability.Tracking)
        assertThat(Graph(floatListOf(1f, 2f)).capabilities)
            .containsExactly(Capability.Tracking, Capability.History)
        assertThat(Badge(label = "Z2", outlineColor = 0).capabilities)
            .containsExactly(Capability.Status)
        assertThat(ContainedRing(progress = 0.5f, value = "42").capabilities)
            .containsExactly(Capability.Tracking, Capability.Text)
    }

    @Test
    fun containedRing_scalesItsValueDownWithASmallerRing() {
        val ring = ContainedRing(progress = 0.5f, value = "42")

        val design = ring.geometry(88.dp)
        assertThat(design.ringSize).isEqualTo(80.dp)
        assertThat(design.fontSize.value).isEqualTo(20f)

        val small = ring.geometry(48.dp)
        assertThat(small.ringSize).isEqualTo(40.dp)
        assertThat(small.fontSize.value).isEqualTo(10f)
    }

    @Test
    fun icon_rejectsMissingDrawableOrBlankDescription() {
        val error = assertThrows(IllegalArgumentException::class.java) { Icon(icon = 0) }
        assertThat(error).hasMessageThat().isEqualTo("An icon needs a drawable resource.")
        assertThrows(IllegalArgumentException::class.java) {
            Icon(icon = GLYPH, contentDescription = " ")
        }
        assertThat(Icon(icon = GLYPH).contentDescription).isNull()
    }

    @Test
    fun icon_addsNoCapability() {
        assertThat(Icon(icon = GLYPH).capabilities).isEmpty()
    }

    @Test
    fun iconButton_rejectsMissingDrawableOrBlankDescription() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                IconButton(icon = 0, contentDescription = "Run")
            }
        assertThat(error).hasMessageThat().isEqualTo("An icon button needs a drawable resource.")
        assertThrows(IllegalArgumentException::class.java) {
            IconButton(icon = GLYPH, contentDescription = "")
        }
    }

    @Test
    fun iconButton_isAnActionOnlyWithAnAction() {
        assertThat(IconButton(GLYPH, "Run", ACTION).capabilities).containsExactly(Capability.Action)
        assertThat(IconButton(GLYPH, "Run").capabilities).isEmpty()
    }

    @Test
    fun buttons_rejectBlankSensitiveReplacements() {
        assertThrows(IllegalArgumentException::class.java) {
            IconButton(GLYPH, "Alex", sensitiveReplacement = " ")
        }
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                IconTile(GLYPH, "Alex", sensitiveReplacement = " ")
            }
        assertThat(error).hasMessageThat().isEqualTo("Sensitive replacement must not be blank.")
    }

    @Test
    fun iconTile_rejectsMissingDrawableOrBlankDescription() {
        assertThrows(IllegalArgumentException::class.java) { IconTile(0, "Run") }
        val error = assertThrows(IllegalArgumentException::class.java) { IconTile(GLYPH, " ") }
        assertThat(error).hasMessageThat().isEqualTo("An icon tile label must not be blank.")
        assertThat(IconGrid(listOf(IconTile(GLYPH, "Run")), columns = 1, rows = 1).showLabels)
            .isTrue()
    }

    @Test
    fun iconGrid_rejectsEmptyOrOverfullGrids() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                IconGrid(tiles = tiles(5), columns = 2, rows = 2)
            }
        assertThat(error)
            .hasMessageThat()
            .isEqualTo("An icon grid of 2 x 2 needs 1 to 4 tiles, had 5.")
        assertThrows(IllegalArgumentException::class.java) {
            IconGrid(tiles = emptyList(), columns = 2, rows = 2)
        }
        assertThrows(IllegalArgumentException::class.java) {
            IconGrid(tiles = tiles(1), columns = 0, rows = 1)
        }
    }

    @Test
    fun iconGrid_copiesTilesAndFillsRowByRowLeavingTrailingCellsEmpty() {
        val source = tiles(3).toMutableList()
        val grid = IconGrid(tiles = source, columns = 2, rows = 2)
        source.add(IconTile(GLYPH, "Extra"))

        assertThat(grid.tiles).hasSize(3)
        assertThat(grid.tileAt(row = 0, column = 0)).isSameInstanceAs(source[0])
        assertThat(grid.tileAt(row = 0, column = 1)).isSameInstanceAs(source[1])
        assertThat(grid.tileAt(row = 1, column = 0)).isSameInstanceAs(source[2])
        assertThat(grid.tileAt(row = 1, column = 1)).isNull()
        assertThat(grid.tileAt(row = -1, column = 0)).isNull()
        assertThat(grid.tileAt(row = 0, column = -1)).isNull()
        assertThat(grid.tileAt(row = 0, column = 2)).isNull()
        assertThat(grid.tileAt(row = 2, column = 0)).isNull()
    }

    @Test
    fun iconGrid_isAnActionWhenAnyTileHasOne() {
        assertThat(IconGrid(tiles(2), columns = 2, rows = 1).capabilities).isEmpty()
        assertThat(
                IconGrid(listOf(IconTile(GLYPH, "Run", ACTION)), columns = 2, rows = 1).capabilities
            )
            .containsExactly(Capability.Action)
    }

    private fun tiles(count: Int): List<IconTile> = List(count) { IconTile(GLYPH, "Tile $it") }

    private companion object {
        const val GLYPH = 1
        val ACTION = GlanceWidgetAction.LaunchDeeplink("app://run")
    }
}
