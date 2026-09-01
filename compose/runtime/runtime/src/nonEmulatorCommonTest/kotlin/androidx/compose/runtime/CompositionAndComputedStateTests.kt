/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.runtime.mock.Linear
import androidx.compose.runtime.mock.Text
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mock.expectChanges
import androidx.compose.runtime.mock.expectNoChanges
import androidx.compose.runtime.mock.revalidate
import androidx.compose.runtime.mock.validate
import androidx.compose.runtime.snapshots.Snapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests the interaction between [computedStateOf] and composition. */
@Stable
class CompositionAndComputedStateTests {

    @Test
    fun computedStateOfChangedOutputInvalidates() = compositionTest {
        var a by mutableIntStateOf(31)
        var b by mutableIntStateOf(10)
        val answer by computedStateOf { a + b }

        compose { Text("Sum is $answer") }

        validate { Text("Sum is ${a + b}") }

        a++
        expectChanges()

        b++
        expectChanges()

        revalidate()
    }

    @Test
    fun computedStateOfChangedInputsSameOutputsDoesNotInvalidate() = compositionTest {
        var a by mutableIntStateOf(31)
        var b by mutableIntStateOf(10)
        val answer by computedStateOf { a + b }
        var invocations = 0

        compose {
            invocations++
            Text("Sum is $answer")
        }

        validate { Text("Sum is ${a + b}") }
        assertEquals(1, invocations, "Text should compose exactly once")

        a++
        b--
        assertFalse(hasPendingWork(), "Composer should be idle")
        expectNoChanges()
        assertEquals(1, invocations, "Text should compose exactly once")
    }

    @Test
    fun computedStateOfChangedInCompositionInvalidatesAllUsages() = compositionTest {
        var count by mutableIntStateOf(0)
        val computedCount = computedStateOf { count * 10 }

        compose {
            Linear {
                Text("First: ${computedCount.value}")
                Text("Second: ${computedCount.value}")
            }
        }

        validate {
            Linear {
                Text("First: 0")
                Text("Second: 0")
            }
        }

        count++
        expectChanges()
        validate {
            Linear {
                Text("First: 10")
                Text("Second: 10")
            }
        }
        revalidate()
    }

    @Test
    fun observingComputedStateInMultipleScopes() = compositionTest {
        var observeInFirstScope by mutableStateOf(true)
        var count by mutableIntStateOf(0)

        compose {
            val items by remember { computedStateOf { List(count) { it } } }

            Linear {
                if (observeInFirstScope) {
                    Text("List of size ${items.size}")
                }
            }

            Linear { Text("List of size ${items.size}") }
        }

        validate {
            Linear { Text("List of size 0") }

            Linear { Text("List of size 0") }
        }

        observeInFirstScope = false
        advance()
        count++
        advance()

        validate {
            Linear {}

            Linear { Text("List of size 1") }
        }
    }

    @Test
    fun writingToAComputedStateDependencyTriggersAForwardInvalidate() = compositionTest {
        var a by mutableIntStateOf(12)
        var b by mutableIntStateOf(30)
        val c = computedStateOf { a + b }
        compose {
            DisplayIndirect("c", c)
            var d by remember { mutableIntStateOf(0) }
            d = a + b
            val e = remember { computedStateOf { a + b + d } }
            DisplayIndirect("e", e)
        }

        validate {
            Text("c = ${a + b}")
            Text("e = ${a + b + a + b}")
        }

        a++
        expectChanges()
        revalidate()

        b--
        expectChanges()
        revalidate()
    }

    @Test
    fun observingBothNormalAndComputedInSameScope() = compositionTest {
        val a = mutableIntStateOf(0)
        val b = computedStateOf { a.intValue > 0 }
        val c = mutableStateOf(false)

        compose {
            Linear {
                if (b.value) Text("B is true")
                if (c.value) Text("C is true")
            }
        }

        validate {
            Linear {
                if (b.value) Text("B is true")
                if (c.value) Text("C is true")
            }
        }

        a.intValue++
        expectChanges()
        revalidate()

        c.value = true
        advance()
        revalidate()
    }

    @Test
    fun changingTheComputedStateInstanceShouldRelease() = compositionTest {
        var reload by mutableIntStateOf(0)

        compose {
            val items = remember(reload) { computedStateOf { List(10) { it } } }

            Text("List of size ${items.value.size}")
        }

        validate { Text("List of size 10") }

        repeat(10) {
            reload++
            advance()
        }

        revalidate()
    }

    @Test
    fun onlyEvaluateComputedStatesThatAreLive() = compositionTest {
        var a by mutableIntStateOf(11)
        var useD by mutableStateOf(true)

        var dCalculated = 0
        val d = computedStateOf {
            dCalculated++
            a
        }

        compose {
            if (useD) {
                Text("d = ${d.value}")
            } else {
                Text("idle")
            }
        }

        validate { Text("d = $a") }

        useD = false
        expectChanges()
        validate { Text("idle") }

        a++
        expectNoChanges()
        revalidate()
    }

