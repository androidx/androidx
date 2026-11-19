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

package androidx.glance.adaptive.appwidget.templates.quickactions

import android.net.Uri
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.adaptive.appwidget.matrix.MatrixBulbContent
import androidx.glance.adaptive.appwidget.matrix.MatrixCardContent
import androidx.glance.adaptive.appwidget.matrix.MatrixContent
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import androidx.glance.adaptive.appwidget.templates.track.nominalDimensions
import androidx.glance.adaptive.core.action.GlanceAdaptiveAction
import androidx.glance.adaptive.core.selection.Dimensions
import androidx.glance.adaptive.core.selection.HeightTier
import androidx.glance.adaptive.core.selection.WidthTier
import androidx.glance.adaptive.core.slots.Icon
import androidx.glance.adaptive.core.slots.IconButton
import androidx.glance.adaptive.core.slots.IconGrid
import androidx.glance.adaptive.core.slots.IconTile
import androidx.glance.adaptive.core.slots.TemplateText
import androidx.glance.adaptive.core.templates.quickactions.QuickActionsTemplate
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class QuickActionsMatrixSelectorTest {

    private val workouts =
        QuickActionsTemplate(
            leading = Icon(APP_ICON),
            primary = TemplateText("Start a workout"),
            secondary = TemplateText("Fitbit"),
            trailing = OTHER_WORKOUTS,
            actions = List(MAX_ACTIONS) { item(it) },
        )

    private val contacts = QuickActionsTemplate(actions = List(MAX_ACTIONS) { item(it) })

    @Test
    fun headerCard_followsTheDensityMatrix() {
        // Rows of tiles by height, and tiles across by width, as the design's matrix draws them.
        val rows =
            mapOf(HeightTier.H1 to 0, HeightTier.H2 to 1, HeightTier.H3 to 1, HeightTier.H4 to 2)
        val columns = mapOf(WidthTier.W2 to 2, WidthTier.W3 to 3, WidthTier.W4 to 4)
        for ((width, expectedColumns) in columns) {
            for ((height, expectedRows) in rows) {
                val card = card(width, height)
                assertThat((card.leading as Icon).icon).isEqualTo(APP_ICON)
                assertThat((card.primary as TemplateText).value).isEqualTo("Start a workout")
                assertThat((card.secondary as TemplateText).value).isEqualTo("Fitbit")
                val grid = card.hero as IconGrid?
                if (expectedRows == 0) {
                    assertThat(grid).isNull()
                } else {
                    assertThat(grid!!.rows).isEqualTo(expectedRows)
                    assertThat(grid.columns).isEqualTo(expectedColumns)
                    assertThat(grid.tiles).hasSize(expectedRows * expectedColumns)
                }
            }
        }
    }

    @Test
    fun headerCard_showsTheTrailingActionFromW3() {
        for (height in listOf(HeightTier.H1, HeightTier.H2, HeightTier.H3, HeightTier.H4)) {
            assertThat(card(WidthTier.W2, height).trailing).isNull()
            for (width in listOf(WidthTier.W3, WidthTier.W4)) {
                val trailing = card(width, height).trailing as IconButton
                assertThat(trailing.icon).isEqualTo(OTHER_WORKOUTS.icon)
                assertThat(trailing.contentDescription).isEqualTo("Other workouts")
                assertThat(trailing.action).isEqualTo(OTHER_WORKOUTS.action)
            }
        }
    }

    @Test
    fun headerCard_drawsTheDesignsHeader() {
        val card = card(WidthTier.W3, HeightTier.H3)

        assertThat((card.leading as Icon).contained).isTrue()
        assertThat(card.padding).isEqualTo(12.dp)
        assertThat(card.cornerRadius).isEqualTo(32.dp)
        assertThat(card.leadingSize).isEqualTo(56.dp)
        assertThat(card.primaryTextSize).isEqualTo(18.sp)
        assertThat(card.slotSpacing).isEqualTo(18.dp)
        assertThat(card.actions).isEmpty()
        // The short row at H2 sits closer to the header.
        assertThat(card(WidthTier.W3, HeightTier.H2).slotSpacing).isEqualTo(8.dp)
        assertThat(card(WidthTier.W3, HeightTier.H4, contacts).cornerRadius).isEqualTo(32.dp)
        assertThat(card(WidthTier.W1, HeightTier.H4).cornerRadius).isEqualTo(32.dp)
    }

    @Test
    fun tiles_carryTheShortcutsInOrder() {
        val grid = card(WidthTier.W4, HeightTier.H4).hero as IconGrid

        assertThat(grid.tiles.map { it.label }).isEqualTo(List(8) { item(it).label })
        assertThat(grid.tiles.map { it.icon }).isEqualTo(List(8) { item(it).icon })
        assertThat(grid.tiles.map { it.action }).isEqualTo(List(8) { item(it).action })
        assertThat(grid.tiles.map { it.subtitle?.value })
            .isEqualTo(List(8) { item(it).subtitle?.value })
    }

    @Test
    fun tiles_narrowerWidthsShowAPlainPrefix() {
        // Pins the current behavior while the design's `5, 8` and `5, 6, 8` numbering, which may
        // pin the last shortcut, is an open UX question.
        val w2 = card(WidthTier.W2, HeightTier.H2).hero as IconGrid
        val w3 = card(WidthTier.W3, HeightTier.H2).hero as IconGrid

        assertThat(w2.tiles.map { it.label }).containsExactly("Workout 0", "Workout 1").inOrder()
        assertThat(w3.tiles.map { it.label })
            .containsExactly("Workout 0", "Workout 1", "Workout 2")
            .inOrder()
    }

    @Test
    fun tiles_shortRowsKeepTheWidthsColumnWidth() {
        val two = workouts.withActions(List(2) { item(it) })
        val five = workouts.withActions(List(5) { item(it) })

        // Two shortcuts take the first 2 of the 4 columns, start-aligned.
        val oneRow = card(WidthTier.W4, HeightTier.H2, two).hero as IconGrid
        assertThat(oneRow.columns).isEqualTo(4)
        assertThat(oneRow.rows).isEqualTo(1)
        assertThat(oneRow.tileAt(0, 1)).isNotNull()
        assertThat(oneRow.tileAt(0, 2)).isNull()

        // The columns follow the width tier, header or not.
        val one = workouts.withActions(listOf(item(0)))
        val oneWithoutHeader = QuickActionsTemplate(actions = listOf(item(0)))
        for ((width, columns) in listOf(WidthTier.W2 to 2, WidthTier.W3 to 3, WidthTier.W4 to 4)) {
            assertThat((card(width, HeightTier.H3, one).hero as IconGrid).columns)
                .isEqualTo(columns)
            assertThat((card(width, HeightTier.H3, oneWithoutHeader).hero as IconGrid).columns)
                .isEqualTo(columns)
        }

        // The last row of five keeps its single tile in the first column.
        val twoRows = card(WidthTier.W4, HeightTier.H4, five).hero as IconGrid
        assertThat(twoRows.columns).isEqualTo(4)
        assertThat(twoRows.rows).isEqualTo(2)
        assertThat(twoRows.tiles).hasSize(5)
        assertThat(twoRows.tileAt(1, 0)).isNotNull()
        assertThat(twoRows.tileAt(1, 1)).isNull()
    }

    @Test
    fun withoutHeader_shortcutsTakeTheWholeCard() {
        val rows =
            mapOf(HeightTier.H1 to 1, HeightTier.H2 to 1, HeightTier.H3 to 2, HeightTier.H4 to 2)
        val columns = mapOf(WidthTier.W2 to 2, WidthTier.W3 to 3, WidthTier.W4 to 4)
        for ((width, expectedColumns) in columns) {
            for ((height, expectedRows) in rows) {
                val card = card(width, height, contacts)
                assertThat(card.leading).isNull()
                assertThat(card.primary).isNull()
                assertThat(card.trailing).isNull()
                val grid = card.hero as IconGrid
                assertThat(grid.rows).isEqualTo(expectedRows)
                assertThat(grid.columns).isEqualTo(expectedColumns)
            }
        }
    }

    @Test
    fun narrow_isAColumnOfTheTitleAboveOneOrTwoShortcuts() {
        val shortcuts =
            mapOf(HeightTier.H1 to 1, HeightTier.H2 to 1, HeightTier.H3 to 1, HeightTier.H4 to 2)
        for ((height, expected) in shortcuts) {
            val column = card(WidthTier.W1, height)
            assertThat(column.leading).isNull()
            assertThat(column.trailing).isNull()
            assertThat((column.primary as TemplateText).value).isEqualTo("Start a workout")
            val grid = column.hero as IconGrid
            assertThat(grid.columns).isEqualTo(1)
            assertThat(grid.tiles).hasSize(expected)
        }
    }

    @Test
    fun labels_underAHeaderAreDrawnFromH3() {
        for (width in listOf(WidthTier.W2, WidthTier.W3, WidthTier.W4)) {
            assertThat((card(width, HeightTier.H2).hero as IconGrid).showLabels).isFalse()
            assertThat((card(width, HeightTier.H3).hero as IconGrid).showLabels).isTrue()
            assertThat((card(width, HeightTier.H4).hero as IconGrid).showLabels).isTrue()
        }
    }

    @Test
    fun labels_areNeverDrawnInTheNarrowColumn() {
        for (height in listOf(HeightTier.H1, HeightTier.H2, HeightTier.H3, HeightTier.H4)) {
            for (template in listOf(workouts, contacts)) {
                val grid = card(WidthTier.W1, height, template).hero as IconGrid
                assertThat(grid.showLabels).isFalse()
                // Tiles are still described by their labels.
                assertThat(grid.tiles.first().label).isEqualTo("Workout 0")
            }
        }
    }

    @Test
    fun labels_withoutHeaderAreDrawnWhereARowFitsThem() {
        // One row up to H2, two from H3, in a card padded by 12 dp, with 6 dp between rows; a
        // labelled row needs 60 dp.
        val labelled =
            mapOf(
                72 to false, // A 48 dp row, at the bottom of H1.
                88 to true, // 64 dp.
                132 to true, // 108 dp.
                154 to true, // Two 62 dp rows, at the bottom of H3.
                264 to true, // Two 117 dp rows.
            )
        for ((heightDp, expected) in labelled) {
            val card = select(Dimensions(264, heightDp), template = contacts) as MatrixCardContent
            assertThat((card.hero as IconGrid).showLabels).isEqualTo(expected)
        }
    }

    @Test
    fun narrow_tileKeepsItsGlyphSizeAtH1() {
        val column = card(WidthTier.W1, HeightTier.H1)
        // The tile is what is left of the nominal 88 dp below the padding, a line of the 14 sp
        // title and the gap.
        val titleLineDp = 20f
        val tileDp = 88f - 2 * column.padding!!.value - titleLineDp - column.slotSpacing!!.value

        assertThat(tileDp).isAtLeast(30f)
        assertThat(column.padding).isEqualTo(8.dp)
        assertThat(card(WidthTier.W1, HeightTier.H2).padding).isEqualTo(12.dp)
    }

    @Test
    fun narrow_showsTheSubtitleFromH3() {
        assertThat(card(WidthTier.W1, HeightTier.H1).secondary).isNull()
        assertThat(card(WidthTier.W1, HeightTier.H2).secondary).isNull()
        assertThat((card(WidthTier.W1, HeightTier.H3).secondary as TemplateText).value)
            .isEqualTo("Fitbit")
        assertThat(card(WidthTier.W1, HeightTier.H4).secondary).isNotNull()
    }

    @Test
    fun narrow_withoutHeader_showsTwoShortcutsFromH3() {
        val shortcuts =
            mapOf(HeightTier.H1 to 1, HeightTier.H2 to 1, HeightTier.H3 to 2, HeightTier.H4 to 2)
        for ((height, expected) in shortcuts) {
            val column = card(WidthTier.W1, height, contacts)
            assertThat(column.primary).isNull()
            assertThat((column.hero as IconGrid).tiles).hasSize(expected)
        }
    }

    @Test
    fun mini_bulbShowsTheHeaderOrTheFirstShortcut() {
        val withHeader = bulb(workouts)
        assertThat(withHeader.icon).isEqualTo(APP_ICON)
        // Described by the action it runs, not by the header's title.
        assertThat(withHeader.contentDescription).isEqualTo("Other workouts")
        assertThat(withHeader.action).isEqualTo(OTHER_WORKOUTS.action)

        val withoutHeader = bulb(contacts)
        assertThat(withoutHeader.icon).isEqualTo(item(0).icon)
        assertThat(withoutHeader.contentDescription).isEqualTo(item(0).label)
        assertThat(withoutHeader.action).isEqualTo(item(0).action)
    }

    @Test
    fun mini_stripShowsTheHeaderOrTheFirstShortcut() {
        val withHeader = strip(workouts)
        assertThat((withHeader.leading as Icon).icon).isEqualTo(APP_ICON)
        assertThat((withHeader.leading as Icon).contained).isFalse()
        assertThat((withHeader.primary as TemplateText).value).isEqualTo("Start a workout")
        // Like the design's Linear, the strip is a glyph and a title, with no button to tap.
        assertThat(withHeader.trailing).isNull()
        assertThat(withHeader.padding).isEqualTo(0.dp)
        assertThat(withHeader.leadingSize).isEqualTo(16.dp)
        assertThat(withHeader.primaryTextSize).isEqualTo(14.sp)

        val withoutHeader = strip(contacts)
        assertThat((withoutHeader.leading as Icon).icon).isEqualTo(item(0).icon)
        assertThat((withoutHeader.primary as TemplateText).value).isEqualTo(item(0).label)
        assertThat(withoutHeader.trailing).isNull()
    }

    @Test
    fun mini_headerWithoutActionRunsTheFirstShortcut() {
        val template =
            QuickActionsTemplate(
                leading = Icon(APP_ICON),
                primary = TemplateText("Start a workout"),
                actions = listOf(item(0)),
            )

        assertThat(bulb(template).action).isEqualTo(item(0).action)
        assertThat(bulb(template).contentDescription).isEqualTo("Start a workout")
        assertThat(strip(template).trailing).isNull()
    }

    @Test
    fun mini_bulbKeepsTheSensitiveReplacementOfWhatDescribesIt() {
        val contact = IconTile(icon = 1, label = "Mom", sensitiveReplacement = "Contact")
        assertThat(bulb(QuickActionsTemplate(actions = listOf(contact))).sensitiveReplacement)
            .isEqualTo("Contact")

        val titled =
            QuickActionsTemplate(
                leading = Icon(APP_ICON),
                primary = TemplateText("Mom", sensitiveReplacement = "Contact"),
                actions = listOf(item(0)),
            )
        assertThat(bulb(titled).sensitiveReplacement).isEqualTo("Contact")
        assertThat(bulb(workouts).sensitiveReplacement).isNull()
    }

    @Test
    fun hasDroppedSlots_reportsOmittedSlotsAcrossTiers() {
        // W4xH4 fits the full header (including trailing) and all 8 actions.
        assertThat(card(WidthTier.W4, HeightTier.H4).hasDroppedSlots).isFalse()
        // W2 drops the trailing button even when all actions fit.
        val twoWithTrailing = workouts.withActions(List(2) { item(it) })
        assertThat(card(WidthTier.W2, HeightTier.H3, twoWithTrailing).hasDroppedSlots).isTrue()
        // Without a trailing button, 4 actions fit in one row at W4xH3 without dropping anything.
        val fourWithoutTrailing =
            QuickActionsTemplate(
                actions = List(4) { item(it) },
                leading = Icon(APP_ICON),
                primary = TemplateText("Start a workout"),
                secondary = TemplateText("Fitbit"),
            )
        assertThat(card(WidthTier.W4, HeightTier.H3, fourWithoutTrailing).hasDroppedSlots).isFalse()
        assertThat(card(WidthTier.W4, HeightTier.H1, fourWithoutTrailing).hasDroppedSlots).isTrue()

        // Actions-only card: 8 actions fit at W4xH3 (2x4) and drop 4 at W4xH2 (1x4).
        assertThat(card(WidthTier.W4, HeightTier.H3, contacts).hasDroppedSlots).isFalse()
        assertThat(card(WidthTier.W4, HeightTier.H2, contacts).hasDroppedSlots).isTrue()

        // Narrow column: drops leading/trailing when there is a header; without a header, 1 action
        // fits at W1xH1 without dropping.
        assertThat(card(WidthTier.W1, HeightTier.H4).hasDroppedSlots).isTrue()
        val singleContact =
            QuickActionsTemplate(actions = listOf(IconTile(icon = 1, label = "Mom")))
        assertThat(card(WidthTier.W1, HeightTier.H1, singleContact).hasDroppedSlots).isFalse()

        // Mini bulb and strip: a single action without a subtitle or header drops nothing; a
        // header, extra actions, or a subtitle marks hasDroppedSlots.
        assertThat(
                select(
                        Dimensions(56, 56),
                        AppWidgetSurface.MOBILE_LOCK_SCREEN,
                        singleContact,
                    )
                    .hasDroppedSlots
            )
            .isFalse()
        assertThat(
                select(
                        Dimensions(168, 28),
                        AppWidgetSurface.MOBILE_LOCK_SCREEN,
                        singleContact,
                    )
                    .hasDroppedSlots
            )
            .isFalse()
        assertThat(
                select(
                        Dimensions(56, 56),
                        AppWidgetSurface.MOBILE_LOCK_SCREEN,
                        workouts,
                    )
                    .hasDroppedSlots
            )
            .isTrue()
        assertThat(
                select(
                        Dimensions(168, 28),
                        AppWidgetSurface.MOBILE_LOCK_SCREEN,
                        contacts,
                    )
                    .hasDroppedSlots
            )
            .isTrue()
    }

    @Test
    fun homeScreen_shortCellIsTheStripToo() {
        val strip = card(WidthTier.W3, HeightTier.H0)

        assertThat(strip.leadingSize).isEqualTo(16.dp)
        assertThat(strip.hero).isNull()
    }

    @Test
    fun unsizedWidget_isTheBulb() {
        assertThat(select(Dimensions(0, 0))).isInstanceOf(MatrixBulbContent::class.java)
    }

    /** This template's header with [actions] instead of its own. */
    private fun QuickActionsTemplate.withActions(actions: List<IconTile>) =
        QuickActionsTemplate(actions, leading, primary, secondary, trailing)

    private fun bulb(template: QuickActionsTemplate): IconButton =
        (select(Dimensions(56, 56), AppWidgetSurface.MOBILE_LOCK_SCREEN, template)
                as MatrixBulbContent)
            .component as IconButton

    private fun strip(template: QuickActionsTemplate): MatrixCardContent =
        select(Dimensions(168, 28), AppWidgetSurface.MOBILE_LOCK_SCREEN, template)
            as MatrixCardContent

    private fun card(
        width: WidthTier,
        height: HeightTier,
        template: QuickActionsTemplate = workouts,
    ): MatrixCardContent =
        select(nominalDimensions(width, height), template = template) as MatrixCardContent

    private fun select(
        dimensions: Dimensions,
        surface: AppWidgetSurface = AppWidgetSurface.MOBILE_HOME_SCREEN,
        template: QuickActionsTemplate = workouts,
    ): MatrixContent =
        QuickActionsMatrixSelector.select(template, surface.withDimensions(dimensions))

    private companion object {
        const val MAX_ACTIONS = 8
        const val APP_ICON = 100
        val OTHER_WORKOUTS =
            IconButton(
                icon = 101,
                contentDescription = "Other workouts",
                action = GlanceAdaptiveAction.DeeplinkAction(Uri.parse("app://workouts")),
            )

        fun item(index: Int) =
            IconTile(
                icon = index + 1,
                label = "Workout $index",
                action = GlanceAdaptiveAction.DeeplinkAction(Uri.parse("app://workout/$index")),
                subtitle = TemplateText("${index + 1} km"),
            )
    }
}
