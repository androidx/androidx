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

import static org.junit.Assume.assumeTrue;

import android.Manifest;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraDevice;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.camera2.internal.Camera2CameraFactory;
import androidx.camera.camera2.internal.Camera2DeviceSurfaceManager;
import androidx.camera.core.CameraFactory;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.ImageReaderProxy;
import androidx.camera.core.ImageReaderProxys;
import androidx.camera.core.ImmediateSurface;
import androidx.camera.core.SessionConfig;
import androidx.camera.core.impl.CameraDeviceSurfaceManager;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.fakes.FakeUseCase;
import androidx.camera.testing.fakes.FakeUseCaseConfig;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

/**
 * Instrument test for {@link ImageReaderProxy}.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public final class ImageReaderProxysTest {

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);

    private static final String CAMERA_ID = "0";

    private static CameraFactory sCameraFactory;
    private static CameraDeviceSurfaceManager sCameraDeviceSurfaceManager;

    private CameraInternal mCameraInternal;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private List<ImageReaderProxy> mReaders = new ArrayList<>();

    private static ImageReaderProxy.OnImageAvailableListener createSemaphoreReleasingListener(
            final Semaphore semaphore) {
        return new ImageReaderProxy.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReaderProxy reader) {
                ImageProxy image = reader.acquireLatestImage();
                if (image != null) {
                    semaphore.release();
                    image.close();
                }
            }
        };
    }

    @BeforeClass
    public static void setUpClass() {
        Context context = ApplicationProvider.getApplicationContext();
        sCameraFactory = new Camera2CameraFactory(context);
        sCameraDeviceSurfaceManager = new Camera2DeviceSurfaceManager(context);
        sCameraDeviceSurfaceManager.init();
    }

    @Before
    public void setUp() throws CameraInfoUnavailableException {
        assumeTrue(CameraUtil.deviceHasCamera());

        mHandlerThread = new HandlerThread("Background");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mReaders = new ArrayList<>();

        // Grab the camera so we can wait for release in tearDown()
        mCameraInternal = sCameraFactory.getCamera(CAMERA_ID);
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {

        if (mCameraInternal != null) {
            // Ensure all cameras are released for the next test
            mCameraInternal.release().get();
        }

        for (ImageReaderProxy reader : mReaders) {
            reader.close();
        }

        if (mHandlerThread != null) {
            mHandlerThread.quitSafely();
        }
    }

    @Test
    public void sharedReadersGetFramesFromCamera() throws InterruptedException {
        List<Semaphore> semaphores = new ArrayList<>();
        for (int i = 0; i < 2; ++i) {
            ImageReaderProxy reader =
                    ImageReaderProxys.createSharedReader(
                            sCameraDeviceSurfaceManager, CAMERA_ID,
                            640, 480, ImageFormat.YUV_420_888, 2,
                            CameraXExecutors.newHandlerExecutor(mHandler));
            Semaphore semaphore = new Semaphore(/*permits=*/ 0);
            reader.setOnImageAvailableListener(
                    createSemaphoreReleasingListener(semaphore), mHandler);
            mReaders.add(reader);
            semaphores.add(semaphore);
        }

        FakeUseCaseConfig config = new FakeUseCaseConfig.Builder().setTargetName(
                "UseCase").getUseCaseConfig();
        UseCase useCase = new UseCase(config, mReaders);
        CameraUtil.openCameraWithUseCase(CAMERA_ID, mCameraInternal, useCase);

        // Wait for a few frames to be observed.
        for (Semaphore semaphore : semaphores) {
            semaphore.acquire(/*permits=*/ 5);
        }
    }

    @Test
    public void isolatedReadersGetFramesFromCamera() throws InterruptedException {
        List<Semaphore> semaphores = new ArrayList<>();
        for (int i = 0; i < 2; ++i) {
            ImageReaderProxy reader =
                    ImageReaderProxys.createIsolatedReader(
                            640, 480, ImageFormat.YUV_420_888, 2);
            Semaphore semaphore = new Semaphore(/*permits=*/ 0);
            reader.setOnImageAvailableListener(
                    createSemaphoreReleasingListener(semaphore), mHandler);
            mReaders.add(reader);
            semaphores.add(semaphore);
        }

        FakeUseCaseConfig config = new FakeUseCaseConfig.Builder().setTargetName(
                "UseCase").getUseCaseConfig();
        UseCase useCase = new UseCase(config, mReaders);
        CameraUtil.openCameraWithUseCase(CAMERA_ID, mCameraInternal, useCase);

        // Wait for a few frames to be observed.
        for (Semaphore semaphore : semaphores) {
            semaphore.acquire(/*permits=*/ 5);
        }
    }

    private static final class UseCase extends FakeUseCase {
        private final List<ImageReaderProxy> mImageReaders;

        private UseCase(FakeUseCaseConfig config, List<ImageReaderProxy> readers) {
            super(config);
            mImageReaders = readers;
            Map<String, Size> suggestedResolutionMap = new HashMap<>();
            suggestedResolutionMap.put(CAMERA_ID, new Size(640, 480));
            updateSuggestedResolution(suggestedResolutionMap);
        }

        @Override
        @NonNull
        protected Map<String, Size> onSuggestedResolutionUpdated(
                @NonNull Map<String, Size> suggestedResolutionMap) {
            SessionConfig.Builder sessionConfigBuilder = new SessionConfig.Builder();
            sessionConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
            for (ImageReaderProxy reader : mImageReaders) {
                sessionConfigBuilder.addSurface(new ImmediateSurface(reader.getSurface()));
            }
            attachToCamera(CAMERA_ID, sessionConfigBuilder.build());
            return suggestedResolutionMap;
        }
    }
}
