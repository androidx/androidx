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
import androidx.compose.runtime.tooling.ObservableComposition
import androidx.compose.runtime.tooling.setObserver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.test.IgnoreJsTarget
import kotlinx.test.IgnoreWasmTarget

@Stable
@OptIn(ExperimentalComposeRuntimeApi::class)
@Suppress("unused")
class CompositionObserverTests {
    private class SingleScopeObserver(var target: RecomposeScope? = null) : CompositionObserver {
        var startCount = 0
        var endCount = 0
        var disposedCount = 0

        override fun onScopeEnter(scope: RecomposeScope) {
            if (scope == target) {
                startCount++
            }
        }

        override fun onScopeExit(scope: RecomposeScope) {
            if (scope == target) {
                endCount++
            }
        }

        override fun onScopeDisposed(scope: RecomposeScope) {
            if (scope == target) {
                disposedCount++
            }
        }

        override fun onBeginComposition(composition: ObservableComposition) {}

        override fun onEndComposition(composition: ObservableComposition) {}

        override fun onReadInScope(scope: RecomposeScope, value: Any) {}

        override fun onScopeInvalidated(scope: RecomposeScope, value: Any?) {}
    }

    @Test
    // TODO: b/409727436
    // TODO: https://youtrack.jetbrains.com/issue/CMP-797
    @IgnoreJsTarget
    @IgnoreWasmTarget
    fun observeScope() {
        val observer = SingleScopeObserver()
        compositionTest {
            var data by mutableStateOf(0)
            var scope: RecomposeScope? = null

            compose {
                scope = currentRecomposeScope
                Text("$data")
            }

            validate { Text("$data") }

            observer.target = scope
            composition?.setObserver(observer)

            data++
            expectChanges()
            revalidate()
        }

        assertEquals(1, observer.startCount)
        assertEquals(1, observer.endCount)
        assertEquals(1, observer.disposedCount)
    }

    @Test
    // TODO: b/409727436
    // TODO: https://youtrack.jetbrains.com/issue/CMP-797
    @IgnoreJsTarget
    @IgnoreWasmTarget
    fun observeScope_dispose() {
        val observer = SingleScopeObserver()
        compositionTest {
            var data by mutableStateOf(0)
            var scope: RecomposeScope? = null

            compose {
                scope = currentRecomposeScope
                Text("$data")
            }

            validate { Text("$data") }

            observer.target = scope
            val handle = composition?.setObserver(observer)

            data++
            expectChanges()
            revalidate()

            handle?.dispose()

            data++
            expectChanges()
            revalidate()
        }

        assertEquals(1, observer.startCount)
        assertEquals(1, observer.endCount)
        // 0 because the observer was disposed before the scope was disposed.
        assertEquals(0, observer.disposedCount)
    }

    @Test
    // TODO: b/409727436
    // TODO: https://youtrack.jetbrains.com/issue/CMP-797
    @IgnoreJsTarget
    @IgnoreWasmTarget
    fun observeScope_scopeRemoved() {
        val observer = SingleScopeObserver()
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

            observer.target = scope
            composition?.setObserver(observer)

            data++
            expectChanges()
            revalidate()

            assertEquals(0, observer.disposedCount)
            visible = false
            expectChanges()
            revalidate()

            assertEquals(1, observer.disposedCount)
        }

