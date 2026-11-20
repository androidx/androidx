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

package androidx.appstate.dataflow

import androidx.appstate.transform.testing.transformTest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlinx.coroutines.test.TestDispatcher

class DataflowRegistryTest {

    @Test
    fun getState_initialState() = transformTest {
        val registry = getTestRegistry(testDispatcher)
        val state = registry.getState(TestData)
        assertThat(state.value).isEqualTo(TestData(0))
        registry.dispose()
    }

    @Test
    fun getState_updatesOnRecomposition() = transformTest {
        var sourceCount by mutableStateOf(1)
        val registry =
            DataflowRegistry(
                coroutineContext = testDispatcher,
                reducerFactories =
                    mapOf(
                        TestData.Companion::class to
                            Reducer.Factory {
                                object : Reducer<DataKey<TestData>, TestData> {
                                    override val initialValue = TestData(0)

                                    @Composable
                                    override fun reduce(key: DataKey<TestData>): TestData {
                                        return TestData(sourceCount)
                                    }
                                }
                            }
                    ),
            )

        val state = registry.getState(TestData)
        advance()
        assertThat(state.value).isEqualTo(TestData(1))

        sourceCount = 42
        advance()
        assertThat(state.value).isEqualTo(TestData(42))

        registry.dispose()
    }

    @Test
    fun getState_primitiveAndGenericOutputs_doNotCollide() = transformTest {
        val registry =
            DataflowRegistry(
                coroutineContext = testDispatcher,
                reducerFactories =
                    mapOf(
                        UsernameKey::class to
                            Reducer.Factory {
                                object : Reducer<UsernameKey, String> {
                                    override val initialValue = ""

                                    @Composable override fun reduce(key: UsernameKey) = "Alice"
                                }
                            },
                        RecentItemsKey::class to
                            Reducer.Factory {
                                object : Reducer<RecentItemsKey, List<String>> {
                                    override val initialValue = emptyList<String>()

                                    @Composable
                                    override fun reduce(key: RecentItemsKey) = listOf("a", "b")
                                }
                            },
                        FeaturedItemsKey::class to
                            Reducer.Factory {
                                object : Reducer<FeaturedItemsKey, List<String>> {
                                    override val initialValue = emptyList<String>()

                                    @Composable
                                    override fun reduce(key: FeaturedItemsKey) = listOf("x", "y")
                                }
                            },
                    ),
            )

        val username = registry.getState(UsernameKey)
        val recent = registry.getState(RecentItemsKey)
        val featured = registry.getState(FeaturedItemsKey)
        advance()

        assertThat(username.value).isEqualTo("Alice")
        assertThat(recent.value).containsExactly("a", "b").inOrder()
        assertThat(featured.value).containsExactly("x", "y").inOrder()

        registry.dispose()
    }

    @Test
    fun getState_activeReducer() = transformTest {
        var createCount = 0
        val registry =
            DataflowRegistry(
                coroutineContext = testDispatcher,
                parent = null,
                reducerFactories =
                    mapOf(
                        TestData.Companion::class to
                            Reducer.Factory {
                                createCount++
                                TestReducer()
                            }
                    ),
            )
        val first: TestData = registry.getState(TestData).value
        val second: TestData = registry.getState(TestData).value
        assertThat(first).isEqualTo(TestData(0))
        assertThat(second).isEqualTo(TestData(0))
        assertThat(createCount).isEqualTo(1)
        registry.dispose()
    }

    @Test
    fun getState_withParameterizedKey_passesKeyAndIsolatesActiveReducers() = transformTest {
        var createCount = 0
        val registry =
            DataflowRegistry(
                coroutineContext = testDispatcher,
                parent = null,
                reducerFactories =
                    mapOf(
                        ParameterizedTestKey::class to
                            Reducer.Factory {
                                createCount++
                                ParameterizedTestReducer()
                            }
                    ),
            )

        val state1 = registry.getState(ParameterizedTestKey(count = 10))
        val state2 = registry.getState(ParameterizedTestKey(count = 20))
        advance()

        assertThat(state1.value).isEqualTo(TestData(10))
        assertThat(state2.value).isEqualTo(TestData(20))
        assertThat(createCount).isEqualTo(2)

        // Requesting an equal key reuses the active reducer for that key
        val state1Again = registry.getState(ParameterizedTestKey(count = 10))
        assertThat(state1Again.value).isEqualTo(TestData(10))
        assertThat(createCount).isEqualTo(2)

        registry.dispose()
    }

