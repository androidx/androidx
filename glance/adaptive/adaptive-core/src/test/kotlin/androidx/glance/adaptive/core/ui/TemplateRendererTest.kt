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

package androidx.glance.adaptive.core.ui

import androidx.glance.adaptive.core.ui.selection.Dimensions
import androidx.glance.adaptive.core.ui.selection.GlanceSurface
import androidx.glance.adaptive.core.ui.templates.GlanceAdaptiveTemplate
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TemplateRendererTest {

    private class TestTemplate(val title: String) : GlanceAdaptiveTemplate(templateId = 1)

    private class TestSurface(tag: String, dimensions: Dimensions) : GlanceSurface(tag, dimensions)

    private enum class TestArchetype {
        COMPACT
    }

    private val testSurface = TestSurface("test_surface", Dimensions(200, 100))

    @Test
    fun render_receivesTemplateArchetypeAndSurface() {
        var observedTemplate: TestTemplate? = null
        var observedArchetype: TestArchetype? = null
        var observedSurface: TestSurface? = null

        val renderer =
            TemplateRenderer<TestTemplate, TestSurface, TestArchetype, Unit> {
                template,
                archetype,
                surface ->
                observedTemplate = template
                observedArchetype = archetype
                observedSurface = surface
            }

        val template = TestTemplate("Morning Run")
        renderer.render(template, TestArchetype.COMPACT, testSurface)

        assertThat(observedTemplate).isSameInstanceAs(template)
        assertThat(observedArchetype).isEqualTo(TestArchetype.COMPACT)
        assertThat(observedSurface).isSameInstanceAs(testSurface)
    }

    /**
     * The point of the [TemplateRenderer] output parameter: a host substitutes its own content type
     * without core ever naming it. Here a test substitutes [String]; the Glance AppWidget host
     * substitutes `@Composable () -> Unit`, a plain host would substitute `RemoteViews`.
     */
    @Test
    fun render_hostChoosesOutputType() {
        val textRenderer =
            TemplateRenderer<TestTemplate, TestSurface, TestArchetype, String> {
                template,
                archetype,
                surface ->
                "${template.title}@${surface.dimensions.widthDp}/${archetype.name}"
            }

        assertThat(
                textRenderer.render(TestTemplate("Morning Run"), TestArchetype.COMPACT, testSurface)
            )
            .isEqualTo("Morning Run@200/COMPACT")
    }

    @Test
    fun render_outputTypeIsCovariant() {
        val renderer: TemplateRenderer<TestTemplate, TestSurface, TestArchetype, String> =
            TemplateRenderer { template, _, _ ->
                template.title
            }

        // Compiles only because R is declared `out`.
        val widened: TemplateRenderer<TestTemplate, TestSurface, TestArchetype, CharSequence> =
            renderer

        assertThat(widened.render(TestTemplate("Morning Run"), TestArchetype.COMPACT, testSurface))
            .isEqualTo("Morning Run")
    }

    @Test
    fun render_templateSurfaceAndArchetypeAreContravariant() {
        val renderer: TemplateRenderer<GlanceAdaptiveTemplate, GlanceSurface, Any, String> =
            TemplateRenderer { _, _, surface ->
                surface.tag
            }

        // Compiles only because T, S and A are declared `in`.
        val narrowed: TemplateRenderer<TestTemplate, TestSurface, TestArchetype, String> = renderer

        assertThat(narrowed.render(TestTemplate("Morning Run"), TestArchetype.COMPACT, testSurface))
            .isEqualTo("test_surface")
    }
}
