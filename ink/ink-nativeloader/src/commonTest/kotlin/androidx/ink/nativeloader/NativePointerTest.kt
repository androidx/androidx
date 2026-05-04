/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.ink.nativeloader

import androidx.ink.nativeloader.testing.awaitNativePointerCleanupAfter
import androidx.kruth.assertThat
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.coroutines.TimeoutCancellationException

class NativePointerTest {

    @Test
    fun awaitNativePointerCleanupAfter_failsIfBlockCompletesButNoPointersAllocated() {
        val ex = assertFailsWith<IllegalStateException> { awaitNativePointerCleanupAfter {} }
        assertThat(ex).hasMessageThat().contains("No pointers were allocated.")
    }

    @Test
    fun awaitNativePointerCleanupAfter_propagatesExceptionFromBlock() {
        val ex =
            assertFailsWith<RuntimeException> {
                awaitNativePointerCleanupAfter { throw RuntimeException("Failure") }
            }
        assertThat(ex).hasMessageThat().contains("Failure")
    }

    @Test
    fun awaitNativePointerCleanupAfter_throwsIfSamePointerAllocatedTwice() {
        // This guards against a condition where some of the other consistency checking wouldn't
        // work. Allocated pointers being unique is a constraint on the consumer of [NativePointer].
        // If we wind up needing to deal with the same address being freed and reused in the code
        // under
        // test, we'll need to be more sophisticated about this in awaitNativePointerCleanupAfter.
        val ex =
            assertFailsWith<IllegalStateException> {
                awaitNativePointerCleanupAfter {
                    val unused = NativePointer({ 1L }, {})
                    val unused2 = NativePointer({ 1L }, {})
                }
            }
        assertThat(ex).hasMessageThat().contains("The same pointer (1) was allocated twice.")
    }

    @Test
    fun awaitNativePointerCleanupAfter_throwsIfPointerCleanedUpWithoutAlloc() {
        // NativePointer should enforce that this condition is not met, but we can test that
        // awaitNativePointerCleanupAfter guards against it by manipulating the observer
        // directly.
        val ex =
            assertFailsWith<IllegalStateException> {
                awaitNativePointerCleanupAfter { NativePointerObserver.onCleanup?.invoke(1L) }
            }
        assertThat(ex)
            .hasMessageThat()
            .contains("Attempted to clean up pointer (1) which was not allocated.")
    }

    @Test
    fun awaitNativePointerCleanupAfter_throwsIfSamePointerCleanedUpTwice() {
        // NativePointer should enforce that this condition is not met, but we can test that
        // awaitNativePointerCleanupAfter guards against it by manipulating the observer
        // directly.
        val ex =
            assertFailsWith<IllegalStateException> {
                awaitNativePointerCleanupAfter {
                    NativePointerObserver.onAlloc?.invoke(1L)
                    NativePointerObserver.onCleanup?.invoke(1L)
                    NativePointerObserver.onCleanup?.invoke(1L)
                }
            }
        assertThat(ex).hasMessageThat().contains("The same pointer (1) was cleaned up twice.")
    }

    @Test
    fun awaitNativePointerCleanupAfter_throwsIfNativePointersStillReferenced() {
        val freedPointers = mutableListOf<Long>()
        // Because of this reference, NativePointer does not go out of scope, so it's not cleaned
        // up.
        var pointer: NativePointer? = null
        assertFailsWith<TimeoutCancellationException> {
            awaitNativePointerCleanupAfter {
                pointer = NativePointer({ 1L }, { freedPointers.add(it) })
            }
        }
        assertThat(pointer).isNotNull()
        assertThat(freedPointers).isEmpty()
    }

    @Test
    fun nativePointer_isCleanedUpAfterGoingOutOfScope() {
        val freedPointers = mutableListOf<Long>()
        awaitNativePointerCleanupAfter {
            val unused = NativePointer({ 1L }, { freedPointers.add(it) })
        }
        assertThat(freedPointers).containsExactly(1L)
    }

    @Test
    fun nativePointer_throwsIfAllocFailed() {
        val freedPointers = mutableListOf<Long>()
        val ex =
            assertFailsWith<IllegalStateException> {
                awaitNativePointerCleanupAfter {
                    val unused =
                        NativePointer(
                            // The native nullptr, which must result in an exception being thrown
                            // (generally we
                            // expect something more meaningful to be thrown at the JVM/C interface
                            // first).
                            { 0L },
                            // The free callback doesn't actually get called unless the pointer is
                            // non-zero.
                            { freedPointers.add(it) },
                        )
                }
            }
        assertThat(ex)
            .hasMessageThat()
            .contains(
                "Native allocation failed, should throw an exception in pointerAlloc in that case."
            )
        assertThat(freedPointers).isEmpty()
    }

    @Test
    fun nativePointer_setsUpCleanupBeforeAllocating() {
        val pointerCleanupsObserved = mutableListOf<Long>()
        val freedPointers = mutableListOf<Long>()
        val ex =
            assertFailsWith<RuntimeException> {
                awaitNativePointerCleanupAfter(onCleanup = { pointerCleanupsObserved.add(it) }) {
                    val unused =
                        NativePointer(
                            { throw RuntimeException("Failure during allocation") },
                            // The free callback doesn't actually get called unless the pointer is
                            // non-zero.
                            { freedPointers.add(it) },
                        )
                }
            }
        assertThat(ex).hasMessageThat().contains("Failure during allocation")
        // Cleanup happens because cleanup is set up before alloc is attempted.
        assertThat(pointerCleanupsObserved).containsExactly(0L)
        // pointerFree callback is not called, that's only supposed to be called if alloc succeeds.
        assertThat(freedPointers).isEmpty()
    }
}
