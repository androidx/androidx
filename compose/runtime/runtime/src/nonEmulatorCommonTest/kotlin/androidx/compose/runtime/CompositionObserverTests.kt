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

package androidx.compose.runtime

import androidx.compose.runtime.mock.Text
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mock.expectChanges
import androidx.compose.runtime.mock.revalidate
import androidx.compose.runtime.mock.validate
import androidx.compose.runtime.tooling.CompositionObserver
import androidx.compose.runtime.tooling.RecomposeScopeObserver
import androidx.compose.runtime.tooling.observe
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Stable
@OptIn(ExperimentalComposeRuntimeApi::class)
@Suppress("unused")
class CompositionObserverTests {
    @Test
    fun observeScope() {
        var startCount = 0
        var endCount = 0
        var disposedCount = 0

        compositionTest {
            var data by mutableStateOf(0)
            var scope: RecomposeScope? = null

            compose {
                scope = currentRecomposeScope
                Text("$data")
            }

            validate {
                Text("$data")
            }

            scope?.observe(object : RecomposeScopeObserver {
                override fun onBeginScopeComposition(scope: RecomposeScope) {
                    startCount++
                }

                override fun onEndScopeComposition(scope: RecomposeScope) {
                    endCount++
                }

                override fun onScopeDisposed(scope: RecomposeScope) {
                    disposedCount++
                }
            })

            data++
            expectChanges()
            revalidate()
        }

        assertEquals(1, startCount)
        assertEquals(1, endCount)
        assertEquals(1, disposedCount)
    }

    @Test
    fun observeScope_dispose() {
        var startCount = 0
        var endCount = 0
        var disposedCount = 0

        compositionTest {
            var data by mutableStateOf(0)
            var scope: RecomposeScope? = null

            compose {
                scope = currentRecomposeScope
                Text("$data")
            }

            validate {
                Text("$data")
            }

            val handle = scope?.observe(object : RecomposeScopeObserver {
                override fun onBeginScopeComposition(scope: RecomposeScope) {
                    startCount++
                }

                override fun onEndScopeComposition(scope: RecomposeScope) {
                    endCount++
                }

                override fun onScopeDisposed(scope: RecomposeScope) {
                    disposedCount++
                }
            })

            data++
            expectChanges()
            revalidate()

            handle?.dispose()

            data++
            expectChanges()
            revalidate()
        }

        assertEquals(1, startCount)
        assertEquals(1, endCount)
        // 0 because the observer was disposed before the scope was disposed.
        assertEquals(0, disposedCount)
    }

    @Test
    fun observeScope_scopeRemoved() {
        var startCount = 0
        var endCount = 0
        var disposedCount = 0

        compositionTest {
            var data by mutableStateOf(0)
            var visible by mutableStateOf(true)
            var scope: RecomposeScope? = null

            compose {
                if (visible) {
                    Wrap {
                        scope = currentRecomposeScope
                        Text("$data")
                    }
                }
            }

            validate {
                if (visible) {
                    Text("$data")
                }
            }

            scope?.observe(object : RecomposeScopeObserver {
                override fun onBeginScopeComposition(scope: RecomposeScope) {
                    startCount++
                }

                override fun onEndScopeComposition(scope: RecomposeScope) {
                    endCount++
                }

                override fun onScopeDisposed(scope: RecomposeScope) {
                    disposedCount++
                }
            })

            data++
            expectChanges()
            revalidate()

            assertEquals(0, disposedCount)
            visible = false
            expectChanges()
            revalidate()

            assertEquals(1, disposedCount)
        }

        assertEquals(1, startCount)
        assertEquals(1, endCount)
        assertEquals(1, disposedCount)
    }

    @Test
    fun observeComposition() = compositionTest {
        var beginCount = 0
        var endCount = 0
        var data by mutableStateOf(0)
        val observer = object : CompositionObserver {
            override fun onBeginComposition(
                composition: Composition,
                invalidationMap: Map<RecomposeScope, Set<Any>?>
            ) {
                beginCount++
            }

            override fun onEndComposition(composition: Composition) {
                endCount++
            }
        }

        val handle = compose(observer) {
            Text("Some composition: $data")
        }

        validate {
            Text("Some composition: $data")
        }

        assertEquals(1, beginCount)
        assertEquals(1, endCount)

        data++
        expectChanges()
        revalidate()

        assertEquals(2, beginCount)
        assertEquals(2, endCount)

        handle?.dispose()

        data++
        expectChanges()
        revalidate()

        assertEquals(2, beginCount)
        assertEquals(2, endCount)
    }

