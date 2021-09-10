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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.os.Build;
import android.util.Log;

import androidx.camera.core.impl.CameraDeviceSurfaceManager;
import androidx.camera.core.impl.CameraFactory;
import androidx.camera.testing.fakes.FakeAppConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.concurrent.Executor;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class CameraXConfigTest {

    private CameraXConfig mCameraXConfig;

    @Before
    public void setUp() {
        mCameraXConfig = FakeAppConfig.create();
    }

    @Test
    public void canGetConfigTarget() {
        Class<CameraX> configTarget = mCameraXConfig.getTargetClass(/*valueIfMissing=*/ null);
        assertThat(configTarget).isEqualTo(CameraX.class);
    }

    @Test
    public void canGetCameraFactoryProvider() {
        CameraFactory.Provider cameraFactoryProvider = mCameraXConfig.getCameraFactoryProvider(
                /*valueIfMissing=*/ null);
        assertThat(cameraFactoryProvider).isInstanceOf(CameraFactory.Provider.class);
    }

    @Test
    public void canGetDeviceSurfaceManagerProvider() {
        CameraDeviceSurfaceManager.Provider surfaceManagerProvider =
                mCameraXConfig.getDeviceSurfaceManagerProvider(/*valueIfMissing=*/ null);
        assertThat(surfaceManagerProvider).isInstanceOf(
                CameraDeviceSurfaceManager.Provider.class);
    }

    @Test
    public void canGetCameraExecutor() {
        Executor mockExecutor = mock(Executor.class);
        CameraXConfig cameraXConfig = new CameraXConfig.Builder()
                .setCameraExecutor(mockExecutor)
                .build();
        Executor cameraExecutor = cameraXConfig.getCameraExecutor(/*valueIfMissing=*/ null);
        assertThat(cameraExecutor).isEqualTo(mockExecutor);
    }

    @Test
    public void canGetMinimumLoggingLevel() {
        final CameraXConfig cameraXConfig = new CameraXConfig.Builder()
                .setMinimumLoggingLevel(Log.WARN)
                .build();

        final Integer minLoggingLevel = cameraXConfig.getMinimumLoggingLevel();
        assertThat(minLoggingLevel).isEqualTo(Log.WARN);
    }

    @Test
    public void canGetDefaultMinimumLoggingLevel() {
        final CameraXConfig cameraXConfig = new CameraXConfig.Builder().build();

        final Integer minLoggingLevel = cameraXConfig.getMinimumLoggingLevel();
        assertThat(minLoggingLevel).isEqualTo(Logger.DEFAULT_MIN_LOG_LEVEL);
    }

    @Test
    public void canGetAvailableCamerasSelector() {
        CameraSelector cameraSelector = new CameraSelector.Builder().build();
        CameraXConfig cameraXConfig = new CameraXConfig.Builder()
                .setAvailableCamerasLimiter(cameraSelector)
                .build();
        assertThat(cameraXConfig.getAvailableCamerasLimiter(null)).isEqualTo(cameraSelector);
    }
}
