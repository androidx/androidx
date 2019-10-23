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

import static junit.framework.Assert.assertFalse;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import android.app.Instrumentation;
import android.content.Context;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager;
import androidx.camera.testing.fakes.FakeCameraFactory;
import androidx.camera.testing.fakes.FakeCameraInfoInternal;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.camera.testing.fakes.FakeUseCase;
import androidx.camera.testing.fakes.FakeUseCaseConfig;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@MediumTest
@RunWith(AndroidJUnit4.class)
public final class CameraXTest {
    private static final LensFacing CAMERA_LENS_FACING = LensFacing.BACK;
    private static final String CAMERA_ID = "0";

    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private Context mContext;
    private String mCameraId;
    private BaseCamera mCamera;
    private FakeLifecycleOwner mLifecycle;
    private AppConfig.Builder mAppConfigBuilder;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();

        CameraDeviceSurfaceManager surfaceManager = new FakeCameraDeviceSurfaceManager();
        ExtendableUseCaseConfigFactory defaultConfigFactory = new ExtendableUseCaseConfigFactory();
        defaultConfigFactory.installDefaultProvider(FakeUseCaseConfig.class,
                new ConfigProvider<FakeUseCaseConfig>() {
                    @Override
                    public FakeUseCaseConfig getConfig(CameraX.LensFacing lensFacing) {
                        return new FakeUseCaseConfig.Builder().build();
                    }
                });
        FakeCameraFactory cameraFactory = new FakeCameraFactory();
        mCamera = new FakeCamera(mock(CameraControlInternal.class), new FakeCameraInfoInternal(0,
                CAMERA_LENS_FACING));
        cameraFactory.insertCamera(CAMERA_LENS_FACING, CAMERA_ID, () -> mCamera);
        cameraFactory.setDefaultCameraIdForLensFacing(CAMERA_LENS_FACING, CAMERA_ID);
        mAppConfigBuilder =
                new AppConfig.Builder()
                        .setCameraFactory(cameraFactory)
                        .setDeviceSurfaceManager(surfaceManager)
                        .setUseCaseConfigFactory(defaultConfigFactory);

        mLifecycle = new FakeLifecycleOwner();

        mCameraId = cameraFactory.cameraIdForLensFacing(CAMERA_LENS_FACING);
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        if (CameraX.isInitialized()) {
            mInstrumentation.runOnMainSync(CameraX::unbindAll);
        }

