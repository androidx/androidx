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

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.junit.Test
import java.util.concurrent.CancellationException

@ExperimentalCoroutinesApi
abstract class LifecycleCoroutineScopeTestBase {
    @Test
    fun initialization() {
        val owner = FakeLifecycleOwner(Lifecycle.State.INITIALIZED)
        val scope = owner.lifecycleScope
        assertThat(owner.lifecycle.mInternalScopeRef.get()).isSameInstanceAs(scope)
        val scope2 = owner.lifecycleScope
        assertThat(scope).isSameInstanceAs(scope2)
        runBlocking(Dispatchers.Main) {
            assertThat((owner.lifecycle as LifecycleRegistry).observerCount).isEqualTo(1)
        }
    }

    @Test
    fun simpleLaunch() {
        val owner = FakeLifecycleOwner(Lifecycle.State.INITIALIZED)
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
        val owner = FakeLifecycleOwner(Lifecycle.State.DESTROYED)
        runBlocking {
            owner.lifecycleScope.launch {
                // do nothing
                throw AssertionError("should not run")
            }.join()
        }
    }

    @Test
    fun launchOnMain() {
        val owner = FakeLifecycleOwner(Lifecycle.State.STARTED)
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
        val owner = FakeLifecycleOwner(Lifecycle.State.STARTED)
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
        val owner = FakeLifecycleOwner(Lifecycle.State.STARTED)
        val actionWasActive = owner.lifecycleScope.async(Dispatchers.IO) {
            startMutex.unlock()
            alwaysLocked.lock() // wait 4ever
        }
        runBlocking(Dispatchers.Main) {
            startMutex.lock() // wait until it starts
            owner.setState(Lifecycle.State.DESTROYED)
            actionWasActive.join()
            assertThat(actionWasActive.isCancelled).isTrue()
        }
    }

    @Test
    fun throwException() {
        val owner = FakeLifecycleOwner(Lifecycle.State.STARTED)
        runBlocking {
            val action = owner.lifecycleScope.async {
                throw RuntimeException("foo")
            }
            action.join()
            assertThat(action.getCompletionExceptionOrNull()).hasMessageThat().isSameInstanceAs(
                "foo")
        }
    }

    @Test
    fun throwException_onStart() {
        val owner = FakeLifecycleOwner(Lifecycle.State.CREATED)
        runBlocking {
            // TODO guarantee later execution
            val action = owner.lifecycleScope.async {
                throw RuntimeException("foo")
            }
            withContext(Dispatchers.Main) {
                owner.setState(Lifecycle.State.STARTED)
            }
            action.join()
            assertThat(action.getCompletionExceptionOrNull()).hasMessageThat().isSameInstanceAs(
                "foo")
        }
    }

    @Test
    fun runAnotherAfterCancellation_cancelOutside() {
        val owner = FakeLifecycleOwner(Lifecycle.State.STARTED)
        runBlocking {
            val action = owner.lifecycleScope.async {
                delay(20000)
            }
            action.cancel()
            action.join()
        }
        assertThat(runBlocking {
            owner.lifecycleScope.async {
                true
            }.await()
        }).isTrue()
    }

    @Test
    fun runAnotherAfterCancellation_cancelInside() {
        val owner = FakeLifecycleOwner(Lifecycle.State.STARTED)
        runBlocking {
            val action = owner.lifecycleScope.async {
                throw CancellationException("")
            }
            action.join()
        }
        assertThat(runBlocking {
            owner.lifecycleScope.async {
                true
            }.await()
        }).isTrue()
    }

    @Test
    fun runAnotherAfterFailure() {
        val owner = FakeLifecycleOwner(Lifecycle.State.STARTED)
        runBlocking {
            val action = owner.lifecycleScope.async {
                throw IllegalArgumentException("why not ?")
            }
            val result = kotlin.runCatching {
                @Suppress("IMPLICIT_NOTHING_AS_TYPE_PARAMETER")
                action.await()
            }
            assertThat(result.exceptionOrNull())
                .isInstanceOf(IllegalArgumentException::class.java)
        }
        assertThat(runBlocking {
            owner.lifecycleScope.async {
                true
            }.await()
        }).isTrue()
    }
}
