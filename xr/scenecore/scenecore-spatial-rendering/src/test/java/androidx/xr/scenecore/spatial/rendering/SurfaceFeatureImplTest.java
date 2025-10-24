/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.scenecore.spatial.rendering;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.view.Surface;

import androidx.xr.runtime.math.FloatSize2d;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.impl.impress.FakeImpressApiImpl;
import androidx.xr.scenecore.impl.impress.ImpressApi;
import androidx.xr.scenecore.runtime.CameraViewScenePose;
import androidx.xr.scenecore.runtime.Dimensions;
import androidx.xr.scenecore.runtime.PixelDimensions;
import androidx.xr.scenecore.runtime.SurfaceEntity;
import androidx.xr.scenecore.runtime.SurfaceEntity.Shape;
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Node;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.androidxr.splitengine.SubspaceNode;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
public final class SurfaceFeatureImplTest {
    private static final int SUBSPACE_ID = 5;

    private SurfaceFeatureImpl mSurfaceFeature;
    // TODO: Update this test to handle Entity.dispose() for Exceptions when FakeImpress is
    //       updated.
    private ImpressApi mImpressApi;
    private FakeImpressApiImpl mFakeImpressApi = new FakeImpressApiImpl();
    private XrExtensions mXrExtensions = XrExtensionsProvider.getXrExtensions();
    SplitEngineSubspaceManager mSplitEngineSubspaceManager = mock(SplitEngineSubspaceManager.class);

    private final ActivityController<Activity> mActivityController =
            Robolectric.buildActivity(Activity.class);
    private final Activity mActivity = mActivityController.create().start().get();

    @Before
    public void setUp() {
        mImpressApi = mock(ImpressApi.class);
        when(mImpressApi.createImpressNode()).thenReturn(mFakeImpressApi.createImpressNode());

        Assert.assertNotNull(mXrExtensions);
        Node node = mXrExtensions.createNode();
        SubspaceNode expectedSubspaceNode = new SubspaceNode(SUBSPACE_ID, node);

        when(mSplitEngineSubspaceManager.createSubspace(anyString(), anyInt()))
                .thenReturn(expectedSubspaceNode);

        createDefaultSurfaceFeature(new SurfaceEntity.Shape.Quad(new FloatSize2d(1f, 1f)));
    }

    @After
    public void tearDown() {
        if (mSurfaceFeature != null) {
            mSurfaceFeature.dispose();
        }
    }

    private SurfaceFeatureImpl createDefaultSurfaceFeature(Shape shape) {
        int stereoMode = SurfaceEntity.StereoMode.MONO;
        int surfaceProtection = 0;
        int useSuperSampling = 0;

        mSurfaceFeature =
                new SurfaceFeatureImpl(
                        mImpressApi,
                        mSplitEngineSubspaceManager,
                        mXrExtensions,
                        stereoMode,
                        shape,
                        surfaceProtection,
                        useSuperSampling);

        return mSurfaceFeature;
    }

    private CameraViewScenePose setupDefaultMockCameraView() {
        CameraViewScenePose cameraView = mock(CameraViewScenePose.class);
        when(cameraView.getCameraType())
                .thenReturn(CameraViewScenePose.CameraType.CAMERA_TYPE_LEFT_EYE);
        when(cameraView.getActivitySpacePose())
                .thenReturn(new Pose(new Vector3(0f, 0f, 0f), Quaternion.Identity));

        CameraViewScenePose.Fov fov =
                new CameraViewScenePose.Fov(
                        (float) Math.atan(1.0), (float) Math.atan(1.0),
                        (float) Math.atan(1.0), (float) Math.atan(1.0));
        when(cameraView.getFov()).thenReturn(fov);
        when(cameraView.getDisplayResolutionInPixels()).thenReturn(new PixelDimensions(1000, 1000));
        return cameraView;
    }

