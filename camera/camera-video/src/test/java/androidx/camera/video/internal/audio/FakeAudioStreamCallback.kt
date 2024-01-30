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

import androidx.annotation.RequiresApi
import androidx.camera.testing.impl.mocks.MockConsumer
import androidx.camera.testing.impl.mocks.helpers.CallTimes
import androidx.camera.testing.impl.mocks.verifyAcceptCallExt

@RequiresApi(21)
class FakeAudioStreamCallback : AudioStream.AudioStreamCallback {
    private val onSilencedCallbacks = MockConsumer<Boolean>()

    override fun onSilenceStateChanged(silenced: Boolean) {
        onSilencedCallbacks.accept(silenced)
    }

    fun verifyOnSilenceStateChangedCall(
        callTimes: CallTimes,
        timeoutMs: Long = MockConsumer.NO_TIMEOUT,
        inOrder: Boolean = false,
        onSilenceStateChanged: ((List<Boolean>) -> Unit)? = null,
    ) = onSilencedCallbacks.verifyAcceptCallExt(
        java.lang.Boolean::class.java,
        inOrder,
        timeoutMs,
        callTimes,
        onSilenceStateChanged,
    )
}
