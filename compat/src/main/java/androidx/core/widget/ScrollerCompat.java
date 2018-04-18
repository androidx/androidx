/*
 * Copyright (C) 2012 The Android Open Source Project
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

package androidx.core.widget;

import android.content.Context;
import android.view.animation.Interpolator;
import android.widget.OverScroller;

/**
 * Provides access to new {@link android.widget.Scroller Scroller} APIs when available.
 *
 * <p>This class provides a platform version-independent mechanism for obeying the
 * current device's preferred scroll physics and fling behavior. It offers a subset of
 * the APIs from Scroller or OverScroller.</p>
 *
 * @deprecated Use {@link OverScroller} directly.
 */
@Deprecated
public final class ScrollerCompat {
    OverScroller mScroller;

    /**
     * @deprecated Use {@link OverScroller} constructor directly.
     */
    @Deprecated
    public static ScrollerCompat create(Context context) {
        return create(context, null);
    }

    /**
     * @deprecated Use {@link OverScroller} constructor directly.
     */
    @Deprecated
    public static ScrollerCompat create(Context context, Interpolator interpolator) {
        return new ScrollerCompat(context, interpolator);
    }

    /**
     * Package protected constructor that allows to specify if API version is newer than ICS.
     * It is useful for unit testing.
     */
    ScrollerCompat(Context context, Interpolator interpolator) {
        mScroller = interpolator != null ?
                new OverScroller(context, interpolator) : new OverScroller(context);
    }

    /**
     * Returns whether the scroller has finished scrolling.
     *
     * @return True if the scroller has finished scrolling, false otherwise.
     *
     * @deprecated Use {@link OverScroller#isFinished()} directly.
     */
    @Deprecated
    public boolean isFinished() {
        return mScroller.isFinished();
    }

    /**
     * Returns the current X offset in the scroll.
     *
     * @return The new X offset as an absolute distance from the origin.
     *
     * @deprecated Use {@link OverScroller#getCurrX()} directly.
     */
    @Deprecated
    public int getCurrX() {
        return mScroller.getCurrX();
    }

    /**
     * Returns the current Y offset in the scroll.
     *
     * @return The new Y offset as an absolute distance from the origin.
     *
     * @deprecated Use {@link OverScroller#getCurrY()} directly.
     */
    @Deprecated
    public int getCurrY() {
        return mScroller.getCurrY();
    }

    /**
     * @return The final X position for the scroll in progress, if known.
     *
     * @deprecated Use {@link OverScroller#getFinalX()} directly.
     */
    @Deprecated
    public int getFinalX() {
        return mScroller.getFinalX();
    }

    /**
     * @return The final Y position for the scroll in progress, if known.
     *
     * @deprecated Use {@link OverScroller#getFinalY()} directly.
     */
    @Deprecated
    public int getFinalY() {
        return mScroller.getFinalY();
    }

    /**
     * Returns the current velocity on platform versions that support it.
     *
     * <p> This method should only be used as input for nonessential visual effects such as
     * {@link EdgeEffectCompat}.</p>
     *
     * @return The original velocity less the deceleration. Result may be
     * negative.
     *
     * @deprecated Use {@link OverScroller#getCurrVelocity()} directly.
     */
    @Deprecated
    public float getCurrVelocity() {
        return mScroller.getCurrVelocity();
    }

    /**
     * Call this when you want to know the new location.  If it returns true,
     * the animation is not yet finished.  loc will be altered to provide the
     * new location.
     *
     * @deprecated Use {@link OverScroller#computeScrollOffset()} directly.
     */
    @Deprecated
    public boolean computeScrollOffset() {
        return mScroller.computeScrollOffset();
    }

    /**
     * Start scrolling by providing a starting point and the distance to travel.
     * The scroll will use the default value of 250 milliseconds for the
     * duration.
     *
     * @param startX Starting horizontal scroll offset in pixels. Positive
     *        numbers will scroll the content to the left.
     * @param startY Starting vertical scroll offset in pixels. Positive numbers
     *        will scroll the content up.
     * @param dx Horizontal distance to travel. Positive numbers will scroll the
     *        content to the left.
     * @param dy Vertical distance to travel. Positive numbers will scroll the
     *        content up.
     *
     * @deprecated Use {@link OverScroller#getCurrX()} directly.
     */
    @Deprecated
    public void startScroll(int startX, int startY, int dx, int dy) {
        mScroller.startScroll(startX, startY, dx, dy);
    }

    /**
     * Start scrolling by providing a starting point and the distance to travel.
     *
     * @param startX Starting horizontal scroll offset in pixels. Positive
     *        numbers will scroll the content to the left.
     * @param startY Starting vertical scroll offset in pixels. Positive numbers
     *        will scroll the content up.
     * @param dx Horizontal distance to travel. Positive numbers will scroll the
     *        content to the left.
     * @param dy Vertical distance to travel. Positive numbers will scroll the
     *        content up.
     * @param duration Duration of the scroll in milliseconds.
     *
     * @deprecated Use {@link OverScroller#startScroll(int, int, int, int, int)} directly.
     */
    @Deprecated
    public void startScroll(int startX, int startY, int dx, int dy, int duration) {
        mScroller.startScroll(startX, startY, dx, dy, duration);
    }

