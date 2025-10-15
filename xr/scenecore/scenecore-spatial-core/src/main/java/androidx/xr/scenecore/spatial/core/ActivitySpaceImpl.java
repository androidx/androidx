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

import android.app.Activity;

import androidx.concurrent.futures.ResolvableFuture;
import androidx.xr.runtime.math.BoundingBox;
import androidx.xr.runtime.math.Matrix4;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.runtime.ActivitySpace;
import androidx.xr.scenecore.runtime.Dimensions;
import androidx.xr.scenecore.runtime.Entity;
import androidx.xr.scenecore.runtime.HitTestResult;
import androidx.xr.scenecore.runtime.PerceptionSpaceScenePose;
import androidx.xr.scenecore.runtime.ScenePose;
import androidx.xr.scenecore.runtime.Space;
import androidx.xr.scenecore.runtime.SpaceValue;
import androidx.xr.scenecore.runtime.SpatialModeChangeListener;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Box3;
import com.android.extensions.xr.node.Node;
import com.android.extensions.xr.node.NodeTransaction;
import com.android.extensions.xr.node.Vec3;
import com.android.extensions.xr.space.Bounds;
import com.android.extensions.xr.space.SpatialState;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Implementation of SceneCore's ActivitySpace.
 *
 * <p>This Entity represents the origin of the Scene, and is positioned by the system.
 */
@SuppressWarnings({"UnnecessarilyFullyQualified"})
final class ActivitySpaceImpl extends SystemSpaceEntityImpl implements ActivitySpace {
    private final Set<OnBoundsChangedListener> mBoundsListeners =
            Collections.synchronizedSet(new HashSet<>());

    private final Supplier<SpatialState> mSpatialStateProvider;
    private final AtomicReference<Dimensions> mBounds = new AtomicReference<>();
    // The current scene parent aka ActivitySpace origin transform.

    private final boolean mUnscaledGravityAlignedActivitySpace;
    // Spatial mode change handler will be invoked on every update to activity space origin we
    // receive from the node transform listener.
    private SpatialModeChangeListener mSpatialModeChangeListener;
    private final AtomicReference<BoundingBox> mCachedRecommendedContentBox =
            new AtomicReference<>(null);

    @SuppressWarnings("HidingField") // super class AndroidXrEntity has mEntityManager
    private final EntityManager mEntityManager;

    ActivitySpaceImpl(
            Node taskNode,
            Activity activity,
            XrExtensions extensions,
            EntityManager entityManager,
            Supplier<SpatialState> spatialStateProvider,
            boolean unscaledGravityAlignedActivitySpace,
            ScheduledExecutorService executor) {
        super(activity, taskNode, extensions, entityManager, executor);
        mEntityManager = entityManager;
        mSpatialStateProvider = spatialStateProvider;
        mUnscaledGravityAlignedActivitySpace = unscaledGravityAlignedActivitySpace;
    }

    @Override
    public @NonNull Pose getPose(@SpaceValue int relativeTo) {
        switch (relativeTo) {
            case Space.PARENT:
                throw new UnsupportedOperationException(
                        "ActivitySpace is a root space and it does not have a parent.");
            case Space.ACTIVITY:
                return getPoseInActivitySpace();
            case Space.REAL_WORLD:
                return getPoseInPerceptionSpace();
            default:
                throw new IllegalArgumentException("Unsupported relativeTo value: " + relativeTo);
        }
    }

    /** Returns the identity pose since this entity defines the origin of the activity space. */
    @Override
    public @NonNull Pose getPoseInActivitySpace() {
        return new Pose();
    }

    public Pose getPoseInPerceptionSpace() {
        PerceptionSpaceScenePose perceptionSpaceScenePose =
                mEntityManager
                        .getSystemSpaceActivityPoseOfType(PerceptionSpaceScenePose.class)
                        .get(0);
        return transformPoseTo(new Pose(), perceptionSpaceScenePose);
    }

    /** Returns the identity pose since we assume the activity space is the world space root. */
    @Override
    public @NonNull Pose getActivitySpacePose() {

        return new Pose();
    }

    @Override
    public @NonNull Vector3 getActivitySpaceScale() {
        return new Vector3(1.0f, 1.0f, 1.0f);
    }

    @Override
    public void setParent(Entity parent) {
        throw new UnsupportedOperationException("Cannot set 'parent' on an ActivitySpace.");
    }

    @Override
    public void setScale(@NonNull Vector3 scale, @SpaceValue int relativeTo) {
        throw new UnsupportedOperationException("Cannot set 'scale' on an ActivitySpace.");
    }

    @Override
    public @NonNull Vector3 getScale(@SpaceValue int relativeTo) {
        switch (relativeTo) {
            case Space.PARENT:
                throw new UnsupportedOperationException(
                        "ActivitySpace is a root space and it does not have a parent.");
            case Space.ACTIVITY:
                return getActivitySpaceScale();
            case Space.REAL_WORLD:
                return super.getWorldSpaceScale();
            default:
                throw new IllegalArgumentException("Unsupported relativeTo value: " + relativeTo);
        }
    }

