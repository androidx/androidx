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

import android.app.Activity;

import androidx.xr.runtime.math.Matrix4;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.impl.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.impl.impress.FakeImpressApiImpl;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Mat4f;
import com.android.extensions.xr.node.NodeTransform;
import com.android.extensions.xr.node.ShadowNode;
import com.android.extensions.xr.node.ShadowNodeTransform;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class PerceptionSpaceActivityPoseImplTest {

    private final AndroidXrEntity mActivitySpaceRoot = Mockito.mock(AndroidXrEntity.class);
    private final XrExtensions mXrExtensions = XrExtensionsProvider.getXrExtensions();
    private final FakeScheduledExecutorService mExecutor = new FakeScheduledExecutorService();
    private final EntityManager mEntityManager = new EntityManager();
    private final Activity mActivity =
            Robolectric.buildActivity(Activity.class).create().start().get();
    private final ActivitySpaceImpl mActivitySpace =
            new ActivitySpaceImpl(
                    mXrExtensions.createNode(),
                    mActivity,
                    mXrExtensions,
                    mEntityManager,
                    () -> mXrExtensions.getSpatialState(mActivity),
                    /* unscaledGravityAlignedActivitySpace= */ false,
                    mExecutor);
    private final FakeImpressApiImpl mFakeImpressApi = new FakeImpressApiImpl();
    private final SplitEngineSubspaceManager mSplitEngineSubspaceManager =
            Mockito.mock(SplitEngineSubspaceManager.class);

    private PerceptionSpaceActivityPoseImpl mPerceptionSpaceActivityPose;

    private void sendTransformEvent(NodeTransform nodeTransform) {
        ShadowNode shadowNode = ShadowNode.extract(mActivitySpace.getNode());
        shadowNode
                .getTransformExecutor()
                .execute(() -> shadowNode.getTransformListener().accept(nodeTransform));
    }

    /** Creates a generic glTF entity. */
    private GltfEntityImpl createGltfEntity() {
        long modelToken = -1;
        try {
            ListenableFuture<Long> modelTokenFuture =
                    mFakeImpressApi.loadGltfAsset("FakeGltfAsset.glb");
            // This resolves the transformation of the Future from a SplitEngine token to the JXR
            // GltfModelResource.  This is a hidden detail from the API surface's perspective.
            mExecutor.runAll();
            modelToken = modelTokenFuture.get();
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        GltfModelResourceImpl model = new GltfModelResourceImpl(modelToken);
        return new GltfEntityImpl(
                mActivity,
                model,
                mActivitySpace,
                mFakeImpressApi,
                mSplitEngineSubspaceManager,
                mXrExtensions,
                mEntityManager,
                mExecutor);
    }

    @Before
    public void setUp() {
        mPerceptionSpaceActivityPose =
                new PerceptionSpaceActivityPoseImpl(mActivitySpace, mActivitySpaceRoot);
    }

    @Test
    public void getPoseInActivitySpace_returnsInverseOfActivitySpacePose() {
        Matrix4 activitySpaceMatrix =
                Matrix4.fromTrs(
                        new Vector3(1.0f, 2.0f, 3.0f),
                        Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f)),
                        new Vector3(1.0f, 1.0f, 1.0f));
        sendTransformEvent(ShadowNodeTransform.create(new Mat4f(activitySpaceMatrix.getData())));
        mExecutor.runAll();

        Pose poseInActivitySpace = mPerceptionSpaceActivityPose.getPoseInActivitySpace();

        Pose expectedPose = activitySpaceMatrix.getInverse().getPose();
        assertPose(poseInActivitySpace, expectedPose);
    }

    @Test
    public void transformPoseTo_returnsCorrectPose() {
        Matrix4 activitySpaceMatrix =
                Matrix4.fromTrs(
                        new Vector3(4.0f, 5.0f, 6.0f),
                        Quaternion.fromEulerAngles(new Vector3(90f, 0f, 0f)),
                        new Vector3(1.0f, 1.0f, 1.0f));
        sendTransformEvent(ShadowNodeTransform.create(new Mat4f(activitySpaceMatrix.getData())));
        mExecutor.runAll();

        Pose transformedPose =
                mPerceptionSpaceActivityPose.transformPoseTo(new Pose(), mActivitySpace);

        Pose expectedPose = activitySpaceMatrix.getInverse().getPose();
        assertPose(transformedPose, expectedPose);
    }

    @Test
    public void transformPoseTo_toScaledEntity_returnsCorrectPose() {
        Matrix4 activitySpaceMatrix =
                Matrix4.fromTrs(
                        new Vector3(4.0f, 5.0f, 6.0f),
                        Quaternion.fromEulerAngles(new Vector3(90f, 0f, 0f)).toNormalized(),
                        new Vector3(1.0f, 1.0f, 1.0f));
        sendTransformEvent(ShadowNodeTransform.create(new Mat4f(activitySpaceMatrix.getData())));
        mExecutor.runAll();
        GltfEntityImpl gltfEntity = createGltfEntity();
        gltfEntity.setScale(new Vector3(2.0f, 2.0f, 2.0f));

        Pose transformedPose = mPerceptionSpaceActivityPose.transformPoseTo(new Pose(), gltfEntity);

        Pose unscaledPose = activitySpaceMatrix.getInverse().getPose();
        Pose expectedPose =
                new Pose(
                        unscaledPose.getTranslation().times(new Vector3(0.5f, 0.5f, 0.5f)),
                        unscaledPose.getRotation());
        assertPose(transformedPose, expectedPose);
    }

    @Test
    public void getActivitySpaceScale_returnsInverseOfActivitySpaceWorldScale() throws Exception {
        float activitySpaceScale = 5f;
        mActivitySpace.setOpenXrReferenceSpacePose(Matrix4.fromScale(activitySpaceScale));
        assertVector3(
                mPerceptionSpaceActivityPose.getActivitySpaceScale(),
                new Vector3(1f, 1f, 1f).div(activitySpaceScale));
    }
}
