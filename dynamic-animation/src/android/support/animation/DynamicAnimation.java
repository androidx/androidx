/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.support.animation;

import android.os.Build;
import android.os.Looper;
import android.util.AndroidRuntimeException;
import android.view.View;

import java.util.ArrayList;

/**
 * This class is the base class of physics-based animations. It manages the animation's
 * lifecycle such as {@link #start()} and {@link #cancel()}. This base class also handles the common
 * setup for all the subclass animations. For example, DynamicAnimation supports adding
 * {@link OnAnimationEndListener} and {@link OnAnimationUpdateListener} so that the important
 * animation events can be observed through the callbacks. The start conditions for any subclass of
 * DynamicAnimation can be set using {@link #setStartValue(float)} and
 * {@link #setStartVelocity(float)}.
 *
 * @param <T> subclass of DynamicAnimation
 */
public abstract class DynamicAnimation<T extends DynamicAnimation<T>>
        implements AnimationHandler.AnimationFrameCallback {

    /**
     * ViewProperty holds the access of a property of a {@link View}. When an animation is
     * created with a {@link ViewProperty} instance, the corresponding property value of the view
     * will be updated through this ViewProperty instance.
     */
    public abstract static class ViewProperty {
        private final String mPropertyName;

        private ViewProperty(String name) {
            mPropertyName = name;
        }

        abstract void setValue(View view, float value);
        abstract float getValue(View view);
    }

    /**
     * View's translationX property.
     */
    public static final ViewProperty TRANSLATION_X = new ViewProperty("translationX") {
        @Override
        void setValue(View view, float value) {
            view.setTranslationX(value);
        }

        @Override
        float getValue(View view) {
            return view.getTranslationX();
        }
    };

    /**
     * View's translationY property.
     */
    public static final ViewProperty TRANSLATION_Y = new ViewProperty("translationY") {
        @Override
        void setValue(View view, float value) {
            view.setTranslationY(value);
        }

        @Override
        float getValue(View view) {
            return view.getTranslationY();
        }
    };

    /**
     * View's translationZ property.
     */
    public static final ViewProperty TRANSLATION_Z = new ViewProperty("translationZ") {
        @Override
        void setValue(View view, float value) {
            if (isZSupported()) {
                view.setTranslationZ(value);
            }
        }

        @Override
        float getValue(View view) {
            if (isZSupported()) {
                return view.getTranslationZ();
            } else {
                return 0;
            }
        }
    };

    /**
     * View's scaleX property.
     */
    public static final ViewProperty SCALE_X = new ViewProperty("scaleX") {
        @Override
        void setValue(View view, float value) {
            view.setScaleX(value);
        }

        @Override
        float getValue(View view) {
            return view.getScaleX();
        }
    };

    /**
     * View's scaleY property.
     */
    public static final ViewProperty SCALE_Y = new ViewProperty("scaleY") {
        @Override
        void setValue(View view, float value) {
            view.setScaleY(value);
        }

        @Override
        float getValue(View view) {
            return view.getScaleY();
        }
    };

    /**
     * View's rotation property.
     */
    public static final ViewProperty ROTATION = new ViewProperty("rotation") {
        @Override
        void setValue(View view, float value) {
            view.setRotation(value);
        }

        @Override
        float getValue(View view) {
            return view.getRotation();
        }
    };

    /**
     * View's rotationX property.
     */
    public static final ViewProperty ROTATION_X = new ViewProperty("rotationX") {
        @Override
        void setValue(View view, float value) {
            view.setRotationX(value);
        }

        @Override
        float getValue(View view) {
            return view.getRotationX();
        }
    };

    /**
     * View's rotationY property.
     */
    public static final ViewProperty ROTATION_Y = new ViewProperty("rotationY") {
        @Override
        void setValue(View view, float value) {
            view.setRotationY(value);
        }

        @Override
        float getValue(View view) {
            return view.getRotationY();
        }
    };

    /**
     * View's x property.
     */
    public static final ViewProperty X = new ViewProperty("x") {
        @Override
        void setValue(View view, float value) {
            view.setX(value);
        }

        @Override
        float getValue(View view) {
            return view.getX();
        }
    };

    /**
     * View's y property.
     */
    public static final ViewProperty Y = new ViewProperty("y") {
        @Override
        void setValue(View view, float value) {
            view.setY(value);
        }

        @Override
        float getValue(View view) {
            return view.getY();
        }
    };

    /**
     * View's z property.
     */
    public static final ViewProperty Z = new ViewProperty("z") {
        @Override
        void setValue(View view, float value) {
            if (isZSupported()) {
                view.setZ(value);
            }
        }

        @Override
        float getValue(View view) {
            if (isZSupported()) {
                return view.getZ();
            } else {
                return 0;
            }
        }
    };

    /**
     * View's alpha property.
     */
    public static final ViewProperty ALPHA = new ViewProperty("alpha") {
        @Override
        void setValue(View view, float value) {
            view.setAlpha(value);
        }

        @Override
        float getValue(View view) {
            return view.getAlpha();
        }
    };

    // Properties below are not RenderThread compatible
    /**
     * View's scrollX property.
     */
    public static final ViewProperty SCROLL_X = new ViewProperty("scrollX") {
        @Override
        void setValue(View view, float value) {
            view.setScrollX((int) value);
        }

        @Override
        float getValue(View view) {
            return view.getScrollX();
        }
    };

    /**
     * View's scrollY property.
     */
    public static final ViewProperty SCROLL_Y = new ViewProperty("scrollY") {
        @Override
        void setValue(View view, float value) {
            view.setScrollY((int) value);
        }

        @Override
        float getValue(View view) {
            return view.getScrollY();
        }
    };

    // Use the max value of float to indicate an unset state.
    private static final float UNSET = Float.MAX_VALUE;

    // Internal tracking for velocity.
    float mVelocity = 0;

    // Internal tracking for value.
    float mValue = UNSET;

    // Tracks whether start value is set. If not, the animation will obtain the value at the time
    // of starting through the getter and use that as the starting value of the animation.
    boolean mStartValueIsSet = false;

    // View target to be animated.
    final View mTarget;

    // View property id.
    final ViewProperty mViewProperty;

    // Package private tracking of animation lifecycle state. Visible to subclass animations.
    boolean mRunning = false;

    // Min and max values that defines the range of the animation values.
    float mMaxValue = Float.MAX_VALUE;
    float mMinValue = -mMaxValue;

    // Last frame time. Always gets reset to -1  at the end of the animation.
    private long mLastFrameTime = 0;

    // List of end listeners
    private final ArrayList<OnAnimationEndListener> mEndListeners = new ArrayList<>();

    // List of update listeners
    private final ArrayList<OnAnimationUpdateListener> mUpdateListeners = new ArrayList<>();

    /**
     * Creates a dynamic animation to animate the given property for the given {@link View}
     *
     * @param view the View whose property is to be animated
     * @param property the property to be animated
     */
    DynamicAnimation(View view, ViewProperty property) {
        mTarget = view;
        mViewProperty = property;
    }

    /**
     * Sets the start value of the animation. If start value is not set, the animation will get
     * the current value for the view's property, and use that as the start value.
     *
     * @param startValue start value for the animation
     * @return the Animation whose start value is being set
     */
    public T setStartValue(float startValue) {
        mValue = startValue;
        mStartValueIsSet = true;
        return (T) this;
    }

    /**
     * Start velocity of the animation. Default velocity is 0. Unit: pixel/second
     *
     * <p>Note when using a fixed value as the start velocity (as opposed to getting the velocity
     * through touch events), it is recommended to define such a value in dp/second and convert it
     * to pixel/second based on the density of the screen to achieve a consistent look across
     * different screens.
     *
     * <p>To convert from dp/second to pixel/second:
     * <pre class="prettyprint">
     * float pixelPerSecond = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpPerSecond,
     *         getResources().getDisplayMetrics());
     * </pre>
     *
     * @param startVelocity start velocity of the animation in pixel/second
     * @return the Animation whose start velocity is being set
     */
    public T setStartVelocity(float startVelocity) {
        mVelocity = startVelocity;
        return (T) this;
    }

    /**
     * Sets the max value of the animation. Animations will not animate beyond their max value.
     * Whether or not animation will come to an end when max value is reached is dependent on the
     * child animation's implementation.
     *
     * @param max maximum value of the property to be animated
     * @return the Animation whose max value is being set
     */
    public T setMaxValue(float max) {
        // This max value should be checked and handled in the subclass animations, instead of
        // assuming the end of the animations when the max/min value is hit in the base class.
        // The reason is that hitting max/min value may just be a transient state, such as during
        // the spring oscillation.
        mMaxValue = max;
        return (T) this;
    }

    /**
     * Sets the min value of the animation. Animations will not animate beyond their min value.
     * Whether or not animation will come to an end when min value is reached is dependent on the
     * child animation's implementation.
     *
     * @param min minimum value of the property to be animated
     * @return the Animation whose min value is being set
     */
    public T setMinValue(float min) {
        mMinValue = min;
        return (T) this;
    }

    /**
     * Adds an end listener to the animation for receiving onAnimationEnd callbacks. If the listener
     * is {@code null} or has already been added to the list of listeners for the animation, no op.
     *
     * @param listener the listener to be added
     * @return the animation to which the listener is added
     */
    public T addEndListener(OnAnimationEndListener listener) {
        if (!mEndListeners.contains(listener)) {
            mEndListeners.add(listener);
        }
        return (T) this;
    }

    /**
     * Removes the end listener from the animation, so as to stop receiving animation end callbacks.
     *
     * @param listener the listener to be removed
     */
    public void removeEndListener(OnAnimationEndListener listener) {
        removeEntry(mEndListeners, listener);
    }

    /**
     * Adds an update listener to the animation for receiving per-frame animation update callbacks.
     * If the listener is {@code null} or has already been added to the list of listeners for the
     * animation, no op.
     *
     * <p>Note that update listener should only be added before the start of the animation.
     *
     * @param listener the listener to be added
     * @return the animation to which the listener is added
     * @throws UnsupportedOperationException if the update listener is added after the animation has
     *                                       started
     */
    public T addUpdateListener(OnAnimationUpdateListener listener) {
        if (isRunning()) {
            // Require update listener to be added before the animation, such as when we start
            // the animation, we know whether the animation is RenderThread compatible.
            throw new UnsupportedOperationException("Error: Update listeners must be added before"
                    + "the animation.");
        }
        if (!mUpdateListeners.contains(listener)) {
            mUpdateListeners.add(listener);
        }
        return (T) this;
    }

    /**
     * Removes the update listener from the animation, so as to stop receiving animation update
     * callbacks.
     *
     * @param listener the listener to be removed
     */
    public void removeUpdateListener(OnAnimationUpdateListener listener) {
        removeEntry(mUpdateListeners, listener);
    }

    /**
     * Remove {@code null} entries from the list.
     */
    private static <T> void removeNullEntries(ArrayList<T> list) {
        // Clean up null entries
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i) == null) {
                list.remove(i);
            }
        }
    }

    /**
     * Remove an entry from the list by marking it {@code null} and clean up later.
     */
    private static <T> void removeEntry(ArrayList<T> list, T entry) {
        int id = list.indexOf(entry);
        if (id >= 0) {
            list.set(id, null);
        }
    }

    /****************Animation Lifecycle Management***************/

    /**
     * Starts an animation. If the animation has already been started, no op. Note that calling
     * {@link #start()} will not immediately set the property value to start value of the animation.
     * The property values will be changed at each animation pulse, which happens before the draw
     * pass. As a result, the changes will be reflected in the next frame, the same as if the values
     * were set immediately. This method should only be called on main thread.
     *
     * @throws AndroidRuntimeException if this method is not called on the main thread
     */
    public void start() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new AndroidRuntimeException("Animations may only be started on the main thread");
        }
        if (!mRunning) {
            startAnimationInternal();
        }
    }

    /**
     * Cancels the on-going animation. If the animation hasn't started, no op. Note that this method
     * should only be called on main thread.
     *
     * @throws AndroidRuntimeException if this method is not called on the main thread
     */
    public void cancel() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new AndroidRuntimeException("Animations may only be canceled on the main thread");
        }
        if (mRunning) {
            endAnimationInternal(true);
        }
    }

    /**
     * Returns whether the animation is currently running.
     *
     * @return {@code true} if the animation is currently running, {@code false} otherwise
     */
    public boolean isRunning() {
        return mRunning;
    }

    /************************** Private APIs below ********************************/

    // This gets called when the animation is started, to finish the setup of the animation
    // before the animation pulsing starts.
    private void startAnimationInternal() {
        if (!mRunning) {
            mRunning = true;
            if (!mStartValueIsSet) {
                mValue = getPropertyValue();
            }
            // Sanity check:
            if (mValue > mMaxValue || mValue < mMinValue) {
                throw new IllegalArgumentException("Starting value need to be in between min"
                        + " value and max value");
            }
            AnimationHandler.getInstance().addAnimationFrameCallback(this, 0);
        }
    }

    /**
     * This gets call on each frame of the animation. Animation value and velocity are updated
     * in this method based on the new frame time. The property value of the view being animated
     * is then updated. The animation's ending conditions are also checked in this method. Once
     * the animation reaches equilibrium, the animation will come to its end, and end listeners
     * will be notified, if any.
     *
     * @hide
     */
    @Override
    public boolean doAnimationFrame(long frameTime) {
        if (mLastFrameTime == 0) {
            // First frame.
            mLastFrameTime = frameTime;
            setPropertyValue(mValue);
            return false;
        }
        long deltaT = frameTime - mLastFrameTime;
        mLastFrameTime = frameTime;
        boolean finished = updateValueAndVelocity(deltaT);
        // Clamp value & velocity.
        mValue = Math.min(mValue, mMaxValue);
        mValue = Math.max(mValue, mMinValue);

        setPropertyValue(mValue);

        if (finished) {
            endAnimationInternal(false);
        }
        return finished;
    }

    /**
     * Updates the animation state (i.e. value and velocity). This method is package private, so
     * subclasses can override this method to calculate the new value and velocity in their custom
     * way.
     *
     * @param deltaT time elapsed in millisecond since last frame
     * @return whether the animation has finished
     */
    abstract boolean updateValueAndVelocity(long deltaT);

    /**
     * Internal method to reset the animation states when animation is finished/canceled.
     */
    private void endAnimationInternal(boolean canceled) {
        mRunning = false;
        AnimationHandler.getInstance().removeCallback(this);
        mLastFrameTime = 0;
        mStartValueIsSet = false;
        for (int i = 0; i < mEndListeners.size(); i++) {
            if (mEndListeners.get(i) != null) {
                mEndListeners.get(i).onAnimationEnd(this, canceled, mValue, mVelocity);
            }
        }
        removeNullEntries(mEndListeners);
    }

    /**
     * Returns whether z and translationZ are supported on the current build version.
     */
    private static boolean isZSupported() {
        return Build.VERSION.SDK_INT >= 21;
    }

    /**
     * Updates the property value through the corresponding setter.
     */
    void setPropertyValue(float value) {
        mViewProperty.setValue(mTarget, value);
        for (int i = 0; i < mUpdateListeners.size(); i++) {
            if (mUpdateListeners.get(i) != null) {
                mUpdateListeners.get(i).onAnimationUpdate(this, mValue, mVelocity);
            }
        }
        removeNullEntries(mUpdateListeners);
    }

    /**
     * Obtain the property value through the corresponding getter.
     */
    private float getPropertyValue() {
        return mViewProperty.getValue(mTarget);
    }

    /****************Sub class animations**************/
    /**
     * Returns the acceleration at the given value with the given velocity.
     **/
    abstract float getAcceleration(float value, float velocity);

    /**
     * Returns whether the animation has reached equilibrium.
     */
    abstract boolean isAtEquilibrium(float value, float velocity);

    /**
     * An animation listener that receives end notifications from an animation.
     */
    public interface OnAnimationEndListener {
        /**
         * Notifies the end of an animation. Note that this callback will be invoked not only when
         * an animation reach equilibrium, but also when the animation is canceled.
         *
         * @param animation animation that has ended or was canceled
         * @param canceled whether the animation has been canceled
         * @param value the final value when the animation stopped
         * @param velocity the final velocity when the animation stopped
         */
        void onAnimationEnd(DynamicAnimation animation, boolean canceled, float value,
                              float velocity);
    }

    /**
     * Implementors of this interface can add themselves as update listeners
     * to an <code>DynamicAnimation</code> instance to receive callbacks on every animation
     * frame, after the current frame's values have been calculated for that
     * <code>DynamicAnimation</code>.
     */
    public interface OnAnimationUpdateListener {

        /**
         * Notifies the occurrence of another frame of the animation.
         *
         * @param animation animation that the update listener is added to
         * @param value the current value of the animation
         * @param velocity the current velocity of the animation
         */
        void onAnimationUpdate(DynamicAnimation animation, float value, float velocity);
    }

}
