/*
 * Copyright 2019 The Android Open Source Project
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

import static androidx.camera.view.PreviewView.ImplementationMode.COMPATIBLE;
import static androidx.camera.view.PreviewView.ImplementationMode.PERFORMANCE;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.testing.fakes.FakeActivity;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraInfoInternal;
import androidx.camera.view.preview.transform.transformation.Transformation;
import androidx.camera.view.test.R;
import androidx.core.content.ContextCompat;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Instrumented tests for {@link PreviewView}.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class PreviewViewTest {

    private static final Size DEFAULT_SURFACE_SIZE = new Size(640, 480);

    @Rule
    public final GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);
    @Rule
    public final ActivityTestRule<FakeActivity> mActivityRule = new ActivityTestRule<>(
            FakeActivity.class);
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private SurfaceRequest mSurfaceRequest;

    private SurfaceRequest createSurfaceRequest(CameraInfo cameraInfo) {
        return createSurfaceRequest(DEFAULT_SURFACE_SIZE, cameraInfo);
    }

    private SurfaceRequest createSurfaceRequest(Size size, CameraInfo cameraInfo) {
        FakeCamera fakeCamera = spy(new FakeCamera());
        when(fakeCamera.getCameraInfo()).thenReturn(cameraInfo);

        return new SurfaceRequest(size, fakeCamera, null);
    }

    private CountDownLatch mCountDownLatch = new CountDownLatch(1);

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width,
                        int height) {

                    mCountDownLatch.countDown();
                }

                @Override
                public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width,
                        int height) {

                }

                @Override
                public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

                }
            };

    @After
    public void tearDown() {
        if (mSurfaceRequest != null) {
            mSurfaceRequest.willNotProvideSurface();
            // Ensure all successful requests have their returned future finish.
            mSurfaceRequest.getDeferrableSurface().close();
            mSurfaceRequest = null;
        }
    }

    private CameraInfo createCameraInfo(String implementationType) {
        FakeCameraInfoInternal cameraInfoInternal = new FakeCameraInfoInternal();
        cameraInfoInternal.setImplementationType(implementationType);
        return cameraInfoInternal;
    }

    private CameraInfo createCameraInfo(int rotationDegrees, String implementationType) {
        FakeCameraInfoInternal cameraInfoInternal = new FakeCameraInfoInternal(rotationDegrees,
                CameraSelector.LENS_FACING_BACK);
        cameraInfoInternal.setImplementationType(implementationType);
        return cameraInfoInternal;
    }

    private CameraInfo createCameraInfo(int rotationDegrees, String implementationType,
            @CameraSelector.LensFacing int lensFacing) {
        FakeCameraInfoInternal cameraInfoInternal = new FakeCameraInfoInternal(rotationDegrees,
                lensFacing);
        cameraInfoInternal.setImplementationType(implementationType);
        return cameraInfoInternal;
    }

    @Test
    @UiThreadTest
    public void clearCameraController_controllerIsNull() {
        // Arrange.
        final PreviewView previewView = new PreviewView(mContext);
        setContentView(previewView);
        CameraController cameraController = new LifecycleCameraController(mContext);
        previewView.setController(cameraController);
        assertThat(previewView.getController()).isEqualTo(cameraController);

        // Act and Assert.
        previewView.setController(null);

        // Assert
        assertThat(previewView.getController()).isNull();
    }

    @Test
    @UiThreadTest
    public void setNewCameraController_oldControllerIsCleared() throws InterruptedException {
        // Arrange.
        final PreviewView previewView = new PreviewView(mContext);
        setContentView(previewView);
        Semaphore previewClearSemaphore = new Semaphore(0);
        CameraController oldController = new CameraController(mContext) {
            @Nullable
            @Override
            Camera startCamera() {
                return null;
            }

            void clearPreviewSurface() {
                previewClearSemaphore.release();
            }
        };
        previewView.setController(oldController);
        assertThat(previewView.getController()).isEqualTo(oldController);
        CameraController newController = new LifecycleCameraController(mContext);

        // Act and Assert.
        previewView.setController(newController);

        // Assert
        assertThat(previewView.getController()).isEqualTo(newController);
        assertThat(previewClearSemaphore.tryAcquire(1, TimeUnit.SECONDS)).isTrue();
    }


    @Test
    @UiThreadTest
    public void usesTextureView_whenLegacyDevice() {
        final CameraInfo cameraInfo =
                createCameraInfo(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2_LEGACY);
        final PreviewView previewView = new PreviewView(mContext);
        setContentView(previewView);
        previewView.setImplementationMode(PERFORMANCE);
        Preview.SurfaceProvider surfaceProvider = previewView.createSurfaceProvider();
        mSurfaceRequest = createSurfaceRequest(cameraInfo);
        surfaceProvider.onSurfaceRequested(mSurfaceRequest);

        assertThat(previewView.mImplementation).isInstanceOf(TextureViewImplementation.class);
    }

    @Test
    @UiThreadTest
    public void usesTextureView_whenAPILevelNotNewerThanN() {
        assumeTrue(Build.VERSION.SDK_INT <= 24);
        final CameraInfo cameraInfo = createCameraInfo(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2);

        final PreviewView previewView = new PreviewView(mContext);
        setContentView(previewView);
        previewView.setImplementationMode(PERFORMANCE);
        Preview.SurfaceProvider surfaceProvider = previewView.createSurfaceProvider();
        mSurfaceRequest = createSurfaceRequest(cameraInfo);
        surfaceProvider.onSurfaceRequested(mSurfaceRequest);

        assertThat(previewView.mImplementation).isInstanceOf(TextureViewImplementation.class);
    }

    @Test
    @UiThreadTest
    public void usesSurfaceView_whenNonLegacyDevice_andAPILevelNewerThanN() {
        assumeTrue(Build.VERSION.SDK_INT > 24);
        final CameraInfo cameraInfo = createCameraInfo(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2);

        final PreviewView previewView = new PreviewView(mContext);
        setContentView(previewView);
        previewView.setImplementationMode(PERFORMANCE);
        Preview.SurfaceProvider surfaceProvider = previewView.createSurfaceProvider();
        mSurfaceRequest = createSurfaceRequest(cameraInfo);
        surfaceProvider.onSurfaceRequested(mSurfaceRequest);

        assertThat(previewView.mImplementation).isInstanceOf(SurfaceViewImplementation.class);
    }

    @Test
    @UiThreadTest
    public void usesTextureView_whenNonLegacyDevice_andImplModeIsTextureView() {
        final CameraInfo cameraInfo = createCameraInfo(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2);

        final PreviewView previewView = new PreviewView(mContext);
        setContentView(previewView);
        previewView.setImplementationMode(COMPATIBLE);
        Preview.SurfaceProvider surfaceProvider = previewView.createSurfaceProvider();
        mSurfaceRequest = createSurfaceRequest(cameraInfo);
        surfaceProvider.onSurfaceRequested(mSurfaceRequest);

        assertThat(previewView.mImplementation).isInstanceOf(TextureViewImplementation.class);
    }

    @Test
    @UiThreadTest
    public void canCreateMeteringPointFactory() {
        final CameraInfo cameraInfo = createCameraInfo(90,
                CameraInfo.IMPLEMENTATION_TYPE_CAMERA2, CameraSelector.LENS_FACING_BACK);

        final PreviewView previewView = new PreviewView(mContext);
        setContentView(previewView);

        Preview.SurfaceProvider surfaceProvider = previewView.createSurfaceProvider();
        mSurfaceRequest = createSurfaceRequest(cameraInfo);
        surfaceProvider.onSurfaceRequested(mSurfaceRequest);
        MeteringPointFactory factory = previewView.getMeteringPointFactory();

        MeteringPoint point = factory.createPoint(100, 100);
        assertThat(point.getX() >= 0f || point.getX() <= 1.0f);
        assertThat(point.getY() >= 0f || point.getY() <= 1.0f);
    }

    @Test
    @UiThreadTest
    public void createMeteringPointFactory_previewViewWidthOrHeightIs0() {
        final CameraInfo cameraInfo = createCameraInfo(90,
                CameraInfo.IMPLEMENTATION_TYPE_CAMERA2, CameraSelector.LENS_FACING_BACK);

        final PreviewView previewView = new PreviewView(mContext);
        Preview.SurfaceProvider surfaceProvider = previewView.createSurfaceProvider();
        mSurfaceRequest = createSurfaceRequest(cameraInfo);
        surfaceProvider.onSurfaceRequested(mSurfaceRequest);

        MeteringPointFactory factory = previewView.getMeteringPointFactory();

        //Width and height is 0,  but surface is requested,
        //verifying the factory only creates invalid points.
        MeteringPoint point = factory.createPoint(100, 100);
        assertThat(point.getX() < 0f || point.getX() > 1.0f);
        assertThat(point.getY() < 0f || point.getY() > 1.0f);
    }

    @Test
    @UiThreadTest
    public void createMeteringPointFactory_beforeCreatingSurfaceProvider() {
        final PreviewView previewView = new PreviewView(mContext);
        // make PreviewView.getWidth() getHeight not 0.
        setContentView(previewView);
        MeteringPointFactory factory = previewView.getMeteringPointFactory();

        //verifying the factory only creates invalid points.
        MeteringPoint point = factory.createPoint(100, 100);
        assertThat(point.getX() < 0f || point.getX() > 1.0f);
        assertThat(point.getY() < 0f || point.getY() > 1.0f);
    }

    @Test
    @UiThreadTest
    public void getsImplementationMode() {
        final PreviewView previewView = new PreviewView(mContext);
        previewView.setImplementationMode(PERFORMANCE);

        assertThat(previewView.getImplementationMode()).isEqualTo(PERFORMANCE);
    }

    @Test
    @UiThreadTest
    public void getsScaleTypeProgrammatically() {
        final PreviewView previewView = new PreviewView(mContext);
        previewView.setScaleType(PreviewView.ScaleType.FIT_END);

        assertThat(previewView.getScaleType()).isEqualTo(PreviewView.ScaleType.FIT_END);
    }

    @Test
    @UiThreadTest
    public void getsScaleTypeFromXMLLayout() {
        final PreviewView previewView = (PreviewView) LayoutInflater.from(mContext).inflate(
                R.layout.preview_view_scale_type_fit_end, null);
        assertThat(previewView.getScaleType()).isEqualTo(PreviewView.ScaleType.FIT_END);
    }

    @Test
    @UiThreadTest
    public void redrawsPreview_whenScaleTypeChanges() {
        final PreviewView previewView = new PreviewView(mContext);
        final PreviewViewImplementation implementation = mock(TestPreviewViewImplementation.class);
        previewView.mImplementation = implementation;

        previewView.setScaleType(PreviewView.ScaleType.FILL_START);

        verify(implementation, times(1)).redrawPreview();
    }

    @Test
    public void redrawsPreview_whenLayoutResized() throws Throwable {
        final AtomicReference<PreviewView> previewView = new AtomicReference<>();
        final AtomicReference<FrameLayout> container = new AtomicReference<>();
        final PreviewViewImplementation implementation = mock(TestPreviewViewImplementation.class);

        mActivityRule.runOnUiThread(() -> {
            previewView.set(new PreviewView(mContext));
            previewView.get().mImplementation = implementation;

            container.set(new FrameLayout(mContext));
            container.get().addView(previewView.get());
            setContentView(container.get());

            // Resize container in order to trigger PreviewView's onLayoutChanged listener.
            final FrameLayout.LayoutParams params =
                    (FrameLayout.LayoutParams) container.get().getLayoutParams();
            params.width = params.width / 2;
            container.get().requestLayout();
        });

        verify(implementation, timeout(1_000).times(1)).redrawPreview();
    }

    @Test
    public void doesNotRedrawPreview_whenDetachedFromWindow() throws Throwable {
        final AtomicReference<PreviewView> previewView = new AtomicReference<>();
        final AtomicReference<FrameLayout> container = new AtomicReference<>();
        final PreviewViewImplementation implementation = mock(TestPreviewViewImplementation.class);

        mActivityRule.runOnUiThread(() -> {
            previewView.set(new PreviewView(mContext));
            previewView.get().mImplementation = implementation;

            container.set(new FrameLayout(mContext));
            container.get().addView(previewView.get());
            setContentView(container.get());

            container.get().removeView(previewView.get());

            // Resize container
            final FrameLayout.LayoutParams params =
                    (FrameLayout.LayoutParams) container.get().getLayoutParams();
            params.width = params.width / 2;
            container.get().requestLayout();
        });

        verify(implementation, never()).redrawPreview();
    }

    @Test
    public void redrawsPreview_whenReattachedToWindow() throws Throwable {
        final AtomicReference<PreviewView> previewView = new AtomicReference<>();
        final AtomicReference<FrameLayout> container = new AtomicReference<>();
        final PreviewViewImplementation implementation = mock(TestPreviewViewImplementation.class);

        mActivityRule.runOnUiThread(() -> {
            previewView.set(new PreviewView(mContext));
            previewView.get().mImplementation = implementation;

            container.set(new FrameLayout(mContext));
            container.get().addView(previewView.get());
            setContentView(container.get());

            container.get().removeView(previewView.get());
            container.get().addView(previewView.get());
        });

        verify(implementation, timeout(1_000).times(1)).redrawPreview();
    }

    @Test
    public void sensorDimensionFlippedCorrectly() throws Throwable {
        final AtomicReference<PreviewView> previewView = new AtomicReference<>();
        final AtomicReference<FrameLayout> container = new AtomicReference<>();
        final AtomicReference<TextureView> textureView = new AtomicReference<>();
        final Size containerSize = new Size(800, 1000);
        final Size bufferSize = new Size(2000, 1000);

        // Creates mock CameraInfo to return sensor degrees as 90. This means the sensor
        // dimension flip is needed in related transform calculations.
        final CameraInfo cameraInfo = createCameraInfo(90, CameraInfo.IMPLEMENTATION_TYPE_CAMERA2);

        mActivityRule.runOnUiThread(() -> {
            previewView.set(new PreviewView(mContext));

            container.set(new FrameLayout(mContext));
            container.get().addView(previewView.get());
            setContentView(container.get());
            // Sets as TEXTURE_VIEW mode so that we can verify the TextureView result
            // transformation.
            previewView.get().setImplementationMode(COMPATIBLE);
            previewView.get().setScaleType(PreviewView.ScaleType.FILL_CENTER);

            // Sets container size as 640x480
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                    containerSize.getWidth(), containerSize.getHeight());
            container.get().setLayoutParams(layoutParams);

            // Creates surface provider and request surface for 1080p surface size.
            Preview.SurfaceProvider surfaceProvider =
                    previewView.get().createSurfaceProvider();
            mSurfaceRequest = createSurfaceRequest(bufferSize, cameraInfo);
            surfaceProvider.onSurfaceRequested(mSurfaceRequest);

            // Retrieves the TextureView
            textureView.set((TextureView) previewView.get().mImplementation.getPreview());

            // Sets SurfaceTextureListener to wait for surface texture available.
            mCountDownLatch = new CountDownLatch(1);
            textureView.get().setSurfaceTextureListener(mSurfaceTextureListener);

        });

        // Wait for surface texture available.
        mCountDownLatch.await(1, TimeUnit.SECONDS);

        // Retrieves the transformation applied to the TextureView
        Transformation resultTransformation = Transformation.getTransformation(textureView.get());
        float[] resultTransformParameters = new float[]{resultTransformation.getScaleX(),
                resultTransformation.getScaleY(), resultTransformation.getTransX(),
                resultTransformation.getTransY(), resultTransformation.getRotation()};

        float[] expectedTransformParameters = new float[]{0.4f, 1.6f, -600.0f, 0.0f, 0.0f};

        assertThat(resultTransformParameters).isEqualTo(expectedTransformParameters);
    }

    @Test
    @UiThreadTest
    public void setsDefaultBackground_whenBackgroundNotExplicitlySet() {
        final PreviewView previewView = new PreviewView(mContext);

        assertThat(previewView.getBackground()).isInstanceOf(ColorDrawable.class);

        final ColorDrawable actualBackground = (ColorDrawable) previewView.getBackground();
        final int expectedBackground = ContextCompat.getColor(mContext,
                PreviewView.DEFAULT_BACKGROUND_COLOR);
        assertThat(actualBackground.getColor()).isEqualTo(expectedBackground);
    }

    @Test
    @UiThreadTest
    public void overridesDefaultBackground_whenBackgroundExplicitlySet_programmatically() {
        final PreviewView previewView = new PreviewView(mContext);
        final int backgroundColor = ContextCompat.getColor(mContext, android.R.color.white);
        previewView.setBackgroundColor(backgroundColor);

        assertThat(previewView.getBackground()).isInstanceOf(ColorDrawable.class);

        final ColorDrawable actualBackground = (ColorDrawable) previewView.getBackground();
        assertThat(actualBackground.getColor()).isEqualTo(backgroundColor);
    }

    @Test
    @UiThreadTest
    public void overridesDefaultBackground_whenBackgroundExplicitlySet_xml() {
        final PreviewView previewView = (PreviewView) LayoutInflater.from(mContext).inflate(
                R.layout.preview_view_background_white, null);

        assertThat(previewView.getBackground()).isInstanceOf(ColorDrawable.class);

        final ColorDrawable actualBackground = (ColorDrawable) previewView.getBackground();
        final int expectedBackground = ContextCompat.getColor(mContext, android.R.color.white);
        assertThat(actualBackground.getColor()).isEqualTo(expectedBackground);
    }

    @Test
    @UiThreadTest
    public void doNotRemovePreview_whenCreatingNewSurfaceProvider() {
        final PreviewView previewView = new PreviewView(mContext);
        setContentView(previewView);

        // Start a preview stream
        final Preview.SurfaceProvider surfaceProvider = previewView.createSurfaceProvider();
        final CameraInfo cameraInfo = createCameraInfo(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2);
        mSurfaceRequest = createSurfaceRequest(cameraInfo);
        surfaceProvider.onSurfaceRequested(mSurfaceRequest);

        // Create a new surfaceProvider
        previewView.createSurfaceProvider();

        // Assert PreviewView doesn't remove the current preview TextureView/SurfaceView
        boolean wasPreviewRemoved = true;
        for (int i = 0; i < previewView.getChildCount(); i++) {
            if (previewView.getChildAt(i) instanceof TextureView
                    || previewView.getChildAt(i) instanceof SurfaceView) {
                wasPreviewRemoved = false;
                break;
            }
        }
        assertThat(wasPreviewRemoved).isFalse();
    }

    private void setContentView(View view) {
        mActivityRule.getActivity().setContentView(view);
    }

    /**
     * An empty implementation of {@link PreviewViewImplementation} used for testing. It allows
     * mocking {@link PreviewViewImplementation} since the latter is package private.
     */
    public static class TestPreviewViewImplementation extends PreviewViewImplementation {

        @Override
        public void initializePreview() {
        }

        @Nullable
        @Override
        public View getPreview() {
            return null;
        }

        @Override
        void onSurfaceRequested(@NonNull SurfaceRequest surfaceRequest,
                @Nullable OnSurfaceNotInUseListener onSurfaceNotInUseListener) {
        }

        @Override
        public void redrawPreview() {
        }

        @Override
        void onAttachedToWindow() {
        }

        @Override
        void onDetachedFromWindow() {
        }

        @Override
        @NonNull
        ListenableFuture<Void> waitForNextFrame() {
            return Futures.immediateFuture(null);
        }

        @Nullable
        @Override
        Bitmap getPreviewBitmap() {
            return null;
        }
    }
}
