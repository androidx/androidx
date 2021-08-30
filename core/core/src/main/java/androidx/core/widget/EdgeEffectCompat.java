/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.EdgeEffect;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.os.BuildCompat;

/**
 * Helper for accessing {@link android.widget.EdgeEffect}.
 *
 * This class is used to access {@link android.widget.EdgeEffect} on platform versions
 * that support it. When running on older platforms it will result in no-ops. It should
 * be used by views that wish to use the standard Android visual effects at the edges
 * of scrolling containers.
 */
public final class EdgeEffectCompat {
    private EdgeEffect mEdgeEffect;

    /**
     * Construct a new EdgeEffect themed using the given context.
     *
     * <p>Note: On platform versions that do not support EdgeEffect, all operations
     * on the newly constructed object will be mocked/no-ops.</p>
     *
     * @param context Context to use for theming the effect
     *
     * @deprecated Use {@link EdgeEffect} constructor directly or
     * {@link EdgeEffectCompat#create(Context, AttributeSet)}.
     */
    @Deprecated
    public EdgeEffectCompat(Context context) {
        mEdgeEffect = new EdgeEffect(context);
    }

    /**
     * Constructs and returns a new EdgeEffect themed using the given context, allowing support
     * for the view attributes.
     *
     * @param context Context to use for theming the effect
     * @param attrs The attributes of the XML tag that is inflating the view
     */
    @NonNull
    public static EdgeEffect create(@NonNull Context context, @Nullable AttributeSet attrs) {
        if (BuildCompat.isAtLeastS()) {
            return Api31Impl.create(context, attrs);
        }

        return new EdgeEffect(context);
    }

    /**
     * Returns the pull distance needed to be released to remove the showing effect.
     * It is determined by the {@link #onPull(float, float)} <code>deltaDistance</code> and
     * any animating values, including from {@link #onAbsorb(int)} and {@link #onRelease()}.
     *
     * This can be used in conjunction with {@link #onPullDistance(EdgeEffect, float, float)} to
     * release the currently showing effect.
     *
     * On {@link Build.VERSION_CODES#R} and earlier, this will return 0.
     *
     * @return The pull distance that must be released to remove the showing effect or 0 for
     * versions {@link Build.VERSION_CODES#R} and earlier.
     */
    public static float getDistance(@NonNull EdgeEffect edgeEffect) {
        if (BuildCompat.isAtLeastS()) {
            return Api31Impl.getDistance(edgeEffect);
        }
        return 0;
    }

    /**
     * Set the size of this edge effect in pixels.
     *
     * @param width Effect width in pixels
     * @param height Effect height in pixels
     *
     * @deprecated Use {@link EdgeEffect#setSize(int, int)} directly.
     */
    @Deprecated
    public void setSize(int width, int height) {
        mEdgeEffect.setSize(width, height);
    }

    /**
     * Reports if this EdgeEffectCompat's animation is finished. If this method returns false
     * after a call to {@link #draw(Canvas)} the host widget should schedule another
     * drawing pass to continue the animation.
     *
     * @return true if animation is finished, false if drawing should continue on the next frame.
     *
     * @deprecated Use {@link EdgeEffect#isFinished()} directly.
     */
    @Deprecated
    public boolean isFinished() {
        return mEdgeEffect.isFinished();
    }

    /**
     * Immediately finish the current animation.
     * After this call {@link #isFinished()} will return true.
     *
     * @deprecated Use {@link EdgeEffect#finish()} directly.
     */
    @Deprecated
    public void finish() {
        mEdgeEffect.finish();
    }

    /**
     * A view should call this when content is pulled away from an edge by the user.
     * This will update the state of the current visual effect and its associated animation.
     * The host view should always {@link android.view.View#invalidate()} if this method
     * returns true and draw the results accordingly.
     *
     * @param deltaDistance Change in distance since the last call. Values may be 0 (no change) to
     *                      1.f (full length of the view) or negative values to express change
     *                      back toward the edge reached to initiate the effect.
     * @return true if the host view should call invalidate, false if it should not.
     *
     * @deprecated Use {@link #onPull(EdgeEffect, float, float)}.
     */
    @Deprecated
    public boolean onPull(float deltaDistance) {
        mEdgeEffect.onPull(deltaDistance);
        return true;
    }

    /**
     * A view should call this when content is pulled away from an edge by the user.
     * This will update the state of the current visual effect and its associated animation.
     * The host view should always {@link android.view.View#invalidate()} if this method
     * returns true and draw the results accordingly.
     *
     * Views using {@link EdgeEffect} should favor {@link EdgeEffect#onPull(float, float)} when
     * the displacement of the pull point is known.
     *
     * @param deltaDistance Change in distance since the last call. Values may be 0 (no change) to
     *                      1.f (full length of the view) or negative values to express change
     *                      back toward the edge reached to initiate the effect.
     * @param displacement The displacement from the starting side of the effect of the point
     *                     initiating the pull. In the case of touch this is the finger position.
     *                     Values may be from 0-1.
     * @return true if the host view should call invalidate, false if it should not.
     *
     * @deprecated Use {@link EdgeEffect#onPull(float)} directly.
     */
    @Deprecated
    public boolean onPull(float deltaDistance, float displacement) {
        onPull(mEdgeEffect, deltaDistance, displacement);
        return true;
    }

