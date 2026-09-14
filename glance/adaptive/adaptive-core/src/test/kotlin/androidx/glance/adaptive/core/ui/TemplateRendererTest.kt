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

import androidx.glance.adaptive.core.ui.selection.GlanceSurface
import androidx.glance.adaptive.core.ui.templates.AdaptiveGlanceTemplate
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TemplateRendererTest {

    private class TestTemplate : AdaptiveGlanceTemplate

    @Test
    fun templateRenderer_invokesRenderWithTemplateAndSurface() {
        var renderedTemplate: TestTemplate? = null
        var renderedSurface: GlanceSurface? = null

        val renderer =
            object : TemplateRenderer<TestTemplate, GlanceSurface> {
                override fun render(template: TestTemplate, surface: GlanceSurface) {
                    renderedTemplate = template
                    renderedSurface = surface
                }
            }

        val template = TestTemplate()
        val surface = GlanceSurface.of("test_surface")
        renderer.render(template, surface)

        assertThat(renderedTemplate).isSameInstanceAs(template)
        assertThat(renderedSurface).isSameInstanceAs(surface)
    }
}
