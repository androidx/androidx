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

import static java.lang.Math.max;

import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.xr.extensions.Consumer;
import androidx.xr.extensions.XrExtensions;
import androidx.xr.extensions.node.ReformEvent;
import androidx.xr.extensions.node.ReformOptions;
import androidx.xr.extensions.node.Vec3;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.JxrPlatformAdapter.AnchorEntity;
import androidx.xr.scenecore.JxrPlatformAdapter.AnchorPlacement;
import androidx.xr.scenecore.JxrPlatformAdapter.Dimensions;
import androidx.xr.scenecore.JxrPlatformAdapter.Entity;
import androidx.xr.scenecore.JxrPlatformAdapter.MovableComponent;
import androidx.xr.scenecore.JxrPlatformAdapter.MoveEvent;
import androidx.xr.scenecore.JxrPlatformAdapter.MoveEventListener;
import androidx.xr.scenecore.JxrPlatformAdapter.PlaneSemantic;
import androidx.xr.scenecore.JxrPlatformAdapter.PlaneType;
import androidx.xr.scenecore.JxrPlatformAdapter.Ray;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Plane;
import androidx.xr.scenecore.impl.perception.Plane.PlaneData;
import androidx.xr.scenecore.impl.perception.Session;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/** Implementation of MovableComponent. */
@SuppressWarnings("BanConcurrentHashMap")
class MovableComponentImpl implements MovableComponent {
    private static final String TAG = "MovableComponentImpl";
    static final float MIN_PLANE_ANCHOR_DISTANCE = .2f;
    private final boolean mSystemMovable;
    private final boolean mScaleInZ;
    private final boolean mShouldDisposeParentAnchor;
    private final PerceptionLibrary mPerceptionLibrary;
    private final XrExtensions mExtensions;
    private final ActivitySpaceImpl mActivitySpaceImpl;
    private final AndroidXrEntity mActivitySpaceEntity;
    private final PerceptionSpaceActivityPoseImpl mPerceptionSpaceActivityPose;
    private final EntityManager mEntityManager;
    private final PanelShadowRenderer mPanelShadowRenderer;
    private final ScheduledExecutorService mRuntimeExecutor;
    private final ConcurrentHashMap<MoveEventListener, Executor> mMoveEventListenersMap =
            new ConcurrentHashMap<>();
    private final Map<PlaneType, Map<PlaneSemantic, AnchorPlacementImpl>> mAnchorableFilters =
            new EnumMap<>(PlaneType.class);
    // Visible for testing.
    Consumer<ReformEvent> mReformEventConsumer;
    private Entity mEntity;
    private Entity mInitialParent;
    private Pose mLastPose = new Pose();
    private Vector3 mLastScale = new Vector3(1f, 1f, 1f);
    private Dimensions mCurrentSize;
    private boolean mUserAnchorable = false;
    private AnchorEntity mCreatedAnchorEntity;
    private AnchorPlacementImpl mCreatedAnchorPlacement;
    @ScaleWithDistanceMode private int mScaleWithDistanceMode = ScaleWithDistanceMode.DEFAULT;

    MovableComponentImpl(
            boolean systemMovable,
            boolean scaleInZ,
            Set<AnchorPlacement> anchorPlacement,
            boolean shouldDisposeParentAnchor,
            PerceptionLibrary perceptionLibrary,
            XrExtensions extensions,
            ActivitySpaceImpl activitySpaceImpl,
            AndroidXrEntity activitySpaceEntity,
            PerceptionSpaceActivityPoseImpl perceptionSpaceActivityPose,
            EntityManager entityManager,
            PanelShadowRenderer panelShadowRenderer,
            ScheduledExecutorService runtimeExecutor) {
        mSystemMovable = systemMovable;
        mScaleInZ = scaleInZ;
        mShouldDisposeParentAnchor = shouldDisposeParentAnchor;
        mPerceptionLibrary = perceptionLibrary;
        mExtensions = extensions;
        mActivitySpaceImpl = activitySpaceImpl;
        mActivitySpaceEntity = activitySpaceEntity;
        mPerceptionSpaceActivityPose = perceptionSpaceActivityPose;
        mEntityManager = entityManager;
        mPanelShadowRenderer = panelShadowRenderer;
        mRuntimeExecutor = runtimeExecutor;
        setUpAnchorPlacement(anchorPlacement);
    }

