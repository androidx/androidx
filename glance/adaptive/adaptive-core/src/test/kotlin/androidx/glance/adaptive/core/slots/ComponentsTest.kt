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

package androidx.glance.adaptive.core.slots

import android.app.Activity
import android.graphics.Color
import androidx.collection.floatListOf
import androidx.collection.intListOf
import androidx.glance.adaptive.core.action.GlanceAdaptiveAction
import androidx.glance.adaptive.core.templates.list.ListItemStyle
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ComponentsTest {

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
        assertThrows(IllegalArgumentException::class.java) { Ring(progress = -0.01f) }
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
    fun graph_supportsExplicitEmphasisAndChartLabels() {
        assertThrows(IllegalArgumentException::class.java) { ChartLabel(" ") }
        assertThrows(IllegalArgumentException::class.java) {
            Graph(floatListOf(0.4f, 0.8f), emphasized = booleanArrayOf(true))
        }

        val graph =
            Graph(
                values = floatListOf(0.4f, 1.0f),
                goal = 1.0f,
                emphasized = booleanArrayOf(true, false),
                startLabel = ChartLabel("7 Day Avg"),
                endLabel = ChartLabel("13,843", icon = 99),
            )
        assertThat(graph.isEmphasizedAt(0)).isTrue()
        assertThat(graph.isEmphasizedAt(1)).isFalse()
        assertThat(graph.hasDescription).isTrue()
        assertThat(graph.withoutDescription().hasDescription).isFalse()

        // Without explicit flags, a bar is emphasized once it reaches the goal.
        val byGoal = Graph(floatListOf(0.4f, 1.0f), goal = 1.0f, endLabel = ChartLabel("13,843"))
        assertThat(byGoal.isEmphasizedAt(0)).isFalse()
        assertThat(byGoal.isEmphasizedAt(1)).isTrue()
        // One label alone is enough for a description.
        assertThat(byGoal.hasDescription).isTrue()
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
        assertThrows(IllegalArgumentException::class.java) { Badge(label = "") }
    }

    @Test
    fun badge_defaultsOutlineColorToNull() {
        assertThat(Badge(label = "Z2").outlineColor).isNull()
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
    fun icon_rejectsMissingDrawableOrBlankDescription() {
        val error = assertThrows(IllegalArgumentException::class.java) { Icon(icon = 0) }
        assertThat(error).hasMessageThat().isEqualTo("An icon needs a drawable resource.")
        assertThrows(IllegalArgumentException::class.java) {
            Icon(icon = GLYPH, contentDescription = " ")
        }
        assertThat(Icon(icon = GLYPH).contentDescription).isNull()
    }

    @Test
    fun icon_addsNoCapabilityAndDefaultsToUncontained() {
        val bare = Icon(icon = GLYPH)
        assertThat(bare.capabilities).isEmpty()
        assertThat(bare.contained).isFalse()
        assertThat(Icon(icon = GLYPH, contained = true).contained).isTrue()
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
    fun iconTile_carriesAnOptionalSubtitle() {
        assertThat(IconTile(GLYPH, "Garage Door", subtitle = TemplateText("Open")).subtitle?.value)
            .isEqualTo("Open")
        assertThat(IconTile(GLYPH, "Garage Door").subtitle).isNull()
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

    @Test
    fun iconComponents_haveValueEquality() {
        val text = TemplateText("11,056", sensitiveReplacement = "--")
        assertThat(text).isEqualTo(TemplateText("11,056", "--"))
        assertThat(text.hashCode()).isEqualTo(TemplateText("11,056", "--").hashCode())
        assertThat(text).isNotEqualTo(TemplateText("11,056"))
        assertThat(text).isNotEqualTo(TemplateText("11,056", "--", isStruckThrough = true))
        assertThat(TemplateText("Declined", isStruckThrough = true))
            .isEqualTo(TemplateText("Declined", isStruckThrough = true))
        assertThat(TemplateText("Declined", isStruckThrough = true).hashCode())
            .isEqualTo(TemplateText("Declined", isStruckThrough = true).hashCode())
        assertThat(text.toString()).contains("11,056")
        assertThat(TemplateText("Declined", isStruckThrough = true).toString())
            .contains("isStruckThrough=true")

        assertThat(Icon(GLYPH, "Fitbit", contained = true))
            .isEqualTo(Icon(GLYPH, "Fitbit", contained = true))
        assertThat(Icon(GLYPH, "Fitbit", contained = true).hashCode())
            .isEqualTo(Icon(GLYPH, "Fitbit", contained = true).hashCode())
        assertThat(Icon(GLYPH)).isNotEqualTo(Icon(GLYPH, "Fitbit"))
        assertThat(Icon(GLYPH, "Fitbit", contained = false))
            .isNotEqualTo(Icon(GLYPH, "Fitbit", contained = true))

        val button = IconButton(GLYPH, "Log activity", ACTION, sensitiveReplacement = "Log")
        assertThat(button).isEqualTo(IconButton(GLYPH, "Log activity", ACTION, "Log"))
        assertThat(button.hashCode())
            .isEqualTo(IconButton(GLYPH, "Log activity", ACTION, "Log").hashCode())
        assertThat(button).isNotEqualTo(IconButton(GLYPH, "Log activity", ACTION))
        assertThat(button)
            .isNotEqualTo(IconButton(GLYPH, "Log activity", ACTION, "Log", contained = false))
        assertThat(button.toString()).contains("Log activity")

        val badge = Badge("Now", outlineColor = OUTLINE_COLOR, dotColors = intListOf(1, 2))
        assertThat(badge)
            .isEqualTo(Badge("Now", outlineColor = OUTLINE_COLOR, dotColors = intListOf(1, 2)))
        assertThat(badge.hashCode())
            .isEqualTo(
                Badge("Now", outlineColor = OUTLINE_COLOR, dotColors = intListOf(1, 2)).hashCode()
            )
        assertThat(badge).isNotEqualTo(Badge("Later", outlineColor = OUTLINE_COLOR))
        assertThat(badge)
            .isNotEqualTo(
                Badge("Now", outlineColor = OTHER_OUTLINE_COLOR, dotColors = intListOf(1, 2))
            )
        assertThat(badge)
            .isNotEqualTo(Badge("Now", outlineColor = OUTLINE_COLOR, dotColors = intListOf(1)))
        assertThat(badge.toString()).contains("Now")

        val tile = IconTile(GLYPH, "Garage Door", ACTION, subtitle = TemplateText("Open"))
        assertThat(tile)
            .isEqualTo(IconTile(GLYPH, "Garage Door", ACTION, subtitle = TemplateText("Open")))
        assertThat(tile.hashCode())
            .isEqualTo(
                IconTile(GLYPH, "Garage Door", ACTION, subtitle = TemplateText("Open")).hashCode()
            )
        assertThat(tile).isNotEqualTo(IconTile(GLYPH, "Garage Door", ACTION))
        assertThat(tile)
            .isNotEqualTo(IconTile(GLYPH, "Garage Door", subtitle = TemplateText("Open")))
        assertThat(tile.toString()).contains("Open")
    }

    @Test
    fun checklistEntry_hasValueEquality() {
        val checked =
            ListItem.DetailCard.ChecklistEntry(
                TemplateText("Salt", isStruckThrough = true),
                isChecked = true,
            )
        assertThat(checked)
            .isEqualTo(
                ListItem.DetailCard.ChecklistEntry(
                    TemplateText("Salt", isStruckThrough = true),
                    isChecked = true,
                )
            )
        assertThat(checked.hashCode())
            .isEqualTo(
                ListItem.DetailCard.ChecklistEntry(
                        TemplateText("Salt", isStruckThrough = true),
                        isChecked = true,
                    )
                    .hashCode()
            )
        assertThat(checked)
            .isNotEqualTo(
                ListItem.DetailCard.ChecklistEntry(TemplateText("Salt"), isChecked = false)
            )
        assertThat(checked.toString()).contains("Salt")
        assertThat(checked.toString()).contains("isChecked=true")
    }

    @Test
    fun listItem_validatesSubtypeConstraintsAndDefensiveCopy() {
        assertThrows(IllegalArgumentException::class.java) {
            ListItem.Compact(
                title = TemplateText("Sync"),
                leadingIcon = Icon(GLYPH, contained = true),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            ListItem.Compact(
                title = TemplateText("Sync"),
                tertiaryText = TemplateText("Room 204"),
                tertiaryIcon = Icon(GLYPH, contained = true),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            ListItem.Compact(
                title = TemplateText("Sync"),
                trailingIcon = Icon(GLYPH),
                trailingChip = Badge("Now"),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            ListItem.Compact(
                title = TemplateText("Sync"),
                tertiaryIcon = Icon(GLYPH),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            ListItem.DetailCard(
                title = TemplateText("Bread"),
                body = TemplateText("Mix flour"),
                checklistItems = listOf(ListItem.DetailCard.ChecklistEntry(TemplateText("Flour"))),
            )
        }
        val maxEntries =
            List(ListItem.DetailCard.MAX_CHECKLIST_ITEMS) { i ->
                ListItem.DetailCard.ChecklistEntry(TemplateText("Step ${i + 1}"))
            }
        assertThat(
                ListItem.DetailCard(title = TemplateText("Bread"), checklistItems = maxEntries)
                    .checklistItems
            )
            .hasSize(ListItem.DetailCard.MAX_CHECKLIST_ITEMS)
        val overflowError =
            assertThrows(IllegalArgumentException::class.java) {
                ListItem.DetailCard(
                    title = TemplateText("Bread"),
                    checklistItems =
                        maxEntries + ListItem.DetailCard.ChecklistEntry(TemplateText("Extra")),
                )
            }
        assertThat(overflowError)
            .hasMessageThat()
            .contains("at most ${ListItem.DetailCard.MAX_CHECKLIST_ITEMS} checklist items")
        assertThrows(IllegalArgumentException::class.java) {
            ListItem.Divider(TemplateText("TOMORROW", isStruckThrough = true))
        }

        val entries = mutableListOf(ListItem.DetailCard.ChecklistEntry(TemplateText("Flour")))
        val card = ListItem.DetailCard(title = TemplateText("Bread"), checklistItems = entries)
        entries.add(ListItem.DetailCard.ChecklistEntry(TemplateText("Salt"), isChecked = true))
        assertThat(card.checklistItems)
            .containsExactly(ListItem.DetailCard.ChecklistEntry(TemplateText("Flour")))
        assertThat(ListItem.Divider(TemplateText("TOMORROW")).action).isNull()
    }

    @Test
    fun listItem_subtypesHaveValueEquality() {
        fun fullCompact() =
            ListItem.Compact(
                title = TemplateText("Sync", isStruckThrough = true),
                subtitle = TemplateText("10:00"),
                tertiaryText = TemplateText("Room 1"),
                tertiaryIcon = Icon(GLYPH),
                leadingIcon = Icon(GLYPH),
                trailingChip = Badge("Now"),
                action = ACTION,
                style = ListItemStyle.Focused,
            )

        val full = fullCompact()
        assertThat(full).isEqualTo(fullCompact())
        assertThat(full.hashCode()).isEqualTo(fullCompact().hashCode())
        assertThat(full).isNotEqualTo(ListItem.Compact(TemplateText("Sync")))
        assertThat(full.toString()).contains("Sync")
        assertThat(full.toString()).contains("Focused")

        val checkbox = ListItem.Checkbox(TemplateText("Salt"), isChecked = true, action = ACTION)
        assertThat(checkbox)
            .isEqualTo(ListItem.Checkbox(TemplateText("Salt"), isChecked = true, action = ACTION))
        assertThat(checkbox.hashCode())
            .isEqualTo(
                ListItem.Checkbox(TemplateText("Salt"), isChecked = true, action = ACTION)
                    .hashCode()
            )
        assertThat(checkbox).isNotEqualTo(ListItem.Checkbox(TemplateText("Salt")))
        assertThat(checkbox.toString()).contains("ListItem.Checkbox")

        fun fullDetailCard() =
            ListItem.DetailCard(
                title = TemplateText("Material Next"),
                body = TemplateText("Audit widgets"),
                action = ACTION,
                style = ListItemStyle.Focused,
            )

        val card = fullDetailCard()
        assertThat(card).isEqualTo(fullDetailCard())
        assertThat(card.hashCode()).isEqualTo(fullDetailCard().hashCode())
        assertThat(card).isNotEqualTo(ListItem.DetailCard(TemplateText("Material Next")))
        assertThat(card.toString()).contains("ListItem.DetailCard")

        val divider = ListItem.Divider(TemplateText("TOMORROW"))
        assertThat(divider).isEqualTo(ListItem.Divider(TemplateText("TOMORROW")))
        assertThat(divider.hashCode())
            .isEqualTo(ListItem.Divider(TemplateText("TOMORROW")).hashCode())
        assertThat(divider).isNotEqualTo(ListItem.Divider(TemplateText("FRIDAY")))
        assertThat(divider.toString()).contains("TOMORROW")
    }

    @Test
    fun itemList_rejectsInvalidConfigurationsAndCopiesItems() {
        assertThrows(IllegalArgumentException::class.java) { ItemList(emptyList()) }
        assertThrows(IllegalArgumentException::class.java) {
            ItemList(listOf(ListItem.Divider(TemplateText("TOMORROW"))))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ItemList(
                items = listOf(ListItem.Compact(TemplateText("Event 1"))),
                isScrollable = true,
                layoutMode = ItemListLayoutMode.ExpandFeatured,
            )
        }
        val twoItems =
            listOf(
                ListItem.Compact(TemplateText("Event 1")),
                ListItem.Compact(TemplateText("Event 2")),
            )
        assertThrows(IllegalArgumentException::class.java) {
            ItemList(items = twoItems, layoutMode = ItemListLayoutMode.ExpandSingle)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ItemList(items = twoItems, layoutMode = ItemListLayoutMode.UncontainedSingle)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ItemList(
                items = listOf(ListItem.Checkbox(TemplateText("Milk"))),
                layoutMode = ItemListLayoutMode.UncontainedSingle,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            ItemList(
                items = listOf(ListItem.DetailCard(TemplateText("Bread"))),
                layoutMode = ItemListLayoutMode.UncontainedSingle,
            )
        }

        val source = mutableListOf<ListItem>(ListItem.Compact(TemplateText("Event 1")))
        val list = ItemList(source)
        source.add(ListItem.Compact(TemplateText("Event 2")))
        assertThat(list.items).containsExactly(ListItem.Compact(TemplateText("Event 1")))
    }

    @Test
    fun itemList_capabilitiesAndEquality() {
        val textOnly = ItemList(listOf(ListItem.Compact(TemplateText("Event"))))
        val withAction = ItemList(listOf(ListItem.Checkbox(TemplateText("Milk"), action = ACTION)))
        val withChip =
            ItemList(listOf(ListItem.Compact(TemplateText("Event"), trailingChip = Badge("Now"))))
        val withBoth =
            ItemList(
                listOf(
                    ListItem.Compact(
                        TemplateText("Event"),
                        trailingChip = Badge("Now"),
                        action = ACTION,
                    )
                )
            )

        assertThat(textOnly.capabilities).containsExactly(Capability.Text)
        assertThat(withAction.capabilities).containsExactly(Capability.Text, Capability.Action)
        assertThat(withChip.capabilities).containsExactly(Capability.Text, Capability.Status)
        assertThat(withBoth.capabilities)
            .containsExactly(Capability.Text, Capability.Action, Capability.Status)

        val base = ItemList(listOf(ListItem.Compact(TemplateText("Event"))))
        assertThat(base).isEqualTo(ItemList(listOf(ListItem.Compact(TemplateText("Event")))))
        assertThat(base.hashCode())
            .isEqualTo(ItemList(listOf(ListItem.Compact(TemplateText("Event")))).hashCode())
        assertThat(base).isNotEqualTo(ItemList(base.items, isScrollable = true))
        assertThat(base).isNotEqualTo(ItemList(base.items, showTertiaryText = true))
        assertThat(base)
            .isNotEqualTo(ItemList(base.items, layoutMode = ItemListLayoutMode.ExpandSingle))
        assertThat(base)
            .isNotEqualTo(ItemList(base.items, layoutMode = ItemListLayoutMode.UncontainedSingle))
        assertThat(base)
            .isNotEqualTo(ItemList(base.items, layoutMode = ItemListLayoutMode.ExpandFeatured))
        assertThat(base.toString()).contains("isScrollable=false")
        assertThat(base.toString()).contains("layoutMode=Default")
        assertThat(ItemListLayoutMode.ExpandSingle.toString()).isEqualTo("ExpandSingle")
        assertThat(ItemListLayoutMode.UncontainedSingle.toString()).isEqualTo("UncontainedSingle")
        assertThat(ItemListLayoutMode.ExpandFeatured.toString()).isEqualTo("ExpandFeatured")
    }

    private fun tiles(count: Int): List<IconTile> = List(count) { IconTile(GLYPH, "Tile $it") }

    private companion object {
        const val GLYPH = 1
        const val OUTLINE_COLOR = Color.GREEN
        const val OTHER_OUTLINE_COLOR = Color.RED
        val ACTION = GlanceAdaptiveAction.LaunchAction<Activity>()
    }
}
