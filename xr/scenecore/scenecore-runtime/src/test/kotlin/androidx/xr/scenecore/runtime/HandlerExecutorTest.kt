/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.scenecore.runtime

import android.os.Handler
import android.os.Looper
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class HandlerExecutorTest {

    @Test
    fun handlerExecutor_executesCommand() {
        val looper = Looper.getMainLooper()
        val handler = Handler(looper)
        val executor = HandlerExecutor(handler)
        val executed = AtomicBoolean(false)

        executor.execute { executed.set(true) }
        shadowOf(looper).idle()

        assertThat(executed.get()).isTrue()
    }

    @Test
    fun handlerExecutor_throwsOnRejectedExecution() {
        val mockHandler = mock<Handler> { on { post(any()) } doReturn false }
        val executor = HandlerExecutor(mockHandler)

        assertThrows(RejectedExecutionException::class.java) { executor.execute {} }
        verify(mockHandler).post(any())
    }

    @Test
    fun mainThreadExecutor_usesMainLooper() {
        val executor = HandlerExecutor.mainThreadExecutor as HandlerExecutor
        assertThat(executor.handler.looper).isEqualTo(Looper.getMainLooper())
    }
}
