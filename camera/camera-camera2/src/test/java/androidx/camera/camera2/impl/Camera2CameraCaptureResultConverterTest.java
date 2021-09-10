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

package androidx.camera.camera2.impl;

import static com.google.common.truth.Truth.assertThat;

import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureResult;
import android.os.Build;

import androidx.camera.camera2.internal.Camera2CameraCaptureFailure;
import androidx.camera.camera2.internal.Camera2CameraCaptureResult;
import androidx.camera.core.impl.CameraCaptureFailure;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.TagBundle;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public final class Camera2CameraCaptureResultConverterTest {
    private CaptureResult mCaptureResult = Mockito.mock(CaptureResult.class);

    @Test
    public void canRetrieveCaptureResult() {
        CameraCaptureResult cameraCaptureResult =
                new Camera2CameraCaptureResult(TagBundle.emptyBundle(), mCaptureResult);

        CaptureResult captureResult = Camera2CameraCaptureResultConverter.getCaptureResult(
                cameraCaptureResult);

        assertThat(captureResult).isSameInstanceAs(mCaptureResult);
    }

    @Test
    public void retrieveNullIfNotCamera2CameraCaptureResult() {
        CameraCaptureResult cameraCaptureResult =
                new CameraCaptureResult.EmptyCameraCaptureResult();

        CaptureResult captureResult = Camera2CameraCaptureResultConverter.getCaptureResult(
                cameraCaptureResult);

        assertThat(captureResult).isNull();
    }

    @Test
    public void canRetrieveCaptureFailure() {
        CaptureFailure captureFailure = Mockito.mock(CaptureFailure.class);
        CameraCaptureFailure cameraCaptureFailure =
                new Camera2CameraCaptureFailure(CameraCaptureFailure.Reason.ERROR, captureFailure);

        CaptureFailure retrievedCaptureResult =
                Camera2CameraCaptureResultConverter.getCaptureFailure(
                cameraCaptureFailure);

        assertThat(retrievedCaptureResult).isSameInstanceAs(captureFailure);
    }

    @Test
    public void retrieveNullIfNotCamera2CameraCaptureFailure() {
        CameraCaptureFailure cameraCaptureFailure =
                new CameraCaptureFailure(CameraCaptureFailure.Reason.ERROR);

        CaptureFailure captureFailure = Camera2CameraCaptureResultConverter.getCaptureFailure(
                cameraCaptureFailure);

        assertThat(captureFailure).isNull();
    }
}
