/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.extensions;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;

import androidx.camera.camera2.impl.Camera2CameraCaptureResultConverter;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.CameraCaptureResults;
import androidx.camera.extensions.impl.PreviewExtenderImpl;
import androidx.camera.extensions.impl.RequestUpdateProcessorImpl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP,
        shadows = {
                AdaptingRequestUpdateProcessorTest.ShadowCameraCaptureResults.class,
                AdaptingRequestUpdateProcessorTest.ShadowCamera2CameraCaptureResultConverter.class})
public class AdaptingRequestUpdateProcessorTest {
    private AdaptingRequestUpdateProcessor mAdaptingRequestUpdateProcessor;
    private PreviewExtenderImpl mPreviewExtenderImpl;
    private RequestUpdateProcessorImpl mImpl;
    private ImageInfo mImageInfo;

    @Before
    public void setup() {
        mImpl = mock(RequestUpdateProcessorImpl.class);
        mPreviewExtenderImpl = mock(PreviewExtenderImpl.class);
        when(mPreviewExtenderImpl.getProcessor()).thenReturn(mImpl);
        when(mPreviewExtenderImpl.getProcessorType()).thenReturn(
                PreviewExtenderImpl.ProcessorType.PROCESSOR_TYPE_REQUEST_UPDATE_ONLY);

        mImageInfo = mock(ImageInfo.class);

        mAdaptingRequestUpdateProcessor = new AdaptingRequestUpdateProcessor(mPreviewExtenderImpl);
    }

    @Test
    public void getCaptureStageDoesNotCallImplAfterClose() {
        clearInvocations(mPreviewExtenderImpl);
        mAdaptingRequestUpdateProcessor.close();

        mAdaptingRequestUpdateProcessor.getCaptureStage();

        verifyZeroInteractions(mPreviewExtenderImpl);
    }

    @Test
    public void processDoesNotCallImplAfterClose() {
        mAdaptingRequestUpdateProcessor.close();

        mAdaptingRequestUpdateProcessor.process(mImageInfo);

        verifyZeroInteractions(mImpl);
    }

    /**
     * Shadow of {@link Camera2CameraCaptureResultConverter} to control return of
     * {@link #getCaptureResult(CameraCaptureResult)}.
     */
    @Implements(
            value = Camera2CameraCaptureResultConverter.class,
            minSdk = 21
    )
    static final class ShadowCamera2CameraCaptureResultConverter {
        /** Returns {@link TotalCaptureResult} regardless of input. */
        @Implementation
        public static CaptureResult getCaptureResult(CameraCaptureResult cameraCaptureResult) {
            return mock(TotalCaptureResult.class);
        }
    }

    /**
     * Shadow of {@link CameraCaptureResults} to control return of
     * {@link #retrieveCameraCaptureResult(ImageInfo)}.
     */
    @Implements(
            value = CameraCaptureResults.class,
            minSdk = 21
    )
    static final class ShadowCameraCaptureResults {
        /** Returns {@link CameraCaptureResult} regardless of input. */
        @Implementation
        public static CameraCaptureResult retrieveCameraCaptureResult(ImageInfo imageInfo) {
            return mock(CameraCaptureResult.class);
        }
    }

}
