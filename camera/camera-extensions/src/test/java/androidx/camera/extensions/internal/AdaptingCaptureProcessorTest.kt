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

package androidx.camera.extensions.internal

import android.hardware.camera2.TotalCaptureResult
import android.media.Image
import android.os.Build
import android.util.Pair
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.internal.Camera2CameraCaptureResult
import androidx.camera.core.impl.ImageProxyBundle
import androidx.camera.core.impl.SingleImageProxyBundle
import androidx.camera.core.impl.TagBundle
import androidx.camera.core.internal.CameraCaptureResultImageInfo
import androidx.camera.extensions.impl.CaptureProcessorImpl
import androidx.camera.testing.fakes.FakeImageProxy
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito
import org.mockito.Mockito.inOrder
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

private const val TAG_BUNDLE_KEY = "FakeTagBundleKey"
private const val CAPTURE_ID = 0

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class AdaptingCaptureProcessorTest {
    private val captureProcessorImpl = Mockito.mock(CaptureProcessorImpl::class.java)
    private var adaptingCaptureProcessor = AdaptingCaptureProcessor(captureProcessorImpl)
    private val imageProxyBundle = createFakeImageProxyBundle()

    @Test
    fun processDoesNotCallImplAfterClose() {
        callOnInitAndVerify()
        adaptingCaptureProcessor.close()
        adaptingCaptureProcessor.process(imageProxyBundle)
        Mockito.verifyZeroInteractions(captureProcessorImpl)
    }

    @Test
    fun onImageFormatUpdateDoesNotCallImplAfterClose() {
        adaptingCaptureProcessor.close()
        adaptingCaptureProcessor.onOutputSurface(Mockito.mock(Surface::class.java), 0)
        adaptingCaptureProcessor.onInit()
        Mockito.verifyZeroInteractions(captureProcessorImpl)
    }

    @Test
    fun onResolutionUpdateDoesNotCallImplAfterClose() {
        adaptingCaptureProcessor.close()
        adaptingCaptureProcessor.onResolutionUpdate(Size(640, 480))
        adaptingCaptureProcessor.onInit()
        Mockito.verifyZeroInteractions(captureProcessorImpl)
    }

    @Test
    fun processCanCallImplBeforeDeInit() {
        callOnInitAndVerify()
        adaptingCaptureProcessor.process(imageProxyBundle)
        Mockito.verify(captureProcessorImpl, Mockito.times(1)).process(any())
    }

    @Test
    fun processDoesNotCallImplAfterDeInit() {
        callOnInitAndVerify()
        adaptingCaptureProcessor.onDeInit()
        adaptingCaptureProcessor.process(imageProxyBundle)
        Mockito.verifyZeroInteractions(captureProcessorImpl)
    }

    private fun createFakeImageProxyBundle(
        bundleKey: String = TAG_BUNDLE_KEY,
        captureId: Int = CAPTURE_ID
    ): ImageProxyBundle {
        val fakeCameraCaptureResult = Mockito.mock(Camera2CameraCaptureResult::class.java)
        Mockito.`when`(fakeCameraCaptureResult.tagBundle)
            .thenReturn(TagBundle.create(Pair.create(bundleKey, captureId)))
        Mockito.`when`(fakeCameraCaptureResult.captureResult)
            .thenReturn(Mockito.mock(TotalCaptureResult::class.java))
        val fakeImageInfo = CameraCaptureResultImageInfo(fakeCameraCaptureResult)
        val fakeImageProxy = FakeImageProxy(fakeImageInfo)
        fakeImageProxy.image = Mockito.mock(Image::class.java)
        return SingleImageProxyBundle(fakeImageProxy, bundleKey)
    }

    private fun callOnInitAndVerify() {
        adaptingCaptureProcessor.onInit()

        inOrder(captureProcessorImpl).apply {
            verify(captureProcessorImpl).onOutputSurface(any(), anyInt())
            verify(captureProcessorImpl).onImageFormatUpdate(anyInt())
            verify(captureProcessorImpl).onResolutionUpdate(any())
        }
    }
}