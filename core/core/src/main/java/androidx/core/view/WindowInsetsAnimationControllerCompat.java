/*
 * Copyright 2020 The Android Open Source Project
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

import android.os.Build;
import android.view.View;
import android.view.WindowInsetsAnimationController;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat.Type;
import androidx.core.view.WindowInsetsCompat.Type.InsetsType;


/**
 * Controller for app-driven animation of system windows.
 * <p>
 * {@code WindowInsetsAnimationController} lets apps animate system windows such as
 * the {@link android.inputmethodservice.InputMethodService IME}. The animation is
 * synchronized, such that changes the system windows and the app's current frame
 * are rendered at the same time.
 * <p>
 * Control is obtained through {@link WindowInsetsControllerCompat#controlWindowInsetsAnimation}.
 */
public final class WindowInsetsAnimationControllerCompat {

    private final Impl mImpl;

    WindowInsetsAnimationControllerCompat() {
        if (Build.VERSION.SDK_INT < 30) {
            mImpl = new Impl();
        } else {
            throw new UnsupportedOperationException("On API 30+, the constructor taking a "
                    + WindowInsetsAnimationController.class.getSimpleName() + " as parameter");
        }
    }

    @RequiresApi(30)
    WindowInsetsAnimationControllerCompat(
            @NonNull WindowInsetsAnimationController controller) {
        mImpl = new Impl30(controller);
    }

    /**
     * Retrieves the {@link Insets} when the windows this animation is controlling are fully hidden.
     * <p>
     * Note that these insets are always relative to the window, which is the same as being relative
     * to {@link View#getRootView}
     * <p>
     * If there are any animation listeners registered, this value is the same as
     * {@link WindowInsetsAnimationCompat.BoundsCompat#getLowerBound()} that is being be passed
     * into the root view of the hierarchy.
     *
     * @return Insets when the windows this animation is controlling are fully hidden.
     * @see WindowInsetsAnimationCompat.BoundsCompat#getLowerBound()
     */
    @NonNull
    public Insets getHiddenStateInsets() {
        return mImpl.getHiddenStateInsets();
    }

    /**
     * Retrieves the {@link Insets} when the windows this animation is
     * controlling are fully shown.
     * <p>
     * Note that these insets are always relative to the window, which is the same as being relative
     * to {@link View#getRootView}
     * <p>
     * If there are any animation listeners registered, this value is the same as
     * {@link WindowInsetsAnimationCompat.BoundsCompat#getUpperBound()} that is being passed
     * into the root view of hierarchy.
     *
     * @return Insets when the windows this animation is controlling are fully shown.
     * @see WindowInsetsAnimationCompat.BoundsCompat#getUpperBound()
     */
    @NonNull
    public Insets getShownStateInsets() {
        return mImpl.getShownStateInsets();
    }

    /**
     * Retrieves the current insets.
     * <p>
     * Note that these insets are always relative to the window, which is the same as
     * being relative
     * to {@link View#getRootView}
     *
     * @return The current insets on the currently showing frame. These insets will change as the
     * animation progresses to reflect the current insets provided by the controlled window.
     */
    @NonNull
    public Insets getCurrentInsets() {
        return mImpl.getCurrentInsets();
    }

    /**
     * Returns the progress as previously set by {@code fraction} in {@link #setInsetsAndAlpha}
     *
     * @return the progress of the animation, where {@code 0} is fully hidden and {@code 1} is
     * fully shown.
     * <p>
     * Note: this value represents raw overall progress of the animation
     * i.e. the combined progress of insets and alpha.
     * <p>
     */
    @FloatRange(from = 0f, to = 1f)
    public float getCurrentFraction() {
        return mImpl.getCurrentFraction();
    }

    /**
     * Current alpha value of the window.
     *
     * @return float value between 0 and 1.
     */
    public float getCurrentAlpha() {
        return mImpl.getCurrentAlpha();
    }

    /**
     * @return The {@link Type}s this object is currently controlling.
     */
    @InsetsType
    public int getTypes() {
        return mImpl.getTypes();
    }

    /**
     * Modifies the insets for the frame being drawn by indirectly moving the windows around in the
     * system that are causing window insets.
     * <p>
     * Note that these insets are always relative to the window, which is the same as being relative
     * to {@link View#getRootView}
     * <p>
     * Also note that this will <b>not</b> inform the view system of a full inset change via
     * {@link View#dispatchApplyWindowInsets} in order to avoid a full layout pass during the
     * animation. If you'd like to animate views during a window inset animation, register a
     * {@link WindowInsetsAnimationCompat.Callback} by calling
     *
     * {@link ViewCompat#setWindowInsetsAnimationCallback(View,
     * WindowInsetsAnimationCompat.Callback)}
     * that will be notified about any insets change via
     * {@link WindowInsetsAnimationCompat.Callback#onProgress} during the animation.
     * <p>
     * {@link View#dispatchApplyWindowInsets} will instead be called once the animation has
     * finished, i.e. once {@link #finish} has been called.
     * Note: If there are no insets, alpha animation is still applied.
     *
     * @param insets   The new insets to apply. Based on the requested insets, the system will
     *                 calculate the positions of the windows in the system causing insets such that
     *                 the resulting insets of that configuration will match the passed in
     *                 parameter.
     *                 Note that these insets are being clamped to the range from
     *                 {@link #getHiddenStateInsets} to {@link #getShownStateInsets}.
     *                 If you intend on changing alpha only, pass null or
     *                 {@link #getCurrentInsets()}.
     * @param alpha    The new alpha to apply to the inset side.
     * @param fraction instantaneous animation progress. This value is dispatched to
     *                 {@link WindowInsetsAnimationCompat.Callback}.
     * @see WindowInsetsAnimationCompat.Callback
     * @see ViewCompat#setWindowInsetsAnimationCallback(View, WindowInsetsAnimationCompat.Callback)
     */

