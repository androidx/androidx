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
import android.net.Uri
import android.os.Bundle
import androidx.collection.floatListOf
import androidx.compose.remote.core.operations.layout.modifiers.HostActionOperation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.adaptive.appwidget.components.RenderedDocument
import androidx.glance.adaptive.appwidget.components.TEST_VECTOR_GLYPH
import androidx.glance.adaptive.appwidget.components.captureAndRender
import androidx.glance.adaptive.appwidget.components.dpToPx
import androidx.glance.adaptive.appwidget.components.layoutChildren
import androidx.glance.adaptive.appwidget.matrix.remotecompose.RemoteComposeMatrixRenderer
import androidx.glance.adaptive.appwidget.renderer.AppWidgetRenderOutput
import androidx.glance.adaptive.appwidget.renderer.RemoteComposeRenderOutput
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import androidx.glance.adaptive.appwidget.templates.track.TrackMatrixSelector
import androidx.glance.adaptive.core.action.GlanceAdaptiveAction
import androidx.glance.adaptive.core.action.GlanceAdaptiveActionCallback
import androidx.glance.adaptive.core.renderer.ArchetypeRenderer
import androidx.glance.adaptive.core.selection.Dimensions
import androidx.glance.adaptive.core.slots.Badge
import androidx.glance.adaptive.core.slots.ChartLabel
import androidx.glance.adaptive.core.slots.ContainedRing
import androidx.glance.adaptive.core.slots.Graph
import androidx.glance.adaptive.core.slots.Icon
import androidx.glance.adaptive.core.slots.IconButton
import androidx.glance.adaptive.core.slots.Ring
import androidx.glance.adaptive.core.slots.TemplateText
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate
import androidx.glance.adaptive.core.templates.TrackTemplate
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Suppress("RestrictedApiAndroidX")
@Config(sdk = [37], qualifiers = "xhdpi")
@RunWith(RobolectricTestRunner::class)
class RemoteComposeMatrixRendererTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun fullCard_absorbsSlackInHeroAndSpreadsActionsAcrossBottom() = runTest {
        val content =
            MatrixCardContent(
                leading = Ring(progress = 0.55f, icon = TEST_VECTOR_GLYPH),
                primary = TemplateText("11,056"),
                secondary = TemplateText("Steps"),
                hero =
                    Graph(
                        values = floatListOf(0.5f, 1.0f, 0.8f),
                        labels = listOf("M", "T", "W"),
                        goal = 1.0f,
                        startLabel = ChartLabel("7 Day Avg"),
                        endLabel = ChartLabel("13,843", icon = TEST_VECTOR_GLYPH),
                    ),
                actions =
                    listOf(
                        TemplateText("55%"),
                        TemplateText("1,982 Cal"),
                        Badge("Z2", outlineColor = 0xFFFFE347.toInt()),
                    ),
            )

        val rendered = render(content, Dimensions(348, 264))
        assertThat(rendered.texts())
            .containsAtLeast("11,056", "Steps", "7 Day Avg", "13,843", "55%", "1,982 Cal", "Z2")

        val cardColumn = rendered.firstLayoutChild()
        assertThat(cardColumn.width).isWithin(1f).of(348.dpToPx(context))
        assertThat(cardColumn.height).isWithin(1f).of(264.dpToPx(context))

        // Header + weighted hero + action row = 3 children in the card column.
        val children = cardColumn.layoutChildren()
        assertThat(children).hasSize(3)
        val header = children[0]
        val hero = children[1]
        val actionRow = children[2]
        // Coordinates are relative to the card's padded content box (264 - 2 * 20 = 224 dp tall).
        assertThat(header.y).isWithin(1f).of(0f)
        assertThat(hero.height).isGreaterThan(header.height)
        assertThat(actionRow.y + actionRow.height).isWithin(1f).of((264 - 40).dpToPx(context))

