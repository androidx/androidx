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

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.xr.extensions.XrExtensions;
import androidx.xr.extensions.node.Node;
import androidx.xr.extensions.node.NodeTransaction;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.runtime.openxr.ExportableAnchor;
import androidx.xr.scenecore.JxrPlatformAdapter.ActivitySpace;
import androidx.xr.scenecore.JxrPlatformAdapter.AnchorEntity;
import androidx.xr.scenecore.JxrPlatformAdapter.AnchorEntity.OnStateChangedListener;
import androidx.xr.scenecore.JxrPlatformAdapter.Dimensions;
import androidx.xr.scenecore.JxrPlatformAdapter.Entity;
import androidx.xr.scenecore.JxrPlatformAdapter.PlaneSemantic;
import androidx.xr.scenecore.JxrPlatformAdapter.PlaneType;
import androidx.xr.scenecore.impl.perception.Anchor;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Plane;
import androidx.xr.scenecore.impl.perception.Plane.PlaneData;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

/**
 * Implementation of AnchorEntity.
 *
 * <p>This entity creates trackable anchors in space.
 */
@SuppressWarnings("BanSynchronizedMethods")
class AnchorEntityImpl extends SystemSpaceEntityImpl implements AnchorEntity {
    public static final Duration ANCHOR_SEARCH_DELAY = Duration.ofMillis(500);
    public static final Duration PERSIST_STATE_CHECK_DELAY = Duration.ofMillis(500);
    public static final String ANCHOR_NODE_NAME = "AnchorNode";
    private static final String TAG = "AnchorEntityImpl";
    private static final String SCENE_UNDERSTANDING_PERMISSION =
            "android.permission.SCENE_UNDERSTANDING";
    private final ActivitySpaceImpl mActivitySpace;
    private final AndroidXrEntity mActivitySpaceRoot;
    private final PerceptionLibrary mPerceptionLibrary;
    private OnStateChangedListener mOnStateChangedListener;
    private State mState = State.UNANCHORED;
    private PersistState mPersistState = PersistState.PERSIST_NOT_REQUESTED;
    private Anchor mAnchor;
    private UUID mUuid = null;
    private PersistStateChangeListener mPersistStateChangeListener;
    private final OpenXrActivityPoseHelper mOpenXrActivityPoseHelper;

    private static class AnchorCreationData {

        static final int ANCHOR_CREATION_SEMANTIC = 1;
        static final int ANCHOR_CREATION_PERSISTED = 2;
        static final int ANCHOR_CREATION_PLANE = 3;
        static final int ANCHOR_CREATION_PERCEPTION_ANCHOR = 4;

        @AnchorCreationType int mAnchorCreationType;

        /** IntDef for Anchor creation types. */
        @IntDef({
            ANCHOR_CREATION_SEMANTIC,
            ANCHOR_CREATION_PERSISTED,
            ANCHOR_CREATION_PLANE,
            ANCHOR_CREATION_PERCEPTION_ANCHOR,
        })
        @Retention(RetentionPolicy.SOURCE)
        private @interface AnchorCreationType {}

        // Anchor that is already created via Perception API.
        androidx.xr.arcore.Anchor mPerceptionAnchor;

        // Anchor search deadline for semantic and persisted anchors.
        Long mAnchorSearchDeadline;

        // Fields exclusively for semantic anchors.
        Dimensions mDimensions;
        PlaneType mPlaneType;
        PlaneSemantic mPlaneSemantic;

        // Fields exclusively for persisted anchors.
        UUID mUuid = null;

        // Fields exclusively for plane anchors.
        Plane mPlane;
        Pose mPlaneOffsetPose;
        Long mPlaneDataTimeNs;
    }

