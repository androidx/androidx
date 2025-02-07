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

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import android.app.Activity;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.test.rule.GrantPermissionRule;
import androidx.xr.extensions.node.Node;
import androidx.xr.runtime.internal.Anchor.PersistenceState;
import androidx.xr.runtime.internal.TrackingState;
import androidx.xr.runtime.math.Matrix4;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.runtime.openxr.ExportableAnchor;
import androidx.xr.scenecore.JxrPlatformAdapter.AnchorEntity.OnStateChangedListener;
import androidx.xr.scenecore.JxrPlatformAdapter.AnchorEntity.PersistState;
import androidx.xr.scenecore.JxrPlatformAdapter.AnchorEntity.PersistStateChangeListener;
import androidx.xr.scenecore.JxrPlatformAdapter.AnchorEntity.State;
import androidx.xr.scenecore.JxrPlatformAdapter.Dimensions;
import androidx.xr.scenecore.JxrPlatformAdapter.PlaneSemantic;
import androidx.xr.scenecore.JxrPlatformAdapter.PlaneType;
import androidx.xr.scenecore.impl.perception.Anchor;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Plane;
import androidx.xr.scenecore.impl.perception.Session;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;
import androidx.xr.scenecore.testing.FakeXrExtensions;
import androidx.xr.scenecore.testing.FakeXrExtensions.FakeGltfModelToken;
import androidx.xr.scenecore.testing.FakeXrExtensions.FakeNode;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import java.time.Duration;
import java.util.UUID;

@RunWith(RobolectricTestRunner.class)
public final class AnchorEntityImplTest extends SystemSpaceEntityImplTest {
    private static class FakeExportableAnchor implements ExportableAnchor {
        private final long mNativePointer;
        private final IBinder mAnchorToken;
        private final Pose mPose;
        private final TrackingState mTrackingState;
        private final PersistenceState mPersistenceState;
        private final UUID mUuid;

