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

package androidx.appstate.transform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.kruth.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ListenerTest {

    @Test
    fun testListenerUpdated() = runTest {
        var sourceState by mutableStateOf(0)
        val state = mutableStateOf(0)
        backgroundScope.launch { listener(testDispatcher) { state.value = sourceState } }

        sourceState = 1
        runRecomposition()
        assertThat(state.value).isEqualTo(1)
    }

    @Test
    fun testListenerCancellation() = runTest {
        val scope = CoroutineScope(testDispatcher + SupervisorJob())
        var sourceState by mutableStateOf(0)
        val state = mutableStateOf(0)
        val job = scope.launch { listener(testDispatcher) { state.value = sourceState } }

        assertThat(state.value).isEqualTo(0)

        sourceState = 1
        runRecomposition()
        assertThat(state.value).isEqualTo(1)

        job.cancel()

        sourceState = 2
        runRecomposition()
        // Should still be 1 because the scope was cancelled.
        assertThat(state.value).isEqualTo(1)
    }
}
