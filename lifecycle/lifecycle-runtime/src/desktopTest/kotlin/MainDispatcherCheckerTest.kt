/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.lifecycle.MainDispatcherChecker
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/*
 * Copyright 2024 The Android Open Source Project
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

class MainDispatcherCheckerTest {
    @Test
    fun checkMainDispatcher() {
        runBlocking(Dispatchers.Main) {
            assertTrue(MainDispatcherChecker.isMainDispatcherThread())
        }
        runBlocking(Dispatchers.Main.immediate) {
            assertTrue(MainDispatcherChecker.isMainDispatcherThread())
        }
    }

    @Test
    fun checkNonMainDispatcher() {
        runBlocking(Dispatchers.IO) {
            assertFalse(MainDispatcherChecker.isMainDispatcherThread())
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun checkMainDispatcherChanged() {
        try {
            Dispatchers.setMain(ThreadChangingMainDispatcher)
            runBlocking(Dispatchers.Main) {
                assertTrue(MainDispatcherChecker.isMainDispatcherThread())
            }
            ThreadChangingMainDispatcher.changeThread()
            runBlocking(Dispatchers.Main) {
                assertTrue(MainDispatcherChecker.isMainDispatcherThread())
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    private object ThreadChangingMainDispatcher : MainCoroutineDispatcher() {
        private var thread: Thread? = null
        private var executor = newExecutorService()

        override val immediate: MainCoroutineDispatcher
            get() = this

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            // support reentrancy
            if (Thread.currentThread() == thread) {
                block.run()
            } else {
                executor.submit(block)
            }
        }

        fun changeThread() {
            executor.shutdown()
            executor = newExecutorService()
        }

        private fun newExecutorService(): ExecutorService =
            Executors.newSingleThreadExecutor {
                thread = Thread(it)
                thread
            }
    }
}