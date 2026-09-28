/*
 * Copyright 2019 The Android Open Source Project
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

@file:OptIn(InternalComposeApi::class)

package androidx.compose.runtime

import androidx.compose.runtime.internal.IntRef
import androidx.compose.runtime.mock.EmptyApplier
import androidx.compose.runtime.mock.Text
import androidx.compose.runtime.mock.View
import androidx.compose.runtime.mock.ViewApplier
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mock.expectChanges
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import java.lang.ref.WeakReference
import kotlin.concurrent.thread
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@Stable
@OptIn(InternalComposeApi::class)
@Suppress("unused")
class JvmCompositionTests {
    // Regression test for b/202967533
    // Test taken from the bug report; reformatted to conform to lint rules.
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun avoidsDeadlockInRecomposerComposerDispose() =
        runTest(UnconfinedTestDispatcher()) {
            val thread = thread {
                while (!Thread.interrupted()) {
                    // -> synchronized(stateLock) -> recordComposerModificationsLocked
                    // -> composition.recordModificationsOf -> synchronized(lock)
                    Snapshot.sendApplyNotifications()
                }
            }

            for (i in 1..1000) {
                localRecomposerTest {
                    @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE") var value by mutableStateOf(0)
                    val snapshotObserver = SnapshotStateObserver {}
                    snapshotObserver.start()
                    @Suppress("UNUSED_VALUE")
                    value = 4
                    val composition = Composition(EmptyApplier(), it)
                    composition.setContent {}

                    // -> synchronized(lock) -> parent.unregisterComposition(this)
                    // -> synchronized(stateLock)
                    composition.dispose()
                    snapshotObserver.stop()
                }
            }

            thread.interrupt()
        }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun avoidRaceConditionWhenInvalidating() = compositionTest {
        var scope: RecomposeScope? = null
        var count = 0
        var threadException: Exception? = null
        val thread = thread {
            try {
                while (!Thread.interrupted()) {
                    scope?.invalidate()
                    count++
                }
            } catch (e: Exception) {
                threadException = e
            }
        }

        compose {
            scope = currentRecomposeScope
            Text("Some text")
            Text("Count $count")
        }

        repeat(20) {
            advance(ignorePendingWork = true)
            delay(1)
        }

        thread.interrupt()
        @Suppress("BlockingMethodInNonBlockingContext") thread.join()
        delay(10)
        threadException?.let { throw it }
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun avoidRaceConditionWhenApplyingSnapshotsInAThread() = compositionTest {
        val count = mutableStateOf(0)
        var threadException: Exception? = null

        compose {
            Text("Some text")
            Text("Count ${count.value}")
        }

        val thread = thread {
            try {
                while (!Thread.interrupted()) {
                    Snapshot.withMutableSnapshot { count.value++ }
                }
            } catch (e: Exception) {
                threadException = e
            }
        }

        repeat(200) {
            advance(ignorePendingWork = true)
            delay(1)
        }

        thread.interrupt()
        @Suppress("BlockingMethodInNonBlockingContext") thread.join()
        delay(10)
        threadException?.let { throw it }
    }

    @Test // b/197064250 and others
    fun canInvalidateDuringApplyChanges() = compositionTest {
        var value by mutableStateOf(0)
        compose {
            Wrap {
                val scope = currentRecomposeScope
                ComposeNode<View, ViewApplier>(
                    factory = { View().also { it.name = "linear" } },
                    update = {
                        set(value) {
                            scope.invalidate()
                            this.attributes["value"] = value.toString()
                        }
                    },
                )
            }
        }

        value = 2
        expectChanges()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test // Regression test for https://issuetracker.google.com/262356264
    fun compositionDoesNotRetainSnapshotReference() = runTest {
        localRecomposerTest { recomposer ->
            var compositionSnapshot: WeakReference<Snapshot>? = null
            val composition = Composition(UnitApplier(), recomposer)
            composition.setContent { compositionSnapshot = WeakReference(Snapshot.current) }
            assertNotNull(compositionSnapshot, "compositionSnapshot weak reference")
            repeat(10) { Runtime.getRuntime().gc() }
            assertNull(compositionSnapshot?.get(), "weak snapshot reference after forced gc")
        }
    }

    private var count = 0

    @BeforeTest
    fun saveSnapshotCount() {
        count = Snapshot.openSnapshotCount()
    }

    @AfterTest
    fun checkSnapshotCount() {
        val afterCount = Snapshot.openSnapshotCount()
        assertEquals(count, afterCount, "A snapshot was left open after the test")
    }

    // The following five test cases ensure that recomposition of a `@Composable` function whose
    // arguments are unchanged is skipped, in particular when one of the arguments is [Float.NaN]
    // or [Double.NaN].
    //
    // They serve as regression tests against https://issuetracker.google.com/issues/564569402.

    @Test
    fun floatNaNArgumentSkips() = compositionTest {
        var parentRecompositionTrigger by mutableStateOf(false)
        var parentCompositionCount = 0
        val childCompositionCount = IntRef()

        compose {
            // [parentRecompositionTrigger] is read here so that changes to it invalidate this
            // scope.
            parentRecompositionTrigger
            parentCompositionCount++
            WithFloatParameter(forceNonStatic(Float.NaN), childCompositionCount)
        }

        assertEquals(1, parentCompositionCount)
        assertEquals(1, childCompositionCount.element)

        parentRecompositionTrigger = true
        advance()

        assertEquals(2, parentCompositionCount)
        assertEquals(1, childCompositionCount.element)
    }

    @Test
    fun doubleNaNArgumentSkips() = compositionTest {
        var parentRecompositionTrigger by mutableStateOf(false)
        var parentCompositionCount = 0
        val childCompositionCount = IntRef()

        compose {
            // [parentRecompositionTrigger] is read here so that changes to it invalidate this
            // scope.
            parentRecompositionTrigger
            parentCompositionCount++
            WithDoubleParameter(forceNonStatic(Double.NaN), childCompositionCount)
        }

        assertEquals(1, parentCompositionCount)
        assertEquals(1, childCompositionCount.element)

        parentRecompositionTrigger = true
        advance()

        assertEquals(2, parentCompositionCount)
        assertEquals(1, childCompositionCount.element)
    }

    @Test
    fun dpOffsetUnspecifiedArgumentSkips() = compositionTest {
        var parentRecompositionTrigger by mutableStateOf(false)
        var parentCompositionCount = 0
        val childCompositionCount = IntRef()

        compose {
            // [parentRecompositionTrigger] is read here so that changes to it invalidate this
            // scope.
            parentRecompositionTrigger
            parentCompositionCount++
            WithDpOffsetParameter(forceNonStatic(DpOffset.Unspecified), childCompositionCount)
        }

        assertEquals(1, parentCompositionCount)
        assertEquals(1, childCompositionCount.element)

        parentRecompositionTrigger = true
        advance()

        assertEquals(2, parentCompositionCount)
        assertEquals(1, childCompositionCount.element)
    }

    @Test
    fun dpSizeUnspecifiedArgumentSkips() = compositionTest {
        var parentRecompositionTrigger by mutableStateOf(false)
        var parentCompositionCount = 0
        val childCompositionCount = IntRef()

        compose {
            // [parentRecompositionTrigger] is read here so that changes to it invalidate this
            // scope.
            parentRecompositionTrigger
            parentCompositionCount++
            WithDpSizeParameter(forceNonStatic(DpSize.Unspecified), childCompositionCount)
        }

        assertEquals(1, parentCompositionCount)
        assertEquals(1, childCompositionCount.element)

        parentRecompositionTrigger = true
        advance()

        assertEquals(2, parentCompositionCount)
        assertEquals(1, childCompositionCount.element)
    }

    @Test
    fun dpUnspecifiedArgumentSkips() = compositionTest {
        var parentRecompositionTrigger by mutableStateOf(false)
        var parentCompositionCount = 0
        val childCompositionCount = IntRef()

        compose {
            // [parentRecompositionTrigger] is read here so that changes to it invalidate this
            // scope.
            parentRecompositionTrigger
            parentCompositionCount++
            WithDpParameter(forceNonStatic(Dp.Unspecified), childCompositionCount)
        }

        assertEquals(1, parentCompositionCount)
        assertEquals(1, childCompositionCount.element)

        parentRecompositionTrigger = true
        advance()

        assertEquals(2, parentCompositionCount)
        assertEquals(1, childCompositionCount.element)
    }
}

/**
 * Returns [value] unchanged, but ensures that the compiler can't prove that the result is static.
 *
 * An argument can be wrapped with this to ensure that at runtime, `Composer.changed` will be used
 * to determine whether the argument has changed.
 */
private fun <T> forceNonStatic(value: T): T = value

private fun <T> Use(@Suppress("UNUSED_PARAMETER") value: T) {}

@Composable
private fun WithFloatParameter(value: Float, compositionCount: IntRef) {
    compositionCount.element++
    Use(value)
}

@Composable
private fun WithDoubleParameter(value: Double, compositionCount: IntRef) {
    compositionCount.element++
    Use(value)
}

@Composable
private fun WithDpParameter(value: Dp, compositionCount: IntRef) {
    compositionCount.element++
    Use(value)
}

@Composable
private fun WithDpOffsetParameter(value: DpOffset, compositionCount: IntRef) {
    compositionCount.element++
    Use(value)
}

@Composable
private fun WithDpSizeParameter(value: DpSize, compositionCount: IntRef) {
    compositionCount.element++
    Use(value)
}
