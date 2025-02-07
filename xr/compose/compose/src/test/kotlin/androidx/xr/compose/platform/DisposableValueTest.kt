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

@file:Suppress("UNUSED_VARIABLE")

package androidx.xr.compose.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.testing.SubspaceTestingActivity
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DisposableValueTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun disposableValue_isDisposed() {
        var isDisposed = false
        var isInComposition by mutableStateOf(true)

        composeTestRule.setContent {
            if (isInComposition) {
                val unused by remember { disposableValueOf(AutoCloseable { isDisposed = true }) }
            }
        }

        composeTestRule.waitForIdle()
        assertThat(isDisposed).isFalse()
        isInComposition = false
        composeTestRule.waitForIdle()
        assertThat(isDisposed).isTrue()
    }

    @Test
    fun disposableValue_disposeBlock_isDisposed() {
        var isDisposed = false
        var isInComposition by mutableStateOf(true)

        composeTestRule.setContent {
            if (isInComposition) {
                val unused by remember { disposableValueOf(null) { isDisposed = true } }
            }
        }

        composeTestRule.waitForIdle()
        assertThat(isDisposed).isFalse()
        isInComposition = false
        composeTestRule.waitForIdle()
        assertThat(isDisposed).isTrue()
    }

    @Test
    fun disposableValue_isCreatedAndDisposedEachTime() {
        var createdCount = 0
        var disposeCount = 0
        var isInComposition by mutableStateOf(true)

        composeTestRule.setContent {
            if (isInComposition) {
                val unused by remember {
                    disposableValueOf(
                        object : AutoCloseable {
                            init {
                                createdCount++
                            }

                            override fun close() {
                                disposeCount++
                            }
                        }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        assertThat(createdCount).isEqualTo(1)
        assertThat(disposeCount).isEqualTo(0)
        isInComposition = false
        composeTestRule.waitForIdle()
        assertThat(createdCount).isEqualTo(1)
        assertThat(disposeCount).isEqualTo(1)
        isInComposition = true
        composeTestRule.waitForIdle()
        assertThat(createdCount).isEqualTo(2)
        assertThat(disposeCount).isEqualTo(1)
        isInComposition = false
        composeTestRule.waitForIdle()
        assertThat(createdCount).isEqualTo(2)
        assertThat(disposeCount).isEqualTo(2)
    }

    @Test
    fun disposableValue_isDisposedWhenAbandoned() {
        var isDisposed = false

        assertFailsWith<IllegalStateException> {
            composeTestRule.setContent {
                val unused by remember { disposableValueOf(AutoCloseable { isDisposed = true }) }
                error("test")
            }
        }

        composeTestRule.waitForIdle()
        assertThat(isDisposed).isTrue()
    }
}
