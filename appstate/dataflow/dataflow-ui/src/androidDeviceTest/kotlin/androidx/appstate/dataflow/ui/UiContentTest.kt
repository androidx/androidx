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

package androidx.appstate.dataflow.ui

import androidx.appstate.dataflow.DataKey
import androidx.appstate.dataflow.DataflowRegistry
import androidx.appstate.dataflow.Reducer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.kruth.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.reflect.KClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiContentTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun observesDataAndRecomposesOnUpdate() {
        var sourceCount by mutableIntStateOf(1)
        val registry = createTestRegistry(TestData.Companion::class) { TestData(sourceCount) }

        var latestData: TestData? = null
        composeTestRule.setContent {
            registry.UiContent(TestData) { data -> latestData = data }
        }

        composeTestRule.waitForIdle()
        assertThat(latestData).isEqualTo(TestData(1))

        sourceCount = 42
        composeTestRule.waitForIdle()
        assertThat(latestData).isEqualTo(TestData(42))

        registry.dispose()
    }

    @Test
    fun withParameterizedKey_passesKeyAndSwitchesActiveReducerOnKeyChange() {
        val createdInputs = mutableListOf<Int>()
        val disposedInputs = mutableListOf<Int>()
        val registry =
            createTestRegistry(InputTestKey::class) { key ->
                DisposableEffect(key.input) {
                    createdInputs.add(key.input)
                    onDispose { disposedInputs.add(key.input) }
                }
                TestData(key.input)
            }

        var currentInput by mutableIntStateOf(10)
        var latestData: TestData? = null
        composeTestRule.setContent {
            registry.UiContent(InputTestKey(currentInput)) { data -> latestData = data }
        }

        composeTestRule.waitForIdle()
        assertThat(latestData).isEqualTo(TestData(10))
        assertThat(createdInputs).containsExactly(10)
        assertThat(disposedInputs).isEmpty()

        // Changing key switches to a new active reducer and disposes the previous one
        currentInput = 20
        composeTestRule.waitForIdle()
        assertThat(latestData).isEqualTo(TestData(20))
        assertThat(createdInputs).containsExactly(10, 20)
        assertThat(disposedInputs).containsExactly(10)

        registry.dispose()
    }

    @Test
    fun disposesActiveReducerWhenLeavingComposition() {
        var disposeCount = 0
        val registry =
            createTestRegistry(TestData.Companion::class) {
                DisposableEffect(Unit) { onDispose { disposeCount++ } }
                TestData(1)
            }

        var showContent by mutableStateOf(true)
        var latestData: TestData? = null
        composeTestRule.setContent {
            if (showContent) {
                registry.UiContent(key = TestData) { data -> latestData = data }
            }
        }

        composeTestRule.waitForIdle()
        assertThat(latestData).isEqualTo(TestData(1))
        assertThat(disposeCount).isEqualTo(0)

        showContent = false
        composeTestRule.waitForIdle()
        assertThat(disposeCount).isEqualTo(1)

        registry.dispose()
    }

    private fun <K : DataKey<TestData>> createTestRegistry(
        keyClass: KClass<K>,
        initialValue: TestData = TestData(0),
        reduce: @Composable (K) -> TestData,
    ): DataflowRegistry =
        DataflowRegistry(
            coroutineContext = AndroidUiDispatcher.Main,
            reducerFactories =
                mapOf(
                    keyClass to
                        Reducer.Factory {
                            object : Reducer<K, TestData> {
                                override val initialValue = initialValue

                                @Composable override fun reduce(key: K): TestData = reduce(key)
                            }
                        }
                ),
        )

    private data class TestData(val count: Int) {
        companion object : DataKey<TestData>
    }

    private data class InputTestKey(val input: Int) : DataKey<TestData>
}
