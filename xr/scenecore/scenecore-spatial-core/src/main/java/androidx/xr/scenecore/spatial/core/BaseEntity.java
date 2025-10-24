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

import static java.lang.Math.max;
import static java.lang.Math.min;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.runtime.Component;
import androidx.xr.scenecore.runtime.Entity;
import androidx.xr.scenecore.runtime.Space;
import androidx.xr.scenecore.runtime.SpaceValue;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Implementation of a subset of core Entity functionality. */
abstract class BaseEntity extends BaseScenePose implements Entity {
    private final List<Entity> mChildren = new ArrayList<>();
    private final List<Component> mComponentList = new ArrayList<>();
    private BaseEntity mParent;
    private Pose mPose = new Pose();
    private Vector3 mScale = new Vector3(1.0f, 1.0f, 1.0f);
    private float mAlpha = 1.0f;
    private boolean mHidden = false;
    private ViewGroup mAccessibilityLayout = null;
    private Context mContext;

    BaseEntity(Context context) {
        mContext = context;
    }

    protected void addChildInternal(@NonNull Entity child) {
        if (mChildren.contains(child)) {
            throw new IllegalStateException("Trying to add child who is already a child.");
        }
        mChildren.add(child);
    }

    protected void removeChildInternal(@NonNull Entity child) {
        if (!mChildren.contains(child)) {
            throw new IllegalStateException("Trying to remove child who is not a child.");
        }
        mChildren.remove(child);
    }

    private View getAccessibilityView() {
        Activity activity = getActivity();
        if (activity == null) {
            throw new IllegalStateException(
                    "Activity is not set and unable to create accessibility view");
        }
        if (mAccessibilityLayout == null) {
            ViewGroup mainLayout = (ViewGroup) activity.getWindow().getDecorView();
            mAccessibilityLayout = new FrameLayout(activity);
            mAccessibilityLayout.setLayoutParams(new FrameLayout.LayoutParams(1, 1));
            mainLayout.addView(mAccessibilityLayout);
        }
        // There should be only one child as per this design
        if (mAccessibilityLayout.getChildCount() > 0) {
            return mAccessibilityLayout.getChildAt(0);
        }
        // If the no view exists create one
        View view = new View(activity);
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        mAccessibilityLayout.addView(view);
        return view;
    }

    private void destroyAccessibilityView() {
        Activity activity = getActivity();
        if (activity != null && mAccessibilityLayout != null) {
            ViewGroup mainLayout = (ViewGroup) activity.getWindow().getDecorView();
            mainLayout.removeView(mAccessibilityLayout);
            mAccessibilityLayout = null;
        }
    }

    protected Context getContext() {
        return mContext;
    }

    protected Activity getActivity() {
        if (mContext instanceof Activity) {
            return (Activity) mContext;
        }
        return null;
    }

    @Override
    public void addChild(@NonNull Entity child) {
        child.setParent(this);
    }

    @Override
    public void addChildren(@NonNull List<? extends Entity> mChildren) {
        for (Entity child : mChildren) {
            child.setParent(this);
        }
    }

    @Override
    public @Nullable Entity getParent() {
        return mParent;
    }

    @Override
    public void setParent(@Nullable Entity parent) {
        if ((parent != null) && !(parent instanceof BaseEntity)) {
            throw new IllegalStateException(
                    "Cannot set non-BaseEntity as a parent of a BaseEntity");
        }
        if (mParent != null) {
            mParent.removeChildInternal(this);
        }
        mParent = (BaseEntity) parent;
        if (mParent != null) {
            mParent.addChildInternal(this);
        }
    }

    @Override
    public @NonNull List<Entity> getChildren() {
        return mChildren;
    }

    @Override
    @NonNull
    public CharSequence getContentDescription() {
        if (mAccessibilityLayout != null) {
            View view = getAccessibilityView();
            if (view != null) {
                return view.getContentDescription();
            }
        }
        // content description is not provided
        return "";
    }