    static AnchorEntityImpl createSemanticAnchor(
            Node node,
            Dimensions dimensions,
            PlaneType planeType,
            PlaneSemantic planeSemantic,
            Duration anchorSearchTimeout,
            ActivitySpace activitySpace,
            Entity activitySpaceRoot,
            XrExtensions extensions,
            EntityManager entityManager,
            ScheduledExecutorService executor,
            PerceptionLibrary perceptionLibrary) {
        AnchorCreationData anchorCreationData = new AnchorCreationData();
        anchorCreationData.mAnchorCreationType = AnchorCreationData.ANCHOR_CREATION_SEMANTIC;
        anchorCreationData.mDimensions = dimensions;
        anchorCreationData.mPlaneType = planeType;
        anchorCreationData.mPlaneSemantic = planeSemantic;
        anchorCreationData.mAnchorSearchDeadline = getAnchorDeadline(anchorSearchTimeout);
        return new AnchorEntityImpl(
                node,
                anchorCreationData,
                activitySpace,
                activitySpaceRoot,
                extensions,
                entityManager,
                executor,
                perceptionLibrary);
    }

    static AnchorEntityImpl createPersistedAnchor(
            Node node,
            UUID uuid,
            Duration anchorSearchTimeout,
            ActivitySpace activitySpace,
            Entity activitySpaceRoot,
            XrExtensions extensions,
            EntityManager entityManager,
            ScheduledExecutorService executor,
            PerceptionLibrary perceptionLibrary) {
        AnchorCreationData anchorCreationData = new AnchorCreationData();
        anchorCreationData.mAnchorCreationType = AnchorCreationData.ANCHOR_CREATION_PERSISTED;
        anchorCreationData.mUuid = uuid;
        anchorCreationData.mAnchorSearchDeadline = getAnchorDeadline(anchorSearchTimeout);
        return new AnchorEntityImpl(
                node,
                anchorCreationData,
                activitySpace,
                activitySpaceRoot,
                extensions,
                entityManager,
                executor,
                perceptionLibrary);
    }

    static AnchorEntityImpl createAnchorFromPlane(
            Node node,
            Plane plane,
            Pose planeOffsetPose,
            @Nullable Long planeDataTimeNs,
            ActivitySpace activitySpace,
            Entity activitySpaceRoot,
            XrExtensions extensions,
            EntityManager entityManager,
            ScheduledExecutorService executor,
            PerceptionLibrary perceptionLibrary) {
        AnchorCreationData anchorCreationData = new AnchorCreationData();
        anchorCreationData.mAnchorCreationType = AnchorCreationData.ANCHOR_CREATION_PLANE;
        anchorCreationData.mPlane = plane;
        anchorCreationData.mPlaneOffsetPose = planeOffsetPose;
        anchorCreationData.mPlaneDataTimeNs = planeDataTimeNs;
        return new AnchorEntityImpl(
                node,
                anchorCreationData,
                activitySpace,
                activitySpaceRoot,
                extensions,
                entityManager,
                executor,
                perceptionLibrary);
    }

    static AnchorEntityImpl createAnchorFromPerceptionAnchor(
            Node node,
            androidx.xr.arcore.Anchor anchor,
            ActivitySpace activitySpace,
            Entity activitySpaceRoot,
            XrExtensions extensions,
            EntityManager entityManager,
            ScheduledExecutorService executor,
            PerceptionLibrary perceptionLibrary) {
        AnchorCreationData anchorCreationData = new AnchorCreationData();
        anchorCreationData.mAnchorCreationType =
                AnchorCreationData.ANCHOR_CREATION_PERCEPTION_ANCHOR;
        anchorCreationData.mPerceptionAnchor = anchor;
        return new AnchorEntityImpl(
                node,
                anchorCreationData,
                activitySpace,
                activitySpaceRoot,
                extensions,
                entityManager,
                executor,
                perceptionLibrary);
    }

