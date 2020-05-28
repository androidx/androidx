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
import static junit.framework.TestCase.assertSame;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import android.app.Instrumentation;
import android.content.Context;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.CameraFactory;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.ExtendableUseCaseConfigFactory;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager;
import androidx.camera.testing.fakes.FakeCameraFactory;
import androidx.camera.testing.fakes.FakeCameraInfoInternal;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.camera.testing.fakes.FakeUseCase;
import androidx.camera.testing.fakes.FakeUseCaseConfig;
import androidx.core.util.Preconditions;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@MediumTest
@RunWith(AndroidJUnit4.class)
public final class CameraXTest {
    @CameraSelector.LensFacing
    private static final int CAMERA_LENS_FACING = CameraSelector.LENS_FACING_BACK;
    @CameraSelector.LensFacing
    private static final int CAMERA_LENS_FACING_FRONT = CameraSelector.LENS_FACING_FRONT;
    private static final CameraSelector CAMERA_SELECTOR =
            new CameraSelector.Builder().requireLensFacing(CAMERA_LENS_FACING).build();
    private static final String CAMERA_ID = "0";
    private static final String CAMERA_ID_FRONT = "1";

    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private Context mContext;
    private CameraInternal mCameraInternal;
    private FakeLifecycleOwner mLifecycle;
    private CameraXConfig.Builder mConfigBuilder;
    private FakeCameraFactory mFakeCameraFactory;
    private UseCaseConfigFactory mUseCaseConfigFactory;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();

        ExtendableUseCaseConfigFactory defaultConfigFactory = new ExtendableUseCaseConfigFactory();
        defaultConfigFactory.installDefaultProvider(FakeUseCaseConfig.class,
                cameraInfo -> new FakeUseCaseConfig.Builder().getUseCaseConfig());
        mUseCaseConfigFactory = defaultConfigFactory;
        mFakeCameraFactory = new FakeCameraFactory();
        mCameraInternal = new FakeCamera(mock(CameraControlInternal.class),
                new FakeCameraInfoInternal(0, CAMERA_LENS_FACING));
        mFakeCameraFactory.insertCamera(CAMERA_LENS_FACING, CAMERA_ID, () -> mCameraInternal);
        mConfigBuilder =
                new CameraXConfig.Builder()
                        .setCameraFactoryProvider((ignored0, ignored1) -> mFakeCameraFactory)
                        .setDeviceSurfaceManagerProvider(ignored ->
                                new FakeCameraDeviceSurfaceManager())
                        .setUseCaseConfigFactoryProvider(ignored -> mUseCaseConfigFactory);

        mLifecycle = new FakeLifecycleOwner();
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
        CameraX.initialize(mContext, mConfigBuilder.build()).get();
        assertThat(CameraX.isInitialized()).isTrue();

