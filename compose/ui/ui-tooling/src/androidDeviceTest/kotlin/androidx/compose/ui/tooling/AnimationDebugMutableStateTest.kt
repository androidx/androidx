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

package androidx.compose.ui.tooling

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalAnimationApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class AnimationDebugMutableStateTest {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @After
    fun tearDown() {
        isAnimationPreviewEnabled = false
    }

    @Test
    fun animationPreviewEnabled() {
        isAnimationPreviewEnabled = true
        lateinit var state: MutableState<Int>
        rule.setContent {
            state = remember {
                animationDebugMutableStateOf(
                    10,
                    { mutableStateOf(it) },
                    { setOf(10, 20) },
                    "customState",
                )
            }
        }
        rule.waitForIdle()
        (state as AnimationDebugMutableState).let {
            assertEquals("customState", it.label)
            assertEquals(setOf(10, 20), it.states())
            assertEquals(10, it.value)
        }
    }

    @Test
    fun animationPreviewDisabled() {
        lateinit var state: MutableState<Int>
        rule.setContent {
            state = remember {
                animationDebugMutableStateOf(
                    10,
                    { mutableStateOf(it) },
                    { setOf(10, 20) },
                    "customState",
                )
            }
        }
        rule.waitForIdle()
        assertFalse(state is AnimationDebugMutableState)
    }
}
