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

package androidx.glance.adaptive.appwidget.ui.templates.track

import androidx.glance.adaptive.appwidget.ui.AppWidgetTemplateRegistry
import androidx.glance.adaptive.appwidget.ui.selection.AppWidgetGlanceSurface
import androidx.glance.adaptive.appwidget.ui.selection.from
import androidx.glance.adaptive.core.ui.selection.Dimensions
import androidx.glance.adaptive.core.ui.selection.HeightTier
import androidx.glance.adaptive.core.ui.selection.HostConstraints
import androidx.glance.adaptive.core.ui.selection.SizeTiers
import androidx.glance.adaptive.core.ui.selection.WidthTier
import androidx.glance.adaptive.core.ui.templates.TrackTemplate
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers [TrackTemplateRenderer] as an implementation of the core rendering contract, and its
 * default wiring into [AppWidgetTemplateRegistry].
 *
 * The emitted hierarchy is covered by [TrackLayoutTest]; what matters here is that content is
 * produced without composing, and that the slot plan handed to it matches the container.
 */
@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class TrackTemplateRendererTest {

    private val template = TrackTemplate(title = "Morning Run", statusText = "5.2 km")

    @Before
    fun setUp() {
        AppWidgetTemplateRegistry.resetForTesting()
    }

    @Test
    fun render_returnsContentWithoutComposing() {
        // The contract is value-returning, so producing content needs no composer at all.
        val content = TrackTemplateRenderer.render(template, constraints(widthDp = 120))

        assertThat(content).isNotNull()
    }

    @Test
    fun render_resolvesSlotsFromContainer() {
        // 348x176 dp is the design's widest column and second tallest row.
        val tiers = SizeTiers.from(Dimensions(348, 176), AppWidgetGlanceSurface.MOBILE_HOME_SCREEN)

        assertThat(tiers).isEqualTo(SizeTiers(WidthTier.W4, HeightTier.H3))

        val slots = TrackSizeSelector.select(template, constraints(widthDp = 348, heightDp = 176))
        assertThat(slots).isEqualTo(TrackSlots.from(tiers, Dimensions(348, 176)))
        assertThat(slots.supportingCount).isEqualTo(3)
    }

    @Test
    fun defaultTemplates_includeTrackTemplate() {
        assertThat(AppWidgetTemplateRegistry.registryMap).containsKey(TrackTemplate::class.java)
        assertThat(AppWidgetTemplateRegistry.registryMap[TrackTemplate::class.java])
            .isSameInstanceAs(TrackTemplateRenderer)
    }

    private fun constraints(widthDp: Int, heightDp: Int = 200) =
        HostConstraints(Dimensions(widthDp, heightDp), AppWidgetGlanceSurface.MOBILE_HOME_SCREEN)
}
