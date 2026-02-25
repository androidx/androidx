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

package androidx.xr.scenecore.testing

import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class FakeSystemSpaceEntityTest {
    private lateinit var underTest: FakeSystemSpaceEntity

    @Before
    fun Setup() {
        underTest = FakeSystemSpaceEntity()
    }

    @Test
    fun setOnOriginChangedListener_callListenersOnActivitySpace() {
        val listener = TestSpaceUpdatedListener()
        val executor = DirectExecutor()

        underTest.setOnOriginChangedListener(listener, executor)
        underTest.onOriginChanged()

        assertThat(listener.wasCalled).isTrue()
    }

    @Test
    fun setOnOriginChangedListener_multipleListeners_callLastListenersOnActivitySpace() {
        val listener1 = TestSpaceUpdatedListener()
        val listener2 = TestSpaceUpdatedListener()
        val executor1 = DirectExecutor()
        val executor2 = DirectExecutor()

        underTest.setOnOriginChangedListener(listener1, executor1)
        // This should override the previous listener.
        underTest.setOnOriginChangedListener(listener2, executor2)
        underTest.onOriginChanged()

        assertThat(listener1.wasCalled).isFalse()
        assertThat(listener2.wasCalled).isTrue()
    }

    @Test
    fun setOnOriginChangedListener_withNullExecutor_usesInternalExecutor() {
        val listener = TestSpaceUpdatedListener()

        underTest.setOnOriginChangedListener(listener, null)
        underTest.onOriginChanged()

        assertThat(listener.wasCalled).isTrue()
    }

    @Test
    fun setOnOriginChangedListener_withNullListener_noListenerCallOnActivitySpace() {
        val listener = TestSpaceUpdatedListener()
        val executor = DirectExecutor()
        underTest.setOnOriginChangedListener(listener, executor)
        underTest.setOnOriginChangedListener(null, executor)

        underTest.onOriginChanged()

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
