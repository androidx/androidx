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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;

import android.Manifest;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.camera.camera2.Camera2AppConfig;
import androidx.camera.core.AppConfig;
import androidx.camera.core.CameraX;
import androidx.camera.core.DisplayOrientedMeteringPointFactory;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.CoreAppTestUtil;
import androidx.camera.testing.fakes.FakeActivity;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class TextureViewMeteringPointFactoryTest {
    public static final float TOLERANCE = 0.000001f;
    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);
    @Rule
    public ActivityTestRule<FakeActivity> mActivityRule =
            new ActivityTestRule<>(FakeActivity.class);

    private void setContentView(View view) throws Throwable {
        final Activity activity = mActivityRule.getActivity();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.setContentView(view);
            }
        });
    }

    private static final int WAIT_FRAMECOUNT = 1;
    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private FakeLifecycleOwner mLifecycle;
    private CountDownLatch mLatchForFrameReady;
    private Context mContext;
    private TextureView mTextureView;
    private int mWidth;
    private int mHeight;

    @Before
    public void setUp() throws Throwable {
        assumeTrue(CameraUtil.deviceHasCamera());
        CoreAppTestUtil.assumeCompatibleDevice();

        mContext = ApplicationProvider.getApplicationContext();
        AppConfig config = Camera2AppConfig.create(mContext);
        CameraX.init(mContext, config);
        mLifecycle = new FakeLifecycleOwner();
        mLatchForFrameReady = new CountDownLatch(1);
        mTextureView = new TextureView(mContext);
        setContentView(mTextureView);
    }

    @After
    public void tearDown() throws InterruptedException, ExecutionException {
        mInstrumentation.runOnMainSync(CameraX::unbindAll);
        CameraX.deinit().get();
    }

    @Test
    public void backCamera_translatedPoint_SameAsDisplayOriented() throws Throwable {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraX.LensFacing.BACK));

        startAndWaitForCameraReady(CameraX.LensFacing.BACK);

        TextureViewMeteringPointFactory factory = new TextureViewMeteringPointFactory(mTextureView);

        // Creates the DisplayOrientedMeteringPointFactory with same width / height as TextureView
        DisplayOrientedMeteringPointFactory displayFactory =
                new DisplayOrientedMeteringPointFactory(mContext, CameraX.LensFacing.BACK,
                        mTextureView.getWidth(), mTextureView.getHeight());

        // Uses DisplayOrientedMeteringPointFactory to verify if coordinates are correct.
        // DisplayOrientedMeteringPointFactory has the same width / height as the TextureView and
        // TextureViewMeteringPointFactory
        assertFactoryOutputSamePoints(factory, displayFactory);
    }

    @Test
    public void frontCamera_translatedPoint_SameAsDisplayOriented() throws Throwable {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraX.LensFacing.FRONT));

        startAndWaitForCameraReady(CameraX.LensFacing.FRONT);

        TextureViewMeteringPointFactory factory = new TextureViewMeteringPointFactory(mTextureView);

        // Creates the DisplayOrientedMeteringPointFactory with same width / height as TextureView
        DisplayOrientedMeteringPointFactory displayFactory =
                new DisplayOrientedMeteringPointFactory(mContext, CameraX.LensFacing.FRONT,
                        mWidth, mHeight);

        // Uses DisplayOrientedMeteringPointFactory to verify if coordinates are correct.
        // DisplayOrientedMeteringPointFactory has the same width / height as the TextureView and
        // TextureViewMeteringPointFactory
        assertFactoryOutputSamePoints(factory, displayFactory);

    }


    @Test
    public void xy_OutOfRange() throws Throwable {
        startAndWaitForCameraReady(CameraX.LensFacing.BACK);

        TextureViewMeteringPointFactory factory = new TextureViewMeteringPointFactory(mTextureView);

        // if x or y is not in range [0.. width or height],  the output will not be valid normalized
        // point ( value not in [0..1])
        MeteringPoint pt1 = factory.createPoint(-1, 0);
        assertThat(isValid(pt1)).isFalse();

        pt1 = factory.createPoint(0, -1);
        assertThat(isValid(pt1)).isFalse();

        pt1 = factory.createPoint(mWidth + 1, 0);
        assertThat(isValid(pt1)).isFalse();

        pt1 = factory.createPoint(0, mHeight + 1);
        assertThat(isValid(pt1)).isFalse();

        pt1 = factory.createPoint(-1, mHeight + 1);
        assertThat(isValid(pt1)).isFalse();

        pt1 = factory.createPoint(mWidth + 1, mHeight + 1);
        assertThat(isValid(pt1)).isFalse();

        pt1 = factory.createPoint(-1, -1);
        assertThat(isValid(pt1)).isFalse();

    }

    private boolean isValid(MeteringPoint pt) {
        boolean xValid = pt.getNormalizedCropRegionX() >= 0 && pt.getNormalizedCropRegionX() <= 1f;
        boolean yValid = pt.getNormalizedCropRegionY() >= 0 && pt.getNormalizedCropRegionY() <= 1f;
        return xValid && yValid;
    }

    private void startAndWaitForCameraReady(CameraX.LensFacing lensFacing)
            throws InterruptedException {
        PreviewConfig.Builder previewConfigBuilder =
                new PreviewConfig.Builder()
                        .setLensFacing(lensFacing);

        Preview preview = new Preview(previewConfigBuilder.build());
        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                preview.setOnPreviewOutputUpdateListener(
                        new Preview.OnPreviewOutputUpdateListener() {
                            @Override
                            public void onUpdated(@NonNull Preview.PreviewOutput output) {
                                mActivityRule.getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ViewGroup viewGroup = (ViewGroup) mTextureView.getParent();
                                        viewGroup.removeView(mTextureView);
                                        viewGroup.addView(mTextureView);

                                        mTextureView.setSurfaceTexture(output.getSurfaceTexture());
                                        output.getSurfaceTexture().setOnFrameAvailableListener(
                                                new SurfaceTexture.OnFrameAvailableListener() {
                                                    int mFrameCount = 0;
                                                    @Override
                                                    public void onFrameAvailable(
                                                            SurfaceTexture surfaceTexture) {
                                                        mFrameCount++;
                                                        if (mFrameCount == WAIT_FRAMECOUNT) {
                                                            mLatchForFrameReady.countDown();
                                                        }
                                                    }
                                                });
                                    }
                                });
                            }
                        });

                // SurfaceTexture#getTransformMatrix is initialized properly when camera starts
                // to output.
                CameraX.bindToLifecycle(mLifecycle, preview);
                mLifecycle.startAndResume();
            }
        });

        mLatchForFrameReady.await(3, TimeUnit.SECONDS);
        mWidth = mTextureView.getWidth();
        mHeight = mTextureView.getHeight();
    }

    private void assertFactoryOutputSamePoints(MeteringPointFactory factory1,
            MeteringPointFactory factory2) {
        MeteringPoint point1;
        MeteringPoint point2;

        // left-top corner
        point1 = factory1.createPoint(0f, 0f);
        point2 = factory2.createPoint(0f, 0f);
        assertThat(isValid(point1)).isTrue();
        Assert.assertEquals(point1.getNormalizedCropRegionX(), point2.getNormalizedCropRegionX(),
                TOLERANCE);
        Assert.assertEquals(point1.getNormalizedCropRegionY(), point2.getNormalizedCropRegionY(),
                TOLERANCE);

        // left-bottom corner
        point1 = factory1.createPoint(0f, mHeight);
        point2 = factory2.createPoint(0f, mHeight);

        assertThat(isValid(point1)).isTrue();
        Assert.assertEquals(point1.getNormalizedCropRegionX(), point2.getNormalizedCropRegionX(),
                TOLERANCE);
        Assert.assertEquals(point1.getNormalizedCropRegionY(), point2.getNormalizedCropRegionY(),
                TOLERANCE);

        // right-top corner
        point1 = factory1.createPoint(mWidth, 0f);
        point2 = factory2.createPoint(mWidth, 0f);

        assertThat(isValid(point1)).isTrue();
        Assert.assertEquals(point1.getNormalizedCropRegionX(), point2.getNormalizedCropRegionX(),
                TOLERANCE);
        Assert.assertEquals(point1.getNormalizedCropRegionY(), point2.getNormalizedCropRegionY(),
                TOLERANCE);

        // right-bottom corner
        point1 = factory1.createPoint(mWidth, mHeight);
        point2 = factory2.createPoint(mWidth, mHeight);

        assertThat(isValid(point1)).isTrue();
        Assert.assertEquals(point1.getNormalizedCropRegionX(), point2.getNormalizedCropRegionX(),
                TOLERANCE);
        Assert.assertEquals(point1.getNormalizedCropRegionY(), point2.getNormalizedCropRegionY(),
                TOLERANCE);

        // some random point
        point1 = factory1.createPoint(100, 120);
        point2 = factory2.createPoint(100, 120);

        assertThat(isValid(point1)).isTrue();
        Assert.assertEquals(point1.getNormalizedCropRegionX(), point2.getNormalizedCropRegionX(),
                TOLERANCE);
        Assert.assertEquals(point1.getNormalizedCropRegionY(), point2.getNormalizedCropRegionY(),
                TOLERANCE);
    }
}
