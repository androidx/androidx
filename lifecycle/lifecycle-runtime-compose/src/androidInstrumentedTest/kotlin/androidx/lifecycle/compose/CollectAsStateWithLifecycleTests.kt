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

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class CollectAsStateWithLifecycleTests {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun test_flowState_getsInitialValue() {
        val _sharedFlow = MutableSharedFlow<String>()
        val flow: Flow<String> = _sharedFlow

        var realValue: String? = null
        rule.setContent {
            realValue = flow.collectAsStateWithLifecycle("0").value
        }

        rule.runOnIdle {
            assertThat(realValue).isEqualTo("0")
        }
    }

    @Test
    fun test_stateFlowState_getsInitialValue() {
        val stateFlow: StateFlow<String> = MutableStateFlow("0")

        var realValue: String? = null
        rule.setContent {
            realValue = stateFlow.collectAsStateWithLifecycle().value
        }

        rule.runOnIdle {
            assertThat(realValue).isEqualTo("0")
        }
    }

    @Test
    fun test_flowState_getsSubsequentFlowEmissions() {
        val _sharedFlow = MutableSharedFlow<String>()
        val flow: Flow<String> = _sharedFlow

        var realValue: String? = null
        rule.setContent {
            realValue = flow.collectAsStateWithLifecycle("0").value
        }

        runBlocking {
            _sharedFlow.emit("1")
        }
        rule.runOnIdle {
            assertThat(realValue).isEqualTo("1")
        }
    }

    @Test
    fun test_stateFlowState_getsSubsequentFlowEmissions() {
        val stateFlow = MutableStateFlow("0")

        var realValue: String? = null
        rule.setContent {
            realValue = stateFlow.collectAsStateWithLifecycle().value
        }

        stateFlow.value = "1"

        rule.runOnIdle {
            assertThat(realValue).isEqualTo("1")
        }
    }

    @Test
    fun test_flowState_doesNotGetEmissionsBelowMinState() {
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.CREATED)
        val _stateFlow = MutableStateFlow("initialValue")
        val flow: Flow<String> = _stateFlow

        var realValue: String? = null
        rule.setContent {
            realValue = flow.collectAsStateWithLifecycle(
                initialValue = "0",
                lifecycle = lifecycleOwner.lifecycle
            ).value
        }

        _stateFlow.value = "1"
        _stateFlow.value = "2"
        rule.runOnIdle {
            assertThat(realValue).isEqualTo("0")
        }
    }

    @Test
    fun test_stateFlowState_doesNotGetEmissionsBelowMinState() {
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.CREATED)
        val stateFlow = MutableStateFlow("0")

        var realValue: String? = null
        rule.setContent {
            realValue = stateFlow.collectAsStateWithLifecycle(
                lifecycle = lifecycleOwner.lifecycle
            ).value
        }

        stateFlow.value = "1"
        stateFlow.value = "2"

        rule.runOnIdle {
            assertThat(realValue).isEqualTo("0")
        }
    }

    @Test
    fun test_flowState_getsEmissionsWhenAboveMinState() {
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.CREATED)
        val _sharedFlow = MutableSharedFlow<String>()
        val flow: Flow<String> = _sharedFlow

        var realValue: String? = null
        rule.setContent {
            realValue = flow.collectAsStateWithLifecycle(
                initialValue = "0",
                lifecycle = lifecycleOwner.lifecycle
            ).value
        }

        runBlocking {
            _sharedFlow.emit("1")
        }
        rule.runOnIdle {
            assertThat(realValue).isEqualTo("0")
        }

        lifecycleOwner.currentState = Lifecycle.State.RESUMED
        runBlocking {
            _sharedFlow.emit("2")
        }
        rule.runOnIdle {
            assertThat(realValue).isEqualTo("2")
        }
    }

    @Test
    fun test_stateFlowState_getsEmissionsWhenAboveMinState() {
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)
        val stateFlow = MutableStateFlow("0")

        var realValue: String? = null
        rule.setContent {
            realValue = stateFlow.collectAsStateWithLifecycle(
                lifecycle = lifecycleOwner.lifecycle
            ).value
        }

        rule.runOnIdle {
            assertThat(realValue).isEqualTo("0")
        }

        stateFlow.value = "1"
        stateFlow.value = "2"

        rule.runOnIdle {
            assertThat(realValue).isEqualTo("2")
        }
    }

    @Test
    fun test_stateFlowState_getsLastEmissionWhenLifecycleIsAboveMin() {
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.CREATED)
        val stateFlow = MutableStateFlow("0")

        var realValue: String? = null
        rule.setContent {
            realValue = stateFlow.collectAsStateWithLifecycle(
                lifecycle = lifecycleOwner.lifecycle
            ).value
        }

        rule.runOnIdle {
            assertThat(realValue).isEqualTo("0")
        }

        stateFlow.value = "1"
        stateFlow.value = "2"
        lifecycleOwner.currentState = Lifecycle.State.RESUMED

        rule.runOnIdle {
            assertThat(realValue).isEqualTo("2")
        }
    }
}