    @Test
    fun computedStateOfNestedChangesInvalidate() = compositionTest {
        var a by mutableIntStateOf(31)
        var b by mutableIntStateOf(10)
        val transient by computedStateOf { a + b }
        val answer by computedStateOf { transient - 1 }

        compose { Text("The answer is $answer") }

        validate { Text("The answer is ${a + b - 1}") }

        a++
        expectChanges()

        b++
        expectChanges()

        revalidate()
    }

    @Test
    fun computedStateOfReferentialMutationPolicyRecomposes() = compositionTest {
        var a by mutableIntStateOf(30)
        var b by mutableIntStateOf(10)
        val answer by computedStateOf(referentialEqualityPolicy()) { listOf(a >= 30, b >= 10) }
        var compositionCount = 0

        compose {
            val remembered = rememberUpdatedState(answer)
            Linear {
                compositionCount++
                Text("The answer is ${remembered.value}")
            }
        }

        validate { Linear { Text("The answer is ${listOf(true, true)}") } }

        assertEquals(1, compositionCount)

        a++
        expectNoChanges()
        assertEquals(2, compositionCount)

        b++
        expectNoChanges()
        assertEquals(3, compositionCount)

        revalidate()
    }

    @Test
    fun computedStateOfStructuralMutationPolicyDoesNotRecompose() = compositionTest {
        var a by mutableIntStateOf(30)
        var b by mutableIntStateOf(10)
        val answer by computedStateOf(structuralEqualityPolicy()) { listOf(a >= 30, b >= 10) }
        var compositionCount = 0

        compose {
            val remembered = rememberUpdatedState(answer)
            Linear {
                compositionCount++
                Text("The answer is ${remembered.value}")
            }
        }

        validate { Linear { Text("The answer is ${listOf(true, true)}") } }

        assertEquals(1, compositionCount)

        a++
        expectNoChanges()
        assertEquals(1, compositionCount)

        b++
        expectNoChanges()
        assertEquals(1, compositionCount)

        revalidate()
    }

    @Test
    fun computedStateInvalidatesAfterUnchanged() = compositionTest {
        var a by mutableIntStateOf(10)
        var b by mutableIntStateOf(20)
        val sum by computedStateOf { a + b }
        var compositionCount = 0

        compose {
            compositionCount++
            Text("Sum: $sum")
        }

        validate { Text("Sum: ${a + b}") }
        assertEquals(1, compositionCount, "Should compose on initialization")

        a++
        b--
        expectNoChanges()
        revalidate()
        assertEquals(1, compositionCount, "Unexpected number of compositions")

        a++
        expectChanges()
        revalidate()
        assertEquals(2, compositionCount, "Unexpected number of compositions")
    }

    @Test
    fun computedStateInvalidatesTwice() = compositionTest {
        var a by mutableIntStateOf(1)
        val computedA by computedStateOf { a * 2 }
        var compositionCount = 0

        compose {
            compositionCount++
            Text("Value is $computedA")
        }

        validate { Text("Value is ${2 * a}") }
        assertEquals(1, compositionCount, "Unexpected number of compositions")

        a++
        expectChanges()
        revalidate()
        assertEquals(2, compositionCount, "Unexpected number of compositions")

        a++
        expectChanges()
        revalidate()
        assertEquals(3, compositionCount, "Unexpected number of compositions")
    }

    @Test
    fun derivedStateReadingComputedStateInComposition() = compositionTest {
        var a by mutableIntStateOf(10)
        var b by mutableIntStateOf(20)
        val computed = computedStateOf { a + b }
        var compositionCount = 0

        compose {
            val derived by remember { derivedStateOf { computed.value * 2 } }
            compositionCount++
            Text("Value is $derived")
        }

        validate { Text("Value is ${(a + b) * 2}") }
        assertEquals(1, compositionCount)

        a++
        expectChanges()
        revalidate()
        assertEquals(2, compositionCount)

        b++
        expectChanges()
        revalidate()
        assertEquals(3, compositionCount)
    }

    @Test
    fun computedStateReadingDerivedStateInComposition() = compositionTest {
        var a by mutableIntStateOf(10)
        var b by mutableIntStateOf(20)
        val derived = derivedStateOf { a + b }
        var compositionCount = 0

        compose {
            val computed by remember { computedStateOf { derived.value * 2 } }
            compositionCount++
            Text("Value is $computed")
        }

        validate { Text("Value is ${(a + b) * 2}") }
        assertEquals(1, compositionCount)

        a++
        expectChanges()
        revalidate()
        assertEquals(2, compositionCount)

        b++
        expectChanges()
        revalidate()
        assertEquals(3, compositionCount)
    }