    @Override
    public boolean onAttach(Entity entity) {
        if (mEntity != null) {
            Log.e(TAG, "Already attached to entity " + mEntity);
            return false;
        }
        mEntity = entity;
        ReformOptions reformOptions = ((AndroidXrEntity) entity).getReformOptions();
        // The math for anchoring uses the pose relative to the activity space so we should not set
        // the reform options to be relative to the parent if the entity is anchorable.
        int reformFlags = mUserAnchorable ? 0 : ReformOptions.FLAG_POSE_RELATIVE_TO_PARENT;
        reformFlags =
                (mSystemMovable && !mUserAnchorable)
                        ? reformFlags | ReformOptions.FLAG_ALLOW_SYSTEM_MOVEMENT
                        : reformFlags;
        reformFlags =
                mScaleInZ ? reformFlags | ReformOptions.FLAG_SCALE_WITH_DISTANCE : reformFlags;
        reformOptions.setFlags(reformFlags);
        reformOptions.setEnabledReform(reformOptions.getEnabledReform() | ReformOptions.ALLOW_MOVE);
        reformOptions.setScaleWithDistanceMode(
                translateScaleWithDistanceMode(mScaleWithDistanceMode));

        // TODO: b/348037292 - Remove this special case for PanelEntityImpl.
        if (entity instanceof PanelEntityImpl && mCurrentSize == null) {
            mCurrentSize = ((PanelEntityImpl) entity).getSize();
        }
        if (mCurrentSize != null) {
            reformOptions.setCurrentSize(
                    new Vec3(mCurrentSize.width, mCurrentSize.height, mCurrentSize.depth));
        }
        if (mUserAnchorable && mSystemMovable && mReformEventConsumer == null) {
            mReformEventConsumer =
                    reformEvent -> {
                        Pair<Pose, Entity> unused = getUpdatedReformEventPoseAndParent(reformEvent);
                    };
        }
        mLastPose = entity.getPose();
        mLastScale = entity.getScale();
        ((AndroidXrEntity) entity).updateReformOptions();
        if (mReformEventConsumer != null) {
            ((AndroidXrEntity) entity)
                    .addReformEventConsumer(mReformEventConsumer, mRuntimeExecutor);
        }
        return true;
    }

    @Override
    public void onDetach(Entity entity) {
        ReformOptions reformOptions = ((AndroidXrEntity) entity).getReformOptions();
        reformOptions.setEnabledReform(
                reformOptions.getEnabledReform() & ~ReformOptions.ALLOW_MOVE);
        // Clear any flags that were set by this component.
        int reformFlags = reformOptions.getFlags();
        reformFlags =
                mSystemMovable
                        ? reformFlags & ~ReformOptions.FLAG_ALLOW_SYSTEM_MOVEMENT
                        : reformFlags;
        reformFlags =
                mScaleInZ ? reformFlags & ~ReformOptions.FLAG_SCALE_WITH_DISTANCE : reformFlags;
        reformOptions.setFlags(reformFlags);
        ((AndroidXrEntity) entity).updateReformOptions();
        if (mReformEventConsumer != null) {
            ((AndroidXrEntity) entity).removeReformEventConsumer(mReformEventConsumer);
            mReformEventConsumer = null;
        }
        mEntity = null;
    }

    @Override
    public void setSize(Dimensions dimensions) {
        mCurrentSize = dimensions;
        if (mEntity == null) {
            Log.i(TAG, "setSize called before component is attached to an Entity.");
            return;
        }
        ReformOptions reformOptions = ((AndroidXrEntity) mEntity).getReformOptions();
        reformOptions.setCurrentSize(
                new Vec3(dimensions.width, dimensions.height, dimensions.depth));
        ((AndroidXrEntity) mEntity).updateReformOptions();
    }

