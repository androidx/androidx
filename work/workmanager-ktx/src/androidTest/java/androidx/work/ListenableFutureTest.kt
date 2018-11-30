/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.work

import androidx.concurrent.futures.ResolvableFuture
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ListenableFutureTest {
    @Test
    fun testFutureWithResult() {
        val future: ResolvableFuture<Int> = ResolvableFuture.create()
        val job = GlobalScope.launch {
            val result = future.await()
            assertThat(result, `is`(10))
        }
        future.set(10)
        runBlocking {
            job.join()
        }
    }
    @Test
    fun testFutureWithException() {
        val future: ResolvableFuture<Int> = ResolvableFuture.create()
        val exception = RuntimeException("Something bad happened")
        val job = GlobalScope.launch {
            try {
                future.await()
            } catch (throwable: Throwable) {
                assertThat(throwable, `is`(instanceOf(RuntimeException::class.java)))
                assertThat(throwable.message, `is`(exception.message))
            }
        }
        future.setException(exception)
        runBlocking {
            job.join()
        }
    }
    @Test
    fun testFutureCancellation() {
        val future: ResolvableFuture<Int> = ResolvableFuture.create()
        val job = GlobalScope.launch {
            future.await()
        }
        future.cancel(true)
        runBlocking {
            job.join()
            assertThat(job.isCancelled, `is`(true))
        }
    }
}
