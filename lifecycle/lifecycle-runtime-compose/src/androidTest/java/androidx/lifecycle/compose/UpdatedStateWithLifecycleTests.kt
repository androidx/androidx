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

package androidx.lifecycle.compose

import androidx.activity.ComponentActivity
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalLifecycleComposeApi::class)
class UpdatedStateWithLifecycleTests {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun test_getsInitialValue() {
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.CREATED)
        var realValue: String? = null

        rule.setContent {
            realValue = rememberUpdatedStateWithLifecycle(
                initialValue = "0",
                lifecycleOwner = lifecycleOwner,
                minActiveState = Lifecycle.State.RESUMED,
                updater = { "1" },
            ).value
        }

        rule.runOnIdle {
            Truth.assertThat(realValue).isEqualTo("0")
        }
    }

    @Test
    fun test_getsSubsequentEmissions() {
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.CREATED)
        var count = 0
        var state: State<Int> = mutableStateOf(-1)
        rule.setContent {
            state = rememberUpdatedStateWithLifecycle(
                initialValue = 0,
                lifecycle = lifecycleOwner.lifecycle,
                minActiveState = Lifecycle.State.RESUMED,
                updater = { ++count },
            )
        }

        rule.runOnIdle {
            Truth.assertThat(state.value).isEqualTo(0)
        }

        lifecycleOwner.currentState = Lifecycle.State.RESUMED

        rule.runOnIdle {
            Truth.assertThat(state.value).isEqualTo(1)
        }
    }
}
