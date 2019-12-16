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

import android.hardware.camera2.CameraCaptureSession.CaptureCallback;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@LargeTest
@RunWith(AndroidJUnit4.class)
public final class CaptureCallbackContainerTest {

    @Test(expected = NullPointerException.class)
    public void createCaptureCallbackContainer_withNullArgument() {
        CaptureCallbackContainer.create(null);
    }

    @Test
    public void getCaptureCallback() {
        CaptureCallback captureCallback = Mockito.mock(CaptureCallback.class);
        CaptureCallbackContainer callbackContainer =
                CaptureCallbackContainer.create(captureCallback);
        assertThat(callbackContainer.getCaptureCallback()).isEqualTo(captureCallback);
    }
}
