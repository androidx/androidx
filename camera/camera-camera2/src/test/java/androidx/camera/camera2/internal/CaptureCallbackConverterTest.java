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

package androidx.camera.camera2.internal;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;

import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraCaptureCallbacks;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.MutableTagBundle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public final class CaptureCallbackConverterTest {

    private CameraCaptureSession mCameraCaptureSession;
    private CaptureRequest mCaptureRequest;
    private TotalCaptureResult mCaptureResult;
    private static final int CAPTURE_CONFIG_ID = 100;


    @Before
    public void setUp() {
        mCameraCaptureSession = mock(CameraCaptureSession.class);
        mCaptureRequest = mock(CaptureRequest.class);
        MutableTagBundle tagBundle = MutableTagBundle.create();
        tagBundle.putTag(CaptureConfig.CAPTURE_CONFIG_ID_TAG_KEY, CAPTURE_CONFIG_ID);
        when(mCaptureRequest.getTag()).thenReturn(tagBundle);
        mCaptureResult = mock(TotalCaptureResult.class);
    }

    @Test
    public void toCaptureCallback() {
        CameraCaptureCallback cameraCallback = Mockito.mock(CameraCaptureCallback.class);
        CaptureCallback callback = CaptureCallbackConverter.toCaptureCallback(cameraCallback);
        callback.onCaptureCompleted(mCameraCaptureSession, mCaptureRequest, mCaptureResult);
        verify(cameraCallback, times(1))
                .onCaptureCompleted(eq(CAPTURE_CONFIG_ID), any(CameraCaptureResult.class));
    }

    @Test
    public void toCaptureCallback_withNullArgument() {
        CaptureCallback callback = CaptureCallbackConverter.toCaptureCallback(null);
        assertThat(callback).isNull();
    }

    @Test
    public void toCaptureCallback_withCaptureCallbackContainer() {
        CaptureCallback actualCallback = Mockito.mock(CaptureCallback.class);
        CaptureCallbackContainer callbackContainer =
                CaptureCallbackContainer.create(actualCallback);
        CaptureCallback callback = CaptureCallbackConverter.toCaptureCallback(callbackContainer);
        callback.onCaptureCompleted(mCameraCaptureSession, mCaptureRequest, mCaptureResult);
        verify(actualCallback, times(1)).onCaptureCompleted(
                any(CameraCaptureSession.class),
                any(CaptureRequest.class),
                any(TotalCaptureResult.class));
    }

    @Test
    public void toCaptureCallback_withComboCameraCallback() {
        CameraCaptureCallback cameraCallback1 = Mockito.mock(CameraCaptureCallback.class);
        CameraCaptureCallback cameraCallback2 = Mockito.mock(CameraCaptureCallback.class);
        CaptureCallback cameraCallback3 = Mockito.mock(CaptureCallback.class);

        CaptureCallback callback =
                CaptureCallbackConverter.toCaptureCallback(
                        CameraCaptureCallbacks.createComboCallback(
                                cameraCallback1,
                                CameraCaptureCallbacks.createComboCallback(
                                        cameraCallback2,
                                        CaptureCallbackContainer.create(cameraCallback3))));

        callback.onCaptureCompleted(mCameraCaptureSession, mCaptureRequest, mCaptureResult);
        verify(cameraCallback1, times(1)).onCaptureCompleted(
                eq(CAPTURE_CONFIG_ID),
                any(CameraCaptureResult.class));
        verify(cameraCallback2, times(1)).onCaptureCompleted(
                eq(CAPTURE_CONFIG_ID),
                any(CameraCaptureResult.class));
        verify(cameraCallback3, times(1))
                .onCaptureCompleted(any(CameraCaptureSession.class),
                        any(CaptureRequest.class), any(TotalCaptureResult.class));
    }
}
