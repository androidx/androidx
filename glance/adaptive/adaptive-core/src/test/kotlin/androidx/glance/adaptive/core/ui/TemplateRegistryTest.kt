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

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.glance.adaptive.core.ui.selection.Dimensions
import androidx.glance.adaptive.core.ui.selection.GlanceSurface
import androidx.glance.adaptive.core.ui.selection.LocalContainerDimensions
import androidx.glance.adaptive.core.ui.templates.AdaptiveGlanceTemplate
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class TemplateRegistryTest {

    private data class DummyTemplate(val title: String) : AdaptiveGlanceTemplate

    private enum class DummyArchetype {
        CARD
    }

    @Before
    fun setUp() {
        TemplateRegistry.resetForTesting()
    }

    @Test
    fun register_and_render_dispatchesCorrectArchetype() {
        var observedData: DummyTemplate? = null
        var observedArchetype: DummyArchetype? = null

        TemplateRegistry.register(
            DummyTemplate::class.java,
            selectArchetype = { _, surface, dimensions ->
                assertThat(surface).isEqualTo(GlanceSurface.MOBILE_HOME_SCREEN)
                assertThat(dimensions).isEqualTo(Dimensions(200, 100))
                DummyArchetype.CARD
            },
            renderArchetype = { data, archetype ->
                observedData = data
                observedArchetype = archetype
            },
        )

        val dummy = DummyTemplate("Test")

        runComposition {
            CompositionLocalProvider(LocalContainerDimensions provides Dimensions(200, 100)) {
                TemplateRegistry.render(dummy, GlanceSurface.MOBILE_HOME_SCREEN)
            }
        }

        assertThat(observedData).isEqualTo(dummy)
        assertThat(observedArchetype).isEqualTo(DummyArchetype.CARD)
    }

    @Test(expected = IllegalArgumentException::class)
    fun render_unregistered_throwsException() {
        val dummy = DummyTemplate("Unregistered")
        runComposition { TemplateRegistry.render(dummy, GlanceSurface.MOBILE_HOME_SCREEN) }
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