    @Test
    fun observeComposition_delayedStart() = compositionTest {
        var beginCount = 0
        var endCount = 0
        var data by mutableStateOf(0)
        val observer = object : CompositionObserver {
            override fun onBeginComposition(
                composition: Composition,
                invalidationMap: Map<RecomposeScope, Set<Any>?>
            ) {
                beginCount++
            }

            override fun onEndComposition(composition: Composition) {
                endCount++
            }
        }
        compose {
            Text("Some composition: $data")
        }

        validate {
            Text("Some composition: $data")
        }
        val handle = composition?.observe(observer)

        assertEquals(0, beginCount)
        assertEquals(0, endCount)

        data++
        expectChanges()
        revalidate()

        assertEquals(1, beginCount)
        assertEquals(1, endCount)

        handle?.dispose()

        data++
        expectChanges()
        revalidate()

        assertEquals(1, beginCount)
        assertEquals(1, endCount)
    }

    @Test
    fun observeComposition_observeSubcompose() = compositionTest {
        var beginCount = 0
        var endCount = 0
        var data by mutableStateOf(0)
        val compositionsSeen = mutableSetOf<Composition>()
        val observer = object : CompositionObserver {
            override fun onBeginComposition(
                composition: Composition,
                invalidationMap: Map<RecomposeScope, Set<Any>?>
            ) {
                compositionsSeen.add(composition)
                beginCount++
            }

            override fun onEndComposition(composition: Composition) {
                endCount++
            }
        }

        var seen = data
        val handle = compose(observer) {
            Text("Root: $data")

            TestSubcomposition {
                seen = data
            }
        }

        assertEquals(data, seen)
        assertEquals(2, beginCount)
        assertEquals(2, endCount)
        assertEquals(2, compositionsSeen.size)

        data++
        expectChanges()

        // It is valid for these to be any mutable of 2 > 4
        assertTrue(beginCount > 4)
        assertEquals(beginCount, endCount)
        assertEquals(2, compositionsSeen.size)
        val lastBeginCount = beginCount
        val lastEndCount = endCount
        handle?.dispose()

        data++
        expectChanges()
        assertEquals(lastBeginCount, beginCount)
        assertEquals(lastEndCount, endCount)
    }

    @Test
    fun observeComposition_observeSubcompose_deferred() = compositionTest {
        var beginCount = 0
        var endCount = 0
        var data by mutableStateOf(0)
        val compositionsSeen = mutableSetOf<Composition>()
        val observer = object : CompositionObserver {
            override fun onBeginComposition(
                composition: Composition,
                invalidationMap: Map<RecomposeScope, Set<Any>?>
            ) {
                compositionsSeen.add(composition)
                beginCount++
            }

            override fun onEndComposition(composition: Composition) {
                endCount++
            }
        }

        var seen = data
        compose {
            Text("Root: $data")

            TestSubcomposition {
                seen = data
            }
        }

        assertEquals(data, seen)
        assertEquals(0, beginCount)
        assertEquals(0, endCount)
        assertEquals(0, compositionsSeen.size)

        val handle = composition?.observe(observer)
        data++
        expectChanges()

        // It is valid for these to be any mutable of 2 > 2
        assertTrue(beginCount > 2)
        assertEquals(beginCount, endCount)
        assertEquals(2, compositionsSeen.size)
        val lastBeginCount = beginCount
        val lastEndCount = endCount

        handle?.dispose()
        data++
        expectChanges()
        assertEquals(lastBeginCount, beginCount)
        assertEquals(lastEndCount, endCount)
    }