    // @Ignore // b/428211243 this test currently leaks android.view.Surface
    @Test
    public void setShape_setsShape() {
        SurfaceEntity.Shape expectedShape = new SurfaceEntity.Shape.Quad(new FloatSize2d(12f, 12f));
        mSurfaceFeature.setShape(expectedShape);
        SurfaceEntity.Shape shape = mSurfaceFeature.getShape();

        assertThat(shape.getClass()).isEqualTo(expectedShape.getClass());
        assertThat(shape.getDimensions()).isEqualTo(expectedShape.getDimensions());
        verify(mImpressApi)
                .setStereoSurfaceEntityCanvasShapeQuad(
                        mSurfaceFeature.getEntityImpressNode(), 12f, 12f);

        expectedShape = new SurfaceEntity.Shape.Sphere(11f);
        mSurfaceFeature.setShape(expectedShape);
        shape = mSurfaceFeature.getShape();

        assertThat(shape.getClass()).isEqualTo(expectedShape.getClass());
        assertThat(shape.getDimensions()).isEqualTo(expectedShape.getDimensions());
        verify(mImpressApi)
                .setStereoSurfaceEntityCanvasShapeSphere(
                        mSurfaceFeature.getEntityImpressNode(), 11f);

        expectedShape = new SurfaceEntity.Shape.Hemisphere(10f);
        mSurfaceFeature.setShape(expectedShape);
        shape = mSurfaceFeature.getShape();

        assertThat(shape.getClass()).isEqualTo(expectedShape.getClass());
        assertThat(shape.getDimensions()).isEqualTo(expectedShape.getDimensions());
        verify(mImpressApi)
                .setStereoSurfaceEntityCanvasShapeHemisphere(
                        mSurfaceFeature.getEntityImpressNode(), 10f);
    }

    // @Ignore // b/428211243 this test currently leaks android.view.Surface
    @Test
    public void setStereoMode_setsStereoMode() {
        int expectedStereoMode = SurfaceEntity.StereoMode.MONO;
        mSurfaceFeature.setStereoMode(expectedStereoMode);
        int stereoMode = mSurfaceFeature.getStereoMode();

        assertThat(stereoMode).isEqualTo(expectedStereoMode);
        verify(mImpressApi)
                .setStereoModeForStereoSurface(
                        mSurfaceFeature.getEntityImpressNode(), expectedStereoMode);

        expectedStereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM;
        mSurfaceFeature.setStereoMode(expectedStereoMode);
        stereoMode = mSurfaceFeature.getStereoMode();

        assertThat(stereoMode).isEqualTo(expectedStereoMode);
        verify(mImpressApi)
                .setStereoModeForStereoSurface(
                        mSurfaceFeature.getEntityImpressNode(), expectedStereoMode);
    }

    // @Ignore // b/428211243 this test currently leaks android.view.Surface
    @Test
    public void dispose_supports_reentry() {
        Shape.Quad quadShape = new Shape.Quad(new FloatSize2d(1.0f, 1.0f)); // 1m x 1m local
        mSurfaceFeature = createDefaultSurfaceFeature(quadShape);

        // Note that we don't test that dispose prevents manipulating other properties because that
        // is enforced at the API level, rather than the implementation level.
        mSurfaceFeature.dispose();
        mSurfaceFeature.dispose(); // shouldn't crash
    }

    @Test
    public void setColliderEnabled_forwardsToImpress() {
        mSurfaceFeature.setColliderEnabled(true);

        verify(mImpressApi)
                .setStereoSurfaceEntityColliderEnabled(
                        mSurfaceFeature.getEntityImpressNode(), true);

        // Set back to false
        mSurfaceFeature.setColliderEnabled(false);

        verify(mImpressApi)
                .setStereoSurfaceEntityColliderEnabled(
                        mSurfaceFeature.getEntityImpressNode(), false);
    }

