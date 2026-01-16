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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.test.rule.GrantPermissionRule;
import androidx.xr.arcore.Anchor;
import androidx.xr.arcore.runtime.Anchor.PersistenceState;
import androidx.xr.arcore.runtime.ExportableAnchor;
import androidx.xr.arcore.testing.FakeRuntimeAnchor;
import androidx.xr.runtime.NodeHolder;
import androidx.xr.runtime.TrackingState;
import androidx.xr.runtime.math.Matrix4;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.runtime.AnchorEntity.OnStateChangedListener;
import androidx.xr.scenecore.runtime.AnchorEntity.State;
import androidx.xr.scenecore.runtime.Space;
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.testing.FakeGltfFeature;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Node;
import com.android.extensions.xr.node.NodeRepository;

import com.google.common.collect.ImmutableList;

import org.jspecify.annotations.NonNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.util.Objects;
import java.util.UUID;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
@SuppressLint("NewApi") // TODO: b/413661481 - Remove this suppression prior to JXR stable release.
public final class AnchorEntityImplTest extends SystemSpaceEntityImplTest {
    private static final long NATIVE_POINTER = 1234567890L;
    private final XrExtensions mXrExtensions = XrExtensionsProvider.getXrExtensions();
    private final OnStateChangedListener mAnchorStateListener =
            Mockito.mock(OnStateChangedListener.class);
    private final IBinder mSharedAnchorToken = Mockito.mock(IBinder.class);
    private final FakeScheduledExecutorService mExecutor = new FakeScheduledExecutorService();
    private final EntityManager mEntityManager = new EntityManager();

    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant("android.permission.SCENE_UNDERSTANDING");

    private ActivitySpaceImpl mActivitySpace;

    @Before
    public void doBeforeEachTest() {
        ActivityController<Activity> activityController = Robolectric.buildActivity(Activity.class);
        Activity activity = activityController.create().start().get();
        Node taskNode = Objects.requireNonNull(mXrExtensions).createNode();
        mActivitySpace =
                new ActivitySpaceImpl(
                        taskNode,
                        activity,
                        mXrExtensions,
                        mEntityManager,
                        () -> mXrExtensions.getSpatialState(activity),
                        /* unscaledGravityAlignedActivitySpace= */ false,
                        mExecutor);
        long currentTimeMillis = 1000000000L;
        SystemClock.setCurrentTimeMillis(currentTimeMillis);
        mEntityManager.addSystemSpaceActivityPose(new PerceptionSpaceScenePoseImpl(mActivitySpace));
    }

    /**
     * Returns the anchor entity impl. Used in the base SystemSpaceEntityImplTest to ensure that the
     * anchor entity complies with all the expected behaviors of a system space entity.
     */
    @Override
    protected SystemSpaceEntityImpl getSystemSpaceEntityImpl() {
        return createAnchorEntityWithRuntimeAnchor();
    }

    @Override
    protected FakeScheduledExecutorService getDefaultFakeExecutor() {
        return mExecutor;
    }

    @Override
    protected AndroidXrEntity createChildAndroidXrEntity() {
        return createGltfEntity();
    }

    @Override
    protected ActivitySpaceImpl getActivitySpaceEntity() {
        return mActivitySpace;
    }

    private AnchorEntityImpl createAnchorEntity() {
        Node node = Objects.requireNonNull(mXrExtensions).createNode();
        ActivityController<Activity> activityController = Robolectric.buildActivity(Activity.class);
        Activity activity = activityController.create().start().get();
        return AnchorEntityImpl.create(
                activity, node, mActivitySpace, mXrExtensions, mEntityManager, mExecutor);
    }

    private AnchorEntityImpl createAnchorEntityWithRuntimeAnchor() {
        Node node = mXrExtensions.createNode();
        ActivityController<Activity> activityController = Robolectric.buildActivity(Activity.class);
        Activity activity = activityController.create().start().get();
        AnchorEntityImpl anchorEntity =
                AnchorEntityImpl.create(
                        activity, node, mActivitySpace, mXrExtensions, mEntityManager, mExecutor);
        FakeExportableAnchor runtimeAnchor =
                new FakeExportableAnchor(
                        NATIVE_POINTER,
                        mSharedAnchorToken,
                        Pose.Identity,
                        TrackingState.TRACKING,
                        PersistenceState.NOT_PERSISTED,
                        null);
        anchorEntity.setAnchor(new Anchor(runtimeAnchor));
        return anchorEntity;
    }

