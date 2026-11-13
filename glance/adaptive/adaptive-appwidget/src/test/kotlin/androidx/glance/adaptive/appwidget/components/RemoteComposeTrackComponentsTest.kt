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

package androidx.glance.adaptive.appwidget.components

import android.content.Context
import androidx.collection.floatListOf
import androidx.collection.intListOf
import androidx.compose.remote.core.operations.ColorExpression
import androidx.compose.remote.core.operations.ColorTheme
import androidx.compose.remote.core.operations.DrawArc
import androidx.compose.remote.core.operations.DrawCircle
import androidx.compose.remote.core.operations.DrawRoundRect
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.AndroidSystemColorMap
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.ui.unit.dp
import androidx.glance.adaptive.appwidget.components.badge.Content
import androidx.glance.adaptive.appwidget.components.graph.Content
import androidx.glance.adaptive.appwidget.components.graph.GraphDefaults
import androidx.glance.adaptive.appwidget.components.ring.Content
import androidx.glance.adaptive.appwidget.components.ring.RingDefaults
import androidx.glance.adaptive.appwidget.components.text.SupportingContent
import androidx.glance.adaptive.core.slots.Badge
import androidx.glance.adaptive.core.slots.ChartLabel
import androidx.glance.adaptive.core.slots.ContainedRing
import androidx.glance.adaptive.core.slots.Graph
import androidx.glance.adaptive.core.slots.Ring
import androidx.glance.adaptive.core.slots.TemplateText
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.roundToInt
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Suppress("RestrictedApiAndroidX")
@Config(sdk = [37], qualifiers = "xhdpi")
@RunWith(RobolectricTestRunner::class)
class RemoteComposeTrackComponentsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun ring_rendersProgressArcAndOptionalGlyph() = runTest {
        val withGlyph =
            captureAndRender(context, widthDp = 56, heightDp = 56) {
                Ring(progress = 0.55f, icon = TEST_VECTOR_GLYPH).Content(56.dp)
            }
        val ringBox = withGlyph.firstLayoutChild()
        assertThat(ringBox.width).isWithin(1f).of(56.dpToPx(context))
        assertThat(ringBox.height).isWithin(1f).of(56.dpToPx(context))
        assertThat(ringBox.layoutChildren()).hasSize(2)
        // Fill circle + full track arc + 55% progress arc.
        assertThat(withGlyph.operationsOfType<DrawCircle>()).hasSize(1)
        assertThat(withGlyph.operationsOfType<DrawArc>()).hasSize(2)

