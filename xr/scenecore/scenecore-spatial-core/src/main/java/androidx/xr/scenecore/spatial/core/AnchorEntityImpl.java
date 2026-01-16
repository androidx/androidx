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

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.xr.arcore.Anchor;
import androidx.xr.arcore.runtime.ExportableAnchor;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.runtime.ActivitySpace;
import androidx.xr.scenecore.runtime.AnchorEntity;
import androidx.xr.scenecore.runtime.Entity;
import androidx.xr.scenecore.runtime.PerceptionSpaceScenePose;
import androidx.xr.scenecore.runtime.Space;
import androidx.xr.scenecore.runtime.SpaceValue;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Node;
import com.android.extensions.xr.node.NodeTransaction;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Implementation of AnchorEntity.
 *
 * <p>This entity creates trackable anchors in space.
 */
@SuppressLint("NewApi") // TODO: b/413661481 - Remove this suppression prior to JXR stable release.
@SuppressWarnings("BanSynchronizedMethods")
class AnchorEntityImpl extends SystemSpaceEntityImpl implements AnchorEntity {
    public static final String ANCHOR_NODE_NAME = "AnchorNode";
    private final ActivitySpaceImpl mActivitySpace;
    private final OpenXrScenePoseHelper mOpenXrScenePoseHelper;
    private OnStateChangedListener mOnStateChangedListener;
    private @State int mState = State.UNANCHORED;

    protected AnchorEntityImpl(
            Context context,
            Node node,
            ActivitySpace activitySpace,
            XrExtensions extensions,
            EntityManager entityManager,
            ScheduledExecutorService executor) {
        super(context, node, extensions, entityManager, executor);

        try (NodeTransaction transaction = extensions.createNodeTransaction()) {
            transaction.setName(node, ANCHOR_NODE_NAME).apply();
        }

        if (activitySpace instanceof ActivitySpaceImpl) {
            mActivitySpace = (ActivitySpaceImpl) activitySpace;
        } else {
            mState = State.ERROR;
            mActivitySpace = null;
        }

        if (mActivitySpace != null) {
            mOpenXrScenePoseHelper = new OpenXrScenePoseHelper((ActivitySpaceImpl) activitySpace);
        } else {
            mOpenXrScenePoseHelper = null;
        }
    }

    static AnchorEntityImpl create(
            Context context,
            Node node,
            ActivitySpace activitySpace,
            XrExtensions extensions,
            EntityManager entityManager,
            ScheduledExecutorService executor) {
        return new AnchorEntityImpl(
                context, node, activitySpace, extensions, entityManager, executor);
    }

    @Override
    public boolean setAnchor(@NonNull Anchor anchor) {
        synchronized (this) {
            if (mState == State.ERROR || !(anchor.getRuntimeAnchor() instanceof ExportableAnchor)) {
                return false;
            }
            ExportableAnchor exportableAnchor = (ExportableAnchor) anchor.getRuntimeAnchor();
            try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
                // Attach to the root CPM node. This will enable the anchored content to be visible.
                // Note that the parent of the Entity is null, but the CPM Node is still attached.
                transaction
                        .setParent(mNode, mActivitySpace.mNode)
                        .setAnchorId(mNode, exportableAnchor.getAnchorToken())
                        .apply();
            }
            updateState(State.ANCHORED);
            return true;
        }
    }

    private void updateState(@State int newState) {
        synchronized (this) {
            if (newState != mState) {
                mState = newState;
                if (mOnStateChangedListener != null) {
                    mOnStateChangedListener.onStateChanged(mState);
                }
            }
        }
    }

    @Override
    public @State int getState() {
        synchronized (this) {
            return mState;
        }
    }

    @Override
    public void setOnStateChangedListener(@NonNull OnStateChangedListener onStateChangedListener) {
        mOnStateChangedListener = onStateChangedListener;
        mExecutor.execute(() -> mOnStateChangedListener.onStateChanged(mState));
    }

    @Override
    public @NonNull Pose getPose(@SpaceValue int relativeTo) {
        switch (relativeTo) {
            case Space.PARENT:
                throw new UnsupportedOperationException(
                        "AnchorEntity is a root space and it does not have a parent.");
            case Space.ACTIVITY:
                return getPoseInActivitySpace();
            case Space.REAL_WORLD:
                return getPoseInPerceptionSpace();
            default:
                throw new IllegalArgumentException("Unsupported relativeTo value: " + relativeTo);
        }
    }

    @Override
    public void setPose(@NonNull Pose pose, @SpaceValue int relativeTo) {
        throw new UnsupportedOperationException("Cannot set 'pose' on an AnchorEntity.");
    }

    @Override
    public void setScale(@NonNull Vector3 scale, @SpaceValue int relativeTo) {
        throw new UnsupportedOperationException("Cannot set 'scale' on an AnchorEntity.");
    }

    @Override
    public @NonNull Vector3 getScale(@SpaceValue int relativeTo) {
        switch (relativeTo) {
            case Space.PARENT:
                throw new UnsupportedOperationException(
                        "AnchorEntity is a root space and it does not have a parent.");
            case Space.ACTIVITY:
                return getActivitySpaceScale();
            case Space.REAL_WORLD:
                return super.getWorldSpaceScale();
            default:
                throw new IllegalArgumentException("Unsupported relativeTo value: " + relativeTo);
        }
    }

    @Override
    public @NonNull Pose getPoseInActivitySpace() {
        synchronized (this) {
            if (mActivitySpace == null || mOpenXrScenePoseHelper == null) {
                throw new IllegalStateException(
                        "Cannot get pose in Activity Space with a null Activity Space.");
            }

            if (mState != State.ANCHORED) {
                return new Pose();
            }

            return mOpenXrScenePoseHelper.getPoseInActivitySpace(getPoseInOpenXrReferenceSpace());
        }
    }

    public Pose getPoseInPerceptionSpace() {
        PerceptionSpaceScenePose perceptionSpaceScenePose =
                mEntityManager
                        .getSystemSpaceActivityPoseOfType(PerceptionSpaceScenePose.class)
                        .get(0);
        return transformPoseTo(new Pose(), perceptionSpaceScenePose);
    }

    @Override
    public @NonNull Pose getActivitySpacePose() {
        if (mOpenXrScenePoseHelper == null) {
            throw new IllegalStateException(
                    "Cannot get pose in Activity Space. Anchor initialized in Error state.");
        }
        return mOpenXrScenePoseHelper.getActivitySpacePose(getPoseInOpenXrReferenceSpace());
    }

    @Override
    public @NonNull Vector3 getActivitySpaceScale() {
        return mOpenXrScenePoseHelper.getActivitySpaceScale(getWorldSpaceScale());
    }

    @Override
    public void setParent(Entity parent) {
        throw new UnsupportedOperationException("Cannot set 'parent' on an  AnchorEntity.");
    }

    @Override
    public void dispose() {
        synchronized (this) {
            // Return early if it is already in the error state.
            if (mState == AnchorEntity.State.ERROR) {
                return;
            }
            updateState(AnchorEntity.State.ERROR);
        }

        // Set the parent of the CPM node to null; to hide the anchored content.The parent of the
        // entity was always null so does not need to be reset.
        try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
            transaction.setAnchorId(mNode, null).setParent(mNode, null).apply();
        }
        super.dispose();
    }
}
