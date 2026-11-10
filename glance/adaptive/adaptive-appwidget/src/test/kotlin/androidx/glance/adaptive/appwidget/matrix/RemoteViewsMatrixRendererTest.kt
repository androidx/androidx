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

import android.app.Application
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.collection.floatListOf
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.adaptive.appwidget.R
import androidx.glance.adaptive.appwidget.components.RemoteViewsActions
import androidx.glance.adaptive.appwidget.components.density
import androidx.glance.adaptive.appwidget.components.dpToPx
import androidx.glance.adaptive.appwidget.matrix.remoteviews.RemoteViewsMatrixRenderer
import androidx.glance.adaptive.appwidget.renderer.RemoteViewsRenderOutput
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import androidx.glance.adaptive.core.action.GlanceAdaptiveActionCallback
import androidx.glance.adaptive.core.action.GlanceWidgetAction
import androidx.glance.adaptive.core.selection.Dimensions
import androidx.glance.adaptive.core.slots.Badge
import androidx.glance.adaptive.core.slots.ContainedRing
import androidx.glance.adaptive.core.slots.Graph
import androidx.glance.adaptive.core.slots.Icon
import androidx.glance.adaptive.core.slots.IconButton
import androidx.glance.adaptive.core.slots.IconGrid
import androidx.glance.adaptive.core.slots.IconTile
import androidx.glance.adaptive.core.slots.Ring
import androidx.glance.adaptive.core.slots.TemplateText
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class RemoteViewsMatrixRendererTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** The renderer reads the matrix only, so the template is never looked at. */
    private val template = object : GlanceAdaptiveTemplate() {}

    private val week =
        Graph(
            values = floatListOf(*FloatArray(7) { i -> 1_000f * (i + 1) }),
            labels = listOf("T", "F", "S", "S", "M", "T", "W"),
            goal = 5_000f,
        )

    @Test
    fun graph_allBarsHaveConsistentWidthAndFlushOuterEdges() {
        for (widthDp in listOf(176, 264, 348)) {
            for (heightDp in listOf(176, 264)) {
                val root = render(fullCard(), Dimensions(widthDp, heightDp))
                val barsRow =
                    root.findViewById<ViewGroup>(R.id.glance_adaptive_appwidget_remoteviews_bars)
                assertThat(barsRow.childCount).isEqualTo(week.values.size)

                val firstColumn = barsRow.getChildAt(0)
                val lastColumn = barsRow.getChildAt(barsRow.childCount - 1)
                assertThat(firstColumn.left).isEqualTo(0)
                assertThat(lastColumn.right).isEqualTo(barsRow.width)

                val barWidths =
                    (0 until barsRow.childCount).map { i ->
                        barsRow
                            .getChildAt(i)
                            .findViewById<View>(R.id.glance_adaptive_appwidget_remoteviews_bar_fill)
                            .width
                    }
                // LinearLayout distributes integer remainder pixels across weighted children by at
                // most 1px; no bar (including first or last) may be wider than that.
                assertThat(barWidths.max() - barWidths.min()).isAtMost(1)
            }
        }
    }

    @Test
    fun graph_tallestBarFillsFullAvailableHeightAboveLabel() {
        for (heightDp in listOf(176, 264)) {
            val root = render(fullCard(), Dimensions(348, heightDp))
            val barsRow =
                root.findViewById<ViewGroup>(R.id.glance_adaptive_appwidget_remoteviews_bars)
            val lastColumn = barsRow.getChildAt(barsRow.childCount - 1)
            val lastBarFill =
                lastColumn.findViewById<View>(R.id.glance_adaptive_appwidget_remoteviews_bar_fill)
            val lastLabel =
                lastColumn.findViewById<View>(R.id.glance_adaptive_appwidget_remoteviews_bar_label)

            assertThat(lastColumn.height).isEqualTo(barsRow.height)
            assertThat(lastBarFill.top).isEqualTo(0)
            assertThat(lastLabel.bottom).isEqualTo(lastColumn.height)
        }
    }

    @Test
    fun graph_dottedDrawsOneDotPerValueWithoutLabels() {
        val water =
            Graph(values = floatListOf(*FloatArray(8) { if (it < 6) 1f else 0f }), dotted = true)
        val root =
            render(
                MatrixCardContent(primary = TemplateText("6 / 8"), hero = water),
                Dimensions(348, 176),
            )
        val barsRow = root.findViewById<ViewGroup>(R.id.glance_adaptive_appwidget_remoteviews_bars)

        assertThat(barsRow.childCount).isEqualTo(8)
        val dots =
            (0 until barsRow.childCount).map { i ->
                barsRow
                    .getChildAt(i)
                    .findViewById<View>(R.id.glance_adaptive_appwidget_remoteviews_bar_fill)
            }
        // Dots keep their own size, square, rather than stretching like bars.
        assertThat(dots.map { it.height }.toSet()).hasSize(1)
        assertThat(dots[0].width).isEqualTo(dots[0].height)
        val label =
            barsRow
                .getChildAt(0)
                .findViewById<View>(R.id.glance_adaptive_appwidget_remoteviews_bar_label)
        assertThat(label.visibility).isEqualTo(View.GONE)
    }

    @Test
    fun actionRow_isShownWithActionsAndHiddenWithout() {
        val withActions = render(fullCard(), Dimensions(348, 132))
        assertThat(actionRow(withActions).visibility).isEqualTo(View.VISIBLE)

        val withoutActions =
            render(MatrixCardContent(primary = TemplateText("11,056")), Dimensions(348, 132))
        assertThat(actionRow(withoutActions).visibility).isEqualTo(View.GONE)
    }

    @Test
    fun actionRow_keepsThePositionOfAMissingAction() {
        val card =
            MatrixCardContent(
                primary = TemplateText("11,056"),
                actions = listOf(null, TemplateText("3,118 to go")),
            )
        val row = actionRow(render(card, Dimensions(348, 132)))

        // The empty first position keeps its spacer, so the value stays at the trailing edge.
        assertThat(row.childCount).isEqualTo(2)
        assertThat(row.getChildAt(1).right).isEqualTo(row.width)
    }

    @Test
    fun header_showsSecondaryTextOnlyWhenPresent() {
        val withSecondary = render(fullCard(), Dimensions(348, 132))
        assertThat(secondary(withSecondary).visibility).isEqualTo(View.VISIBLE)
        assertThat(secondary(withSecondary).text.toString()).isEqualTo("Steps")

        val withoutSecondary =
            render(MatrixCardContent(primary = TemplateText("11,056")), Dimensions(348, 132))
        assertThat(secondary(withoutSecondary).visibility).isEqualTo(View.GONE)
    }

    @Test
    fun header_longSecondaryTextIsEllipsizedBeforeItSqueezesThePrimaryText() {
        val root =
            render(
                MatrixCardContent(
                    leading = Ring(progress = 0.55f),
                    primary = TemplateText("11,056"),
                    secondary = TemplateText("Steps walked since midnight on all of your devices"),
                ),
                Dimensions(348, 132),
            )

        val primary =
            root.findViewById<TextView>(R.id.glance_adaptive_appwidget_remoteviews_primary)
        // 348 dp wide less 2 x 20 dp padding leaves 308 dp, of which the secondary takes 40%.
        val capPx = (308f * 0.4f).dpToPx(context.density)
        assertThat(secondary(root).maxWidth).isEqualTo(capPx)
        assertThat(secondary(root).width).isAtMost(capPx)
        assertThat(primary.width).isGreaterThan(0)
    }

    @Test
    fun header_emptyPrimaryKeepsItsPlace() {
        val root =
            render(MatrixCardContent(secondary = TemplateText("Steps")), Dimensions(348, 132))

        assertThat(
                root
                    .findViewById<View>(R.id.glance_adaptive_appwidget_remoteviews_primary)
                    .visibility
            )
            .isEqualTo(View.INVISIBLE)
    }

    @Test
    fun card_withEverySlotEmptyDrawsOnlyItsBackground() {
        val root = render(MatrixCardContent(), Dimensions(348, 176))

        assertThat(
                root
                    .findViewById<View>(R.id.glance_adaptive_appwidget_remoteviews_slot_header)
                    .visibility
            )
            .isEqualTo(View.GONE)
        assertThat(
                root
                    .findViewById<View>(R.id.glance_adaptive_appwidget_remoteviews_slot_hero)
                    .visibility
            )
            .isEqualTo(View.GONE)
        assertThat(actionRow(root).visibility).isEqualTo(View.GONE)
    }

    @Test
    fun bulb_drawsTheValueInsideTheRing() {
        val root =
            render(
                MatrixBulbContent(ContainedRing(progress = 0.5f, value = "42")),
                Dimensions(88, 88),
            )

        assertThat(
                root
                    .findViewById<TextView>(R.id.glance_adaptive_appwidget_remoteviews_ring_value)
                    .text
                    .toString()
            )
            .isEqualTo("42")
    }

    @Test
    fun card_isNotClickable() {
        val root = render(MatrixCardContent(primary = TemplateText("11,056")), Dimensions(176, 88))

        val card = root.findViewById<View>(android.R.id.background)
        assertThat(card.hasOnClickListeners()).isFalse()
        assertThat(card.isClickable).isFalse()
    }

    @Test
    fun actions_equalActionsShareAPendingIntentAndDifferentOnesDoNot() {
        val actions = RemoteViewsActions(context, widgetName = "dashboard")

        val run = actions.pendingIntentFor(RUN)
        assertThat(run).isNotNull()
        assertThat(actions.pendingIntentFor(GlanceWidgetAction.LaunchDeeplink(RUN_URI)))
            .isEqualTo(run)
        assertThat(actions.pendingIntentFor(GlanceWidgetAction.LaunchDeeplink("app://bike")))
            .isNotEqualTo(run)
        assertThat(actions.pendingIntentFor(null)).isNull()
    }

    @Test
    fun actions_callbackRunsOnlyWithTheNameOfItsWidget() {
        val callback = GlanceWidgetAction.Callback(NoOpCallback::class.java)

        assertThat(RemoteViewsActions(context, widgetName = "dashboard").pendingIntentFor(callback))
            .isNotNull()
        // Without the widget's name, a callback cannot be delivered, so the view ignores taps.
        assertThat(RemoteViewsActions(context, widgetName = null).pendingIntentFor(callback))
            .isNull()
    }

    @Test
    fun icon_isCappedAtTheLeadingSizeAndDescribed() {
        val root =
            render(
                MatrixCardContent(
                    leading = Icon(GLYPH, contentDescription = "Fitbit"),
                    primary = TemplateText("Start a workout"),
                    leadingSize = 24.dp,
                    primaryTextSize = 16.sp,
                ),
                Dimensions(348, 264),
            )

        val icon = root.findViewById<ImageView>(R.id.glance_adaptive_appwidget_remoteviews_icon)
        assertThat(icon.maxWidth).isEqualTo(24f.dpToPx(context.density))
        assertThat(icon.maxHeight).isEqualTo(24f.dpToPx(context.density))
        assertThat(icon.contentDescription.toString()).isEqualTo("Fitbit")
    }

    @Test
    fun compactCard_headerIsAsTallAsItsSlotsRatherThanTheDefaultLeadingComponent() {
        val root =
            render(
                MatrixCardContent(
                    leading = Icon(GLYPH),
                    primary = TemplateText("Start a workout"),
                    hero = week,
                    leadingSize = 24.dp,
                    primaryTextSize = 16.sp,
                ),
                Dimensions(348, 264),
            )

        val header = root.findViewById<View>(R.id.glance_adaptive_appwidget_remoteviews_slot_header)
        assertThat(header.height).isLessThan(56f.dpToPx(context.density))
    }

    @Test
    fun compactHeader_runsTheSecondaryTextUnderThePrimaryText() {
        val root =
            render(
                MatrixCardContent(
                    leading = Icon(GLYPH),
                    primary = TemplateText("Start a workout"),
                    secondary = TemplateText("Fitbit"),
                    leadingSize = 40.dp,
                    primaryTextSize = 16.sp,
                ),
                Dimensions(348, 132),
            )

        val primary =
            root.findViewById<TextView>(R.id.glance_adaptive_appwidget_remoteviews_primary)
        val secondary = secondary(root)
        assertThat(secondary.text.toString()).isEqualTo("Fitbit")
        assertThat(secondary.top).isAtLeast(primary.bottom)
        assertThat(secondary.left).isEqualTo(primary.left)
    }

    @Test
    fun compactCard_fitsTheLockScreenStrip() {
        val root =
            render(
                MatrixCardContent(
                    leading = Icon(GLYPH),
                    primary = TemplateText("Start a run"),
                    padding = 0.dp,
                    leadingSize = 16.dp,
                    primaryTextSize = 14.sp,
                ),
                Dimensions(168, 28),
                AppWidgetSurface.MOBILE_LOCK_SCREEN,
            )

        val primary =
            root.findViewById<TextView>(R.id.glance_adaptive_appwidget_remoteviews_primary)
        assertThat(primary.text.toString()).isEqualTo("Start a run")
        assertThat(primary.bottom).isAtMost(28f.dpToPx(context.density))
    }

    @Test
    fun card_withoutHeaderStartsItsHeroAtThePadding() {
        val root = render(MatrixCardContent(hero = week, padding = 12.dp), Dimensions(176, 176))

        assertThat(
                root.findViewById<View>(R.id.glance_adaptive_appwidget_remoteviews_slot_hero).top
            )
            .isEqualTo(12f.dpToPx(context.density))
    }

    @Test
    fun iconButton_bulbIsACircleAroundTheDesignGlyphAndRunsItsAction() {
        val button = IconButton(GLYPH, "Start a run", RUN)
        val density = context.density

        val large = render(MatrixBulbContent(button), Dimensions(88, 88))
        val largeButton = iconButton(large)
        assertThat(iconButtonGlyph(large).maxWidth).isEqualTo(44f.dpToPx(density))
        // The circle is padded out from the glyph to the 88 dp diameter.
        assertThat(largeButton.paddingLeft + largeButton.paddingRight)
            .isEqualTo(88f.dpToPx(density) - 44f.dpToPx(density))
        assertThat(largeButton.contentDescription.toString()).isEqualTo("Start a run")

        val small = render(MatrixBulbContent(button), Dimensions(56, 56))
        assertThat(iconButtonGlyph(small).maxWidth).isEqualTo(26f.dpToPx(density))

        largeButton.performClick()
        assertThat(shadowOf(context as Application).nextStartedActivity.data)
            .isEqualTo(Uri.parse(RUN_URI))
    }

    @Test
    fun iconButton_trailingTheHeaderMatchesTheLeadingComponentAndRunsItsAction() {
        val root =
            render(
                MatrixCardContent(
                    leading = Icon(GLYPH),
                    primary = TemplateText("Start a workout"),
                    secondary = TemplateText("Fitbit"),
                    trailing = IconButton(GLYPH, "Other workouts", RUN),
                    leadingSize = 40.dp,
                    primaryTextSize = 16.sp,
                ),
                Dimensions(264, 88),
            )

        val density = context.density
        val trailingSlot =
            root.findViewById<View>(R.id.glance_adaptive_appwidget_remoteviews_slot_trailing)
        assertThat(trailingSlot.visibility).isEqualTo(View.VISIBLE)
        val button = iconButton(root)
        assertThat(button.contentDescription.toString()).isEqualTo("Other workouts")
        assertThat(iconButtonGlyph(root).maxWidth).isEqualTo(20f.dpToPx(density))
        assertThat(button.paddingLeft + button.paddingRight)
            .isEqualTo(40f.dpToPx(density) - 20f.dpToPx(density))
        // The trailing button sits at the header's end, after the texts.
        assertThat(button.right + trailingSlot.left).isAtMost(root.width)
        assertThat(trailingSlot.left).isAtLeast(secondary(root).right)

        button.performClick()
        assertThat(shadowOf(context as Application).nextStartedActivity.data)
            .isEqualTo(Uri.parse(RUN_URI))
    }

    @Test
    fun header_withoutTrailingComponentHidesTheTrailingSlot() {
        val root = render(MatrixCardContent(primary = TemplateText("11,056")), Dimensions(348, 132))

        assertThat(
                root
                    .findViewById<View>(R.id.glance_adaptive_appwidget_remoteviews_slot_trailing)
                    .visibility
            )
            .isEqualTo(View.GONE)
    }

    @Test
    fun header_withOnlyATrailingComponentIsShown() {
        val root =
            render(
                MatrixCardContent(trailing = IconButton(GLYPH, "Other workouts", RUN), hero = week),
                Dimensions(348, 176),
            )

        assertThat(
                root
                    .findViewById<View>(R.id.glance_adaptive_appwidget_remoteviews_slot_header)
                    .visibility
            )
            .isEqualTo(View.VISIBLE)
        assertThat(iconButton(root).contentDescription.toString()).isEqualTo("Other workouts")
    }

    @Test
    fun iconButton_withoutActionIsNotClickable() {
        val root = render(MatrixBulbContent(IconButton(GLYPH, "Run")), Dimensions(88, 88))

        assertThat(iconButton(root).isClickable).isFalse()
        assertThat(iconButton(root).hasOnClickListeners()).isFalse()
    }

    @Test
    fun forWidget_callbackTapIsDeliveredForTheWidget() {
        val surface = AppWidgetSurface.MOBILE_HOME_SCREEN.withDimensions(Dimensions(88, 88))
        val content =
            MatrixBulbContent(
                IconButton(GLYPH, "Run", GlanceWidgetAction.Callback(NoOpCallback::class.java))
            )
        val output =
            RemoteViewsMatrixRenderer.forWidget("dashboard").render(template, content, surface)
                as RemoteViewsRenderOutput
        val view = output.provider(context).apply(context, FrameLayout(context))

        view
            .findViewById<View>(R.id.glance_adaptive_appwidget_remoteviews_icon_button)
            .performClick()

        val broadcast = shadowOf(context as Application).broadcastIntents.last()
        assertThat(broadcast.getStringExtra(EXTRA_WIDGET_NAME)).isEqualTo("dashboard")
    }

    @Test
    fun iconButton_callbackWithoutAWidgetNameIsNotClickable() {
        val root =
            render(
                MatrixBulbContent(
                    IconButton(GLYPH, "Run", GlanceWidgetAction.Callback(NoOpCallback::class.java))
                ),
                Dimensions(88, 88),
            )

        assertThat(iconButton(root).isClickable).isFalse()
        assertThat(iconButton(root).hasOnClickListeners()).isFalse()
    }

    @Test
    fun iconButton_inAnActionSlotIsATouchTargetSizedCircle() {
        val root =
            render(
                MatrixCardContent(
                    primary = TemplateText("11,056"),
                    actions = listOf(TemplateText("1"), IconButton(GLYPH, "More", RUN)),
                ),
                Dimensions(348, 132),
            )

        val button = iconButton(root)
        assertThat(iconButtonGlyph(root).maxWidth).isEqualTo(24f.dpToPx(context.density))
        assertThat(button.paddingLeft + button.paddingRight)
            .isEqualTo(48f.dpToPx(context.density) - 24f.dpToPx(context.density))
        assertThat(button.hasOnClickListeners()).isTrue()
    }

    @Test
    fun iconGrid_drawsItsTilesRowByRowAndKeepsEmptyCells() {
        val root =
            render(
                MatrixCardContent(
                    hero =
                        IconGrid(
                            List(5) { IconTile(GLYPH, "Workout $it", RUN) },
                            columns = 3,
                            rows = 2,
                        )
                ),
                Dimensions(264, 176),
            )

        val grid =
            root.findViewById<ViewGroup>(R.id.glance_adaptive_appwidget_remoteviews_icon_grid)
        assertThat(grid.childCount).isEqualTo(2)
        val rows = List(grid.childCount) { grid.getChildAt(it) as ViewGroup }
        assertThat(rows.map { it.childCount }).containsExactly(3, 3)
        assertThat(tile(rows[0], 0).contentDescription.toString()).isEqualTo("Workout 0")
        assertThat(tile(rows[1], 1).contentDescription.toString()).isEqualTo("Workout 4")
        // The sixth cell is empty but keeps its place, so the tiles keep their size.
        assertThat(tile(rows[1], 2).visibility).isEqualTo(View.INVISIBLE)
        assertThat(rows[1].getChildAt(2).width).isEqualTo(rows[1].getChildAt(1).width)
    }

    @Test
    fun iconGrid_rowsShareTheHeroHeightAndTilesShareTheRowWidth() {
        val root =
            render(
                MatrixCardContent(
                    hero = IconGrid(List(6) { IconTile(GLYPH, "Workout $it") }, 3, 2),
                    padding = 12.dp,
                ),
                Dimensions(264, 264),
            )

        val grid =
            root.findViewById<ViewGroup>(R.id.glance_adaptive_appwidget_remoteviews_icon_grid)
        val rows = List(grid.childCount) { grid.getChildAt(it) as ViewGroup }
        assertThat(rows[0].height - rows[1].height).isIn(-1..1)
        val tiles = List(3) { tile(rows[0], it) }
        assertThat(tiles.maxOf { it.width } - tiles.minOf { it.width }).isAtMost(1)
        // The rows fill the grid, with the design's gap between them.
        assertThat(rows[1].bottom).isEqualTo(grid.height)
        assertThat(rows[1].top - rows[0].bottom).isEqualTo(4f.dpToPx(context.density))
        assertThat(
                rows[0]
                    .getChildAt(0)
                    .findViewById<ImageView>(
                        R.id.glance_adaptive_appwidget_remoteviews_icon_tile_glyph
                    )
                    .maxWidth
            )
            .isEqualTo(30f.dpToPx(context.density))
    }

    @Test
    fun iconGrid_cellFilledAgainOnAnUpdateShowsItsTile() {
        val surface = AppWidgetSurface.MOBILE_HOME_SCREEN.withDimensions(Dimensions(264, 176))
        fun content(tiles: Int) =
            MatrixCardContent(hero = IconGrid(List(tiles) { IconTile(GLYPH, "Workout $it") }, 3, 2))
        val view =
            RemoteViewsMatrixRenderer.renderRemoteViews(context, template, content(5), surface)
                .apply(context, FrameLayout(context))

        // The host reapplies an update onto the hierarchy it already has, recycling the cells.
        RemoteViewsMatrixRenderer.renderRemoteViews(context, template, content(6), surface)
            .reapply(context, view)

        val grid =
            view.findViewById<ViewGroup>(R.id.glance_adaptive_appwidget_remoteviews_icon_grid)
        val lastRow = grid.getChildAt(1) as ViewGroup
        assertThat(tile(lastRow, 2).visibility).isEqualTo(View.VISIBLE)
        assertThat(tile(lastRow, 2).contentDescription.toString()).isEqualTo("Workout 5")
    }

    @Test
    fun iconGrid_tileShowsItsLabelBeneathItsGlyph() {
        val root =
            render(
                MatrixCardContent(
                    hero = IconGrid(listOf(IconTile(GLYPH, "Rachel", RUN)), columns = 1, rows = 1)
                ),
                Dimensions(176, 132),
            )

        val row =
            root.findViewById<ViewGroup>(R.id.glance_adaptive_appwidget_remoteviews_icon_grid_row)
        val labelled = tile(row, 0)
        val label =
            labelled.findViewById<TextView>(
                R.id.glance_adaptive_appwidget_remoteviews_icon_tile_label
            )
        val glyph =
            labelled.findViewById<View>(R.id.glance_adaptive_appwidget_remoteviews_icon_tile_glyph)
        assertThat(label.visibility).isEqualTo(View.VISIBLE)
        assertThat(label.text.toString()).isEqualTo("Rachel")
        assertThat(label.top).isAtLeast(glyph.bottom)
        assertThat(labelled.contentDescription.toString()).isEqualTo("Rachel")
    }

    @Test
    fun iconGrid_withoutLabelsDrawsGlyphsAloneButStillDescribesTiles() {
        val root =
            render(
                MatrixCardContent(
                    hero =
                        IconGrid(
                            listOf(IconTile(GLYPH, "Rachel", RUN), IconTile(GLYPH, "Walk")),
                            columns = 2,
                            rows = 1,
                            showLabels = false,
                        )
                ),
                Dimensions(176, 132),
            )

        val row =
            root.findViewById<ViewGroup>(R.id.glance_adaptive_appwidget_remoteviews_icon_grid_row)
        for (column in 0 until 2) {
            val bare = tile(row, column)
            assertThat(
                    bare
                        .findViewById<View>(
                            R.id.glance_adaptive_appwidget_remoteviews_icon_tile_label
                        )
                        .visibility
                )
                .isEqualTo(View.GONE)
        }
        assertThat(tile(row, 0).contentDescription.toString()).isEqualTo("Rachel")
        assertThat(tile(row, 1).contentDescription.toString()).isEqualTo("Walk")
    }

    @Test
    fun iconGrid_tileTapRunsItsActionAndInertTilesAreNotClickable() {
        val root =
            render(
                MatrixCardContent(
                    hero =
                        IconGrid(
                            listOf(IconTile(GLYPH, "Run", RUN), IconTile(GLYPH, "Walk")),
                            columns = 2,
                            rows = 1,
                        )
                ),
                Dimensions(176, 132),
            )

        val row =
            root.findViewById<ViewGroup>(R.id.glance_adaptive_appwidget_remoteviews_icon_grid_row)
        assertThat(tile(row, 1).isClickable).isFalse()
        tile(row, 0).performClick()
        assertThat(shadowOf(context as Application).nextStartedActivity.data)
            .isEqualTo(Uri.parse(RUN_URI))
    }

    @Test
    fun iconGrid_sensitiveTileIsDescribedByItsReplacementOnTheLockScreen() {
        val root =
            render(
                MatrixCardContent(
                    hero =
                        IconGrid(
                            listOf(
                                IconTile(GLYPH, "Call Alex", RUN, sensitiveReplacement = "Call")
                            ),
                            columns = 2,
                            rows = 1,
                        )
                ),
                Dimensions(176, 132),
                AppWidgetSurface.MOBILE_LOCK_SCREEN,
            )

        val row =
            root.findViewById<ViewGroup>(R.id.glance_adaptive_appwidget_remoteviews_icon_grid_row)
        assertThat(tile(row, 0).contentDescription.toString()).isEqualTo("Call")
    }

    @Test
    fun badge_withDefaultOutlineColor_rendersWithSystemThemeOutline() {
        val root =
            render(
                MatrixCardContent(
                    primary = TemplateText("11,056"),
                    actions = listOf(Badge("Z2")),
                ),
                Dimensions(348, 132),
            )

        val badgeLabel =
            root.findViewById<TextView>(R.id.glance_adaptive_appwidget_remoteviews_badge_label)
        assertThat(badgeLabel.text.toString()).isEqualTo("Z2")
        val tertiary = context.getColor(R.color.glance_adaptive_appwidget_remoteviews_tertiary)
        assertThat(badgeOutline(root).colorFilter).isEqualTo(outlineFilter(tertiary))
    }

    @Test
    fun badge_withOutlineColor_rendersWithThatOutline() {
        val yellow = 0xFFFFE347.toInt()
        val root =
            render(
                MatrixCardContent(
                    primary = TemplateText("11,056"),
                    actions = listOf(Badge("Z2", outlineColor = yellow)),
                ),
                Dimensions(348, 132),
            )

        assertThat(badgeOutline(root).colorFilter).isEqualTo(outlineFilter(yellow))
    }

    /** A card with every slot filled. */
    private fun fullCard() =
        MatrixCardContent(
            leading = Ring(progress = 0.55f),
            primary = TemplateText("11,056"),
            secondary = TemplateText("Steps"),
            hero = week,
            actions =
                listOf(
                    TemplateText("1,982 Cal"),
                    TemplateText("3,118 to go"),
                    Badge(label = "Z2", outlineColor = 0xFFFFE347.toInt()),
                ),
        )

    private fun actionRow(root: View): ViewGroup =
        root.findViewById(R.id.glance_adaptive_appwidget_remoteviews_slot_supporting)

    private fun badgeOutline(root: View): ImageView =
        root.findViewById(R.id.glance_adaptive_appwidget_remoteviews_badge_outline)

    /** The filter `ImageView.setColorFilter(color)` sets. */
    private fun outlineFilter(color: Int) = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)

    private fun iconButton(root: View): View =
        root.findViewById(R.id.glance_adaptive_appwidget_remoteviews_icon_button)

    private fun iconButtonGlyph(root: View): ImageView =
        root.findViewById(R.id.glance_adaptive_appwidget_remoteviews_icon_button_glyph)

    private fun tile(row: ViewGroup, column: Int): View =
        row.getChildAt(column).findViewById(R.id.glance_adaptive_appwidget_remoteviews_icon_tile)

    private fun secondary(root: View): TextView =
        root.findViewById(R.id.glance_adaptive_appwidget_remoteviews_secondary)

    private fun render(
        content: MatrixContent,
        dimensions: Dimensions,
        surface: AppWidgetSurface = AppWidgetSurface.MOBILE_HOME_SCREEN,
    ): View {
        val surface = surface.withDimensions(dimensions)
        val rv = RemoteViewsMatrixRenderer.renderRemoteViews(context, template, content, surface)

        val host = FrameLayout(context)
        val view = rv.apply(context, host)
        host.addView(view)

        val widthPx = dimensions.widthDp.toFloat().dpToPx(context.density)
        val heightPx = dimensions.heightDp.toFloat().dpToPx(context.density)
        view.measure(
            MeasureSpec.makeMeasureSpec(widthPx, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(heightPx, MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, widthPx, heightPx)
        return view
    }

    private companion object {
        /** A glyph drawable of the library, declared at 32 dp. */
        val GLYPH = R.drawable.glance_adaptive_appwidget_remoteviews_track_placeholder_steps_glyph

        const val RUN_URI = "app://run"
        val RUN = GlanceWidgetAction.LaunchDeeplink(RUN_URI)

        /** Extra under which a callback broadcast names its widget. */
        const val EXTRA_WIDGET_NAME = "androidx.glance.adaptive.core.extra.WIDGET_NAME"
    }
}

/** A callback that does nothing, for taps that are only checked for delivery. */
class NoOpCallback : GlanceAdaptiveActionCallback {
    override suspend fun onAction(
        context: Context,
        widgetName: String,
        widgetId: String?,
        parameters: Bundle,
    ) {}
}