    @Override
    public void setPose(@NonNull Pose pose, @SpaceValue int relativeTo) {
        throw new UnsupportedOperationException("Cannot set 'pose' on an ActivitySpace.");
    }

    @SuppressWarnings("ObjectToString")
    @Override
    public void dispose() {
        super.dispose();
    }

    /**
     * Handles the updates to scene core root transform.
     *
     * <pre>
     * Hierarchy:
     * OpenXR Unbounded Reference Space Origin
     *  └── Scene Parent Node (Intermediate system-managed node)
     *      └── Scene Root Node (ActivitySpace Node)
     *
     * Transform Flow:
     *   1. The system updates the transform of the 'Scene Parent Node' when in HOME_SPACE mode.
     *   2. The 'Scene Root Node' becomes a child of 'Scene Parent Node' and inherits its transform
     *      when activity enters FULL_SPACE_MANAGED mode.
     * </pre>
     *
     * <p>By inverting the full inherited rotation and scale, SceneCore effectively re-orients the
     * ActivitySpace to be unscaled and gravity-aligned like its grand parent OpenXR unbounded
     * space.
     *
     * <p>To maintain continuity when entering FSM, SceneCore provides the original rotation and
     * scale of the scene parent transform via the onSpatialModeChanged callback. This ensures FSM
     * continuity when spatial modes change.
     *
     * @param newTransform New scene parent transform relative to OpenXR unbounded reference space.
     */
    public void handleOriginUpdate(Matrix4 newTransform) {
        mOpenXrReferenceSpaceTransform.set(newTransform);
        Vector3 transformScaleAbsolute = new Vector3(1.0f, 1.0f, 1.0f);
        Quaternion activitySpaceRotation = Quaternion.Identity;

        if (mUnscaledGravityAlignedActivitySpace) {
            Vector3 transformScale = newTransform.getScale();
            transformScaleAbsolute =
                    new Vector3(
                            Math.abs(transformScale.getX()),
                            Math.abs(transformScale.getY()),
                            Math.abs(transformScale.getZ()));
            // Get the unscaled rotation of the activity space.
            activitySpaceRotation = newTransform.unscaled().getRotation();
            Quaternion gravityAlignedRotation = activitySpaceRotation.getInverse();
            try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
                transaction
                        .setScale(
                                mNode,
                                1.0f / transformScaleAbsolute.getX(),
                                1.0f / transformScaleAbsolute.getY(),
                                1.0f / transformScaleAbsolute.getZ())
                        .setOrientation(
                                mNode,
                                gravityAlignedRotation.getX(),
                                gravityAlignedRotation.getY(),
                                gravityAlignedRotation.getZ(),
                                gravityAlignedRotation.getW())
                        .apply();
            }
        }

