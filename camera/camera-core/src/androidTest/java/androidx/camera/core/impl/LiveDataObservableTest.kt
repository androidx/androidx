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

package androidx.camera.core.impl

import androidx.camera.testing.impl.asFlow
import androidx.concurrent.futures.await
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith

private const val MAGIC_VALUE = 42
private val TEST_ERROR = TestError("TEST")

@SmallTest
@RunWith(AndroidJUnit4::class)
public class LiveDataObservableTest {

    @Test
    public fun uninitializedFetch_throwsISE(): Unit = runBlocking {
        val uninitializedObservable = LiveDataObservable<Int>()

        assertThrows<IllegalStateException> { uninitializedObservable.fetchData().await() }
    }

    @Test
    public fun canSetAndFetchValue(): Unit = runBlocking {
        val observable = LiveDataObservable<Int>().apply { postValue(MAGIC_VALUE) }

        val fetched = observable.fetchData().await()
        assertThat(fetched).isEqualTo(MAGIC_VALUE)
    }

    @Test
    public fun canSetAndFetchValue_onMainThread(): Unit = runBlocking {
        val fetched =
            withContext(Dispatchers.Main) {
                val observable = LiveDataObservable<Int>().apply { postValue(MAGIC_VALUE) }

                observable.fetchData().await()
            }

        assertThat(fetched).isEqualTo(MAGIC_VALUE)
    }

    @Test
    public fun canSetAndReceiveError(): Unit = runBlocking {
        val observable = LiveDataObservable<Int>().apply { postError(TEST_ERROR) }

        assertThrows<TestError> { observable.fetchData().await() }
    }

    @Test
    public fun canObserveToRetrieveValue(): Unit = runBlocking {
        val observable = LiveDataObservable<Int>()
        observable.postValue(MAGIC_VALUE)

        val fetched = observable.asFlow().first()
        assertThat(fetched).isEqualTo(MAGIC_VALUE)
    }

    @Test
    public fun canObserveToRetrieveError(): Unit = runBlocking {
        val observable = LiveDataObservable<Int>()
        observable.postError(TEST_ERROR)

        assertThrows<TestError> { observable.asFlow().first() }
    }

    @Test
    public fun canObserveToRetrieveValue_whenSetAfterObserve(): Unit = runBlocking {
        val observable = LiveDataObservable<Int>()

        val flow = observable.asFlow() // Sets observer
        observable.postValue(MAGIC_VALUE)
        assertThat(flow.first()).isEqualTo(MAGIC_VALUE)
    }

    @Test
    public fun canObserveToRetrieveError_whenSetAfterObserve(): Unit = runBlocking {
        val observable = LiveDataObservable<Int>()

        val flow = observable.asFlow() // Sets observer
        observable.postError(TEST_ERROR)
        assertThrows<TestError> { flow.first() }
    }
}

internal class TestError(message: String) : Exception(message)
