/*
 * Copyright (C) 2019 The Android Open Source Project
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.view.Surface;

import androidx.camera.core.CameraCaptureCallback;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@SmallTest
@RunWith(AndroidJUnit4.class)
public final class CameraCaptureCallbackAdapterTest {

    private CameraCaptureCallback mCameraCaptureCallback;
    private CameraCaptureSession mCameraCaptureSession;
    private CaptureRequest mCaptureRequest;
    private CameraCaptureCallbackAdapter mCameraCaptureCallbackAdapter;

    @Before
    public void setUp() {
        mCameraCaptureCallback = Mockito.mock(CameraCaptureCallback.class);
        mCameraCaptureSession = Mockito.mock(CameraCaptureSession.class);
        // Mockito can't mock final class
        mCaptureRequest = null;
        Mockito.mock(Surface.class);
        mCameraCaptureCallbackAdapter = new CameraCaptureCallbackAdapter(mCameraCaptureCallback);
    }

    @Test(expected = NullPointerException.class)
    public void createCameraCaptureCallbackAdapterWithNullArgument() {
        new CameraCaptureCallbackAdapter(null);
    }

    @Test
    public void onCaptureCompleted() {
        mCameraCaptureCallbackAdapter.onCaptureCompleted(
                mCameraCaptureSession, mCaptureRequest, any());
        verify(mCameraCaptureCallback, times(1)).onCaptureCompleted(any());
    }

    @Test
    public void onCaptureFailed() {
        mCameraCaptureCallbackAdapter.onCaptureFailed(mCameraCaptureSession, mCaptureRequest,
                any());
        verify(mCameraCaptureCallback, times(1)).onCaptureFailed(any());
    }
}
