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

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.collection.floatListOf
import androidx.glance.adaptive.appwidget.R
import androidx.glance.adaptive.appwidget.components.RemoteViewsActions
import androidx.glance.adaptive.appwidget.components.density
import androidx.glance.adaptive.appwidget.components.dpToPx
import androidx.glance.adaptive.appwidget.matrix.remoteviews.RemoteViewsMatrixRenderer
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import androidx.glance.adaptive.core.action.GlanceAdaptiveActionCallback
import androidx.glance.adaptive.core.action.GlanceWidgetAction
import androidx.glance.adaptive.core.selection.Dimensions
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
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
                    .findViewById<View>(R.id.glance_adaptive_appwidget_remoteviews_primary)
                    .visibility
            )
            .isEqualTo(View.INVISIBLE)
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

    private fun secondary(root: View): TextView =
        root.findViewById(R.id.glance_adaptive_appwidget_remoteviews_secondary)

    private fun render(content: MatrixContent, dimensions: Dimensions): View {
        val surface = AppWidgetSurface.MOBILE_HOME_SCREEN.withDimensions(dimensions)
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
        const val RUN_URI = "app://run"
        val RUN = GlanceWidgetAction.LaunchDeeplink(RUN_URI)
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
