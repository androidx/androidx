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

package androidx.camera.camera2;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraDevice;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;

import androidx.camera.core.BaseCamera;
import androidx.camera.core.CameraDeviceConfiguration;
import androidx.camera.core.CameraFactory;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.FakeUseCase;
import androidx.camera.core.FakeUseCaseConfiguration;
import androidx.camera.core.ImmediateSurface;
import androidx.camera.core.SessionConfiguration;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class CameraAndroidTest {
    private static final LensFacing DEFAULT_LENS_FACING = LensFacing.BACK;
    static CameraFactory cameraFactory;

    BaseCamera camera;

    UseCase fakeUseCase;
    OnImageAvailableListener mockOnImageAvailableListener;
    String cameraId;

    private static String getCameraIdForLensFacingUnchecked(LensFacing lensFacing) {
        try {
            return cameraFactory.cameraIdForLensFacing(lensFacing);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to attach to camera with LensFacing " + lensFacing, e);
        }
    }

    @BeforeClass
    public static void classSetup() {
        cameraFactory = new Camera2CameraFactory(ApplicationProvider.getApplicationContext());
    }

    @Before
    public void setup() {
        mockOnImageAvailableListener = Mockito.mock(ImageReader.OnImageAvailableListener.class);
        FakeUseCaseConfiguration configuration =
                new FakeUseCaseConfiguration.Builder()
                        .setTargetName("UseCase")
                        .setLensFacing(DEFAULT_LENS_FACING)
                        .build();
        cameraId = getCameraIdForLensFacingUnchecked(DEFAULT_LENS_FACING);
        fakeUseCase = new UseCase(configuration, mockOnImageAvailableListener);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(cameraId, new Size(640, 480));
        fakeUseCase.updateSuggestedResolution(suggestedResolutionMap);

        camera = cameraFactory.getCamera(cameraId);
    }

    @After
    public void teardown() throws InterruptedException {
        // Need to release the camera no matter what is done, otherwise the CameraDevice is not
        // closed.
        // When the CameraDevice is not closed, then it can cause problems with interferes with
        // other
        // test cases.
        if (camera != null) {
            camera.release();
            camera = null;
        }

        // Wait a little bit for the camera device to close.
        // TODO(b/111991758): Listen for the close signal when it becomes available.
        Thread.sleep(2000);

        if (fakeUseCase != null) {
            fakeUseCase.close();
            fakeUseCase = null;
        }
    }

    @Test
    public void onlineUseCase() {
        camera.open();

        camera.addOnlineUseCase(Collections.singletonList(fakeUseCase));

        verify(mockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));

        camera.release();
    }

    @Test
    public void activeUseCase() {
        camera.open();

        camera.onUseCaseActive(fakeUseCase);

        verify(mockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));

        camera.release();
    }

    @Test
    public void onlineAndActiveUseCase() throws InterruptedException {
        camera.open();

        camera.addOnlineUseCase(Collections.singletonList(fakeUseCase));
        camera.onUseCaseActive(fakeUseCase);

        verify(mockOnImageAvailableListener, timeout(4000).atLeastOnce())
                .onImageAvailable(any(ImageReader.class));
    }

    @Test
    public void removeOnlineUseCase() {
        camera.open();

        camera.addOnlineUseCase(Collections.singletonList(fakeUseCase));
        camera.removeOnlineUseCase(Collections.singletonList(fakeUseCase));
        camera.onUseCaseActive(fakeUseCase);

        verify(mockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));
    }

    @Test
    public void unopenedCamera() {
        camera.addOnlineUseCase(Collections.singletonList(fakeUseCase));
        camera.removeOnlineUseCase(Collections.singletonList(fakeUseCase));

        verify(mockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));
    }

    @Test
    public void closedCamera() {
        camera.open();

        camera.close();
        camera.addOnlineUseCase(Collections.singletonList(fakeUseCase));
        camera.removeOnlineUseCase(Collections.singletonList(fakeUseCase));

        verify(mockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));
    }

    @Test
    public void releaseUnopenedCamera() {
        camera.release();
        camera.open();

        camera.addOnlineUseCase(Collections.singletonList(fakeUseCase));
        camera.onUseCaseActive(fakeUseCase);

        verify(mockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));
    }

    @Test
    public void releasedOpenedCamera() {
        camera.release();
        camera.open();

        camera.addOnlineUseCase(Collections.singletonList(fakeUseCase));
        camera.onUseCaseActive(fakeUseCase);

        verify(mockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));
    }

    private static class UseCase extends FakeUseCase {
        private final ImageReader.OnImageAvailableListener imageAvailableListener;
        HandlerThread handlerThread = new HandlerThread("HandlerThread");
        Handler handler;
        ImageReader imageReader;

        UseCase(
                FakeUseCaseConfiguration configuration,
                ImageReader.OnImageAvailableListener listener) {
            super(configuration);
            imageAvailableListener = listener;
            handlerThread.start();
            handler = new Handler(handlerThread.getLooper());
            Map<String, Size> suggestedResolutionMap = new HashMap<>();
            String cameraId = getCameraIdForLensFacingUnchecked(configuration.getLensFacing());
            suggestedResolutionMap.put(cameraId, new Size(640, 480));
            updateSuggestedResolution(suggestedResolutionMap);
        }

        void close() {
            handler.removeCallbacksAndMessages(null);
            handlerThread.quitSafely();
            if (imageReader != null) {
                imageReader.close();
            }
        }

        @Override
        protected Map<String, Size> onSuggestedResolutionUpdated(
                Map<String, Size> suggestedResolutionMap) {
            LensFacing lensFacing =
                    ((CameraDeviceConfiguration) getUseCaseConfiguration()).getLensFacing();
            String cameraId = getCameraIdForLensFacingUnchecked(lensFacing);
            Size resolution = suggestedResolutionMap.get(cameraId);
            SessionConfiguration.Builder builder = new SessionConfiguration.Builder();
            builder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
            imageReader =
                    ImageReader.newInstance(
                            resolution.getWidth(),
                            resolution.getHeight(),
                            ImageFormat.YUV_420_888, /*maxImages*/
                            2);
            imageReader.setOnImageAvailableListener(imageAvailableListener, handler);
            builder.addSurface(new ImmediateSurface(imageReader.getSurface()));

            attachToCamera(cameraId, builder.build());
            return suggestedResolutionMap;
        }
    }
}