    @Override
    @ScaleWithDistanceMode
    public int getScaleWithDistanceMode() {
        return mScaleWithDistanceMode;
    }

    @Override
    public void setScaleWithDistanceMode(@ScaleWithDistanceMode int scaleWithDistanceMode) {
        mScaleWithDistanceMode = scaleWithDistanceMode;
        if (mEntity == null) {
            Log.w(
                    TAG,
                    "setScaleWithDistanceMode called before component is attached to an Entity.");
            return;
        }
        ReformOptions reformOptions = ((AndroidXrEntity) mEntity).getReformOptions();
        reformOptions.setScaleWithDistanceMode(
                translateScaleWithDistanceMode(scaleWithDistanceMode));
        ((AndroidXrEntity) mEntity).updateReformOptions();
    }

    @Override
    public void addMoveEventListener(Executor executor, MoveEventListener moveEventListener) {
        if (mReformEventConsumer != null) {
            ((AndroidXrEntity) mEntity).removeReformEventConsumer(mReformEventConsumer);
        }
        mReformEventConsumer =
                reformEvent -> {
                    if (reformEvent.getType() != ReformEvent.REFORM_TYPE_MOVE) {
                        return;
                    }
                    if (reformEvent.getState() == ReformEvent.REFORM_STATE_START) {
                        mInitialParent = mEntity.getParent();
                    }
                    Pose newPose;
                    Entity updatedParent = null;
                    if (mUserAnchorable) {
                        Pair<Pose, Entity> updatedPoseParentPair =
                                getUpdatedReformEventPoseAndParent(reformEvent);
                        newPose = updatedPoseParentPair.first;
                        updatedParent = updatedPoseParentPair.second;
                    } else {
                        newPose =
                                RuntimeUtils.getPose(
                                        reformEvent.getProposedPosition(),
                                        reformEvent.getProposedOrientation());
                    }
                    Vector3 newScale = RuntimeUtils.getVector3(reformEvent.getProposedScale());
                    Entity disposeEntity = null;

                    Entity parent = updatedParent;
                    mMoveEventListenersMap.forEach(
                            (listener, listenerExecutor) ->
                                    executor.execute(
                                            () ->
                                                    listener.onMoveEvent(
                                                            new MoveEvent(
                                                                    reformEvent.getState(),
                                                                    new Ray(
                                                                            RuntimeUtils.getVector3(
                                                                                    reformEvent
                                                                                            .getInitialRayOrigin()),
                                                                            RuntimeUtils.getVector3(
                                                                                    reformEvent
                                                                                            .getInitialRayDirection())),
                                                                    new Ray(
                                                                            RuntimeUtils.getVector3(
                                                                                    reformEvent
                                                                                            .getCurrentRayOrigin()),
                                                                            RuntimeUtils.getVector3(
                                                                                    reformEvent
                                                                                            .getCurrentRayDirection())),
                                                                    mLastPose,
                                                                    newPose,
                                                                    mLastScale,
                                                                    newScale,
                                                                    mInitialParent,
                                                                    parent,
                                                                    disposeEntity))));
                    mLastPose = newPose;
                    mLastScale = newScale;
                };
        mMoveEventListenersMap.put(moveEventListener, executor);
        if (mEntity == null) {
            Log.i(TAG, "setMoveEventListener called before component is attached to an Entity.");
            return;
        }
        ((AndroidXrEntity) mEntity).addReformEventConsumer(mReformEventConsumer, executor);
    }

    private void setUpAnchorPlacement(Set<AnchorPlacement> anchorPlacement) {

        for (AnchorPlacement placement : anchorPlacement) {
            if (!(placement instanceof AnchorPlacementImpl)) {
                continue;
            }
            AnchorPlacementImpl placementImpl = (AnchorPlacementImpl) placement;
            Map<PlaneSemantic, AnchorPlacementImpl> anchorablePlaneSemantic =
                    new EnumMap<>(PlaneSemantic.class);
            for (PlaneSemantic planeSemantic : placementImpl.mPlaneSemanticFilter) {
                anchorablePlaneSemantic.put(planeSemantic, placementImpl);
            }
            for (PlaneType planeType : placementImpl.mPlaneTypeFilter) {
                mAnchorableFilters.put(planeType, anchorablePlaneSemantic);
            }
        }
        if (!mAnchorableFilters.isEmpty()) {
            mUserAnchorable = true;
        }
    }

