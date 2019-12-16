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
package androidx.camera.core;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.content.Context;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager;
import androidx.camera.testing.fakes.FakeCameraFactory;
import androidx.camera.testing.fakes.FakeCameraInfoInternal;
import androidx.camera.testing.fakes.FakeUseCaseConfig;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class CameraSelectorTest {
    private CameraInternal mRearCamera;
    private CameraInternal mFrontCamera;
    private static final String REAR_ID = "0";
    private static final String FRONT_ID = "1";
    private Set<String> mCameraIds = new HashSet<>();

    @Before
    public void setUp() throws ExecutionException, InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        CameraDeviceSurfaceManager surfaceManager = new FakeCameraDeviceSurfaceManager();
        ExtendableUseCaseConfigFactory defaultConfigFactory = new ExtendableUseCaseConfigFactory();
        defaultConfigFactory.installDefaultProvider(FakeUseCaseConfig.class,
                new ConfigProvider<FakeUseCaseConfig>() {
                    @Override
                    public FakeUseCaseConfig getConfig(@Nullable Integer lensFacing) {
                        return new FakeUseCaseConfig.Builder().getUseCaseConfig();
                    }
                });
        FakeCameraFactory cameraFactory = new FakeCameraFactory();
        mRearCamera = new FakeCamera(mock(CameraControlInternal.class),
                new FakeCameraInfoInternal(0,
                        CameraSelector.LENS_FACING_BACK));
        cameraFactory.insertCamera(CameraSelector.LENS_FACING_BACK, REAR_ID, () -> mRearCamera);
        cameraFactory.setDefaultCameraIdForLensFacing(CameraSelector.LENS_FACING_BACK, REAR_ID);
        mFrontCamera = new FakeCamera(mock(CameraControlInternal.class),
                new FakeCameraInfoInternal(0,
                        CameraSelector.LENS_FACING_FRONT));
        cameraFactory.insertCamera(CameraSelector.LENS_FACING_FRONT, FRONT_ID, () -> mFrontCamera);
        cameraFactory.setDefaultCameraIdForLensFacing(CameraSelector.LENS_FACING_FRONT, FRONT_ID);
        CameraXConfig.Builder appConfigBuilder =
                new CameraXConfig.Builder()
                        .setCameraFactory(cameraFactory)
                        .setDeviceSurfaceManager(surfaceManager)
                        .setUseCaseConfigFactory(defaultConfigFactory);
        CameraX.initialize(context, appConfigBuilder.build()).get();
        mCameraIds.add(REAR_ID);
        mCameraIds.add(FRONT_ID);
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        CameraX.shutdown().get();
    }

    @Test
    public void canSelectWithLensFacing() {
        CameraSelector.Builder cameraSelectorBuilder = new CameraSelector.Builder();
        cameraSelectorBuilder.requireLensFacing(CameraSelector.LENS_FACING_BACK);
        String result = cameraSelectorBuilder.build().select(mCameraIds);
        assertThat(result).isEqualTo(REAR_ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void exception_ifNoAvailableCamera() {
        CameraSelector.Builder cameraSelectorBuilder = new CameraSelector.Builder();
        cameraSelectorBuilder.requireLensFacing(CameraSelector.LENS_FACING_BACK).requireLensFacing(
                CameraSelector.LENS_FACING_FRONT);
        cameraSelectorBuilder.build().select(mCameraIds);
    }

    @Test
    public void canGetLensFacing() {
        CameraSelector.Builder cameraSelectorBuilder = new CameraSelector.Builder();
        cameraSelectorBuilder.requireLensFacing(CameraSelector.LENS_FACING_BACK);
        assertThat(cameraSelectorBuilder.build().getLensFacing()).isEqualTo(
                CameraSelector.LENS_FACING_BACK);
    }

    @Test(expected = IllegalStateException.class)
    public void exception_ifGetLensFacingConflicted() {
        CameraSelector.Builder cameraSelectorBuilder = new CameraSelector.Builder();
        cameraSelectorBuilder.requireLensFacing(CameraSelector.LENS_FACING_BACK).requireLensFacing(
                CameraSelector.LENS_FACING_FRONT);
        cameraSelectorBuilder.build().getLensFacing();
    }

    @Test
    public void canAppendFilters() {
        CameraIdFilter filter0 = mock(CameraIdFilter.class);
        CameraIdFilter filter1 = mock(CameraIdFilter.class);
        CameraIdFilter filter2 = mock(CameraIdFilter.class);

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .appendFilter(filter0)
                .appendFilter(filter1)
                .appendFilter(filter2)
                .build();

        assertThat(cameraSelector.getCameraFilterSet()).containsAtLeast(filter0, filter1, filter2);
    }

    @Test
    public void canSelectDefaultBackCamera() {
        assertThat(CameraSelector.DEFAULT_BACK_CAMERA.select(mCameraIds)).isEqualTo(REAR_ID);
    }

    @Test
    public void canSelectDefaultFrontCamera() {
        assertThat(CameraSelector.DEFAULT_FRONT_CAMERA.select(mCameraIds)).isEqualTo(FRONT_ID);
    }
}
