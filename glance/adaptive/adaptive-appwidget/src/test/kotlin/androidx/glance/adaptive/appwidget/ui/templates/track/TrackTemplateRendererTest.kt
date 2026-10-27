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
import androidx.glance.adaptive.appwidget.ui.GlanceRenderOutput
import androidx.glance.adaptive.appwidget.ui.selection.AppWidgetSurface
import androidx.glance.adaptive.core.ui.selection.Dimensions
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
 * The emitted hierarchy is covered by [TrackLayoutTest] and slot resolution by
 * [TrackSizeSelectorTest]; what matters here is that content is produced without composing.
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
    fun render_returnsGlanceOutputWithoutComposing() {
        // The contract is value-returning, so producing content needs no composer at all.
        val surface = homeScreen(widthDp = 120)
        val output =
            TrackTemplateRenderer.render(
                template,
                TrackSizeSelector.select(template, surface),
                surface,
            )

        assertThat(output).isInstanceOf(GlanceRenderOutput::class.java)
    }

    @Test
    fun defaultTemplates_includeTrackTemplate() {
        val surfaces = AppWidgetSurface.ALL
        for (i in surfaces.indices) {
            val output =
                AppWidgetTemplateRegistry.render(
                    template,
                    surfaces[i].withDimensions(Dimensions(120, 200)),
                )

            assertThat(output).isInstanceOf(GlanceRenderOutput::class.java)
        }
    }

    private fun homeScreen(widthDp: Int, heightDp: Int = 200) =
        AppWidgetSurface.MOBILE_HOME_SCREEN.withDimensions(Dimensions(widthDp, heightDp))
}