        public FakeExportableAnchor(
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
        public IBinder getAnchorToken() {
            return mAnchorToken;
        }

        @Override
        public Pose getPose() {
            return mPose;
        }

        @Override
        public TrackingState getTrackingState() {
            return mTrackingState;
        }

        @Override
        public PersistenceState getPersistenceState() {
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

    private static final Dimensions ANCHOR_DIMENSIONS = new Dimensions(2f, 5f, 0f);
    private static final Plane.Type PLANE_TYPE = Plane.Type.VERTICAL;
    private static final Plane.Label PLANE_LABEL = Plane.Label.WALL;
    private static final long NATIVE_POINTER = 1234567890L;
    private final AndroidXrEntity mActivitySpaceRoot = Mockito.mock(AndroidXrEntity.class);
    private final FakeXrExtensions mFakeExtensions = new FakeXrExtensions();
    private final PerceptionLibrary mPerceptionLibrary = Mockito.mock(PerceptionLibrary.class);
    private final Session mSession = Mockito.mock(Session.class);
    private final Plane mPlane = mock(Plane.class);
    private final Anchor mAnchor = Mockito.mock(Anchor.class);
    private final OnStateChangedListener mAnchorStateListener =
            Mockito.mock(OnStateChangedListener.class);
    private final IBinder mSharedAnchorToken = Mockito.mock(IBinder.class);
    private final FakeScheduledExecutorService mExecutor = new FakeScheduledExecutorService();
    private final EntityManager mEntityManager = new EntityManager();
    private final PersistStateChangeListener mPersistStateChangeListener =
            Mockito.mock(PersistStateChangeListener.class);
    private final androidx.xr.scenecore.impl.perception.Pose mPerceptionIdentityPose =
            androidx.xr.scenecore.impl.perception.Pose.identity();
    private long mCurrentTimeMillis = 1000000000L;
    private ActivitySpaceImpl mActivitySpace;

    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant("android.permission.SCENE_UNDERSTANDING");

    @Before
    public void doBeforeEachTest() {
        ActivityController<Activity> activityController = Robolectric.buildActivity(Activity.class);
        Activity activity = activityController.create().start().get();
        when(mPerceptionLibrary.getActivity()).thenReturn(activity);
        Node taskNode = mFakeExtensions.createNode();
        mActivitySpace =
                new ActivitySpaceImpl(
                        taskNode,
                        mFakeExtensions,
                        mEntityManager,
                        () -> mFakeExtensions.fakeSpatialState,
                        mExecutor);
        SystemClock.setCurrentTimeMillis(mCurrentTimeMillis);

        // By default, set the activity space to the root of the underlying OpenXR reference space.
        mActivitySpace.setOpenXrReferenceSpacePose(Matrix4.Identity);
    }

    /**
     * Returns the anchor entity impl. Used in the base SystemSpaceEntityImplTest to ensure that the
     * anchor entity complies with all the expected behaviors of a system space entity.
     */
    @Override
    protected SystemSpaceEntityImpl getSystemSpaceEntityImpl() {
        return createSemanticAnchorEntity();
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

    // Advances the clock and the executor. The fake executor is not based on the clock because we
    // are
    // using the SystemClock but they can be advanced together.
    void advanceClock(Duration duration) {
        mCurrentTimeMillis += duration.toMillis();
        SystemClock.setCurrentTimeMillis(mCurrentTimeMillis);
        mExecutor.simulateSleepExecutingAllTasks(duration);
    }

    /** Creates an AnchorEntityImpl instance. */
    private AnchorEntityImpl createSemanticAnchorEntity() {
        return createAnchorEntityWithTimeout(null);
    }

    /** Creates an AnchorEntityImpl instance with a timeout. */
    private AnchorEntityImpl createAnchorEntityWithTimeout(Duration anchorSearchTimeout) {
        Node node = mFakeExtensions.createNode();
        return AnchorEntityImpl.createSemanticAnchor(
                node,
                ANCHOR_DIMENSIONS,
                PlaneType.VERTICAL,
                PlaneSemantic.WALL,
                anchorSearchTimeout,
                mActivitySpace,
                mActivitySpaceRoot,
                mFakeExtensions,
                mEntityManager,
                mExecutor,
                mPerceptionLibrary);
    }

    /**
     * Creates an AnchorEntityImpl instance that will search for a persisted anchor within
     * `anchorSearchTimeout`.
     */
    private AnchorEntityImpl createPersistedAnchorEntityWithTimeout(
            UUID uuid, Duration anchorSearchTimeout) {
        Node node = mFakeExtensions.createNode();
        return AnchorEntityImpl.createPersistedAnchor(
                node,
                uuid,
                anchorSearchTimeout,
                mActivitySpace,
                mActivitySpaceRoot,
                mFakeExtensions,
                mEntityManager,
                mExecutor,
                mPerceptionLibrary);
    }

    /** Creates an AnchorEntityImpl instance and initializes it with a persisted anchor. */
    private AnchorEntityImpl createInitializedPersistedAnchorEntity(Anchor anchor, UUID uuid) {
        when(mPerceptionLibrary.getSession()).thenReturn(mSession);
        when(mSession.createAnchorFromUuid(uuid)).thenReturn(anchor);
        when(anchor.getAnchorToken()).thenReturn(mSharedAnchorToken);

        Node node = mFakeExtensions.createNode();
        return AnchorEntityImpl.createPersistedAnchor(
                node,
                uuid,
                /* anchorSearchTimeout= */ null,
                mActivitySpace,
                mActivitySpaceRoot,
                mFakeExtensions,
                mEntityManager,
                mExecutor,
                mPerceptionLibrary);
    }

    /**
     * Creates an AnchorEntityImpl and initializes it with an anchor from the perception library.
     */
    private AnchorEntityImpl createAndInitAnchorEntity() {
        when(mPerceptionLibrary.getSession()).thenReturn(mSession);
        when(mSession.getAllPlanes()).thenReturn(ImmutableList.of(mPlane));
        when(mPlane.getData(any()))
                .thenReturn(
                        new Plane.PlaneData(
                                mPerceptionIdentityPose,
                                ANCHOR_DIMENSIONS.width,
                                ANCHOR_DIMENSIONS.height,
                                PLANE_TYPE.intValue,
                                PLANE_LABEL.intValue));
        when(mPlane.createAnchor(eq(mPerceptionIdentityPose), any())).thenReturn(mAnchor);
        when(mAnchor.getAnchorToken()).thenReturn(mSharedAnchorToken);

        return createSemanticAnchorEntity();
    }

    private AnchorEntityImpl createInitAndPersistAnchorEntity() {
        when(mAnchor.persist()).thenReturn(UUID.randomUUID());
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
        anchorEntity.registerPersistStateChangeListener(mPersistStateChangeListener);
        UUID unused = anchorEntity.persist();
        return anchorEntity;
    }

    private AnchorEntityImpl createAnchorEntityFromPlane() {
        when(mAnchor.persist()).thenReturn(UUID.randomUUID());

        Node node = mFakeExtensions.createNode();
        return AnchorEntityImpl.createAnchorFromPlane(
                node,
                mPlane,
                new Pose(),
                MILLISECONDS.toNanos(mCurrentTimeMillis),
                mActivitySpace,
                mActivitySpaceRoot,
                mFakeExtensions,
                mEntityManager,
                mExecutor,
                mPerceptionLibrary);
    }

    private AnchorEntityImpl createAnchorEntityFromPerceptionAnchor(
            androidx.xr.arcore.Anchor perceptionAnchor) {
        Node node = mFakeExtensions.createNode();

        return AnchorEntityImpl.createAnchorFromPerceptionAnchor(
                node,
                perceptionAnchor,
                mActivitySpace,
                mActivitySpaceRoot,
                mFakeExtensions,
                mEntityManager,
                mExecutor,
                mPerceptionLibrary);
    }

    /** Creates a generic glTF entity. */
    private GltfEntityImpl createGltfEntity() {
        FakeGltfModelToken modelToken = new FakeGltfModelToken("model");
        GltfModelResourceImpl model = new GltfModelResourceImpl(modelToken);
        return new GltfEntityImpl(
                model, mActivitySpace, mFakeExtensions, mEntityManager, mExecutor);
    }

    @Test
    public void createAnchorEntityWithPersistedAnchor_returnsAnchored() throws Exception {
        AnchorEntityImpl anchorEntity =
                createInitializedPersistedAnchorEntity(mAnchor, UUID.randomUUID());
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);
        assertThat(((FakeNode) anchorEntity.getNode()).getName())
                .isEqualTo(AnchorEntityImpl.ANCHOR_NODE_NAME);
        assertThat(((FakeNode) anchorEntity.getNode()).getAnchorId()).isEqualTo(mSharedAnchorToken);
        assertThat(anchorEntity.getPersistState()).isEqualTo(PersistState.PERSISTED);
    }

    @Test
    public void createAnchorEntityWithPersistedAnchor_persistAnchor_returnsUuid() throws Exception {
        UUID uuid = UUID.randomUUID();
        AnchorEntityImpl anchorEntity = createInitializedPersistedAnchorEntity(mAnchor, uuid);
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);
        assertThat(((FakeNode) anchorEntity.getNode()).getName())
                .isEqualTo(AnchorEntityImpl.ANCHOR_NODE_NAME);
        assertThat(((FakeNode) anchorEntity.getNode()).getAnchorId()).isEqualTo(mSharedAnchorToken);
        assertThat(anchorEntity.getPersistState()).isEqualTo(PersistState.PERSISTED);

        UUID returnedUuid = anchorEntity.persist();
        assertThat(returnedUuid).isEqualTo(uuid);
    }

    @Test
    public void createPersistedAnchorEntity_sessionNotReady_keepUnanchored() throws Exception {
        when(mPerceptionLibrary.getSession()).thenReturn(null);
        AnchorEntityImpl anchorEntity =
                createPersistedAnchorEntityWithTimeout(
                        UUID.randomUUID(), /* anchorSearchTimeout= */ null);

        // if the session isn't ready, we should retry later and search for the anchor.
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);
    }

    @Test
    public void createPersistedAnchorEntity_persistedAnchorNotFound_keepUnanchored()
            throws Exception {
        UUID uuid = UUID.randomUUID();
        when(mPerceptionLibrary.getSession()).thenReturn(mSession);
        when(mSession.createAnchorFromUuid(uuid)).thenReturn(null);
        AnchorEntityImpl anchorEntity =
                createPersistedAnchorEntityWithTimeout(uuid, /* anchorSearchTimeout= */ null);

        // If the session is ready and we can't find it, the perception stack might be warming up,
        // so
        // we'll retry later.
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);
    }

