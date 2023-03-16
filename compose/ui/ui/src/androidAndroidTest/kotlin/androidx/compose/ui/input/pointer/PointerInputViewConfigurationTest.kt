/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.compose.ui.input.pointer

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.TestViewConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The block of code for a pointer input should be reset if the view configuration changes. This
 * class tests all those key possibilities.
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class PointerInputViewConfigurationTest {

    @get:Rule
    val rule = createComposeRule()

    private val tag = "Tagged Layout"

    @Test
    fun compositionLocalViewConfigurationChangeRestartsPointerInputOverload1() {
        compositionLocalViewConfigurationChangeRestartsPointerInput {
            Modifier.pointerInput(Unit, block = it)
        }
    }

    @Test
    fun compositionLocalViewConfigurationChangeRestartsPointerInputOverload2() {
        compositionLocalViewConfigurationChangeRestartsPointerInput {
            Modifier.pointerInput(Unit, Unit, block = it)
        }
    }

    @Test
    fun compositionLocalViewConfigurationChangeRestartsPointerInputOverload3() {
        compositionLocalViewConfigurationChangeRestartsPointerInput {
            Modifier.pointerInput(Unit, Unit, Unit, block = it)
        }
    }

    private fun compositionLocalViewConfigurationChangeRestartsPointerInput(
        pointerInput: (block: suspend PointerInputScope.() -> Unit) -> Modifier
    ) {
        var viewConfigurationTouchSlop by mutableStateOf(18f)

        val pointerInputViewConfigurations = mutableListOf<Float>()
        rule.setContent {
            CompositionLocalProvider(
                LocalViewConfiguration provides TestViewConfiguration(
                    touchSlop = viewConfigurationTouchSlop
                ),
            ) {
                Box(pointerInput {
                    pointerInputViewConfigurations.add(viewConfigurationTouchSlop)
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent()
                        }
                    }
                }.testTag(tag)
                )
            }
        }

        // Because the pointer input coroutine scope is created lazily, that is, it won't be
        // created/triggered until there is a event(tap), we must trigger a tap to instantiate the
        // pointer input block of code.
        rule.waitForIdle()
        rule.onNodeWithTag(tag)
            .performTouchInput {
                down(Offset.Zero)
                moveBy(Offset(1f, 1f))
                up()
            }

        rule.runOnIdle {
            assertThat(pointerInputViewConfigurations.size).isEqualTo(1)
            assertThat(pointerInputViewConfigurations.last()).isEqualTo(18f)
            viewConfigurationTouchSlop = 20f
        }

        rule.waitForIdle()
        rule.onNodeWithTag(tag)
            .performTouchInput {
                down(Offset.Zero)
                moveBy(Offset(1f, 1f))
                up()
            }

        rule.runOnIdle {
            assertThat(pointerInputViewConfigurations.size).isEqualTo(2)
            assertThat(pointerInputViewConfigurations.last()).isEqualTo(20f)
        }
    }
}
