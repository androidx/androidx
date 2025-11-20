/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.core.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.Interpolator;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;

public final class ViewPropertyAnimatorCompat {
    private final WeakReference<View> mView;

    ViewPropertyAnimatorCompat(View view) {
        mView = new WeakReference<>(view);
    }

    /**
     * Sets the duration for the underlying animator that animates the requested properties.
     * By default, the animator uses the default value for ValueAnimator. Calling this method
     * will cause the declared value to be used instead.
     *
     * @param value The length of ensuing property animations, in milliseconds. The value
     * cannot be negative.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public @NonNull ViewPropertyAnimatorCompat setDuration(long value) {
        View view;
        if ((view = mView.get()) != null) {
            view.animate().setDuration(value);
        }
        return this;
    }

    /**
     * This method will cause the View's <code>alpha</code> property to be animated to the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The value to be animated to.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public @NonNull ViewPropertyAnimatorCompat alpha(float value) {
        View view;
        if ((view = mView.get()) != null) {
            view.animate().alpha(value);
        }
        return this;
    }

    /**
     * This method will cause the View's <code>alpha</code> property to be animated by the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The amount to be animated by, as an offset from the current value.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public @NonNull ViewPropertyAnimatorCompat alphaBy(float value) {
        View view;
        if ((view = mView.get()) != null) {
            view.animate().alphaBy(value);
        }
        return this;
    }

    /**
     * This method will cause the View's <code>translationX</code> property to be animated to the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The value to be animated to.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public @NonNull ViewPropertyAnimatorCompat translationX(float value) {
        View view;
        if ((view = mView.get()) != null) {
            view.animate().translationX(value);
        }
        return this;
    }

    /**
     * This method will cause the View's <code>translationY</code> property to be animated to the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The value to be animated to.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public @NonNull ViewPropertyAnimatorCompat translationY(float value) {
        View view;
        if ((view = mView.get()) != null) {
            view.animate().translationY(value);
        }
        return this;
    }

    /**
     * Specifies an action to take place when the next animation ends. The action is only
     * run if the animation ends normally; if the ViewPropertyAnimator is canceled during
     * that animation, the runnable will not run.
     * This method, along with {@link #withStartAction(Runnable)}, is intended to help facilitate
     * choreographing ViewPropertyAnimator animations with other animations or actions
     * in the application.
     *
     * <p>For example, the following code animates a view to x=200 and then back to 0:</p>
     * <pre>
     *     Runnable endAction = new Runnable() {
     *         public void run() {
     *             view.animate().x(0);
     *         }
     *     };
     *     view.animate().x(200).withEndAction(endAction);
     * </pre>
     *
     * <p>For API 14 and 15, this method will run by setting
     * a listener on the ViewPropertyAnimatorCompat object and running the action
     * in that listener's {@link ViewPropertyAnimatorListener#onAnimationEnd(View)} method.</p>
     *
     * @param runnable The action to run when the next animation ends.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public @NonNull ViewPropertyAnimatorCompat withEndAction(@NonNull Runnable runnable) {
        View view;
        if ((view = mView.get()) != null) {
            ViewPropertyAnimator animator = view.animate();
            animator.withEndAction(runnable);
        }
        return this;
    }

    /**
     * Returns the current duration of property animations. If the duration was set on this
     * object, that value is returned. Otherwise, the default value of the underlying Animator
     * is returned.
     *
     * @see #setDuration(long)
     * @return The duration of animations, in milliseconds.
     */
    public long getDuration() {
        View view;
        if ((view = mView.get()) != null) {
            return view.animate().getDuration();
        } else {
            return 0;
        }
    }

    /**
     * Sets the interpolator for the underlying animator that animates the requested properties.
     * By default, the animator uses the default interpolator for ValueAnimator. Calling this method
     * will cause the declared object to be used instead.
     *
     * @param value The TimeInterpolator to be used for ensuing property animations.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public @NonNull ViewPropertyAnimatorCompat setInterpolator(@Nullable Interpolator value) {
        View view;
        if ((view = mView.get()) != null) {
            view.animate().setInterpolator(value);
        }
        return this;
    }

    /**
     * Returns the timing interpolator that this animation uses.
     *
     * @return The timing interpolator for this animation.
     */
    public @Nullable Interpolator getInterpolator() {
        View view;
        if ((view = mView.get()) != null) {
            ViewPropertyAnimator animator = view.animate();
            return (Interpolator) animator.getInterpolator();
        }
        return null;
    }