    @Override
    public void removeMoveEventListener(MoveEventListener moveEventListener) {
        mMoveEventListenersMap.remove(moveEventListener);
    }

    private Pair<Pose, Entity> getUpdatedReformEventPoseAndParent(ReformEvent reformEvent) {
        if (reformEvent.getState() == ReformEvent.REFORM_STATE_END && shouldRenderPlaneShadow()) {
            mPanelShadowRenderer.destroy();
        }
        Pose proposedPose =
                RuntimeUtils.getPose(
                        reformEvent.getProposedPosition(), reformEvent.getProposedOrientation());
        Pair<Pose, Entity> updatedEntity = updatePoseWithPlanes(proposedPose, reformEvent);
        if (mSystemMovable) {
            mEntity.setPose(updatedEntity.first);
        }
        return updatedEntity;
    }

    private Pair<Pose, Entity> updatePoseWithPlanes(Pose proposedPose, ReformEvent reformEvent) {
        Session session = mPerceptionLibrary.getSession();
        if (session == null) {
            Log.w(TAG, "Unable to load perception session, cannot anchor object to a plane.");
            return Pair.create(proposedPose, null);
        }
        List<Plane> planes = session.getAllPlanes();
        if (planes.isEmpty()) {
            return Pair.create(proposedPose, null);
        }

        // The proposed pose is relative to the activity space, it needs to be updated to be in the
        // perception reference space to be compared against the planes..
        Pose updatedPoseInOpenXr =
                mActivitySpaceImpl.transformPoseTo(proposedPose, mPerceptionSpaceActivityPose);

        // Create variables to store the plane in case we need to anchor to it later.
        Plane anchorablePlane = null;
        PlaneData anchorablePlaneData = null;
        AnchorPlacementImpl anchorPlacement = null;
        Long dataTimeNs = SystemClock.uptimeMillis() * 1000000;

        // Update the pose based on all the known planes.
        for (Plane plane : planes) {

            PlaneData planeData = plane.getData(dataTimeNs);
            if (planeData == null) {
                continue;
            }
            Pose planePoseUpdate = updatePoseForPlane(planeData, updatedPoseInOpenXr);
            if (planePoseUpdate == null) {
                continue;
            }
            AnchorPlacementImpl newAnchorPlacement = getAnchorPlacementIfAnchorable(planeData);
            if (newAnchorPlacement != null) {
                anchorablePlane = plane;
                anchorablePlaneData = planeData;
                anchorPlacement = newAnchorPlacement;
            }
            updatedPoseInOpenXr = planePoseUpdate;
        }

        if (anchorablePlane != null) {
            // If the reform state is end, we should try to anchor the entity to the plane.
            if (reformEvent.getState() == ReformEvent.REFORM_STATE_END) {
                Pair<Pose, AnchorEntity> resultPair =
                        anchorEntityToPlane(
                                updatedPoseInOpenXr,
                                anchorablePlane,
                                anchorablePlaneData,
                                anchorPlacement,
                                dataTimeNs);
                if (resultPair.second != null) {
                    return Pair.create(resultPair.first, (Entity) resultPair.second);
                }
            } else {
                // Otherwise, we should try to render the plane shadow onto the plane.
                tryRenderPlaneShadow(
                        updatedPoseInOpenXr,
                        RuntimeUtils.fromPerceptionPose(anchorablePlaneData.centerPose));
            }
        } else if (shouldRenderPlaneShadow()) {
            // If there is nothing to anchor to, hide the plane shadow.
            mPanelShadowRenderer.hidePlane();
        }

        // If the entity was anchored and the reform is complete, update the entity to be in the
        // activity space and remove the previously created anchor data. If
        // shouldDisposeParentAnchor is
        // true dispose the previously created anchor entity.
        if (mCreatedAnchorEntity != null
                && mEntity.getParent() == mCreatedAnchorEntity
                && reformEvent.getState() == ReformEvent.REFORM_STATE_END) {

            // TODO: b/367754233 - Revisit if this needs to use ActivitySpaceScale or
            // WorldSpaceScale.
            mEntity.setScale(
                    mEntity.getWorldSpaceScale().div(mActivitySpaceImpl.getWorldSpaceScale()));
            mEntity.setParent(mActivitySpaceImpl);
            checkAndDisposeAnchorEntity();
            mCreatedAnchorEntity = null;
            mCreatedAnchorPlacement = null;

            // Move the updated pose back to the activity space.
            Pose updatedPoseInActivitySpace =
                    mPerceptionSpaceActivityPose.transformPoseTo(
                            updatedPoseInOpenXr, mActivitySpaceImpl);
            return Pair.create(updatedPoseInActivitySpace, mActivitySpaceImpl);
        }

        // If the entity has a parent, transform the pose to the parent's space.
        Entity parent = mEntity.getParent();
        if (parent == null || parent == mActivitySpaceImpl) {
            return Pair.create(
                    mPerceptionSpaceActivityPose.transformPoseTo(
                            updatedPoseInOpenXr, mActivitySpaceImpl),
                    null);
        }

        return Pair.create(
                mPerceptionSpaceActivityPose.transformPoseTo(updatedPoseInOpenXr, parent), null);
    }

