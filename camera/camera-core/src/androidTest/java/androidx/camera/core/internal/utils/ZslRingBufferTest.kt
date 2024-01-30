/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.camera.core.internal.utils

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageInfo
import androidx.camera.core.ImageProxy
import androidx.camera.core.impl.CameraCaptureMetaData.AeState
import androidx.camera.core.impl.CameraCaptureMetaData.AfState
import androidx.camera.core.impl.CameraCaptureMetaData.AwbState
import androidx.camera.core.impl.CameraCaptureResult
import androidx.camera.core.internal.CameraCaptureResultImageInfo
import androidx.camera.core.internal.utils.RingBuffer.OnRemoveCallback
import androidx.camera.testing.impl.fakes.FakeImageProxy
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@RunWith(JUnit4::class)
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ZslRingBufferTest {

    lateinit var mMockedCameraCaptureResult: CameraCaptureResult

    @Before
    fun setup() {
        mMockedCameraCaptureResult = mock(CameraCaptureResult::class.java)
        `when`(mMockedCameraCaptureResult.aeState).thenReturn(AeState.CONVERGED)
        `when`(mMockedCameraCaptureResult.afState).thenReturn(AfState.LOCKED_FOCUSED)
        `when`(mMockedCameraCaptureResult.awbState).thenReturn(AwbState.CONVERGED)
    }

    @Test
    fun enqueue_ensureOldFramesAreRemoved() {
        @Suppress("UNCHECKED_CAST")
        val onRemoveCallback = mock(OnRemoveCallback::class.java) as OnRemoveCallback<ImageProxy>
        val ringBuffer = ZslRingBuffer(
            2,
            onRemoveCallback
        )

        val imageInfo: ImageInfo = CameraCaptureResultImageInfo(mMockedCameraCaptureResult)

        val imageProxy1 =
            FakeImageProxy(imageInfo)
        ringBuffer.enqueue(imageProxy1)
        ringBuffer.enqueue(
            FakeImageProxy(
                imageInfo
            )
        )
        ringBuffer.enqueue(
            FakeImageProxy(
                imageInfo
            )
        )

        verify(onRemoveCallback).onRemove(imageProxy1)
        verify(onRemoveCallback, times(1)).onRemove(any())
    }

    @Test
    fun enqueue_framesWithBad3AStatesNotQueued() {
        @Suppress("UNCHECKED_CAST")
        val onRemoveCallback = mock(OnRemoveCallback::class.java) as OnRemoveCallback<ImageProxy>
        val ringBuffer = ZslRingBuffer(
            2,
            onRemoveCallback
        )

        val imageInfo: ImageInfo = CameraCaptureResultImageInfo(mMockedCameraCaptureResult)

        val imageProxy1 =
            FakeImageProxy(imageInfo)
        ringBuffer.enqueue(imageProxy1)

        `when`(mMockedCameraCaptureResult.aeState).thenReturn(AeState.SEARCHING)
        val imageProxy2 =
            FakeImageProxy(imageInfo)
        ringBuffer.enqueue(imageProxy2)
        verify(onRemoveCallback, times(1)).onRemove(imageProxy2)
        `when`(mMockedCameraCaptureResult.aeState).thenReturn(AeState.CONVERGED)

        `when`(mMockedCameraCaptureResult.afState).thenReturn(AfState.PASSIVE_NOT_FOCUSED)
        val imageProxy3 =
            FakeImageProxy(imageInfo)
        ringBuffer.enqueue(imageProxy3)
        verify(onRemoveCallback, times(1)).onRemove(imageProxy3)
        `when`(mMockedCameraCaptureResult.afState).thenReturn(AfState.PASSIVE_NOT_FOCUSED)

        `when`(mMockedCameraCaptureResult.awbState).thenReturn(AwbState.METERING)
        val imageProxy4 =
            FakeImageProxy(imageInfo)
        ringBuffer.enqueue(imageProxy4)
        verify(onRemoveCallback, times(1)).onRemove(imageProxy4)
        `when`(mMockedCameraCaptureResult.awbState).thenReturn(AwbState.CONVERGED)

        verify(onRemoveCallback, times(3)).onRemove(any())
        assertThat(ringBuffer.dequeue()).isEqualTo(imageProxy1)
    }
}
