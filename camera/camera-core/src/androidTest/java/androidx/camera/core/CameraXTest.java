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

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.camera.core.impl.CameraFactory;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.testing.fakes.FakeAppConfig;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraInfoInternal;
import androidx.camera.testing.impl.fakes.FakeCameraFactory;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@LargeTest
@RunWith(AndroidJUnit4.class)
public final class CameraXTest {

    private static final String CAMERA_ID_0 = "0";
    private static final String CAMERA_ID_1 = "1";
    private Context mContext;
    private CameraXConfig.Builder mConfigBuilder;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mConfigBuilder = CameraXConfig.Builder.fromConfig(FakeAppConfig.create());
    }

    @Test
    public void initDeinit_success() throws ExecutionException, InterruptedException {
        CameraX cameraX = new CameraX(mContext, () -> mConfigBuilder.build());
        cameraX.getInitializeFuture().get();
        assertThat(cameraX.isInitialized()).isTrue();

        cameraX.shutdown().get();
        assertThat(cameraX.isInitialized()).isFalse();
    }

    @Test
    public void failInit_shouldInDeinitState() throws InterruptedException {
        // Create an empty config to cause a failed init.
        CameraXConfig cameraXConfig = new CameraXConfig.Builder().build();
        CameraX cameraX = new CameraX(mContext, () -> cameraXConfig);
        Exception exception = null;
        try {
            cameraX.getInitializeFuture().get();
        } catch (ExecutionException e) {
            exception = e;
        }
        assertThat(exception).isInstanceOf(ExecutionException.class);
        assertThat(cameraX.isInitialized()).isFalse();
    }

    @Test
    public void reinit_success() throws ExecutionException, InterruptedException {
        CameraX cameraX = new CameraX(mContext, () -> mConfigBuilder.build());
        cameraX.getInitializeFuture().get();
        assertThat(cameraX.isInitialized()).isTrue();

        cameraX.shutdown().get();
        assertThat(cameraX.isInitialized()).isFalse();

        CameraX cameraX2 = new CameraX(mContext, () -> mConfigBuilder.build());
        cameraX2.getInitializeFuture().get();
        assertThat(cameraX2.isInitialized()).isTrue();

        cameraX2.shutdown().get();
    }

    @Test
    public void initDeinit_withDirectExecutor() {
        mConfigBuilder.setCameraExecutor(CameraXExecutors.directExecutor());

        // Don't call Future.get() because its behavior should be the same as synchronous call.
        CameraX cameraX = new CameraX(mContext, () -> mConfigBuilder.build());
        assertThat(cameraX.isInitialized()).isTrue();

        cameraX.shutdown();
        assertThat(cameraX.isInitialized()).isFalse();
    }

    @Test
    public void initDeinit_withMultiThreadExecutor()
            throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        mConfigBuilder.setCameraExecutor(executorService);

        CameraX cameraX = new CameraX(mContext, () -> mConfigBuilder.build());
        cameraX.getInitializeFuture().get();

        cameraX.shutdown().get();
        assertThat(cameraX.isInitialized()).isFalse();

        executorService.shutdown();
    }

    @Test
    public void init_withDifferentCameraXConfig() throws ExecutionException, InterruptedException {
        CameraFactory cameraFactory0 = createFakeCameraFactory();
        CameraFactory.Provider cameraFactoryProvider0 =
                (ignored0, ignored1, ignored2, ignored3, ignored4, ignored5) -> cameraFactory0;
        CameraFactory cameraFactory1 = createFakeCameraFactory();
        CameraFactory.Provider cameraFactoryProvider1 =
                (ignored0, ignored1, ignored2, ignored3, ignored4, ignored5) -> cameraFactory1;

        mConfigBuilder.setCameraFactoryProvider(cameraFactoryProvider0);
        CameraX cameraX0 = new CameraX(mContext, () -> mConfigBuilder.build());
        cameraX0.getInitializeFuture().get();

        assertThat(cameraX0.getCameraFactory()).isEqualTo(cameraFactory0);

        cameraX0.shutdown().get();

        mConfigBuilder.setCameraFactoryProvider(cameraFactoryProvider1);
        CameraX cameraX1 = new CameraX(mContext, () -> mConfigBuilder.build());
        cameraX1.getInitializeFuture().get();

        assertThat(cameraX1.getCameraFactory()).isEqualTo(cameraFactory1);

        cameraX1.shutdown().get();
    }

    @Test
    public void minLogLevelIsCorrectlySetAndReset()
            throws ExecutionException, InterruptedException, TimeoutException {
        mConfigBuilder.setMinimumLoggingLevel(Log.ERROR);
        CameraX cameraX = new CameraX(mContext, () -> mConfigBuilder.build());
        cameraX.getInitializeFuture().get(10000, TimeUnit.MILLISECONDS);

        assertThat(Logger.getMinLogLevel()).isEqualTo(Log.ERROR);

        cameraX.shutdown().get(10000, TimeUnit.MILLISECONDS);

        assertThat(Logger.getMinLogLevel()).isEqualTo(Log.DEBUG);
    }

    @Test
    public void minLogLevelIsCorrectlySetAndReset_whenSettingMultipleTimes()
            throws ExecutionException, InterruptedException, TimeoutException {
        mConfigBuilder.setMinimumLoggingLevel(Log.INFO);
        CameraX cameraX1 = new CameraX(mContext, () -> mConfigBuilder.build());
        cameraX1.getInitializeFuture().get(10000, TimeUnit.MILLISECONDS);

        // Checks whether minimum log level is correctly set by the first CameraX instance
        assertThat(Logger.getMinLogLevel()).isEqualTo(Log.INFO);

        mConfigBuilder.setMinimumLoggingLevel(Log.ERROR);
        CameraX cameraX2 = new CameraX(mContext, () -> mConfigBuilder.build());
        cameraX2.getInitializeFuture().get(10000, TimeUnit.MILLISECONDS);

        // Checks whether minimum log level is correctly kept as INFO level since it is lower
        // than the target minimum log level setting of the second CameraX instance
        assertThat(Logger.getMinLogLevel()).isEqualTo(Log.INFO);

        cameraX1.shutdown().get(10000, TimeUnit.MILLISECONDS);

        // Checks whether minimum log level is correctly updated as ERROR level after the first
        // CameraX instance is shutdown
        assertThat(Logger.getMinLogLevel()).isEqualTo(Log.ERROR);

        cameraX2.shutdown().get(10000, TimeUnit.MILLISECONDS);

        // Checks whether minimum log level is correctly reset as DEBUG level after the second
        // CameraX instance is shutdown
        assertThat(Logger.getMinLogLevel()).isEqualTo(Log.DEBUG);
    }

    @Test
    public void canBeShutdownWithoutSchedulerHandler()
            throws ExecutionException, InterruptedException, TimeoutException {
        // Makes sure that the fake config does not contain a scheduler handler in it
        assertThat(mConfigBuilder.build().getSchedulerHandler(null)).isNull();

        // Creates a CameraX instance with the config
        CameraX cameraX = new CameraX(mContext, () -> mConfigBuilder.build());

        // Waits for the created CameraX instance being initialized completely.
        cameraX.getInitializeFuture().get(10000, TimeUnit.MILLISECONDS);

        // Waits for the CameraX instance being shutdown successfully.
        cameraX.shutdown().get(10000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void canBeShutdownWithSchedulerHandler()
            throws ExecutionException, InterruptedException, TimeoutException {
        // Adds a scheduler handler to the config builder
        Handler handler = new Handler(Looper.getMainLooper());
        mConfigBuilder.setSchedulerHandler(handler);

        // Creates a CameraX instance with the config
        CameraX cameraX = new CameraX(mContext, () -> mConfigBuilder.build());

        // Waits for the created CameraX instance being initialized completely.
        cameraX.getInitializeFuture().get(10000, TimeUnit.MILLISECONDS);

        // Waits for the CameraX instance being shutdown successfully.
        cameraX.shutdown().get(10000, TimeUnit.MILLISECONDS);
    }

    private CameraFactory createFakeCameraFactory() {
        FakeCameraFactory cameraFactory = new FakeCameraFactory();
        cameraFactory.insertCamera(CameraSelector.LENS_FACING_BACK, CAMERA_ID_0,
                () -> new FakeCamera(CAMERA_ID_0, null,
                        new FakeCameraInfoInternal(CAMERA_ID_0, 0,
                                CameraSelector.LENS_FACING_BACK)));
        cameraFactory.insertCamera(CameraSelector.LENS_FACING_FRONT, CAMERA_ID_1,
                () -> new FakeCamera(CAMERA_ID_1, null,
                        new FakeCameraInfoInternal(CAMERA_ID_1, 0,
                                CameraSelector.LENS_FACING_FRONT)));
        return cameraFactory;
    }
}