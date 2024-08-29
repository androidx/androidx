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

package androidx.camera.camera2.pipe.integration.adapter

import android.os.Build
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@DoNotInstrument
class CoroutineAdapterTest {
    @Test
    fun propagateCompleteResult(): Unit = runBlocking {
        // Arrange.
        val resultValue = 123
        val sourceDeferred = CompletableDeferred<Int>()
        val resultDeferred = CompletableDeferred<Int>()
        sourceDeferred.propagateTo(resultDeferred)

        // Act.
        sourceDeferred.complete(resultValue)

        // Assert.
        assertThat(resultDeferred.await()).isEqualTo(resultValue)
    }

    @Test
    fun propagateTransformedCompleteResult(): Unit = runBlocking {
        // Arrange.
        val resultValue = 123
        val resultValueTransformed = resultValue.toString()

        val sourceDeferred = CompletableDeferred<Int>()
        val resultDeferred = CompletableDeferred<String>()
        sourceDeferred.propagateTo(resultDeferred) { res -> res.toString() }

        // Act.
        sourceDeferred.complete(resultValue)

        // Assert.
        assertThat(resultDeferred.await()).isEqualTo(resultValueTransformed)
    }

    @Test
    fun propagateCancelResult() {
        // Arrange.
        val sourceDeferred = CompletableDeferred<Unit>()
        val resultDeferred = CompletableDeferred<Unit>()
        sourceDeferred.propagateTo(resultDeferred)

        // Act.
        sourceDeferred.cancel()

        // Assert.
        assertThat(resultDeferred.isCancelled).isTrue()
    }

    @Test
    fun propagateCancelResult_whenTransformFunctionIsUsed() {
        // Arrange.
        val sourceDeferred = CompletableDeferred<Unit>()
        val resultDeferred = CompletableDeferred<Unit>()
        sourceDeferred.propagateTo(resultDeferred) { res -> res.toString() }

        // Act.
        sourceDeferred.cancel()

        // Assert.
        assertThat(resultDeferred.isCancelled).isTrue()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun propagateExceptionResult() {
        // Arrange.
        val sourceDeferred = CompletableDeferred<Unit>()
        val resultDeferred = CompletableDeferred<Unit>()
        sourceDeferred.propagateTo(resultDeferred)
        val testThrowable = Throwable()

        // Act.
        sourceDeferred.completeExceptionally(testThrowable)

        // Assert.
        assertThat(resultDeferred.getCompletionExceptionOrNull()).isSameInstanceAs(testThrowable)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun propagateExceptionResult_whenTransformFunctionIsUsed() {
        // Arrange.
        val sourceDeferred = CompletableDeferred<Unit>()
        val resultDeferred = CompletableDeferred<Unit>()
        sourceDeferred.propagateTo(resultDeferred) { res -> res.toString() }
        val testThrowable = Throwable()

        // Act.
        sourceDeferred.completeExceptionally(testThrowable)

        // Assert.
        assertThat(resultDeferred.getCompletionExceptionOrNull()).isSameInstanceAs(testThrowable)
    }
}
