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

package androidx.camera.camera2;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;

import androidx.camera.core.CameraCaptureCallback;
import androidx.camera.core.CameraCaptureFailure;
import androidx.camera.core.CameraCaptureResult;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public final class CaptureCallbackAdapterTest {

    private CameraCaptureCallback mCameraCaptureCallback;
    private CameraCaptureSession mCameraCaptureSession;
    private CaptureRequest mCaptureRequest;
    private TotalCaptureResult mCaptureResult;
    private CaptureCallbackAdapter mCaptureCallbackAdapter;

    @Before
    public void setUp() {
        mCameraCaptureCallback = mock(CameraCaptureCallback.class);
        mCameraCaptureSession = mock(CameraCaptureSession.class);
        mCaptureRequest = mock(CaptureRequest.class);
        mCaptureResult = mock(TotalCaptureResult.class);
        mCaptureCallbackAdapter = new CaptureCallbackAdapter(mCameraCaptureCallback);
    }

    @Test(expected = NullPointerException.class)
    public void createCaptureCallbackAdapterWithNullArgument() {
        new CaptureCallbackAdapter(null);
    }

    @Test
    public void onCaptureCompleted() {
        mCaptureCallbackAdapter.onCaptureCompleted(
                mCameraCaptureSession, mCaptureRequest, mCaptureResult);
        verify(mCameraCaptureCallback, times(1)).onCaptureCompleted(any(CameraCaptureResult.class));
    }

    @Test
    public void onCaptureFailed() {
        mCaptureCallbackAdapter.onCaptureFailed(mCameraCaptureSession, mCaptureRequest,
                mock(CaptureFailure.class));
        verify(mCameraCaptureCallback, times(1)).onCaptureFailed(any(CameraCaptureFailure.class));
    }
}