    // @Ignore // b/428211243 this test currently leaks android.view.Surface
    @Test
    public void setEdgeFeather_forwardsToImpress() {
        float kFeatherRadiusX = 0.14f;
        float kFeatherRadiusY = 0.28f;
        SurfaceEntity.EdgeFeather expectedFeather =
                new SurfaceEntity.EdgeFeather.RectangleFeather(kFeatherRadiusX, kFeatherRadiusY);
        mSurfaceFeature.setEdgeFeather(expectedFeather);
        SurfaceEntity.EdgeFeather returnedFeather = mSurfaceFeature.getEdgeFeather();

        assertThat(returnedFeather).isEqualTo(expectedFeather);
        verify(mImpressApi)
                .setFeatherRadiusForStereoSurface(
                        mSurfaceFeature.getEntityImpressNode(), kFeatherRadiusX, kFeatherRadiusY);

        // Set back to NoFeathering to simulate turning feathering off
        expectedFeather = new SurfaceEntity.EdgeFeather.NoFeathering();
        mSurfaceFeature.setEdgeFeather(expectedFeather);
        returnedFeather = mSurfaceFeature.getEdgeFeather();

        assertThat(returnedFeather).isEqualTo(expectedFeather);
        verify(mImpressApi)
                .setFeatherRadiusForStereoSurface(mSurfaceFeature.getEntityImpressNode(), 0f, 0f);
    }