    private AnchorEntityImpl createUnanchoredAnchorEntity() {
        Node node = Objects.requireNonNull(mXrExtensions).createNode();
        ActivityController<Activity> activityController = Robolectric.buildActivity(Activity.class);
        Activity activity = activityController.create().start().get();
        return AnchorEntityImpl.create(
                activity, node, mActivitySpace, mXrExtensions, mEntityManager, mExecutor);
    }

    /** Creates a generic glTF entity. */
    private GltfEntityImpl createGltfEntity() {
        ActivityController<Activity> activityController = Robolectric.buildActivity(Activity.class);
        Activity activity = activityController.create().start().get();
        NodeHolder<?> nodeHolder =
                new NodeHolder<>(Objects.requireNonNull(mXrExtensions).createNode(), Node.class);
        return new GltfEntityImpl(
                activity,
                new FakeGltfFeature(nodeHolder),
                mActivitySpace,
                mXrExtensions,
                mEntityManager,
                mExecutor);
    }

    @Test
    public void anchorEntityAddChildren_addsChildren() {
        GltfEntityImpl childEntity1 = createGltfEntity();
        GltfEntityImpl childEntity2 = createGltfEntity();
        AnchorEntityImpl parentEntity = createAnchorEntityWithRuntimeAnchor();

        parentEntity.addChild(childEntity1);

        assertThat(parentEntity.getChildren()).containsExactly(childEntity1);

        parentEntity.addChildren(ImmutableList.of(childEntity2));

        assertThat(childEntity1.getParent()).isEqualTo(parentEntity);
        assertThat(childEntity2.getParent()).isEqualTo(parentEntity);
        assertThat(parentEntity.getChildren()).containsExactly(childEntity1, childEntity2);

        Node parentNode = parentEntity.getNode();
        Node childNode1 = childEntity1.getNode();
        Node childNode2 = childEntity2.getNode();

        assertThat(NodeRepository.getInstance().getParent(childNode1)).isEqualTo(parentNode);
        assertThat(NodeRepository.getInstance().getParent(childNode2)).isEqualTo(parentNode);
    }

    @Test
    public void anchorEntitySetPose_throwsException() {
        AnchorEntityImpl anchorEntity = createAnchorEntityWithRuntimeAnchor();
        Pose pose = new Pose();
        assertThrows(UnsupportedOperationException.class, () -> anchorEntity.setPose(pose));
    }

    @Test
    public void anchorEntityGetPoseRelativeToParentSpace_throwsException() {
        AnchorEntityImpl anchorEntity = createAnchorEntityWithRuntimeAnchor();

        assertThrows(UnsupportedOperationException.class, () -> anchorEntity.getPose(Space.PARENT));
    }

    @Test
    public void anchorEntityGetPoseRelativeToActivitySpace_returnsActivitySpacePose() {
        AnchorEntityImpl anchorEntity = createAnchorEntityWithRuntimeAnchor();

        assertPose(anchorEntity.getPose(Space.ACTIVITY), anchorEntity.getPoseInActivitySpace());
    }

    @Test
    public void anchorEntityGetPoseRelativeToRealWorldSpace_returnsPerceptionSpacePose() {
        AnchorEntityImpl anchorEntity = createAnchorEntityWithRuntimeAnchor();

        assertPose(anchorEntity.getPose(Space.REAL_WORLD), anchorEntity.getPoseInPerceptionSpace());
    }

    @Test
    public void anchorEntitySetScale_throwsException() {
        AnchorEntityImpl anchorEntity = createAnchorEntityWithRuntimeAnchor();
        Vector3 scale = new Vector3(1, 1, 1);
        assertThrows(UnsupportedOperationException.class, () -> anchorEntity.setScale(scale));
    }

    @Test
    public void anchorEntityGetScale_throwsException() {
        AnchorEntityImpl anchorEntity = createAnchorEntityWithRuntimeAnchor();
        assertThrows(UnsupportedOperationException.class, anchorEntity::getScale);
    }

    @Test
    public void anchorEntityGetWorldSpaceScale_returnsIdentityScale() {
        AnchorEntityImpl anchorEntity = createAnchorEntityWithRuntimeAnchor();
        assertVector3(anchorEntity.getWorldSpaceScale(), new Vector3(1f, 1f, 1f));
    }