    /**
     * Sets the startDelay for the underlying animator that animates the requested properties.
     * By default, the animator uses the default value for ValueAnimator. Calling this method
     * will cause the declared value to be used instead.
     *
     * @param value The delay of ensuing property animations, in milliseconds. The value
     * cannot be negative.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public @NonNull ViewPropertyAnimatorCompat setStartDelay(long value) {
        View view;
        if ((view = mView.get()) != null) {
            view.animate().setStartDelay(value);
        }
        return this;
    }

    /**
     * Returns the current startDelay of property animations. If the startDelay was set on this
     * object, that value is returned. Otherwise, the default value of the underlying Animator
     * is returned.
     *
     * @see #setStartDelay(long)
     * @return The startDelay of animations, in milliseconds.
     */
    public long getStartDelay() {
        View view;
        if ((view = mView.get()) != null) {
            return view.animate().getStartDelay();
        } else {
            return 0;
        }
    }

    /**
     * This method will cause the View's <code>rotation</code> property to be animated to the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The value to be animated to.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public @NonNull ViewPropertyAnimatorCompat rotation(float value) {
        View view;
        if ((view = mView.get()) != null) {
            view.animate().rotation(value);
        }
        return this;
    }

    /**
     * This method will cause the View's <code>rotation</code> property to be animated by the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The amount to be animated by, as an offset from the current value.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public @NonNull ViewPropertyAnimatorCompat rotationBy(float value) {
        View view;
        if ((view = mView.get()) != null) {
            view.animate().rotationBy(value);
        }
        return this;
    }

    /**
     * This method will cause the View's <code>rotationX</code> property to be animated to the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The value to be animated to.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public @NonNull ViewPropertyAnimatorCompat rotationX(float value) {
        View view;
        if ((view = mView.get()) != null) {
            view.animate().rotationX(value);
        }
        return this;
    }

    /**
     * This method will cause the View's <code>rotationX</code> property to be animated by the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The amount to be animated by, as an offset from the current value.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public @NonNull ViewPropertyAnimatorCompat rotationXBy(float value) {
        View view;
        if ((view = mView.get()) != null) {
            view.animate().rotationXBy(value);
        }
        return this;
    }

    /**
     * This method will cause the View's <code>rotationY</code> property to be animated to the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The value to be animated to.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public @NonNull ViewPropertyAnimatorCompat rotationY(float value) {
        View view;
        if ((view = mView.get()) != null) {
            view.animate().rotationY(value);
        }
        return this;
    }

    /**
     * This method will cause the View's <code>rotationY</code> property to be animated by the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The amount to be animated by, as an offset from the current value.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public @NonNull ViewPropertyAnimatorCompat rotationYBy(float value) {
        View view;
        if ((view = mView.get()) != null) {
            view.animate().rotationYBy(value);
        }
        return this;
    }

    /**
     * This method will cause the View's <code>scaleX</code> property to be animated to the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The value to be animated to.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public @NonNull ViewPropertyAnimatorCompat scaleX(float value) {
        View view;
        if ((view = mView.get()) != null) {
            view.animate().scaleX(value);
        }
        return this;
    }

    /**
     * This method will cause the View's <code>scaleX</code> property to be animated by the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The amount to be animated by, as an offset from the current value.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public @NonNull ViewPropertyAnimatorCompat scaleXBy(float value) {
        View view;
        if ((view = mView.get()) != null) {
            view.animate().scaleXBy(value);
        }
        return this;
    }

    /**
     * This method will cause the View's <code>scaleY</code> property to be animated to the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The value to be animated to.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public @NonNull ViewPropertyAnimatorCompat scaleY(float value) {
        View view;
        if ((view = mView.get()) != null) {
            view.animate().scaleY(value);
        }
        return this;
    }

    /**
     * This method will cause the View's <code>scaleY</code> property to be animated by the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The amount to be animated by, as an offset from the current value.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public @NonNull ViewPropertyAnimatorCompat scaleYBy(float value) {
        View view;
        if ((view = mView.get()) != null) {
            view.animate().scaleYBy(value);
        }
        return this;
    }

    /**
     * Cancels all property animations that are currently running or pending.
     */
    public void cancel() {
        View view;
        if ((view = mView.get()) != null) {
            view.animate().cancel();
        }
    }

