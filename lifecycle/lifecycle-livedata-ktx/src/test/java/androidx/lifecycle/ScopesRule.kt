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

@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package androidx.lifecycle

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class ScopesRule : TestWatcher() {
    private val mainDispatcher = TestCoroutineDispatcher()
    val mainScope = TestCoroutineScope(mainDispatcher)

    private val testDispatcher = TestCoroutineDispatcher()
    val testScope = TestCoroutineScope(testDispatcher)

    override fun starting(description: Description?) {
        Dispatchers.setMain(mainDispatcher)
        ArchTaskExecutor.getInstance().setDelegate(
            object : TaskExecutor() {
                override fun executeOnDiskIO(runnable: Runnable) {
                    error("unsupported")
                }

                override fun postToMainThread(runnable: Runnable) {
                    mainScope.launch {
                        runnable.run()
                    }
                }

                override fun isMainThread(): Boolean {
                    // we have only one thread in this test.
                    return true
                }
            }
        )
        // manually roll the time
        mainScope.pauseDispatcher()
        testScope.pauseDispatcher()
    }

    override fun finished(description: Description?) {
        advanceTimeBy(100000)
        mainScope.cleanupTestCoroutines()
        testScope.cleanupTestCoroutines()
        ArchTaskExecutor.getInstance().setDelegate(null)
        Dispatchers.resetMain()
    }

    fun advanceTimeBy(time: Long) {
        mainScope.advanceTimeBy(time)
        testScope.advanceTimeBy(time)
        triggerAllActions()
    }

    fun triggerAllActions() {
        do {
            mainScope.runCurrent()
            testScope.runCurrent()
            val allIdle = listOf(mainDispatcher, testDispatcher).all {
                it.isIdle()
            }
        } while (!allIdle)
    }

    fun <T> runOnMain(block: () -> T): T {
        return runBlocking {
            val async = mainScope.async {
                block()
            }
            mainScope.runCurrent()
            async.await()
        }
    }

    private fun TestCoroutineDispatcher.isIdle(): Boolean {
        val queueField = this::class.java
            .getDeclaredField("queue")
        queueField.isAccessible = true
        val queue = queueField.get(this)
        val peekMethod = queue::class.java
            .getDeclaredMethod("peek")
        val nextTask = peekMethod.invoke(queue) ?: return true
        val timeField = nextTask::class.java.getDeclaredField("time")
        timeField.isAccessible = true
        val time = timeField.getLong(nextTask)
        return time > testDispatcher.currentTime
    }
}

fun <T> LiveData<T>.addObserver(scopes: ScopesRule): CollectingObserver<T> {
    return scopes.runOnMain {
        val observer = CollectingObserver(this, scopes)
        observeForever(observer)
        observer
    }
}

class CollectingObserver<T>(
    private val liveData: LiveData<T>,
    private val scopes: ScopesRule
) : Observer<T> {
    private var items = mutableListOf<T>()
    override fun onChanged(t: T) {
        items.add(t)
    }

    fun assertItems(vararg expected: T) {
        Truth.assertThat(items).containsExactly(*expected)
    }

    fun unsubscribe() = scopes.runOnMain {
        liveData.removeObserver(this)
    }
}