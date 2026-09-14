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

package androidx.glance.adaptive.appwidget.ui.templates

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.glance.LocalContext
import androidx.glance.adaptive.core.ui.selection.HeightTier
import androidx.glance.adaptive.core.ui.selection.WidthTier
import androidx.glance.adaptive.core.ui.templates.TrackTemplate
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasText
import androidx.glance.testing.unit.hasTextEqualTo
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers the Glance hierarchy [TrackLayout] actually emits, per cell of the breakpoint matrix.
 *
 * [TrackSlotsTest] already pins down which slots *should* be enabled; these tests confirm the
 * composable honours that plan, and — just as importantly — that composing it does not throw.
 *
 * Robolectric is required because the progress ring rasterizes through a real `Canvas`.
 */
@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class TrackLayoutTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val template =
        TrackTemplate(
            title = "11,056",
            subtitle = "Steps",
            progress = 0.55f,
            statusText = "55%",
        )

    @Test
    fun titleSlot_isAlwaysEmitted() = runGlanceAppWidgetUnitTest {
        provideComposable { Track(template, WidthTier.W2, HeightTier.H1) }

        onNode(hasText("11,056")).assertExists()
    }

    @Test
    fun tertiaryLabel_isHiddenBelowW3() = runGlanceAppWidgetUnitTest {
        provideComposable { Track(template, WidthTier.W2, HeightTier.H4) }

        onNode(hasText("Steps")).assertDoesNotExist()
    }

    @Test
    fun tertiaryLabel_isShownFromW3() = runGlanceAppWidgetUnitTest {
        provideComposable { Track(template, WidthTier.W3, HeightTier.H4) }

        onNode(hasText("Steps")).assertExists()
    }

    @Test
    fun supportingRow_isHiddenAtH1() = runGlanceAppWidgetUnitTest {
        provideComposable { Track(template, WidthTier.W4, HeightTier.H1) }

        onNode(hasText("55%")).assertDoesNotExist()
    }

    @Test
    fun supportingRow_showsOnlyTheFirstValueAtW2() = runGlanceAppWidgetUnitTest {
        provideComposable { Track(template, WidthTier.W2, HeightTier.H2) }

        onNode(hasText("55%")).assertExists()
        onNode(hasText("3,118 to go")).assertDoesNotExist()
    }

    @Test
    fun supportingRow_addsTheSecondValueAtW3() = runGlanceAppWidgetUnitTest {
        provideComposable { Track(template, WidthTier.W3, HeightTier.H2) }

        onNode(hasText("3,118 to go")).assertExists()
        onNode(hasText("Z2")).assertDoesNotExist()
    }

    @Test
    fun supportingRow_addsTheBadgeAtW4() = runGlanceAppWidgetUnitTest {
        provideComposable { Track(template, WidthTier.W4, HeightTier.H2) }

        onNode(hasText("Z2")).assertExists()
    }

    @Test
    fun hero_isHiddenBelowH3() = runGlanceAppWidgetUnitTest {
        provideComposable { Track(template, WidthTier.W4, HeightTier.H2) }

        onNode(hasText("7 Day Avg")).assertDoesNotExist()
    }

    @Test
    fun hero_dropsItsDescriptionAtH3() = runGlanceAppWidgetUnitTest {
        provideComposable { Track(template, WidthTier.W4, HeightTier.H3) }

        onNode(hasText("7 Day Avg")).assertDoesNotExist()
    }

    @Test
    fun hero_showsItsDescriptionAtH4() = runGlanceAppWidgetUnitTest {
        provideComposable { Track(template, WidthTier.W4, HeightTier.H4) }

        onNode(hasText("7 Day Avg")).assertExists()
        onNode(hasText("13,843 steps")).assertExists()
    }

    @Test
    fun hero_emitsEveryChartColumn() = runGlanceAppWidgetUnitTest {
        provideComposable { Track(template, WidthTier.W4, HeightTier.H4) }

        // The week is T F S S M T W. A Glance container holds at most ten children and drops the
        // rest silently, so any layout that inflates the child count loses the tail of the chart.
        onAllNodes(hasTextEqualTo("T")).assertCountEquals(2)
        onAllNodes(hasTextEqualTo("S")).assertCountEquals(2)
        onAllNodes(hasTextEqualTo("F")).assertCountEquals(1)
        onAllNodes(hasTextEqualTo("M")).assertCountEquals(1)
        onAllNodes(hasTextEqualTo("W")).assertCountEquals(1)
    }

    @Test
    fun narrowTier_stacksTheSameBlocksItShowsElsewhere() = runGlanceAppWidgetUnitTest {
        // W1 is the same component, not a different one: the title slot merely runs down instead
        // of across, and at H4 it keeps both the unit label and a supporting value.
        provideComposable { Track(template, WidthTier.W1, HeightTier.H4) }

        onNode(hasText("11,056")).assertExists()
        onNode(hasText("Steps")).assertExists()
        onNode(hasText("55%")).assertExists()
    }

    @Test
    fun narrowTier_hasNoHero() = runGlanceAppWidgetUnitTest {
        provideComposable { Track(template, WidthTier.W1, HeightTier.H4) }

        onAllNodes(hasTextEqualTo("W")).assertCountEquals(0)
    }

    @Test
    fun optionalTemplateFields_areNullGuarded() = runGlanceAppWidgetUnitTest {
        val bare = TrackTemplate(title = "0")

        provideComposable { Track(bare, WidthTier.W4, HeightTier.H4) }

        onNode(hasText("0")).assertExists()
    }

    /**
     * Composes [TrackLayout] at the given breakpoint.
     *
     * The unit-test environment provides no `LocalContext`, so one is supplied here — the progress
     * ring resolves theme colors and rasterizes its arc through it.
     */
    @Composable
    private fun Track(template: TrackTemplate, width: WidthTier, height: HeightTier) {
        CompositionLocalProvider(LocalContext provides context) {
            TrackLayout(template, nominalSlots(width, height))
        }
    }
}
