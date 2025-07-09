/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.scenecore.impl;

import static androidx.xr.runtime.testing.math.MathAssertions.assertPose;
import static androidx.xr.runtime.testing.math.MathAssertions.assertVector3;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.Futures.immediateFuture;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;

import androidx.xr.runtime.internal.ActivityPose.HitTestFilter;
import androidx.xr.runtime.internal.HitTestResult;
import androidx.xr.runtime.internal.Space;
import androidx.xr.runtime.math.Matrix4;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.impl.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.impl.impress.FakeImpressApiImpl;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.PerceptionLibraryConstants;
import androidx.xr.scenecore.impl.perception.Session;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;

import com.android.extensions.xr.ShadowXrExtensions;
import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Node;
import com.android.extensions.xr.node.Vec3;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.ar.imp.view.splitengine.ImpSplitEngineRenderer;
import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.ScheduledExecutorService;

@RunWith(RobolectricTestRunner.class)
public final class EntityTest {
    private final XrExtensions mXrExtensions = XrExtensionsProvider.getXrExtensions();
    private final EntityManager mEntityManager = new EntityManager();
    private final FakeScheduledExecutorService mFakeScheduledExecutorService =
            new FakeScheduledExecutorService();
    private final Pose mTestPose = new Pose(new Vector3(1f, 2f, 3f), Quaternion.Identity);
    private JxrPlatformAdapterAxr mJxrPlatformAdapterAxr;
    private TestEntity mEntity;
    private Activity mActivity;

    static class TestEntity extends AndroidXrEntity {
        TestEntity(
                Context context,
                Node node,
                XrExtensions extensions,
                EntityManager entityManager,
                ScheduledExecutorService executor) {
            super(context, node, extensions, entityManager, executor);
        }
    }

    @Before
    public void setUp() {
        mActivity = Robolectric.buildActivity(Activity.class).create().start().get();

        PerceptionLibrary perceptionLibrary = mock(PerceptionLibrary.class);
        ShadowXrExtensions.extract(mXrExtensions)
                .setOpenXrWorldSpaceType(PerceptionLibraryConstants.OPEN_XR_SPACE_TYPE_VIEW);
        when(perceptionLibrary.initSession(
                        mActivity,
                        PerceptionLibraryConstants.OPEN_XR_SPACE_TYPE_VIEW,
                        mFakeScheduledExecutorService))
                .thenReturn(immediateFuture(mock(Session.class)));
        when(perceptionLibrary.getActivity()).thenReturn(mActivity);
        mJxrPlatformAdapterAxr =
                JxrPlatformAdapterAxr.create(
                        mActivity,
                        mFakeScheduledExecutorService,
                        mXrExtensions,
                        new FakeImpressApiImpl(),
                        mEntityManager,
                        perceptionLibrary,
                        mock(SplitEngineSubspaceManager.class),
                        mock(ImpSplitEngineRenderer.class),
                        false,
                        /* unscaledGravityAlignedActivitySpace= */ false);
        mEntity =
                new TestEntity(
                        mActivity,
                        mXrExtensions.createNode(),
                        mXrExtensions,
                        mEntityManager,
                        mFakeScheduledExecutorService);
        mEntity.setParent(mJxrPlatformAdapterAxr.getActivitySpace());
    }

    @After
    public void tearDown() {
        mJxrPlatformAdapterAxr.dispose();
        mJxrPlatformAdapterAxr = null;
    }

    @Test
    public void getPose_defaultsToPoseInParentSpace() {
        mEntity.setPose(mTestPose);
        assertPose(mEntity.getPose(Space.PARENT), mTestPose);
    }

    @Test
    public void getPose_parentSpace_returnsParentPose() {
        ActivitySpaceImpl activitySpace =
                (ActivitySpaceImpl) mJxrPlatformAdapterAxr.getActivitySpace();
        activitySpace.setOpenXrReferenceSpacePose(
                Matrix4.fromTrs(
                        new Vector3(5f, 6f, 7f),
                        Quaternion.fromEulerAngles(22f, 33f, 44f),
                        new Vector3(2f, 2f, 2f)));
        assertVector3(activitySpace.getScale(Space.PARENT), new Vector3(2f, 2f, 2f));

        mEntity.setPose(mTestPose, Space.PARENT);
        assertPose(mEntity.getPose(Space.PARENT), mTestPose);
    }

    @Test
    public void getPose_activitySpace_returnsActivitySpacePose() {
        ActivitySpaceImpl activitySpace =
                (ActivitySpaceImpl) mJxrPlatformAdapterAxr.getActivitySpace();
        activitySpace.setOpenXrReferenceSpacePose(
                Matrix4.fromTrs(
                        new Vector3(5f, 6f, 7f),
                        Quaternion.fromEulerAngles(22f, 33f, 44f),
                        new Vector3(2f, 2f, 2f)));
        assertVector3(activitySpace.getScale(Space.PARENT), new Vector3(2f, 2f, 2f));

        mEntity.setParent(activitySpace);
        mEntity.setPose(mTestPose, Space.PARENT);
        assertPose(mEntity.getPose(Space.ACTIVITY), mTestPose);
    }

