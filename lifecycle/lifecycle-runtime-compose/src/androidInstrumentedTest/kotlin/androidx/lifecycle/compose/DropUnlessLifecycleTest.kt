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

package androidx.lifecycle.compose

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.kruth.assertThat
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.testing.TestLifecycleOwner
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class DropUnlessLifecycleTest {

    //region dropUnlessStarted
    @Test
    fun dropUnlessStarted_lifecycleInitialized_doNothing() {
        testDropUnlessStarted(currentLifecycleState = State.INITIALIZED, shouldInvoke = false)
    }

    @Test
    fun dropUnlessStarted_lifecycleCreated_doNothing() {
        testDropUnlessStarted(currentLifecycleState = State.CREATED, shouldInvoke = false)
    }

    @Test
    fun dropUnlessStarted_lifecycleStarted_invoke() {
        testDropUnlessStarted(currentLifecycleState = State.STARTED, shouldInvoke = true)
    }

    @Test
    fun dropUnlessStarted_lifecycleResumed_invoke() {
        testDropUnlessStarted(currentLifecycleState = State.RESUMED, shouldInvoke = true)
    }

    @Test
    fun dropUnlessStarted_lifecycleDestroyed_doNothing() {
        testDropUnlessStarted(currentLifecycleState = State.DESTROYED, shouldInvoke = false)
    }

    private fun testDropUnlessStarted(
        currentLifecycleState: State,
        shouldInvoke: Boolean
    ) = runComposeUiTest {
        val lifecycleOwner = TestLifecycleOwner(State.CREATED).apply {
            currentState = currentLifecycleState
        }
        var hasBeenInvoked = false

        waitForIdle()
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                val underTest = dropUnlessStarted {
                    hasBeenInvoked = true
                }
                underTest.invoke()
            }
        }

        runOnIdle {
            assertThat(hasBeenInvoked).isEqualTo(shouldInvoke)
        }
    }
    //endregion

    //region dropUnlessResumed
    @Test
    fun dropUnlessResumed_lifecycleInitialized_doNothing() {
        testDropUnlessResumed(currentLifecycleState = State.INITIALIZED, shouldInvoke = false)
    }

    @Test
    fun dropUnlessResumed_lifecycleCreated_doNothing() {
        testDropUnlessResumed(currentLifecycleState = State.CREATED, shouldInvoke = false)
    }

    @Test
    fun dropUnlessResumed_lifecycleStarted_invokes() {
        testDropUnlessResumed(currentLifecycleState = State.STARTED, shouldInvoke = false)
    }

    @Test
    fun dropUnlessResumed_lifecycleResumed_invoke() {
        testDropUnlessResumed(currentLifecycleState = State.RESUMED, shouldInvoke = true)
    }

    @Test
    fun dropUnlessResumed_lifecycleDestroyed_doNothing() {
        testDropUnlessResumed(currentLifecycleState = State.DESTROYED, shouldInvoke = false)
    }

    private fun testDropUnlessResumed(
        currentLifecycleState: State,
        shouldInvoke: Boolean
    ) = runComposeUiTest {
        val lifecycleOwner = TestLifecycleOwner(State.CREATED).apply {
            currentState = currentLifecycleState
        }
        var hasBeenInvoked = false

        waitForIdle()
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                val underTest = dropUnlessResumed {
                    hasBeenInvoked = true
                }
                underTest.invoke()
            }
        }

        runOnIdle {
            assertThat(hasBeenInvoked).isEqualTo(shouldInvoke)
        }
    }
    //endregion
}