    @Test
    fun observeComposition_observeSubcompose_shadowing() = compositionTest {
        var beginCountOne = 0
        var endCountOne = 0
        var beginCountTwo = 0
        var endCountTwo = 0
        var data by mutableStateOf(0)
        val compositionsSeen = mutableSetOf<Composition>()
        val observer1 = object : CompositionObserver {
            override fun onBeginComposition(
                composition: Composition,
                invalidationMap: Map<RecomposeScope, Set<Any>?>
            ) {
                compositionsSeen.add(composition)
                beginCountOne++
            }

            override fun onEndComposition(composition: Composition) {
                endCountOne++
            }
        }
        val observer2 = object : CompositionObserver {
            override fun onBeginComposition(
                composition: Composition,
                invalidationMap: Map<RecomposeScope, Set<Any>?>
            ) {
                beginCountTwo++
            }

            override fun onEndComposition(composition: Composition) {
                endCountTwo++
            }
        }

        var seen = data
        compose {
            Text("Root: $data")

            TestSubcomposition {
                seen = data

                TestSubcomposition {
                    seen = data
                }
            }
        }

        assertEquals(data, seen)
        assertEquals(0, beginCountOne)
        assertEquals(0, endCountOne)
        assertEquals(0, compositionsSeen.size)

        val composition = composition ?: error("No composition found")
        val handle = composition.observe(observer1)
        data++
        expectChanges()

        // It is valid for these to be any mutable of 2 > 2
        assertTrue(beginCountOne > 2)
        assertEquals(beginCountOne, endCountOne)
        assertEquals(3, compositionsSeen.size)

        val subComposition = compositionsSeen.first { it != composition }
        val subcomposeHandle = subComposition.observe(observer2)

        data++
        expectChanges()
        assertTrue(beginCountTwo > 0)
        assertEquals(beginCountTwo, endCountTwo)
        val firstBeginCountTwo = beginCountTwo

        val middleCountOne = beginCountOne
        handle?.dispose()
        data++
        expectChanges()

        // Changes for the parent have stopped
        assertEquals(middleCountOne, beginCountOne)
        assertEquals(middleCountOne, endCountOne)

        // but changes for the sub-compositions have not
        assertTrue(beginCountTwo > firstBeginCountTwo)
        assertEquals(beginCountTwo, endCountTwo)
        val middleCountTwo = beginCountTwo

        // Restart the main observer
        val handle2 = composition.observe(observer1)
        data++
        expectChanges()

        assertTrue(beginCountOne > middleCountOne)
        assertTrue(beginCountTwo > middleCountTwo)

        val penultimateCountOne = beginCountOne
        val lastCountTwo = beginCountTwo

        // Dispose the subcompose observer
        subcomposeHandle?.dispose()
        data++
        expectChanges()

        // Assert that we are no longer receiving changes sent to observer2
        assertEquals(lastCountTwo, beginCountTwo)

        // But we are for observer1 and it receives the sub-composition changes.
        assertTrue(beginCountOne >= penultimateCountOne + 3)
        val lastCountOne = beginCountOne

        handle2?.dispose()
        data++
        expectChanges()

        // Assert no are sent.
        assertEquals(lastCountOne, beginCountOne)
        assertEquals(lastCountTwo, beginCountTwo)
    }

    @Test
    fun observeDataChanges() = compositionTest {
        val data = Array(4) { mutableStateOf(0) }
        val expectedScopes = Array<RecomposeScope?>(4) { null }

        compose {
            for (i in data.indices) {
                Wrap {
                    Text("Data ${data[i].value}")
                    expectedScopes[i] = currentRecomposeScope
                }
            }
        }

        validate {
            for (i in data.indices) {
                Text("Data ${data[i].value}")
            }
        }

        // Validate that the scopes are unique
        assertEquals(4, expectedScopes.toSet().size)

        val composition = composition ?: error("No composition")
        fun changes(vararg indexes: Int) {
            var validatedSomething = false
            val handle = composition.observe(
                object : CompositionObserver {
                    override fun onBeginComposition(
                        composition: Composition,
                        invalidationMap: Map<RecomposeScope, Set<Any>?>
                    ) {
                        validatedSomething = true
                        for (index in indexes) {
                            assertTrue(invalidationMap.containsKey(expectedScopes[index]))
                        }
                    }

                    override fun onEndComposition(composition: Composition) {
                        // Nothing to do
                    }
                }
            )
            for (index in indexes) {
                data[index].value++
            }
            expectChanges()
            assertTrue(validatedSomething)
            handle?.dispose()
        }

        changes(0)
        changes(1)
        changes(2)
        changes(3)
        changes(0, 1)
        changes(0, 2)
        changes(0, 3)
        changes(1, 2)
        changes(1, 3)
        changes(2, 3)
        changes(0, 1, 2)
        changes(0, 1, 3)
        changes(0, 2, 3)
        changes(1, 2, 3)
        changes(0, 1, 2, 3)
    }
}