    public void setInsetsAndAlpha(@Nullable Insets insets,
            @FloatRange(from = 0f, to = 1f) float alpha,
            @FloatRange(from = 0f, to = 1f) float fraction) {
        mImpl.setInsetsAndAlpha(insets, alpha, fraction);
    }

    /**
     * Finishes the animation, and leaves the windows shown or hidden.
     * <p>
     * After invoking {@link #finish}, this instance is no longer {@link #isReady ready}.
     * <p>
     * Note: Finishing an animation implicitly {@link #setInsetsAndAlpha sets insets and alpha}
     * according to the requested end state without any further animation.
     *
     * @param shown if {@code true}, the windows will be shown after finishing the
     *              animation. Otherwise they will be hidden.
     */
    public void finish(boolean shown) {
        mImpl.finish(shown);
    }

    /**
     * Returns whether this instance is ready to be used to control window insets.
     * <p>
     * Instances are ready when passed in {@link WindowInsetsAnimationControlListenerCompat#onReady}
     * and stop being ready when it is either {@link #isFinished() finished} or
     * {@link #isCancelled() cancelled}.
     *
     * @return {@code true} if the instance is ready, {@code false} otherwise.
     */

    public boolean isReady() {
        return !isFinished() && !isCancelled();
    }

    /**
     * Returns whether this instance has been finished by a call to {@link #finish}.
     *
     * @return {@code true} if the instance is finished, {@code false} otherwise.
     * @see WindowInsetsAnimationControlListenerCompat#onFinished
     */
    public boolean isFinished() {
        return mImpl.isFinished();
    }

    /**
     * Returns whether this instance has been cancelled by the system, or by invoking the
     * {@link android.os.CancellationSignal} passed into
     * {@link WindowInsetsControllerCompat#controlWindowInsetsAnimation}.
     *
     * @return {@code true} if the instance is cancelled, {@code false} otherwise.
     * @see WindowInsetsAnimationControlListenerCompat#onCancelled
     */
    public boolean isCancelled() {
        return mImpl.isCancelled();
    }

    private static class Impl {
        Impl() {
            //privatex
        }

        @NonNull
        public Insets getHiddenStateInsets() {
            return Insets.NONE;
        }

        @NonNull
        public Insets getShownStateInsets() {
            return Insets.NONE;
        }

        @NonNull
        public Insets getCurrentInsets() {
            return Insets.NONE;
        }

        @FloatRange(from = 0f, to = 1f)
        public float getCurrentFraction() {
            return 0f;
        }

        public float getCurrentAlpha() {
            return 0f;
        }

        @InsetsType
        public int getTypes() {
            return 0;
        }

        public void setInsetsAndAlpha(@Nullable Insets insets,
                @FloatRange(from = 0f, to = 1f) float alpha,
                @FloatRange(from = 0f, to = 1f) float fraction) {
        }

        void finish(boolean shown) {
        }

        public boolean isReady() {
            return false;
        }

        boolean isFinished() {
            return false;
        }

        boolean isCancelled() {
            return true;
        }
    }

    @RequiresApi(30)
    private static class Impl30 extends Impl {

        private final WindowInsetsAnimationController mController;

        Impl30(@NonNull WindowInsetsAnimationController controller) {
            mController = controller;
        }

        @NonNull
        @Override
        public Insets getHiddenStateInsets() {
            return Insets.toCompatInsets(mController.getHiddenStateInsets());
        }

        @NonNull
        @Override
        public Insets getShownStateInsets() {
            return Insets.toCompatInsets(mController.getShownStateInsets());
        }

        @NonNull
        @Override
        public Insets getCurrentInsets() {
            return Insets.toCompatInsets(mController.getCurrentInsets());
        }

        @Override
        public float getCurrentFraction() {
            return mController.getCurrentFraction();
        }

        @Override
        public float getCurrentAlpha() {
            return mController.getCurrentAlpha();
        }

        @Override
        public int getTypes() {
            return mController.getTypes();
        }

        @Override
        public void setInsetsAndAlpha(@Nullable Insets insets, float alpha, float fraction) {
            mController.setInsetsAndAlpha(insets == null ? null : insets.toPlatformInsets(),
                    alpha,
                    fraction
            );
        }

        @Override
        void finish(boolean shown) {
            mController.finish(shown);
        }

        @Override
        public boolean isReady() {
            return mController.isReady();
        }

        @Override
        boolean isFinished() {
            return mController.isFinished();
        }

        @Override
        boolean isCancelled() {
            return mController.isCancelled();
        }
    }
}

