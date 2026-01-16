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

package androidx.xr.scenecore.spatial.core;

import static androidx.xr.runtime.testing.math.MathAssertions.assertPose;
import static androidx.xr.runtime.testing.math.MathAssertions.assertVector3;

import android.app.Activity;

import androidx.xr.runtime.NodeHolder;
import androidx.xr.runtime.math.Matrix4;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.testing.FakeGltfFeature;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Mat4f;
import com.android.extensions.xr.node.Node;
import com.android.extensions.xr.node.NodeTransform;
import com.android.extensions.xr.node.ShadowNode;
import com.android.extensions.xr.node.ShadowNodeTransform;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
public final class PerceptionSpaceScenePoseImplTest {
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

    private PerceptionSpaceScenePoseImpl mPerceptionSpaceScenePose;

    private void sendTransformEvent(NodeTransform nodeTransform) {
        ShadowNode shadowNode = ShadowNode.extract(mActivitySpace.getNode());
        shadowNode
                .getTransformExecutor()
                .execute(() -> shadowNode.getTransformListener().accept(nodeTransform));
    }

    /** Creates a generic glTF entity. */
    private GltfEntityImpl createGltfEntity() {
        NodeHolder<?> node = new NodeHolder<>(mXrExtensions.createNode(), Node.class);
        return new GltfEntityImpl(
                mActivity,
                new FakeGltfFeature(node),
                mActivitySpace,
                mXrExtensions,
                mEntityManager,
                mExecutor);
    }

    @Before
    public void setUp() {
        mPerceptionSpaceScenePose = new PerceptionSpaceScenePoseImpl(mActivitySpace);
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

        Pose poseInActivitySpace = mPerceptionSpaceScenePose.getPoseInActivitySpace();

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
                mPerceptionSpaceScenePose.transformPoseTo(new Pose(), mActivitySpace);

        Pose expectedPose = activitySpaceMatrix.getInverse().getPose();

        assertPose(transformedPose, expectedPose);
    }

    @Test
    public void transformPoseTo_toScaledEntity_returnsCorrectPose() {
        Matrix4 activitySpaceMatrix =
                Matrix4.fromTrs(
                        new Vector3(4.0f, 5.0f, 6.0f),
                        Quaternion.fromEulerAngles(new Vector3(90f, 0f, 0f)),
                        new Vector3(1.0f, 1.0f, 1.0f));
        sendTransformEvent(ShadowNodeTransform.create(new Mat4f(activitySpaceMatrix.getData())));
        mExecutor.runAll();
        GltfEntityImpl gltfEntity = createGltfEntity();
        gltfEntity.setScale(new Vector3(2.0f, 2.0f, 2.0f));

        Pose transformedPose = mPerceptionSpaceScenePose.transformPoseTo(new Pose(), gltfEntity);

        Pose unscaledPose = activitySpaceMatrix.getInverse().getPose();
        Pose expectedPose =
                new Pose(
                        unscaledPose.getTranslation().scale(new Vector3(0.5f, 0.5f, 0.5f)),
                        unscaledPose.getRotation());

        assertPose(transformedPose, expectedPose);
    }

    @Test
    public void getActivitySpaceScale_returnsInverseOfActivitySpaceWorldScale() {
        float activitySpaceScale = 5f;
        mActivitySpace.setOpenXrReferenceSpaceTransform(Matrix4.fromScale(activitySpaceScale));

        assertVector3(
                mPerceptionSpaceScenePose.getActivitySpaceScale(),
                new Vector3(1f, 1f, 1f).div(activitySpaceScale));
    }
}
