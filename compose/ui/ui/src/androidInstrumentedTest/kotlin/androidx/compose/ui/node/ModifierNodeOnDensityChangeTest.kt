/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.node

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.PointerInputModifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ModifierNodeOnDensityChangeTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun densityChange_triggersNodeCallback() {
        var onDensityChangeCalls = 0
        val initialDensity = Density(1f)
        val newDensity = Density(3f)
        var density by mutableStateOf(initialDensity)
        lateinit var nodeDensity: Density
        val modifierNode =
            object : Modifier.Node() {
                override fun onAttach() {
                    nodeDensity = requireDensity()
                }

                override fun onDensityChange() {
                    onDensityChangeCalls++
                    nodeDensity = requireDensity()
                }
            }

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                Box(Modifier.elementOf(modifierNode))
            }
        }

        rule.runOnIdle {
            assertThat(nodeDensity).isEqualTo(initialDensity)
            assertThat(onDensityChangeCalls).isEqualTo(0)
            density = newDensity
        }

        rule.runOnIdle {
            assertThat(nodeDensity).isEqualTo(newDensity)
            assertThat(onDensityChangeCalls).isEqualTo(1)
        }
    }

    @Test
    fun densityChange_backwardsCompatNode_pointerInputModifier_triggersOnCancel() {
        var onCancelCalled = false
        val initialDensity = Density(1f)
        val newDensity = Density(3f)
        var density by mutableStateOf(initialDensity)

        val modifier =
            object : PointerInputModifier {
                override val pointerInputFilter =
                    object : PointerInputFilter() {
                        override fun onPointerEvent(
                            pointerEvent: PointerEvent,
                            pass: PointerEventPass,
                            bounds: IntSize,
                        ) {}

                        override fun onCancel() {
                            onCancelCalled = true
                        }
                    }
            }

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides density) { Box(modifier) }
        }

        rule.runOnIdle {
            assertThat(onCancelCalled).isFalse()
            density = newDensity
        }

        rule.runOnIdle { assertThat(onCancelCalled).isTrue() }
    }

    // Regression test for b/374079517
    @Test
    fun densityChange_backwardsCompatNode_nonPointerInputModifier_doesNotCrash() {
        val initialDensity = Density(1f)
        val newDensity = Density(3f)
        var density by mutableStateOf(initialDensity)

        val modifier =
            object : DrawModifier {
                override fun ContentDrawScope.draw() {
                    drawContent()
                }
            }

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides density) { Box(modifier) }
        }

        rule.runOnIdle { density = newDensity }

        // Should not crash
        rule.waitForIdle()
    }
}