        val zeroProgress =
            captureAndRender(context, widthDp = 56, heightDp = 56) {
                Ring(progress = 0f, icon = null).Content(56.dp)
            }
        assertThat(zeroProgress.firstLayoutChild().layoutChildren()).hasSize(1)
        // Fill circle + full track arc only (no progress arc when progress is 0).
        assertThat(zeroProgress.operationsOfType<DrawCircle>()).hasSize(1)
        assertThat(zeroProgress.operationsOfType<DrawArc>()).hasSize(1)
        // Both secondaryContainer (fill) and primary (track) are exposed for host theme updates.
        assertThat(checkNotNull(zeroProgress.document.themedColors)).hasSize(2)
    }

    @Test
    fun containedRing_rendersFittedValueAndOptionalGlyphInsideCircle() = runTest {
        val ring = ContainedRing(progress = 0.55f, value = "11,056", icon = TEST_VECTOR_GLYPH)
        val geometry = RingDefaults.containedRingGeometry(88.dp)
        val nominal = captureAndRender(context, widthDp = 88, heightDp = 88) { ring.Content(88.dp) }
        val container = nominal.firstLayoutChild()
        assertThat(container.width).isWithin(1f).of(88.dpToPx(context))
        assertThat(container.height).isWithin(1f).of(88.dpToPx(context))
        assertThat(nominal.texts())
            .containsExactly(fitMetric("11,056", geometry.valueMaxWidth.value, geometry.fontSize))
        assertThat(nominal.operationsOfType<DrawCircle>()).hasSize(1)
        assertThat(nominal.operationsOfType<DrawArc>()).hasSize(2)

        val compact =
            captureAndRender(context, widthDp = 48, heightDp = 48) {
                ContainedRing(progress = null, value = "1,234,567").Content(48.dp)
            }
        val fittedText = compact.texts().single()
        assertThat(fittedText).isNotEqualTo("1,234,567")
        assertThat(fittedText.length).isLessThan("1,234,567".length)
        assertThat(compact.operationsOfType<DrawArc>()).hasSize(1)
    }

    @Test
    fun graph_barsHaveConsistentWidthFlushOuterEdgesAndGoalScaling() = runTest {
        val week =
            Graph(
                values = floatListOf(*FloatArray(7) { i -> 1_000f * (i + 1) }),
                labels = listOf("T", "F", "S", "S", "M", "T", "W"),
                goal = 5_000f,
            )
        val rendered =
            captureAndRender(context, widthDp = 308, heightDp = 120) { week.Content(16.dp) }

        val chartColumn = rendered.firstLayoutChild()
        val barsRow = chartColumn.layoutChildren().first { it is RowLayout }
        val columns = barsRow.layoutChildren().filterIsInstance<ColumnLayout>()
        assertThat(columns).hasSize(7)

        assertThat(columns.first().x).isEqualTo(0f)
        assertThat(columns.last().x + columns.last().width).isWithin(1f).of(barsRow.width)

        val barFills = columns.map { column ->
            val indicatorBox = column.layoutChildren().first()
            indicatorBox.layoutChildren().single()
        }
        val barWidths = barFills.map { it.width }
        assertThat(barWidths.max() - barWidths.min()).isAtMost(1f)

        // Last bar exceeds goal (7,000 vs 5,000 -> scale = 7,000, fraction = 1.0) and fills the
        // entire indicator box above its label.
        val lastColumn = columns.last()
        val lastIndicatorBox = lastColumn.layoutChildren().first()
        val lastBarFill = barFills.last()
        val lastLabel = lastColumn.layoutChildren().filterIsInstance<CoreText>().single()
        assertThat(lastColumn.height).isWithin(1f).of(barsRow.height)
        assertThat(lastBarFill.y).isEqualTo(0f)
        assertThat(lastBarFill.height).isWithin(1f).of(lastIndicatorBox.height)
        assertThat(lastLabel.y + lastLabel.height).isWithin(1f).of(lastColumn.height)

        // First bar (1,000 / 7,000) is shorter and anchored to the bottom of its indicator box.
        val firstIndicatorBox = columns.first().layoutChildren().first()
        val firstBarFill = barFills.first()
        assertThat(firstBarFill.height).isLessThan(lastBarFill.height)
        assertThat(firstBarFill.y + firstBarFill.height).isWithin(1f).of(firstIndicatorBox.height)
        assertThat(rendered.texts()).containsExactly("T", "F", "S", "S", "M", "T", "W").inOrder()
    }

    @Test
    fun graph_dottedEmphasizedIndicesAndChartLabelsWithIcons_renderExpectedLayout() = runTest {
        val water =
            Graph(values = floatListOf(*FloatArray(8) { if (it < 6) 1f else 0f }), dotted = true)
        val dotted = captureAndRender(context, widthDp = 308, heightDp = 96) { water.Content(8.dp) }
        val barsRow = dotted.firstLayoutChild().layoutChildren().first { it is RowLayout }
        val columns = barsRow.layoutChildren().filterIsInstance<ColumnLayout>()
        assertThat(columns).hasSize(8)

        val dots = columns.map { it.layoutChildren().single() }
        assertThat(dots.map { it.height.roundToInt() }.toSet())
            .containsExactly(12.dpToPxInt(context))
        assertThat(dots[0].width).isWithin(1f).of(dots[0].height)
        assertThat(dotted.texts()).isEmpty()

        val captioned =
            Graph(
                values = floatListOf(0.4f, 0.9f, 0.7f),
                labels = listOf("M", "T", "W"),
                emphasized = booleanArrayOf(false, true, false),
                startLabel = ChartLabel("7 Day Avg"),
                endLabel = ChartLabel("13,843", icon = TEST_VECTOR_GLYPH),
            )
        val renderedCaptioned =
            captureAndRender(context, widthDp = 308, heightDp = 140) { captioned.Content(16.dp) }
        assertThat(renderedCaptioned.texts())
            .containsExactly("M", "T", "W", "7 Day Avg", "13,843")
            .inOrder()

        val descriptionRow = renderedCaptioned.firstLayoutChild().layoutChildren().last()
        val endSlotBox = descriptionRow.layoutChildren().last()
        val endCaptionRow = endSlotBox.layoutChildren().single()
        val captionIconBox = endCaptionRow.layoutChildren().first()
        assertThat(captionIconBox.width)
            .isWithin(1f)
            .of(GraphDefaults.CaptionIconSize.value.roundToInt().dpToPx(context))
    }

    @Test
    fun supportingText_and_badge_renderExpectedLayoutAndPillOutline() = runTest {
        val supporting =
            captureAndRender(context, widthDp = 120, heightDp = 32) {
                TemplateText("1,982 Cal").SupportingContent()
            }
        assertThat(supporting.texts()).containsExactly("1,982 Cal")
        val blendedColorOp = supporting.operationsOfType<ColorExpression>().first()
        val blendedArgb = supporting.textContext.getColor(blendedColorOp.mId)
        assertThat(blendedArgb ushr 24).isEqualTo(0xFF)
        assertThat(blendedArgb and 0x00FFFFFF).isNotEqualTo(0)

        val outlineColor = 0xFFFFE347.toInt()
        val badgeWithDots =
            captureAndRender(context, widthDp = 140, heightDp = 36) {
                Badge(
                        label = "Z2",
                        outlineColor = outlineColor,
                        dotColors =
                            intListOf(
                                0xFFFFA4A4.toInt(),
                                0xFFA0FF47.toInt(),
                                0xFF47A6FF.toInt(),
                            ),
                    )
                    .Content()
            }
        assertThat(badgeWithDots.texts()).containsExactly("Z2")
        val badgeRow = badgeWithDots.firstLayoutChild()
        val children = badgeRow.layoutChildren()
        // 1 leading dot + pill box + 2 trailing dots = 4 direct children (single BoxLayout per
        // dot).
        assertThat(children).hasSize(4)
        assertThat(children[0].layoutChildren()).isEmpty()
        assertThat(children[2].layoutChildren()).isEmpty()
        assertThat(children[3].layoutChildren()).isEmpty()

        val pillBox = children[1]
        val pillCanvasOps = checkNotNull(pillBox.getCanvasOperations()).list
        val roundRects = pillCanvasOps.filterIsInstance<DrawRoundRect>()
        // Outer outline pill + 4 dp inset inner background pill.
        assertThat(roundRects).hasSize(2)

        val defaultOutlineBadge =
            captureAndRender(context, widthDp = 100, heightDp = 36) {
                Badge(label = "HR", outlineColor = null).Content()
            }
        assertThat(defaultOutlineBadge.texts()).containsExactly("HR")
        // Both tertiary (outline), widgetBackground (fill), and primary (label) are exposed at the
        // layout container level for host theme updates.
        assertThat(defaultOutlineBadge.document.themedColors).hasSize(3)
        val defaultPill = defaultOutlineBadge.firstLayoutChild().layoutChildren().single()
        assertThat(
                checkNotNull(defaultPill.getCanvasOperations())
                    .list
                    .filterIsInstance<DrawRoundRect>()
            )
            .hasSize(2)
    }

    @Test
    fun palette_matchesRemoteViewsColorResources() = runTest {
        val paletteEntries =
            linkedMapOf(
                "glance_adaptive_appwidget_remoteviews_primary" to RemoteComposePalette.primary,
                "glance_adaptive_appwidget_remoteviews_secondary_container" to
                    RemoteComposePalette.secondaryContainer,
                "glance_adaptive_appwidget_remoteviews_tertiary" to RemoteComposePalette.tertiary,
                "glance_adaptive_appwidget_remoteviews_on_surface_variant" to
                    RemoteComposePalette.onSurfaceVariant,
                "glance_adaptive_appwidget_remoteviews_widget_background" to
                    RemoteComposePalette.widgetBackground,
                "glance_adaptive_appwidget_remoteviews_primary_container" to
                    RemoteComposePalette.primaryContainer,
                "glance_adaptive_appwidget_remoteviews_on_primary_container" to
                    RemoteComposePalette.onPrimaryContainer,
                "glance_adaptive_appwidget_remoteviews_on_secondary_container" to
                    RemoteComposePalette.onSecondaryContainer,
                "glance_adaptive_appwidget_remoteviews_surface_container_highest" to
                    RemoteComposePalette.surfaceContainerHighest,
            )

        val lightFallbacks = readXmlColors("src/main/res/values/remoteviews_colors.xml")
        val darkFallbacks = readXmlColors("src/main/res/values-night/remoteviews_colors.xml")
        val lightDynamic = readXmlColors("src/main/res/values-v31/remoteviews_colors.xml")
        val darkDynamic = readXmlColors("src/main/res/values-night-v31/remoteviews_colors.xml")

        assertThat(paletteEntries.keys).containsExactlyElementsIn(lightFallbacks.keys)

        for ((resName, remoteColor) in paletteEntries) {
            val rendered =
                captureAndRender(context, widthDp = 16, heightDp = 16) {
                    RemoteBox(
                        modifier = RemoteModifier.size(16.rdp).registerDynamicColor(remoteColor)
                    )
                }
            val themeOp: ColorTheme = checkNotNull(rendered.document.themedColors).single()
            assertWithMessage("$resName lightFallback")
                .that(Integer.toHexString(themeOp.mLightModeFallback))
                .isEqualTo(
                    Integer.toHexString(
                        lightFallbacks.getValue(resName).substring(1).toLong(16).toInt()
                    )
                )
            assertWithMessage("$resName darkFallback")
                .that(Integer.toHexString(themeOp.mDarkModeFallback))
                .isEqualTo(
                    Integer.toHexString(
                        darkFallbacks.getValue(resName).substring(1).toLong(16).toInt()
                    )
                )
            val expectedLightSysRes =
                android.R.color::class
                    .java
                    .getField(lightDynamic.getValue(resName).removePrefix("@android:color/"))
                    .getInt(null)
            val expectedDarkSysRes =
                android.R.color::class
                    .java
                    .getField(darkDynamic.getValue(resName).removePrefix("@android:color/"))
                    .getInt(null)
            assertWithMessage("$resName lightColorRes")
                .that(AndroidSystemColorMap.getResourceId(themeOp.mLightModeIndex.toInt()))
                .isEqualTo(expectedLightSysRes)
            assertWithMessage("$resName darkColorRes")
                .that(AndroidSystemColorMap.getResourceId(themeOp.mDarkModeIndex.toInt()))
                .isEqualTo(expectedDarkSysRes)
        }
    }

    private fun readXmlColors(path: String): Map<String, String> {
        val file = File(path)
        assertWithMessage("Expected $path under ${File("").absolutePath}")
            .that(file.isFile)
            .isTrue()
        val nodes =
            DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(file)
                .getElementsByTagName("color")
        return buildMap {
            for (i in 0 until nodes.length) {
                val node = nodes.item(i)
                put(node.attributes.getNamedItem("name").nodeValue, node.textContent.trim())
            }
        }
    }
}