        // First action starts at left edge; third action reaches the right edge.
        val actionChildren = actionRow.layoutChildren()
        assertThat(actionChildren.first().x).isEqualTo(0f)
        assertThat(actionChildren.last().x + actionChildren.last().width)
            .isWithin(1f)
            .of(actionRow.width)
    }

    @Test
    fun cardWithoutHero_pinsActionsToBottomAndKeepsEmptyLeadingActionPlace() = runTest {
        val twoActionsWithNullFirst =
            MatrixCardContent(
                primary = TemplateText("11,056"),
                secondary = TemplateText("Steps"),
                actions = listOf(null, TemplateText("1,982 Cal")),
            )

        val rendered = render(twoActionsWithNullFirst, Dimensions(260, 140))
        val cardChildren = rendered.firstLayoutChild().layoutChildren()
        // Header + weighted spacer + action row = 3 children.
        assertThat(cardChildren).hasSize(3)
        val header = cardChildren[0]
        val actionRow = cardChildren[2]
        assertThat(header.y).isWithin(1f).of(0f)
        assertThat(actionRow.y + actionRow.height).isWithin(1f).of((140 - 40).dpToPx(context))

        // Missing first action keeps its weighted spacer so the second action stays at the end.
        val trailingAction = actionRow.layoutChildren().last()
        assertThat(trailingAction.x + trailingAction.width).isWithin(1f).of(actionRow.width)

        val singleAction =
            render(
                MatrixCardContent(
                    primary = TemplateText("11,056"),
                    actions = listOf(TemplateText("55%")),
                ),
                Dimensions(176, 132),
            )
        val singleRow = singleAction.firstLayoutChild().layoutChildren().last()
        val centeredItem = singleRow.layoutChildren().single()
        assertThat(centeredItem.x).isGreaterThan(0f)
        assertThat(centeredItem.x + centeredItem.width).isLessThan(singleRow.width)
    }

    @Test
    fun headerOnlyCard_centersVertically_and_emptyCardRendersBackgroundOnly() = runTest {
        val headerOnly =
            render(
                MatrixCardContent(
                    leading = Ring(progress = 0.55f, icon = TEST_VECTOR_GLYPH),
                    primary = TemplateText("11,056"),
                ),
                Dimensions(176, 120),
            )
        // Content box is 120 - 32 = 88 dp tall; 56 dp header centers at y = 16 dp inside it.
        val header = headerOnly.firstLayoutChild().layoutChildren().single()
        assertThat(header.y).isWithin(1f).of(16.dpToPx(context))
        assertThat(header.y + header.height).isWithin(1f).of((88 - 16).dpToPx(context))

        val empty = render(MatrixCardContent(), Dimensions(176, 88))
        assertThat(empty.firstLayoutChild().layoutChildren()).isEmpty()
    }

    @Test
    fun stackedHeader_stacksSlotsDropsTrailingAndKeepsEmptyPrimaryPlace() = runTest {
        val stacked =
            render(
                MatrixCardContent(
                    leading = Ring(progress = 0.55f, icon = TEST_VECTOR_GLYPH),
                    primary = TemplateText("11,056"),
                    secondary = TemplateText("Steps"),
                    trailing =
                        IconButton(
                            icon = TEST_VECTOR_GLYPH,
                            contentDescription = "Action",
                            action = GlanceAdaptiveAction.DeeplinkAction(Uri.parse("app://action")),
                        ),
                ),
                Dimensions(88, 176),
            )
        val headerChildren = stacked.firstLayoutChild().layoutChildren().single().layoutChildren()
        // Leading + 8dp spacer + primary + 8dp spacer + secondary = 5 children; trailing dropped.
        assertThat(headerChildren).hasSize(5)
        assertThat(headerChildren[2].y).isGreaterThan(headerChildren[0].y)
        assertThat(headerChildren[4].y).isGreaterThan(headerChildren[2].y)
        assertThat(stacked.captured.pendingIntents.size).isEqualTo(0)

        val missingPrimary =
            render(
                MatrixCardContent(secondary = TemplateText("Steps")),
                Dimensions(88, 132),
            )
        val missingPrimaryChildren =
            missingPrimary.firstLayoutChild().layoutChildren().single().layoutChildren()
        // Primary placeholder + 8dp spacer + secondary = 3 children.
        assertThat(missingPrimaryChildren).hasSize(3)
        assertThat(missingPrimaryChildren.last().y).isGreaterThan(0f)
    }

    @Test
    fun compactHeaderAndForWidgetCallbacks_renderAndRouteTaps() = runTest {
        val callbackAction = GlanceAdaptiveAction.CallbackAction<NoOpCallback>()
        val deeplinkAction = GlanceAdaptiveAction.DeeplinkAction(Uri.parse("app://workout"))
        val interactiveCard =
            MatrixCardContent(
                leading =
                    Icon(icon = TEST_VECTOR_GLYPH, contentDescription = "Run", contained = true),
                leadingSize = 56.dp,
                primary = TemplateText("11,056"),
                primaryTextSize = 20.sp,
                secondary = TemplateText("Steps today"),
                trailing =
                    IconButton(
                        icon = TEST_VECTOR_GLYPH,
                        contentDescription = "Refresh",
                        action = callbackAction,
                    ),
                hero = Graph(values = floatListOf(0.4f, 0.8f, 1.0f)),
                actions =
                    listOf(
                        IconButton(
                            icon = TEST_VECTOR_GLYPH,
                            contentDescription = "Open workout",
                            action = deeplinkAction,
                        ),
                        IconButton(
                            icon = TEST_VECTOR_GLYPH,
                            contentDescription = "Log lap",
                            action = callbackAction,
                        ),
                    ),
            )

        // Default renderer: deeplink is tappable (1 PendingIntent), callbacks are inert.
        val unnamed = render(interactiveCard, Dimensions(344, 264))
        assertThat(unnamed.texts()).containsAtLeast("11,056", "Steps today")
        assertThat(unnamed.captured.pendingIntents.size).isEqualTo(1)
        assertThat(shadowOf(unnamed.captured.pendingIntents[0]!!).savedIntent.dataString)
            .isEqualTo("app://workout")
        assertThat(unnamed.operationsOfType<HostActionOperation>()).hasSize(1)

        // Compact header stacks secondary below primary inside the weighted header text column,
        // and draws the trailing pill button at 64x56dp on the right.
        val headerRow = unnamed.firstLayoutChild().layoutChildren().first()
        val headerChildren = headerRow.layoutChildren()
        val textColumn = headerChildren[2]
        assertThat(textColumn.layoutChildren()).hasSize(2)
        assertThat(textColumn.layoutChildren()[1].y).isGreaterThan(textColumn.layoutChildren()[0].y)
        val trailingPill = headerChildren.last()
        assertThat(trailingPill.width).isWithin(1f).of(64.dpToPx(context))
        assertThat(trailingPill.height).isWithin(1f).of(56.dpToPx(context))

        // Named renderer via forWidget("track_steps"): delivers all 3 tappable buttons,
        // deduplicating the shared callback PendingIntent (1 deeplink + 1 callback = 2).
        val named =
            render(
                interactiveCard,
                Dimensions(344, 264),
                renderer = RemoteComposeMatrixRenderer.forWidget("track_steps"),
            )
        assertThat(named.captured.pendingIntents.size).isEqualTo(2)
        assertThat(named.operationsOfType<HostActionOperation>()).hasSize(3)
    }

    @Test
    fun heroOnlyCard_hidesEmptyHeaderSoHeroTakesFullPaddedHeight() = runTest {
        val heroOnly =
            render(
                MatrixCardContent(hero = Graph(values = floatListOf(0.5f, 1.0f))),
                Dimensions(176, 132),
            )
        val cardChildren = heroOnly.firstLayoutChild().layoutChildren()
        val heroBox = cardChildren.single()
        assertThat(heroBox.y).isWithin(1f).of(0f)
        assertThat(heroBox.height).isWithin(1f).of((132 - 32).dpToPx(context))
    }

    @Test
    fun bulb_centersContainedRingOrIconButtonInsideShorterAxisCircle() = runTest {
        val ringBulb =
            render(
                MatrixBulbContent(
                    ContainedRing(
                        progress = 0.55f,
                        value = "11,056",
                        icon = TEST_VECTOR_GLYPH,
                        sensitiveReplacement = "--",
                    )
                ),
                Dimensions(120, 88),
                surface = AppWidgetSurface.MOBILE_LOCK_SCREEN,
            )
        val ringCircle = ringBulb.firstLayoutChild().layoutChildren().single()
        assertThat(ringCircle.width).isWithin(1f).of(88.dpToPx(context))
        assertThat(ringCircle.height).isWithin(1f).of(88.dpToPx(context))
        assertThat(ringBulb.texts()).containsExactly("--")

        val buttonBulb =
            render(
                MatrixBulbContent(
                    IconButton(
                        icon = TEST_VECTOR_GLYPH,
                        contentDescription = "Quick action",
                        action = GlanceAdaptiveAction.DeeplinkAction(Uri.parse("app://quick")),
                    )
                ),
                Dimensions(56, 56),
                surface = AppWidgetSurface.MOBILE_LOCK_SCREEN,
            )
        val buttonCircle = buttonBulb.firstLayoutChild().layoutChildren().single()
        assertThat(buttonCircle.width).isWithin(1f).of(56.dpToPx(context))
        assertThat(buttonBulb.captured.pendingIntents.size).isEqualTo(1)
    }

    @Test
    fun trackMatrixSelector_rendersAllTierShapesThroughRemoteComposeMatrixRenderer() = runTest {
        val template =
            TrackTemplate(
                leading = Ring(progress = 0.55f, icon = TEST_VECTOR_GLYPH),
                primary = TemplateText("11,056"),
                secondary = TemplateText("Steps"),
                hero =
                    Graph(
                        values = floatListOf(0.65f, 1.0f, 0.80f, 1.0f, 0.45f, 1.0f, 0.55f),
                        labels = listOf("M", "T", "W", "T", "F", "S", "S"),
                        emphasized = booleanArrayOf(false, true, false, true, false, true, false),
                        startLabel = ChartLabel("7 Day Avg"),
                        endLabel = ChartLabel("13,843", icon = TEST_VECTOR_GLYPH),
                    ),
                supporting =
                    listOf(
                        TemplateText("55%"),
                        TemplateText("3,118 to go"),
                        Badge("Z2"),
                    ),
            )

        for (dimensions in
            listOf(
                Dimensions(88, 88), // W1H1 bulb
                Dimensions(88, 176), // W1H3 stacked card
                Dimensions(176, 88), // W2H1 row header card
                Dimensions(260, 140), // W3H2 header + 2 actions
                Dimensions(348, 264), // W4H4 full card with captioned graph + 3 actions
            )) {
            val sized = AppWidgetSurface.MOBILE_HOME_SCREEN.withDimensions(dimensions)
            val matrix = TrackMatrixSelector.select(template, sized)
            val rendered = render(matrix, dimensions)
            assertThat(rendered.texts()).isNotEmpty()
        }
    }

    private suspend fun render(
        content: MatrixContent,
        dimensions: Dimensions,
        surface: AppWidgetSurface = AppWidgetSurface.MOBILE_HOME_SCREEN,
        renderer:
            ArchetypeRenderer<
                GlanceAdaptiveTemplate,
                AppWidgetSurface,
                MatrixContent,
                AppWidgetRenderOutput,
            > =
            RemoteComposeMatrixRenderer,
    ): RenderedDocument {
        val sizedSurface = surface.withDimensions(dimensions)
        val output = renderer.render(DummyTemplate, content, sizedSurface)
        assertThat(output).isInstanceOf(RemoteComposeRenderOutput::class.java)
        val rcOutput = output as RemoteComposeRenderOutput
        return captureAndRender(
            context = context,
            widthDp = dimensions.widthDp,
            heightDp = dimensions.heightDp,
            content = rcOutput.content,
        )
    }

    private object DummyTemplate : GlanceAdaptiveTemplate(templateId = 0)

    private class NoOpCallback : GlanceAdaptiveActionCallback {
        override suspend fun onAction(
            context: Context,
            widgetName: String,
            widgetId: String?,
            parameters: Bundle,
        ) {}
    }
}