        CameraX.shutdown().get();
    }

    @Test
    public void initDeinit_success() throws ExecutionException, InterruptedException {
        CameraX.initialize(mContext, mAppConfigBuilder.build()).get();
        assertThat(CameraX.isInitialized()).isTrue();

        CameraX.shutdown().get();
        assertThat(CameraX.isInitialized()).isFalse();
    }

    @Test
    public void failInit_shouldInDeinitState() throws InterruptedException {
        // Create an empty config to cause a failed init.
        AppConfig appConfig = new AppConfig.Builder().build();
        Exception exception = null;
        try {
            CameraX.initialize(mContext, appConfig).get();
        } catch (ExecutionException e) {
            exception = e;
        }
        assertThat(exception).isInstanceOf(ExecutionException.class);
        assertThat(CameraX.isInitialized()).isFalse();
    }

    @Test
    public void reinit_success() throws ExecutionException, InterruptedException {
        CameraX.initialize(mContext, mAppConfigBuilder.build()).get();
        assertThat(CameraX.isInitialized()).isTrue();

        CameraX.shutdown().get();
        assertThat(CameraX.isInitialized()).isFalse();

        CameraX.initialize(mContext, mAppConfigBuilder.build()).get();
        assertThat(CameraX.isInitialized()).isTrue();
    }

    @Test
    public void reinit_withPreviousFailedInit() throws ExecutionException, InterruptedException {
        // Create an empty config to cause a failed init.
        AppConfig appConfig = new AppConfig.Builder().build();
        Exception exception = null;
        try {
            CameraX.initialize(mContext, appConfig).get();
        } catch (ExecutionException e) {
            exception = e;
        }
        assertThat(exception).isInstanceOf(ExecutionException.class);

        CameraX.initialize(mContext, mAppConfigBuilder.build()).get();
        assertThat(CameraX.isInitialized()).isTrue();
    }

    @Test
    public void initDeinit_withDirectExecutor() {
        mAppConfigBuilder.setCameraExecutor(CameraXExecutors.directExecutor());

        // Don't call Future.get() because its behavior should be the same as synchronous call.
        CameraX.initialize(mContext, mAppConfigBuilder.build());
        assertThat(CameraX.isInitialized()).isTrue();

        CameraX.shutdown();
        assertThat(CameraX.isInitialized()).isFalse();
    }

    @Test
    public void initDeinit_withMultiThreadExecutor()
            throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        mAppConfigBuilder.setCameraExecutor(executorService);

        CameraX.initialize(mContext, mAppConfigBuilder.build()).get();
        assertThat(CameraX.isInitialized()).isTrue();

        CameraX.shutdown().get();
        assertThat(CameraX.isInitialized()).isFalse();

        executorService.shutdown();
    }

    @Test
    public void init_withDifferentAppConfig() {
        FakeCameraFactory cameraFactory0 = new FakeCameraFactory();
        FakeCameraFactory cameraFactory1 = new FakeCameraFactory();

        mAppConfigBuilder.setCameraFactory(cameraFactory0);
        CameraX.initialize(mContext, mAppConfigBuilder.build());

        assertThat(CameraX.getCameraFactory()).isEqualTo(cameraFactory0);

        CameraX.shutdown();

        mAppConfigBuilder.setCameraFactory(cameraFactory1);
        CameraX.initialize(mContext, mAppConfigBuilder.build());

        assertThat(CameraX.getCameraFactory()).isEqualTo(cameraFactory1);
    }

    @Test
    @UiThreadTest
    public void bind_createsNewUseCaseGroup() {
        initCameraX();
        CameraX.bindToLifecycle(mLifecycle, new FakeUseCase());
        // One observer is the use case group. The other observer removes the use case upon the
        // lifecycle's destruction.
        assertThat(mLifecycle.getObserverCount()).isEqualTo(2);
    }

    @Test
    @UiThreadTest
    public void bindMultipleUseCases() {
        initCameraX();
        FakeUseCaseConfig config0 =
                new FakeUseCaseConfig.Builder().setTargetName("config0").build();
        FakeUseCase fakeUseCase = new FakeUseCase(config0);
        FakeOtherUseCaseConfig config1 =
                new FakeOtherUseCaseConfig.Builder().setTargetName("config1").build();
        FakeOtherUseCase fakeOtherUseCase = new FakeOtherUseCase(config1);

        CameraX.bindToLifecycle(mLifecycle, fakeUseCase, fakeOtherUseCase);

        assertThat(CameraX.isBound(fakeUseCase)).isTrue();
        assertThat(CameraX.isBound(fakeOtherUseCase)).isTrue();
    }

    @Test
    @UiThreadTest
    public void isNotBound_afterUnbind() {
        initCameraX();
        FakeUseCase fakeUseCase = new FakeUseCase();
        CameraX.bindToLifecycle(mLifecycle, fakeUseCase);

        CameraX.unbind(fakeUseCase);
        assertThat(CameraX.isBound(fakeUseCase)).isFalse();
    }

    @Test
    @UiThreadTest
    public void bind_createsDifferentUseCaseGroups_forDifferentLifecycles() {
        initCameraX();
        FakeUseCaseConfig config0 =
                new FakeUseCaseConfig.Builder().setTargetName("config0").build();
        CameraX.bindToLifecycle(mLifecycle, new FakeUseCase(config0));

        FakeUseCaseConfig config1 =
                new FakeUseCaseConfig.Builder().setTargetName("config1").build();
        FakeLifecycleOwner anotherLifecycle = new FakeLifecycleOwner();
        CameraX.bindToLifecycle(anotherLifecycle, new FakeUseCase(config1));

        // One observer is the use case group. The other observer removes the use case upon the
        // lifecycle's destruction.
        assertThat(mLifecycle.getObserverCount()).isEqualTo(2);
        assertThat(anotherLifecycle.getObserverCount()).isEqualTo(2);
    }

    @Test(expected = IllegalArgumentException.class)
    @UiThreadTest
    public void exception_withDestroyedLifecycle() {
        initCameraX();
        FakeUseCase useCase = new FakeUseCase();

        mLifecycle.destroy();

        CameraX.bindToLifecycle(mLifecycle, useCase);
    }

    @Test
    @UiThreadTest
    public void noException_bindUseCases_withDifferentLensFacing() {
        initCameraX();
        FakeUseCaseConfig config0 =
                new FakeUseCaseConfig.Builder().setLensFacing(LensFacing.FRONT).build();
        FakeUseCase fakeUseCase = new FakeUseCase(config0);
        FakeOtherUseCaseConfig config1 =
                new FakeOtherUseCaseConfig.Builder().setLensFacing(LensFacing.BACK).build();
        FakeOtherUseCase fakeOtherUseCase = new FakeOtherUseCase(config1);

        boolean hasException = false;
        try {
            CameraX.bindToLifecycle(mLifecycle, fakeUseCase, fakeOtherUseCase);
        } catch (IllegalArgumentException e) {
            hasException = true;
        }
        assertFalse(hasException);
    }

    @Test
    public void requestingDefaultConfiguration_returnsDefaultConfiguration() {
        initCameraX();
        // Requesting a default configuration will throw if CameraX is not initialized.
        FakeUseCaseConfig config = CameraX.getDefaultUseCaseConfig(
                FakeUseCaseConfig.class, CAMERA_LENS_FACING);
        assertThat(config).isNotNull();
        assertThat(config.getTargetClass(null)).isEqualTo(FakeUseCase.class);
    }

    @Test
    @UiThreadTest
    public void attachCameraControl_afterBindToLifecycle() {
        initCameraX();
        FakeUseCaseConfig config0 =
                new FakeUseCaseConfig.Builder().setTargetName("config0").build();
        AttachCameraFakeCase fakeUseCase = new AttachCameraFakeCase(config0);

        CameraX.bindToLifecycle(mLifecycle, fakeUseCase);

        assertThat(fakeUseCase.getCameraControl(mCameraId)).isEqualTo(
                mCamera.getCameraControlInternal());
    }

    @Test
    @UiThreadTest
    public void onCameraControlReadyIsCalled_afterBindToLifecycle() {
        initCameraX();
        FakeUseCaseConfig config0 =
                new FakeUseCaseConfig.Builder().setTargetName("config0").build();
        AttachCameraFakeCase fakeUseCase = spy(new AttachCameraFakeCase(config0));

        CameraX.bindToLifecycle(mLifecycle, fakeUseCase);

        Mockito.verify(fakeUseCase).onCameraControlReady(mCameraId);
    }

    @Test
    @UiThreadTest
    public void detachCameraControl_afterUnbind() {
        initCameraX();
        FakeUseCaseConfig config0 =
                new FakeUseCaseConfig.Builder().setTargetName("config0").build();
        AttachCameraFakeCase fakeUseCase = new AttachCameraFakeCase(config0);
        CameraX.bindToLifecycle(mLifecycle, fakeUseCase);

        CameraX.unbind(fakeUseCase);

        // after unbind, Camera's CameraControlInternal should be detached from Usecase
        assertThat(fakeUseCase.getCameraControl(mCameraId)).isNotEqualTo(
                mCamera.getCameraControlInternal());
        // UseCase still gets a non-null default CameraControlInternal that does nothing.
        assertThat(fakeUseCase.getCameraControl(mCameraId)).isEqualTo(
                CameraControlInternal.DEFAULT_EMPTY_INSTANCE);
    }

    @Test
    @UiThreadTest
    public void eventCallbackCalled_bindAndUnbind() {
        initCameraX();
        UseCase.EventCallback eventCallback = Mockito.mock(UseCase.EventCallback.class);

        FakeUseCaseConfig.Builder fakeConfigBuilder = new FakeUseCaseConfig.Builder();
        fakeConfigBuilder.setUseCaseEventCallback(eventCallback);
        AttachCameraFakeCase fakeUseCase = new AttachCameraFakeCase(fakeConfigBuilder.build());

        CameraX.bindToLifecycle(mLifecycle, fakeUseCase);
        Mockito.verify(eventCallback).onBind(mCameraId);

        CameraX.unbind(fakeUseCase);
        Mockito.verify(eventCallback).onUnbind();
    }

    @Test
    public void canRetrieveCameraInfo() throws CameraInfoUnavailableException {
        initCameraX();
        String cameraId = CameraX.getCameraWithLensFacing(CAMERA_LENS_FACING);
        CameraInfoInternal cameraInfoInternal = CameraX.getCameraInfo(cameraId);
        assertThat(cameraInfoInternal).isNotNull();
        assertThat(cameraInfoInternal.getLensFacing()).isEqualTo(CAMERA_LENS_FACING);
    }

    @Test
    public void canGetCameraXContext() {
        initCameraX();
        Context context = CameraX.getContext();
        assertThat(context).isNotNull();
    }

    @Test
    @UiThreadTest
    public void canGetActiveUseCases_afterBindToLifecycle() {
        initCameraX();
        FakeUseCaseConfig config0 =
                new FakeUseCaseConfig.Builder().setTargetName("config0").build();
        FakeUseCase fakeUseCase = new FakeUseCase(config0);
        FakeOtherUseCaseConfig config1 =
                new FakeOtherUseCaseConfig.Builder().setTargetName("config1").build();
        FakeOtherUseCase fakeOtherUseCase = new FakeOtherUseCase(config1);

        CameraX.bindToLifecycle(mLifecycle, fakeUseCase, fakeOtherUseCase);
        mLifecycle.startAndResume();

        Collection<UseCase> useCases = CameraX.getActiveUseCases();

        assertThat(useCases.contains(fakeUseCase)).isTrue();
        assertThat(useCases.contains(fakeOtherUseCase)).isTrue();
    }

    @Test
    public void canGetCameraIdWithConfig() throws CameraInfoUnavailableException {
        initCameraX();
        FakeUseCaseConfig.Builder fakeConfigBuilder = new FakeUseCaseConfig.Builder();
        fakeConfigBuilder.setLensFacing(CAMERA_LENS_FACING);
        String cameraId = CameraX.getCameraWithCameraDeviceConfig(fakeConfigBuilder.build());

        assertThat(cameraId).isEqualTo(mCameraId);
    }

    @Test(expected = CameraInfoUnavailableException.class)
    public void cameraInfo_returnFlashAvailableFailed_forFrontCamera()
            throws CameraInfoUnavailableException {
        initCameraX();
        CameraInfo cameraInfo = CameraX.getCameraInfo(CameraX.LensFacing.FRONT);
        cameraInfo.isFlashAvailable();
    }

    private void initCameraX() {
        CameraX.initialize(mContext, mAppConfigBuilder.build());
    }

    /** FakeUseCase that will call attachToCamera */
    public static class AttachCameraFakeCase extends FakeUseCase {

        public AttachCameraFakeCase(FakeUseCaseConfig config) {
            super(config);
        }

        @Override
        @NonNull
        protected Map<String, Size> onSuggestedResolutionUpdated(
                @NonNull Map<String, Size> suggestedResolutionMap) {

            SessionConfig.Builder builder = new SessionConfig.Builder();

            UseCaseConfig<?> config = getUseCaseConfig();
            String cameraId = getCameraIdUnchecked(config);
            attachToCamera(cameraId, builder.build());
            return suggestedResolutionMap;
        }
    }
}