    @Test
    fun computedStateReadingDerivedStateSkips() = compositionTest {
        var a by mutableIntStateOf(10)
        var b by mutableIntStateOf(20)
        val derived = derivedStateOf { a + b }
        var compositionCount = 0

        compose {
            val computed by remember { computedStateOf { derived.value > 0 } }
            compositionCount++
            Text("Is positive: $computed")
        }

        validate { Text("Is positive: ${a + b > 0}") }
        assertEquals(1, compositionCount)

        a++
        expectNoChanges()
        assertEquals(1, compositionCount)

        a = -100
        b = 0
        expectChanges()
        revalidate()
        assertEquals(2, compositionCount)
    }

    @Test
    fun derivedStateReadingComputedStateSkips() = compositionTest {
        var a by mutableIntStateOf(10)
        var b by mutableIntStateOf(20)
        val computed = computedStateOf { a + b }
        var compositionCount = 0

        compose {
            val derived by remember { derivedStateOf { computed.value > 0 } }
            compositionCount++
            Text("Is positive: $derived")
        }

        validate { Text("Is positive: ${a + b > 0}") }
        assertEquals(1, compositionCount)

        a++
        expectNoChanges()
        assertEquals(1, compositionCount)

        a = -100
        b = 0
        expectChanges()
        revalidate()
        assertEquals(2, compositionCount)
    }

    @Test
    fun computedStateChangesDependencies() = compositionTest {
        var a by mutableIntStateOf(10)
        var b by mutableIntStateOf(20)
        var c by mutableIntStateOf(30)
        var cond by mutableStateOf(true)
        val computed by computedStateOf { if (cond) a + b else c }
        var compositionCount = 0

        compose {
            compositionCount++
            Text("Value is $computed")
        }

        validate { Text("Value is ${if (cond) a + b else c}") }
        assertEquals(1, compositionCount)

        cond = false
        expectNoChanges()
        revalidate()
        assertEquals(1, compositionCount)

        a = 30
        expectNoChanges()
        revalidate()
        assertEquals(1, compositionCount)

        c = 0
        expectChanges()
        revalidate()
        assertEquals(2, compositionCount)

        cond = true
        a = -20
        expectNoChanges()
        revalidate()
        assertEquals(2, compositionCount)

        a = 0
        expectChanges()
        revalidate()
        assertEquals(3, compositionCount)
    }

    @Test
    fun computedStateChangesDependenciesBetweenDerivedAndDirectStates() = compositionTest {
        var a by mutableIntStateOf(10)
        var b by mutableIntStateOf(20)
        var c by mutableIntStateOf(30)
        var cond by mutableStateOf(true)
        val derived = derivedStateOf { a + b }
        val computed by computedStateOf { if (cond) derived.value else c }
        var compositionCount = 0

        compose {
            compositionCount++
            Text("Value is $computed")
        }

        validate { Text("Value is ${if (cond) a + b else c}") }
        assertEquals(1, compositionCount)

        cond = false
        expectNoChanges()
        revalidate()
        assertEquals(1, compositionCount)

        a = 30
        expectNoChanges()
        revalidate()
        assertEquals(1, compositionCount)

        c = 0
        expectChanges()
        revalidate()
        assertEquals(2, compositionCount)

        cond = true
        a = -20
        expectNoChanges()
        revalidate()
        assertEquals(2, compositionCount)

        a = 0
        expectChanges()
        revalidate()
        assertEquals(3, compositionCount)
    }

    @Test
    fun changingTheComputedStateInstanceShouldClearDependencies() = compositionTest {
        var reload by mutableIntStateOf(0)

        compose {
            val items = remember(reload) { computedStateOf { List(10) { it } } }

            Text("List of size ${items.value.size}")
        }

        validate { Text("List of size 10") }

        repeat(10) {
            reload++
            advance()
        }

        revalidate()

        // Validate there are only 2 observed object which should be `reload` and the last
        // created computedStateOf instance
        val observed = (composition as? CompositionImpl)?.observedObjects ?: emptyList()
        assertEquals(2, observed.count())
    }

    @Test
    fun changingComputedStateDependenciesShouldClearThem() = compositionTest {
        var reload by mutableIntStateOf(0)

        compose {
            val itemValue = remember(reload) { computedStateOf { 1 } }

            val intermediateState = rememberUpdatedState(itemValue)

            val snapshot = remember {
                computedStateOf { List(10) { intermediateState.value.value } }
            }

            Text("List of size ${snapshot.value.size}")
        }

        validate { Text("List of size 10") }

        repeat(10) {
            reload++
            advance()
        }

        revalidate()

        // Validate there are only 2 observed dependencies, one for intermediateState, one for
        // itemValue
        val observed = (composition as? CompositionImpl)?.derivedStateDependencies ?: emptyList()
        assertEquals(2, observed.count())
    }

