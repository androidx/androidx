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

package androidx.glance.adaptive.core.renderer

import androidx.glance.adaptive.core.selection.ArchetypeSelector
import androidx.glance.adaptive.core.selection.Dimensions
import androidx.glance.adaptive.core.selection.GlanceSurface
import androidx.glance.adaptive.core.slots.Ring
import androidx.glance.adaptive.core.slots.TemplateText
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate
import androidx.glance.adaptive.core.templates.TrackTemplate
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class TemplateRegistryTest {

    private class TestTemplate(val title: String) : GlanceAdaptiveTemplate(templateId = 1)

    private class OtherTemplate : GlanceAdaptiveTemplate(templateId = 2)

    private class TestSurface(tag: String, dimensions: Dimensions = Dimensions(0, 0)) :
        GlanceSurface(tag, dimensions)

    private val home = TestSurface("home")
    private val lock = TestSurface("lock")

    private val defaultRenderers =
        object : TemplateRegistry.DefaultRenderers<TestSurface, String> {
            override fun trackRenderer(
                surface: TestSurface
            ): TemplateRenderer<TrackTemplate, TestSurface, String> =
                TemplateRenderer { template, _ ->
                    "builtIn(${surface.tag}):${(template.primary as TemplateText).value}"
                }
        }

    private val registry = TemplateRegistry(listOf(home, lock), defaultRenderers)

    private val widthSelector =
        ArchetypeSelector<TestTemplate, TestSurface, String> { _, surface ->
            if (surface.dimensions.widthDp >= 200) "wide" else "narrow"
        }

    private fun renderer(label: String) =
        TemplateRenderer(
            widthSelector,
            ArchetypeRenderer<TestTemplate, TestSurface, String, String> {
                template,
                archetype,
                surface ->
                "$label:${template.title}:$archetype:${surface.tag}"
            },
        )

    @Test
    fun render_usesTheRegisteredRenderer() {
        registry.registerDefault(TestTemplate::class.java, renderer("default"))

        assertThat(registry.render(TestTemplate("Run"), TestSurface("home", Dimensions(300, 100))))
            .isEqualTo("default:Run:wide:home")
        assertThat(registry.render(TestTemplate("Run"), TestSurface("lock", Dimensions(120, 50))))
            .isEqualTo("default:Run:narrow:lock")
    }

    @Test
    fun registerDefault_withSurface_replacesOnlyThatSurface() {
        registry.registerDefault(TestTemplate::class.java, renderer("all"))
        registry.registerDefault(TestTemplate::class.java, renderer("lock"), lock)

        assertThat(registry.render(TestTemplate("Run"), home)).startsWith("all:")
        assertThat(registry.render(TestTemplate("Run"), lock)).startsWith("lock:")
    }

    @Test
    fun registerDefault_lastRegistrationWins() {
        registry.registerDefault(TestTemplate::class.java, renderer("first"))
        registry.registerDefault(TestTemplate::class.java, renderer("second"))

        assertThat(registry.render(TestTemplate("Run"), home)).startsWith("second:")
    }

    @Test
    fun registerOverride_takesPrecedenceOnlyForItsWidgetAndSurface() {
        registry.registerDefault(TestTemplate::class.java, renderer("default"))
        registry.registerOverride(
            "custom",
            TestTemplate::class.java,
            renderer("custom"),
            lock,
        )

        assertThat(registry.render(TestTemplate("Run"), lock, widgetName = "custom"))
            .startsWith("custom:")
        assertThat(registry.render(TestTemplate("Run"), home, widgetName = "custom"))
            .startsWith("default:")
        assertThat(registry.render(TestTemplate("Run"), lock, widgetName = "other"))
            .startsWith("default:")
        assertThat(registry.render(TestTemplate("Run"), lock)).startsWith("default:")
    }

    @Test
    fun render_placedSurface_findsEntriesOfItsSurfaceConstant() {
        registry.registerDefault(TestTemplate::class.java, renderer("lock"), lock)

        assertThat(registry.render(TestTemplate("Run"), TestSurface("lock", Dimensions(300, 100))))
            .isEqualTo("lock:Run:wide:lock")
    }

    @Test
    fun render_unregisteredTemplate_throwsIllegalStateException() {
        assertThrows(IllegalStateException::class.java) { registry.render(OtherTemplate(), home) }
    }

    @Test
    fun resetForTesting_clearsTestEntriesAndRestoresBuiltInDefaults() {
        registry.registerDefault(TestTemplate::class.java, renderer("default"))
        registry.registerOverride(
            "custom",
            TrackTemplate::class.java,
            TemplateRenderer { template, _ ->
                "override:${(template.primary as TemplateText).value}"
            },
        )

        registry.resetForTesting()

        assertThrows(IllegalStateException::class.java) {
            registry.render(TestTemplate("Run"), home)
        }
        val stepsTemplate = TrackTemplate(leading = Ring(0.5f), primary = TemplateText("Steps"))
        assertThat(registry.render(stepsTemplate, home, widgetName = "custom"))
            .isEqualTo("builtIn(home):Steps")
    }

    @Test
    fun constructor_copiesSurfacesDefensively() {
        val mutableSurfaces = mutableListOf(home)
        val isolated = TemplateRegistry(mutableSurfaces, defaultRenderers)
        mutableSurfaces.clear()

        isolated.registerDefault(TestTemplate::class.java, renderer("default"))

        assertThat(isolated.render(TestTemplate("Run"), home)).startsWith("default:")
    }
}
