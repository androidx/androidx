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

package androidx.camera.video.internal.audio

import androidx.camera.testing.mocks.MockConsumer
import androidx.camera.testing.mocks.MockConsumer.NO_TIMEOUT
import androidx.camera.testing.mocks.helpers.CallTimes
import androidx.camera.testing.mocks.verifyAcceptCallExt

class FakeAudioSourceCallback : AudioSource.AudioSourceCallback {
    private val onSuspendedCallbacks = MockConsumer<Boolean>()
    private val onSilencedCallbacks = MockConsumer<Boolean>()
    private val onErrorCallbacks = MockConsumer<Throwable>()
    private val onAmplitudeCallbacks = MockConsumer<Double>()

    override fun onSuspendStateChanged(suspended: Boolean) {
        onSuspendedCallbacks.accept(suspended)
    }

    override fun onSilenceStateChanged(silenced: Boolean) {
        onSilencedCallbacks.accept(silenced)
    }

    override fun onError(error: Throwable) {
        onErrorCallbacks.accept(error)
    }

    override fun onAmplitudeValue(maxAmplitude: Double) {
        onAmplitudeCallbacks.accept(maxAmplitude)
    }

    fun verifyOnSuspendStateChanged(
        callTimes: CallTimes,
        timeoutMs: Long = NO_TIMEOUT,
        inOder: Boolean = false,
        onSuspendStateChanged: ((List<Boolean>) -> Unit)? = null,
    ) = onSuspendedCallbacks.verifyAcceptCallExt(
        java.lang.Boolean::class.java,
        inOder,
        timeoutMs,
        callTimes,
        onSuspendStateChanged,
    )

    fun verifyOnSilenceStateChanged(
        callTimes: CallTimes,
        timeoutMs: Long = NO_TIMEOUT,
        inOder: Boolean = false,
        onSilenceStateChanged: ((List<Boolean>) -> Unit)? = null,
    ) = onSilencedCallbacks.verifyAcceptCallExt(
        java.lang.Boolean::class.java,
        inOder,
        timeoutMs,
        callTimes,
        onSilenceStateChanged,
    )

    fun verifyOnError(
        callTimes: CallTimes,
        timeoutMs: Long = NO_TIMEOUT,
        inOder: Boolean = false,
        onError: ((List<Throwable>) -> Unit)? = null,
    ) = onErrorCallbacks.verifyAcceptCallExt(
        Throwable::class.java,
        inOder,
        timeoutMs,
        callTimes,
        onError,
    )
}