    @Test
    public void createSurfaceEntity_returnsStereoSurface() {
        final float kTestWidth = 14.0f;
        final float kTestHeight = 28.0f;
        final float kTestSphereRadius = 7.0f;
        final float kTestHemisphereRadius = 11.0f;

        SurfaceFeatureImpl surfaceEntityQuad =
                new SurfaceFeatureImpl(
                        mFakeImpressApi,
                        mSplitEngineSubspaceManager,
                        mXrExtensions,
                        SurfaceEntity.StereoMode.SIDE_BY_SIDE,
                        new SurfaceEntity.Shape.Quad(new FloatSize2d(kTestWidth, kTestHeight)),
                        SurfaceEntity.SurfaceProtection.NONE,
                        SurfaceEntity.SuperSampling.DEFAULT);
        FakeImpressApiImpl.StereoSurfaceEntityData quadData =
                mFakeImpressApi
                        .getStereoSurfaceEntities()
                        .get(surfaceEntityQuad.getEntityImpressNode());

        SurfaceFeatureImpl surfaceEntitySphere =
                new SurfaceFeatureImpl(
                        mFakeImpressApi,
                        mSplitEngineSubspaceManager,
                        mXrExtensions,
                        SurfaceEntity.StereoMode.TOP_BOTTOM,
                        new SurfaceEntity.Shape.Sphere(kTestSphereRadius),
                        SurfaceEntity.SurfaceProtection.NONE,
                        SurfaceEntity.SuperSampling.DEFAULT);
        FakeImpressApiImpl.StereoSurfaceEntityData sphereData =
                mFakeImpressApi
                        .getStereoSurfaceEntities()
                        .get(surfaceEntitySphere.getEntityImpressNode());

        SurfaceFeatureImpl surfaceEntityHemisphere =
                new SurfaceFeatureImpl(
                        mFakeImpressApi,
                        mSplitEngineSubspaceManager,
                        mXrExtensions,
                        SurfaceEntity.StereoMode.MONO,
                        new SurfaceEntity.Shape.Hemisphere(kTestHemisphereRadius),
                        SurfaceEntity.SurfaceProtection.NONE,
                        SurfaceEntity.SuperSampling.DEFAULT);
        FakeImpressApiImpl.StereoSurfaceEntityData hemisphereData =
                mFakeImpressApi
                        .getStereoSurfaceEntities()
                        .get(surfaceEntityHemisphere.getEntityImpressNode());

        assertThat(mFakeImpressApi.getStereoSurfaceEntities()).hasSize(3);

        // TODO: b/366588688 - Move these into tests for SurfaceEntityImpl
        assertThat(quadData.getStereoMode()).isEqualTo(SurfaceEntity.StereoMode.SIDE_BY_SIDE);
        assertThat(quadData.getCanvasShape())
                .isEqualTo(FakeImpressApiImpl.StereoSurfaceEntityData.CanvasShape.QUAD);
        assertThat(sphereData.getStereoMode()).isEqualTo(SurfaceEntity.StereoMode.TOP_BOTTOM);
        assertThat(sphereData.getCanvasShape())
                .isEqualTo(FakeImpressApiImpl.StereoSurfaceEntityData.CanvasShape.VR_360_SPHERE);
        assertThat(hemisphereData.getStereoMode()).isEqualTo(SurfaceEntity.StereoMode.MONO);
        assertThat(hemisphereData.getCanvasShape())
                .isEqualTo(
                        FakeImpressApiImpl.StereoSurfaceEntityData.CanvasShape.VR_180_HEMISPHERE);

        assertThat(quadData.getWidth()).isEqualTo(kTestWidth);
        assertThat(quadData.getHeight()).isEqualTo(kTestHeight);
        Dimensions quadDimensions = surfaceEntityQuad.getDimensions();
        assertThat(quadDimensions.width).isEqualTo(kTestWidth);
        assertThat(quadDimensions.height).isEqualTo(kTestHeight);
        assertThat(quadDimensions.depth).isEqualTo(0.0f);

        assertThat(sphereData.getRadius()).isEqualTo(kTestSphereRadius);
        Dimensions sphereDimensions = surfaceEntitySphere.getDimensions();
        assertThat(sphereDimensions.width).isEqualTo(kTestSphereRadius * 2.0f);
        assertThat(sphereDimensions.height).isEqualTo(kTestSphereRadius * 2.0f);
        assertThat(sphereDimensions.depth).isEqualTo(kTestSphereRadius * 2.0f);

        assertThat(hemisphereData.getRadius()).isEqualTo(kTestHemisphereRadius);
        Dimensions hemisphereDimensions = surfaceEntityHemisphere.getDimensions();
        assertThat(hemisphereDimensions.width).isEqualTo(kTestHemisphereRadius * 2.0f);
        assertThat(hemisphereDimensions.height).isEqualTo(kTestHemisphereRadius * 2.0f);
        assertThat(hemisphereDimensions.depth).isEqualTo(kTestHemisphereRadius);

        assertThat(quadData.getSurface()).isEqualTo(surfaceEntityQuad.getSurface());
        assertThat(sphereData.getSurface()).isEqualTo(surfaceEntitySphere.getSurface());
        assertThat(hemisphereData.getSurface()).isEqualTo(surfaceEntityHemisphere.getSurface());

        // Check that calls to set the Shape and StereoMode after construction call through
        // Change the Quad to a Sphere
        surfaceEntityQuad.setShape(new SurfaceEntity.Shape.Sphere(kTestSphereRadius));
        // change the StereoMode to Top/Bottom from Side/Side
        surfaceEntityQuad.setStereoMode(SurfaceEntity.StereoMode.TOP_BOTTOM);
        quadData =
                mFakeImpressApi
                        .getStereoSurfaceEntities()
                        .get(surfaceEntityQuad.getEntityImpressNode());

        assertThat(quadData.getCanvasShape())
                .isEqualTo(FakeImpressApiImpl.StereoSurfaceEntityData.CanvasShape.VR_360_SPHERE);
        assertThat(quadData.getStereoMode()).isEqualTo(SurfaceEntity.StereoMode.TOP_BOTTOM);

        Surface surface = surfaceEntityQuad.getSurface();

        assertThat(surface).isNotNull();
        assertThat(surface).isEqualTo(quadData.getSurface());
    }

    @Test
    public void setPixelDimensions_forwardsToImpress() {
        int kTestWidth = 100;
        int kTestHeight = 200;
        mSurfaceFeature.setSurfacePixelDimensions(kTestWidth, kTestHeight);
        verify(mImpressApi)
                .setStereoSurfaceEntitySurfaceSize(
                        mSurfaceFeature.getEntityImpressNode(), kTestWidth, kTestHeight);
    }
}