    // Gets the anchor placement settings for the given plane data, if it is null the entity should
    // not be anchored to this plane.
    @Nullable
    private AnchorPlacementImpl getAnchorPlacementIfAnchorable(PlaneData planeData) {
        if (!mUserAnchorable || !mSystemMovable) {
            return null;
        }
        Map<PlaneSemantic, AnchorPlacementImpl> anchorablePlaneSemantic =
                mAnchorableFilters.get(RuntimeUtils.getPlaneType(planeData.type));
        if (anchorablePlaneSemantic != null) {
            if (anchorablePlaneSemantic.containsKey(
                    RuntimeUtils.getPlaneSemantic(planeData.label))) {
                return anchorablePlaneSemantic.get(RuntimeUtils.getPlaneSemantic(planeData.label));
            } else if (anchorablePlaneSemantic.containsKey(PlaneSemantic.ANY)) {
                return anchorablePlaneSemantic.get(PlaneSemantic.ANY);
            }
        }
        anchorablePlaneSemantic = mAnchorableFilters.get(PlaneType.ANY);
        if (anchorablePlaneSemantic != null) {
            if (anchorablePlaneSemantic.containsKey(
                    RuntimeUtils.getPlaneSemantic(planeData.label))) {
                return anchorablePlaneSemantic.get(RuntimeUtils.getPlaneSemantic(planeData.label));
            } else if (anchorablePlaneSemantic.containsKey(PlaneSemantic.ANY)) {
                return anchorablePlaneSemantic.get(PlaneSemantic.ANY);
            }
        }
        return null;
    }

    private Pair<Pose, AnchorEntity> anchorEntityToPlane(
            Pose updatedPose,
            Plane plane,
            PlaneData anchorablePlaneData,
            AnchorPlacementImpl anchorPlacement,
            Long dataTimeNs) {
        AnchorEntityImpl anchorEntity =
                AnchorEntityImpl.createAnchorFromPlane(
                        mExtensions.createNode(),
                        plane,
                        new Pose(),
                        dataTimeNs,
                        mActivitySpaceImpl,
                        mActivitySpaceEntity,
                        mExtensions,
                        mEntityManager,
                        mRuntimeExecutor,
                        mPerceptionLibrary);
        if (anchorEntity.getState() != AnchorEntityImpl.State.ANCHORED) {
            return Pair.create(updatedPose, null);
        }

        // TODO: b/367754233: Fix the flashing when parented to a new anchor.
        // Check the scale of the entity before the move so we can rescale when we move it to the
        // AnchorEntity. Note the AnchorEntity has a scale of 1 so we don't need to also scale by
        // the
        // anchor entity's scale.
        Vector3 entityScale = mEntity.getWorldSpaceScale();
        mEntity.setScale(entityScale);
        Quaternion planeRotation =
                RuntimeUtils.fromPerceptionPose(anchorablePlaneData.centerPose).getRotation();
        Pose rotatedPose =
                new Pose(
                        updatedPose.getTranslation(),
                        PlaneUtils.rotateEntityToPlane(updatedPose.getRotation(), planeRotation));

        Pose planeCenterPose = RuntimeUtils.fromPerceptionPose(anchorablePlaneData.centerPose);
        Pose poseToAnchor = planeCenterPose.getInverse().compose(rotatedPose);
        poseToAnchor =
                new Pose(
                        new Vector3(
                                poseToAnchor.getTranslation().getX(),
                                0f,
                                poseToAnchor.getTranslation().getZ()),
                        poseToAnchor.getRotation());
        mEntity.setParent(anchorEntity);
        // If the anchor placement settings specify that the anchor should be disposed, dispose of
        // the
        // previously created anchor entity.
        checkAndDisposeAnchorEntity();
        mCreatedAnchorEntity = anchorEntity;
        mCreatedAnchorPlacement = anchorPlacement;
        return Pair.create(poseToAnchor, anchorEntity);
    }

