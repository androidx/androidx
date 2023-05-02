/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.lifecycle.compose

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class LifecycleEffectTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var lifecycleOwner: TestLifecycleOwner

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        lifecycleOwner = TestLifecycleOwner(coroutineDispatcher = dispatcher)
        Dispatchers.setMain(dispatcher)
    }

    @Test
    fun lifecycleEventEffectTest_noEvent() {
        var stopCount = 0

        composeTestRule.waitForIdle()
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
                    stopCount++
                }
            }
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Lifecycle should not have been stopped")
                .that(stopCount)
                .isEqualTo(0)
        }
    }

    @Test
    fun lifecycleEventEffectTest_localLifecycleOwner() {
        val expectedEvent = Lifecycle.Event.ON_STOP
        var stopCount = 0

        composeTestRule.waitForIdle()
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                LifecycleEventEffect(expectedEvent) {
                    stopCount++
                }
            }
        }

        composeTestRule.runOnIdle {
            lifecycleOwner.handleLifecycleEvent(expectedEvent)
            assertWithMessage("Lifecycle should have been stopped")
                .that(stopCount)
                .isEqualTo(1)
        }
    }

    @Test
    fun lifecycleEventEffectTest_customLifecycleOwner() {
        val expectedEvent = Lifecycle.Event.ON_STOP
        var stopCount = 0

        composeTestRule.waitForIdle()
        composeTestRule.setContent {
            LifecycleEventEffect(expectedEvent, lifecycleOwner) {
                stopCount++
            }
        }

        composeTestRule.runOnIdle {
            lifecycleOwner.handleLifecycleEvent(expectedEvent)
            assertWithMessage("Lifecycle should have been stopped")
                .that(stopCount)
                .isEqualTo(1)
        }
    }
}