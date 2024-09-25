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

package androidx.camera.core.internal;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build;

import androidx.camera.core.ImageInfo;
import androidx.camera.testing.fakes.FakeCameraCaptureResult;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class CameraCaptureResultImageInfoTest {
    @Test
    public void creationSuccess() {
        long timestamp = 10L;
        FakeCameraCaptureResult cameraCaptureResult = new FakeCameraCaptureResult();
        cameraCaptureResult.setTimestamp(timestamp);
        ImageInfo imageInfo = new CameraCaptureResultImageInfo(cameraCaptureResult);

        assertThat(imageInfo.getTimestamp()).isEqualTo(timestamp);
    }
}
