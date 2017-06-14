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
package android.support.v4.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.widget.EdgeEffect;

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

    private static final EdgeEffectBaseImpl IMPL;

    static {
        if (Build.VERSION.SDK_INT >= 21) {
            IMPL = new EdgeEffectApi21Impl();
        } else {
            IMPL = new EdgeEffectBaseImpl();
        }
    }

    static class EdgeEffectBaseImpl {
        public void onPull(EdgeEffect edgeEffect, float deltaDistance, float displacement) {
            edgeEffect.onPull(deltaDistance);
        }
    }

    @RequiresApi(21)
    static class EdgeEffectApi21Impl extends EdgeEffectBaseImpl {
        @Override
        public void onPull(EdgeEffect edgeEffect, float deltaDistance, float displacement) {
            edgeEffect.onPull(deltaDistance, displacement);
        }
    }

    /**
     * Construct a new EdgeEffect themed using the given context.
     *
     * <p>Note: On platform versions that do not support EdgeEffect, all operations
     * on the newly constructed object will be mocked/no-ops.</p>
     *
     * @param context Context to use for theming the effect
     *
     * @deprecated Use {@link EdgeEffect} constructor directly.
     */
    @Deprecated
    public EdgeEffectCompat(Context context) {
        mEdgeEffect = new EdgeEffect(context);
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
        IMPL.onPull(mEdgeEffect, deltaDistance, displacement);
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
     * @see {@link EdgeEffect#onPull(float, float)}
     */
    public static void onPull(EdgeEffect edgeEffect, float deltaDistance, float displacement) {
        IMPL.onPull(edgeEffect, deltaDistance, displacement);
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
}