        CameraX.shutdown().get();
        assertThat(CameraX.isInitialized()).isFalse();
    }

    @Test
    public void failInit_shouldInDeinitState() throws InterruptedException {
        // Create an empty config to cause a failed init.
        CameraXConfig cameraXConfig = new CameraXConfig.Builder().build();
        Exception exception = null;
        try {
            CameraX.initialize(mContext, cameraXConfig).get();
        } catch (ExecutionException e) {
            exception = e;
        }
        assertThat(exception).isInstanceOf(ExecutionException.class);
        assertThat(CameraX.isInitialized()).isFalse();
    }

    @Test
    public void reinit_success() throws ExecutionException, InterruptedException {
        CameraX.initialize(mContext, mConfigBuilder.build()).get();
        assertThat(CameraX.isInitialized()).isTrue();

        CameraX.shutdown().get();
        assertThat(CameraX.isInitialized()).isFalse();

        CameraX.initialize(mContext, mConfigBuilder.build()).get();
        assertThat(CameraX.isInitialized()).isTrue();
    }

    @Test
    public void reinit_withPreviousFailedInit() throws ExecutionException, InterruptedException {
        // Create an empty config to cause a failed init.
        CameraXConfig cameraXConfig = new CameraXConfig.Builder().build();
        Exception exception = null;
        try {
            CameraX.initialize(mContext, cameraXConfig).get();
        } catch (ExecutionException e) {
            exception = e;
        }
        assertThat(exception).isInstanceOf(ExecutionException.class);

        CameraX.initialize(mContext, mConfigBuilder.build()).get();
        assertThat(CameraX.isInitialized()).isTrue();
    }

    @Test
    public void initDeinit_withDirectExecutor() {
        mConfigBuilder.setCameraExecutor(CameraXExecutors.directExecutor());

        // Don't call Future.get() because its behavior should be the same as synchronous call.
        CameraX.initialize(mContext, mConfigBuilder.build());
        assertThat(CameraX.isInitialized()).isTrue();

        CameraX.shutdown();
        assertThat(CameraX.isInitialized()).isFalse();
    }

    @Test
    public void initDeinit_withMultiThreadExecutor()
            throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        mConfigBuilder.setCameraExecutor(executorService);

        CameraX.initialize(mContext, mConfigBuilder.build()).get();
        assertThat(CameraX.isInitialized()).isTrue();

        CameraX.shutdown().get();
        assertThat(CameraX.isInitialized()).isFalse();

        executorService.shutdown();
    }

    @Test
    public void init_withDifferentCameraXConfig() {
        CameraFactory cameraFactory0 = new FakeCameraFactory();
        CameraFactory.Provider cameraFactoryProvider0 = (ignored0, ignored1) -> cameraFactory0;
        CameraFactory cameraFactory1 = new FakeCameraFactory();
        CameraFactory.Provider cameraFactoryProvider1 = (ignored0, ignored1) -> cameraFactory1;

        mConfigBuilder.setCameraFactoryProvider(cameraFactoryProvider0);
        CameraX.initialize(mContext, mConfigBuilder.build());

        assertThat(CameraX.getCameraFactory()).isEqualTo(cameraFactory0);

        CameraX.shutdown();

        mConfigBuilder.setCameraFactoryProvider(cameraFactoryProvider1);
        CameraX.initialize(mContext, mConfigBuilder.build());

        assertThat(CameraX.getCameraFactory()).isEqualTo(cameraFactory1);
    }

    @Test
    @UiThreadTest
    public void bind_createsNewUseCaseMediator() {
        initCameraX();
        CameraX.bindToLifecycle(mLifecycle, CAMERA_SELECTOR, new FakeUseCase());
        // One observer is the use case mediator. The other observer removes the use case upon the
        // lifecycle's destruction.
        assertThat(mLifecycle.getObserverCount()).isEqualTo(2);
    }

    @Test
    @UiThreadTest
    public void bindMultipleUseCases() {
        initCameraX();
        FakeUseCase fakeUseCase = new FakeUseCaseConfig.Builder().setTargetName("config0").build();
        FakeOtherUseCase fakeOtherUseCase = new FakeOtherUseCaseConfig.Builder().setTargetName(
                "config1").build();

        CameraX.bindToLifecycle(mLifecycle, CAMERA_SELECTOR, fakeUseCase, fakeOtherUseCase);

        assertThat(CameraX.isBound(fakeUseCase)).isTrue();
        assertThat(CameraX.isBound(fakeOtherUseCase)).isTrue();
    }

    @Test
    @UiThreadTest
    public void isNotBound_afterUnbind() {
        initCameraX();
        FakeUseCase fakeUseCase = new FakeUseCase();
        CameraX.bindToLifecycle(mLifecycle, CAMERA_SELECTOR, fakeUseCase);

        CameraX.unbind(fakeUseCase);
        assertThat(CameraX.isBound(fakeUseCase)).isFalse();
    }

    @Test
    @UiThreadTest
    public void bind_createsDifferentUseCaseMediators_forDifferentLifecycles() {
        initCameraX();
        CameraX.bindToLifecycle(mLifecycle, CAMERA_SELECTOR,
                new FakeUseCaseConfig.Builder().setTargetName("config0").build());

        FakeLifecycleOwner anotherLifecycle = new FakeLifecycleOwner();
        CameraX.bindToLifecycle(anotherLifecycle, CAMERA_SELECTOR,
                new FakeUseCaseConfig.Builder().setTargetName("config1").build());

        // One observer is the use case mediator. The other observer removes the use case upon the
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

        CameraX.bindToLifecycle(mLifecycle, CAMERA_SELECTOR, useCase);
    }

    @Test
    @UiThreadTest
    public void bind_returnTheSameCameraForSameSelector() {
        // This test scope does not include the Extension, so we only bind a fake use case with a
        // simple lensFacing selector.
        initCameraX();
        Camera camera1 = CameraX.bindToLifecycle(mLifecycle, CAMERA_SELECTOR, new FakeUseCase());
        Camera camera2 = CameraX.bindToLifecycle(mLifecycle, CAMERA_SELECTOR, new FakeUseCase());

        assertSame(camera1, camera2);
    }

    @Test
    @UiThreadTest
    public void noException_bindUseCases_withDifferentLensFacing() {
        // Initial the front camera for this test.
        CameraInternal cameraInternalFront =
                new FakeCamera(mock(CameraControlInternal.class),
                        new FakeCameraInfoInternal(0, CAMERA_LENS_FACING_FRONT));
        mFakeCameraFactory.insertCamera(CAMERA_LENS_FACING_FRONT, CAMERA_ID_FRONT,
                () -> cameraInternalFront);
        CameraXConfig.Builder appConfigBuilder =
                new CameraXConfig.Builder()
                        .setCameraFactoryProvider((ignored0, ignored1) -> mFakeCameraFactory)
                        .setDeviceSurfaceManagerProvider(ignored ->
                                new FakeCameraDeviceSurfaceManager())
                        .setUseCaseConfigFactoryProvider(ignored -> mUseCaseConfigFactory);

        CameraX.initialize(mContext, appConfigBuilder.build());

        CameraSelector frontSelector =
                new CameraSelector.Builder().requireLensFacing(
                        CameraSelector.LENS_FACING_FRONT).build();
        FakeUseCase fakeUseCase = new FakeUseCaseConfig.Builder().build();
        CameraSelector backSelector =
                new CameraSelector.Builder().requireLensFacing(
                        CameraSelector.LENS_FACING_BACK).build();
        FakeOtherUseCase fakeOtherUseCase = new FakeOtherUseCaseConfig.Builder().build();

        boolean hasException = false;
        try {
            CameraX.bindToLifecycle(mLifecycle, frontSelector, fakeUseCase);
            CameraX.bindToLifecycle(mLifecycle, backSelector, fakeOtherUseCase);
        } catch (IllegalArgumentException e) {
            hasException = true;
        }
        assertFalse(hasException);
    }

    @Test
    @UiThreadTest
    public void bindUseCases_successReturnCamera() {
        initCameraX();
        FakeUseCaseConfig config0 = new FakeUseCaseConfig.Builder().getUseCaseConfig();

        assertThat(CameraX.bindToLifecycle(mLifecycle, CAMERA_SELECTOR,
                new FakeUseCase(config0))).isInstanceOf(Camera.class);
    }

    @Test(expected = IllegalArgumentException.class)
    @UiThreadTest
    public void bindUseCases_withNotExistedLensFacingCamera() {
        initCameraX();
        FakeUseCaseConfig config0 = new FakeUseCaseConfig.Builder().getUseCaseConfig();
        FakeUseCase fakeUseCase = new FakeUseCase(config0);

        // The front camera is not defined, we should get the IllegalArgumentException when it
        // tries to get the camera.
        CameraX.bindToLifecycle(mLifecycle, CameraSelector.DEFAULT_FRONT_CAMERA, fakeUseCase);
    }

    @Test
    @UiThreadTest
    public void bindUseCases_canUpdateUseCase() {
        initCameraX();
        FakeUseCaseConfig config0 = new FakeUseCaseConfig.Builder().getUseCaseConfig();
        FakeUseCase fakeUseCase = new FakeUseCase(config0);

        Camera camera = CameraX.bindToLifecycle(mLifecycle, CameraSelector.DEFAULT_BACK_CAMERA,
                fakeUseCase);

        assertThat(fakeUseCase.getCamera()).isEqualTo(camera);
    }

    @Test
    public void requestingDefaultConfiguration_returnsDefaultConfiguration() {
        initCameraX();
        // Requesting a default configuration will throw if CameraX is not initialized.
        FakeUseCaseConfig config = CameraX.getDefaultUseCaseConfig(FakeUseCaseConfig.class, null);
        assertThat(config).isNotNull();
        assertThat(config.getTargetClass(null)).isEqualTo(FakeUseCase.class);
    }

    @Test
    @UiThreadTest
    public void attachCameraControl_afterBindToLifecycle() {
        initCameraX();
        FakeUseCaseConfig config0 = new FakeUseCaseConfig.Builder().setTargetName(
                "config0").getUseCaseConfig();
        AttachCameraFakeCase fakeUseCase = new AttachCameraFakeCase(config0);

        CameraX.bindToLifecycle(mLifecycle, CAMERA_SELECTOR, fakeUseCase);

        assertThat(fakeUseCase.getCameraControl()).isEqualTo(
                mCameraInternal.getCameraControlInternal());
    }

    @Test
    @UiThreadTest
    public void onCameraControlReadyIsCalled_afterBindToLifecycle() {
        initCameraX();
        FakeUseCaseConfig config0 = new FakeUseCaseConfig.Builder().setTargetName(
                "config0").getUseCaseConfig();
        AttachCameraFakeCase fakeUseCase = spy(new AttachCameraFakeCase(config0));

        CameraX.bindToLifecycle(mLifecycle, CAMERA_SELECTOR, fakeUseCase);

        Mockito.verify(fakeUseCase).onCameraControlReady();
    }

    @Test
    @UiThreadTest
    public void detachCameraControl_afterUnbind() {
        initCameraX();
        FakeUseCaseConfig config0 = new FakeUseCaseConfig.Builder().setTargetName(
                "config0").getUseCaseConfig();
        AttachCameraFakeCase fakeUseCase = new AttachCameraFakeCase(config0);
        CameraX.bindToLifecycle(mLifecycle, CAMERA_SELECTOR, fakeUseCase);

        CameraX.unbind(fakeUseCase);

        // after unbind, Camera's CameraControlInternal should be detached from Usecase
        assertThat(fakeUseCase.getCameraControl()).isNotEqualTo(
                mCameraInternal.getCameraControlInternal());
        // UseCase still gets a non-null default CameraControlInternal that does nothing.
        assertThat(fakeUseCase.getCameraControl()).isEqualTo(
                CameraControlInternal.DEFAULT_EMPTY_INSTANCE);
    }

    @Test
    @UiThreadTest
    public void eventCallbackCalled_bindAndUnbind() {
        initCameraX();
        UseCase.EventCallback eventCallback = Mockito.mock(UseCase.EventCallback.class);

        FakeUseCaseConfig.Builder fakeConfigBuilder = new FakeUseCaseConfig.Builder();
        fakeConfigBuilder.setUseCaseEventCallback(eventCallback);
        AttachCameraFakeCase fakeUseCase = new AttachCameraFakeCase(
                fakeConfigBuilder.getUseCaseConfig());

        CameraX.bindToLifecycle(mLifecycle, CAMERA_SELECTOR, fakeUseCase);
        Mockito.verify(eventCallback).onBind(CAMERA_ID);

        CameraX.unbind(fakeUseCase);
        Mockito.verify(eventCallback).onUnbind();
    }

    @Test
    public void canRetrieveCameraInfo() {
        initCameraX();
        CameraInfoInternal cameraInfoInternal = CameraX.getCameraInfo(CAMERA_ID);
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
        FakeUseCase fakeUseCase = new FakeUseCaseConfig.Builder().setTargetName("config0").build();
        FakeOtherUseCase fakeOtherUseCase = new FakeOtherUseCaseConfig.Builder().setTargetName(
                "config1").build();

        CameraX.bindToLifecycle(mLifecycle, CAMERA_SELECTOR, fakeUseCase, fakeOtherUseCase);
        mLifecycle.startAndResume();

        Collection<UseCase> useCases = Preconditions.checkNotNull(CameraX.getActiveUseCases());

        assertThat(useCases.contains(fakeUseCase)).isTrue();
        assertThat(useCases.contains(fakeOtherUseCase)).isTrue();
    }

    @Test(expected = IllegalArgumentException.class)
    public void cameraInfo_cannotRetrieveCameraInfo_forFrontCamera() {
        initCameraX();
        // Expect throw the IllegalArgumentException when try to get the cameraInfo from the camera
        // which does not exist.
        CameraX.getCameraInfo(CAMERA_ID_FRONT);
    }

    @Test
    public void checkHasCameraTrueForExistentCamera() throws CameraInfoUnavailableException {
        initCameraX();
        assertThat(CameraX.hasCamera(
                new CameraSelector.Builder().requireLensFacing(
                        CameraSelector.LENS_FACING_BACK).build())).isTrue();
    }

    @Test
    public void checkHasCameraFalseForNonexistentCamera() throws CameraInfoUnavailableException {
        initCameraX();
        assertThat(CameraX.hasCamera(new CameraSelector.Builder().requireLensFacing(
                CameraSelector.LENS_FACING_BACK).requireLensFacing(
                CameraSelector.LENS_FACING_FRONT).build())).isFalse();
    }

    private void initCameraX() {
        CameraX.initialize(mContext, mConfigBuilder.build());
    }

    /** FakeUseCase that will call attachToCamera */
    public static class AttachCameraFakeCase extends FakeUseCase {

        AttachCameraFakeCase(FakeUseCaseConfig config) {
            super(config);
        }

        @Override
        @NonNull
        protected Size onSuggestedResolutionUpdated(@NonNull Size suggestedResolution) {
            SessionConfig.Builder builder = new SessionConfig.Builder();

            updateSessionConfig(builder.build());
            return suggestedResolution;
        }
    }
}
