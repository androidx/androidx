/*
 * Copyright (C) 2017 The Android Open Source Project
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

import androidx.arch.core.executor.JunitTaskExecutorRule
import androidx.lifecycle.testing.TestLifecycleOwner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ThreadedLiveDataTest {
    @JvmField
    @Rule
    var taskExecutorRule = JunitTaskExecutorRule(1, false)

    private lateinit var liveData: LiveData<String>
    private lateinit var lifecycleOwner: TestLifecycleOwner

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun init() {
        liveData = MutableLiveData()
        lifecycleOwner = TestLifecycleOwner(
            Lifecycle.State.INITIALIZED,
            UnconfinedTestDispatcher(null, null)
        )
    }

    @Test
    @Throws(InterruptedException::class)
    fun testPostValue() {
        val taskExecutor = taskExecutorRule.taskExecutor
        val finishTestLatch = CountDownLatch(1)
        val observer = Observer<String?> { newValue ->
            try {
                assertThat(taskExecutor.isMainThread, `is`(true))
                assertThat(newValue, `is`("success"))
            } finally {
                finishTestLatch.countDown()
            }
        }
        taskExecutor.executeOnMainThread {
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            liveData.observe(lifecycleOwner, observer)
            val latch = CountDownLatch(1)
            taskExecutor.executeOnDiskIO {
                liveData.postValue("fail")
                liveData.postValue("success")
                latch.countDown()
            }
            try {
                assertThat(
                    latch.await(TIMEOUT_SECS.toLong(), SECONDS),
                    `is`(true)
                )
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
        }
        assertThat(
            finishTestLatch.await(TIMEOUT_SECS.toLong(), SECONDS),
            `is`(true)
        )
    }

    companion object {
        private const val TIMEOUT_SECS = 3
    }
}
