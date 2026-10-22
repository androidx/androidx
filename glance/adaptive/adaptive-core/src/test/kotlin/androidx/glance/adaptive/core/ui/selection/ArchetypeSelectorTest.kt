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

package androidx.glance.adaptive.core.ui.selection

import androidx.glance.adaptive.core.ui.templates.GlanceAdaptiveTemplate
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ArchetypeSelectorTest {

    private class TestTemplate(val hasDetails: Boolean) : GlanceAdaptiveTemplate(templateId = 1)

    private class TestSurface(tag: String, dimensions: Dimensions) : GlanceSurface(tag, dimensions)

    private enum class TestArchetype {
        COMPACT,
        FULL,
    }

    private fun placedSurface(widthDp: Int, heightDp: Int) =
        TestSurface("test_surface", Dimensions(widthDp, heightDp))

    @Test
    fun select_resolvesFromDimensionsAndData() {
        val selector =
            ArchetypeSelector<TestTemplate, TestSurface, TestArchetype> { template, surface ->
                if (template.hasDetails && surface.dimensions.widthDp >= 200) {
                    TestArchetype.FULL
                } else {
                    TestArchetype.COMPACT
                }
            }

        assertThat(selector.select(TestTemplate(hasDetails = true), placedSurface(200, 100)))
            .isEqualTo(TestArchetype.FULL)
        assertThat(selector.select(TestTemplate(hasDetails = true), placedSurface(120, 100)))
            .isEqualTo(TestArchetype.COMPACT)
        assertThat(selector.select(TestTemplate(hasDetails = false), placedSurface(200, 100)))
            .isEqualTo(TestArchetype.COMPACT)
    }

    @Test
    fun select_isUsableThroughWidenedArchetypeType() {
        val selector: ArchetypeSelector<TestTemplate, TestSurface, TestArchetype> =
            ArchetypeSelector { _, _ ->
                TestArchetype.FULL
            }

        // Compiles only because A is declared `out`.
        val widened: ArchetypeSelector<TestTemplate, TestSurface, Any> = selector

        assertThat(widened.select(TestTemplate(true), placedSurface(200, 100)))
            .isEqualTo(TestArchetype.FULL)
    }
}
