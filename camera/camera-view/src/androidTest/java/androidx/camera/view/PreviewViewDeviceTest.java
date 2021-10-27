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
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.util.Size;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
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
import androidx.camera.core.ViewPort;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.CoreAppTestUtil;
import androidx.camera.testing.SurfaceFormatUtil;
import androidx.camera.testing.fakes.FakeActivity;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraInfoInternal;
import androidx.camera.view.test.R;
import androidx.core.content.ContextCompat;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Instrumented tests for {@link PreviewView}.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 21)
public class PreviewViewDeviceTest {

    private static final Size DEFAULT_SURFACE_SIZE = new Size(640, 480);
    private static final Rect DEFAULT_CROP_RECT = new Rect(0, 0, 640, 480);
    private static final Rect SMALLER_CROP_RECT = new Rect(20, 20, 600, 400);

    @Rule
    public TestRule mUseCamera = CameraUtil.grantCameraPermissionAndPreTest();

    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private ActivityScenario<FakeActivity> mActivityScenario;
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private ProcessCameraProvider mCameraProvider = null;
    private final List<SurfaceRequest> mSurfaceRequestList = new ArrayList<>();
    private PreviewView mPreviewView;
    private MeteringPointFactory mMeteringPointFactory;
    private final UiDevice mUiDevice = UiDevice.getInstance(mInstrumentation);

    @Before
    public void setUp() throws CoreAppTestUtil.ForegroundOccupiedError, ExecutionException,
            InterruptedException, TimeoutException {
        CoreAppTestUtil.prepareDeviceUI(mInstrumentation);
        mActivityScenario = ActivityScenario.launch(FakeActivity.class);
        mCameraProvider = ProcessCameraProvider.getInstance(mContext).get(10000,
                TimeUnit.MILLISECONDS);
    }

    @After
    public void tearDown() throws InterruptedException, ExecutionException, TimeoutException {
        for (SurfaceRequest surfaceRequest : mSurfaceRequestList) {
            surfaceRequest.willNotProvideSurface();
            // Ensure all successful requests have their returned future finish.
            surfaceRequest.getDeferrableSurface().close();
        }
        if (mCameraProvider != null) {
            mCameraProvider.shutdown();
        }
    }

