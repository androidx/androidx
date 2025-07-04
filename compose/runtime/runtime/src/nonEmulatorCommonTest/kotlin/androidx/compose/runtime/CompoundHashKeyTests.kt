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

import androidx.collection.IntObjectMap
import androidx.collection.MutableIntObjectMap
import androidx.compose.runtime.mock.CompositionTestScope
import androidx.compose.runtime.mock.NonReusableText
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mock.expectNoChanges
import androidx.compose.runtime.mock.revalidate
import androidx.compose.runtime.mock.validate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CompoundHashKeyTests {
    @Test // b/157905524
    fun testWithSubCompose() = compositionTest {
        val outerKeys = mutableListOf<CompositeKeyHashCode>()
        val innerKeys = mutableListOf<CompositeKeyHashCode>()
        val invalidates = mutableListOf<RecomposeScope>()
        fun invalidateComposition() {
            invalidates.forEach { it.invalidate() }
            invalidates.clear()
        }
        @Composable
        fun recordHashKeys() {
            invalidates.add(currentRecomposeScope)
            outerKeys.add(currentCompositeKeyHashCode)
            TestSubcomposition {
                invalidates.add(currentRecomposeScope)
                innerKeys.add(currentCompositeKeyHashCode)
            }
        }

        val firstOuter = mutableListOf<CompositeKeyHashCode>()
        val firstInner = mutableListOf<CompositeKeyHashCode>()
        compose { (0..1).forEach { key(it) { recordHashKeys() } } }
        assertEquals(2, outerKeys.size)
        assertEquals(2, innerKeys.size)
        assertNotEquals(outerKeys[0], outerKeys[1])
        assertNotEquals(innerKeys[0], innerKeys[1])

        firstOuter.addAll(outerKeys)
        outerKeys.clear()
        firstInner.addAll(innerKeys)
        innerKeys.clear()
        invalidateComposition()

        expectNoChanges()

        assertEquals(firstInner, innerKeys)
        assertEquals(firstOuter, outerKeys)
    }

    @Test // b/195185633
    fun testEnumKeys() = compositionTest {
        val testClass = EnumTestClass()
        compose { testClass.Test() }

        val originalKey = testClass.currentKey
        testClass.scope.invalidate()
        advance()

        assertEquals(originalKey, testClass.currentKey)
    }

    @Test // b/263760668
    fun testReusableContentNodeKeys() = compositionTest {
        var keyOnEnter = CompositeKeyHashCode(-1)
        var keyOnExit = CompositeKeyHashCode(-1)

        var contentKey by mutableStateOf(0)

        compose {
            ReusableContent(contentKey) {
                keyOnEnter = currentCompositeKeyHashCode

                NonReusableText("$contentKey")

                keyOnExit = currentCompositeKeyHashCode
            }
        }

        assertEquals(keyOnEnter, keyOnExit)

        contentKey = 1
        advance()

        assertEquals(keyOnEnter, keyOnExit)
    }

    @Test // b/287537290
    fun adjacentCallsProduceUniqueKeys() = compositionTest {
        expectUniqueHashCodes {
            A()
            A()
        }
    }

    @Test // b/287537290
    fun indirectConditionalCallsProduceUniqueKeys() = compositionTest {
        expectUniqueHashCodes {
            C(condition = true)
            C(condition = true)
        }
    }

    @Test // b/287537290
    fun repeatedCallsProduceUniqueKeys() = compositionTest {
        expectUniqueHashCodes { repeat(10) { A() } }
    }

    @Test
    fun uniqueKeysGenerateUniqueCompositeKeys() = compositionTest {
        expectUniqueHashCodes {
            key(1) { A() }
            key(2) { A() }
        }
    }

    @Test
    fun duplicateKeysGenerateDuplicateCompositeKeys() = compositionTest {
        expectHashCodes(duplicateCount = 4) { listOf(1, 2, 1, 2, 1, 2).forEach { key(it) { A() } } }
    }

    @Test
    fun compositeKeysAreConsistentBetweenOnRecreate() = compositionTest {
        val state = mutableStateOf(true)
        var markers = expectUniqueHashCodes {
            C(state.value)
            C(!state.value)
        }
        repeat(4) {
            state.value = !state.value
            markers = retraceConsistentWith(markers)
        }
    }

    @Test // regression test for b/382670037
    fun nestedCompositeHashKeyIsConsistentOnRecompose() = compositionTest {
        lateinit var scope: RecomposeScope
        var lastRecordedHash = CompositeKeyHashCode(-1)

        compose {
            RestartGroup {}
            key(0) {
                RestartGroup {
                    scope = currentRecomposeScope
                    lastRecordedHash = currentCompositeKeyHashCode
                }
            }
        }

        validate {}
        val originalHash = lastRecordedHash
        lastRecordedHash = -1
        scope.invalidate()
        expectNoChanges()
        revalidate()
        assertEquals(originalHash, lastRecordedHash)
    }

    @Test
    fun compositeKeysMoveWithKeys() = compositionTest {
        val list = mutableStateListOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        var markers = expectUniqueHashCodes {
            for (item in list) {
                key(item) { A() }
            }
        }
        list.reverse()
        markers = retraceConsistentWith(markers)
        list.reverse()
        markers = retraceConsistentWith(markers)
        list.shuffle()
        markers = retraceConsistentWith(markers)
        list.shuffle()
        retraceConsistentWith(markers)
    }
}

