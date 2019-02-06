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

package androidx.camera.core;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

@RunWith(JUnit4.class)
public final class CameraCaptureCallbacksAndroidTest {

  @Test
  public void comboCallbackInvokesConstituentCallbacks() {
    CameraCaptureCallback callback0 = Mockito.mock(CameraCaptureCallback.class);
    CameraCaptureCallback callback1 = Mockito.mock(CameraCaptureCallback.class);
    CameraCaptureCallback comboCallback =
        CameraCaptureCallbacks.createComboCallback(callback0, callback1);
    CameraCaptureResult result = Mockito.mock(CameraCaptureResult.class);
    CameraCaptureFailure failure = new CameraCaptureFailure(CameraCaptureFailure.Reason.ERROR);

    comboCallback.onCaptureCompleted(result);
    verify(callback0, times(1)).onCaptureCompleted(result);
    verify(callback1, times(1)).onCaptureCompleted(result);

    comboCallback.onCaptureFailed(failure);
    verify(callback0, times(1)).onCaptureFailed(failure);
    verify(callback1, times(1)).onCaptureFailed(failure);
  }
}
