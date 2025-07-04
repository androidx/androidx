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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.test.rule.GrantPermissionRule;
import androidx.xr.runtime.TrackingState;
import androidx.xr.runtime.internal.Anchor.PersistenceState;
import androidx.xr.runtime.internal.AnchorEntity.OnStateChangedListener;
import androidx.xr.runtime.internal.AnchorEntity.State;
import androidx.xr.runtime.internal.Dimensions;
import androidx.xr.runtime.internal.PlaneSemantic;
import androidx.xr.runtime.internal.PlaneType;
import androidx.xr.runtime.math.Matrix4;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.runtime.openxr.ExportableAnchor;
import androidx.xr.scenecore.impl.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Plane;
import androidx.xr.scenecore.impl.perception.Session;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Node;
import com.android.extensions.xr.node.NodeRepository;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.ar.imp.apibindings.FakeImpressApiImpl;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;

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
@SuppressLint("NewApi") // TODO: b/413661481 - Remove this suppression prior to JXR stable release.
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
    private final XrExtensions mXrExtensions = XrExtensionsProvider.getXrExtensions();
    private final PerceptionLibrary mPerceptionLibrary = Mockito.mock(PerceptionLibrary.class);
    private final Session mSession = Mockito.mock(Session.class);
    private final Plane mPlane = mock(Plane.class);
    private final androidx.xr.scenecore.impl.perception.Anchor mAnchor =
            Mockito.mock(androidx.xr.scenecore.impl.perception.Anchor.class);
    private final OnStateChangedListener mAnchorStateListener =
            Mockito.mock(OnStateChangedListener.class);
    private final IBinder mSharedAnchorToken = Mockito.mock(IBinder.class);
    private final FakeScheduledExecutorService mExecutor = new FakeScheduledExecutorService();
    private final EntityManager mEntityManager = new EntityManager();
    private final androidx.xr.scenecore.impl.perception.Pose mPerceptionIdentityPose =
            androidx.xr.scenecore.impl.perception.Pose.identity();
    private long mCurrentTimeMillis = 1000000000L;
    private ActivitySpaceImpl mActivitySpace;
    private final NodeRepository mNodeRepository = NodeRepository.getInstance();
    private final FakeImpressApiImpl mFakeImpressApi = new FakeImpressApiImpl();
    private final SplitEngineSubspaceManager mSplitEngineSubspaceManager =
            Mockito.mock(SplitEngineSubspaceManager.class);

    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant("android.permission.SCENE_UNDERSTANDING");

    @Before
    public void doBeforeEachTest() throws Exception {
        ActivityController<Activity> activityController = Robolectric.buildActivity(Activity.class);
        Activity activity = activityController.create().start().get();
        when(mPerceptionLibrary.getActivity()).thenReturn(activity);
        Node taskNode = mXrExtensions.createNode();
        mActivitySpace =
                new ActivitySpaceImpl(
                        taskNode,
                        activity,
                        mXrExtensions,
                        mEntityManager,
                        () -> mXrExtensions.getSpatialState(activity),
                        /* unscaledGravityAlignedActivitySpace= */ false,
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
        ActivityController<Activity> activityController = Robolectric.buildActivity(Activity.class);
        Activity activity = activityController.create().start().get();
        Node node = mXrExtensions.createNode();
        return AnchorEntityImpl.createSemanticAnchor(
                activity,
                node,
                ANCHOR_DIMENSIONS,
                PlaneType.VERTICAL,
                PlaneSemantic.WALL,
                anchorSearchTimeout,
                mActivitySpace,
                mActivitySpaceRoot,
                mXrExtensions,
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
        ActivityController<Activity> activityController = Robolectric.buildActivity(Activity.class);
        Activity activity = activityController.create().start().get();
        Node node = mXrExtensions.createNode();
        return AnchorEntityImpl.createPersistedAnchor(
                activity,
                node,
                uuid,
                anchorSearchTimeout,
                mActivitySpace,
                mActivitySpaceRoot,
                mXrExtensions,
                mEntityManager,
                mExecutor,
                mPerceptionLibrary);
    }

    /** Creates an AnchorEntityImpl instance and initializes it with a persisted anchor. */
    private AnchorEntityImpl createInitializedPersistedAnchorEntity(
            androidx.xr.scenecore.impl.perception.Anchor anchor, UUID uuid) {
        when(mPerceptionLibrary.getSession()).thenReturn(mSession);
        when(mSession.createAnchorFromUuid(uuid)).thenReturn(anchor);
        when(anchor.getAnchorToken()).thenReturn(mSharedAnchorToken);

        ActivityController<Activity> activityController = Robolectric.buildActivity(Activity.class);
        Activity activity = activityController.create().start().get();
        Node node = mXrExtensions.createNode();
        return AnchorEntityImpl.createPersistedAnchor(
                activity,
                node,
                uuid,
                /* anchorSearchTimeout= */ null,
                mActivitySpace,
                mActivitySpaceRoot,
                mXrExtensions,
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

    private AnchorEntityImpl createAnchorEntityFromPlane() {
        when(mAnchor.persist()).thenReturn(UUID.randomUUID());

        Node node = mXrExtensions.createNode();
        ActivityController<Activity> activityController = Robolectric.buildActivity(Activity.class);
        Activity activity = activityController.create().start().get();
        return AnchorEntityImpl.createAnchorFromPlane(
                activity,
                node,
                mPlane,
                new Pose(),
                MILLISECONDS.toNanos(mCurrentTimeMillis),
                mActivitySpace,
                mActivitySpaceRoot,
                mXrExtensions,
                mEntityManager,
                mExecutor,
                mPerceptionLibrary);
    }

    private AnchorEntityImpl createAnchorEntityFromPerceptionAnchor(
            androidx.xr.arcore.Anchor perceptionAnchor) {
        ActivityController<Activity> activityController = Robolectric.buildActivity(Activity.class);
        Activity activity = activityController.create().start().get();
        Node node = mXrExtensions.createNode();

        return AnchorEntityImpl.createAnchorFromRuntimeAnchor(
                activity,
                node,
                perceptionAnchor.getRuntimeAnchor(),
                mActivitySpace,
                mActivitySpaceRoot,
                mXrExtensions,
                mEntityManager,
                mExecutor,
                mPerceptionLibrary);
    }

    /** Creates a generic glTF entity. */
    private GltfEntityImpl createGltfEntity() {
        long modelToken = -1;
        ActivityController<Activity> activityController = Robolectric.buildActivity(Activity.class);
        Activity activity = activityController.create().start().get();
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
                activity,
                model,
                mActivitySpace,
                mFakeImpressApi,
                mSplitEngineSubspaceManager,
                mXrExtensions,
                mEntityManager,
                mExecutor);
    }

    @Test
    public void createAnchorEntityWithPersistedAnchor_returnsAnchored() throws Exception {
        AnchorEntityImpl anchorEntity =
                createInitializedPersistedAnchorEntity(mAnchor, UUID.randomUUID());
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);
        assertThat(mNodeRepository.getName(anchorEntity.getNode()))
                .isEqualTo(AnchorEntityImpl.ANCHOR_NODE_NAME);
        assertThat(mNodeRepository.getAnchorId(anchorEntity.getNode()))
                .isEqualTo(mSharedAnchorToken);
    }

    @Test
    public void createAnchorEntityWithPersistedAnchor_persistAnchor_returnsUuid() throws Exception {
        UUID uuid = UUID.randomUUID();
        AnchorEntityImpl anchorEntity = createInitializedPersistedAnchorEntity(mAnchor, uuid);
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);
        assertThat(mNodeRepository.getName(anchorEntity.getNode()))
                .isEqualTo(AnchorEntityImpl.ANCHOR_NODE_NAME);
        assertThat(mNodeRepository.getAnchorId(anchorEntity.getNode()))
                .isEqualTo(mSharedAnchorToken);
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
    public void createAnchorEntity_defaultUnanchored() throws Exception {
        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);
        assertThat(mNodeRepository.getName(anchorEntity.getNode()))
                .isEqualTo(AnchorEntityImpl.ANCHOR_NODE_NAME);
    }

    @Test
    public void createAndInitAnchor_returnsAnchored() throws Exception {
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);
        assertThat(mNodeRepository.getName(anchorEntity.getNode()))
                .isEqualTo(AnchorEntityImpl.ANCHOR_NODE_NAME);
        assertThat(mNodeRepository.getAnchorId(anchorEntity.getNode()))
                .isEqualTo(mSharedAnchorToken);
    }

    @Test
    public void createAndInitAnchor_noPlanes_remainsUnanchored() throws Exception {
        when(mPerceptionLibrary.getSession()).thenReturn(mSession);
        when(mSession.getAllPlanes()).thenReturn(ImmutableList.of());

        AnchorEntityImpl anchorEntity = createSemanticAnchorEntity();

        assertThat(anchorEntity.getState()).isEqualTo(State.UNANCHORED);
        assertThat(mNodeRepository.getName(anchorEntity.getNode()))
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
        assertThat(mNodeRepository.getName(anchorEntity.getNode()))
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
            verify(mAnchorStateListener, never()).onStateChanged(anyInt());
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

        Node parentNode = parentEntity.getNode();
        Node childNode1 = childEntity1.getNode();
        Node childNode2 = childEntity2.getNode();

        assertThat(NodeRepository.getInstance().getParent(childNode1)).isEqualTo(parentNode);
        assertThat(NodeRepository.getInstance().getParent(childNode2)).isEqualTo(parentNode);
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
        ActivityController<Activity> activityController = Robolectric.buildActivity(Activity.class);
        Activity activity = activityController.create().start().get();
        Node node = mXrExtensions.createNode();
        AnchorEntityImpl anchorEntity =
                AnchorEntityImpl.createSemanticAnchor(
                        activity,
                        node,
                        /* dimensions= */ null,
                        /* planeType= */ null,
                        /* planeSemantic= */ null,
                        /* anchorSearchTimeout= */ null,
                        /* activitySpace= */ null,
                        mActivitySpaceRoot,
                        mXrExtensions,
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
        ActivityController<Activity> activityController = Robolectric.buildActivity(Activity.class);
        Activity activity = activityController.create().start().get();
        Node node = mXrExtensions.createNode();
        AnchorEntityImpl anchorEntity =
                AnchorEntityImpl.createSemanticAnchor(
                        activity,
                        node,
                        /* dimensions= */ null,
                        /* planeType= */ null,
                        /* planeSemantic= */ null,
                        /* anchorSearchTimeout= */ null,
                        mActivitySpace,
                        /* activitySpaceRoot= */ null,
                        mXrExtensions,
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

        Node anchorNode = anchorEntity.getNode();
        Node rootNode = mActivitySpace.getNode();
        assertThat(NodeRepository.getInstance().getParent(anchorNode)).isNull();

        // The anchor starts as unanchored. Advance the executor to wait for it to become anchored
        // and
        // verify that the parent is the root node.
        advanceClock(AnchorEntityImpl.ANCHOR_SEARCH_DELAY);
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);
        assertThat(NodeRepository.getInstance().getParent(anchorNode)).isEqualTo(rootNode);
    }

    @Test
    public void disposeAnchor_detachesAnchor() throws Exception {
        when(mAnchor.detach()).thenReturn(true);
        AnchorEntityImpl anchorEntity = createAndInitAnchorEntity();
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
    }

    @Test
    public void createAnchorEntityFromPlane_returnsAnchorEntity() throws Exception {
        when(mPerceptionLibrary.getSession()).thenReturn(mSession);
        when(mPlane.createAnchor(any(), any())).thenReturn(mAnchor);
        when(mAnchor.getAnchorToken()).thenReturn(mSharedAnchorToken);

        AnchorEntityImpl anchorEntity = createAnchorEntityFromPlane();

        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);
        assertThat(NodeRepository.getInstance().getName(anchorEntity.getNode()))
                .isEqualTo(AnchorEntityImpl.ANCHOR_NODE_NAME);
        assertThat(NodeRepository.getInstance().getAnchorId(anchorEntity.getNode()))
                .isEqualTo(mSharedAnchorToken);
        assertThat(NodeRepository.getInstance().getParent(anchorEntity.getNode()))
                .isEqualTo(mActivitySpace.getNode());
    }

    @Test
    public void createAnchorEntityFromPlane_failureToAnchor_hasErrorState() throws Exception {
        when(mPerceptionLibrary.getSession()).thenReturn(mSession);
        when(mPlane.createAnchor(any(), any())).thenReturn(null);

        AnchorEntityImpl anchorEntity = createAnchorEntityFromPlane();
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.ERROR);
        assertThat(NodeRepository.getInstance().getParent(anchorEntity.getNode())).isEqualTo(null);
    }

    @Test
    public void createAnchorEntityFromRuntimeAnchor_nativePointerMatches() throws Exception {
        when(mPerceptionLibrary.getSession()).thenReturn(mSession);
        FakeExportableAnchor fakeAnchor =
                new FakeExportableAnchor(
                        NATIVE_POINTER,
                        mSharedAnchorToken,
                        new Pose(),
                        TrackingState.TRACKING,
                        PersistenceState.NOT_PERSISTED,
                        null);
        androidx.xr.arcore.Anchor perceptionAnchor = new androidx.xr.arcore.Anchor(fakeAnchor);

        AnchorEntityImpl anchorEntity = createAnchorEntityFromPerceptionAnchor(perceptionAnchor);

        assertThat(anchorEntity).isNotNull();
    }

    @Test
    public void createAnchorEntityFromPerceptionAnchor_returnsAnchorEntity() throws Exception {
        when(mPerceptionLibrary.getSession()).thenReturn(mSession);

        FakeExportableAnchor fakeAnchor =
                new FakeExportableAnchor(
                        NATIVE_POINTER,
                        mSharedAnchorToken,
                        new Pose(),
                        TrackingState.TRACKING,
                        PersistenceState.NOT_PERSISTED,
                        null);
        androidx.xr.arcore.Anchor perceptionAnchor = new androidx.xr.arcore.Anchor(fakeAnchor);

        AnchorEntityImpl anchorEntity = createAnchorEntityFromPerceptionAnchor(perceptionAnchor);
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.ANCHORED);
        assertThat(NodeRepository.getInstance().getName(anchorEntity.getNode()))
                .isEqualTo(AnchorEntityImpl.ANCHOR_NODE_NAME);
        assertThat(NodeRepository.getInstance().getAnchorId(anchorEntity.getNode()))
                .isEqualTo(mSharedAnchorToken);
        assertThat(NodeRepository.getInstance().getParent(anchorEntity.getNode()))
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
                        TrackingState.TRACKING,
                        PersistenceState.NOT_PERSISTED,
                        null);
        androidx.xr.arcore.Anchor perceptionAnchor = new androidx.xr.arcore.Anchor(fakeAnchor);

        AnchorEntityImpl anchorEntity = createAnchorEntityFromPerceptionAnchor(perceptionAnchor);
        assertThat(anchorEntity).isNotNull();
        assertThat(anchorEntity.getState()).isEqualTo(State.ERROR);
        assertThat(mNodeRepository.getParent(anchorEntity.getNode())).isEqualTo(null);
    }
}