        // The translation is zero - since the activity space origin has been already translated by
        // system. SceneCore is relaying the same rotation and scale that activity space would have
        // inherited if it was in HOME_SPACE mode for continuity in FULL_SPACE_MANAGED mode.
        if (mSpatialModeChangeListener != null) {
            mSpatialModeChangeListener.onSpatialModeChanged(
                    new Pose(Vector3.Zero, activitySpaceRotation), transformScaleAbsolute);
        }
    }

    @Override
    public @NonNull Dimensions getBounds() {
        // The bounds are kept in sync with the Extensions in the onBoundsChangedEvent callback. We
        // only invoke getSpatialState if they've never been set.
        return mBounds.updateAndGet(
                oldBounds -> {
                    if (oldBounds == null) {
                        Bounds bounds = mSpatialStateProvider.get().getBounds();
                        return new Dimensions(
                                bounds.getWidth(), bounds.getHeight(), bounds.getDepth());
                    }
                    return oldBounds;
                });
    }

    @Override
    public void addOnBoundsChangedListener(@NonNull OnBoundsChangedListener listener) {
        mBoundsListeners.add(listener);
    }

    @Override
    public void removeOnBoundsChangedListener(@NonNull OnBoundsChangedListener listener) {
        mBoundsListeners.remove(listener);
    }

    /**
     * This method is called by the Runtime when the bounds of the Activity change. We dispatch the
     * event upwards to the JXRCoreSession via ActivitySpace.
     *
     * <p>Note that this call happens on the Activity's UI thread, so we should be careful not to
     * block it.
     */
    public void onBoundsChanged(Bounds newBounds) {
        Dimensions newDimensions =
                mBounds.updateAndGet(
                        oldBounds ->
                                new Dimensions(
                                        newBounds.getWidth(),
                                        newBounds.getHeight(),
                                        newBounds.getDepth()));
        for (OnBoundsChangedListener listener : mBoundsListeners) {
            listener.onBoundsChanged(newDimensions);
        }
    }

    public void setSpatialModeChangeListener(SpatialModeChangeListener spatialModeChangeListener) {
        mSpatialModeChangeListener = spatialModeChangeListener;
    }

    @SuppressWarnings("RestrictTo")
    static class HitTestResultConsumer
            implements com.android.extensions.xr.function.Consumer<
                    com.android.extensions.xr.space.HitTestResult> {
        ResolvableFuture<HitTestResult> mFuture;

        HitTestResultConsumer(ResolvableFuture<HitTestResult> future) {
            mFuture = future;
        }

        @Override
        @SuppressWarnings("RestrictTo")
        public void accept(com.android.extensions.xr.space.HitTestResult hitTestResultExt) {
            mFuture.set(RuntimeUtils.getHitTestResult(hitTestResultExt));
        }
    }

    @Override
    @SuppressWarnings("RestrictTo")
    public @NonNull ListenableFuture<HitTestResult> hitTest(
            @NonNull Vector3 origin,
            @NonNull Vector3 direction,
            @HitTestFilterValue int hitTestFilter) {
        ResolvableFuture<HitTestResult> hitTestFuture = ResolvableFuture.create();
        HitTestResultConsumer hitTestConsumer = new HitTestResultConsumer(hitTestFuture);

        mExtensions.hitTest(
                getActivity(),
                new Vec3(origin.getX(), origin.getY(), origin.getZ()),
                new Vec3(direction.getX(), direction.getY(), direction.getZ()),
                RuntimeUtils.getHitTestFilter(hitTestFilter),
                mExecutor,
                hitTestConsumer);
        return hitTestFuture;
    }

    @Override
    @SuppressWarnings("RestrictTo")
    public @NonNull ListenableFuture<HitTestResult> hitTestRelativeToActivityPose(
            @NonNull Vector3 origin,
            @NonNull Vector3 direction,
            @HitTestFilterValue int hitTestFilter,
            ScenePose scenePose) {

        // Get the Translation of the origin relative to the ActivitySpace.
        Vector3 originInActivitySpace =
                scenePose.transformPoseTo(new Pose(origin), this).getTranslation();

        // Get the Translation of the direction pose relative to the ActivitySpace.
        Pose directionPoseInActivitySpace = scenePose.transformPoseTo(new Pose(direction), this);

        // Convert the direction pose to a direction vector relative to the ActivitySpace.
        Vector3 directionInActivitySpace =
                directionPoseInActivitySpace
                        .compose(scenePose.getActivitySpacePose().getInverse())
                        .getTranslation();

        ResolvableFuture<HitTestResult> updatedHitTestFuture = ResolvableFuture.create();

        // Perform the hit test then convert the result to be relative to the provided ScenePose.
        ListenableFuture<HitTestResult> hitTestFuture =
                hitTest(originInActivitySpace, directionInActivitySpace, hitTestFilter);
        hitTestFuture.addListener(
                () -> {
                    try {
                        // Convert the hit test result to be relative to the provided ScenePose.
                        HitTestResult result = hitTestFuture.get();
                        // No need to do a conversion if the hit test result is not a hit.
                        if (result.getDistance() == Float.POSITIVE_INFINITY) {
                            updatedHitTestFuture.set(result);
                        }
                        // Update the hit position and surface normal to be relative to the
                        // ScenePose.
                        Vector3 updatedHitPosition =
                                result.getHitPosition() == null
                                        ? null
                                        : transformPoseTo(
                                                        new Pose(result.getHitPosition()),
                                                scenePose)
                                                .getTranslation();
                        Vector3 updatedSurfaceNormal =
                                result.getSurfaceNormal() == null
                                        ? null
                                        : transformPoseTo(
                                                        new Pose(
                                                                new Vector3(
                                                                        result.getSurfaceNormal())),
                                                scenePose)
                                                .compose(
                                                        this.transformPoseTo(
                                                                        Pose.Identity, scenePose)
                                                                .getInverse())
                                                .getTranslation();
                        updatedHitTestFuture.set(
                                new HitTestResult(
                                        updatedHitPosition,
                                        updatedSurfaceNormal,
                                        result.getSurfaceType(),
                                        result.getDistance()));
                    } catch (InterruptedException | ExecutionException e) {
                        // Failed to get hit test result
                        updatedHitTestFuture.setException(e);
                    }
                },
                mExecutor);
        return updatedHitTestFuture;
    }

    /**
     * Return a recommended box for content to be placed in when in Full Space Mode.
     *
     * <p>The box is relative to the ActivitySpace's coordinate system. It is not scaled by the
     * ActivitySpace's transform. The dimensions are always in meters. This provides a
     * device-specific default volume that developers can use to size their content appropriately.
     *
     * @return a [BoundingBox] sized to place content in.
     */
    @Override
    @NonNull
    public BoundingBox getRecommendedContentBoxInFullSpace() {
        return mCachedRecommendedContentBox.updateAndGet(
                currentBox -> {
                    if (currentBox != null) {
                        return currentBox;
                    }

                    Box3 recommendedBox = mExtensions.getRecommendedContentBoxInFullSpace();
                    return BoundingBox.fromMinMax(
                            new Vector3(
                                    recommendedBox.getMin().x,
                                    recommendedBox.getMin().y,
                                    recommendedBox.getMin().z),
                            new Vector3(
                                    recommendedBox.getMax().x,
                                    recommendedBox.getMax().y,
                                    recommendedBox.getMax().z));
                });
    }
}