    @Test
    public void anchorEntityGetActivitySpaceScale_returnsInverseOfActivitySpace() {
        float activitySpaceScale = 5f;
        mActivitySpace.setOpenXrReferenceSpaceTransform(Matrix4.fromScale(activitySpaceScale));
        AnchorEntityImpl anchorEntity = createAnchorEntityWithRuntimeAnchor();
        assertVector3(
                anchorEntity.getActivitySpaceScale(),
                new Vector3(1f, 1f, 1f).div(activitySpaceScale));
    }

    @Test
    public void getPoseInActivitySpace_unanchored_returnsIdentityPose() {
        AnchorEntityImpl anchorEntity = createUnanchoredAnchorEntity();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1));
        mActivitySpace.setOpenXrReferenceSpaceTransform(Matrix4.Identity);
        anchorEntity.setOpenXrReferenceSpaceTransform(Matrix4.fromPose(pose));

        assertPose(anchorEntity.getPoseInActivitySpace(), new Pose());
    }

    @Test
    public void
            getPoseInActivitySpace_noActivitySpaceOpenXrReferenceSpacePose_returnsIdentityPose() {
        AnchorEntityImpl anchorEntity = createAnchorEntityWithRuntimeAnchor();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1));
        anchorEntity.setOpenXrReferenceSpaceTransform(Matrix4.fromPose(pose));
        assertPose(anchorEntity.getPoseInActivitySpace(), new Pose());
    }

    @Test
    public void getPoseInActivitySpace_noAnchorOpenXrReferenceSpacePose_returnsIdentityPose() {
        AnchorEntityImpl anchorEntity = createAnchorEntityWithRuntimeAnchor();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1));
        mActivitySpace.setOpenXrReferenceSpaceTransform(Matrix4.fromPose(pose));
        // anchorEntity.setOpenXrReferenceSpacePose(..) is not called to set the underlying pose.

        assertPose(anchorEntity.getPoseInActivitySpace(), new Pose());
    }

    @Test
    public void getPoseInActivitySpace_whenAtSamePose_returnsIdentityPose() {
        AnchorEntityImpl anchorEntity = createAnchorEntityWithRuntimeAnchor();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1));
        mActivitySpace.setOpenXrReferenceSpaceTransform(
                Matrix4.fromTrs(
                        pose.getTranslation(), pose.getRotation(), new Vector3(2f, 2f, 2f)));
        anchorEntity.setOpenXrReferenceSpaceTransform(Matrix4.fromPose(pose));

        assertPose(anchorEntity.getPoseInActivitySpace(), new Pose());
    }

    @Test
    public void getPoseInActivitySpace_returnsDifferencePose() {
        AnchorEntityImpl anchorEntity = createAnchorEntityWithRuntimeAnchor();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1));
        mActivitySpace.setOpenXrReferenceSpaceTransform(Matrix4.Identity);
        anchorEntity.setOpenXrReferenceSpaceTransform(Matrix4.fromPose(pose));

        assertPose(anchorEntity.getPoseInActivitySpace(), pose);
    }

    @Test
    public void getPoseInActivitySpace_withScaledAndRotatedActivitySpace_returnsDifferencePose() {
        AnchorEntityImpl anchorEntity = createAnchorEntityWithRuntimeAnchor();
        Quaternion activitySpaceQuaternion = Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f));
        Pose pose = new Pose(new Vector3(1, 1, 1), Quaternion.Identity);
        anchorEntity.setOpenXrReferenceSpaceTransform(Matrix4.fromPose(pose));
        mActivitySpace.setOpenXrReferenceSpaceTransform(
                Matrix4.fromTrs(
                        new Vector3(2f, 3f, 4f),
                        activitySpaceQuaternion,
                        /* scale= */ new Vector3(2f, 2f, 2f)));
        // A 90 degree rotation around the z axis is a clockwise rotation of the XY plane.
        Pose expectedPose =
                new Pose(
                        new Vector3(-1.0f, 0.5f, -1.5f),
                        Quaternion.fromEulerAngles(new Vector3(0f, 0f, -90f)));

        assertPose(anchorEntity.getPoseInActivitySpace(), expectedPose);
    }

    @Test
    public void getPoseInActivitySpace_withNoActivitySpace_throwsException() {
        ActivityController<Activity> activityController = Robolectric.buildActivity(Activity.class);
        Activity activity = activityController.create().start().get();
        Node node = mXrExtensions.createNode();
        AnchorEntityImpl anchorEntity =
                AnchorEntityImpl.create(
                        activity,
                        node,
                        /* activitySpace= */ null,
                        mXrExtensions,
                        mEntityManager,
                        mExecutor);

        assertThat(anchorEntity.getState()).isEqualTo(State.ERROR);
        assertThrows(IllegalStateException.class, anchorEntity::getPoseInActivitySpace);
    }

    @Test
    public void getActivitySpacePose_whenAtSamePose_returnsIdentityPose() {
        mActivitySpace.setOpenXrReferenceSpaceTransform(Matrix4.Identity);
        AnchorEntityImpl anchorEntity = createAnchorEntityWithRuntimeAnchor();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1));

        anchorEntity.setOpenXrReferenceSpaceTransform(Matrix4.fromPose(pose));

        assertPose(anchorEntity.getActivitySpacePose(), pose);
    }

    @Test
    public void getActivitySpacePose_returnsDifferencePose() {
        mActivitySpace.setOpenXrReferenceSpaceTransform(Matrix4.Identity);
        AnchorEntityImpl anchorEntity = createAnchorEntityWithRuntimeAnchor();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1));

        anchorEntity.setOpenXrReferenceSpaceTransform(Matrix4.fromPose(pose));

        assertPose(anchorEntity.getActivitySpacePose(), pose);
    }

    @Test
    public void transformPoseTo_withActivitySpace_returnsTransformedPose() {
        AnchorEntityImpl anchorEntity = createAnchorEntityWithRuntimeAnchor();
        Pose pose = new Pose(new Vector3(1f, 2f, 3f), Quaternion.Identity);
        mActivitySpace.setOpenXrReferenceSpaceTransform(Matrix4.Identity);
        anchorEntity.setOpenXrReferenceSpaceTransform(Matrix4.fromPose(pose));

        Pose anchorOffset =
                new Pose(
                        new Vector3(10f, 0f, 0f),
                        Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f)));
        Pose transformedPose = anchorEntity.transformPoseTo(anchorOffset, mActivitySpace);

        assertPose(
                transformedPose,
                new Pose(
                        new Vector3(11f, 2f, 3f),
                        Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f))));
    }

    @Test
    public void transformPoseTo_fromActivitySpaceChild_returnsAnchorSpacePose() {
        AnchorEntityImpl anchorEntity = createAnchorEntityWithRuntimeAnchor();
        GltfEntityImpl childEntity1 = createGltfEntity();
        Pose pose = new Pose(new Vector3(1f, 2f, 3f), Quaternion.Identity);
        Pose childPose = new Pose(new Vector3(-1f, -2f, -3f), Quaternion.Identity);

        mActivitySpace.setOpenXrReferenceSpaceTransform(Matrix4.Identity);
        mActivitySpace.addChild(childEntity1);
        childEntity1.setPose(childPose);
        anchorEntity.setOpenXrReferenceSpaceTransform(Matrix4.fromPose(pose));

        assertPose(
                mActivitySpace.transformPoseTo(new Pose(), anchorEntity),
                new Pose(new Vector3(-1f, -2f, -3f), Quaternion.Identity));

        Pose transformedPose = childEntity1.transformPoseTo(new Pose(), anchorEntity);
        assertPose(transformedPose, new Pose(new Vector3(-2f, -4f, -6f), Quaternion.Identity));
    }

    @Test
    public void setAnchor_nonExportableAnchor_remainsUnanchored() {
        AnchorEntityImpl anchorEntity = createAnchorEntity();
        anchorEntity.setOnStateChangedListener(mAnchorStateListener);
        FakeRuntimeAnchor runtimeAnchor = new FakeRuntimeAnchor(Pose.Identity, null, true);
        anchorEntity.setAnchor(new Anchor(runtimeAnchor));
        mExecutor.runAll();

        verify(mAnchorStateListener, never()).onStateChanged(State.ERROR);
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);
    }

    @Test
    public void disposeAnchor_detachesAnchor() {
        AnchorEntityImpl anchorEntity = createAnchorEntityWithRuntimeAnchor();
        anchorEntity.setOnStateChangedListener(mAnchorStateListener);
        verify(mAnchorStateListener, never()).onStateChanged(State.ERROR);
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);

        // Verify the parent of the anchor is the root node (ActivitySpace) before disposing it.
        Node anchorNode = anchorEntity.getNode();
        Node rootNode = mActivitySpace.getNode();
        assertThat(NodeRepository.getInstance().getParent(anchorNode)).isEqualTo(rootNode);
        assertThat(NodeRepository.getInstance().getAnchorId(anchorNode))
                .isEqualTo(mSharedAnchorToken);

        // Dispose the entity and verify that the state was updated.
        anchorEntity.dispose();

        verify(mAnchorStateListener).onStateChanged(State.ERROR);
        assertThat(anchorEntity.getState()).isEqualTo(State.ERROR);
        assertThat(NodeRepository.getInstance().getParent(anchorNode)).isNull();
        assertThat(NodeRepository.getInstance().getAnchorId(anchorNode)).isNull();
    }

    @Test
    public void disposeAnchorTwice_callsCalbackOnce() {
        AnchorEntityImpl anchorEntity = createAnchorEntityWithRuntimeAnchor();
        anchorEntity.setOnStateChangedListener(mAnchorStateListener);
        verify(mAnchorStateListener, never()).onStateChanged(State.ERROR);

        // Dispose the entity and verify that the state was updated.
        anchorEntity.dispose();

        verify(mAnchorStateListener).onStateChanged(State.ERROR);

        // Dispose anchor again and verify onStateChanged was called only once.
        anchorEntity.dispose();

        verify(mAnchorStateListener).onStateChanged(State.ERROR);
    }

    @Test
    public void getScaleRelativeToParentSpace_throwsException() {
        AnchorEntityImpl anchorEntity = createAnchorEntityWithRuntimeAnchor();

        assertThrows(
                UnsupportedOperationException.class, () -> anchorEntity.getScale(Space.PARENT));
    }

    @Test
    public void getScaleRelativeToActivitySpace_returnsActivitySpaceScale() {
        AnchorEntityImpl anchorEntity = createAnchorEntityWithRuntimeAnchor();

        assertVector3(anchorEntity.getScale(Space.ACTIVITY), anchorEntity.getActivitySpaceScale());
    }

    @Test
    public void getScaleRelativeToRealWorldSpace_returnsVector3One() {
        AnchorEntityImpl anchorEntity = createAnchorEntityWithRuntimeAnchor();

        assertVector3(anchorEntity.getScale(Space.REAL_WORLD), new Vector3(1f, 1f, 1f));
    }

    @Test
    public void setAnchor_unanchoredAnchorEntity_updatesState() {
        AnchorEntityImpl anchorEntity = createUnanchoredAnchorEntity();
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);
        assertThat(NodeRepository.getInstance().getName(anchorEntity.getNode()))
                .isEqualTo(AnchorEntityImpl.ANCHOR_NODE_NAME);
        assertThat(NodeRepository.getInstance().getAnchorId(anchorEntity.getNode())).isNull();
        assertThat(NodeRepository.getInstance().getParent(anchorEntity.getNode())).isNull();

        FakeExportableAnchor runtimeAnchor =
                new FakeExportableAnchor(
                        NATIVE_POINTER,
                        mSharedAnchorToken,
                        Pose.Identity,
                        TrackingState.TRACKING,
                        PersistenceState.NOT_PERSISTED,
                        null);
        anchorEntity.setAnchor(new Anchor(runtimeAnchor));

        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);
        assertThat(NodeRepository.getInstance().getAnchorId(anchorEntity.getNode()))
                .isEqualTo(mSharedAnchorToken);
        assertThat(NodeRepository.getInstance().getParent(anchorEntity.getNode()))
                .isEqualTo(mActivitySpace.getNode());
    }

    private static class FakeExportableAnchor implements ExportableAnchor {
        private final long mNativePointer;
        private final IBinder mAnchorToken;
        private final Pose mPose;
        private final TrackingState mTrackingState;
        private final PersistenceState mPersistenceState;
        private final UUID mUuid;

        FakeExportableAnchor(
                long nativePointer,
                IBinder anchorToken,
                Pose pose,
                TrackingState trackingState,
                PersistenceState persistenceState,
                UUID uuid) {
            mNativePointer = nativePointer;
            mAnchorToken = anchorToken;
            mPose = pose;
            mTrackingState = trackingState;
            mPersistenceState = persistenceState;
            mUuid = uuid;
        }

        @Override
        public long getNativePointer() {
            return mNativePointer;
        }

        @Override
        public @NonNull IBinder getAnchorToken() {
            return mAnchorToken;
        }

        @Override
        public @NonNull Pose getPose() {
            return mPose;
        }

        @Override
        public @NonNull TrackingState getTrackingState() {
            return mTrackingState;
        }

        @Override
        public @NonNull PersistenceState getPersistenceState() {
            return mPersistenceState;
        }

        @Override
        public UUID getUuid() {
            return mUuid;
        }

        @Override
        public void detach() {}

        @Override
        public void persist() {}
    }
}
