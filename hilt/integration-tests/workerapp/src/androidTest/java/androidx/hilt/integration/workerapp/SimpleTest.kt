/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.hilt.integration.workerapp

import androidx.hilt.work.HiltWorkerFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
// TODO: Find out why random ClassNotFoundException is thrown in APIs lower than 21.
class SimpleTest {

    @get:Rule val testRule = HiltAndroidRule(this)

    @Inject lateinit var hiltWorkerFactory: HiltWorkerFactory

    @Before
    fun setup() {
        testRule.inject()
    }

    @Test
    fun testWorkerInject() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val worker =
            TestListenableWorkerBuilder<SimpleWorker>(context)
                .apply { setWorkerFactory(hiltWorkerFactory) }
                .build()
        val result = worker.doWork()
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun testCoroutineWorkerInject() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val worker =
            TestListenableWorkerBuilder<SimpleCoroutineWorker>(context)
                .apply { setWorkerFactory(hiltWorkerFactory) }
                .build()
        runBlocking {
            val result = worker.doWork()
            assertThat(result).isEqualTo(ListenableWorker.Result.success())
        }
    }

    @Test
    fun testNestedWorkerInject() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val worker =
            TestListenableWorkerBuilder<TopClass.NestedWorker>(context)
                .apply { setWorkerFactory(hiltWorkerFactory) }
                .build()
        val result = worker.doWork()
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }
}
