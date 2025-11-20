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

package androidx.camera.camera2.pipe.core

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
internal class WakeLockTest {

    @Test
    fun testWakeLockInvokesCallbackAfterTokenIsReleased() = runTest {
        val result = CompletableDeferred<Boolean>()

        val wakelock = WakeLock(this) { result.complete(true) }

        wakelock.acquire()!!.release()
        assertThat(result.await()).isTrue()
    }

    @Test
    fun testWakelockDoesNotCompleteUntilAllTokensAreReleased() = runTest {
        val result = CompletableDeferred<Boolean>()

        val wakelock = WakeLock(this) { result.complete(true) }

        val token1 = wakelock.acquire()!!
        val token2 = wakelock.acquire()!!

        token1.release()
        delay(50)

        assertThat(result.isActive).isTrue()
        token2.release()

        assertThat(result.await()).isTrue()
    }

    @Test
    fun testClosingWakelockInvokesCallback() = runTest {
        val result = CompletableDeferred<Boolean>()
        val wakelock = WakeLock(this, 100) { result.complete(true) }
        wakelock.release()
        assertThat(result.await()).isTrue()
    }

    @Test
    fun testWakeLockCompletesWhenStartTimeoutOnCreation() = runTest {
        val result = CompletableDeferred<Boolean>()
        WakeLock(this, 100, startTimeoutOnCreation = true) { result.complete(true) }
        assertThat(result.await()).isTrue()
    }
}
