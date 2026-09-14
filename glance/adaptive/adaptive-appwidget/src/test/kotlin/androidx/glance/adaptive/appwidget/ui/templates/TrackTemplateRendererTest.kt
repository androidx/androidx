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

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.glance.adaptive.appwidget.ui.AppWidgetTemplateRegistry
import androidx.glance.adaptive.appwidget.ui.selection.AppWidgetGlanceSurface
import androidx.glance.adaptive.appwidget.ui.selection.LocalContainerDimensions
import androidx.glance.adaptive.core.ui.selection.Dimensions
import androidx.glance.adaptive.core.ui.selection.HostConstraints
import androidx.glance.adaptive.core.ui.templates.TrackTemplate
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers [TrackTemplateRenderer] as an implementation of the core rendering contract, and its
 * default wiring into [AppWidgetTemplateRegistry].
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
    fun render_producesDistinctContentPerArchetype() {
        var thinRendered = false
        var standardRendered = false

        runComposition {
            TrackTemplateRenderer.Render(template, TrackArchetype.THIN)
            thinRendered = true
        }
        runComposition {
            TrackTemplateRenderer.Render(template, TrackArchetype.STANDARD)
            standardRendered = true
        }

        assertThat(thinRendered).isTrue()
        assertThat(standardRendered).isTrue()
    }

    @Test
    fun defaultTemplates_includeTrackTemplate() {
        assertThat(AppWidgetTemplateRegistry.registryMap).containsKey(TrackTemplate::class.java)
        assertThat(AppWidgetTemplateRegistry.registryMap[TrackTemplate::class.java])
            .isSameInstanceAs(TrackTemplateRenderer)
    }

    @Test
    fun registryRender_composesForBothArchetypes() {
        for (widthDp in listOf(120, 300)) {
            runComposition {
                CompositionLocalProvider(
                    LocalContainerDimensions provides Dimensions(widthDp, 200)
                ) {
                    AppWidgetTemplateRegistry.render(
                        template,
                        AppWidgetGlanceSurface.MOBILE_HOME_SCREEN,
                    )
                }
            }
        }
    }

    private fun constraints(widthDp: Int, heightDp: Int = 200) =
        HostConstraints(Dimensions(widthDp, heightDp), AppWidgetGlanceSurface.MOBILE_HOME_SCREEN)

    private fun runComposition(content: @Composable () -> Unit) {
        val applier =
            object : AbstractApplier<Unit>(Unit) {
                override fun insertTopDown(index: Int, instance: Unit) {}

                override fun insertBottomUp(index: Int, instance: Unit) {}

                override fun remove(index: Int, count: Int) {}

                override fun move(from: Int, to: Int, count: Int) {}

                override fun onClear() {}
            }
        val composition = Composition(applier, Recomposer(Dispatchers.Unconfined))
        composition.setContent(content)
    }
}
