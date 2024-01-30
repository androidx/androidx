/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.lifecycle.util

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class ScopesRule : TestWatcher() {
    private val scheduler = TestCoroutineScheduler()
    private val mainDispatcher = StandardTestDispatcher(scheduler)
    val mainScope = TestScope(mainDispatcher)

    private val testDispatcher = StandardTestDispatcher(scheduler)
    val testScope = TestScope(testDispatcher)

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
    }

    override fun finished(description: Description?) {
        advanceTimeBy(100000)
        ArchTaskExecutor.getInstance().setDelegate(null)
        Dispatchers.resetMain()
    }

    fun advanceTimeBy(time: Long) {
        scheduler.advanceTimeBy(time)
        triggerAllActions()
    }

    fun triggerAllActions() {
        scheduler.runCurrent()
    }

    fun <T> runOnMain(block: () -> T): T {
        return runBlocking {
            val async = mainScope.async {
                block()
            }
            mainScope.testScheduler.runCurrent()
            async.await()
        }
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
    override fun onChanged(value: T) {
        items.add(value)
    }

    fun assertItems(vararg expected: T) {
        Truth.assertThat(items).containsExactly(*expected)
    }

    fun unsubscribe() = scopes.runOnMain {
        liveData.removeObserver(this)
    }
}