    @Test
    public void previewViewSetScaleType_controllerRebinds() throws InterruptedException {
        // Arrange.
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Semaphore fitTypeSemaphore = new Semaphore(0);
        CameraController fakeController = new CameraController(mContext) {

            @Override
            void attachPreviewSurface(@NonNull Preview.SurfaceProvider surfaceProvider,
                    @NonNull ViewPort viewPort, @NonNull Display display) {
                if (viewPort.getScaleType() == ViewPort.FIT) {
                    fitTypeSemaphore.release();
                }
            }

            @Nullable
            @Override
            Camera startCamera() {
                return null;
            }
        };
        AtomicReference<PreviewView> previewViewAtomicReference = new AtomicReference<>();
        mInstrumentation.runOnMainSync(() -> {
            PreviewView previewView = new PreviewView(mContext);
            previewViewAtomicReference.set(previewView);
            previewView.setImplementationMode(COMPATIBLE);
            notifyLatchWhenLayoutReady(previewView, countDownLatch);
            setContentView(previewView);
        });
        // Wait for layout ready
        assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isTrue();

        // Act: set controller then change the scale type.
        mInstrumentation.runOnMainSync(() -> {
            previewViewAtomicReference.get().setController(fakeController);
            previewViewAtomicReference.get().setScaleType(PreviewView.ScaleType.FIT_CENTER);
        });

        // Assert: cameraController receives a fit type ViewPort.
        assertThat(fitTypeSemaphore.tryAcquire(1, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void receiveSurfaceRequest_transformIsValid() throws InterruptedException {
        // Arrange: set up PreviewView.
        AtomicReference<PreviewView> previewView = new AtomicReference<>();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        mInstrumentation.runOnMainSync(() -> {
            previewView.set(new PreviewView(mContext));
            setContentView(previewView.get());
            // Feed the PreviewView with a fake SurfaceRequest
            CameraInfoInternal cameraInfo =
                    createCameraInfo(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2);
            previewView.get().getSurfaceProvider().onSurfaceRequested(
                    createSurfaceRequest(cameraInfo));
            notifyLatchWhenLayoutReady(previewView.get(), countDownLatch);
        });
        updateCropRectAndWaitForIdle(DEFAULT_CROP_RECT);

        // Assert: OutputTransform is not null.
        assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isTrue();
        mInstrumentation.runOnMainSync(
                () -> assertThat(previewView.get().getOutputTransform()).isNotNull());
    }

    @Test
    public void noSurfaceRequest_transformIsInvalid() throws InterruptedException {
        // Arrange: set up PreviewView.
        AtomicReference<PreviewView> previewView = new AtomicReference<>();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        mInstrumentation.runOnMainSync(() -> {
            previewView.set(new PreviewView(mContext));
            setContentView(previewView.get());
            notifyLatchWhenLayoutReady(previewView.get(), countDownLatch);
        });

        // Assert: OutputTransform is null.
        assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isTrue();
        mInstrumentation.runOnMainSync(
                () -> assertThat(previewView.get().getOutputTransform()).isNull());
    }

    @Test
    public void previewViewPinched_pinchToZoomInvokedOnController()
            throws InterruptedException, UiObjectNotFoundException {
        // TODO(b/169058735): investigate and enable on Cuttlefish.
        Assume.assumeFalse("Skip Cuttlefish until further investigation.",
                android.os.Build.MODEL.contains("Cuttlefish"));

        // Arrange.
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Semaphore semaphore = new Semaphore(0);
        CameraController fakeController = new CameraController(mContext) {
            @Override
            void onPinchToZoom(float pinchToZoomScale) {
                semaphore.release();
            }

            @Nullable
            @Override
            Camera startCamera() {
                return null;
            }
        };
        mInstrumentation.runOnMainSync(() -> {
            PreviewView previewView = new PreviewView(mContext);
            previewView.setController(fakeController);
            previewView.setImplementationMode(COMPATIBLE);
            notifyLatchWhenLayoutReady(previewView, countDownLatch);
            setContentView(previewView);
        });
        // Wait for layout ready
        assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isTrue();

        // Act: pinch-in 80% in 100 steps.
        mUiDevice.findObject(new UiSelector().index(0)).pinchIn(80, 100);

        // Assert: pinch-to-zoom is called.
        assertThat(semaphore.tryAcquire(1, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void previewViewClicked_tapToFocusInvokedOnController()
            throws InterruptedException, UiObjectNotFoundException {
        // Arrange.
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Semaphore semaphore = new Semaphore(0);
        CameraController fakeController = new CameraController(mContext) {
            @Override
            void onTapToFocus(MeteringPointFactory meteringPointFactory, float x, float y) {
                semaphore.release();
            }

            @Nullable
            @Override
            Camera startCamera() {
                return null;
            }
        };
        mInstrumentation.runOnMainSync(() -> {
            PreviewView previewView = new PreviewView(mContext);
            previewView.setController(fakeController);
            notifyLatchWhenLayoutReady(previewView, countDownLatch);
            setContentView(previewView);
        });
        // Wait for layout ready
        assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isTrue();

        // Act: click on PreviewView
        mUiDevice.findObject(new UiSelector().index(0)).click();

        // Assert: tap-to-focus is invoked.
        assertThat(semaphore.tryAcquire(1, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void previewView_onClickListenerWorks()
            throws InterruptedException, UiObjectNotFoundException {
        // Arrange.
        Semaphore semaphore = new Semaphore(0);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        mInstrumentation.runOnMainSync(() -> {
            PreviewView previewView = new PreviewView(mContext);
            previewView.setOnClickListener(view -> semaphore.release());
            notifyLatchWhenLayoutReady(previewView, countDownLatch);
            setContentView(previewView);
        });
        // Wait for layout ready
        assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isTrue();

        // Act: click on PreviewView.
        mUiDevice.findObject(new UiSelector().index(0)).click();

        // Assert: view is clicked.
        assertThat(semaphore.tryAcquire(1, TimeUnit.SECONDS)).isTrue();
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
        final CameraInfoInternal cameraInfo =
                createCameraInfo(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2_LEGACY);
        final PreviewView previewView = new PreviewView(mContext);
        setContentView(previewView);
        previewView.setImplementationMode(PERFORMANCE);
        Preview.SurfaceProvider surfaceProvider = previewView.getSurfaceProvider();
        surfaceProvider.onSurfaceRequested(createSurfaceRequest(cameraInfo));

        assertThat(previewView.mImplementation).isInstanceOf(TextureViewImplementation.class);
    }

    @Test
    @UiThreadTest
    public void usesTextureView_whenAPILevelNotNewerThanN() {
        assumeTrue(Build.VERSION.SDK_INT <= 24);
        final CameraInfoInternal cameraInfo =
                createCameraInfo(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2);

        final PreviewView previewView = new PreviewView(mContext);
        setContentView(previewView);
        previewView.setImplementationMode(PERFORMANCE);
        Preview.SurfaceProvider surfaceProvider = previewView.getSurfaceProvider();
        surfaceProvider.onSurfaceRequested(createSurfaceRequest(cameraInfo));

        assertThat(previewView.mImplementation).isInstanceOf(TextureViewImplementation.class);
    }

    @Test
    @UiThreadTest
    public void usesSurfaceView_whenNonLegacyDevice_andAPILevelNewerThanN() {
        assumeTrue(Build.VERSION.SDK_INT > 24);
        final CameraInfoInternal cameraInfo =
                createCameraInfo(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2);

        final PreviewView previewView = new PreviewView(mContext);
        setContentView(previewView);
        previewView.setImplementationMode(PERFORMANCE);
        Preview.SurfaceProvider surfaceProvider = previewView.getSurfaceProvider();
        surfaceProvider.onSurfaceRequested(createSurfaceRequest(cameraInfo));

        assertThat(previewView.mImplementation).isInstanceOf(SurfaceViewImplementation.class);
    }

    @Test
    @UiThreadTest
    public void usesTextureView_whenNonLegacyDevice_andImplModeIsTextureView() {
        final CameraInfoInternal cameraInfo =
                createCameraInfo(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2);

        final PreviewView previewView = new PreviewView(mContext);
        setContentView(previewView);
        previewView.setImplementationMode(COMPATIBLE);
        Preview.SurfaceProvider surfaceProvider = previewView.getSurfaceProvider();
        surfaceProvider.onSurfaceRequested(createSurfaceRequest(cameraInfo));

        assertThat(previewView.mImplementation).isInstanceOf(TextureViewImplementation.class);
    }

    @Test
    public void correctSurfacePixelFormat_whenRGBA8888IsRequired() throws Throwable {
        final CameraInfoInternal cameraInfo =
                createCameraInfo(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2);
        SurfaceRequest surfaceRequest = createRgb8888SurfaceRequest(cameraInfo);
        ListenableFuture<Surface> future = surfaceRequest.getDeferrableSurface().getSurface();

        mActivityScenario.onActivity(activity -> {
            final PreviewView previewView = new PreviewView(mContext);
            setContentView(previewView);

            previewView.setImplementationMode(PERFORMANCE);
            Preview.SurfaceProvider surfaceProvider = previewView.getSurfaceProvider();
            surfaceProvider.onSurfaceRequested(surfaceRequest);
        });
        final Surface[] surface = new Surface[1];
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Futures.addCallback(future, new FutureCallback<Surface>() {
            @Override
            public void onSuccess(@Nullable Surface result) {
                surface[0] = result;
                countDownLatch.countDown();
            }

            @Override
            public void onFailure(Throwable t) {
            }
        }, CameraXExecutors.directExecutor());

        assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(SurfaceFormatUtil.getSurfaceFormat(surface[0])).isEqualTo(PixelFormat.RGBA_8888);
    }

    @Test
    public void canCreateValidMeteringPoint() throws Exception {
        final CameraInfoInternal cameraInfo = createCameraInfo(90,
                CameraInfo.IMPLEMENTATION_TYPE_CAMERA2, CameraSelector.LENS_FACING_BACK);

        CountDownLatch countDownLatch = new CountDownLatch(1);
        mInstrumentation.runOnMainSync(() -> {
            mPreviewView = new PreviewView(mContext);
            notifyLatchWhenLayoutReady(mPreviewView, countDownLatch);
            setContentView(mPreviewView);
            Preview.SurfaceProvider surfaceProvider = mPreviewView.getSurfaceProvider();
            surfaceProvider.onSurfaceRequested(createSurfaceRequest(cameraInfo));
        });
        // Wait for layout ready
        assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isTrue();

        updateCropRectAndWaitForIdle(DEFAULT_CROP_RECT);

        mInstrumentation.runOnMainSync(() -> {
            MeteringPointFactory factory = mPreviewView.getMeteringPointFactory();
            MeteringPoint point = factory.createPoint(100, 100);
            assertPointIsValid(point);
        });
    }

    private void assertPointIsValid(MeteringPoint point) {
        assertThat(point.getX() >= 0f && point.getX() <= 1.0f).isTrue();
        assertThat(point.getY() >= 0f && point.getY() <= 1.0f).isTrue();
    }

    @Test
    public void meteringPointFactoryAutoAdjusted_whenViewSizeChange() throws Exception {
        final CameraInfoInternal cameraInfo = createCameraInfo(90,
                CameraInfo.IMPLEMENTATION_TYPE_CAMERA2, CameraSelector.LENS_FACING_BACK);

        mInstrumentation.runOnMainSync(() -> {
            mPreviewView = new PreviewView(mContext);
            mMeteringPointFactory = mPreviewView.getMeteringPointFactory();

            setContentView(mPreviewView);
            Preview.SurfaceProvider surfaceProvider = mPreviewView.getSurfaceProvider();
            surfaceProvider.onSurfaceRequested(createSurfaceRequest(cameraInfo));
        });
        updateCropRectAndWaitForIdle(DEFAULT_CROP_RECT);

        changeViewSize(mPreviewView, 1000, 1000);
        MeteringPoint point1 = mMeteringPointFactory.createPoint(100, 100);

        changeViewSize(mPreviewView, 500, 400);
        MeteringPoint point2 = mMeteringPointFactory.createPoint(100, 100);

        assertPointIsValid(point1);
        assertPointIsValid(point2);
        // These points should be different because the layout is changed.
        assertPointsAreDifferent(point1, point2);
    }

    private void changeViewSize(PreviewView previewView, int newWidth, int newHeight)
            throws InterruptedException {
        CountDownLatch latchToWaitForLayoutChange = new CountDownLatch(1);
        mInstrumentation.runOnMainSync(() -> {
            previewView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom,
                        int oldLeft,
                        int oldTop, int oldRight, int oldBottom) {
                    if (previewView.getWidth() == newWidth
                            && previewView.getHeight() == newHeight) {
                        latchToWaitForLayoutChange.countDown();
                        previewView.removeOnLayoutChangeListener(this);
                    }
                }
            });
            previewView.setLayoutParams(new FrameLayout.LayoutParams(newWidth, newHeight));
        });

        // Wait until the new layout is changed.
        assertThat(latchToWaitForLayoutChange.await(1, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void meteringPointFactoryAutoAdjusted_whenScaleTypeChanged() throws Exception {
        final CameraInfoInternal cameraInfo = createCameraInfo(90,
                CameraInfo.IMPLEMENTATION_TYPE_CAMERA2, CameraSelector.LENS_FACING_BACK);
        mInstrumentation.runOnMainSync(() -> {
            mPreviewView = new PreviewView(mContext);
            mMeteringPointFactory = mPreviewView.getMeteringPointFactory();
            setContentView(mPreviewView);
            Preview.SurfaceProvider surfaceProvider = mPreviewView.getSurfaceProvider();
            surfaceProvider.onSurfaceRequested(createSurfaceRequest(cameraInfo));
        });
        updateCropRectAndWaitForIdle(DEFAULT_CROP_RECT);

        // Surface resolution is 640x480 , set a different size for PreviewView.
        changeViewSize(mPreviewView, 800, 700);

        mInstrumentation.runOnMainSync(
                () -> mPreviewView.setScaleType(PreviewView.ScaleType.FILL_CENTER));
        MeteringPoint point1 = mMeteringPointFactory.createPoint(100, 100);

        mInstrumentation.runOnMainSync(
                () -> mPreviewView.setScaleType(PreviewView.ScaleType.FIT_START));
        MeteringPoint point2 = mMeteringPointFactory.createPoint(100, 100);

        assertPointIsValid(point1);
        assertPointIsValid(point2);
        // These points should be different
        assertPointsAreDifferent(point1, point2);
    }

    @Test
    public void meteringPointFactoryAutoAdjusted_whenTransformationInfoChanged() throws Exception {
        final CameraInfoInternal cameraInfo1 = createCameraInfo(90,
                CameraInfo.IMPLEMENTATION_TYPE_CAMERA2, CameraSelector.LENS_FACING_BACK);
        final CameraInfoInternal cameraInfo2 = createCameraInfo(270,
                CameraInfo.IMPLEMENTATION_TYPE_CAMERA2, CameraSelector.LENS_FACING_FRONT);

        mInstrumentation.runOnMainSync(() -> {
            mPreviewView = new PreviewView(mContext);
            mMeteringPointFactory = mPreviewView.getMeteringPointFactory();
            setContentView(mPreviewView);
            Preview.SurfaceProvider surfaceProvider = mPreviewView.getSurfaceProvider();
            surfaceProvider.onSurfaceRequested(createSurfaceRequest(cameraInfo1));
        });

        changeViewSize(mPreviewView, 1000, 1000);
        updateCropRectAndWaitForIdle(DEFAULT_CROP_RECT);

        // get a MeteringPoint from a non-center point.
        MeteringPoint point1 = mMeteringPointFactory.createPoint(100, 120);

        mInstrumentation.runOnMainSync(() -> {
            setContentView(mPreviewView);
            Preview.SurfaceProvider surfaceProvider = mPreviewView.getSurfaceProvider();
            surfaceProvider.onSurfaceRequested(createSurfaceRequest(cameraInfo2));
        });
        updateCropRectAndWaitForIdle(SMALLER_CROP_RECT);

        MeteringPoint point2 = mMeteringPointFactory.createPoint(100, 120);

        assertPointIsValid(point1);
        assertPointIsValid(point2);
        // These points should be different
        assertPointsAreDifferent(point1, point2);
    }

    private void assertPointsAreDifferent(MeteringPoint point1, MeteringPoint point2) {
        assertThat(point1.getX() != point2.getX() || point1.getY() != point2.getY()).isTrue();
    }

    private void notifyLatchWhenLayoutReady(PreviewView previewView,
            CountDownLatch countDownLatch) {
        previewView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                    int oldLeft,
                    int oldTop, int oldRight, int oldBottom) {
                if (v.getWidth() > 0 && v.getHeight() > 0) {
                    countDownLatch.countDown();
                    previewView.removeOnLayoutChangeListener(this);
                }
            }
        });
    }

    @Test
    @UiThreadTest
    public void meteringPointInvalid_whenPreviewViewWidthOrHeightIs0() {
        final CameraInfoInternal cameraInfo = createCameraInfo(90,
                CameraInfo.IMPLEMENTATION_TYPE_CAMERA2, CameraSelector.LENS_FACING_BACK);

        final PreviewView previewView = new PreviewView(mContext);
        Preview.SurfaceProvider surfaceProvider = previewView.getSurfaceProvider();
        surfaceProvider.onSurfaceRequested(createSurfaceRequest(cameraInfo));

        MeteringPointFactory factory = previewView.getMeteringPointFactory();

        //Width and height is 0,  but surface is requested,
        //verifying the factory only creates invalid points.
        MeteringPoint point = factory.createPoint(100, 100);
        assertPointIsInvalid(point);
    }

    private void assertPointIsInvalid(MeteringPoint point) {
        assertThat(point.getX() < 0f || point.getX() > 1.0f).isTrue();
        assertThat(point.getY() < 0f || point.getY() > 1.0f).isTrue();
    }

    @Test
    @UiThreadTest
    public void meteringPointInvalid_beforeCreatingSurfaceProvider() {
        final PreviewView previewView = new PreviewView(mContext);
        // make PreviewView.getWidth() getHeight not 0.
        setContentView(previewView);
        MeteringPointFactory factory = previewView.getMeteringPointFactory();

        //verifying the factory only creates invalid points.
        MeteringPoint point = factory.createPoint(100, 100);
        assertPointIsInvalid(point);
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
    public void defaultImplementationMode_isPerformance() {
        PreviewView previewView = new PreviewView(mContext);
        assertThat(previewView.getImplementationMode()).isEqualTo(PERFORMANCE);
    }

    @Test
    @UiThreadTest
    public void getsImplementationModeFromXmlLayout() {
        PreviewView previewView = (PreviewView) LayoutInflater.from(mContext).inflate(
                R.layout.preview_view_implementation_mode_compatible, null);
        assertThat(previewView.getImplementationMode()).isEqualTo(COMPATIBLE);
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
    public void redrawsPreview_whenLayoutResized() {
        final AtomicReference<PreviewView> previewView = new AtomicReference<>();
        final AtomicReference<FrameLayout> container = new AtomicReference<>();
        final PreviewViewImplementation implementation = mock(TestPreviewViewImplementation.class);

        mActivityScenario.onActivity(activity -> {
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
    public void doesNotRedrawPreview_whenDetachedFromWindow() {
        final AtomicReference<PreviewView> previewView = new AtomicReference<>();
        final AtomicReference<FrameLayout> container = new AtomicReference<>();
        final PreviewViewImplementation implementation = mock(TestPreviewViewImplementation.class);

        mActivityScenario.onActivity(activity -> {
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
    public void redrawsPreview_whenReattachedToWindow() {
        final AtomicReference<PreviewView> previewView = new AtomicReference<>();
        final AtomicReference<FrameLayout> container = new AtomicReference<>();
        final PreviewViewImplementation implementation = mock(TestPreviewViewImplementation.class);

        mActivityScenario.onActivity(activity -> {
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
        final Preview.SurfaceProvider surfaceProvider = previewView.getSurfaceProvider();
        final CameraInfoInternal cameraInfo =
                createCameraInfo(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2);
        surfaceProvider.onSurfaceRequested(createSurfaceRequest(cameraInfo));

        // Create a new surfaceProvider
        previewView.getSurfaceProvider();

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
        mActivityScenario.onActivity(activity -> activity.setContentView(view));
    }

    private SurfaceRequest createRgb8888SurfaceRequest(CameraInfoInternal cameraInfo) {
        return createSurfaceRequest(cameraInfo, true);
    }

    private SurfaceRequest createSurfaceRequest(CameraInfoInternal cameraInfo) {
        return createSurfaceRequest(cameraInfo, false);
    }

    private SurfaceRequest createSurfaceRequest(CameraInfoInternal cameraInfo,
            boolean isRGBA8888Required) {
        final FakeCamera fakeCamera = new FakeCamera(/*cameraControl=*/null, cameraInfo);

        final SurfaceRequest surfaceRequest = new SurfaceRequest(DEFAULT_SURFACE_SIZE, fakeCamera,
                isRGBA8888Required);
        mSurfaceRequestList.add(surfaceRequest);
        return surfaceRequest;
    }

    private CameraInfoInternal createCameraInfo(String implementationType) {
        FakeCameraInfoInternal cameraInfoInternal = new FakeCameraInfoInternal();
        cameraInfoInternal.setImplementationType(implementationType);
        return cameraInfoInternal;
    }

    private CameraInfoInternal createCameraInfo(int rotationDegrees, String implementationType,
            @CameraSelector.LensFacing int lensFacing) {
        FakeCameraInfoInternal cameraInfoInternal = new FakeCameraInfoInternal(rotationDegrees,
                lensFacing);
        cameraInfoInternal.setImplementationType(implementationType);
        return cameraInfoInternal;
    }

    private void updateCropRectAndWaitForIdle(Rect cropRect) {
        for (SurfaceRequest surfaceRequest : mSurfaceRequestList) {
            surfaceRequest.updateTransformationInfo(
                    SurfaceRequest.TransformationInfo.of(cropRect, 0, Surface.ROTATION_0));
        }
        mInstrumentation.waitForIdleSync();
    }

    /**
     * An empty implementation of {@link PreviewViewImplementation} used for testing. It allows
     * mocking {@link PreviewViewImplementation} since the latter is package private.
     */
    public static class TestPreviewViewImplementation extends PreviewViewImplementation {

        TestPreviewViewImplementation(@NonNull FrameLayout parent,
                @NonNull PreviewTransformation previewTransform) {
            super(parent, previewTransform);
        }

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