    @Test
    fun computedStateOfMutationPolicyDoesNotInvalidateNestedStates() = compositionTest {
        var a by mutableIntStateOf(30)
        var b by mutableIntStateOf(10)
        val transient by computedStateOf(structuralEqualityPolicy()) { (a + b) / 10 }
        var invocationCount = 0
        val answer by computedStateOf {
            invocationCount++
            transient
        }

        compose { Text("The answer is $answer") }

        validate { Text("The answer is ${(a + b) / 10}") }

        assertEquals(1, invocationCount)

        a += 10
        expectChanges()
        assertEquals(3, invocationCount)

        b++
        expectNoChanges()
        assertEquals(3, invocationCount)

        revalidate()
    }

    @Test
    fun nestedComputedStateWithChangingState() = compositionTest {
        var item by mutableStateOf(ComputedNestedItem(10))

        val evenActiveItem: ComputedNestedItem? by
            computedStateOf(structuralEqualityPolicy()) {
                item.takeIf { it.active && (it.number % 2 == 0) }
            }

        val evenActiveNumber: Int? by
            computedStateOf(structuralEqualityPolicy()) { evenActiveItem?.number }

        compose { Text("evenActiveNumber = $evenActiveNumber") }

        validate { Text("evenActiveNumber = 10") }

        item.active = false
        expectChanges()
        validate { Text("evenActiveNumber = null") }

        item = item.copyWith(9)
        expectNoChanges()
        revalidate()

        item = item.copyWith(8)
        expectNoChanges()
        revalidate()

        item.active = true
        expectChanges()
        validate { Text("evenActiveNumber = 8") }
    }

    @Test
    fun ensureCalculateIsNotCalledTooSoon() = compositionTest {
        var a by mutableIntStateOf(11)
        var dCalculated = 0
        var dChanged = false
        val d =
            "d" to
                computedStateOf {
                    dCalculated++
                    a + 10
                }

        compose {
            Text("a = $a")
            val oldDCalculated = dCalculated
            Display(d)
            dChanged = oldDCalculated != dCalculated
        }

        validate {
            Text("a = $a")
            Text("d = ${a + 10}")
        }

        assertTrue(dChanged, "Expected d to recalculate")

        a++
        expectChanges()
        revalidate()
        assertTrue(dChanged, "Expected d to recalculate")
    }

    @Test
    fun derivedStateChangesDependenciesBetweenComputedAndDirectStates() = compositionTest {
        var a by mutableIntStateOf(10)
        var b by mutableIntStateOf(20)
        var c by mutableIntStateOf(30)
        var cond by mutableStateOf(true)
        val computed = computedStateOf { a + b }
        val derived by derivedStateOf { if (cond) computed.value else c }
        var compositionCount = 0

        compose {
            compositionCount++
            Text("Value is $derived")
        }

        validate { Text("Value is 30") }
        assertEquals(1, compositionCount)

        cond = false
        expectNoChanges()
        assertEquals(1, compositionCount)

        a = 30
        expectNoChanges()
        assertEquals(1, compositionCount)

        c = 0
        expectChanges()
        validate { Text("Value is 0") }
        assertEquals(2, compositionCount)

        Snapshot.withMutableSnapshot {
            cond = true
            a = -20
        }
        expectNoChanges()
        assertEquals(2, compositionCount)

        a = 0
        expectChanges()
        validate { Text("Value is 20") }
        assertEquals(3, compositionCount)
    }

    @Test
    fun readInterwovenComputedAndDerivedStates() = compositionTest {
        var state by mutableIntStateOf(1)
        // computed -> derived -> computed
        val c1 = computedStateOf { state * 2 }
        val d1 = derivedStateOf { c1.value * 3 }
        val c2 by computedStateOf { d1.value * 5 }
        var compositionCount = 0

        compose {
            compositionCount++
            Text("Value is $c2")
        }

        validate { Text("Value is 30") }
        assertEquals(1, compositionCount)

        state++
        expectChanges()
        validate { Text("Value is 60") }
        assertEquals(2, compositionCount)
    }
}

private class ComputedNestedItem(val number: Int) {
    var active by mutableStateOf(true)

    fun copyWith(number: Int): ComputedNestedItem =
        ComputedNestedItem(number).also { it.active = active }

    override fun toString(): String {
        return "ComputedNestedItem(number=$number, active=$active)"
    }
}

@Composable
private fun DisplayItem(name: String, state: State<Int>) {
    Text("$name = ${state.value}")
}

@Composable
private fun DisplayIndirect(name: String, state: State<Int>) {
    DisplayItem(name, state)
}

@Composable
private fun Display(vararg names: Pair<String, State<Int>>) {
    for ((name, state) in names) {
        DisplayIndirect(name, state)
    }
}