    protected AnchorEntityImpl(
            Node node,
            AnchorCreationData anchorCreationData,
            ActivitySpace activitySpace,
            Entity activitySpaceRoot,
            XrExtensions extensions,
            EntityManager entityManager,
            ScheduledExecutorService executor,
            PerceptionLibrary perceptionLibrary) {
        super(node, extensions, entityManager, executor);
        mPerceptionLibrary = perceptionLibrary;

        try (NodeTransaction transaction = extensions.createNodeTransaction()) {
            transaction.setName(node, ANCHOR_NODE_NAME).apply();
        }

        if (activitySpace instanceof ActivitySpaceImpl) {
            mActivitySpace = (ActivitySpaceImpl) activitySpace;
        } else {
            Log.e(
                    TAG,
                    "ActivitySpace is not an instance of ActivitySpaceImpl.Anchor is in Error"
                            + " state.");
            mState = State.ERROR;
            mActivitySpace = null;
        }

        if (activitySpaceRoot instanceof AndroidXrEntity) {
            mActivitySpaceRoot = (AndroidXrEntity) activitySpaceRoot;
        } else {
            Log.e(
                    TAG,
                    "ActivitySpaceRoot is not an instance of AndroidXrEntity. Anchor is in Error"
                            + " state.");
            mState = State.ERROR;
            mActivitySpaceRoot = null;
        }

        if (mActivitySpace != null && mActivitySpaceRoot != null) {
            mOpenXrActivityPoseHelper =
                    new OpenXrActivityPoseHelper(
                            (ActivitySpaceImpl) activitySpace, (AndroidXrEntity) activitySpaceRoot);
        } else {
            mOpenXrActivityPoseHelper = null;
        }

        if (ContextCompat.checkSelfPermission(
                        perceptionLibrary.getActivity(), SCENE_UNDERSTANDING_PERMISSION)
                == PackageManager.PERMISSION_DENIED) {
            Log.e(
                    TAG,
                    "Scene understanding permission is not granted. Anchor is in"
                            + " PERMISSIONS_NOT_GRANTED state.");
            mState = State.PERMISSIONS_NOT_GRANTED;
        }

        // Return early if the state is already in an error state.
        if (mState == State.ERROR || mState == State.PERMISSIONS_NOT_GRANTED) {
            return;
        }

        // If we are creating a semantic or persisted anchor then we need to search for the anchor
        // asynchronously. Otherwise we can create the anchor on the plane.
        if (anchorCreationData.mAnchorCreationType
                == AnchorCreationData.ANCHOR_CREATION_PERCEPTION_ANCHOR) {
            tryConvertAnchor(anchorCreationData.mPerceptionAnchor);
        } else if (anchorCreationData.mAnchorCreationType
                        == AnchorCreationData.ANCHOR_CREATION_SEMANTIC
                || anchorCreationData.mAnchorCreationType
                        == AnchorCreationData.ANCHOR_CREATION_PERSISTED) {
            tryFindAnchor(anchorCreationData);
        } else if (anchorCreationData.mAnchorCreationType
                == AnchorCreationData.ANCHOR_CREATION_PLANE) {
            tryCreateAnchorForPlane(anchorCreationData);
        }
    }

    @Nullable
    private static Long getAnchorDeadline(Duration anchorSearchTimeout) {
        // If the timeout is zero or null then we return null here and the anchor search will
        // continue
        // indefinitely.
        if (anchorSearchTimeout == null || anchorSearchTimeout.isZero()) {
            return null;
        }
        return SystemClock.uptimeMillis() + anchorSearchTimeout.toMillis();
    }

    // Converts a perception anchor to JXRCore runtime anchor.
    private void tryConvertAnchor(androidx.xr.arcore.Anchor perceptionAnchor) {
        ExportableAnchor exportableAnchor = (ExportableAnchor) perceptionAnchor.getRuntimeAnchor();
        mAnchor =
                new Anchor(exportableAnchor.getNativePointer(), exportableAnchor.getAnchorToken());
        if (mAnchor.getAnchorToken() == null) {
            updateState(State.ERROR);
            return;
        }
        updateState(State.ANCHORED);
    }

