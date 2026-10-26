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

import androidx.glance.adaptive.core.ui.selection.ArchetypeSelector
import androidx.glance.adaptive.core.ui.selection.Dimensions
import androidx.glance.adaptive.core.ui.selection.GlanceSurface
import androidx.glance.adaptive.core.ui.templates.GlanceAdaptiveTemplate
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ArchetypeRendererTest {

    private class TestTemplate(val title: String) : GlanceAdaptiveTemplate(templateId = 1)

    private class TestSurface(tag: String, dimensions: Dimensions) : GlanceSurface(tag, dimensions)

    private enum class TestArchetype {
        COMPACT,
        WIDE,
    }

    private val selector =
        ArchetypeSelector<TestTemplate, TestSurface, TestArchetype> { _, surface ->
            if (surface.dimensions.widthDp >= 200) TestArchetype.WIDE else TestArchetype.COMPACT
        }

    private val archetypeRenderer =
        ArchetypeRenderer<TestTemplate, TestSurface, TestArchetype, String> {
            template,
            archetype,
            surface ->
            "${template.title}@${surface.dimensions.widthDp}/${archetype.name}"
        }

    @Test
    fun templateRenderer_drawsTheArchetypeTheSelectorPicks() {
        val renderer = TemplateRenderer(selector, archetypeRenderer)

        assertThat(
                renderer.render(
                    TestTemplate("Morning Run"),
                    TestSurface("test_surface", Dimensions(200, 100)),
                )
            )
            .isEqualTo("Morning Run@200/WIDE")
        assertThat(
                renderer.render(
                    TestTemplate("Morning Run"),
                    TestSurface("test_surface", Dimensions(120, 100)),
                )
            )
            .isEqualTo("Morning Run@120/COMPACT")
    }

    @Test
    fun templateRenderer_runsTheSelectorOncePerRender() {
        var selections = 0
        val countingSelector =
            ArchetypeSelector<TestTemplate, TestSurface, TestArchetype> { template, surface ->
                selections++
                selector.select(template, surface)
            }
        val renderer = TemplateRenderer(countingSelector, archetypeRenderer)

        renderer.render(
            TestTemplate("Morning Run"),
            TestSurface("test_surface", Dimensions(200, 100)),
        )

        assertThat(selections).isEqualTo(1)
    }

    @Test
    fun templateRenderer_handsTheSelectedArchetypeToTheRenderer() {
        var selectedArchetype: TestArchetype? = null
        val recordingSelector =
            ArchetypeSelector<TestTemplate, TestSurface, TestArchetype> { template, surface ->
                selector.select(template, surface).also { selectedArchetype = it }
            }
        var observedTemplate: TestTemplate? = null
        var observedArchetype: TestArchetype? = null
        var observedSurface: TestSurface? = null
        val recordingRenderer =
            ArchetypeRenderer<TestTemplate, TestSurface, TestArchetype, Unit> {
                template,
                archetype,
                surface ->
                observedTemplate = template
                observedArchetype = archetype
                observedSurface = surface
            }
        val template = TestTemplate("Morning Run")
        val surface = TestSurface("test_surface", Dimensions(120, 100))

        TemplateRenderer(recordingSelector, recordingRenderer).render(template, surface)

        assertThat(observedArchetype).isNotNull()
        assertThat(observedArchetype).isSameInstanceAs(selectedArchetype)
        assertThat(observedTemplate).isSameInstanceAs(template)
        assertThat(observedSurface).isSameInstanceAs(surface)
    }
}
