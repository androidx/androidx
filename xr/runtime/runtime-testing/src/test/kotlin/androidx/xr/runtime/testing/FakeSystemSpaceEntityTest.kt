/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.runtime.testing

import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FakeSystemSpaceEntityTest {
    private lateinit var underTest: FakeSystemSpaceEntity

    @Before
    fun Setup() {
        underTest = FakeSystemSpaceEntity()
    }

    @Test
    fun setOnSpaceUpdatedListener_callListenersOnActivitySpaceUpdated() {
        val listener = TestSpaceUpdatedListener()
        val executor = DirectExecutor()

        underTest.setOnSpaceUpdatedListener(listener, executor)
        underTest.onSpaceUpdated()

        assertThat(listener.wasCalled).isTrue()
    }

    @Test
    fun setOnSpaceUpdatedListener_multipleListeners_callLastListenersOnActivitySpaceUpdated() {
        val listener1 = TestSpaceUpdatedListener()
        val listener2 = TestSpaceUpdatedListener()
        val executor1 = DirectExecutor()
        val executor2 = DirectExecutor()

        underTest.setOnSpaceUpdatedListener(listener1, executor1)
        // This should override the previous listener.
        underTest.setOnSpaceUpdatedListener(listener2, executor2)
        underTest.onSpaceUpdated()

        assertThat(listener1.wasCalled).isFalse()
        assertThat(listener2.wasCalled).isTrue()
    }

    @Test
    fun setOnSpaceUpdatedListener_withNullExecutor_usesInternalExecutor() {
        val listener = TestSpaceUpdatedListener()

        underTest.setOnSpaceUpdatedListener(listener, null)
        underTest.onSpaceUpdated()

        assertThat(listener.wasCalled).isTrue()
    }

    @Test
    fun setOnSpaceUpdatedListener_withNullListener_noListenerCallOnActivitySpaceUpdated() {
        val listener = TestSpaceUpdatedListener()
        val executor = DirectExecutor()
        underTest.setOnSpaceUpdatedListener(listener, executor)
        underTest.setOnSpaceUpdatedListener(null, executor)

        underTest.onSpaceUpdated()

        assertThat(listener.wasCalled).isFalse()
    }

    private class TestSpaceUpdatedListener(
        private val throwExceptionOnUpdate: Boolean = false,
        private val exceptionToThrow: RuntimeException =
            RuntimeException("Test exception from listener"),
    ) : Runnable {
        var wasCalled = false
            private set

        var callCount = 0
            private set

        override fun run() {
            wasCalled = true
            callCount++
            if (throwExceptionOnUpdate) {
                throw exceptionToThrow
            }
        }

        fun reset() {
            wasCalled = false
            callCount = 0
        }
    }

    private class DirectExecutor : Executor {
        override fun execute(r: Runnable) {
            r.run()
        }
    }
}