    // Creates an anchor on the provided plane.
    private void tryCreateAnchorForPlane(AnchorCreationData anchorCreationData) {
        androidx.xr.scenecore.impl.perception.Pose perceptionPose =
                RuntimeUtils.poseToPerceptionPose(anchorCreationData.mPlaneOffsetPose);
        mAnchor =
                anchorCreationData.mPlane.createAnchor(
                        perceptionPose, anchorCreationData.mPlaneDataTimeNs);
        if (mAnchor == null || mAnchor.getAnchorToken() == null) {
            updateState(State.ERROR);
            return;
        }
        updateState(State.ANCHORED);
    }

    // Schedules a search for the anchor.
    private void scheduleTryFindAnchor(AnchorCreationData anchorCreationData) {
        ScheduledFuture<?> unusedAnchorFuture =
                mExecutor.schedule(
                        () -> tryFindAnchor(anchorCreationData),
                        ANCHOR_SEARCH_DELAY.toMillis(),
                        MILLISECONDS);
    }

    // Checks if the anchor search has exceeded the deadline.
    private boolean searchDeadlineExceeded(Long anchorSearchDeadline) {
        // If the system is paused it will continue to count after it wakes up.
        return anchorSearchDeadline != null && SystemClock.uptimeMillis() > anchorSearchDeadline;
    }

    private synchronized void cancelAnchorSearch() {
        if (mState == State.UNANCHORED) {
            Log.i(TAG, "Stopping search for anchor, reached timeout.");
            updateState(State.TIMED_OUT);
        }
    }

    // Searches for the anchor and updates the state based on the result. If the anchor wasn't found
    // then the search is scheduled again if the deadline has not been exceeded.
    private void tryFindAnchor(AnchorCreationData anchorCreationData) {
        if (mActivitySpace == null) {
            Log.e(TAG, "Skipping search for anchor there is no valid parent.");
            return;
        }
        synchronized (this) {
            if (mState != State.UNANCHORED) {
                // This should only be searching for an anchor if the state is UNANCHORED. If the
                // state is
                // ANCHORED then the anchor was already found, if it is ERROR then the entity no
                // longer can
                // use the anchor. Return here to stop the search.
                Log.i(TAG, "Stopping search for anchor, the state is: " + mState);
                return;
            }
        }
        // Check if we are passed the deadline if so, cancel the search.
        if (searchDeadlineExceeded(anchorCreationData.mAnchorSearchDeadline)) {
            cancelAnchorSearch();
            return;
        }

        if (mPerceptionLibrary.getSession() == null) {
            scheduleTryFindAnchor(anchorCreationData);
            return;
        }

        if (anchorCreationData.mAnchorCreationType == AnchorCreationData.ANCHOR_CREATION_SEMANTIC) {
            mAnchor = findPlaneAnchor(anchorCreationData);
        } else if (anchorCreationData.mAnchorCreationType
                == AnchorCreationData.ANCHOR_CREATION_PERSISTED) {
            mAnchor =
                    mPerceptionLibrary.getSession().createAnchorFromUuid(anchorCreationData.mUuid);
        } else {
            Log.e(
                    TAG,
                    "Searching for anchor creation type is not supported: "
                            + anchorCreationData.mAnchorCreationType);
        }

        if (mAnchor == null || mAnchor.getAnchorToken() == null) {
            scheduleTryFindAnchor(anchorCreationData);
            return;
        }
        Log.i(TAG, "Received anchor: " + mAnchor.getAnchorToken());
        // TODO: b/330933143 - Handle Additional anchor states (e.g. Error/ Becoming unanchored)
        synchronized (this) {
            // Make sure that we are still looking for the anchor before updating the state. The
            // application might have closed or disposed of the AnchorEntity while the search was
            // still
            // active on another thread.
            if (mState != State.UNANCHORED
                    || searchDeadlineExceeded(anchorCreationData.mAnchorSearchDeadline)) {
                Log.i(TAG, "Found anchor but no longer searching.");
                if (searchDeadlineExceeded(anchorCreationData.mAnchorSearchDeadline)) {
                    cancelAnchorSearch();
                }
                // Detach the found anchor since it is no longer needed.
                if (!mAnchor.detach()) {
                    Log.e(TAG, "Error when detaching anchor.");
                }
                return;
            }
            updateState(State.ANCHORED);
            if (anchorCreationData.mAnchorCreationType
                    == AnchorCreationData.ANCHOR_CREATION_PERSISTED) {
                mUuid = anchorCreationData.mUuid;
                updatePersistState(PersistState.PERSISTED);
            }
        }
    }