    @Nullable
    private Pose updatePoseForPlane(PlaneData planeData, Pose proposedPoseInOpenXr) {
        // Get the pose as related to the center of the plane.

        Pose centerPose = RuntimeUtils.fromPerceptionPose(planeData.centerPose);
        Pose centerPoseToProposedPose = centerPose.getInverse().compose(proposedPoseInOpenXr);

        // The extents of the plane are in the X and Z directions so we can use those to determine
        // if
        // the point is outside the plane.
        if (centerPoseToProposedPose.getTranslation().getX() < -planeData.extentWidth
                || centerPoseToProposedPose.getTranslation().getX() > planeData.extentWidth
                || centerPoseToProposedPose.getTranslation().getZ() < -planeData.extentHeight
                || centerPoseToProposedPose.getTranslation().getZ() > planeData.extentHeight) {
            return null;
        }

        // The distance between the point and the plane. If it is less than the minimum allowed
        // distance, move it to the plane. We only need to take the y-value because the y-value is
        // normal to the plane. We established above that the point is within the extents of the
        // plane.
        float distance = centerPoseToProposedPose.getTranslation().getY();
        if (distance >= MIN_PLANE_ANCHOR_DISTANCE) {
            return null;
        }
        centerPoseToProposedPose =
                new Pose(
                        new Vector3(
                                centerPoseToProposedPose.getTranslation().getX(),
                                max(0f, distance),
                                centerPoseToProposedPose.getTranslation().getZ()),
                        centerPoseToProposedPose.getRotation());
        return centerPose.compose(centerPoseToProposedPose);
    }

    private void tryRenderPlaneShadow(Pose proposedPose, Pose planePose) {
        if (!shouldRenderPlaneShadow()) {
            return;
        }
        mPanelShadowRenderer.updatePanelPose(proposedPose, planePose, (PanelEntityImpl) mEntity);
    }

    private boolean shouldRenderPlaneShadow() {
        return mEntity instanceof PanelEntityImpl && mSystemMovable;
    }

    // Checks if there is a created anchor entity and if it should be disposed. If so, disposes of
    // the
    // anchor entity. Resets the createdAnchorEntity and createdAnchorPlacement to null.
    private void checkAndDisposeAnchorEntity() {
        if (mCreatedAnchorEntity != null
                && mCreatedAnchorEntity.getChildren().isEmpty()
                && mCreatedAnchorPlacement != null
                && mShouldDisposeParentAnchor) {
            mCreatedAnchorEntity.dispose();
        }
    }

    private static @ReformOptions.ScaleWithDistanceMode int translateScaleWithDistanceMode(
            @ScaleWithDistanceMode int scale) {
        switch (scale) {
            case ScaleWithDistanceMode.DMM:
                return ReformOptions.SCALE_WITH_DISTANCE_MODE_DMM;
            default:
                return ReformOptions.SCALE_WITH_DISTANCE_MODE_DEFAULT;
        }
    }
}