private class EnumTestClass {
    var currentKey = EmptyCompositeKeyHashCode
    lateinit var scope: RecomposeScope
    val state = mutableStateOf(0)
    private val config = mutableStateOf(Config.A)

    @Composable
    fun Test() {
        key(config.value) { Child() }
    }

    @Composable
    private fun Child() {
        scope = currentRecomposeScope
        currentKey = currentCompositeKeyHashCode
    }

    enum class Config {
        A,
        B,
    }
}

private var hashTraceRecomposeState = mutableStateOf(0)
private var hashTrace = mutableListOf<CompositeKeyHashCode>()
private var markerToHash = MutableIntObjectMap<CompositeKeyHashCode>()

private var marker = 100

private fun newMarker() = marker++

private data class TraceResult(
    val trace: List<CompositeKeyHashCode>,
    val markers: MutableIntObjectMap<CompositeKeyHashCode>,
)

private fun CompositionTestScope.composeTrace(content: @Composable () -> Unit): TraceResult {
    hashTrace = mutableListOf()
    markerToHash = MutableIntObjectMap()
    compose(content)
    val result = TraceResult(hashTrace, markerToHash)
    hashTrace = mutableListOf()
    markerToHash = MutableIntObjectMap()
    return result
}

private fun CompositionTestScope.retrace(): TraceResult {
    hashTraceRecomposeState.value++
    hashTrace = mutableListOf()
    markerToHash = MutableIntObjectMap()
    advance()
    val result = TraceResult(hashTrace, markerToHash)
    hashTrace = mutableListOf()
    markerToHash = MutableIntObjectMap()
    return result
}

private fun CompositionTestScope.retraceConsistentWith(
    markers: IntObjectMap<CompositeKeyHashCode>
): IntObjectMap<CompositeKeyHashCode> {
    val recomposeMarkers = retrace().markers
    return markers.mergedWith(recomposeMarkers) { key, existing, merged ->
        error("Inconsistent marker $key expected hash $existing but found $merged")
    }
}

private fun CompositionTestScope.expectUniqueHashCodes(content: @Composable () -> Unit) =
    expectHashCodes(duplicateCount = 0, content)

private fun CompositionTestScope.expectHashCodes(
    duplicateCount: Int,
    content: @Composable () -> Unit,
): IntObjectMap<CompositeKeyHashCode> {
    val (hashCodes, markers) = composeTrace(content)
    val uniqueCodes = hashCodes.distinct()
    assertEquals(
        hashCodes.size,
        uniqueCodes.size + duplicateCount,
        if (duplicateCount == 0)
            "Non-unique codes detected. " +
                "Count of unique hash codes doesn't match the number of codes collected"
        else
            "Expected $duplicateCount keys but found but found ${
            hashCodes.size - uniqueCodes.size
        }",
    )
    val (recomposeTrace, recomposeMarkers) = retrace()
    assertEquals(hashCodes.size, recomposeTrace.size)
    hashCodes.forEachIndexed { index, code ->
        assertEquals(code, recomposeTrace[index], "Unexpected hash code at $index")
    }

    return markers.mergedWith(recomposeMarkers) { key, existing, merged ->
        error("Inconsistent marker $key expected hash $existing but found $merged")
    }
}

private fun IntObjectMap<CompositeKeyHashCode>.mergedWith(
    other: IntObjectMap<CompositeKeyHashCode>,
    inconsistent:
        (
            key: Int, existingValue: CompositeKeyHashCode, mergedValue: CompositeKeyHashCode,
        ) -> CompositeKeyHashCode,
): IntObjectMap<CompositeKeyHashCode> {
    val result = MutableIntObjectMap<CompositeKeyHashCode>()
    forEach { key, value ->
        var mergedValue = value
        if (key in other) {
            val otherValue = other[key] ?: EmptyCompositeKeyHashCode
            if (otherValue != value) mergedValue = inconsistent(key, value, otherValue)
        }
        result[key] = mergedValue
    }
    other.forEach { key, value ->
        if (key !in this) {
            result[key] = value
        }
    }
    return result
}

@Composable
private fun A(marker: Int = remember { newMarker() }) {
    hashTraceRecomposeState.value
    val hash = currentCompositeKeyHashCode
    hashTrace.add(hash)
    markerToHash[marker] = hash
}

@Composable
private fun B() {
    A()
    A()
    A()
}

@Composable
private fun C(condition: Boolean) {
    val one = remember { newMarker() }
    val two = remember { newMarker() }
    val three = remember { newMarker() }
    if (condition) {
        A(one)
        A(two)
        A(three)
    }
}
