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

package androidx.glance.adaptive.appwidget.renderer

import android.content.Context
import android.widget.RemoteViews
import androidx.glance.adaptive.appwidget.AppWidgetTemplateRegistry
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import androidx.glance.adaptive.core.renderer.TemplateRenderer
import androidx.glance.adaptive.core.selection.ArchetypeSelector
import androidx.glance.adaptive.core.selection.Dimensions
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class ApiLevelTemplateRendererTest {

    private class TestTemplate : GlanceAdaptiveTemplate(templateId = 1)

    private enum class TestArchetype {
        CARD,
        BULB,
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val surface = AppWidgetSurface.MOBILE_HOME_SCREEN.withDimensions(Dimensions(200, 100))

    private val remoteViews = RemoteViews(context.packageName, android.R.layout.simple_list_item_1)

    private var remoteComposeCalls = 0
    private var remoteViewsCalls = 0
    private val observedTemplates = mutableListOf<TestTemplate>()
    private val observedArchetypes = mutableListOf<TestArchetype>()
    private val observedSurfaces = mutableListOf<AppWidgetSurface>()

    private val renderer =
        ApiLevelTemplateRenderer<TestTemplate, TestArchetype>(
            remoteCompose =
                RemoteComposeTemplateRenderer { template, archetype, placedSurface ->
                    remoteComposeCalls++
                    observedTemplates += template
                    observedArchetypes += archetype
                    observedSurfaces += placedSurface
                    {}
                },
            remoteViews =
                RemoteViewsTemplateRenderer { _, template, archetype, placedSurface ->
                    remoteViewsCalls++
                    observedTemplates += template
                    observedArchetypes += archetype
                    observedSurfaces += placedSurface
                    remoteViews
                },
        )

    @After
    fun tearDown() {
        AppWidgetTemplateRegistry.resetForTesting()
    }

    @Test
    @Config(sdk = [37])
    fun render_api37_usesRemoteComposeRenderer() {
        val output = renderer.render(TestTemplate(), TestArchetype.CARD, surface)

        assertThat(output).isInstanceOf(RemoteComposeRenderOutput::class.java)
        assertThat(remoteComposeCalls).isEqualTo(1)
        assertThat(remoteViewsCalls).isEqualTo(0)
    }

    @Test
    @Config(sdk = [36])
    fun render_belowApi37_usesRemoteViewsRenderer() {
        val output = renderer.render(TestTemplate(), TestArchetype.CARD, surface)

        assertThat(output).isInstanceOf(RemoteViewsRenderOutput::class.java)
        assertThat((output as RemoteViewsRenderOutput).provider(context))
            .isSameInstanceAs(remoteViews)
        assertThat(remoteComposeCalls).isEqualTo(0)
        assertThat(remoteViewsCalls).isEqualTo(1)
    }

    @Test
    @Config(sdk = [36, 37])
    fun render_forwardsTemplateArchetypeAndSurface() {
        val template = TestTemplate()
        val output = renderer.render(template, TestArchetype.CARD, surface)
        (output as? RemoteViewsRenderOutput)?.provider?.invoke(context)

        assertThat(observedTemplates).containsExactly(template)
        assertThat(observedArchetypes).containsExactly(TestArchetype.CARD)
        assertThat(observedSurfaces).containsExactly(AppWidgetSurface.MOBILE_HOME_SCREEN)
        assertThat(observedSurfaces.single().dimensions).isEqualTo(Dimensions(200, 100))
    }

    @Test
    @Config(sdk = [36, 37])
    fun templateRenderer_runsSelectorOnceAndDrawsSameArchetypeOnEveryApiLevel() {
        var selections = 0
        val selector =
            ArchetypeSelector<TestTemplate, AppWidgetSurface, TestArchetype> { _, placedSurface ->
                selections++
                if (placedSurface.dimensions.widthDp >= 200) TestArchetype.CARD
                else TestArchetype.BULB
            }
        val builtInRenderer = TemplateRenderer(selector, renderer)

        val output = builtInRenderer.render(TestTemplate(), surface)
        (output as? RemoteViewsRenderOutput)?.provider?.invoke(context)

        assertThat(selections).isEqualTo(1)
        assertThat(observedArchetypes).containsExactly(TestArchetype.CARD)
    }

    @Test
    @Config(sdk = [36, 37])
    fun developerOverride_replacesBuiltInApiLevelRendererOnHomeScreen() {
        val selector =
            ArchetypeSelector<TestTemplate, AppWidgetSurface, TestArchetype> { _, _ ->
                TestArchetype.CARD
            }
        AppWidgetTemplateRegistry.registerDefault(
            TestTemplate::class.java,
            TemplateRenderer(selector, renderer),
            AppWidgetSurface.MOBILE_HOME_SCREEN,
        )
        val overrideViews = RemoteViews(context.packageName, android.R.layout.simple_list_item_2)
        AppWidgetTemplateRegistry.register(
            widgetName = "custom_widget",
            templateClass = TestTemplate::class.java,
            renderer = { _, _ -> RemoteViewsRenderOutput(overrideViews) },
            surface = AppWidgetSurface.MOBILE_HOME_SCREEN,
        )

        val output =
            AppWidgetTemplateRegistry.render(
                data = TestTemplate(),
                surface = surface,
                widgetName = "custom_widget",
            )

        assertThat(output).isInstanceOf(RemoteViewsRenderOutput::class.java)
        assertThat((output as RemoteViewsRenderOutput).provider(context))
            .isSameInstanceAs(overrideViews)
        assertThat(remoteComposeCalls).isEqualTo(0)
        assertThat(remoteViewsCalls).isEqualTo(0)
    }
}