    // Tries to find a plane that matches the semantic anchor requirements. This creates an anchor
    // on
    // the plane if found.
    @Nullable
    private Anchor findPlaneAnchor(AnchorCreationData anchorCreationData) {
        for (Plane plane : mPerceptionLibrary.getSession().getAllPlanes()) {
            long timeNow = SystemClock.uptimeMillis() * 1000000;
            PlaneData planeData = plane.getData(timeNow);
            if (planeData == null) {
                Log.e(TAG, "Plane data is null for plane");
                continue;
            }
            Log.i(
                    TAG,
                    "Found a matching plane with Extent Width: "
                            + planeData.extentWidth
                            + ", Extent Height: "
                            + planeData.extentHeight
                            + ", Type: "
                            + planeData.type
                            + ", Label: "
                            + planeData.label);
            Plane.Type perceptionType = RuntimeUtils.getPlaneType(anchorCreationData.mPlaneType);
            Plane.Label perceptionLabel =
                    RuntimeUtils.getPlaneLabel(anchorCreationData.mPlaneSemantic);
            if (anchorCreationData.mDimensions.width <= planeData.extentWidth
                    && anchorCreationData.mDimensions.height <= planeData.extentHeight
                    && (planeData.type == perceptionType || perceptionType == Plane.Type.ARBITRARY)
                    && (planeData.label == perceptionLabel
                            || perceptionLabel == Plane.Label.UNKNOWN)) {
                return plane.createAnchor(
                        androidx.xr.scenecore.impl.perception.Pose.identity(), timeNow);
            }
        }
        return null;
    }

