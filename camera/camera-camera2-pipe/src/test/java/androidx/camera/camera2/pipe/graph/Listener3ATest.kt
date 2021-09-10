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

package androidx.camera.camera2.pipe.graph

import android.hardware.camera2.CaptureResult
import android.os.Build
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.RequestNumber
import androidx.camera.camera2.pipe.testing.FakeFrameMetadata
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.testing.UpdateCounting3AStateListener
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
internal class Listener3ATest {
    @Test
    fun testListenersInvoked() {
        val result3AStateListener = Result3AStateListenerImpl(
            mapOf(
                CaptureResult.CONTROL_AF_MODE to listOf(CaptureResult.CONTROL_AF_MODE_AUTO)
            )
        )
        val listener3A = Listener3A()
        listener3A.addListener(result3AStateListener)

        // The deferred result of 3a state listener shouldn't be complete right now.
        assertThat(result3AStateListener.result.isCompleted).isFalse()
        listener3A.onRequestSequenceCreated(
            FakeRequestMetadata(requestNumber = RequestNumber(1))
        )

        val requestMetadata = FakeRequestMetadata(requestNumber = RequestNumber(1))
        val frameNumber = FrameNumber(1L)
        val captureResult = FakeFrameMetadata(
            mapOf(
                CaptureResult.CONTROL_AF_MODE to CaptureResult.CONTROL_AF_MODE_AUTO
            )
        )
        // Once the correct metadata is updated the listener3A should broadcast it to the
        // result3AState listener added to it, making the deferred result complete.
        listener3A.onPartialCaptureResult(requestMetadata, frameNumber, captureResult)
        assertThat(result3AStateListener.result.isCompleted).isTrue()
    }

    @Test
    fun testListenersInvokedWithMultipleUpdates() {
        val result3AStateListener = UpdateCounting3AStateListener(
            Result3AStateListenerImpl(
                mapOf(
                    CaptureResult.CONTROL_AF_MODE to listOf(CaptureResult.CONTROL_AF_MODE_AUTO)
                )
            )
        )
        val listener3A = Listener3A()
        listener3A.addListener(result3AStateListener)

        listener3A.onRequestSequenceCreated(
            FakeRequestMetadata(requestNumber = RequestNumber(1))
        )

        val requestMetadata = FakeRequestMetadata(requestNumber = RequestNumber(1))
        val captureResult = FakeFrameMetadata(
            mapOf(
                CaptureResult.CONTROL_AF_MODE to CaptureResult.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )
        )
        val captureResult1 = FakeFrameMetadata(
            mapOf(
                CaptureResult.CONTROL_AF_MODE to CaptureResult.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )
        )
        listener3A.onPartialCaptureResult(requestMetadata, FrameNumber(1), captureResult)
        assertThat(result3AStateListener.updateCount).isEqualTo(1)

        // Since the first update didn't have the right key and it's desired value, the second
        // update should also be supplies to the result3AListener.
        listener3A.onPartialCaptureResult(requestMetadata, FrameNumber(2), captureResult1)
        assertThat(result3AStateListener.updateCount).isEqualTo(2)
    }

    @Test
    fun testListenersAreRemovedWhenDone() {
        val result3AStateListener1 = UpdateCounting3AStateListener(
            Result3AStateListenerImpl(
                mapOf(
                    CaptureResult.CONTROL_AF_MODE to listOf(CaptureResult.CONTROL_AF_MODE_AUTO)
                )
            )
        )
        val result3AStateListener2 = UpdateCounting3AStateListener(
            Result3AStateListenerImpl(
                mapOf(
                    CaptureResult.CONTROL_AE_MODE to listOf(CaptureResult.CONTROL_AE_MODE_OFF)
                )
            )
        )

        val listener3A = Listener3A()
        listener3A.addListener(result3AStateListener1)
        listener3A.addListener(result3AStateListener2)

        val requestMetadata = FakeRequestMetadata(requestNumber = RequestNumber(1))
        val frameNumber = FrameNumber(1L)
        val captureResult = FakeFrameMetadata(
            mapOf(
                CaptureResult.CONTROL_AF_MODE to CaptureResult.CONTROL_AF_MODE_AUTO
            )
        )

        // There should be no update to either of the listeners right now.
        assertThat(result3AStateListener1.updateCount).isEqualTo(0)
        assertThat(result3AStateListener2.updateCount).isEqualTo(0)

        listener3A.onRequestSequenceCreated(
            FakeRequestMetadata(requestNumber = RequestNumber(1))
        )

        // Once the metadata for correct AF mode is updated, the listener3A should broadcast it to
        // the result3AState listeners added to it, making result3AStateListener1 complete.
        listener3A.onPartialCaptureResult(requestMetadata, frameNumber, captureResult)
        assertThat(result3AStateListener1.updateCount).isEqualTo(1)
        assertThat(result3AStateListener2.updateCount).isEqualTo(1)

        // Once the metadata for correct AE mode is updated, the listener3A should broadcast it to
        // the result3AState listeners added to it, making result3AStateListener2 complete. Since
        // result3AStateListener1 was already completed it will not be updated again.
        val captureResult1 = FakeFrameMetadata(
            mapOf(
                CaptureResult.CONTROL_AF_MODE to CaptureResult.CONTROL_AF_MODE_AUTO,
                CaptureResult.CONTROL_AE_MODE to CaptureResult.CONTROL_AE_MODE_OFF
            )
        )
        listener3A.onPartialCaptureResult(requestMetadata, frameNumber, captureResult1)
        assertThat(result3AStateListener1.updateCount).isEqualTo(1)
        assertThat(result3AStateListener2.updateCount).isEqualTo(2)

        // Since both result3AStateListener1 and result3AStateListener2 are complete, they will not
        // receive further updates.
        listener3A.onPartialCaptureResult(requestMetadata, frameNumber, captureResult1)
        assertThat(result3AStateListener1.updateCount).isEqualTo(1)
        assertThat(result3AStateListener2.updateCount).isEqualTo(2)
    }
}