    /**
     * A view should call this when content is pulled away from an edge by the user.
     * This will update the state of the current visual effect and its associated animation.
     * The host view should always {@link android.view.View#invalidate()} after call this method
     * and draw the results accordingly.
     *
     * @param edgeEffect The EdgeEffect that is attached to the view that is getting pulled away
     *                   from an edge by the user.
     * @param deltaDistance Change in distance since the last call. Values may be 0 (no change) to
     *                      1.f (full length of the view) or negative values to express change
     *                      back toward the edge reached to initiate the effect.
     * @param displacement The displacement from the starting side of the effect of the point
     *                     initiating the pull. In the case of touch this is the finger position.
     *                     Values may be from 0-1.
     *
     * @see EdgeEffect#onPull(float, float)
     */
    public static void onPull(@NonNull EdgeEffect edgeEffect, float deltaDistance,
            float displacement) {
        if (Build.VERSION.SDK_INT >= 21) {
            edgeEffect.onPull(deltaDistance, displacement);
        } else {
            edgeEffect.onPull(deltaDistance);
        }
    }

    /**
     * A view should call this when content is pulled away from an edge by the user.
     * This will update the state of the current visual effect and its associated animation.
     * The host view should always {@link android.view.View#invalidate()} after this
     * and draw the results accordingly. This works similarly to {@link #onPull(float, float)},
     * but returns the amount of <code>deltaDistance</code> that has been consumed. For versions
     * {@link Build.VERSION_CODES#S} and above, if the {@link #getDistance(EdgeEffect)} is currently
     * 0 and <code>deltaDistance</code> is negative, this function will return 0 and the drawn value
     * will remain unchanged. For versions {@link Build.VERSION_CODES#R} and below, this will
     * consume all of the provided value and return <code>deltaDistance</code>.
     *
     * This method can be used to reverse the effect from a pull or absorb and partially consume
     * some of a motion:
     *
     * <pre class="prettyprint">
     *     if (deltaY < 0 && EdgeEffectCompat.getDistance(edgeEffect) != 0) {
     *         float displacement = x / getWidth();
     *         float dist = deltaY / getHeight();
     *         float consumed = EdgeEffectCompat.onPullDistance(edgeEffect, dist, displacement);
     *         deltaY -= consumed * getHeight();
     *         if (edgeEffect.getDistance() == 0f) edgeEffect.onRelease();
     *     }
     * </pre>
     *
     * @param deltaDistance Change in distance since the last call. Values may be 0 (no change) to
     *                      1.f (full length of the view) or negative values to express change
     *                      back toward the edge reached to initiate the effect.
     * @param displacement The displacement from the starting side of the effect of the point
     *                     initiating the pull. In the case of touch this is the finger position.
     *                     Values may be from 0-1.
     * @return The amount of <code>deltaDistance</code> that was consumed, a number between
     * 0 and <code>deltaDistance</code>.
     */
    public static float onPullDistance(
            @NonNull EdgeEffect edgeEffect,
            float deltaDistance,
            float displacement
    ) {
        if (BuildCompat.isAtLeastS()) {
            return Api31Impl.onPullDistance(edgeEffect, deltaDistance, displacement);
        }
        onPull(edgeEffect, deltaDistance, displacement);
        return deltaDistance;
    }

    /**
     * Call when the object is released after being pulled.
     * This will begin the "decay" phase of the effect. After calling this method
     * the host view should {@link android.view.View#invalidate()} if this method
     * returns true and thereby draw the results accordingly.
     *
     * @return true if the host view should invalidate, false if it should not.
     *
     * @deprecated Use {@link EdgeEffect#onRelease()} directly.
     */
    @Deprecated
    public boolean onRelease() {
        mEdgeEffect.onRelease();
        return mEdgeEffect.isFinished();
    }

    /**
     * Call when the effect absorbs an impact at the given velocity.
     * Used when a fling reaches the scroll boundary.
     *
     * <p>When using a {@link android.widget.Scroller} or {@link android.widget.OverScroller},
     * the method <code>getCurrVelocity</code> will provide a reasonable approximation
     * to use here.</p>
     *
     * @param velocity Velocity at impact in pixels per second.
     * @return true if the host view should invalidate, false if it should not.
     *
     * @deprecated Use {@link EdgeEffect#onAbsorb(int)} directly.
     */
    @Deprecated
    public boolean onAbsorb(int velocity) {
        mEdgeEffect.onAbsorb(velocity);
        return true;
    }

    /**
     * Draw into the provided canvas. Assumes that the canvas has been rotated
     * accordingly and the size has been set. The effect will be drawn the full
     * width of X=0 to X=width, beginning from Y=0 and extending to some factor <
     * 1.f of height.
     *
     * @param canvas Canvas to draw into
     * @return true if drawing should continue beyond this frame to continue the
     *         animation
     *
     * @deprecated Use {@link EdgeEffect#draw(Canvas)} directly.
     */
    @Deprecated
    public boolean draw(Canvas canvas) {
        return mEdgeEffect.draw(canvas);
    }

    // TODO(b/181171227): This actually requires S, but we don't have a version for S yet.
    @RequiresApi(Build.VERSION_CODES.R)
    private static class Api31Impl {
        private Api31Impl() {}

        @DoNotInline
        public static EdgeEffect create(Context context, AttributeSet attrs) {
            try {
                return new EdgeEffect(context, attrs);
            } catch (Throwable t) {
                return new EdgeEffect(context); // Old preview release
            }
        }

        @DoNotInline
        public static float onPullDistance(
                EdgeEffect edgeEffect,
                float deltaDistance,
                float displacement
        ) {
            try {
                return edgeEffect.onPullDistance(deltaDistance, displacement);
            } catch (Throwable t) {
                edgeEffect.onPull(deltaDistance, displacement); // Old preview release
                return 0;
            }
        }

        @DoNotInline
        public static float getDistance(EdgeEffect edgeEffect) {
            try {
                return edgeEffect.getDistance();
            } catch (Throwable t) {
                return 0; // Old preview release
            }
        }
    }
}
