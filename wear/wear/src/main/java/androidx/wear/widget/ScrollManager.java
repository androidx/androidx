/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.wear.widget;

import android.view.MotionEvent;
import android.view.VelocityTracker;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Class adding circular scrolling support to {@link WearableRecyclerView}.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY)
class ScrollManager {
    // One second in milliseconds.
    private static final int ONE_SEC_IN_MS = 1000;
    private static final float VELOCITY_MULTIPLIER = 1.5f;
    private static final float FLING_EDGE_RATIO = 1.5f;

    /**
     * Taps beyond this radius fraction are considered close enough to the bezel to be candidates
     * for circular scrolling.
     */
    private float mMinRadiusFraction = 0.0f;

    private float mMinRadiusFractionSquared = mMinRadiusFraction * mMinRadiusFraction;

    /** How many degrees you have to drag along the bezel to scroll one screen height. */
    private float mScrollDegreesPerScreen = 180;

    private float mScrollRadiansPerScreen = (float) Math.toRadians(mScrollDegreesPerScreen);

    /** Radius of screen in pixels, ignoring insets, if any. */
    private float mScreenRadiusPx;

    private float mScreenRadiusPxSquared;

    /** How many pixels to scroll for each radian of bezel scrolling. */
    private float mScrollPixelsPerRadian;

    /** Whether an {@link MotionEvent#ACTION_DOWN} was received near the bezel. */
    private boolean mDown;

    /**
     * Whether the user tapped near the bezel and dragged approximately tangentially to initiate
     * bezel scrolling.
     */
    private boolean mScrolling;
    /**
     * The angle of the user's finger relative to the center of the screen for the last {@link
     * MotionEvent} during bezel scrolling.
     */
    private float mLastAngleRadians;

    private RecyclerView mRecyclerView;
    VelocityTracker mVelocityTracker;

    /** Should be called after the window is attached to the view. */
    void setRecyclerView(RecyclerView recyclerView, int width, int height) {
        mRecyclerView = recyclerView;
        mScreenRadiusPx = Math.max(width, height) / 2f;
        mScreenRadiusPxSquared = mScreenRadiusPx * mScreenRadiusPx;
        mScrollPixelsPerRadian = height / mScrollRadiansPerScreen;
        mVelocityTracker = VelocityTracker.obtain();
    }

    /** Remove the binding with a {@link RecyclerView} */
    void clearRecyclerView() {
        mRecyclerView = null;
    }

    /**
     * Method dealing with touch events intercepted from the attached {@link RecyclerView}.
     *
     * @param event the intercepted touch event.
     * @return true if the even was handled, false otherwise.
     */
    boolean onTouchEvent(MotionEvent event) {
        float deltaX = event.getRawX() - mScreenRadiusPx;
        float deltaY = event.getRawY() - mScreenRadiusPx;
        float radiusSquared = deltaX * deltaX + deltaY * deltaY;
        final MotionEvent vtev = MotionEvent.obtain(event);
        mVelocityTracker.addMovement(vtev);
        vtev.recycle();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (radiusSquared / mScreenRadiusPxSquared > mMinRadiusFractionSquared) {
                    mDown = true;
                    return true; // Consume the event.
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (mScrolling) {
                    float angleRadians = (float) Math.atan2(deltaY, deltaX);
                    float deltaRadians = angleRadians - mLastAngleRadians;
                    deltaRadians = normalizeAngleRadians(deltaRadians);
                    int scrollPixels = Math.round(deltaRadians * mScrollPixelsPerRadian);
                    if (scrollPixels != 0) {
                        mRecyclerView.scrollBy(0 /* x */, scrollPixels /* y */);
                        // Recompute deltaRadians in terms of rounded scrollPixels.
                        deltaRadians = scrollPixels / mScrollPixelsPerRadian;
                        mLastAngleRadians += deltaRadians;
                        mLastAngleRadians = normalizeAngleRadians(mLastAngleRadians);
                    }
                    // Always consume the event so that we never break the circular scrolling
                    // gesture.
                    return true;
                }

                if (mDown) {
                    float deltaXFromCenter = event.getRawX() - mScreenRadiusPx;
                    float deltaYFromCenter = event.getRawY() - mScreenRadiusPx;
                    float distFromCenter = (float) Math.hypot(deltaXFromCenter, deltaYFromCenter);
                    if (distFromCenter != 0) {
                        deltaXFromCenter /= distFromCenter;
                        deltaYFromCenter /= distFromCenter;

                        mScrolling = true;
                        mRecyclerView.invalidate();
                        mLastAngleRadians = (float) Math.atan2(deltaYFromCenter, deltaXFromCenter);
                        return true; // Consume the event.
                    }
                } else {
                    // Double check we're not missing an event we should really be handling.
                    if (radiusSquared / mScreenRadiusPxSquared > mMinRadiusFractionSquared) {
                        mDown = true;
                        return true; // Consume the event.
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                mDown = false;
                mScrolling = false;
                mVelocityTracker.computeCurrentVelocity(ONE_SEC_IN_MS,
                        mRecyclerView.getMaxFlingVelocity());
                int velocityY = (int) mVelocityTracker.getYVelocity();
                if (event.getX() < FLING_EDGE_RATIO * mScreenRadiusPx) {
                    velocityY = -velocityY;
                }
                mVelocityTracker.clear();
                if (Math.abs(velocityY) > mRecyclerView.getMinFlingVelocity()) {
                    return mRecyclerView.fling(0, (int) (VELOCITY_MULTIPLIER * velocityY));
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                if (mDown) {
                    mDown = false;
                    mScrolling = false;
                    mRecyclerView.invalidate();
                    return true; // Consume the event.
                }
                break;
        }

        return false;
    }

    /**
     * Normalizes an angle to be in the range [-pi, pi] by adding or subtracting 2*pi if necessary.
     *
     * @param angleRadians an angle in radians. Must be no more than 2*pi out of normal range.
     * @return an angle in radians in the range [-pi, pi]
     */
    private static float normalizeAngleRadians(float angleRadians) {
        if (angleRadians < -Math.PI) {
            angleRadians = (float) (angleRadians + Math.PI * 2);
        }
        if (angleRadians > Math.PI) {
            angleRadians = (float) (angleRadians - Math.PI * 2);
        }
        return angleRadians;
    }

    /**
     * Set how many degrees you have to drag along the bezel to scroll one screen height.
     *
     * @param degreesPerScreen desired degrees per screen scroll.
     */
    public void setScrollDegreesPerScreen(float degreesPerScreen) {
        mScrollDegreesPerScreen = degreesPerScreen;
        mScrollRadiansPerScreen = (float) Math.toRadians(mScrollDegreesPerScreen);
    }

    /**
     * Sets the width of a virtual 'bezel' close to the edge of the screen within which taps can be
     * recognized as belonging to a rotary scrolling gesture.
     *
     * @param fraction desired fraction of the width of the screen to be treated as a valid rotary
     *                 scrolling target.
     */
    public void setBezelWidth(float fraction) {
        mMinRadiusFraction = 1 - fraction;
        mMinRadiusFractionSquared = mMinRadiusFraction * mMinRadiusFraction;
    }

    /**
     * Returns how many degrees you have to drag along the bezel to scroll one screen height. See
     * {@link #setScrollDegreesPerScreen(float)} for details.
     */
    public float getScrollDegreesPerScreen() {
        return mScrollDegreesPerScreen;
    }

    /**
     * Returns the current bezel width for circular scrolling. See {@link #setBezelWidth(float)}
     * for details.
     */
    public float getBezelWidth() {
        return 1 - mMinRadiusFraction;
    }
}
