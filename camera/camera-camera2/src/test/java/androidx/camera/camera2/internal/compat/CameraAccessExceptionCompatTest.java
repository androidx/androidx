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

package androidx.camera.camera2.internal.compat;

import static com.google.common.truth.Truth.assertThat;

import android.hardware.camera2.CameraAccessException;
import android.os.Build;

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
public final class CameraAccessExceptionCompatTest {

    @Test
    public void createCameraAccessExceptionCompat_byCompatError() {
        for (int error : CameraAccessExceptionCompat.COMPAT_ERRORS) {
            CameraAccessExceptionCompat cameraAccessExceptionCompat =
                    new CameraAccessExceptionCompat(error);

            assertThat(cameraAccessExceptionCompat.getReason()).isEqualTo(error);
            assertThat(cameraAccessExceptionCompat.toCameraAccessException()).isNull();
        }
    }

    @Test
    public void createCameraAccessExceptionCompat_byCamera2AccessError() {
        Throwable cause = new RuntimeException();
        for (int error : CameraAccessExceptionCompat.PLATFORM_ERRORS) {
            CameraAccessExceptionCompat cameraAccessExceptionCompat =
                    new CameraAccessExceptionCompat(error, cause);

            assertThat(cameraAccessExceptionCompat.getReason()).isEqualTo(error);

            CameraAccessException cameraAccessException =
                    cameraAccessExceptionCompat.toCameraAccessException();

            assertThat(cameraAccessException).isNotNull();
            assertThat(cameraAccessException.getReason()).isEqualTo(error);
            assertThat(cameraAccessException.getCause()).isEqualTo(cause);
        }
    }

    @Test
    public void createCameraAccessExceptionCompat_byCameraAccessException() {
        for (int error : CameraAccessExceptionCompat.PLATFORM_ERRORS) {
            CameraAccessException cameraAccessException = new CameraAccessException(error);
            CameraAccessExceptionCompat cameraAccessExceptionCompat =
                    CameraAccessExceptionCompat.toCameraAccessExceptionCompat(
                            cameraAccessException);

            assertThat(cameraAccessExceptionCompat.getReason()).isEqualTo(error);
            assertThat(cameraAccessExceptionCompat.toCameraAccessException())
                    .isEqualTo(cameraAccessException);

        }
    }
}
