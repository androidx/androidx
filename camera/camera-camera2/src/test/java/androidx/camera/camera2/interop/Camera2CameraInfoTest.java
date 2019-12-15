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

package androidx.camera.camera2.interop;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.os.Build;

import androidx.annotation.experimental.UseExperimental;
import androidx.camera.camera2.internal.Camera2CameraInfoImpl;
import androidx.camera.core.CameraInfo;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@UseExperimental(markerClass = ExperimentalCamera2Interop.class)
public final class Camera2CameraInfoTest {

    @Test
    public void canExtractId_fromCamera2CameraInfo() {
        String cameraId = "42";
        Camera2CameraInfoImpl impl = mock(Camera2CameraInfoImpl.class);
        when(impl.getCameraId()).thenAnswer(ignored -> cameraId);

        String extractedId = Camera2CameraInfo.extractCameraId(impl);

        assertThat(extractedId).isEqualTo(cameraId);
    }

    @Test(expected = IllegalStateException.class)
    public void extractIdThrows_whenNotCamera2Impl() {
        CameraInfo info = mock(CameraInfo.class);

        Camera2CameraInfo.extractCameraId(info);
    }

}
