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
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraX.ErrorCode;
import androidx.camera.core.CameraX.ErrorListener;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager;
import androidx.camera.testing.fakes.FakeCameraFactory;
import androidx.camera.testing.fakes.FakeCameraInfo;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.camera.testing.fakes.FakeUseCase;
import androidx.camera.testing.fakes.FakeUseCaseConfig;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@MediumTest
@RunWith(AndroidJUnit4.class)
public final class CameraXTest {
    // TODO(b/126431497): This shouldn't need to be static, but the initialization behavior does
    //  not allow us to reinitialize before each test.
    private static FakeCameraFactory sCameraFactory = new FakeCameraFactory();

    static {
        String cameraId = sCameraFactory.cameraIdForLensFacing(LensFacing.BACK);
        sCameraFactory.insertCamera(cameraId,
                new FakeCamera(new FakeCameraInfo(), mock(CameraControlInternal.class)));
    }

    private String mCameraId;
    private BaseCamera mCamera;
    private FakeLifecycleOwner mLifecycle;
    private CountingErrorListener mErrorlistener;
    private CountDownLatch mLatch;
    private HandlerThread mHandlerThread;
    private Handler mHandler;

    private static String getCameraIdUnchecked(LensFacing lensFacing) {
        try {
            return CameraX.getCameraWithLensFacing(lensFacing);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to get camera id for camera lens facing " + lensFacing, e);
        }
    }

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        CameraDeviceSurfaceManager surfaceManager = new FakeCameraDeviceSurfaceManager();
        ExtendableUseCaseConfigFactory defaultConfigFactory = new ExtendableUseCaseConfigFactory();
        defaultConfigFactory.installDefaultProvider(FakeUseCaseConfig.class,
                new ConfigProvider<FakeUseCaseConfig>() {
                    @Override
                    public FakeUseCaseConfig getConfig(CameraX.LensFacing lensFacing) {
                        return new FakeUseCaseConfig.Builder().build();
                    }
                });
        AppConfig.Builder appConfigBuilder =
                new AppConfig.Builder()
                        .setCameraFactory(sCameraFactory)
                        .setDeviceSurfaceManager(surfaceManager)
                        .setUseCaseConfigFactory(defaultConfigFactory);

        // CameraX.init will actually init just once across all test cases. However we need to get
        // the real CameraFactory instance being injected into the init process.  So here we store
        // the CameraFactory instance in static fields.
        CameraX.init(context, appConfigBuilder.build());
        mLifecycle = new FakeLifecycleOwner();


        mLatch = new CountDownLatch(1);
        mErrorlistener = new CountingErrorListener(mLatch);
        mHandlerThread = new HandlerThread("ErrorHandlerThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mCameraId = getCameraIdUnchecked(LensFacing.BACK);
        mCamera = sCameraFactory.getCamera(mCameraId);

    }

    @After
    public void tearDown() throws InterruptedException {
        CameraX.unbindAll();
        mHandlerThread.quitSafely();

        // Wait some time for the cameras to close. We need the cameras to close to bring CameraX
        // back
        // to the initial state.
        Thread.sleep(3000);
    }

    @Test
    public void bind_createsNewUseCaseGroup() {
        CameraX.bindToLifecycle(mLifecycle, new FakeUseCase());

        // One observer is the use case group. The other observer removes the use case upon the
        // lifecycle's destruction.
        assertThat(mLifecycle.getObserverCount()).isEqualTo(2);
    }

