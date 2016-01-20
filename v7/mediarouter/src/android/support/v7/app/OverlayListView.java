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

package android.support.v7.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.animation.Interpolator;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A ListView which has an additional overlay layer. {@link BitmapDrawable}
 * can be added to the layer and can be animated.
 */
final class OverlayListView extends ListView {
    private final List<OverlayObject> mOverlayObjects = new ArrayList<>();

    public OverlayListView(Context context) {
        super(context);
    }

    public OverlayListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OverlayListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Adds an object to the overlay layer.
     *
     * @param object An object to be added.
     */
    public void addOverlayObject(OverlayObject object) {
        mOverlayObjects.add(object);
    }

    /**
     * Starts all animations of objects in the overlay layer.
     */
    public void startAnimationAll() {
        for (OverlayObject object : mOverlayObjects) {
            if (!object.isAnimationStarted()) {
                object.startAnimation(getDrawingTime());
            }
        }
    }

    /**
     * Stops all animations of objects in the overlay layer.
     */
    public void stopAnimationAll() {
        for (OverlayObject object : mOverlayObjects) {
            object.stopAnimation();
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mOverlayObjects.size() > 0) {
            Iterator<OverlayObject> it = mOverlayObjects.iterator();
            while (it.hasNext()) {
                OverlayObject object = it.next();
                BitmapDrawable bitmap = object.getBitmapDrawable();
                if (bitmap != null) {
                    bitmap.draw(canvas);
                }
                if (!object.update(getDrawingTime())) {
                    it.remove();
                }
            }
        }
    }

    /**
     * A class that represents an object to be shown in the overlay layer.
     */
    public static class OverlayObject {
        private BitmapDrawable mBitmap;
        private float mCurrentAlpha = 1.0f;
        private Rect mCurrentBounds;
        private Interpolator mInterpolator;
        private long mDuration;
        private Rect mStartRect;
        private int mDeltaY;
        private float mStartAlpha = 1.0f;
        private float mEndAlpha = 1.0f;
        private long mStartTime;
        private boolean mIsAnimationStarted;
        private boolean mIsAnimationEnded;
        private OnAnimationEndListener mListener;

        public OverlayObject(BitmapDrawable bitmap, Rect startRect) {
            mBitmap = bitmap;
            mStartRect = startRect;
            mCurrentBounds = new Rect(startRect);
            if (mBitmap != null && mCurrentBounds != null) {
                mBitmap.setAlpha((int) (mCurrentAlpha * 255));
                mBitmap.setBounds(mCurrentBounds);
            }
        }

        /**
         * Returns the bitmap that this object represents.
         *
         * @return BitmapDrawable that this object has.
         */
        public BitmapDrawable getBitmapDrawable() {
            return mBitmap;
        }

        /**
         * Returns the started status of the animation.
         *
         * @return True if the animation has started, false otherwise.
         */
        public boolean isAnimationStarted() {
            return mIsAnimationStarted;
        }

        /**
         * Sets animation for varying alpha.
         *
         * @param startAlpha Starting alpha value for the animation, where 1.0 means
         * fully opaque and 0.0 means fully transparent.
         * @param endAlpha Ending alpha value for the animation.
         * @return This OverlayObject to allow for chaining of calls.
         */
        public OverlayObject setAlphaAnimation(float startAlpha, float endAlpha) {
            mStartAlpha = startAlpha;
            mEndAlpha = endAlpha;
            return this;
        }

        /**
         * Sets animation for moving objects vertically.
         *
         * @param deltaY Distance to move in pixels.
         * @return This OverlayObject to allow for chaining of calls.
         */
        public OverlayObject setTranslateYAnimation(int deltaY) {
            mDeltaY = deltaY;
            return this;
        }

        /**
         * Sets how long the animation will last.
         *
         * @param duration Duration in milliseconds
         * @return This OverlayObject to allow for chaining of calls.
         */
        public OverlayObject setDuration(long duration) {
            mDuration = duration;
            return this;
        }

        /**
         * Sets the acceleration curve for this animation.
         *
         * @param interpolator The interpolator which defines the acceleration curve
         * @return This OverlayObject to allow for chaining of calls.
         */
        public OverlayObject setInterpolator(Interpolator interpolator) {
            mInterpolator = interpolator;
            return this;
        }

        /**
         * Binds an animation end listener to the animation.
         *
         * @param listener the animation end listener to be notified.
         * @return This OverlayObject to allow for chaining of calls.
         */
        public OverlayObject setAnimationEndListener(OnAnimationEndListener listener) {
            mListener = listener;
            return this;
        }

        /**
         * Starts the animation and sets the start time.
         *
         * @param startTime Start time to be set in Millis
         */
        public void startAnimation(long startTime) {
            mStartTime = startTime;
            mIsAnimationStarted = true;
        }

        /**
         * Stops the animation.
         */
        public void stopAnimation() {
            mIsAnimationStarted = true;
            mIsAnimationEnded = true;
            if (mListener != null) {
                mListener.onAnimationEnd();
            }
        }

        /**
         * Calculates and updates current bounds and alpha value.
         *
         * @param currentTime Current time.in millis
         */
        public boolean update(long currentTime) {
            if (mIsAnimationEnded) {
                return false;
            }
            float normalizedTime = (currentTime - mStartTime) / (float) mDuration;
            normalizedTime = Math.max(0.0f, Math.min(1.0f, normalizedTime));
            if (!mIsAnimationStarted) {
                normalizedTime = 0.0f;
            }
            float interpolatedTime = (mInterpolator == null) ? normalizedTime
                    : mInterpolator.getInterpolation(normalizedTime);
            int deltaY = (int) (mDeltaY * interpolatedTime);
            mCurrentBounds.top = mStartRect.top + deltaY;
            mCurrentBounds.bottom = mStartRect.bottom + deltaY;
            mCurrentAlpha = mStartAlpha + (mEndAlpha - mStartAlpha) * interpolatedTime;
            if (mBitmap != null && mCurrentBounds != null) {
                mBitmap.setAlpha((int) (mCurrentAlpha * 255));
                mBitmap.setBounds(mCurrentBounds);
            }
            if (mIsAnimationStarted && normalizedTime >= 1.0f) {
                mIsAnimationEnded = true;
                if (mListener != null) {
                    mListener.onAnimationEnd();
                }
            }
            return !mIsAnimationEnded;
        }

        /**
         * An animation listener that receives notifications when the animation ends.
         */
        public interface OnAnimationEndListener {
            /**
             * Notifies the end of the animation.
             */
            public void onAnimationEnd();
        }
    }
}
