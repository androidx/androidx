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

package androidx.glance.adaptive.appwidget

import android.content.Context
import android.widget.RemoteViews
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.runtime.Composable
import androidx.glance.adaptive.appwidget.renderer.RemoteComposeRenderOutput
import androidx.glance.adaptive.appwidget.renderer.RemoteViewsRenderOutput
import androidx.glance.adaptive.appwidget.renderer.RemoteViewsTemplateRenderer
import androidx.glance.adaptive.appwidget.renderer.remoteComposeRenderer
import androidx.glance.adaptive.appwidget.renderer.remoteViewsRenderer
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import androidx.glance.adaptive.core.renderer.TemplateRenderer
import androidx.glance.adaptive.core.selection.ArchetypeSelector
import androidx.glance.adaptive.core.selection.Dimensions
import androidx.glance.adaptive.core.slots.Ring
import androidx.glance.adaptive.core.slots.TemplateText
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate
import androidx.glance.adaptive.core.templates.TrackTemplate
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
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
    fun defaultTemplates_includeTrackTemplate() {
        val template =
            TrackTemplate(
                leading = Ring(0.55f),
                primary = TemplateText("Morning Run"),
                supporting = listOf(TemplateText("5.2 km")),
            )
        for (targetSurface in AppWidgetSurface.ALL) {
            val output =
                AppWidgetTemplateRegistry.render(
                    template,
                    targetSurface.withDimensions(Dimensions(200, 100)),
                )
            assertThat(output).isInstanceOf(RemoteViewsRenderOutput::class.java)
        }
    }

    @Test
    fun register_and_render_invokesRegisteredRenderer() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        var observedTemplate: DummyTemplate? = null
        var observedSurface: AppWidgetSurface? = null
        var rendered = false

        AppWidgetTemplateRegistry.registerDefault(
            DummyTemplate::class.java,
            TemplateRenderer(
                noArchetype,
                RemoteViewsTemplateRenderer { ctx, template, _, placedSurface ->
                    observedTemplate = template
                    observedSurface = placedSurface
                    rendered = true
                    RemoteViews(ctx.packageName, android.R.layout.simple_list_item_1)
                },
            ),
        )

        val dummy = DummyTemplate("Test")

        val output =
            AppWidgetTemplateRegistry.render(dummy, surface.withDimensions(Dimensions(200, 100)))
        assertThat(output).isInstanceOf(RemoteViewsRenderOutput::class.java)
        (output as RemoteViewsRenderOutput).provider(context)

        assertThat(observedTemplate).isSameInstanceAs(dummy)
        assertThat(observedSurface).isEqualTo(surface)
        assertThat(observedSurface?.dimensions).isEqualTo(Dimensions(200, 100))
        assertThat(rendered).isTrue()
    }

    @Test
    fun render_passesSelectedArchetypeToRenderer() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        var observedArchetype: DummyArchetype? = null
        AppWidgetTemplateRegistry.registerDefault(
            DummyTemplate::class.java,
            TemplateRenderer(
                ArchetypeSelector { _, placedSurface ->
                    if (placedSurface.dimensions.widthDp >= 250) DummyArchetype.ROW
                    else DummyArchetype.CARD
                },
                RemoteViewsTemplateRenderer { ctx, _, archetype, _ ->
                    observedArchetype = archetype
                    RemoteViews(ctx.packageName, android.R.layout.simple_list_item_1)
                },
            ),
        )

        val output =
            AppWidgetTemplateRegistry.render(
                DummyTemplate("Test"),
                surface.withDimensions(Dimensions(300, 100)),
            )
        (output as RemoteViewsRenderOutput).provider(context)

        assertThat(observedArchetype).isEqualTo(DummyArchetype.ROW)
    }

    @Test
    fun render_remoteViewsRenderer_returnsRemoteViewsOutput() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val expectedRemoteViews =
            RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
        AppWidgetTemplateRegistry.registerDefault(
            DummyTemplate::class.java,
            remoteViewsRenderer { _, _, _ -> expectedRemoteViews },
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
        AppWidgetTemplateRegistry.registerDefault(
            DummyTemplate::class.java,
            remoteComposeRenderer { _, _ -> expectedContent },
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
        val context = ApplicationProvider.getApplicationContext<Context>()
        var firstCalls = 0
        var secondCalls = 0

        AppWidgetTemplateRegistry.registerDefault(
            DummyTemplate::class.java,
            remoteViewsRenderer { ctx, _, _ ->
                firstCalls++
                RemoteViews(ctx.packageName, android.R.layout.simple_list_item_1)
            },
        )
        AppWidgetTemplateRegistry.registerDefault(
            DummyTemplate::class.java,
            remoteViewsRenderer { ctx, _, _ ->
                secondCalls++
                RemoteViews(ctx.packageName, android.R.layout.simple_list_item_1)
            },
        )

        val output =
            AppWidgetTemplateRegistry.render(
                DummyTemplate("Test"),
                surface.withDimensions(Dimensions(200, 100)),
            )
        (output as RemoteViewsRenderOutput).provider(context)

        assertThat(firstCalls).isEqualTo(0)
        assertThat(secondCalls).isEqualTo(1)
    }

    @Test(expected = IllegalStateException::class)
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
        AppWidgetTemplateRegistry.registerDefault(
            DummyTemplate::class.java,
            remoteComposeRenderer { _, _ -> {} },
        )
        AppWidgetTemplateRegistry.registerDefault(
            DummyTemplate::class.java,
            remoteViewsRenderer { _, _, _ -> lockScreenRemoteViews },
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
        AppWidgetTemplateRegistry.registerDefault(
            DummyTemplate::class.java,
            remoteComposeRenderer { _, _ -> {} },
        )
        AppWidgetTemplateRegistry.registerDefault(
            OtherTemplate::class.java,
            remoteViewsRenderer { ctx, _, _ ->
                RemoteViews(ctx.packageName, android.R.layout.simple_list_item_2)
            },
        )
        AppWidgetTemplateRegistry.register(
            widgetName = "custom_widget",
            templateClass = DummyTemplate::class.java,
            renderer = remoteViewsRenderer { _, _, _ -> customRemoteViews },
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
        assertThat(otherTemplateOnCustomWidgetOutput)
            .isInstanceOf(RemoteViewsRenderOutput::class.java)
    }

    @Test
    fun hasDroppedSlots_evaluatesBuiltInSelectorAndReturnsFalseForUnregisteredTemplates() {
        val track =
            TrackTemplate(
                leading = Ring(0.55f),
                primary = TemplateText("7,922"),
                secondary = TemplateText("steps"),
                supporting = listOf(TemplateText("Goal: 10,000")),
            )

        assertThat(
                AppWidgetTemplateRegistry.hasDroppedSlots(
                    track,
                    AppWidgetSurface.MOBILE_LOCK_SCREEN.withDimensions(Dimensions(168, 28)),
                )
            )
            .isTrue()
        assertThat(
                AppWidgetTemplateRegistry.hasDroppedSlots(
                    track,
                    AppWidgetSurface.MOBILE_HOME_SCREEN.withDimensions(Dimensions(348, 264)),
                )
            )
            .isFalse()
        assertThat(
                AppWidgetTemplateRegistry.hasDroppedSlots(
                    DummyTemplate("Unregistered"),
                    surface.withDimensions(Dimensions(88, 88)),
                )
            )
            .isFalse()
    }
}
