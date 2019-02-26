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
import androidx.camera.core.BaseUseCase;
import androidx.camera.core.CameraDeviceConfiguration;
import androidx.camera.core.CameraFactory;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.ImmediateSurface;
import androidx.camera.core.SessionConfiguration;
import androidx.camera.testing.fakes.FakeUseCase;
import androidx.camera.testing.fakes.FakeUseCaseConfiguration;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SmallTest
@RunWith(AndroidJUnit4.class)
public final class CameraTest {
    private static final LensFacing DEFAULT_LENS_FACING = LensFacing.BACK;
    static CameraFactory sCameraFactory;

    BaseCamera mCamera;

    UseCase mFakeUseCase;
    OnImageAvailableListener mMockOnImageAvailableListener;
    String mCameraId;

    private static String getCameraIdForLensFacingUnchecked(LensFacing lensFacing) {
        try {
            return sCameraFactory.cameraIdForLensFacing(lensFacing);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to attach to camera with LensFacing " + lensFacing, e);
        }
    }

    @BeforeClass
    public static void classSetup() {
        sCameraFactory = new Camera2CameraFactory(ApplicationProvider.getApplicationContext());
    }

    @Before
    public void setup() {
        mMockOnImageAvailableListener = Mockito.mock(ImageReader.OnImageAvailableListener.class);
        FakeUseCaseConfiguration configuration =
                new FakeUseCaseConfiguration.Builder()
                        .setTargetName("UseCase")
                        .setLensFacing(DEFAULT_LENS_FACING)
                        .build();
        mCameraId = getCameraIdForLensFacingUnchecked(DEFAULT_LENS_FACING);
        mFakeUseCase = new UseCase(configuration, mMockOnImageAvailableListener);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(mCameraId, new Size(640, 480));
        mFakeUseCase.updateSuggestedResolution(suggestedResolutionMap);

        mCamera = sCameraFactory.getCamera(mCameraId);
    }

    @After
    public void teardown() throws InterruptedException {
        // Need to release the camera no matter what is done, otherwise the CameraDevice is not
        // closed.
        // When the CameraDevice is not closed, then it can cause problems with interferes with
        // other
        // test cases.
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }

        // Wait a little bit for the camera device to close.
        // TODO(b/111991758): Listen for the close signal when it becomes available.
        Thread.sleep(2000);

        if (mFakeUseCase != null) {
            mFakeUseCase.close();
            mFakeUseCase = null;
        }
    }

    @Test
    public void onlineUseCase() {
        mCamera.open();

        mCamera.addOnlineUseCase(Collections.<BaseUseCase>singletonList(mFakeUseCase));

        verify(mMockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));

        mCamera.release();
    }

    @Test
    public void activeUseCase() {
        mCamera.open();

        mCamera.onUseCaseActive(mFakeUseCase);

        verify(mMockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));

        mCamera.release();
    }

    @Test
    public void onlineAndActiveUseCase() throws InterruptedException {
        mCamera.open();

        mCamera.addOnlineUseCase(Collections.<BaseUseCase>singletonList(mFakeUseCase));
        mCamera.onUseCaseActive(mFakeUseCase);

        verify(mMockOnImageAvailableListener, timeout(4000).atLeastOnce())
                .onImageAvailable(any(ImageReader.class));
    }

    @Test
    public void removeOnlineUseCase() {
        mCamera.open();

        mCamera.addOnlineUseCase(Collections.<BaseUseCase>singletonList(mFakeUseCase));
        mCamera.removeOnlineUseCase(Collections.<BaseUseCase>singletonList(mFakeUseCase));
        mCamera.onUseCaseActive(mFakeUseCase);

        verify(mMockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));
    }

    @Test
    public void unopenedCamera() {
        mCamera.addOnlineUseCase(Collections.<BaseUseCase>singletonList(mFakeUseCase));
        mCamera.removeOnlineUseCase(Collections.<BaseUseCase>singletonList(mFakeUseCase));

        verify(mMockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));
    }

    @Test
    public void closedCamera() {
        mCamera.open();

        mCamera.close();
        mCamera.addOnlineUseCase(Collections.<BaseUseCase>singletonList(mFakeUseCase));
        mCamera.removeOnlineUseCase(Collections.<BaseUseCase>singletonList(mFakeUseCase));

        verify(mMockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));
    }

    @Test
    public void releaseUnopenedCamera() {
        mCamera.release();
        mCamera.open();

        mCamera.addOnlineUseCase(Collections.<BaseUseCase>singletonList(mFakeUseCase));
        mCamera.onUseCaseActive(mFakeUseCase);

        verify(mMockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));
    }

    @Test
    public void releasedOpenedCamera() {
        mCamera.release();
        mCamera.open();

        mCamera.addOnlineUseCase(Collections.<BaseUseCase>singletonList(mFakeUseCase));
        mCamera.onUseCaseActive(mFakeUseCase);

        verify(mMockOnImageAvailableListener, never()).onImageAvailable(any(ImageReader.class));
    }

    private static class UseCase extends FakeUseCase {
        private final ImageReader.OnImageAvailableListener mImageAvailableListener;
        HandlerThread mHandlerThread = new HandlerThread("HandlerThread");
        Handler mHandler;
        ImageReader mImageReader;

        UseCase(
                FakeUseCaseConfiguration configuration,
                ImageReader.OnImageAvailableListener listener) {
            super(configuration);
            mImageAvailableListener = listener;
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper());
            Map<String, Size> suggestedResolutionMap = new HashMap<>();
            String cameraId = getCameraIdForLensFacingUnchecked(configuration.getLensFacing());
            suggestedResolutionMap.put(cameraId, new Size(640, 480));
            updateSuggestedResolution(suggestedResolutionMap);
        }

        void close() {
            mHandler.removeCallbacksAndMessages(null);
            mHandlerThread.quitSafely();
            if (mImageReader != null) {
                mImageReader.close();
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
            mImageReader =
                    ImageReader.newInstance(
                            resolution.getWidth(),
                            resolution.getHeight(),
                            ImageFormat.YUV_420_888, /*maxImages*/
                            2);
            mImageReader.setOnImageAvailableListener(mImageAvailableListener, mHandler);
            builder.addSurface(new ImmediateSurface(mImageReader.getSurface()));

            attachToCamera(cameraId, builder.build());
            return suggestedResolutionMap;
        }
    }
}
