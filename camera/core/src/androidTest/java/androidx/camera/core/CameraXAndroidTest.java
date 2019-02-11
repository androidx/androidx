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

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;

import androidx.camera.core.CameraX.ErrorCode;
import androidx.camera.core.CameraX.ErrorListener;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager;
import androidx.camera.testing.fakes.FakeCameraFactory;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(AndroidJUnit4.class)
public final class CameraXAndroidTest {
    static CameraFactory cameraFactory = new FakeCameraFactory();
    String cameraId;
    BaseCamera camera;
    private FakeLifecycleOwner lifecycle;
    private CountingErrorListener errorListener;
    private CountDownLatch latch;
    private HandlerThread handlerThread;
    private Handler handler;

    private static final String getCameraIdUnchecked(LensFacing lensFacing) {
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
        UseCaseConfigurationFactory defaultConfigFactory = new ExtendableUseCaseConfigFactory();
        AppConfiguration.Builder appConfigBuilder =
                new AppConfiguration.Builder()
                        .setCameraFactory(cameraFactory)
                        .setDeviceSurfaceManager(surfaceManager)
                        .setUseCaseConfigFactory(defaultConfigFactory);

        // CameraX.init will actually init just once across all test cases. However we need to get
        // the real CameraFactory instance being injected into the init process.  So here we store
        // the
        // CameraFactory instance in static fields.
        CameraX.init(context, appConfigBuilder.build());
        lifecycle = new FakeLifecycleOwner();
        cameraId = getCameraIdUnchecked(LensFacing.BACK);
        camera = cameraFactory.getCamera(cameraId);
        latch = new CountDownLatch(1);
        errorListener = new CountingErrorListener(latch);
        handlerThread = new HandlerThread("ErrorHandlerThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @After
    public void tearDown() throws InterruptedException {
        CameraX.unbindAll();
        handlerThread.quitSafely();

        // Wait some time for the cameras to close. We need the cameras to close to bring CameraX
        // back
        // to the initial state.
        Thread.sleep(3000);
    }

    @Test
    public void bind_createsNewUseCaseGroup() {
        CameraX.bindToLifecycle(lifecycle, new FakeUseCase());

        // One observer is the use case group. The other observer removes the use case upon the
        // lifecycle's destruction.
        assertThat(lifecycle.getObserverCount()).isEqualTo(2);
    }

    @Test
    public void bindMultipleUseCases() {
        FakeUseCaseConfiguration configuration0 =
                new FakeUseCaseConfiguration.Builder().setTargetName("config0").build();
        FakeUseCase fakeUseCase = new FakeUseCase(configuration0);
        FakeOtherUseCaseConfiguration configuration1 =
                new FakeOtherUseCaseConfiguration.Builder().setTargetName("config1").build();
        FakeOtherUseCase fakeOtherUseCase = new FakeOtherUseCase(configuration1);

        CameraX.bindToLifecycle(lifecycle, fakeUseCase, fakeOtherUseCase);

        assertThat(CameraX.isBound(fakeUseCase)).isTrue();
        assertThat(CameraX.isBound(fakeOtherUseCase)).isTrue();
    }

    @Test
    public void isNotBound_afterUnbind() {
        FakeUseCase fakeUseCase = new FakeUseCase();
        CameraX.bindToLifecycle(lifecycle, fakeUseCase);

        CameraX.unbind(fakeUseCase);
        assertThat(CameraX.isBound(fakeUseCase)).isFalse();
    }

    @Test
    public void bind_createsDifferentUseCaseGroups_forDifferentLifecycles() {
        FakeUseCaseConfiguration configuration0 =
                new FakeUseCaseConfiguration.Builder().setTargetName("config0").build();
        CameraX.bindToLifecycle(lifecycle, new FakeUseCase(configuration0));

        FakeUseCaseConfiguration configuration1 =
                new FakeUseCaseConfiguration.Builder().setTargetName("config1").build();
        FakeLifecycleOwner anotherLifecycle = new FakeLifecycleOwner();
        CameraX.bindToLifecycle(anotherLifecycle, new FakeUseCase(configuration1));

        // One observer is the use case group. The other observer removes the use case upon the
        // lifecycle's destruction.
        assertThat(lifecycle.getObserverCount()).isEqualTo(2);
        assertThat(anotherLifecycle.getObserverCount()).isEqualTo(2);
    }

    @Test
    public void exception_withDestroyedLifecycle() {
        FakeUseCase useCase = new FakeUseCase();

        lifecycle.destroy();

        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    CameraX.bindToLifecycle(lifecycle, useCase);
                });
    }

