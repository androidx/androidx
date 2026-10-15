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

package androidx.glance.adaptive.appwidget.ui

import android.content.Context
import android.widget.RemoteViews
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.glance.adaptive.appwidget.ui.selection.AppWidgetGlanceSurface
import androidx.glance.adaptive.core.ui.selection.ArchetypeSelector
import androidx.glance.adaptive.core.ui.selection.Dimensions
import androidx.glance.adaptive.core.ui.selection.HostConstraints
import androidx.glance.adaptive.core.ui.templates.AdaptiveGlanceTemplate
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class AppWidgetTemplateRegistryTest {

    private data class DummyTemplate(val title: String) : AdaptiveGlanceTemplate {
        override val templateId: String = "DummyTemplate"
    }

    private enum class DummyArchetype {
        CARD
    }

    private val surface = AppWidgetGlanceSurface.MOBILE_HOME_SCREEN

    @Before
    fun setUp() {
        AppWidgetTemplateRegistry.resetForTesting()
    }

    @Test
    fun register_and_render_invokesRegisteredRenderer() {
        var observedTemplate: DummyTemplate? = null
        var observedConstraints: HostConstraints<AppWidgetGlanceSurface>? = null
        var rendered = false

        AppWidgetTemplateRegistry.register(
            DummyTemplate::class.java,
            GlanceTemplateRenderer { template, constraints ->
                observedTemplate = template
                observedConstraints = constraints
                { rendered = true }
            },
        )

        val dummy = DummyTemplate("Test")

        val output =
            AppWidgetTemplateRegistry.render(dummy, HostConstraints(Dimensions(200, 100), surface))
        assertThat(output).isInstanceOf(GlanceRenderOutput::class.java)
        runComposition { (output as GlanceRenderOutput).content() }

        assertThat(observedTemplate).isSameInstanceAs(dummy)
        assertThat(observedConstraints).isEqualTo(HostConstraints(Dimensions(200, 100), surface))
        assertThat(rendered).isTrue()
    }

    @Test
    fun render_remoteViewsRenderer_returnsRemoteViewsOutput() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val expectedRemoteViews =
            RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
        AppWidgetTemplateRegistry.register(
            DummyTemplate::class.java,
            RemoteViewsTemplateRenderer { _, _, _ -> expectedRemoteViews },
        )

        val output =
            AppWidgetTemplateRegistry.render(
                DummyTemplate("Test"),
                HostConstraints(Dimensions(200, 100), surface),
            )

        assertThat(output).isInstanceOf(RemoteViewsRenderOutput::class.java)
        val rvOutput = output as RemoteViewsRenderOutput
        assertThat(rvOutput.provider(context)).isSameInstanceAs(expectedRemoteViews)
    }

    @Test
    fun register_replacesPreviousRenderer() {
        var firstCalls = 0
        var secondCalls = 0

        AppWidgetTemplateRegistry.register(
            DummyTemplate::class.java,
            GlanceTemplateRenderer { _, _ ->
                firstCalls++
                {}
            },
        )
        AppWidgetTemplateRegistry.register(
            DummyTemplate::class.java,
            GlanceTemplateRenderer { _, _ ->
                secondCalls++
                {}
            },
        )

        AppWidgetTemplateRegistry.render(
            DummyTemplate("Test"),
            HostConstraints(Dimensions(200, 100), surface),
        )

        assertThat(firstCalls).isEqualTo(0)
        assertThat(secondCalls).isEqualTo(1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun render_unregistered_throwsException() {
        AppWidgetTemplateRegistry.render(
            DummyTemplate("Unregistered"),
            HostConstraints(Dimensions(200, 100), surface),
        )
    }

    @Test
    fun resolveArchetypeId_enumArchetype_returnsEnumName() {
        AppWidgetTemplateRegistry.register(
            DummyTemplate::class.java,
            GlanceTemplateRenderer { _, _ -> {} },
            ArchetypeSelector { _, _ -> DummyArchetype.CARD },
        )

        val archetypeId =
            AppWidgetTemplateRegistry.resolveArchetypeId(
                data = DummyTemplate("Test"),
                surface = surface,
                dimensions = Dimensions(200, 100),
            )

        assertThat(archetypeId).isEqualTo("CARD")
    }

    @Test
    fun resolveArchetypeId_nonEnumArchetype_returnsToString() {
        class CustomArchetype(private val id: String) {
            override fun toString(): String = id
        }

        AppWidgetTemplateRegistry.register(
            DummyTemplate::class.java,
            GlanceTemplateRenderer { _, _ -> {} },
            ArchetypeSelector { _, _ -> CustomArchetype("CUSTOM_SPLIT") },
        )

        val archetypeId =
            AppWidgetTemplateRegistry.resolveArchetypeId(
                data = DummyTemplate("Test"),
                surface = surface,
                dimensions = Dimensions(200, 100),
            )

        assertThat(archetypeId).isEqualTo("CUSTOM_SPLIT")
    }

    @Test
    fun resolveArchetypeId_unregisteredTemplate_returnsNull() {
        val archetypeId =
            AppWidgetTemplateRegistry.resolveArchetypeId(
                data = DummyTemplate("Unregistered"),
                surface = surface,
                dimensions = Dimensions(200, 100),
            )

        assertThat(archetypeId).isNull()
    }

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