    /**
     * This method will cause the View's <code>x</code> property to be animated to the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The value to be animated to.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public @NonNull ViewPropertyAnimatorCompat x(float value) {
        View view;
        if ((view = mView.get()) != null) {
            view.animate().x(value);
        }
        return this;
    }

    /**
     * This method will cause the View's <code>x</code> property to be animated by the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The amount to be animated by, as an offset from the current value.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public @NonNull ViewPropertyAnimatorCompat xBy(float value) {
        View view;
        if ((view = mView.get()) != null) {
            view.animate().xBy(value);
        }
        return this;
    }

    /**
     * This method will cause the View's <code>y</code> property to be animated to the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The value to be animated to.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public @NonNull ViewPropertyAnimatorCompat y(float value) {
        View view;
        if ((view = mView.get()) != null) {
            view.animate().y(value);
        }
        return this;
    }

    /**
     * This method will cause the View's <code>y</code> property to be animated by the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The amount to be animated by, as an offset from the current value.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public @NonNull ViewPropertyAnimatorCompat yBy(float value) {
        View view;
        if ((view = mView.get()) != null) {
            view.animate().yBy(value);
        }
        return this;
    }

    /**
     * This method will cause the View's <code>translationX</code> property to be animated by the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The amount to be animated by, as an offset from the current value.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public @NonNull ViewPropertyAnimatorCompat translationXBy(float value) {
        View view;
        if ((view = mView.get()) != null) {
            view.animate().translationXBy(value);
        }
        return this;
    }

    /**
     * This method will cause the View's <code>translationY</code> property to be animated by the
     * specified value. Animations already running on the property will be canceled.
     *
     * @param value The amount to be animated by, as an offset from the current value.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public @NonNull ViewPropertyAnimatorCompat translationYBy(float value) {
        View view;
        if ((view = mView.get()) != null) {
            view.animate().translationYBy(value);
        }
        return this;
    }

    /**
     * This method will cause the View's <code>translationZ</code> property to be animated by the
     * specified value. Animations already running on the property will be canceled.
     *
     * <p>Prior to API 21, this method will do nothing.</p>
     *
     * @param value The amount to be animated by, as an offset from the current value.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public @NonNull ViewPropertyAnimatorCompat translationZBy(float value) {
        View view;
        if ((view = mView.get()) != null) {
            ViewPropertyAnimator animator = view.animate();
            animator.translationZBy(value);
        }
        return this;
    }

    /**
     * This method will cause the View's <code>translationZ</code> property to be animated to the
     * specified value. Animations already running on the property will be canceled.
     *
     * <p>Prior to API 21, this method will do nothing.</p>
     *
     * @param value The amount to be animated by, as an offset from the current value.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public @NonNull ViewPropertyAnimatorCompat translationZ(float value) {
        View view;
        if ((view = mView.get()) != null) {
            ViewPropertyAnimator animator = view.animate();
            animator.translationZ(value);
        }
        return this;
    }

    /**
     * This method will cause the View's <code>z</code> property to be animated to the
     * specified value. Animations already running on the property will be canceled.
     *
     * <p>Prior to API 21, this method will do nothing.</p>
     *
     * @param value The amount to be animated by, as an offset from the current value.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public @NonNull ViewPropertyAnimatorCompat z(float value) {
        View view;
        if ((view = mView.get()) != null) {
            ViewPropertyAnimator animator = view.animate();
            animator.z(value);
        }
        return this;
    }

    /**
     * This method will cause the View's <code>z</code> property to be animated by the
     * specified value. Animations already running on the property will be canceled.
     *
     * <p>Prior to API 21, this method will do nothing.</p>
     *
     * @param value The amount to be animated by, as an offset from the current value.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public @NonNull ViewPropertyAnimatorCompat zBy(float value) {
        View view;
        if ((view = mView.get()) != null) {
            ViewPropertyAnimator animator = view.animate();
            animator.zBy(value);
        }
        return this;
    }

    /**
     * Starts the currently pending property animations immediately. Calling <code>start()</code>
     * is optional because all animations start automatically at the next opportunity. However,
     * if the animations are needed to start immediately and synchronously (not at the time when
     * the next event is processed by the hierarchy, which is when the animations would begin
     * otherwise), then this method can be used.
     */
    public void start() {
        View view;
        if ((view = mView.get()) != null) {
            view.animate().start();
        }
    }

