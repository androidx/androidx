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
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.glance.adaptive.appwidget.ui.selection.AppWidgetSurface
import androidx.glance.adaptive.core.ui.selection.ArchetypeSelector
import androidx.glance.adaptive.core.ui.selection.Dimensions
import androidx.glance.adaptive.core.ui.templates.GlanceAdaptiveTemplate
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

    private data class DummyTemplate(val title: String) : GlanceAdaptiveTemplate(templateId = 1)

    private data class OtherTemplate(val label: String) : GlanceAdaptiveTemplate(templateId = 2)

    private enum class DummyArchetype {
        CARD,
        ROW,
    }

    private val noArchetype =
        ArchetypeSelector<GlanceAdaptiveTemplate, AppWidgetSurface, Unit> { _, _ -> }

    private val surface = AppWidgetSurface.MOBILE_HOME_SCREEN

    @Before
    fun setUp() {
        AppWidgetTemplateRegistry.resetForTesting()
    }

    @Test
    fun register_and_render_invokesRegisteredRenderer() {
        var observedTemplate: DummyTemplate? = null
        var observedSurface: AppWidgetSurface? = null
        var rendered = false

        AppWidgetTemplateRegistry.register(
            DummyTemplate::class.java,
            noArchetype,
            GlanceTemplateRenderer { template, _, placedSurface ->
                observedTemplate = template
                observedSurface = placedSurface
                { rendered = true }
            },
        )

        val dummy = DummyTemplate("Test")

        val output =
            AppWidgetTemplateRegistry.render(dummy, surface.withDimensions(Dimensions(200, 100)))
        assertThat(output).isInstanceOf(GlanceRenderOutput::class.java)
        runComposition { (output as GlanceRenderOutput).content() }

        assertThat(observedTemplate).isSameInstanceAs(dummy)
        assertThat(observedSurface).isEqualTo(surface)
        assertThat(observedSurface?.dimensions).isEqualTo(Dimensions(200, 100))
        assertThat(rendered).isTrue()
    }

    @Test
    fun render_passesSelectedArchetypeToRenderer() {
        var observedArchetype: DummyArchetype? = null
        AppWidgetTemplateRegistry.register(
            DummyTemplate::class.java,
            ArchetypeSelector { _, placedSurface ->
                if (placedSurface.dimensions.widthDp >= 250) DummyArchetype.ROW
                else DummyArchetype.CARD
            },
            RemoteComposeTemplateRenderer { _, archetype, _ ->
                observedArchetype = archetype
                {}
            },
        )

        AppWidgetTemplateRegistry.render(
            DummyTemplate("Test"),
            surface.withDimensions(Dimensions(300, 100)),
        )

        assertThat(observedArchetype).isEqualTo(DummyArchetype.ROW)
    }

    @Test
    fun render_remoteViewsRenderer_returnsRemoteViewsOutput() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val expectedRemoteViews =
            RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
        AppWidgetTemplateRegistry.register(
            DummyTemplate::class.java,
            noArchetype,
            RemoteViewsTemplateRenderer { _, _, _, _ -> expectedRemoteViews },
        )

        val output =
            AppWidgetTemplateRegistry.render(
                DummyTemplate("Test"),
                surface.withDimensions(Dimensions(200, 100)),
            )

        assertThat(output).isInstanceOf(RemoteViewsRenderOutput::class.java)
        val rvOutput = output as RemoteViewsRenderOutput
        assertThat(rvOutput.provider(context)).isSameInstanceAs(expectedRemoteViews)
    }

    @Test
    fun render_remoteComposeRenderer_returnsRemoteComposeOutput() {
        val expectedContent: @RemoteComposable @Composable () -> Unit = {}
        AppWidgetTemplateRegistry.register(
            DummyTemplate::class.java,
            noArchetype,
            RemoteComposeTemplateRenderer { _, _, _ -> expectedContent },
        )

        val output =
            AppWidgetTemplateRegistry.render(
                DummyTemplate("Test"),
                surface.withDimensions(Dimensions(200, 100)),
            )

        assertThat(output).isInstanceOf(RemoteComposeRenderOutput::class.java)
        val contentOutput = output as RemoteComposeRenderOutput
        assertThat(contentOutput.content).isSameInstanceAs(expectedContent)
    }

    @Test
    fun register_replacesPreviousRenderer() {
        var firstCalls = 0
        var secondCalls = 0

        AppWidgetTemplateRegistry.register(
            DummyTemplate::class.java,
            noArchetype,
            GlanceTemplateRenderer { _, _, _ ->
                firstCalls++
                {}
            },
        )
        AppWidgetTemplateRegistry.register(
            DummyTemplate::class.java,
            noArchetype,
            GlanceTemplateRenderer { _, _, _ ->
                secondCalls++
                {}
            },
        )

        AppWidgetTemplateRegistry.render(
            DummyTemplate("Test"),
            surface.withDimensions(Dimensions(200, 100)),
        )

        assertThat(firstCalls).isEqualTo(0)
        assertThat(secondCalls).isEqualTo(1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun render_unregistered_throwsException() {
        AppWidgetTemplateRegistry.render(
            DummyTemplate("Unregistered"),
            surface.withDimensions(Dimensions(200, 100)),
        )
    }

    @Test
    fun register_defaultWithSurface_overwritesOnlyTargetSurface() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val lockScreenRemoteViews =
            RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
        AppWidgetTemplateRegistry.register(
            DummyTemplate::class.java,
            noArchetype,
            RemoteComposeTemplateRenderer { _, _, _ -> {} },
        )
        AppWidgetTemplateRegistry.register(
            DummyTemplate::class.java,
            noArchetype,
            RemoteViewsTemplateRenderer { _, _, _, _ -> lockScreenRemoteViews },
            surface = AppWidgetSurface.MOBILE_LOCK_SCREEN,
        )

        val homeOutput =
            AppWidgetTemplateRegistry.render(
                DummyTemplate("Test"),
                AppWidgetSurface.MOBILE_HOME_SCREEN.withDimensions(Dimensions(200, 100)),
            )
        val lockOutput =
            AppWidgetTemplateRegistry.render(
                DummyTemplate("Test"),
                AppWidgetSurface.MOBILE_LOCK_SCREEN.withDimensions(Dimensions(200, 100)),
            )

        assertThat(homeOutput).isInstanceOf(RemoteComposeRenderOutput::class.java)
        assertThat(lockOutput).isInstanceOf(RemoteViewsRenderOutput::class.java)
    }

    @Test
    fun register_byWidgetName_overridesOnlyTargetWidgetTemplateAndSurface() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val customRemoteViews =
            RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
        AppWidgetTemplateRegistry.register(
            DummyTemplate::class.java,
            noArchetype,
            RemoteComposeTemplateRenderer { _, _, _ -> {} },
        )
        AppWidgetTemplateRegistry.register(
            OtherTemplate::class.java,
            noArchetype,
            GlanceTemplateRenderer { _, _, _ -> {} },
        )
        AppWidgetTemplateRegistry.register(
            widgetName = "custom_widget",
            templateClass = DummyTemplate::class.java,
            selector = noArchetype,
            renderer = RemoteViewsTemplateRenderer { _, _, _, _ -> customRemoteViews },
            surface = AppWidgetSurface.MOBILE_LOCK_SCREEN,
        )

        val customLockOutput =
            AppWidgetTemplateRegistry.render(
                DummyTemplate("Test"),
                AppWidgetSurface.MOBILE_LOCK_SCREEN.withDimensions(Dimensions(200, 100)),
                widgetName = "custom_widget",
            )
        val customHomeOutput =
            AppWidgetTemplateRegistry.render(
                DummyTemplate("Test"),
                AppWidgetSurface.MOBILE_HOME_SCREEN.withDimensions(Dimensions(200, 100)),
                widgetName = "custom_widget",
            )
        val defaultWidgetLockOutput =
            AppWidgetTemplateRegistry.render(
                DummyTemplate("Test"),
                AppWidgetSurface.MOBILE_LOCK_SCREEN.withDimensions(Dimensions(200, 100)),
                widgetName = "other_widget",
            )
        val otherTemplateOnCustomWidgetOutput =
            AppWidgetTemplateRegistry.render(
                OtherTemplate("Other"),
                AppWidgetSurface.MOBILE_LOCK_SCREEN.withDimensions(Dimensions(200, 100)),
                widgetName = "custom_widget",
            )

        assertThat(customLockOutput).isInstanceOf(RemoteViewsRenderOutput::class.java)
        assertThat(customHomeOutput).isInstanceOf(RemoteComposeRenderOutput::class.java)
        assertThat(defaultWidgetLockOutput).isInstanceOf(RemoteComposeRenderOutput::class.java)
        assertThat(otherTemplateOnCustomWidgetOutput).isInstanceOf(GlanceRenderOutput::class.java)
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