    @Override
    public void setContentDescription(@NonNull CharSequence text) {
        if (text.length() == 0) {
            // setContentDescription ignoring empty string.
            if (mAccessibilityLayout != null) {
                destroyAccessibilityView();
            }
            return;
        }
        View view = getAccessibilityView();
        if (view != null) {
            view.setContentDescription(text);
        } else {
            throw new IllegalStateException("setContentDescription is unable to get view.");
        }
    }

    @Override
    public @NonNull Pose getPose(@SpaceValue int relativeTo) {
        return mPose;
    }

    @Override
    public void setPose(@NonNull Pose pose, @SpaceValue int relativeTo) {
        mPose = pose;
    }

    @Override
    public @NonNull Pose getActivitySpacePose() {
        // Any parentless "space" entities (such as the root and anchor entities) are expected to
        // override this method non-recursively so that this error is never thrown.
        if (mParent == null) {
            throw new IllegalStateException("Cannot get pose in ActivitySpace with a null parent");
        }

        return mParent.getActivitySpacePose()
                .compose(
                        new Pose(
                                mPose.getTranslation().scale(mParent.getWorldSpaceScale()),
                                mPose.getRotation()));
    }

    @Override
    @NonNull
    public Pose getGravityAlignedPose(@NonNull Pose pose) {
        if (mParent == null) {
            throw new IllegalStateException("Cannot get gravity aligned pose with a null parent");
        }

        // 1. Determine the world pose of the reference frame for the PARENT space.
        //    This is needed to convert rotations between the local space and world space.
        Pose referenceFrameWorldPose = mParent.getPose(Space.REAL_WORLD);

        // 2. Convert the input pose's local rotation to a world-space rotation.
        Quaternion inputWorldRotation =
                referenceFrameWorldPose.getRotation().times(pose.getRotation());

        // 3. Perform the gravity-alignment calculation in world space. The yaw of the resulting
        // gravity-aligned pose is generally derived from the world-space yaw of the input `pose`.
        // This world-space yaw is a combination of the parent's world rotation and the entity's
        // local rotation (`pose.getRotation()`).
        //    - Get the "forward" vector from the world rotation.
        //    - Project it onto the horizontal (X-Z) ground plane.
        //    - Create a new rotation that looks in that projected direction.
        Vector3 worldForward = inputWorldRotation.times(new Vector3(0f, 0f, 1f));
        Vector3 gravityAlignedForward = new Vector3(worldForward.getX(), 0f, worldForward.getZ());

        Quaternion gravityAlignedWorldRotation;
        if (gravityAlignedForward.getLengthSquared() < 1e-6f) {
            // The original pose was looking straight up or down, so its yaw is undefined.
            // As a fallback, we create a rotation that is upright but uses the yaw of the reference
            // frame (`mParent`) in world space..
            Vector3 referenceForward =
                    referenceFrameWorldPose.getRotation().times(new Vector3(0f, 0f, 1f));
            Vector3 projectedReference =
                    new Vector3(referenceForward.getX(), 0f, referenceForward.getZ());
            if (projectedReference.getLengthSquared() < 1e-6f) {
                gravityAlignedWorldRotation = Quaternion.Identity; // Ultimate fallback
            } else {
                gravityAlignedWorldRotation =
                        Quaternion.fromLookTowards(projectedReference.toNormalized(), Vector3.Up);
            }
        } else {
            gravityAlignedWorldRotation =
                    Quaternion.fromLookTowards(gravityAlignedForward.toNormalized(), Vector3.Up);
        }

        // 4. Convert the new, aligned world rotation back to the PARENT space.
        Quaternion finalLocalRotation =
                referenceFrameWorldPose
                        .getRotation()
                        .getInverse()
                        .times(gravityAlignedWorldRotation);

        // 5. Return a new pose using the original translation rotated by the NEWLY calculated local
        // rotation.
        return new Pose(pose.getTranslation(), finalLocalRotation);
    }