        assertEquals(1, observer.startCount)
        assertEquals(1, observer.endCount)
        assertEquals(1, observer.disposedCount)
    }

    @Test
    fun observeComposition() = compositionTest {
        var beginCount = 0
        var endCount = 0
        var data by mutableStateOf(0)
        val observer =
            object : CompositionObserver {
                override fun onBeginComposition(composition: ObservableComposition) {
                    beginCount++
                }

                override fun onEndComposition(composition: ObservableComposition) {
                    endCount++
                }

                override fun onScopeEnter(scope: RecomposeScope) {}

                override fun onScopeExit(scope: RecomposeScope) {}

                override fun onScopeInvalidated(scope: RecomposeScope, value: Any?) {}

                override fun onReadInScope(scope: RecomposeScope, value: Any) {}

                override fun onScopeDisposed(scope: RecomposeScope) {}
            }

        val handle = compose(observer) { Text("Some composition: $data") }

        validate { Text("Some composition: $data") }

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
        val observer =
            object : CompositionObserver {
                override fun onBeginComposition(composition: ObservableComposition) {
                    beginCount++
                }

                override fun onEndComposition(composition: ObservableComposition) {
                    endCount++
                }

                override fun onScopeEnter(scope: RecomposeScope) {}

                override fun onScopeExit(scope: RecomposeScope) {}

                override fun onReadInScope(scope: RecomposeScope, value: Any) {}

                override fun onScopeInvalidated(scope: RecomposeScope, value: Any?) {}

                override fun onScopeDisposed(scope: RecomposeScope) {}
            }
        compose { Text("Some composition: $data") }

        validate { Text("Some composition: $data") }
        val handle = composition?.setObserver(observer)

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
        val compositionsSeen = mutableSetOf<ObservableComposition>()
        val observer =
            object : CompositionObserver {
                override fun onBeginComposition(composition: ObservableComposition) {
                    beginCount++
                    compositionsSeen += composition
                }

                override fun onEndComposition(composition: ObservableComposition) {
                    endCount++
                }

                override fun onScopeEnter(scope: RecomposeScope) {}

                override fun onScopeExit(scope: RecomposeScope) {}

                override fun onReadInScope(scope: RecomposeScope, value: Any) {}

                override fun onScopeInvalidated(scope: RecomposeScope, value: Any?) {}

                override fun onScopeDisposed(scope: RecomposeScope) {}
            }

        var seen = data
        val handle =
            compose(observer) {
                Text("Root: $data")

                TestSubcomposition { seen = data }
            }

        assertEquals(data, seen)
        assertEquals(2, beginCount)
        assertEquals(2, endCount)
        assertEquals(2, compositionsSeen.size)

        data++
        expectChanges()

        // It is valid for these to be any mutable of 2 > 2
        assertTrue(beginCount > 2)
        assertEquals(beginCount, endCount)
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
        val compositionsSeen = mutableSetOf<ObservableComposition>()
        val observer =
            object : CompositionObserver {
                override fun onBeginComposition(composition: ObservableComposition) {
                    beginCount++
                    compositionsSeen += composition
                }

                override fun onEndComposition(composition: ObservableComposition) {
                    endCount++
                }

                override fun onScopeEnter(scope: RecomposeScope) {}

                override fun onScopeExit(scope: RecomposeScope) {}

                override fun onReadInScope(scope: RecomposeScope, value: Any) {}

                override fun onScopeInvalidated(scope: RecomposeScope, value: Any?) {}

                override fun onScopeDisposed(scope: RecomposeScope) {}
            }

        var seen = data
        compose {
            Text("Root: $data")

            TestSubcomposition { seen = data }
        }

        assertEquals(data, seen)
        assertEquals(0, beginCount)
        assertEquals(0, endCount)
        assertEquals(0, compositionsSeen.size)

        val handle = composition?.setObserver(observer)
        data++
        expectChanges()

        // It is valid for these to be any mutable of 2 > 0
        assertTrue(beginCount > 0)
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
        val compositionsSeen = mutableSetOf<ObservableComposition>()
        val observer1 =
            object : CompositionObserver {
                override fun onBeginComposition(composition: ObservableComposition) {
                    compositionsSeen.add(composition)
                    beginCountOne++
                }

                override fun onEndComposition(composition: ObservableComposition) {
                    endCountOne++
                }

                override fun onScopeEnter(scope: RecomposeScope) {}

                override fun onScopeExit(scope: RecomposeScope) {}

                override fun onReadInScope(scope: RecomposeScope, value: Any) {}

                override fun onScopeDisposed(scope: RecomposeScope) {}

                override fun onScopeInvalidated(scope: RecomposeScope, value: Any?) {}
            }
        val observer2 =
            object : CompositionObserver {
                override fun onBeginComposition(composition: ObservableComposition) {
                    beginCountTwo++
                }

                override fun onEndComposition(composition: ObservableComposition) {
                    endCountTwo++
                }

                override fun onScopeEnter(scope: RecomposeScope) {}

                override fun onScopeExit(scope: RecomposeScope) {}

                override fun onReadInScope(scope: RecomposeScope, value: Any) {}

                override fun onScopeDisposed(scope: RecomposeScope) {}

                override fun onScopeInvalidated(scope: RecomposeScope, value: Any?) {}
            }

        var seen = data
        var composition2: Composition? = null
        compose {
            Text("Root: $data")

            TestSubcomposition {
                seen = data

                TestSubcomposition { seen = data }
            }
        }

        assertEquals(data, seen)
        assertEquals(0, beginCountOne)
        assertEquals(0, endCountOne)
        assertEquals(0, compositionsSeen.size)

        val composition = composition ?: error("No composition found")
        val handle = composition.setObserver(observer1)
        data++
        expectChanges()

        // It is valid for these to be any mutable of ;3 > 0
        assertTrue(beginCountOne > 0)
        assertEquals(beginCountOne, endCountOne)
        assertEquals(3, compositionsSeen.size)

        val subComposition = compositionsSeen.first { it != composition }
        val subcomposeHandle = subComposition.setObserver(observer2)

        data++
        expectChanges()

        // It is valid for these to be any mutable of 2 > 0
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
        val handle2 = composition.setObserver(observer1)
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
            val validated = mutableListOf<Int>()
            val invalidations = mutableMapOf<RecomposeScope, Any>()
            val reads = mutableMapOf<RecomposeScope, Any>()
            val handle =
                composition.setObserver(
                    object : CompositionObserver {
                        override fun onBeginComposition(composition: ObservableComposition) {}

                        override fun onEndComposition(composition: ObservableComposition) {}

                        override fun onScopeEnter(scope: RecomposeScope) {
                            validated += expectedScopes.indexOf(scope)
                        }

                        override fun onScopeExit(scope: RecomposeScope) {}

                        override fun onScopeDisposed(scope: RecomposeScope) {}

                        override fun onReadInScope(scope: RecomposeScope, value: Any) {
                            reads[scope] = value
                        }

                        override fun onScopeInvalidated(scope: RecomposeScope, value: Any?) {
                            invalidations[scope] = value!!
                        }
                    }
                )
            for (index in indexes) {
                data[index].value++
            }
            expectChanges()
            assertEquals(validated, indexes.toList())

            reads.entries.forEach { (scope, v) ->
                val index = expectedScopes.indexOf(scope)
                assertEquals(data[index], v)
                assertTrue { index in indexes }
            }

            invalidations.entries.forEach { (scope, v) ->
                val index = expectedScopes.indexOf(scope)
                assertEquals(data[index], v)
                assertTrue { index in indexes }
            }

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

    @Test
    fun nestedScopeObservations() = compositionTest {
        val result = StringBuilder()
        val invalidations = mutableMapOf<RecomposeScope, Int>()
        val observer =
            object : CompositionObserver {
                override fun onBeginComposition(composition: ObservableComposition) {
                    result.appendLine("begin")
                }

                override fun onEndComposition(composition: ObservableComposition) {
                    result.appendLine("end")
                }

                override fun onScopeEnter(scope: RecomposeScope) {
                    result.append("enter")
                    val count = invalidations[scope]
                    if (count != null) {
                        result.append(" ")
                        result.append(count)
                    }
                    result.appendLine()
                }

                override fun onScopeExit(scope: RecomposeScope) {
                    result.appendLine("exit")
                }

                override fun onReadInScope(scope: RecomposeScope, value: Any) {
                    result.appendLine("read")
                }

                override fun onScopeDisposed(scope: RecomposeScope) {
                    result.appendLine("dispose")
                }

                override fun onScopeInvalidated(scope: RecomposeScope, value: Any?) {
                    val count = invalidations[scope] ?: 0
                    invalidations[scope] = count + 1
                    result.appendLine("invalidate ${value.toString().takeWhile { it != '@' }}")
                }
            }

        var state by mutableStateOf("text")
        compose(observer) { Wrapper { Text(state) } }

        validate { Text(state) }

        assertEquals(
            """
                begin
                enter
                enter
                enter
                read
                exit
                exit
                exit
                end
            """
                .trimIndent()
                .trim(),
            result.toString().trim(),
        )

        result.clear()

        state = "text2"
        advance()

        assertEquals(
            """
                invalidate MutableState(value=text2)
                begin
                enter 1
                read
                exit
                end
            """
                .trimIndent()
                .trim(),
            result.toString().trim(),
        )

        revalidate()
    }
}