    /**
     * Start scrolling based on a fling gesture. The distance travelled will
     * depend on the initial velocity of the fling.
     *
     * @param startX Starting point of the scroll (X)
     * @param startY Starting point of the scroll (Y)
     * @param velocityX Initial velocity of the fling (X) measured in pixels per
     *        second.
     * @param velocityY Initial velocity of the fling (Y) measured in pixels per
     *        second
     * @param minX Minimum X value. The scroller will not scroll past this
     *        point.
     * @param maxX Maximum X value. The scroller will not scroll past this
     *        point.
     * @param minY Minimum Y value. The scroller will not scroll past this
     *        point.
     * @param maxY Maximum Y value. The scroller will not scroll past this
     *        point.
     *
     * @deprecated Use {@link OverScroller#fling(int, int, int, int, int, int, int, int)} directly.
     */
    @Deprecated
    public void fling(int startX, int startY, int velocityX, int velocityY,
            int minX, int maxX, int minY, int maxY) {
        mScroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY);
    }

    /**
     * Start scrolling based on a fling gesture. The distance travelled will
     * depend on the initial velocity of the fling.
     *
     * @param startX Starting point of the scroll (X)
     * @param startY Starting point of the scroll (Y)
     * @param velocityX Initial velocity of the fling (X) measured in pixels per
     *        second.
     * @param velocityY Initial velocity of the fling (Y) measured in pixels per
     *        second
     * @param minX Minimum X value. The scroller will not scroll past this
     *        point.
     * @param maxX Maximum X value. The scroller will not scroll past this
     *        point.
     * @param minY Minimum Y value. The scroller will not scroll past this
     *        point.
     * @param maxY Maximum Y value. The scroller will not scroll past this
     *        point.
     * @param overX Overfling range. If > 0, horizontal overfling in either
     *            direction will be possible.
     * @param overY Overfling range. If > 0, vertical overfling in either
     *            direction will be possible.
     *
     * @deprecated Use {@link OverScroller#fling(int, int, int, int, int, int, int, int, int, int)}
     * directly.
     */
    @Deprecated
    public void fling(int startX, int startY, int velocityX, int velocityY,
            int minX, int maxX, int minY, int maxY, int overX, int overY) {
        mScroller.fling(startX, startY, velocityX, velocityY,
                minX, maxX, minY, maxY, overX, overY);
    }

    /**
     * Call this when you want to 'spring back' into a valid coordinate range.
     *
     * @param startX Starting X coordinate
     * @param startY Starting Y coordinate
     * @param minX Minimum valid X value
     * @param maxX Maximum valid X value
     * @param minY Minimum valid Y value
     * @param maxY Maximum valid Y value
     * @return true if a springback was initiated, false if startX and startY were
     *          already within the valid range.
     *
     * @deprecated Use {@link OverScroller#springBack(int, int, int, int, int, int)} directly.
     */
    @Deprecated
    public boolean springBack(int startX, int startY, int minX, int maxX, int minY, int maxY) {
        return mScroller.springBack(startX, startY, minX, maxX, minY, maxY);
    }

    /**
     * Stops the animation. Aborting the animation causes the scroller to move to the final x and y
     * position.
     *
     * @deprecated Use {@link OverScroller#abortAnimation()} directly.
     */
    @Deprecated
    public void abortAnimation() {
        mScroller.abortAnimation();
    }


    /**
     * Notify the scroller that we've reached a horizontal boundary.
     * Normally the information to handle this will already be known
     * when the animation is started, such as in a call to one of the
     * fling functions. However there are cases where this cannot be known
     * in advance. This function will transition the current motion and
     * animate from startX to finalX as appropriate.
     *
     * @param startX Starting/current X position
     * @param finalX Desired final X position
     * @param overX Magnitude of overscroll allowed. This should be the maximum
     *              desired distance from finalX. Absolute value - must be positive.
     *
     * @deprecated Use {@link OverScroller#notifyHorizontalEdgeReached(int, int, int)} directly.
     */
    @Deprecated
    public void notifyHorizontalEdgeReached(int startX, int finalX, int overX) {
        mScroller.notifyHorizontalEdgeReached(startX, finalX, overX);
    }

    /**
     * Notify the scroller that we've reached a vertical boundary.
     * Normally the information to handle this will already be known
     * when the animation is started, such as in a call to one of the
     * fling functions. However there are cases where this cannot be known
     * in advance. This function will animate a parabolic motion from
     * startY to finalY.
     *
     * @param startY Starting/current Y position
     * @param finalY Desired final Y position
     * @param overY Magnitude of overscroll allowed. This should be the maximum
     *              desired distance from finalY. Absolute value - must be positive.
     *
     * @deprecated Use {@link OverScroller#notifyVerticalEdgeReached(int, int, int)} directly.
     */
    @Deprecated
    public void notifyVerticalEdgeReached(int startY, int finalY, int overY) {
        mScroller.notifyVerticalEdgeReached(startY, finalY, overY);
    }

    /**
     * Returns whether the current Scroller is currently returning to a valid position.
     * Valid bounds were provided by the
     * {@link #fling(int, int, int, int, int, int, int, int, int, int)} method.
     *
     * One should check this value before calling
     * {@link #startScroll(int, int, int, int)} as the interpolation currently in progress
     * to restore a valid position will then be stopped. The caller has to take into account
     * the fact that the started scroll will start from an overscrolled position.
     *
     * @return true when the current position is overscrolled and in the process of
     *         interpolating back to a valid value.
     *
     * @deprecated Use {@link OverScroller#isOverScrolled()} directly.
     */
    @Deprecated
    public boolean isOverScrolled() {
        return mScroller.isOverScrolled();
    }
}