    @Test
    fun getState_dispose_deactivatesReducerWhenAllReferencesDisposed() = transformTest {
        var createCount = 0
        var disposeCount = 0
        var sourceCount by mutableStateOf(1)

        val registry =
            DataflowRegistry(
                coroutineContext = testDispatcher,
                parent = null,
                reducerFactories =
                    mapOf(
                        TestData.Companion::class to
                            Reducer.Factory {
                                createCount++
                                object : Reducer<DataKey<TestData>, TestData> {
                                    override val initialValue = TestData(0)

                                    @Composable
                                    override fun reduce(key: DataKey<TestData>): TestData {
                                        DisposableEffect(Unit) { onDispose { disposeCount++ } }
                                        return TestData(sourceCount)
                                    }
                                }
                            }
                    ),
            )

        val state1 = registry.getState(TestData)
        val state2 = registry.getState(TestData)
        advance()

        assertThat(createCount).isEqualTo(1)
        assertThat(disposeCount).isEqualTo(0)
        assertThat(state1.value).isEqualTo(TestData(1))
        assertThat(state2.value).isEqualTo(TestData(1))

        // Disposing the first reference keeps the reducer active because state2 is still active
        state1.dispose()
        // Disposing state1 again is idempotent and must not decrement references a second time
        state1.dispose()
        advance()
        assertThat(disposeCount).isEqualTo(0)

        sourceCount = 2
        advance()
        assertThat(state2.value).isEqualTo(TestData(2))

        // Disposing the last reference deactivates and removes the reducer
        state2.dispose()
        advance()
        assertThat(disposeCount).isEqualTo(1)

        // Requesting state again creates a new active reducer instance
        val state3 = registry.getState(TestData)
        advance()
        assertThat(createCount).isEqualTo(2)
        assertThat(state3.value).isEqualTo(TestData(2))

        registry.dispose()
    }

    @Test
    fun parentRegistry_delegatesGetState() = transformTest {
        val parentReducer = TestReducer(initialValue = TestData(42))
        val parentRegistry =
            getTestRegistry(
                testDispatcher,
                mapOf(TestData.Companion::class to Reducer.Factory { parentReducer }),
            )
        val childRegistry =
            DataflowRegistry(
                coroutineContext = testDispatcher,
                parent = parentRegistry,
                reducerFactories = emptyMap(),
            )

        val state = childRegistry.getState(TestData)
        assertThat(state.value).isEqualTo(TestData(42))

        parentRegistry.dispose()
        assertThrows<IllegalStateException> { childRegistry.getState(TestData) }
            .hasMessageThat()
            .contains("Parent registry is closed")

        childRegistry.dispose()
    }

    @Test
    fun getState_notFound() = transformTest {
        val registry = getTestRegistry(testDispatcher, emptyMap())
        assertThrows<IllegalStateException> { registry.getState(UnregisteredKey) }
            .hasMessageThat()
            .startsWith("No reducer for")
        registry.dispose()
    }

    @Test
    fun throwsWhenRegistryIsClosed() = transformTest {
        val registry = getTestRegistry(testDispatcher)
        registry.dispose()

        assertThrows<IllegalStateException> { registry.getState(TestData) }
            .hasMessageThat()
            .contains("Registry is closed")
    }

    @Test
    fun idempotentClose() = transformTest {
        val registry = getTestRegistry(testDispatcher)
        registry.dispose()
        registry.dispose()
    }

    private fun getTestRegistry(
        testDispatcher: TestDispatcher,
        reducerFactories: Map<KClass<out DataKey<*>>, Reducer.Factory<*, *>> =
            mapOf(TestData.Companion::class to Reducer.Factory { TestReducer() }),
    ) =
        DataflowRegistry(
            coroutineContext = testDispatcher,
            parent = null,
            reducerFactories = reducerFactories,
        )

    private data class TestData(val count: Int) {
        companion object : DataKey<TestData>
    }

    private data class ParameterizedTestKey(val count: Int) : DataKey<TestData>

    private data object UsernameKey : DataKey<String>

    private data object RecentItemsKey : DataKey<List<String>>

    private data object FeaturedItemsKey : DataKey<List<String>>

    private data object UnregisteredKey : DataKey<String>

    private class TestReducer(override val initialValue: TestData = TestData(0)) :
        Reducer<DataKey<TestData>, TestData> {
        @Composable
        override fun reduce(key: DataKey<TestData>): TestData {
            return initialValue
        }
    }

    private class ParameterizedTestReducer(override val initialValue: TestData = TestData(0)) :
        Reducer<ParameterizedTestKey, TestData> {
        @Composable
        override fun reduce(key: ParameterizedTestKey): TestData {
            return TestData(key.count)
        }
    }
}
