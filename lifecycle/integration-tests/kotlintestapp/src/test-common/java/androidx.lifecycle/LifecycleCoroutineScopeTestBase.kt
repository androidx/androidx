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

package androidx.lifecycle

import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.junit.Test
import java.util.concurrent.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
abstract class LifecycleCoroutineScopeTestBase {
    @Test
    fun initialization() {
        val owner = TestLifecycleOwner(Lifecycle.State.INITIALIZED, TestCoroutineDispatcher())
        val scope = owner.lifecycleScope
        assertThat(owner.lifecycle.mInternalScopeRef.get()).isSameInstanceAs(scope)
        val scope2 = owner.lifecycleScope
        assertThat(scope).isSameInstanceAs(scope2)
        runBlocking(Dispatchers.Main) {
            assertThat(owner.observerCount).isEqualTo(1)
        }
    }

    @Test
    fun simpleLaunch() {
        val owner = TestLifecycleOwner(Lifecycle.State.INITIALIZED, TestCoroutineDispatcher())
        assertThat(
            runBlocking {
                owner.lifecycleScope.async {
                    // do nothing
                    true
                }.await()
            }
        ).isTrue()
    }

    @Test
    fun launchAfterDestroy() {
        val owner = TestLifecycleOwner(Lifecycle.State.CREATED, TestCoroutineDispatcher())
        owner.lifecycle.currentState = Lifecycle.State.DESTROYED
        runBlocking {
            owner.lifecycleScope.launch {
                // do nothing
                throw AssertionError("should not run")
            }.join()
        }
    }

    @Test
    fun launchOnMain() {
        val owner = TestLifecycleOwner(Lifecycle.State.STARTED, TestCoroutineDispatcher())
        assertThat(
            runBlocking(Dispatchers.Main) {
                owner.lifecycleScope.async {
                    true
                }.await()
            }
        ).isTrue()
    }

    @Test
    fun launchOnIO() {
        val owner = TestLifecycleOwner(Lifecycle.State.STARTED, TestCoroutineDispatcher())
        assertThat(
            runBlocking(Dispatchers.IO) {
                owner.lifecycleScope.async {
                    true
                }.await()
            }
        ).isTrue()
    }

    @Test
    fun destroyWhileRunning() {
        val startMutex = Mutex(locked = true)
        val alwaysLocked = Mutex(locked = true)
        val owner = TestLifecycleOwner(Lifecycle.State.STARTED, TestCoroutineDispatcher())
        val actionWasActive = owner.lifecycleScope.async(Dispatchers.IO) {
            startMutex.unlock()
            alwaysLocked.lock() // wait 4ever
        }
        runBlocking(Dispatchers.Main) {
            startMutex.lock() // wait until it starts
            owner.currentState = Lifecycle.State.DESTROYED
            actionWasActive.join()
            assertThat(actionWasActive.isCancelled).isTrue()
        }
    }

    @Test
    fun throwException() {
        val owner = TestLifecycleOwner(Lifecycle.State.STARTED, TestCoroutineDispatcher())
        runBlocking {
            val action = owner.lifecycleScope.async {
                throw RuntimeException("foo")
            }
            action.join()
            assertThat(action.getCompletionExceptionOrNull()).hasMessageThat().isSameInstanceAs(
                "foo"
            )
        }
    }

    @Test
    fun throwException_onStart() {
        val owner = TestLifecycleOwner(Lifecycle.State.CREATED, TestCoroutineDispatcher())
        runBlocking {
            // TODO guarantee later execution
            val action = owner.lifecycleScope.async {
                throw RuntimeException("foo")
            }
            withContext(Dispatchers.Main) {
                owner.currentState = Lifecycle.State.STARTED
            }
            action.join()
            assertThat(action.getCompletionExceptionOrNull()).hasMessageThat().isSameInstanceAs(
                "foo"
            )
        }
    }

    @Test
    fun runAnotherAfterCancellation_cancelOutside() {
        val owner = TestLifecycleOwner(Lifecycle.State.STARTED, TestCoroutineDispatcher())
        runBlocking {
            val action = owner.lifecycleScope.async {
                delay(20000)
            }
            action.cancel()
            action.join()
        }
        assertThat(
            runBlocking {
                owner.lifecycleScope.async {
                    true
                }.await()
            }
        ).isTrue()
    }

    @Test
    fun runAnotherAfterCancellation_cancelInside() {
        val owner = TestLifecycleOwner(Lifecycle.State.STARTED, TestCoroutineDispatcher())
        runBlocking {
            val action = owner.lifecycleScope.async {
                throw CancellationException("")
            }
            action.join()
        }
        assertThat(
            runBlocking {
                owner.lifecycleScope.async {
                    true
                }.await()
            }
        ).isTrue()
    }

    @Test
    fun runAnotherAfterFailure() {
        val owner = TestLifecycleOwner(Lifecycle.State.STARTED, TestCoroutineDispatcher())
        runBlocking {
            val action = owner.lifecycleScope.async {
                throw IllegalArgumentException("why not ?")
            }
            val result = kotlin.runCatching {
                action.await()
            }
            assertThat(result.exceptionOrNull())
                .isInstanceOf(IllegalArgumentException::class.java)
        }
        assertThat(
            runBlocking {
                owner.lifecycleScope.async {
                    true
                }.await()
            }
        ).isTrue()
    }
}