    private synchronized void updateState(State newState) {
        if (mState == newState) {
            return;
        }
        mState = newState;
        if (mState == State.ANCHORED) {
            try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
                // Attach to the root CPM node. This will enable the anchored content to be visible.
                // Note
                // that the parent of the Entity is null, but the CPM Node is still attached.
                transaction
                        .setParent(mNode, mActivitySpace.getNode())
                        .setAnchorId(mNode, mAnchor.getAnchorToken())
                        .apply();
            }
        }
        if (mOnStateChangedListener != null) {
            mOnStateChangedListener.onStateChanged(mState);
        }
    }

    @Override
    public State getState() {
        return mState;
    }

    @Override
    public void setOnStateChangedListener(OnStateChangedListener onStateChangedListener) {
        mOnStateChangedListener = onStateChangedListener;
        if (mState == State.PERMISSIONS_NOT_GRANTED) {
            onStateChangedListener.onStateChanged(State.PERMISSIONS_NOT_GRANTED);
        }
    }

    @Override
    @Nullable
    public UUID persist() {
        if (mUuid != null) {
            return mUuid;
        }
        if (mState != State.ANCHORED) {
            Log.e(TAG, "Cannot persist an anchor that is not in the ANCHORED state.");
            return null;
        }
        mUuid = mAnchor.persist();
        if (mUuid == null) {
            Log.e(TAG, "Failed to get a UUID for the anchor.");
            return null;
        }
        updatePersistState(PersistState.PERSIST_PENDING);
        schedulePersistStateCheck();
        return mUuid;
    }

    private void schedulePersistStateCheck() {
        ScheduledFuture<?> unusedPersistStateFuture =
                mExecutor.schedule(
                        this::checkPersistState,
                        PERSIST_STATE_CHECK_DELAY.toMillis(),
                        MILLISECONDS);
    }

    private void checkPersistState() {
        synchronized (this) {
            if (mAnchor == null) {
                Log.i(
                        TAG,
                        "Anchor is disposed before becoming persisted, stop checking its persist"
                                + " state.");
                return;
            }
            if (mAnchor.getPersistState() == Anchor.PersistState.PERSISTED) {
                updatePersistState(PersistState.PERSISTED);
                Log.i(TAG, "Anchor is persisted.");
                return;
            }
        }
        schedulePersistStateCheck();
    }

    @Override
    public void registerPersistStateChangeListener(
            PersistStateChangeListener persistStateChangeListener) {
        mPersistStateChangeListener = persistStateChangeListener;
    }

    private synchronized void updatePersistState(PersistState newPersistState) {
        if (mPersistState == newPersistState) {
            return;
        }
        mPersistState = newPersistState;
        if (mPersistStateChangeListener != null) {
            mPersistStateChangeListener.onPersistStateChanged(newPersistState);
        }
    }

    @Override
    public PersistState getPersistState() {
        return mPersistState;
    }

    @Override
    public long nativePointer() {
        return mAnchor.getAnchorId();
    }

    @Override
    public Pose getPose() {
        throw new UnsupportedOperationException("Cannot get 'pose' on an AnchorEntity.");
    }

    @Override
    public void setPose(Pose pose) {
        throw new UnsupportedOperationException("Cannot set 'pose' on an AnchorEntity.");
    }

    @Override
    public void setScale(Vector3 scale) {
        // TODO(b/349391097): make this behavior consistent with ActivitySpaceImpl
        throw new UnsupportedOperationException("Cannot set 'scale' on an AnchorEntity.");
    }

    @Override
    public Pose getPoseInActivitySpace() {
        synchronized (this) {
            if (mActivitySpace == null || mOpenXrActivityPoseHelper == null) {
                throw new IllegalStateException(
                        "Cannot get pose in Activity Space with a null Activity Space.");
            }

            if (mState != State.ANCHORED) {
                Log.w(
                        TAG,
                        "Cannot retrieve pose in underlying space. Ensure that the anchor is"
                                + " anchored before calling this method. Returning identity pose.");
                return new Pose();
            }

            return mOpenXrActivityPoseHelper.getPoseInActivitySpace(
                    getPoseInOpenXrReferenceSpace());
        }
    }

    // TODO: b/360168321 Use the OpenXrPosableHelper when retrieving the pose in world space.
    @Override
    public Pose getActivitySpacePose() {
        if (mOpenXrActivityPoseHelper == null) {
            throw new IllegalStateException(
                    "Cannot get pose in Activity Space. Anchor initialized in Error state.");
        }
        return mOpenXrActivityPoseHelper.getActivitySpacePose(getPoseInOpenXrReferenceSpace());
    }

    @Override
    public Vector3 getActivitySpaceScale() {
        return mOpenXrActivityPoseHelper.getActivitySpaceScale(getWorldSpaceScale());
    }

    @Override
    public void setParent(Entity parent) {
        throw new UnsupportedOperationException("Cannot set 'parent' on an  AnchorEntity.");
    }

    @SuppressWarnings("ObjectToString")
    @Override
    public void dispose() {
        Log.i(TAG, "Disposing " + this);

        synchronized (this) {
            // Return early if it is already in the error state.
            if (mState == State.ERROR) {
                return;
            }
            updateState(State.ERROR);
            if (mAnchor != null && !mAnchor.detach()) {
                Log.e(TAG, "Error when detaching anchor.");
            }
            mAnchor = null;
        }

        // Set the parent of the CPM node to null; to hide the anchored content.The parent of the
        // entity
        // was always null so does not need to be reset.
        try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
            transaction.setAnchorId(mNode, null);
            transaction.setParent(mNode, null).apply();
        }
        super.dispose();
    }
}