    @Override
    public @NonNull Vector3 getScale(@SpaceValue int relativeTo) {
        switch (relativeTo) {
            case Space.PARENT:
                return mScale;
            case Space.ACTIVITY:
                return getActivitySpaceScale();
            case Space.REAL_WORLD:
                return getWorldSpaceScale();
            default:
                throw new IllegalArgumentException("Unsupported relativeTo value: " + relativeTo);
        }
    }

    @Override
    public void setScale(@NonNull Vector3 scale, @SpaceValue int relativeTo) {
        switch (relativeTo) {
            case Space.PARENT:
                mScale = scale;
                break;
            case Space.ACTIVITY:
                mScale = scale.scale(mParent.getActivitySpaceScale().inverse());
                break;
            case Space.REAL_WORLD:
                mScale = scale.scale(mParent.getWorldSpaceScale().inverse());
                break;
            default:
                throw new IllegalArgumentException("Unsupported relativeTo value: " + relativeTo);
        }
    }

    // Purely sets the value of the scale.
    protected final void setScaleInternal(@NonNull Vector3 scale) {
        mScale = scale;
    }

    @Override
    public float getAlpha(@SpaceValue int relativeTo) {
        switch (relativeTo) {
            case Space.PARENT:
                return mAlpha;
            case Space.ACTIVITY:
            case Space.REAL_WORLD:
                return getActivitySpaceAlpha();
            default:
                throw new IllegalArgumentException("Unsupported relativeTo value: " + relativeTo);
        }
    }

    @Override
    public void setAlpha(float alpha, @SpaceValue int relativeTo) {
        alpha = max(0.0f, min(1.0f, alpha));
        switch (relativeTo) {
            case Space.PARENT:
                mAlpha = alpha;
                break;
            case Space.ACTIVITY:
            case Space.REAL_WORLD:
                mAlpha = alpha / getActivitySpaceAlpha();
                break;
            default:
                throw new IllegalArgumentException("Unsupported relativeTo value: " + relativeTo);
        }
    }

    private float getActivitySpaceAlpha() {
        if (mParent == null) {
            return mAlpha;
        }
        return mParent.getActivitySpaceAlpha() * mAlpha;
    }

    @Override
    public @NonNull Vector3 getWorldSpaceScale() {
        if (mParent == null) {
            throw new IllegalStateException("Cannot get scale in WorldSpace with a null parent");
        }
        return mParent.getWorldSpaceScale().scale(mScale);
    }

    @Override
    public @NonNull Vector3 getActivitySpaceScale() {
        if (mParent == null) {
            throw new IllegalStateException("Cannot get scale in ActivitySpace with a null parent");
        }
        return mParent.getActivitySpaceScale().scale(mScale);
    }

    @Override
    public boolean isHidden(boolean includeParents) {
        if (!includeParents || mParent == null) {
            return mHidden;
        }
        return mHidden || mParent.isHidden(true);
    }

    @Override
    public void setHidden(boolean hidden) {
        mHidden = hidden;
    }

    @Override
    public void dispose() {
        // Create a copy to avoid concurrent modification issues since the children detach
        // themselves from their parents as they are disposed.
        destroyAccessibilityView();
        mContext = null;
    }

    @Override
    public boolean addComponent(@NonNull Component component) {
        if (component.onAttach(this)) {
            mComponentList.add(component);
            return true;
        }
        return false;
    }

    @Override
    public <T extends Component> @NonNull List<T> getComponentsOfType(
            @NonNull Class<? extends T> type) {
        List<T> components = new ArrayList<>();
        for (Component component : mComponentList) {
            if (type.isInstance(component)) {
                components.add(type.cast(component));
            }
        }
        return components;
    }

    @Override
    public @NonNull List<Component> getComponents() {
        return mComponentList;
    }

    @Override
    public void removeComponent(@NonNull Component component) {
        if (mComponentList.contains(component)) {
            component.onDetach(this);
            mComponentList.remove(component);
        }
    }

    @Override
    public void removeAllComponents() {
        for (Component component : mComponentList) {
            component.onDetach(this);
        }
        mComponentList.clear();
    }
}