    @Test
    public void errorListenerGetsCalled_whenErrorPosted() throws InterruptedException {
        CameraX.setErrorListener(errorListener, handler);
        CameraX.postError(CameraX.ErrorCode.CAMERA_STATE_INCONSISTENT, "");
        latch.await(1, TimeUnit.SECONDS);

        assertThat(errorListener.getCount()).isEqualTo(1);
    }

    @Test
    public void requestingDefaultConfiguration_returnsDefaultConfiguration() {
        // Requesting a default configuration will throw if CameraX is not initialized.
        FakeUseCaseConfiguration config =
                CameraX.getDefaultUseCaseConfiguration(FakeUseCaseConfiguration.class);
        assertThat(config).isNotNull();
        assertThat(config.getTargetClass(null)).isEqualTo(FakeUseCase.class);
    }

    @Test
    public void attachCameraControl_afterBindToLifecycle() {
        FakeUseCaseConfiguration configuration0 =
                new FakeUseCaseConfiguration.Builder().setTargetName("config0").build();
        AttachCameraFakeCase fakeUseCase = new AttachCameraFakeCase(configuration0);

        CameraX.bindToLifecycle(lifecycle, fakeUseCase);

        assertThat(fakeUseCase.getCameraControl(cameraId)).isEqualTo(camera.getCameraControl());
    }

    @Test
    public void onCameraControlReadyIsCalled_afterBindToLifecycle() {
        FakeUseCaseConfiguration configuration0 =
                new FakeUseCaseConfiguration.Builder().setTargetName("config0").build();
        AttachCameraFakeCase fakeUseCase = spy(new AttachCameraFakeCase(configuration0));

        CameraX.bindToLifecycle(lifecycle, fakeUseCase);

        Mockito.verify(fakeUseCase).onCameraControlReady(cameraId);
    }

    @Test
    public void detachCameraControl_afterUnbind() {
        FakeUseCaseConfiguration configuration0 =
                new FakeUseCaseConfiguration.Builder().setTargetName("config0").build();
        AttachCameraFakeCase fakeUseCase = new AttachCameraFakeCase(configuration0);
        CameraX.bindToLifecycle(lifecycle, fakeUseCase);

        CameraX.unbind(fakeUseCase);

        // after unbind, Camera's CameraControl should be detached from Usecase
        assertThat(fakeUseCase.getCameraControl(cameraId)).isNotEqualTo(camera.getCameraControl());
        // UseCase still gets a non-null default CameraControl that does nothing.
        assertThat(fakeUseCase.getCameraControl(cameraId)).isNotNull();
    }

    @Test
    public void canRetrieveCameraInfo() throws CameraInfoUnavailableException {
        String cameraId = CameraX.getCameraWithLensFacing(LensFacing.BACK);
        CameraInfo cameraInfo = CameraX.getCameraInfo(cameraId);
        assertThat(cameraInfo).isNotNull();
    }

    private static class CountingErrorListener implements ErrorListener {
        CountDownLatch latch;
        AtomicInteger count = new AtomicInteger(0);

        CountingErrorListener(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onError(ErrorCode errorCode, String message) {
            count.getAndIncrement();
            latch.countDown();
        }

        public int getCount() {
            return count.get();
        }
    }

    /** FakeUseCase that will call attachToCamera */
    public static class AttachCameraFakeCase extends FakeUseCase {

        public AttachCameraFakeCase(FakeUseCaseConfiguration configuration) {
            super(configuration);
        }

        @Override
        protected Map<String, Size> onSuggestedResolutionUpdated(
                Map<String, Size> suggestedResolutionMap) {

            SessionConfiguration.Builder builder = new SessionConfiguration.Builder();

            CameraDeviceConfiguration configuration =
                    (CameraDeviceConfiguration) getUseCaseConfiguration();
            String cameraId = getCameraIdUnchecked(configuration.getLensFacing());
            attachToCamera(cameraId, builder.build());
            return suggestedResolutionMap;
        }
    }
}
