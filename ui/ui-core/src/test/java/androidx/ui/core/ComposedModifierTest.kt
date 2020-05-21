/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.core

import androidx.compose.Applier
import androidx.compose.ApplyAdapter
import androidx.compose.Composable
import androidx.compose.Composer
import androidx.compose.CompositionFrameClock
import androidx.compose.InternalComposeApi
import androidx.compose.Recomposer
import androidx.compose.SlotTable
import androidx.compose.currentComposer
import androidx.compose.invalidate
import androidx.compose.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class TestTagModifier<T>(val name: String, val value: T) : Modifier.Element

fun <T> Modifier.testTag(name: String, value: T) = this + TestTagModifier(name, value)

fun <T> Modifier.getTestTag(name: String, default: T): T = foldIn(default) { acc, element ->
    @Suppress("UNCHECKED_CAST")
    if (element is TestTagModifier<*> && element.name == name) element.value as T else acc
}

class ComposedModifierTest {

    private val composer: Composer<*> get() = error("should not be called")

    /**
     * Confirm that a [composed] modifier correctly constructs separate instances when materialized
     */
    @Test
    fun materializeComposedModifier() {
        // Note: assumes single-threaded composition
        var counter = 0
        val sourceMod = Modifier.testTag("static", 0)
            .composed { testTag("dynamic", ++counter) }

        lateinit var firstMaterialized: Modifier
        lateinit var secondMaterialized: Modifier
        compose {
            firstMaterialized = currentComposer.materialize(sourceMod)
            secondMaterialized = currentComposer.materialize(sourceMod)
        }

        assertNotEquals("I recomposed some modifiers", 0, counter)

        assertEquals("first static value equal to source",
            sourceMod.getTestTag("static", Int.MIN_VALUE),
            firstMaterialized.getTestTag("static", Int.MAX_VALUE)
        )
        assertEquals("second static value equal to source",
            sourceMod.getTestTag("static", Int.MIN_VALUE),
            secondMaterialized.getTestTag("static", Int.MAX_VALUE)
        )
        assertEquals(
            "dynamic value not present in source",
            Int.MIN_VALUE,
            sourceMod.getTestTag("dynamic", Int.MIN_VALUE)
        )
        assertNotEquals(
            "dynamic value present in first materialized",
            Int.MIN_VALUE,
            firstMaterialized.getTestTag("dynamic", Int.MIN_VALUE)
        )
        assertNotEquals(
            "dynamic value present in second materialized",
            Int.MIN_VALUE,
            firstMaterialized.getTestTag("dynamic", Int.MIN_VALUE)
        )
        assertNotEquals(
            "first and second dynamic values must be unequal",
            firstMaterialized.getTestTag("dynamic", Int.MIN_VALUE),
            secondMaterialized.getTestTag("dynamic", Int.MIN_VALUE)
        )
    }

    /**
     * Confirm that recomposition occurs on invalidation
     */
    @Test
    fun recomposeComposedModifier() {
        // Manually invalidate the composition of the modifier instead of using mutableStateOf
        // Frame-based recomposition requires the FrameManager be up and running.
        var value = 0
        lateinit var invalidator: () -> Unit

        val sourceMod = Modifier.composed {
            invalidator = invalidate
            testTag("changing", value)
        }

        lateinit var materialized: Modifier
        val composer = compose {
            materialized = currentComposer.materialize(sourceMod)
        }

        assertEquals(
            "initial composition value",
            0,
            materialized.getTestTag("changing", Int.MIN_VALUE)
        )

        value = 5
        invalidator()
        composer.recompose()
        composer.applyChanges()

        assertEquals(
            "recomposed composition value",
            5,
            materialized.getTestTag("changing", Int.MIN_VALUE)
        )
    }

    @Test
    fun rememberComposedModifier() {
        lateinit var invalidator: () -> Unit
        val sourceMod = Modifier.composed {
            invalidator = invalidate
            val state = remember { Any() }
            testTag("remembered", state)
        }

        val results = mutableListOf<Any?>()
        val notFound = Any()
        val composer = compose {
            results.add(currentComposer.materialize(sourceMod).getTestTag("remembered", notFound))
        }

        assertTrue("one item added for initial composition", results.size == 1)
        assertNotNull("remembered object not null", results[0])

        invalidator()
        composer.recompose()
        composer.applyChanges()

        assertEquals("two items added after recomposition", 2, results.size)
        assertTrue("no null items", results.none { it === notFound })
        assertEquals("remembered references are equal", results[0], results[1])
    }

    @Test
    fun nestedComposedModifiers() {
        val mod = Modifier.composed {
            composed {
                testTag("nested", 10)
            }
        }

        lateinit var materialized: Modifier
        compose {
            materialized = currentComposer.materialize(mod)
        }

        assertEquals(
            "fully unwrapped composed modifier value",
            10,
            materialized.getTestTag("nested", 0)
        )
    }
}

private fun compose(
    block: @Composable () -> Unit
): Composer<Unit> = UnitComposer().apply {
    compose(block)
    applyChanges()
    slotTable.verifyWellFormed()
}

/**
 * This ApplyAdapter does nothing. These tests only confirm modifier materialization.
 */
private object UnitApplierAdapter : ApplyAdapter<Unit> {
    override fun Unit.start(instance: Unit) {}
    override fun Unit.insertAt(index: Int, instance: Unit) {}
    override fun Unit.removeAt(index: Int, count: Int) {}
    override fun Unit.move(from: Int, to: Int, count: Int) {}
    override fun Unit.end(instance: Unit, parent: Unit) {}
}

@OptIn(InternalComposeApi::class)
private object NoOpSchedulingRecomposer : Recomposer() {
    override fun hasPendingChanges(): Boolean = false
    override fun recomposeSync() {}
    override fun scheduleChangesDispatch() {}
    override val compositionFrameClock = object : CompositionFrameClock {
        override suspend fun <R> awaitFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            error("awaitFrameNanos not supported for this test")
        }
    }
    override val effectCoroutineScope = CoroutineScope(SupervisorJob())
}

private class UnitComposer : Composer<Unit>(
    SlotTable(),
    Applier(Unit, UnitApplierAdapter),
    NoOpSchedulingRecomposer
) {
    fun compose(composable: @Composable () -> Unit) {
        composeRoot {
            @Suppress("UNCHECKED_CAST")
            val fn = composable as (Composer<*>, Int, Int) -> Unit
            fn(this@UnitComposer, 0, 0)
        }
    }
}