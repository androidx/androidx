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

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.kruth.assertThat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import kotlin.test.Test
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalTestApi::class)
class LifecycleExtTest {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val lifecycleOwner = TestLifecycleOwner(
        Lifecycle.State.INITIALIZED,
        UnconfinedTestDispatcher()
    )

    @Test
    fun lifecycleCollectAsState() = runComposeUiTest {
        val lifecycle = lifecycleOwner.lifecycle
        assertThat(lifecycle.currentStateFlow.value).isEqualTo(Lifecycle.State.INITIALIZED)

        var realStateValue: Lifecycle.State? = null
        setContent {
            realStateValue = lifecycle.currentStateAsState().value
        }

        runOnIdle {
            assertThat(realStateValue).isEqualTo(Lifecycle.State.INITIALIZED)
        }

        // TODO(b/280362188): commenting this portion out until bug is fixed
        /*
        lifecycleOwner.currentState = Lifecycle.State.RESUMED
        runOnIdle {
            assertThat(realStateValue).isEqualTo(Lifecycle.State.RESUMED)
        }
        */
    }
}