    @Test
    public void getPose_worldSpace_returnsWorldSpacePose() {
        mEntity.setPose(mTestPose, Space.REAL_WORLD);

        assertPose(mEntity.getPose(Space.REAL_WORLD), mTestPose);
    }

    @Test
    public void getPose_invalidSpace_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> mEntity.getPose(999));
    }

    @Test
    public void setPose_parentSpace_setsPoseInParentSpace() {
        mEntity.setPose(mTestPose, Space.PARENT);

        assertPose(mEntity.getPose(Space.PARENT), mTestPose);
    }

    @Test
    public void setPose_activitySpace_setsActivitySpacePose() {
        mEntity.setPose(mTestPose, Space.PARENT);
        TestEntity child =
                new TestEntity(
                        mActivity,
                        mXrExtensions.createNode(),
                        mXrExtensions,
                        mEntityManager,
                        mFakeScheduledExecutorService);
        child.setParent(mEntity);
        child.setPose(mTestPose, Space.PARENT);

        assertPose(
                child.getPose(Space.ACTIVITY),
                new Pose(new Vector3(2.0f, 4.0f, 6.0f), Quaternion.Identity));
    }

    @Test
    public void setPose_worldSpace_setsWorldSpacePose() {
        mEntity.setPose(mTestPose, Space.REAL_WORLD);

        assertPose(mEntity.getPose(Space.REAL_WORLD), mTestPose);
    }

    @Test
    public void setPose_invalidSpace_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> mEntity.setPose(new Pose(), 999));
    }

    @Test
    public void getScale_parentSpace_returnsParentScale() {
        Vector3 scale = new Vector3(1.0f, 2.0f, 3.0f);
        mEntity.setScale(scale, Space.PARENT);

        assertVector3(mEntity.getScale(Space.PARENT), scale);
    }

    @Test
    public void getScale_activitySpace_returnsActivitySpaceScale() {
        Vector3 scale = new Vector3(1.0f, 2.0f, 3.0f);
        mEntity.setScale(scale, Space.PARENT);

        assertVector3(mEntity.getScale(Space.PARENT), scale);
        assertVector3(mEntity.getScale(Space.ACTIVITY), scale);
    }

    @Test
    public void getScale_worldSpace_returnsWorldSpaceScale() {
        ActivitySpaceImpl activitySpace =
                (ActivitySpaceImpl) mJxrPlatformAdapterAxr.getActivitySpace();
        activitySpace.mWorldSpaceScale = new Vector3(2.0f, 2.0f, 2.0f);
        Vector3 scale = new Vector3(1.0f, 2.0f, 3.0f);
        mEntity.setScale(scale, Space.PARENT);

        assertVector3(
                mEntity.getScale(Space.REAL_WORLD), scale.times(activitySpace.mWorldSpaceScale));
    }

    @Test
    public void getScale_invalidSpace_throwsException() {
        Vector3 scale = new Vector3(1.0f, 2.0f, 3.0f);
        mEntity.setScale(scale, Space.PARENT);

        assertThrows(IllegalArgumentException.class, () -> mEntity.getScale(999));
    }

    @Test
    public void setScaleActivitySpace_setsActivitySpaceScale() {
        Vector3 scale = new Vector3(1.0f, 2.0f, 3.0f);
        mEntity.setScale(scale, Space.PARENT);
        TestEntity child =
                new TestEntity(
                        mActivity,
                        mXrExtensions.createNode(),
                        mXrExtensions,
                        mEntityManager,
                        mFakeScheduledExecutorService);
        child.setParent(mEntity);
        child.setScale(scale.times(scale), Space.ACTIVITY);

        assertVector3(child.getScale(Space.ACTIVITY), scale.times(scale));
    }

    @Test
    public void setScale_worldSpace_setsWorldSpaceScale() {
        ActivitySpaceImpl activitySpace =
                (ActivitySpaceImpl) mJxrPlatformAdapterAxr.getActivitySpace();
        activitySpace.mWorldSpaceScale = new Vector3(2.0f, 2.0f, 2.0f);
        Vector3 scale = new Vector3(1.0f, 2.0f, 3.0f);
        mEntity.setScale(scale, Space.PARENT);
        TestEntity child =
                new TestEntity(
                        mActivity,
                        mXrExtensions.createNode(),
                        mXrExtensions,
                        mEntityManager,
                        mFakeScheduledExecutorService);
        child.setParent(mEntity);
        child.setScale(scale.times(scale.times(activitySpace.mWorldSpaceScale)), Space.REAL_WORLD);

        assertVector3(
                child.getScale(Space.REAL_WORLD),
                scale.times(scale.times(activitySpace.mWorldSpaceScale)));
    }

    @Test
    public void getPoseInActivitySpaceWithScale_returnsPose() {
        mEntity.setPose(mTestPose, Space.PARENT);
        mEntity.setScale(new Vector3(2f, 2f, 2f), Space.PARENT);
        TestEntity child =
                new TestEntity(
                        mActivity,
                        mXrExtensions.createNode(),
                        mXrExtensions,
                        mEntityManager,
                        mFakeScheduledExecutorService);
        child.setPose(mTestPose, Space.PARENT);
        child.setParent(mEntity);
        child.setScale(new Vector3(3f, 3f, 3f), Space.PARENT);
        TestEntity grandchild =
                new TestEntity(
                        mActivity,
                        mXrExtensions.createNode(),
                        mXrExtensions,
                        mEntityManager,
                        mFakeScheduledExecutorService);
        grandchild.setPose(mTestPose, Space.PARENT);
        grandchild.setParent(child);
        ActivitySpaceImpl activitySpace =
                (ActivitySpaceImpl) mJxrPlatformAdapterAxr.getActivitySpace();
        activitySpace.setOpenXrReferenceSpacePose(
                Matrix4.fromTrs(
                        new Vector3(5f, 6f, 7f),
                        Quaternion.fromEulerAngles(22f, 33f, 44f),
                        new Vector3(2f, 2f, 2f)));
        assertVector3(activitySpace.getScale(Space.PARENT), new Vector3(2f, 2f, 2f));

        assertPose(mEntity.getPose(Space.PARENT), mTestPose);
        assertPose(mEntity.getPose(Space.ACTIVITY), mTestPose);

        assertPose(child.getPose(Space.PARENT), mTestPose);
        assertPose(
                child.getPose(Space.ACTIVITY),
                new Pose(new Vector3(3f, 6f, 9f), Quaternion.Identity));

        grandchild.setPose(mTestPose, Space.PARENT);
        assertPose(grandchild.getPose(Space.PARENT), mTestPose);
        assertPose(
                grandchild.getPose(Space.ACTIVITY),
                new Pose(new Vector3(9f, 18f, 27f), Quaternion.Identity));
    }

    @Test
    public void setScale_invalidSpace_throwsException() {
        mEntity.setScale(new Vector3(1.0f, 2.0f, 3.0f), Space.PARENT);

        assertThrows(IllegalArgumentException.class, () -> mEntity.setScale(new Vector3(), 999));
    }

    @Test
    public void getAlpha_parentSpace_returnsParentAlpha() {
        mEntity.setAlpha(0.5f, Space.PARENT);

        assertThat(mEntity.getAlpha(Space.PARENT)).isEqualTo(0.5f);
    }

    @Test
    public void getAlpha_activitySpace_returnsActivitySpaceAlpha() {
        mEntity.setAlpha(0.5f, Space.PARENT);

        assertThat(mEntity.getAlpha(Space.ACTIVITY)).isEqualTo(0.5f);
    }

    @Test
    public void getAlpha_worldSpace_returnsWorldSpaceAlpha() {
        mEntity.setAlpha(0.5f, Space.REAL_WORLD);

        assertThat(mEntity.getAlpha(Space.REAL_WORLD)).isEqualTo(0.5f);
    }

    @Test
    public void getAlpha_invalidSpace_throwsException() {
        mEntity.setAlpha(0.5f, Space.PARENT);
        assertThrows(IllegalArgumentException.class, () -> mEntity.getAlpha(999));
    }

    @Test
    public void setAlpha_setsAlpha() {
        mEntity.setAlpha(0.5f, Space.PARENT);

        assertThat(mEntity.getAlpha(Space.PARENT)).isEqualTo(0.5f);
    }

    @Test
    public void setAlpha_parentSpace_setsParentAlpha() {
        mEntity.setAlpha(0.5f, Space.PARENT);

        assertThat(mEntity.getAlpha(Space.PARENT)).isEqualTo(0.5f);
    }

    @Test
    public void setAlpha_activitySpace_setsActivitySpaceAlpha() {
        mEntity.setAlpha(0.5f, Space.PARENT);
        TestEntity child =
                new TestEntity(
                        mActivity,
                        mXrExtensions.createNode(),
                        mXrExtensions,
                        mEntityManager,
                        mFakeScheduledExecutorService);
        child.setParent(mEntity);
        child.setAlpha(0.5f, Space.PARENT);

        assertThat(child.getAlpha(Space.ACTIVITY)).isEqualTo(0.25f);
    }

    @Test
    public void setAlpha_worldSpace_setsWorldSpaceAlpha() {
        mJxrPlatformAdapterAxr.getActivitySpace().setAlpha(4f, Space.PARENT);
        mEntity.setAlpha(0.5f, Space.PARENT);
        TestEntity child =
                new TestEntity(
                        mActivity,
                        mXrExtensions.createNode(),
                        mXrExtensions,
                        mEntityManager,
                        mFakeScheduledExecutorService);
        child.setParent(mEntity);
        child.setAlpha(0.5f, Space.PARENT);

        assertThat(child.getAlpha(Space.REAL_WORLD)).isEqualTo(1f);
    }

    @Test
    public void setAlpha_invalidSpace_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> mEntity.setAlpha(0.5f, 999));
    }

    @Test
    public void hitTest_returnsTransformedHitTest() throws Exception {
        float distance = 2.0f;
        Vec3 hitPosition = new Vec3(1.0f, 2.0f, 3.0f);
        Vec3 surfaceNormal = new Vec3(0.0f, 1.0f, 0.0f);
        int surfaceType = com.android.extensions.xr.space.HitTestResult.SURFACE_PANEL;
        com.android.extensions.xr.space.HitTestResult extensionsHitTestResult =
                new com.android.extensions.xr.space.HitTestResult.Builder(
                                distance, hitPosition, true, surfaceType)
                        .setSurfaceNormal(surfaceNormal)
                        .build();
        mEntity.setPose(
                new Pose(
                        new Vector3(1f, 1, 1f),
                        Quaternion.fromEulerAngles(new Vector3(90f, 0f, 0f))),
                Space.ACTIVITY);
        ShadowXrExtensions.extract(mXrExtensions)
                .setHitTestResult(mActivity, extensionsHitTestResult);

        ListenableFuture<HitTestResult> hitTestResultFuture =
                mEntity.hitTest(
                        new Vector3(1f, 1f, 1f), new Vector3(1f, 1f, 1f), HitTestFilter.SELF_SCENE);
        mFakeScheduledExecutorService.runAll();
        HitTestResult hitTestResult = hitTestResultFuture.get();

        assertThat(hitTestResult.getDistance()).isEqualTo(distance);
        // Since the entity is rotated 90 degrees about the x axis, the hit position should be
        // rotated
        // 90 degrees about the x axis.
        assertVector3(hitTestResult.getHitPosition(), new Vector3(0f, 2f, -1f));
        assertVector3(hitTestResult.getSurfaceNormal(), new Vector3(0f, 0f, -1f));
        assertThat(hitTestResult.getSurfaceType())
                .isEqualTo(HitTestResult.HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_PLANE);
    }

    @Test
    public void hitTest_withScaledActivitySpace_returnsTransformedHitTest() throws Exception {
        float distance = 2.0f;
        Vec3 hitPosition = new Vec3(0.5f, 1.0f, 1.5f);
        Vec3 surfaceNormal = new Vec3(0.0f, 1.0f, 0.0f);
        int surfaceType = com.android.extensions.xr.space.HitTestResult.SURFACE_PANEL;
        ((ActivitySpaceImpl) mJxrPlatformAdapterAxr.getActivitySpace())
                .setOpenXrReferenceSpacePose(Matrix4.fromScale(2f));
        com.android.extensions.xr.space.HitTestResult extensionsHitTestResult =
                new com.android.extensions.xr.space.HitTestResult.Builder(
                                distance, hitPosition, true, surfaceType)
                        .setSurfaceNormal(surfaceNormal)
                        .build();
        mEntity.setPose(
                new Pose(
                        new Vector3(0.5f, 0.5f, 0.5f),
                        Quaternion.fromEulerAngles(new Vector3(90f, 0f, 0f))),
                Space.ACTIVITY);
        ShadowXrExtensions.extract(mXrExtensions)
                .setHitTestResult(mActivity, extensionsHitTestResult);

        ListenableFuture<HitTestResult> hitTestResultFuture =
                mEntity.hitTest(
                        new Vector3(1f, 1f, 1f), new Vector3(1f, 1f, 1f), HitTestFilter.SELF_SCENE);
        mFakeScheduledExecutorService.runAll();
        HitTestResult hitTestResult = hitTestResultFuture.get();

        assertThat(hitTestResult.getDistance()).isEqualTo(distance);
        // Since the entity is rotated 90 degrees about the x axis, the hit position should be
        // rotated
        // 90 degrees about the x axis.
        assertVector3(hitTestResult.getHitPosition(), new Vector3(0f, 1f, -0.5f));
        assertVector3(hitTestResult.getSurfaceNormal(), new Vector3(0f, 0f, -1f));
        assertThat(hitTestResult.getSurfaceType())
                .isEqualTo(HitTestResult.HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_PLANE);
    }
}
