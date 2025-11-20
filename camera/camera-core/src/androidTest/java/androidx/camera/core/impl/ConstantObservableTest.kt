/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.impl.asFlow
import androidx.concurrent.futures.await
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

private const val MAGIC_VALUE = 42
private const val MAGIC_STRING = "Magic"

@SmallTest
@RunWith(AndroidJUnit4::class)
public class ConstantObservableTest {

    @Test
    public fun fetchData_returnsValue(): Unit = runBlocking {
        val constantObservable = ConstantObservable.withValue(MAGIC_VALUE)
        val value = constantObservable.fetchData().await()
        assertThat(value).isEqualTo(MAGIC_VALUE)
    }

    @Test
    public fun fetchData_canReturnNull(): Unit = runBlocking {
        val constantObservable: Observable<Any?> = ConstantObservable.withValue(null)
        val value = constantObservable.fetchData().await()
        assertThat(value).isNull()
    }

    @Test
    public fun observer_receivesValue(): Unit = runBlocking {
        val constantObservable = ConstantObservable.withValue(MAGIC_VALUE)
        val value = constantObservable.asFlow().first()
        assertThat(value).isEqualTo(MAGIC_VALUE)
    }

    @Test
    public fun observerOfSuperClass_receivesValue(): Unit = runBlocking {
        val constantObservable: Observable<String> = ConstantObservable.withValue(MAGIC_STRING)

        // Create on observer of CharSequence, a superclass of String
        val deferredValue = CompletableDeferred<CharSequence?>()
        val observer: Observable.Observer<CharSequence> =
            object : Observable.Observer<CharSequence> {
                override fun onNewData(value: CharSequence?) {
                    deferredValue.complete(value)
                }

                override fun onError(t: Throwable) {
                    deferredValue.completeExceptionally(t)
                }
            }

        // Add the observer to receive the value
        constantObservable.addObserver(CameraXExecutors.directExecutor(), observer)

        assertThat(deferredValue.await()).isEqualTo(MAGIC_STRING)
    }
}