    /**
     * The View associated with this ViewPropertyAnimator will have its
     * {@link View#setLayerType(int, Paint) layer type} set to
     * {@link View#LAYER_TYPE_HARDWARE} for the duration of the next animation.
     * As stated in the documentation for {@link View#LAYER_TYPE_HARDWARE},
     * the actual type of layer used internally depends on the runtime situation of the
     * view. If the activity and this view are hardware-accelerated, then the layer will be
     * accelerated as well. If the activity or the view is not accelerated, then the layer will
     * effectively be the same as {@link View#LAYER_TYPE_SOFTWARE}.
     *
     * <p>This state is not persistent, either on the View or on this ViewPropertyAnimator: the
     * layer type of the View will be restored when the animation ends to what it was when this
     * method was called, and this setting on ViewPropertyAnimator is only valid for the next
     * animation. Note that calling this method and then independently setting the layer type of
     * the View (by a direct call to
     * {@link View#setLayerType(int, Paint)}) will result in some
     * inconsistency, including having the layer type restored to its pre-withLayer()
     * value when the animation ends.</p>
     *
     * @see View#setLayerType(int, Paint)
     * @return This object, allowing calls to methods in this class to be chained.
     */
    @SuppressLint("WrongConstant")
    public @NonNull ViewPropertyAnimatorCompat withLayer() {
        View view;
        if ((view = mView.get()) != null) {
            ViewPropertyAnimator animator = view.animate();
            animator.withLayer();
        }
        return this;
    }

    /**
     * Specifies an action to take place when the next animation runs. If there is a
     * {@link #setStartDelay(long) startDelay} set on this ViewPropertyAnimator, then the
     * action will run after that startDelay expires, when the actual animation begins.
     * This method, along with {@link #withEndAction(Runnable)}, is intended to help facilitate
     * choreographing ViewPropertyAnimator animations with other animations or actions
     * in the application.
     *
     * @param runnable The action to run when the next animation starts.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public @NonNull ViewPropertyAnimatorCompat withStartAction(@NonNull Runnable runnable) {
        View view;
        if ((view = mView.get()) != null) {
            ViewPropertyAnimator animator = view.animate();
            animator.withStartAction(runnable);
        }
        return this;
    }

    /**
     * Sets a listener for events in the underlying Animators that run the property
     * animations.
     *
     * @param listener The listener to be called with AnimatorListener events. A value of
     * <code>null</code> removes any existing listener.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public @NonNull ViewPropertyAnimatorCompat setListener(
            final @Nullable ViewPropertyAnimatorListener listener) {
        final View view;
        if ((view = mView.get()) != null) {
            setListenerInternal(view, listener);
        }
        return this;
    }

    private void setListenerInternal(final View view, final ViewPropertyAnimatorListener listener) {
        if (listener != null) {
            view.animate().setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationCancel(Animator animation) {
                    listener.onAnimationCancel(view);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    listener.onAnimationEnd(view);
                }

                @Override
                public void onAnimationStart(Animator animation) {
                    listener.onAnimationStart(view);
                }
            });
        } else {
            view.animate().setListener(null);
        }
    }

    /**
     * Sets a listener for update events in the underlying Animator that runs
     * the property animations.
     *
     * @param listener The listener to be called with update events. A value of
     * <code>null</code> removes any existing listener.
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public @NonNull ViewPropertyAnimatorCompat setUpdateListener(
            final @Nullable ViewPropertyAnimatorUpdateListener listener) {
        final View view;
        if ((view = mView.get()) != null) {
            ValueAnimator.AnimatorUpdateListener wrapped = null;
            if (listener != null) {
                    wrapped = valueAnimator -> listener.onAnimationUpdate(view);
                }
            ViewPropertyAnimator animator = view.animate();
            animator.setUpdateListener(wrapped);
        }
        return this;
    }
}
