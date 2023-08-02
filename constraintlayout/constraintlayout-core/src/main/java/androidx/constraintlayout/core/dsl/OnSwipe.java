/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.constraintlayout.core.dsl;

/**
 * Create automatic swipe handling object
 */
public class OnSwipe {

    private Drag mDragDirection = null;
    private Side mTouchAnchorSide = null;

    private String mTouchAnchorId = null;
    private String mLimitBoundsTo = null;
    private TouchUp mOnTouchUp = null;
    private String mRotationCenterId = null;
    private float mMaxVelocity = Float.NaN;
    private float mMaxAcceleration = Float.NaN;
    private float mDragScale = Float.NaN;

    private float mDragThreshold = Float.NaN;
    private float mSpringDamping = Float.NaN;
    private float mSpringMass = Float.NaN;
    private float mSpringStiffness = Float.NaN;
    private float mSpringStopThreshold = Float.NaN;
    private Boundary mSpringBoundary = null;
    private Mode mAutoCompleteMode = null;

    public OnSwipe() {
    }

    public OnSwipe(String anchor, Side side, Drag dragDirection) {
        mTouchAnchorId = anchor;
        mTouchAnchorSide = side;
        mDragDirection = dragDirection;
    }

    public enum Mode {
        VELOCITY,
        SPRING
    }

    public enum Boundary {
        OVERSHOOT,
        BOUNCE_START,
        BOUNCE_END,
        BOUNCE_BOTH,
    }


    public enum Drag {
        UP,
        DOWN,
        LEFT,
        RIGHT,
        START,
        END,
        CLOCKWISE,
        ANTICLOCKWISE
    }

    public static final int FLAG_DISABLE_POST_SCROLL = 1;
    public static final int FLAG_DISABLE_SCROLL = 2;

    public enum Side {
        TOP,
        LEFT,
        RIGHT,
        BOTTOM,
        MIDDLE,
        START,
        END,
    }

    public enum TouchUp {
        AUTOCOMPLETE,
        TO_START,
        NEVER_COMPLETE_END,
        TO_END,
        STOP,
        DECELERATE,
        DECELERATE_COMPLETE,
        NEVER_COMPLETE_START
    }

    /**
     * The id of the view who's movement is matched to your drag
     * If not specified it will map to a linear movement across the width of the motionLayout
     */
    public OnSwipe setTouchAnchorId(String id) {
        mTouchAnchorId = id;
        return this;
    }

    public String getTouchAnchorId() {
        return mTouchAnchorId;
    }

    /**
     * This side of the view that matches the drag movement.
     * Only meaning full if the object changes size during the movement.
     * (rotation is not considered)
     */
    public OnSwipe setTouchAnchorSide(Side side) {
        mTouchAnchorSide = side;
        return this;
    }

    public Side getTouchAnchorSide() {
        return mTouchAnchorSide;
    }

    /**
     * The direction of the drag.
     */
    public OnSwipe setDragDirection(Drag dragDirection) {
        mDragDirection = dragDirection;
        return this;
    }

    public Drag getDragDirection() {
        return mDragDirection;
    }

    /**
     * The maximum velocity (Change in progress per second) animation can achieve
     */
    public OnSwipe setMaxVelocity(int maxVelocity) {
        mMaxVelocity = maxVelocity;
        return this;
    }

    public float getMaxVelocity() {
        return mMaxVelocity;
    }

    /**
     * The maximum acceleration and deceleration of the animation
     * (Change in Change in progress per second)
     * Faster makes the object seem lighter and quicker
     */
    public OnSwipe setMaxAcceleration(int maxAcceleration) {
        mMaxAcceleration = maxAcceleration;
        return this;
    }

    public float getMaxAcceleration() {
        return mMaxAcceleration;
    }


    /**
     * Normally 1 this can be tweaked to make the acceleration faster
     */
    public OnSwipe setDragScale(int dragScale) {
        mDragScale = dragScale;
        return this;
    }

    public float getDragScale() {
        return mDragScale;
    }

    /**
     * This sets the threshold before the animation is kicked off.
     * It is important when have multi state animations the have some play before the
     * System decides which animation to jump on.
     */
    public OnSwipe setDragThreshold(int dragThreshold) {
        mDragThreshold = dragThreshold;
        return this;
    }

    public float getDragThreshold() {
        return mDragThreshold;
    }


    /**
     * Configures what happens when the user releases on mouse up.
     * One of: ON_UP_AUTOCOMPLETE, ON_UP_AUTOCOMPLETE_TO_START, ON_UP_AUTOCOMPLETE_TO_END,
     * ON_UP_STOP, ON_UP_DECELERATE, ON_UP_DECELERATE_AND_COMPLETE
     *
     * @param mode default = ON_UP_AUTOCOMPLETE
     */
    public OnSwipe setOnTouchUp(TouchUp mode) {
        mOnTouchUp = mode;
        return this;
    }

    public TouchUp getOnTouchUp() {
        return mOnTouchUp;
    }

    /**
     * Only allow touch actions to be initiated within this region
     */
    public OnSwipe setLimitBoundsTo(String id) {
        mLimitBoundsTo = id;
        return this;
    }

    public String getLimitBoundsTo() {
        return mLimitBoundsTo;
    }

