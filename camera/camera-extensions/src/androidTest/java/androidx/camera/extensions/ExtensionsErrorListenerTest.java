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

package androidx.camera.extensions;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

import android.Manifest;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCase;
import androidx.camera.extensions.ExtensionsErrorListener.ExtensionsErrorCode;
import androidx.camera.extensions.ExtensionsManager.EffectMode;
import androidx.camera.extensions.util.ExtensionsTestUtil;
import androidx.camera.testing.CameraUtil;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

@SmallTest
@RunWith(Parameterized.class)
/**
 * Unit tests for {@link androidx.camera.extensions.ExtensionsErrorListener}.
 * */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
public final class ExtensionsErrorListenerTest {
    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);

    @Parameterized.Parameters
    public static Collection<Object[]> getParameters() {
        return ExtensionsTestUtil.getAllEffectLensFacingCombinations();
    }

    private EffectMode mEffectMode;
    @CameraSelector.LensFacing
    private int mLensFacing;
    private CountDownLatch mLatch;

    final AtomicReference<ExtensionsErrorCode> mErrorCode = new AtomicReference<>();
    ExtensionsErrorListener mExtensionsErrorListener = new ExtensionsErrorListener() {
        @Override
        public void onError(@NonNull ExtensionsErrorCode errorCode) {
            mErrorCode.set(errorCode);
            mLatch.countDown();
        }
    };

    public ExtensionsErrorListenerTest(EffectMode effectMode,
            @CameraSelector.LensFacing int lensFacing) {
        mEffectMode = effectMode;
        mLensFacing = lensFacing;
    }

    @Before
    public void setUp() throws InterruptedException, ExecutionException, TimeoutException {
        assumeTrue(CameraUtil.deviceHasCamera());

        Context context = ApplicationProvider.getApplicationContext();
        CameraXConfig cameraXConfig = Camera2Config.defaultConfig();
        CameraX.initialize(context, cameraXConfig).get();

        assumeTrue(CameraUtil.hasCameraWithLensFacing(mLensFacing));
        assumeTrue(ExtensionsTestUtil.initExtensions());
        assumeTrue(ExtensionsManager.isExtensionAvailable(mEffectMode, mLensFacing));

        mLatch = new CountDownLatch(1);
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        CameraX.shutdown().get();
    }

    @Test
    public void receiveErrorCode_whenOnlyEnableImageCaptureExtender() throws InterruptedException {
        ExtensionsManager.setExtensionsErrorListener(mExtensionsErrorListener);

        ImageCapture imageCapture = ExtensionsTestUtil.createImageCaptureWithEffect(mEffectMode,
                mLensFacing);
        Preview noEffectPreview = ExtensionsTestUtil.createPreviewWithEffect(EffectMode.NORMAL,
                mLensFacing);

        List<UseCase> useCaseList = Arrays.asList(imageCapture, noEffectPreview);
        mErrorCode.set(null);
        ImageCaptureExtender.checkPreviewEnabled(mEffectMode, useCaseList);
        PreviewExtender.checkImageCaptureEnabled(mEffectMode, useCaseList);

        // Waits for one second to get error code.
        mLatch.await(1, TimeUnit.SECONDS);
        assertThat(mErrorCode.get()).isEqualTo(ExtensionsErrorCode.PREVIEW_EXTENSION_REQUIRED);
    }

    @Test
    public void receiveErrorCode_whenOnlyEnablePreviewExtender() throws InterruptedException {
        ExtensionsManager.setExtensionsErrorListener(mExtensionsErrorListener);

        ImageCapture noEffectImageCapture =
                ExtensionsTestUtil.createImageCaptureWithEffect(EffectMode.NORMAL, mLensFacing);
        Preview preview = ExtensionsTestUtil.createPreviewWithEffect(mEffectMode, mLensFacing);

        List<UseCase> useCaseList = Arrays.asList(noEffectImageCapture, preview);
        mErrorCode.set(null);
        ImageCaptureExtender.checkPreviewEnabled(mEffectMode, useCaseList);
        PreviewExtender.checkImageCaptureEnabled(mEffectMode, useCaseList);

        // Waits for one second to get error code.
        mLatch.await(1, TimeUnit.SECONDS);
        assertThat(mErrorCode.get()).isEqualTo(
                ExtensionsErrorCode.IMAGE_CAPTURE_EXTENSION_REQUIRED);
    }

    @Test
    @MediumTest
    public void notReceiveErrorCode_whenEnableBothImageCapturePreviewExtenders()
            throws InterruptedException {
        ExtensionsErrorListener mockExtensionsErrorListener = mock(ExtensionsErrorListener.class);
        ExtensionsManager.setExtensionsErrorListener(mockExtensionsErrorListener);

        ImageCapture imageCapture = ExtensionsTestUtil.createImageCaptureWithEffect(mEffectMode,
                mLensFacing);
        Preview preview = ExtensionsTestUtil.createPreviewWithEffect(mEffectMode, mLensFacing);

        List<UseCase> useCaseList = Arrays.asList(imageCapture, preview);
        ImageCaptureExtender.checkPreviewEnabled(mEffectMode, useCaseList);
        PreviewExtender.checkImageCaptureEnabled(mEffectMode, useCaseList);

        // Waits for one second to get error code.
        mLatch.await(1, TimeUnit.SECONDS);
        verifyZeroInteractions(mockExtensionsErrorListener);
    }

    @Test
    public void receiveErrorCode_whenEnableMismatchedImageCapturePreviewExtenders()
            throws InterruptedException {
        ExtensionsManager.setExtensionsErrorListener(mExtensionsErrorListener);

        // Creates ImageCapture
        ImageCapture imageCapture = ExtensionsTestUtil.createImageCaptureWithEffect(mEffectMode,
                mLensFacing);

        // Creates mismatched Preview
        EffectMode mismatchedEffectMode;

        if (mEffectMode != EffectMode.BOKEH) {
            assumeTrue(ExtensionsManager.isExtensionAvailable(EffectMode.BOKEH,
                    mLensFacing));
            mismatchedEffectMode = EffectMode.BOKEH;
        } else {
            assumeTrue(ExtensionsManager.isExtensionAvailable(EffectMode.HDR,
                    mLensFacing));
            mismatchedEffectMode = EffectMode.HDR;
        }

        Preview preview = ExtensionsTestUtil.createPreviewWithEffect(mismatchedEffectMode,
                mLensFacing);

        List<UseCase> useCaseList = Arrays.asList(imageCapture, preview);

        mErrorCode.set(null);
        // ImageCaptureExtender will find mismatched PreviewExtender is enabled.
        ImageCaptureExtender.checkPreviewEnabled(mEffectMode, useCaseList);
        mLatch.await(1, TimeUnit.SECONDS);
        assertThat(mErrorCode.get()).isEqualTo(ExtensionsErrorCode.MISMATCHED_EXTENSIONS_ENABLED);

        mLatch = new CountDownLatch(1);
        mErrorCode.set(null);
        // PreviewExtender will find mismatched ImageCaptureExtender is enabled.
        PreviewExtender.checkImageCaptureEnabled(mismatchedEffectMode, useCaseList);
        mLatch.await(1, TimeUnit.SECONDS);
        assertThat(mErrorCode.get()).isEqualTo(ExtensionsErrorCode.MISMATCHED_EXTENSIONS_ENABLED);
    }
}
