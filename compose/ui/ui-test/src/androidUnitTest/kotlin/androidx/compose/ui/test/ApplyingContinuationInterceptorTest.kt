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

package androidx.compose.ui.test

import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Delay
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ApplyingContinuationInterceptorTest {
    @Test
    fun addApplyingContinuationInterceptor_replacesContinuationInterceptor() {
        val continuationInterceptor = FakeContinuationInterceptor()
        val applyingContinuationInterceptor =
            ApplyingContinuationInterceptor(continuationInterceptor)
        val context = continuationInterceptor + applyingContinuationInterceptor
        assertThat(context[ContinuationInterceptor]).isEqualTo(applyingContinuationInterceptor)
    }

    @Test
    fun addApplyingContinuationInterceptor_replacesTestDispatcher() {
        val dispatcher = StandardTestDispatcher()
        val applyingContinuationInterceptor = ApplyingContinuationInterceptor(dispatcher)
        val context = dispatcher + applyingContinuationInterceptor
        assertThat(context[ContinuationInterceptor]).isEqualTo(applyingContinuationInterceptor)
    }

    @OptIn(InternalCoroutinesApi::class)
    private class FakeContinuationInterceptor : ContinuationInterceptor, Delay {
        override val key: CoroutineContext.Key<*>
            get() = ContinuationInterceptor

        override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> =
            TODO("Not yet implemented")

        override fun scheduleResumeAfterDelay(
            timeMillis: Long,
            continuation: CancellableContinuation<Unit>
        ): Unit = TODO("Not yet implemented")
    }
}