    /**
     * The view to center the rotation about
     *
     * @return this
     */
    public OnSwipe setRotateCenter(String rotationCenterId) {
        mRotationCenterId = rotationCenterId;
        return this;
    }

    public String getRotationCenterId() {
        return mRotationCenterId;
    }


    public float getSpringDamping() {
        return mSpringDamping;
    }

    /**
     * Set the damping of the spring if using spring.
     * c in "a = (-k*x-c*v)/m" equation for the acceleration of a spring
     *
     * @return this
     */
    public OnSwipe setSpringDamping(float springDamping) {
        mSpringDamping = springDamping;
        return this;
    }

    /**
     * Get the mass of the spring.
     * the m in "a = (-k*x-c*v)/m" equation for the acceleration of a spring
     */
    public float getSpringMass() {
        return mSpringMass;
    }

    /**
     * Set the Mass of the spring if using spring.
     * m in "a = (-k*x-c*v)/m" equation for the acceleration of a spring
     *
     * @return this
     */
    public OnSwipe setSpringMass(float springMass) {
        mSpringMass = springMass;
        return this;
    }

    /**
     * get the stiffness of the spring
     *
     * @return NaN if not set
     */
    public float getSpringStiffness() {
        return mSpringStiffness;
    }

    /**
     * set the stiffness of the spring if using spring.
     * If this is set the swipe will use a spring return system.
     * If set to NaN it will revert to the norm system.
     * K in "a = (-k*x-c*v)/m" equation for the acceleration of a spring
     */
    public OnSwipe setSpringStiffness(float springStiffness) {
        mSpringStiffness = springStiffness;
        return this;
    }

    /**
     * The threshold for spring motion to stop.
     */
    public float getSpringStopThreshold() {
        return mSpringStopThreshold;
    }

    /**
     * set the threshold for spring motion to stop.
     * This is in change in progress / second
     * If the spring will never go above that threshold again it will stop.
     *
     * @param springStopThreshold when to stop.
     */
    public OnSwipe setSpringStopThreshold(float springStopThreshold) {
        mSpringStopThreshold = springStopThreshold;
        return this;
    }

    /**
     * The behaviour at the boundaries 0 and 1
     */
    public Boundary getSpringBoundary() {
        return mSpringBoundary;
    }

    /**
     * The behaviour at the boundaries 0 and 1.
     *
     * @param springBoundary behaviour at the boundaries
     */
    public OnSwipe setSpringBoundary(Boundary springBoundary) {
        mSpringBoundary = springBoundary;
        return this;
    }

    public Mode getAutoCompleteMode() {
        return mAutoCompleteMode;
    }


    /**
     * sets the behaviour at the boundaries 0 and 1
     * COMPLETE_MODE_CONTINUOUS_VELOCITY = 0;
     * COMPLETE_MODE_SPRING = 1;
     */
    public void setAutoCompleteMode(Mode autoCompleteMode) {
        mAutoCompleteMode = autoCompleteMode;
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append("OnSwipe:{\n");
        if (mTouchAnchorId != null) {
            ret.append("anchor:'").append(mTouchAnchorId).append("',\n");
        }
        if (mDragDirection != null) {
            ret.append("direction:'").append(mDragDirection.toString().toLowerCase()).append(
                    "',\n");
        }
        if (mTouchAnchorSide != null) {
            ret.append("side:'").append(mTouchAnchorSide.toString().toLowerCase()).append("',\n");
        }
        if (!Float.isNaN(mDragScale)) {
            ret.append("scale:'").append(mDragScale).append("',\n");
        }
        if (!Float.isNaN(mDragThreshold)) {
            ret.append("threshold:'").append(mDragThreshold).append("',\n");
        }
        if (!Float.isNaN(mMaxVelocity)) {
            ret.append("maxVelocity:'").append(mMaxVelocity).append("',\n");
        }
        if (!Float.isNaN(mMaxAcceleration)) {
            ret.append("maxAccel:'").append(mMaxAcceleration).append("',\n");
        }
        if (mLimitBoundsTo != null) {
            ret.append("limitBounds:'").append(mLimitBoundsTo).append("',\n");
        }
        if (mAutoCompleteMode != null) {
            ret.append("mode:'").append(mAutoCompleteMode.toString().toLowerCase()).append("',\n");
        }
        if (mOnTouchUp != null) {
            ret.append("touchUp:'").append(mOnTouchUp.toString().toLowerCase()).append("',\n");
        }
        if (!Float.isNaN(mSpringMass)) {
            ret.append("springMass:'").append(mSpringMass).append("',\n");
        }

        if (!Float.isNaN(mSpringStiffness)) {
            ret.append("springStiffness:'").append(mSpringStiffness).append("',\n");
        }
        if (!Float.isNaN(mSpringDamping)) {
            ret.append("springDamping:'").append(mSpringDamping).append("',\n");
        }
        if (!Float.isNaN(mSpringStopThreshold)) {
            ret.append("stopThreshold:'").append(mSpringStopThreshold).append("',\n");
        }
        if (mSpringBoundary != null) {
            ret.append("springBoundary:'").append(mSpringBoundary).append("',\n");
        }
        if (mRotationCenterId != null) {
            ret.append("around:'").append(mRotationCenterId).append("',\n");
        }

        ret.append("},\n");

        return ret.toString();
    }
}