    @Test
    public void bindMultipleUseCases() {
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
    public void isNotBound_afterUnbind() {
        FakeUseCase fakeUseCase = new FakeUseCase();
        CameraX.bindToLifecycle(mLifecycle, fakeUseCase);

        CameraX.unbind(fakeUseCase);
        assertThat(CameraX.isBound(fakeUseCase)).isFalse();
    }

    @Test
    public void bind_createsDifferentUseCaseGroups_forDifferentLifecycles() {
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
    public void exception_withDestroyedLifecycle() {
        FakeUseCase useCase = new FakeUseCase();

        mLifecycle.destroy();

        CameraX.bindToLifecycle(mLifecycle, useCase);
    }

    @Test
    public void errorListenerGetsCalled_whenErrorPosted() throws InterruptedException {
        CameraX.setErrorListener(mErrorlistener, mHandler);
        CameraX.postError(CameraX.ErrorCode.CAMERA_STATE_INCONSISTENT, "");
        mLatch.await(1, TimeUnit.SECONDS);

        assertThat(mErrorlistener.getCount()).isEqualTo(1);
    }

    @Test
    public void requestingDefaultConfiguration_returnsDefaultConfiguration() {
        // Requesting a default configuration will throw if CameraX is not initialized.
        FakeUseCaseConfig config = CameraX.getDefaultUseCaseConfig(
                FakeUseCaseConfig.class, LensFacing.BACK);
        assertThat(config).isNotNull();
        assertThat(config.getTargetClass(null)).isEqualTo(FakeUseCase.class);
    }

    @Test
    public void attachCameraControl_afterBindToLifecycle() {
        FakeUseCaseConfig config0 =
                new FakeUseCaseConfig.Builder().setTargetName("config0").build();
        AttachCameraFakeCase fakeUseCase = new AttachCameraFakeCase(config0);

        CameraX.bindToLifecycle(mLifecycle, fakeUseCase);

        assertThat(fakeUseCase.getCameraControl(mCameraId)).isEqualTo(
                mCamera.getCameraControlInternal());
    }

    @Test
    public void onCameraControlReadyIsCalled_afterBindToLifecycle() {
        FakeUseCaseConfig config0 =
                new FakeUseCaseConfig.Builder().setTargetName("config0").build();
        AttachCameraFakeCase fakeUseCase = spy(new AttachCameraFakeCase(config0));

        CameraX.bindToLifecycle(mLifecycle, fakeUseCase);

        Mockito.verify(fakeUseCase).onCameraControlReady(mCameraId);
    }

    @Test
    public void detachCameraControl_afterUnbind() {
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
    public void eventListenerCalled_bindAndUnbind() {
        UseCase.EventListener eventListener = Mockito.mock(UseCase.EventListener.class);

        FakeUseCaseConfig.Builder fakeConfigBuilder = new FakeUseCaseConfig.Builder();
        fakeConfigBuilder.setUseCaseEventListener(eventListener);
        AttachCameraFakeCase fakeUseCase = new AttachCameraFakeCase(fakeConfigBuilder.build());

        CameraX.bindToLifecycle(mLifecycle, fakeUseCase);
        Mockito.verify(eventListener).onBind(mCameraId);

        CameraX.unbind(fakeUseCase);
        Mockito.verify(eventListener).onUnbind();
    }

    @Test
    public void canRetrieveCameraInfo() throws CameraInfoUnavailableException {
        String cameraId = CameraX.getCameraWithLensFacing(LensFacing.BACK);
        CameraInfo cameraInfo = CameraX.getCameraInfo(cameraId);
        assertThat(cameraInfo).isNotNull();
    }

    @Test
    public void canGetCameraXContext() {
        Context context = CameraX.getContext();
        assertThat(context).isNotNull();
    }

    private static class CountingErrorListener implements ErrorListener {
        CountDownLatch mLatch;
        AtomicInteger mCount = new AtomicInteger(0);

        CountingErrorListener(CountDownLatch latch) {
            mLatch = latch;
        }

        @Override
        public void onError(@NonNull ErrorCode errorCode, @NonNull String message) {
            mCount.getAndIncrement();
            mLatch.countDown();
        }

        public int getCount() {
            return mCount.get();
        }
    }

    /** FakeUseCase that will call attachToCamera */
    public static class AttachCameraFakeCase extends FakeUseCase {

        public AttachCameraFakeCase(FakeUseCaseConfig config) {
            super(config);
        }

        @Override
        protected Map<String, Size> onSuggestedResolutionUpdated(
                Map<String, Size> suggestedResolutionMap) {

            SessionConfig.Builder builder = new SessionConfig.Builder();

            CameraDeviceConfig config = (CameraDeviceConfig) getUseCaseConfig();
            String cameraId = getCameraIdUnchecked(config.getLensFacing());
            attachToCamera(cameraId, builder.build());
            return suggestedResolutionMap;
        }
    }
}