    @Test
    public void createPersistedAnchorEntity_persistedAnchorHasNoToken_keepUnanchored()
            throws Exception {
        UUID uuid = UUID.randomUUID();
        when(mPerceptionLibrary.getSession()).thenReturn(mSession);
        when(mSession.createAnchorFromUuid(uuid)).thenReturn(mAnchor);
        when(mAnchor.getAnchorToken()).thenReturn(null);
        AnchorEntityImpl anchorEntity =
                createPersistedAnchorEntityWithTimeout(uuid, /* anchorSearchTimeout= */ null);

        // If the anchor is ready but its token isn't available, we'll retry later.
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);
    }

    @Test
    public void createPersistedAnchorEntity_delayedSession_callsCallback() throws Exception {
        // This will return an error on the first attempt so will need to be called twice.
        when(mPerceptionLibrary.getSession()).thenReturn(null).thenReturn(mSession);
        UUID uuid = UUID.randomUUID();
        when(mSession.createAnchorFromUuid(uuid)).thenReturn(mAnchor);
        when(mAnchor.getAnchorToken()).thenReturn(mSharedAnchorToken);

        AnchorEntityImpl anchorEntity =
                createPersistedAnchorEntityWithTimeout(uuid, /* anchorSearchTimeout= */ null);
        anchorEntity.setOnStateChangedListener(mAnchorStateListener);

        verify(mAnchorStateListener, never()).onStateChanged(State.ANCHORED);
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);

        // The anchor starts as unanchored. Advance the executor to try again successfully and get a
        // callback for the anchor to be anchored.
        advanceClock(AnchorEntityImpl.ANCHOR_SEARCH_DELAY);
        verify(mAnchorStateListener).onStateChanged(State.ANCHORED);
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);
        assertThat(anchorEntity.getPersistState()).isEqualTo(PersistState.PERSISTED);
    }

    @Test
    public void createPersistedAnchorEntity_delayedAnchor_callsCallback() throws Exception {
        // This will return an error on the first attempt so will need to be called twice.
        when(mPerceptionLibrary.getSession()).thenReturn(mSession);
        UUID uuid = UUID.randomUUID();
        when(mSession.createAnchorFromUuid(uuid)).thenReturn(mAnchor).thenReturn(mAnchor);
        when(mAnchor.getAnchorToken()).thenReturn(null).thenReturn(mSharedAnchorToken);

        AnchorEntityImpl anchorEntity =
                createPersistedAnchorEntityWithTimeout(uuid, /* anchorSearchTimeout= */ null);
        anchorEntity.setOnStateChangedListener(mAnchorStateListener);

        verify(mAnchorStateListener, never()).onStateChanged(State.ANCHORED);
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);

        // The anchor starts as unanchored. Advance the executor to try again successfully and get a
        // callback for the anchor to be anchored.
        advanceClock(AnchorEntityImpl.ANCHOR_SEARCH_DELAY);
        verify(mAnchorStateListener).onStateChanged(State.ANCHORED);
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);
        assertThat(anchorEntity.getPersistState()).isEqualTo(PersistState.PERSISTED);
    }

    @Test
    public void createPersistedAnchorEntity_delayedSession_timeout_noCallback() throws Exception {
        // This will return an error on the first attempt so will need to be called twice.
        when(mPerceptionLibrary.getSession()).thenReturn(null).thenReturn(mSession);
        UUID uuid = UUID.randomUUID();
        when(mSession.createAnchorFromUuid(uuid)).thenReturn(mAnchor);
        when(mAnchor.getAnchorToken()).thenReturn(mSharedAnchorToken);

        AnchorEntityImpl anchorEntity =
                createPersistedAnchorEntityWithTimeout(
                        uuid, AnchorEntityImpl.ANCHOR_SEARCH_DELAY.dividedBy(2));
        anchorEntity.setOnStateChangedListener(mAnchorStateListener);

        advanceClock(AnchorEntityImpl.ANCHOR_SEARCH_DELAY);

        verify(mAnchorStateListener, never()).onStateChanged(State.ANCHORED);
        verify(mAnchorStateListener).onStateChanged(State.TIMED_OUT);
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.TIMED_OUT);
    }

    @Test
    public void createAnchorEntity_defaultUnanchored() throws Exception {
        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);
        assertThat(((FakeNode) anchorEntity.getNode()).getName())
                .isEqualTo(AnchorEntityImpl.ANCHOR_NODE_NAME);
    }

    @Test
    public void createAndInitAnchor_returnsAnchored() throws Exception {
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);
        assertThat(((FakeNode) anchorEntity.getNode()).getName())
                .isEqualTo(AnchorEntityImpl.ANCHOR_NODE_NAME);
        assertThat(((FakeNode) anchorEntity.getNode()).getAnchorId()).isEqualTo(mSharedAnchorToken);
    }

    @Test
    public void createAndInitAnchor_noPlanes_remainsUnanchored() throws Exception {
        when(mPerceptionLibrary.getSession()).thenReturn(mSession);
        when(mSession.getAllPlanes()).thenReturn(ImmutableList.of());

        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();

        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);
        assertThat(((FakeNode) anchorEntity.getNode()).getName())
                .isEqualTo(AnchorEntityImpl.ANCHOR_NODE_NAME);
    }

    @Test
    public void createAndInitAnchor_noViablePlane_remainsUnanchored() throws Exception {
        when(mPerceptionLibrary.getSession()).thenReturn(mSession);
        when(mSession.getAllPlanes()).thenReturn(ImmutableList.of(mPlane));
        when(mPlane.getData(any()))
                .thenReturn(
                        new Plane.PlaneData(
                                mPerceptionIdentityPose,
                                ANCHOR_DIMENSIONS.width - 1,
                                ANCHOR_DIMENSIONS.height,
                                PLANE_TYPE.intValue,
                                PLANE_LABEL.intValue));

        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();

        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);
        assertThat(((FakeNode) anchorEntity.getNode()).getName())
                .isEqualTo(AnchorEntityImpl.ANCHOR_NODE_NAME);
    }

    @Test
    public void createAndInitAnchor_delayedSession_callsCallback() throws Exception {
        // This will return an error on the first attempt so will need to be called twice.
        when(mPerceptionLibrary.getSession()).thenReturn(null).thenReturn(mSession);
        when(mSession.getAllPlanes()).thenReturn(ImmutableList.of(mPlane));
        when(mPlane.getData(any()))
                .thenReturn(
                        new Plane.PlaneData(
                                mPerceptionIdentityPose,
                                ANCHOR_DIMENSIONS.width,
                                ANCHOR_DIMENSIONS.height,
                                PLANE_TYPE.intValue,
                                PLANE_LABEL.intValue));
        when(mPlane.createAnchor(eq(mPerceptionIdentityPose), any())).thenReturn(mAnchor);
        when(mAnchor.getAnchorToken()).thenReturn(mSharedAnchorToken);

        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();
        anchorEntity.setOnStateChangedListener(mAnchorStateListener);

        verify(mAnchorStateListener, never()).onStateChanged(State.ANCHORED);
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);

        // The anchor starts as unanchored. Advance the executor to try again successfully and get a
        // callback for the anchor to be anchored.
        advanceClock(AnchorEntityImpl.ANCHOR_SEARCH_DELAY);
        verify(mAnchorStateListener).onStateChanged(State.ANCHORED);
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);
    }

    @Test
    public void createAndInitAnchor_delayedAnchor_callsCallback() throws Exception {
        when(mPerceptionLibrary.getSession()).thenReturn(mSession);
        // This will return no planes on the first attempt so will need to be called twice.
        when(mSession.getAllPlanes())
                .thenReturn(ImmutableList.of())
                .thenReturn(ImmutableList.of(mPlane));
        when(mPlane.getData(any()))
                .thenReturn(
                        new Plane.PlaneData(
                                mPerceptionIdentityPose,
                                ANCHOR_DIMENSIONS.width,
                                ANCHOR_DIMENSIONS.height,
                                PLANE_TYPE.intValue,
                                PLANE_LABEL.intValue));
        when(mPlane.createAnchor(eq(mPerceptionIdentityPose), any())).thenReturn(mAnchor);
        when(mAnchor.getAnchorToken()).thenReturn(mSharedAnchorToken);

        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();
        anchorEntity.setOnStateChangedListener(mAnchorStateListener);

        verify(mAnchorStateListener, never()).onStateChanged(State.ANCHORED);
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);

        // The anchor starts as unanchored. Advance the executor to try again successfully and get a
        // callback for the anchor to be anchored.
        advanceClock(AnchorEntityImpl.ANCHOR_SEARCH_DELAY);
        verify(mAnchorStateListener).onStateChanged(State.ANCHORED);
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);
    }

    @Test
    public void createAndInitAnchor_delayedSessionAndAnchor_callsCallback() throws Exception {
        // This will return an error on the first attempt then it will find no planes on the
        // second attempt. So it will need to be called three times.
        when(mPerceptionLibrary.getSession())
                .thenReturn(null)
                .thenReturn(mSession)
                .thenReturn(mSession);
        when(mSession.getAllPlanes())
                .thenReturn(ImmutableList.of())
                .thenReturn(ImmutableList.of(mPlane));
        when(mPlane.getData(any()))
                .thenReturn(
                        new Plane.PlaneData(
                                mPerceptionIdentityPose,
                                ANCHOR_DIMENSIONS.width,
                                ANCHOR_DIMENSIONS.height,
                                PLANE_TYPE.intValue,
                                PLANE_LABEL.intValue));
        when(mPlane.createAnchor(eq(mPerceptionIdentityPose), any())).thenReturn(mAnchor);
        when(mAnchor.getAnchorToken()).thenReturn(mSharedAnchorToken);

        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();
        anchorEntity.setOnStateChangedListener(mAnchorStateListener);

        verify(mAnchorStateListener, never()).onStateChanged(State.ANCHORED);
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);

        // Advance the executor to try again with a working session but an error on the anchor.
        advanceClock(AnchorEntityImpl.ANCHOR_SEARCH_DELAY);
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);

        // Advance the executor attempt the anchor again successfully and get a callback for the
        // anchor
        // to be anchored.
        advanceClock(AnchorEntityImpl.ANCHOR_SEARCH_DELAY);
        verify(mAnchorStateListener).onStateChanged(State.ANCHORED);
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);
    }

    @Test
    public void createAndInitAnchor_noToken_returnsUnanchored() throws Exception {
        when(mPerceptionLibrary.getSession()).thenReturn(mSession);
        when(mSession.getAllPlanes()).thenReturn(ImmutableList.of(mPlane));
        when(mPlane.getData(any()))
                .thenReturn(
                        new Plane.PlaneData(
                                mPerceptionIdentityPose,
                                ANCHOR_DIMENSIONS.width,
                                ANCHOR_DIMENSIONS.height,
                                PLANE_TYPE.intValue,
                                PLANE_LABEL.intValue));
        when(mPlane.createAnchor(eq(mPerceptionIdentityPose), any())).thenReturn(mAnchor);
        when(mAnchor.getAnchorToken()).thenReturn(null);

        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();

        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);
    }

    @Test
    public void createAndInitAnchor_withinTimeout_success() throws Exception {
        // The anchor creation will return an error so that it is called again on the executor. The
        // timeout will happen after the second attempt so it will still succeed.
        when(mPerceptionLibrary.getSession()).thenReturn(mSession);
        when(mSession.getAllPlanes())
                .thenReturn(ImmutableList.of())
                .thenReturn(ImmutableList.of(mPlane));
        when(mPlane.getData(any()))
                .thenReturn(
                        new Plane.PlaneData(
                                mPerceptionIdentityPose,
                                ANCHOR_DIMENSIONS.width,
                                ANCHOR_DIMENSIONS.height,
                                PLANE_TYPE.intValue,
                                PLANE_LABEL.intValue));
        when(mPlane.createAnchor(eq(mPerceptionIdentityPose), any())).thenReturn(mAnchor);
        when(mAnchor.getAnchorToken()).thenReturn(mSharedAnchorToken);

        // Create an anchor entity with a timeout of 1ms above the normal search delay.
        Duration timeout = AnchorEntityImpl.ANCHOR_SEARCH_DELAY.plusMillis(1);
        AnchorEntityImpl anchorEntity = createAnchorEntityWithTimeout(timeout);
        anchorEntity.setOnStateChangedListener(mAnchorStateListener);

        verify(mAnchorStateListener, never()).onStateChanged(State.ANCHORED);
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);

        // Advance the executor to try again it should now be anchored.
        advanceClock(AnchorEntityImpl.ANCHOR_SEARCH_DELAY);
        verify(mAnchorStateListener).onStateChanged(State.ANCHORED);
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);

        // Advance the clock past the timeout and verify that nothing changed.
        advanceClock(timeout.minus(AnchorEntityImpl.ANCHOR_SEARCH_DELAY));
        verify(mAnchorStateListener, never()).onStateChanged(State.TIMED_OUT);
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);
    }

    @Test
    public void createAndInitAnchor_passedTimeout_error() throws Exception {
        // The anchor creation will return an error so that it is called again on the executor. The
        // timeout will happen before the next call.
        when(mPerceptionLibrary.getSession()).thenReturn(mSession);
        when(mSession.getAllPlanes()).thenReturn(ImmutableList.of());
        when(mAnchor.getAnchorToken()).thenReturn(mSharedAnchorToken);

        // Create an anchor entity with a timeout of 1ms below the normal search delay.
        Duration timeout = AnchorEntityImpl.ANCHOR_SEARCH_DELAY.minusMillis(1);
        AnchorEntityImpl anchorEntity = createAnchorEntityWithTimeout(timeout);
        anchorEntity.setOnStateChangedListener(mAnchorStateListener);

        verify(mAnchorStateListener, never()).onStateChanged(State.ERROR);
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);

        // Advance the executor to try again it should now be timed out.
        advanceClock(AnchorEntityImpl.ANCHOR_SEARCH_DELAY);
        assertThat(anchorEntity.getState()).isEqualTo(State.TIMED_OUT);

        // Advance the clock again and verify that nothing changed.
        advanceClock(AnchorEntityImpl.ANCHOR_SEARCH_DELAY);
        assertThat(anchorEntity.getState()).isEqualTo(State.TIMED_OUT);
    }

    @Test
    public void createAndInitAnchor_zeroTimeout_keepsSearching() throws Exception {
        // The anchor creation will return an error so that it is called again on the executor. The
        // timeout will happen after the second attempt so it will still succeed.
        int anchorAttempts = 100;
        when(mPerceptionLibrary.getSession()).thenReturn(mSession);
        when(mSession.getAllPlanes()).thenReturn(ImmutableList.of());
        when(mAnchor.getAnchorToken()).thenReturn(mSharedAnchorToken);

        // Create an anchor entity with a zero duration it should be search for indefinitely..
        AnchorEntityImpl anchorEntity = createAnchorEntityWithTimeout(Duration.ZERO);
        anchorEntity.setOnStateChangedListener(mAnchorStateListener);

        for (int i = 0; i < anchorAttempts - 1; i++) {
            advanceClock(AnchorEntityImpl.ANCHOR_SEARCH_DELAY);
            verify(mAnchorStateListener, never()).onStateChanged(any());
            assertThat(anchorEntity).isNotNull();
            assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);
        }
    }

    @Test
    public void anchorEntityAddChildren_addsChildren() throws Exception {
        GltfEntityImpl childEntity1 = createGltfEntity();
        GltfEntityImpl childEntity2 = createGltfEntity();
        AnchorEntityImpl parentEntity = createSemanticAnchorEntity();

        parentEntity.addChild(childEntity1);

        assertThat(parentEntity.getChildren()).containsExactly(childEntity1);

        parentEntity.addChildren(ImmutableList.of(childEntity2));

        assertThat(childEntity1.getParent()).isEqualTo(parentEntity);
        assertThat(childEntity2.getParent()).isEqualTo(parentEntity);
        assertThat(parentEntity.getChildren()).containsExactly(childEntity1, childEntity2);

        FakeNode parentNode = (FakeNode) parentEntity.getNode();
        FakeNode childNode1 = (FakeNode) childEntity1.getNode();
        FakeNode childNode2 = (FakeNode) childEntity2.getNode();

        assertThat(childNode1.getParent()).isEqualTo(parentNode);
        assertThat(childNode2.getParent()).isEqualTo(parentNode);
    }

    @Test
    public void anchorEntitySetPose_throwsException() throws Exception {
        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();
        Pose pose = new Pose();
        assertThrows(UnsupportedOperationException.class, () -> anchorEntity.setPose(pose));
    }

    @Test
    public void anchorEntitySetScale_throwsException() throws Exception {
        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();
        Vector3 scale = new Vector3(1, 1, 1);
        assertThrows(UnsupportedOperationException.class, () -> anchorEntity.setScale(scale));
    }

    @Test
    public void anchorEntityGetScale_returnsIdentityScale() throws Exception {
        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();
        assertVector3(anchorEntity.getScale(), new Vector3(1f, 1f, 1f));
    }

    @Test
    public void anchorEntityGetWorldSpaceScale_returnsIdentityScale() throws Exception {
        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();
        assertVector3(anchorEntity.getWorldSpaceScale(), new Vector3(1f, 1f, 1f));
    }

    @Test
    public void anchorEntityGetActivitySpaceScale_returnsInverseOfActivitySpace() throws Exception {
        float activitySpaceScale = 5f;
        mActivitySpace.setOpenXrReferenceSpacePose(Matrix4.fromScale(activitySpaceScale));
        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();
        assertVector3(
                anchorEntity.getActivitySpaceScale(),
                new Vector3(1f, 1f, 1f).div(activitySpaceScale));
    }

    @Test
    public void getPoseInActivitySpace_unanchored_returnsIdentityPose() {
        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1));
        mActivitySpace.setOpenXrReferenceSpacePose(Matrix4.Identity);
        anchorEntity.setOpenXrReferenceSpacePose(Matrix4.fromPose(pose));

        assertPose(anchorEntity.getPoseInActivitySpace(), new Pose());
    }

    @Test
    public void
            getPoseInActivitySpace_noActivitySpaceOpenXrReferenceSpacePose_returnsIdentityPose() {
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1));
        mActivitySpace.mOpenXrReferenceSpacePose = null;
        anchorEntity.setOpenXrReferenceSpacePose(Matrix4.fromPose(pose));
        assertPose(anchorEntity.getPoseInActivitySpace(), new Pose());
    }

    @Test
    public void getPoseInActivitySpace_noAnchorOpenXrReferenceSpacePose_returnsIdentityPose() {
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1));
        mActivitySpace.setOpenXrReferenceSpacePose(Matrix4.fromPose(pose));
        // anchorEntity.setOpenXrReferenceSpacePose(..) is not called to set the underlying pose.

        assertPose(anchorEntity.getPoseInActivitySpace(), new Pose());
    }

    @Test
    public void getPoseInActivitySpace_whenAtSamePose_returnsIdentityPose() {
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1).toNormalized());
        mActivitySpace.setOpenXrReferenceSpacePose(
                Matrix4.fromTrs(
                        pose.getTranslation(), pose.getRotation(), new Vector3(2f, 2f, 2f)));
        anchorEntity.setOpenXrReferenceSpacePose(Matrix4.fromPose(pose));

        assertPose(anchorEntity.getPoseInActivitySpace(), new Pose());
    }

    @Test
    public void getPoseInActivitySpace_returnsDifferencePose() {
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1).toNormalized());
        mActivitySpace.setOpenXrReferenceSpacePose(Matrix4.Identity);
        anchorEntity.setOpenXrReferenceSpacePose(Matrix4.fromPose(pose));

        assertPose(anchorEntity.getPoseInActivitySpace(), pose);
    }

    @Test
    public void getPoseInActivitySpace_withScaledAndRotatedActivitySpace_returnsDifferencePose() {
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
        Quaternion activitySpaceQuaternion = Quaternion.fromEulerAngles(new Vector3(0f, 0f, 90f));
        Pose pose = new Pose(new Vector3(1, 1, 1), Quaternion.Identity);
        anchorEntity.setOpenXrReferenceSpacePose(Matrix4.fromPose(pose));
        mActivitySpace.setOpenXrReferenceSpacePose(
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
    public void getPoseInActivitySpace_withNoActivitySpace_throwsException() throws Exception {
        Node node = mFakeExtensions.createNode();
        AnchorEntityImpl anchorEntity =
                AnchorEntityImpl.createSemanticAnchor(
                        node,
                        /* dimensions= */ null,
                        /* planeType= */ null,
                        /* planeSemantic= */ null,
                        /* anchorSearchTimeout= */ null,
                        /* activitySpace= */ null,
                        mActivitySpaceRoot,
                        mFakeExtensions,
                        mEntityManager,
                        mExecutor,
                        mPerceptionLibrary);

        assertThat(anchorEntity.getState()).isEqualTo(State.ERROR);
        assertThrows(IllegalStateException.class, anchorEntity::getPoseInActivitySpace);
    }

    @Test
    public void getActivitySpacePose_whenAtSamePose_returnsIdentityPose() {
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1).toNormalized());
        when(mActivitySpaceRoot.getPoseInActivitySpace()).thenReturn(pose);

        anchorEntity.setOpenXrReferenceSpacePose(Matrix4.fromPose(pose));

        assertPose(anchorEntity.getActivitySpacePose(), new Pose());
    }

    @Test
    public void getActivitySpacePose_returnsDifferencePose() {
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
        Pose pose = new Pose(new Vector3(1, 1, 1), new Quaternion(0, 1, 0, 1).toNormalized());
        when(mActivitySpaceRoot.getPoseInActivitySpace()).thenReturn(new Pose());

        anchorEntity.setOpenXrReferenceSpacePose(Matrix4.fromPose(pose));

        assertPose(anchorEntity.getActivitySpacePose(), pose);
    }

    @Test
    public void getActivitySpacePose_withNonAndroidXrActivitySpaceRoot_throwsException()
            throws Exception {
        Node node = mFakeExtensions.createNode();
        AnchorEntityImpl anchorEntity =
                AnchorEntityImpl.createSemanticAnchor(
                        node,
                        /* dimensions= */ null,
                        /* planeType= */ null,
                        /* planeSemantic= */ null,
                        /* anchorSearchTimeout= */ null,
                        mActivitySpace,
                        /* activitySpaceRoot= */ null,
                        mFakeExtensions,
                        mEntityManager,
                        mExecutor,
                        mPerceptionLibrary);

        assertThat(anchorEntity.getState()).isEqualTo(State.ERROR);
        assertThrows(IllegalStateException.class, anchorEntity::getActivitySpacePose);
    }

    @Test
    public void transformPoseTo_withActivitySpace_returnsTransformedPose() {
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
        Pose pose = new Pose(new Vector3(1f, 2f, 3f), Quaternion.Identity);
        mActivitySpace.setOpenXrReferenceSpacePose(Matrix4.Identity);
        anchorEntity.setOpenXrReferenceSpacePose(Matrix4.fromPose(pose));

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
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
        GltfEntityImpl childEntity1 = createGltfEntity();
        Pose pose = new Pose(new Vector3(1f, 2f, 3f), Quaternion.Identity);
        Pose childPose = new Pose(new Vector3(-1f, -2f, -3f), Quaternion.Identity);

        mActivitySpace.setOpenXrReferenceSpacePose(Matrix4.Identity);
        mActivitySpace.addChild(childEntity1);
        childEntity1.setPose(childPose);
        anchorEntity.setOpenXrReferenceSpacePose(Matrix4.fromPose(pose));

        assertPose(
                mActivitySpace.transformPoseTo(new Pose(), anchorEntity),
                new Pose(new Vector3(-1f, -2f, -3f), Quaternion.Identity));

        Pose transformedPose = childEntity1.transformPoseTo(new Pose(), anchorEntity);
        assertPose(transformedPose, new Pose(new Vector3(-2f, -4f, -6f), Quaternion.Identity));
    }

    @Test
    public void anchorEntity_setsParentAfterAnchoring() throws Exception {
        // This will return an error on the first attempt so will need to be called twice.
        when(mPerceptionLibrary.getSession()).thenReturn(null).thenReturn(mSession);

        when(mSession.getAllPlanes()).thenReturn(ImmutableList.of(mPlane));
        when(mPlane.getData(any()))
                .thenReturn(
                        new Plane.PlaneData(
                                mPerceptionIdentityPose,
                                ANCHOR_DIMENSIONS.width,
                                ANCHOR_DIMENSIONS.height,
                                PLANE_TYPE.intValue,
                                PLANE_LABEL.intValue));
        when(mPlane.createAnchor(eq(mPerceptionIdentityPose), any())).thenReturn(mAnchor);
        when(mAnchor.getAnchorToken()).thenReturn(mSharedAnchorToken);

        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);

        FakeNode anchorNode = (FakeNode) anchorEntity.getNode();
        FakeNode rootNode = (FakeNode) mActivitySpace.getNode();
        assertThat(anchorNode.getParent()).isNull();

        // The anchor starts as unanchored. Advance the executor to wait for it to become anchored
        // and
        // verify that the parent is the root node.
        advanceClock(AnchorEntityImpl.ANCHOR_SEARCH_DELAY);
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);
        assertThat(anchorNode.getParent()).isEqualTo(rootNode);
    }

    @Test
    public void disposeAnchor_detachesAnchor() throws Exception {
        when(mAnchor.detach()).thenReturn(true);
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
        anchorEntity.setOnStateChangedListener(mAnchorStateListener);
        verify(mAnchorStateListener, never()).onStateChanged(State.ERROR);
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);

        // Verify the parent of the anchor is the root node (ActivitySpace) before disposing it.
        FakeNode anchorNode = (FakeNode) anchorEntity.getNode();
        FakeNode rootNode = (FakeNode) mActivitySpace.getNode();
        assertThat(anchorNode.getParent()).isEqualTo(rootNode);
        assertThat(anchorNode.getAnchorId()).isEqualTo(mSharedAnchorToken);

        // Dispose the entity and verify that the state was updated.
        anchorEntity.dispose();

        verify(mAnchorStateListener).onStateChanged(State.ERROR);
        assertThat(anchorEntity.getState()).isEqualTo(State.ERROR);
        assertThat(anchorNode.getParent()).isNull();
        assertThat(anchorNode.getAnchorId()).isNull();
    }

    @Test
    public void disposeAnchorUnanchered_stopsSearching() throws Exception {
        when(mPerceptionLibrary.getSession()).thenReturn(null);

        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();
        anchorEntity.setOnStateChangedListener(mAnchorStateListener);

        verify(mPerceptionLibrary).getSession();
        verify(mAnchorStateListener, never()).onStateChanged(State.ERROR);
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);

        // Dispose the anchor entity before it was anchored.
        anchorEntity.dispose();

        // verify(anchorStateListener).onStateChanged(State.ERROR);
        assertThat(anchorEntity.getState()).isEqualTo(State.ERROR);

        // Advance the executor attempt to show that it will not call into the perception library
        // again
        // once disposed.
        advanceClock(AnchorEntityImpl.ANCHOR_SEARCH_DELAY);
        verify(mPerceptionLibrary).getSession();
    }

    @Test
    public void disposeAnchorTwice_callsCalbackOnce() throws Exception {
        when(mAnchor.detach()).thenReturn(true);
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
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
    public void createAnchorEntity_defaultPersistState_returnsPersistNotRequested()
            throws Exception {
        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getPersistState()).isEqualTo(PersistState.PERSIST_NOT_REQUESTED);
    }

    @Test
    public void persistAnchor_notAnchored_returnsNull() throws Exception {
        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.persist()).isNull();
    }

    @Test
    public void persistAnchor_returnsUuid() throws Exception {
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
        when(mAnchor.persist()).thenReturn(UUID.randomUUID());
        anchorEntity.registerPersistStateChangeListener(mPersistStateChangeListener);
        assertThat(anchorEntity.persist()).isNotNull();
        assertThat(anchorEntity.getPersistState()).isEqualTo(PersistState.PERSIST_PENDING);
        verify(mPersistStateChangeListener).onPersistStateChanged(PersistState.PERSIST_PENDING);
    }

    @Test
    public void persistAnchor_returnsNull() throws Exception {
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
        when(mAnchor.persist()).thenReturn(null);
        anchorEntity.registerPersistStateChangeListener(mPersistStateChangeListener);
        assertThat(anchorEntity.persist()).isNull();
        assertThat(anchorEntity.getPersistState()).isEqualTo(PersistState.PERSIST_NOT_REQUESTED);
        verify(mPersistStateChangeListener, never()).onPersistStateChanged(any());
    }

    @Test
    public void persistAnchor_secondPersist_returnsSameUuid_updatesPersistStateOnce()
            throws Exception {
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
        when(mAnchor.persist()).thenReturn(UUID.randomUUID());
        anchorEntity.registerPersistStateChangeListener(mPersistStateChangeListener);
        UUID firstUuid = anchorEntity.persist();
        assertThat(firstUuid).isNotNull();
        assertThat(anchorEntity.getPersistState()).isEqualTo(PersistState.PERSIST_PENDING);

        UUID secondUuid = anchorEntity.persist();
        assertThat(firstUuid).isEquivalentAccordingToCompareTo(secondUuid);
        assertThat(anchorEntity.getPersistState()).isEqualTo(PersistState.PERSIST_PENDING);
        verify(mPersistStateChangeListener).onPersistStateChanged(PersistState.PERSIST_PENDING);
    }

    @Test
    public void persistAnchor_updatesPersistStateToPersisted() throws Exception {
        AnchorEntityImpl anchorEntity = createInitAndPersistAnchorEntity();
        when(mAnchor.getPersistState()).thenReturn(Anchor.PersistState.PERSISTED);

        mExecutor.simulateSleepExecutingAllTasks(AnchorEntityImpl.PERSIST_STATE_CHECK_DELAY);
        assertThat(anchorEntity.getPersistState()).isEqualTo(PersistState.PERSISTED);
        verify(mPersistStateChangeListener).onPersistStateChanged(PersistState.PERSISTED);
    }

    @Test
    public void updatePersistState_delayedPersistedState_callsCallback() throws Exception {
        AnchorEntityImpl anchorEntity = createInitAndPersistAnchorEntity();

        when(mAnchor.getPersistState())
                .thenReturn(Anchor.PersistState.PERSIST_PENDING)
                .thenReturn(Anchor.PersistState.PERSISTED);
        verify(mPersistStateChangeListener, never()).onPersistStateChanged(PersistState.PERSISTED);

        mExecutor.simulateSleepExecutingAllTasks(AnchorEntityImpl.PERSIST_STATE_CHECK_DELAY);
        verify(mPersistStateChangeListener, never()).onPersistStateChanged(PersistState.PERSISTED);
        assertThat(anchorEntity.getPersistState()).isEqualTo(PersistState.PERSIST_PENDING);

        mExecutor.simulateSleepExecutingAllTasks(AnchorEntityImpl.PERSIST_STATE_CHECK_DELAY);
        verify(mPersistStateChangeListener).onPersistStateChanged(PersistState.PERSISTED);
        assertThat(anchorEntity.getPersistState()).isEqualTo(PersistState.PERSISTED);
    }

    @Test
    public void updatePersistState_noCallbackAfterStateBecomesPersisted() throws Exception {
        AnchorEntityImpl anchorEntity = createInitAndPersistAnchorEntity();

        when(mAnchor.getPersistState()).thenReturn(Anchor.PersistState.PERSISTED);
        mExecutor.simulateSleepExecutingAllTasks(AnchorEntityImpl.PERSIST_STATE_CHECK_DELAY);

        assertThat(anchorEntity.getPersistState()).isEqualTo(PersistState.PERSISTED);
        verify(mPersistStateChangeListener).onPersistStateChanged(PersistState.PERSISTED);
        Mockito.clearInvocations(mAnchor);
        Mockito.clearInvocations(mPersistStateChangeListener);

        mExecutor.simulateSleepExecutingAllTasks(AnchorEntityImpl.PERSIST_STATE_CHECK_DELAY);
        verify(mAnchor, never()).getPersistState();
        verify(mPersistStateChangeListener, never()).onPersistStateChanged(any());
    }

    @Test
    public void updatePersistState_noQueryForPersistStateAfterDispose() throws Exception {
        AnchorEntityImpl anchorEntity = createInitAndPersistAnchorEntity();
        when(mAnchor.getPersistState()).thenReturn(Anchor.PersistState.PERSIST_PENDING);
        mExecutor.simulateSleepExecutingAllTasks(AnchorEntityImpl.PERSIST_STATE_CHECK_DELAY);
        verify(mAnchor).getPersistState();

        mExecutor.simulateSleepExecutingAllTasks(AnchorEntityImpl.PERSIST_STATE_CHECK_DELAY);
        verify(mAnchor, times(2)).getPersistState();

        Mockito.clearInvocations(mAnchor);
        anchorEntity.dispose();
        mExecutor.simulateSleepExecutingAllTasks(AnchorEntityImpl.PERSIST_STATE_CHECK_DELAY);
        verify(mAnchor, never()).getPersistState();
    }

    @Test
    public void createAnchorEntityFromPlane_returnsAnchorEntity() throws Exception {
        when(mPerceptionLibrary.getSession()).thenReturn(mSession);
        when(mPlane.createAnchor(any(), any())).thenReturn(mAnchor);
        when(mAnchor.getAnchorToken()).thenReturn(mSharedAnchorToken);

        AnchorEntityImpl anchorEntity = createAnchorEntityFromPlane();

        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);
        assertThat(((FakeNode) anchorEntity.getNode()).getName())
                .isEqualTo(AnchorEntityImpl.ANCHOR_NODE_NAME);
        assertThat(((FakeNode) anchorEntity.getNode()).getAnchorId()).isEqualTo(mSharedAnchorToken);
        assertThat(anchorEntity.getPersistState()).isEqualTo(PersistState.PERSIST_NOT_REQUESTED);
        assertThat(((FakeNode) anchorEntity.getNode()).getParent())
                .isEqualTo(mActivitySpace.getNode());
    }

    @Test
    public void createAnchorEntityFromPlane_failureToAnchor_hasErrorState() throws Exception {
        when(mPerceptionLibrary.getSession()).thenReturn(mSession);
        when(mPlane.createAnchor(any(), any())).thenReturn(null);

        AnchorEntityImpl anchorEntity = createAnchorEntityFromPlane();
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.ERROR);
        assertThat(((FakeNode) anchorEntity.getNode()).getParent()).isEqualTo(null);
    }

    @Test
    public void createAnchorEntityFromPerceptionAnchor_nativePointerMatches() throws Exception {
        when(mPerceptionLibrary.getSession()).thenReturn(mSession);
        FakeExportableAnchor fakeAnchor =
                new FakeExportableAnchor(
                        NATIVE_POINTER,
                        mSharedAnchorToken,
                        new Pose(),
                        TrackingState.Tracking,
                        PersistenceState.NotPersisted,
                        null);
        androidx.xr.arcore.Anchor perceptionAnchor = new androidx.xr.arcore.Anchor(fakeAnchor);

        AnchorEntityImpl anchorEntity = createAnchorEntityFromPerceptionAnchor(perceptionAnchor);

        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.nativePointer()).isEqualTo(NATIVE_POINTER);
    }

    @Test
    public void createAnchorEntityFromPerceptionAnchor_returnsAnchorEntity() throws Exception {
        when(mPerceptionLibrary.getSession()).thenReturn(mSession);

        FakeExportableAnchor fakeAnchor =
                new FakeExportableAnchor(
                        NATIVE_POINTER,
                        mSharedAnchorToken,
                        new Pose(),
                        TrackingState.Tracking,
                        PersistenceState.NotPersisted,
                        null);
        androidx.xr.arcore.Anchor perceptionAnchor = new androidx.xr.arcore.Anchor(fakeAnchor);

        AnchorEntityImpl anchorEntity = createAnchorEntityFromPerceptionAnchor(perceptionAnchor);
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);
        assertThat(((FakeNode) anchorEntity.getNode()).getName())
                .isEqualTo(AnchorEntityImpl.ANCHOR_NODE_NAME);
        assertThat(((FakeNode) anchorEntity.getNode()).getAnchorId()).isEqualTo(mSharedAnchorToken);
        assertThat(anchorEntity.getPersistState()).isEqualTo(PersistState.PERSIST_NOT_REQUESTED);
        assertThat(((FakeNode) anchorEntity.getNode()).getParent())
                .isEqualTo(mActivitySpace.getNode());
    }

    @Test
    public void createAnchorEntityFromPerceptionAnchor_failureToAnchor_hasErrorState()
            throws Exception {
        when(mPerceptionLibrary.getSession()).thenReturn(mSession);

        FakeExportableAnchor fakeAnchor =
                new FakeExportableAnchor(
                        NATIVE_POINTER,
                        null,
                        new Pose(),
                        TrackingState.Tracking,
                        PersistenceState.NotPersisted,
                        null);
        androidx.xr.arcore.Anchor perceptionAnchor = new androidx.xr.arcore.Anchor(fakeAnchor);

        AnchorEntityImpl anchorEntity = createAnchorEntityFromPerceptionAnchor(perceptionAnchor);
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.ERROR);
        assertThat(((FakeNode) anchorEntity.getNode()).getParent()).isEqualTo(null);
    }
}
