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

import android.content.Context;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.camera.testing.CameraUtil;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Contains tests for {@link androidx.camera.core.CameraX} which varies use case combinations to
 * run.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public final class UseCaseCombinationTest {
    private static final CameraSelector DEFAULT_SELECTOR = CameraSelector.DEFAULT_BACK_CAMERA;

    @Rule
    public TestRule mCameraRule = CameraUtil.grantCameraPermissionAndPreTest();

    private Context mContext;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        final CameraXConfig config = Camera2Config.defaultConfig();
        CameraX.initialize(mContext, config);
    }

    @After
    public void tearDown() throws InterruptedException, ExecutionException, TimeoutException {
        CameraX.shutdown().get(10000, TimeUnit.MILLISECONDS);
    }

    /**
     * Test Combination: Preview + ImageCapture
     */
    @Test
    public void previewCombinesImageCapture()  {
        final Preview preview = initPreview();
        final ImageCapture imageCapture = initImageCapture();

        CameraUseCaseAdapter camera = CameraUtil.createCameraUseCaseAdapter(mContext,
                DEFAULT_SELECTOR);
        camera.detachUseCases();

        // TODO(b/160249108) move off of main thread once UseCases can be attached on any
        //  thread
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            // This should not throw CameraUseCaseAdapter.CameraException
            try {
                camera.addUseCases(Arrays.asList(preview, imageCapture));
            } catch (CameraUseCaseAdapter.CameraException e) {
                throw new IllegalArgumentException(e);
            }
        });
    }

    /**
     * Test Combination: Preview + ImageAnalysis
     */
    @Test
    public void previewCombinesImageAnalysis()  {
        final Preview preview = initPreview();
        final ImageAnalysis imageAnalysis = initImageAnalysis();

        CameraUseCaseAdapter camera = CameraUtil.createCameraUseCaseAdapter(mContext,
                DEFAULT_SELECTOR);
        camera.detachUseCases();

        // TODO(b/160249108) move off of main thread once UseCases can be attached on any
        //  thread
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            // This should not throw CameraUseCaseAdapter.CameraException
            try {
                camera.addUseCases(Arrays.asList(preview, imageAnalysis));
            } catch (CameraUseCaseAdapter.CameraException e) {
                throw new IllegalArgumentException(e);
            }
        });
    }

    /** Test Combination: Preview + ImageAnalysis + ImageCapture */
    @Test
    public void previewCombinesImageAnalysisAndImageCapture() {
        final Preview preview = initPreview();
        final ImageAnalysis imageAnalysis = initImageAnalysis();
        final ImageCapture imageCapture = initImageCapture();

        CameraUseCaseAdapter camera = CameraUtil.createCameraUseCaseAdapter(mContext,
                DEFAULT_SELECTOR);
        camera.detachUseCases();

        // TODO(b/160249108) move off of main thread once UseCases can be attached on any
        //  thread
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            // This should not throw CameraUseCaseAdapter.CameraException
            try {
                camera.addUseCases(Arrays.asList(preview, imageAnalysis, imageCapture));
            } catch (CameraUseCaseAdapter.CameraException e) {
                throw new IllegalArgumentException(e);
            }
        });
    }

    private Preview initPreview() {
        return new Preview.Builder().setTargetName("Preview").build();
    }

    private ImageAnalysis initImageAnalysis() {
        return new ImageAnalysis.Builder()
                .setTargetName("ImageAnalysis")
                .build();
    }

    private ImageCapture initImageCapture() {
        return new ImageCapture.Builder().build();
    }
}
