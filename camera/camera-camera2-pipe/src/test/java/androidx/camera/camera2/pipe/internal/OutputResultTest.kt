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

package androidx.camera.camera2.pipe.internal

import android.os.Build
import androidx.camera.camera2.pipe.OutputStatus
import androidx.camera.camera2.pipe.internal.OutputResult.Companion.completeWithFailure
import androidx.camera.camera2.pipe.internal.OutputResult.Companion.completeWithOutput
import androidx.camera.camera2.pipe.internal.OutputResult.Companion.outputOrNull
import androidx.camera.camera2.pipe.internal.OutputResult.Companion.outputStatus
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class OutputResultTest {

    @Test
    fun outputResultCanBeCreatedWithObjects() {
        val value = Any()
        val result = OutputResult.from(value)

        assertThat(result.available).isTrue()
        assertThat(result.output).isSameInstanceAs(value)
        assertThat(result.status).isEqualTo(OutputStatus.AVAILABLE)
    }

    @Test
    fun outputResultsCanFail() {
        val result = OutputResult.failure<Any>(OutputStatus.ERROR_OUTPUT_ABORTED)

        assertThat(result.available).isFalse()
        assertThat(result.output).isNull()
        assertThat(result.status).isEqualTo(OutputStatus.ERROR_OUTPUT_ABORTED)
    }

    @Test
    fun outputResultWorkWithIntegers() {
        // Check to make sure this works with integers since OutputStatus is an inline value class.
        val value = 42
        val result = OutputResult.from(value)

        assertThat(result.available).isTrue()
        assertThat(result.output).isSameInstanceAs(value)
        assertThat(result.status).isEqualTo(OutputStatus.AVAILABLE)

        val failed = OutputResult.failure<Int>(OutputStatus.ERROR_OUTPUT_DROPPED)

        assertThat(failed.available).isFalse()
        assertThat(failed.output).isNull()
        assertThat(failed.status).isEqualTo(OutputStatus.ERROR_OUTPUT_DROPPED)
    }

    @Test
    fun outputResultWithNullableTypesAccuratelyHandleFailure() {
        val result = OutputResult.from(null)

        assertThat(result.available).isFalse()
        assertThat(result.failure).isFalse()
        assertThat(result.status).isEqualTo(OutputStatus.UNAVAILABLE)
        assertThat<Any?>(result.output).isNull()
    }

    @Test
    fun deferredWithOutputResultCompleteWithRealValue() {
        val value = 42
        val deferred = CompletableDeferred<OutputResult<Int>>()

        assertThat(deferred.outputStatus()).isEqualTo(OutputStatus.PENDING)
        assertThat(deferred.outputOrNull()).isNull()

        deferred.completeWithOutput(value)

        assertThat(deferred.outputStatus()).isEqualTo(OutputStatus.AVAILABLE)
        assertThat(deferred.outputOrNull()).isEqualTo(value)
    }

    @Test
    fun deferredWithOutputResultCanBeCanceled() {
        val deferred = CompletableDeferred<OutputResult<Int>>()
        deferred.cancel()

        assertThat(deferred.outputStatus()).isEqualTo(OutputStatus.UNAVAILABLE)
        assertThat(deferred.outputOrNull()).isNull()
    }

    @Test
    fun deferredWithOutputResultCanBeFailedWithStatus() {
        val deferred = CompletableDeferred<OutputResult<Int>>()
        deferred.completeWithFailure(OutputStatus.ERROR_OUTPUT_DROPPED)

        assertThat(deferred.outputStatus()).isEqualTo(OutputStatus.ERROR_OUTPUT_DROPPED)
        assertThat(deferred.outputOrNull()).isNull()
    }
}
