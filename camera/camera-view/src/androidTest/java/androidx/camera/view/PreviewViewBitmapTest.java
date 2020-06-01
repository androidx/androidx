/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.view;

import static com.google.common.truth.Truth.assertThat;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.fakes.FakeActivity;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.camera.view.PreviewView.ImplementationMode;
import androidx.lifecycle.Observer;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class PreviewViewBitmapTest {

    @Rule
    public final ActivityTestRule<FakeActivity> mActivityRule =
            new ActivityTestRule<>(FakeActivity.class);
    @Rule
    public final GrantPermissionRule mPermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);

    private static final int CAMERA_LENS = CameraSelector.LENS_FACING_BACK;
    private ProcessCameraProvider mCameraProvider;

    @Before
    public void setUp() throws ExecutionException, InterruptedException {
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(CAMERA_LENS));

        final Context context = ApplicationProvider.getApplicationContext();
        final CameraXConfig config = Camera2Config.defaultConfig();
        CameraX.initialize(context, config);
        mCameraProvider = ProcessCameraProvider.getInstance(context).get();
    }

    @After
    public void tearDown() throws Throwable {
        if (CameraX.isInitialized()) {
            runOnMainThread(CameraX::unbindAll);
        }
        CameraX.shutdown().get();
    }

    @Test
    public void bitmapIsNull_whenPreviewNotDisplaying_textureView() {
        // Arrange
        final PreviewView previewView = setUpPreviewView(ImplementationMode.TEXTURE_VIEW);

        // Act
        startPreview(previewView);

        // assert
        final Bitmap bitmap = previewView.getBitmap();
        assertThat(bitmap).isNull();
    }

    @Test
    public void bitmapIsNull_whenPreviewNotDisplaying_surfaceView() {
        // Arrange
        final PreviewView previewView = setUpPreviewView(ImplementationMode.SURFACE_VIEW);

        // Act
        startPreview(previewView);

        // assert
        final Bitmap bitmap = previewView.getBitmap();
        assertThat(bitmap).isNull();
    }

    @Test
    public void bitmapNotNull_whenPreviewIsDisplaying_textureView() throws Throwable {
        // Arrange
        final PreviewView previewView = setUpPreviewView(ImplementationMode.TEXTURE_VIEW);

        // Act
        startPreview(previewView);
        waitForPreviewToStart(previewView);

        // assert
        final Bitmap bitmap = previewView.getBitmap();
        assertThat(bitmap).isNotNull();
    }

    @Test
    public void bitmapNotNull_whenPreviewIsDisplaying_surfaceViewView() throws Throwable {
        // Arrange
        final PreviewView previewView = setUpPreviewView(ImplementationMode.SURFACE_VIEW);

        // Act
        startPreview(previewView);
        waitForPreviewToStart(previewView);

        // assert
        final Bitmap bitmap = previewView.getBitmap();
        assertThat(bitmap).isNotNull();
    }

    @Test
    public void bitmapHasSameSizeAsPreviewView_fillStart_textureView() throws Throwable {
        bitmapHasSameSizeAsPreviewView(ImplementationMode.TEXTURE_VIEW,
                PreviewView.ScaleType.FILL_START);
    }

    @Test
    public void bitmapHasSameSizeAsPreviewView_fillCenter_textureView() throws Throwable {
        bitmapHasSameSizeAsPreviewView(ImplementationMode.TEXTURE_VIEW,
                PreviewView.ScaleType.FILL_CENTER);
    }

    @Test
    public void bitmapHasSameSizeAsPreviewView_fillEnd_textureView() throws Throwable {
        bitmapHasSameSizeAsPreviewView(ImplementationMode.TEXTURE_VIEW,
                PreviewView.ScaleType.FILL_END);
    }

    @Test
    public void bitmapHasSameSizeAsPreviewView_fillStart_surfaceView() throws Throwable {
        bitmapHasSameSizeAsPreviewView(ImplementationMode.SURFACE_VIEW,
                PreviewView.ScaleType.FILL_START);
    }

    @Test
    public void bitmapHasSameSizeAsPreviewView_fillCenter_surfaceView() throws Throwable {
        bitmapHasSameSizeAsPreviewView(ImplementationMode.SURFACE_VIEW,
                PreviewView.ScaleType.FILL_CENTER);
    }

    @Test
    public void bitmapHasSameSizeAsPreviewView_fillEnd_surfaceView() throws Throwable {
        bitmapHasSameSizeAsPreviewView(ImplementationMode.SURFACE_VIEW,
                PreviewView.ScaleType.FILL_END);
    }

    private void bitmapHasSameSizeAsPreviewView(@NonNull PreviewView.ImplementationMode mode,
            @NonNull PreviewView.ScaleType scaleType) throws Throwable {
        // Arrange
        final PreviewView previewView = setUpPreviewView(mode, scaleType);

        // Act
        startPreview(previewView);
        waitForPreviewToStart(previewView);

        // assert
        final Bitmap bitmap = previewView.getBitmap();
        assertThat(bitmap).isNotNull();
        assertThat(bitmap.getWidth()).isEqualTo(previewView.getWidth());
        assertThat(bitmap.getHeight()).isEqualTo(previewView.getHeight());
    }

    @Test
    public void bitmapSmallerInSizeThanPreviewView_fitStart_textureView() throws Throwable {
        bitmapSmallerInSizeThanPreviewView(ImplementationMode.TEXTURE_VIEW,
                PreviewView.ScaleType.FIT_START);
    }

    @Test
    public void bitmapSmallerInSizeThanPreviewView_fitCenter_textureView() throws Throwable {
        bitmapSmallerInSizeThanPreviewView(ImplementationMode.TEXTURE_VIEW,
                PreviewView.ScaleType.FIT_CENTER);
    }

    @Test
    public void bitmapSmallerInSizeThanPreviewView_fitEnd_textureView() throws Throwable {
        bitmapSmallerInSizeThanPreviewView(ImplementationMode.TEXTURE_VIEW,
                PreviewView.ScaleType.FIT_END);
    }

    @Test
    public void bitmapSmallerInSizeThanPreviewView_fitStart_surfaceView() throws Throwable {
        bitmapSmallerInSizeThanPreviewView(ImplementationMode.SURFACE_VIEW,
                PreviewView.ScaleType.FIT_START);
    }

    @Test
    public void bitmapSmallerInSizeThanPreviewView_fitCenter_surfaceView() throws Throwable {
        bitmapSmallerInSizeThanPreviewView(ImplementationMode.SURFACE_VIEW,
                PreviewView.ScaleType.FIT_CENTER);
    }

    @Test
    public void bitmapSmallerInSizeThanPreviewView_fitEnd_surfaceView() throws Throwable {
        bitmapSmallerInSizeThanPreviewView(ImplementationMode.SURFACE_VIEW,
                PreviewView.ScaleType.FIT_END);
    }

    private void bitmapSmallerInSizeThanPreviewView(@NonNull PreviewView.ImplementationMode mode,
            @NonNull PreviewView.ScaleType scaleType) throws Throwable {
        // Arrange
        final PreviewView previewView = setUpPreviewView(mode, scaleType);

        // Act
        startPreview(previewView);
        waitForPreviewToStart(previewView);

        // assert
        final Bitmap bitmap = previewView.getBitmap();
        assertThat(bitmap).isNotNull();
        assertThat(bitmap.getWidth()).isAtMost(previewView.getWidth());
        assertThat(bitmap.getHeight()).isAtMost(previewView.getHeight());
        assertThat(bitmap.getWidth() == previewView.getWidth()
                || bitmap.getHeight() == previewView.getHeight()).isTrue();
    }

    @NonNull
    private PreviewView setUpPreviewView(@NonNull PreviewView.ImplementationMode mode) {
        return setUpPreviewView(mode, PreviewView.ScaleType.FILL_CENTER);
    }

    @NonNull
    private PreviewView setUpPreviewView(@NonNull PreviewView.ImplementationMode mode,
            @NonNull PreviewView.ScaleType scaleType) {
        final Context context = ApplicationProvider.getApplicationContext();
        final PreviewView previewView = new PreviewView(context);
        previewView.setPreferredImplementationMode(mode);
        previewView.setScaleType(scaleType);
        runOnMainThread(() -> mActivityRule.getActivity().setContentView(previewView));
        return previewView;
    }

    private void startPreview(@NonNull final PreviewView previewView) {
        final Preview preview = new Preview.Builder().build();
        final CameraSelector cameraSelector =
                new CameraSelector.Builder().requireLensFacing(CAMERA_LENS).build();
        final FakeLifecycleOwner lifecycleOwner = new FakeLifecycleOwner();
        lifecycleOwner.startAndResume();

        runOnMainThread(() -> {
            preview.setSurfaceProvider(previewView.createSurfaceProvider());
            mCameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview);
        });
    }

    private void waitForPreviewToStart(@NonNull final PreviewView previewView) throws Throwable {
        final Semaphore semaphore = new Semaphore(0);
        final Observer<PreviewView.StreamState> observer = streamState -> {
            if (streamState == PreviewView.StreamState.STREAMING) {
                semaphore.release();
            }
        };

        runOnMainThread(() -> previewView.getPreviewStreamState().observeForever(observer));

        try {
            assertThat(semaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            runOnMainThread(() -> previewView.getPreviewStreamState().removeObserver(observer));
        }
    }

    private void runOnMainThread(@NonNull final Runnable block) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block);
    }
}
