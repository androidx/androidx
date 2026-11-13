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
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class TemplateRegistryTest {

    private class TestTemplate(val title: String) : GlanceAdaptiveTemplate(templateId = 1)

    private class BuiltInTemplate(val value: String) : GlanceAdaptiveTemplate(templateId = 2)

    private class SecondBuiltInTemplate(val label: String) : GlanceAdaptiveTemplate(templateId = 3)

    private class OtherTemplate : GlanceAdaptiveTemplate(templateId = 4)

    private abstract class AbstractTestTemplate : GlanceAdaptiveTemplate(templateId = 5)

    private class TestSurface(tag: String, dimensions: Dimensions = Dimensions(0, 0)) :
        GlanceSurface(tag, dimensions)

    private val home = TestSurface("home")
    private val lock = TestSurface("lock")

    private val defaultRenderers =
        listOf(
            defaultRenderer<BuiltInTemplate, TestSurface, String> { template, placedSurface ->
                "builtIn(${placedSurface.tag}):${template.value}"
            },
            defaultRenderer<SecondBuiltInTemplate, TestSurface, String> { template, placedSurface ->
                "secondBuiltIn(${placedSurface.tag}):${template.label}"
            },
        )

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
    fun builtInDefaults_coverEveryRegisteredDefaultOnEverySurface() {
        val first = BuiltInTemplate("Steps")
        val second = SecondBuiltInTemplate("Run")

        for (surface in listOf(home, lock)) {
            assertThat(registry.render(first, surface)).isEqualTo("builtIn(${surface.tag}):Steps")
            assertThat(registry.render(second, surface))
                .isEqualTo("secondBuiltIn(${surface.tag}):Run")
        }
    }

    @Test
    fun resetForTesting_clearsTestEntriesAndRestoresBuiltInDefaults() {
        registry.registerDefault(TestTemplate::class.java, renderer("default"))
        registry.registerDefault(
            BuiltInTemplate::class.java,
            TemplateRenderer { template, _ -> "overwrittenDefault:${template.value}" },
        )
        registry.registerOverride(
            "custom",
            BuiltInTemplate::class.java,
            TemplateRenderer { template, _ -> "override:${template.value}" },
        )

        registry.resetForTesting()

        assertThrows(IllegalStateException::class.java) {
            registry.render(TestTemplate("Run"), home)
        }
        assertThat(registry.render(BuiltInTemplate("Steps"), home)).isEqualTo("builtIn(home):Steps")
        assertThat(registry.render(BuiltInTemplate("Steps"), home, widgetName = "custom"))
            .isEqualTo("builtIn(home):Steps")
    }

    @Test
    fun constructor_copiesSurfacesAndDefaultRenderersDefensively() {
        val mutableSurfaces = mutableListOf(home)
        val mutableDefaults = defaultRenderers.toMutableList()
        val isolated = TemplateRegistry(mutableSurfaces, mutableDefaults)
        mutableSurfaces.clear()
        mutableDefaults.clear()

        isolated.registerDefault(TestTemplate::class.java, renderer("default"))
        assertThat(isolated.render(TestTemplate("Run"), home)).startsWith("default:")

        isolated.resetForTesting()
        assertThat(isolated.render(BuiltInTemplate("Steps"), home)).isEqualTo("builtIn(home):Steps")
    }

    @Test
    fun constructor_duplicateDefaultRenderers_throwsIllegalArgumentException() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                TemplateRegistry(
                    listOf(home, lock),
                    listOf(
                        defaultRenderer<BuiltInTemplate, TestSurface, String> { _, _ -> "first" },
                        defaultRenderer<BuiltInTemplate, TestSurface, String> { _, _ -> "second" },
                    ),
                )
            }

        assertThat(error).hasMessageThat().contains(BuiltInTemplate::class.java.name)
    }

    @Test
    fun defaultRenderer_abstractTemplateClass_throwsIllegalArgumentException() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                defaultRenderer<AbstractTestTemplate, TestSurface, String> { _, _ -> "abstract" }
            }

        assertThat(error).hasMessageThat().contains(AbstractTestTemplate::class.java.name)
    }

    @Test
    fun registerDefault_abstractTemplateClass_throwsIllegalArgumentException() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                registry.registerDefault(
                    AbstractTestTemplate::class.java,
                    TemplateRenderer { _, _ -> "abstract" },
                )
            }

        assertThat(error).hasMessageThat().contains(AbstractTestTemplate::class.java.name)
    }

    @Test
    fun registerOverride_abstractTemplateClass_throwsIllegalArgumentException() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                registry.registerOverride(
                    "custom",
                    AbstractTestTemplate::class.java,
                    TemplateRenderer { _, _ -> "abstract" },
                )
            }

        assertThat(error).hasMessageThat().contains(AbstractTestTemplate::class.java.name)
    }
}
