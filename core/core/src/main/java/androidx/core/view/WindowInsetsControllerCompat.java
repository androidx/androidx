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

import static android.os.Build.VERSION.SDK_INT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.os.CancellationSignal;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsAnimationControlListener;
import android.view.WindowInsetsAnimationController;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.collection.SimpleArrayMap;
import androidx.core.graphics.Insets;
import androidx.core.util.Preconditions;
import androidx.core.view.WindowInsetsCompat.Type.InsetsType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.TimeUnit;

/**
 * Provide simple controls of windows that generate insets.
 *
 * For SDKs >= 30, this class is a simple wrapper around {@link WindowInsetsController}. For
 * lower SDKs, this class aims to behave as close as possible to the original implementation.
 */
public final class WindowInsetsControllerCompat {

    /**
     * The default option for {@link #setSystemBarsBehavior(int)}. System bars will be forcibly
     * shown on any user interaction on the corresponding display if navigation bars are hidden
     * by {@link #hide(int)} or
     * {@link WindowInsetsAnimationControllerCompat#setInsetsAndAlpha(Insets, float, float)}.
     */
    public static final int BEHAVIOR_SHOW_BARS_BY_TOUCH = 0;

    /**
     * Option for {@link #setSystemBarsBehavior(int)}: Window would like to remain interactive
     * when hiding navigation bars by calling {@link #hide(int)} or
     * {@link WindowInsetsAnimationControllerCompat#setInsetsAndAlpha(Insets, float, float)}.
     * <p>
     * When system bars are hidden in this mode, they can be revealed with system
     * gestures, such as swiping from the edge of the screen where the bar is hidden from.
     */
    public static final int BEHAVIOR_SHOW_BARS_BY_SWIPE = 1;

    /**
     * Option for {@link #setSystemBarsBehavior(int)}: Window would like to remain
     * interactive when hiding navigation bars by calling {@link #hide(int)} or
     * {@link WindowInsetsAnimationControllerCompat#setInsetsAndAlpha(Insets, float, float)}.
     * <p>
     * When system bars are hidden in this mode, they can be revealed temporarily with system
     * gestures, such as swiping from the edge of the screen where the bar is hidden from. These
     * transient system bars will overlay app’s content, may have some degree of
     * transparency, and will automatically hide after a short timeout.
     */
    public static final int BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE = 2;

    private final Impl mImpl;

    /**
     * This version fails to workaround
     * <a href="https://issuetracker.google.com/issues/180881870">
     *     https://issuetracker.google.com/issues/180881870
     * </a>, but is present for backwards compatibility.
     */
    @RequiresApi(30)
    @Deprecated
    private WindowInsetsControllerCompat(@NonNull WindowInsetsController insetsController) {
        mImpl = new Impl30(insetsController, this);
    }

    WindowInsetsControllerCompat(@NonNull View view) {
        if (SDK_INT >= 30) {
            mImpl = new Impl30(view, this);
        } else if (SDK_INT >= 26) {
            mImpl = new Impl26(view);
        } else if (SDK_INT >= 23) {
            mImpl = new Impl23(view);
        } else if (SDK_INT >= 20) {
            mImpl = new Impl20(view);
        } else {
            mImpl = new Impl();
        }
    }

    /**
     * @deprecated Use {@link ViewCompat#getWindowInsetsController(View)} instead
     */
    @Deprecated
    public WindowInsetsControllerCompat(@NonNull Window window, @NonNull View view) {
        this(view);
    }

    /**
     * Wrap a {@link WindowInsetsController} into a {@link WindowInsetsControllerCompat} for
     * compatibility purpose.
     *
     * @param insetsController The {@link WindowInsetsController} to wrap.
     * @return The provided {@link WindowInsetsController} wrapped into a
     * {@link WindowInsetsControllerCompat}
     * @deprecated Use {@link ViewCompat#getWindowInsetsController(View)} instead
     */
    @NonNull
    @RequiresApi(30)
    @Deprecated
    public static WindowInsetsControllerCompat toWindowInsetsControllerCompat(
            @NonNull WindowInsetsController insetsController) {
        return new WindowInsetsControllerCompat(insetsController);
    }

    /**
     * Determines the behavior of system bars when hiding them by calling {@link #hide}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {BEHAVIOR_SHOW_BARS_BY_TOUCH, BEHAVIOR_SHOW_BARS_BY_SWIPE,
            BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE})
    @interface Behavior {
    }

    /**
     * Makes a set of windows that cause insets appear on screen.
     * <p>
     * Note that if the window currently doesn't have control over a certain type, it will apply the
     * change as soon as the window gains control. The app can listen to the event by observing
     * {@link View#onApplyWindowInsets} and checking visibility with {@link WindowInsets#isVisible}.
     *
     * @param types A bitmask of {@link WindowInsetsCompat.Type} specifying what windows the app
     *              would like to make appear on screen.
     */
    public void show(@InsetsType int types) {
        mImpl.show(types);
    }

    /**
     * Makes a set of windows causing insets disappear.
     * <p>
     * Note that if the window currently doesn't have control over a certain type, it will apply the
     * change as soon as the window gains control. The app can listen to the event by observing
     * {@link View#onApplyWindowInsets} and checking visibility with {@link WindowInsets#isVisible}.
     *
     * @param types A bitmask of {@link WindowInsetsCompat.Type} specifying what windows the app
     *              would like to make disappear.
     */
    public void hide(@InsetsType int types) {
        mImpl.hide(types);
    }


    /**
     * Checks if the foreground of the status bar is set to light.
     * <p>
     * This method always returns false on API < 23.
     *
     * @return true if the foreground is light
     * @see #setAppearanceLightStatusBars(boolean)
     */
    public boolean isAppearanceLightStatusBars() {
        return mImpl.isAppearanceLightStatusBars();
    }

    /**
     * If true, changes the foreground color of the status bars to light so that the items on the
     * bar can be read clearly. If false, reverts to the default appearance.
     * <p>
     * This method has no effect on API < 23.
     *
     * @see #isAppearanceLightStatusBars()
     */
    public void setAppearanceLightStatusBars(boolean isLight) {
        mImpl.setAppearanceLightStatusBars(isLight);
    }

    /**
     * Checks if the foreground of the navigation bar is set to light.
     * <p>
     * This method always returns false on API < 26.
     *
     * @return true if the foreground is light
     * @see #setAppearanceLightNavigationBars(boolean)
     */
    public boolean isAppearanceLightNavigationBars() {
        return mImpl.isAppearanceLightNavigationBars();
    }

    /**
     * If true, changes the foreground color of the navigation bars to light so that the items on
     * the bar can be read clearly. If false, reverts to the default appearance.
     * <p>
     * This method has no effect on API < 26.
     *
     * @see #isAppearanceLightNavigationBars()
     */
    public void setAppearanceLightNavigationBars(boolean isLight) {
        mImpl.setAppearanceLightNavigationBars(isLight);
    }

    /**
     * Lets the application control window inset animations in a frame-by-frame manner by
     * modifying the position of the windows in the system causing insets directly using
     * {@link WindowInsetsAnimationControllerCompat#setInsetsAndAlpha} in the controller provided
     * by the given listener.
     * <p>
     * This method only works on API >= 30 since there is no way to control the window in the
     * system on prior APIs.
     *
     * @param types              The {@link WindowInsetsCompat.Type}s the application has
     *                           requested to control.
     * @param durationMillis     Duration of animation in {@link TimeUnit#MILLISECONDS}, or -1 if
     *                           the animation doesn't have a predetermined duration. This value
     *                           will be passed to
     *                           {@link WindowInsetsAnimationCompat#getDurationMillis()}
     * @param interpolator       The interpolator used for this animation, or {@code null } if
     *                           this animation doesn't follow an interpolation curve. This value
     *                           will be passed to
     *                           {@link WindowInsetsAnimationCompat#getInterpolator()} and used
     *                           to calculate
     *                           {@link WindowInsetsAnimationCompat#getInterpolatedFraction()}.
     * @param cancellationSignal A cancellation signal that the caller can use to cancel the
     *                           request to obtain control, or once they have control, to cancel
     *                           the control.
     * @param listener           The {@link WindowInsetsAnimationControlListener} that gets
     *                           called when the windows are ready to be controlled, among other
     *                           callbacks.
     * @see WindowInsetsAnimationCompat#getFraction()
     * @see WindowInsetsAnimationCompat#getInterpolatedFraction()
     * @see WindowInsetsAnimationCompat#getInterpolator()
     * @see WindowInsetsAnimationCompat#getDurationMillis()
     */
    public void controlWindowInsetsAnimation(@InsetsType int types, long durationMillis,
            @Nullable Interpolator interpolator,
            @Nullable CancellationSignal cancellationSignal,
            @NonNull WindowInsetsAnimationControlListenerCompat listener) {
        mImpl.controlWindowInsetsAnimation(types,
                durationMillis,
                interpolator,
                cancellationSignal,
                listener);
    }

    /**
     * Controls the behavior of system bars.
     *
     * @param behavior Determines how the bars behave when being hidden by the application.
     * @see #getSystemBarsBehavior
     */
    public void setSystemBarsBehavior(@Behavior int behavior) {
        mImpl.setSystemBarsBehavior(behavior);
    }

    /**
     * Retrieves the requested behavior of system bars.
     *
     * @return the system bar behavior controlled by this window.
     * @see #setSystemBarsBehavior(int)
     */
    @SuppressLint("WrongConstant")
    @Behavior
    public int getSystemBarsBehavior() {
        return mImpl.getSystemBarsBehavior();
    }

    /**
     * Adds a {@link WindowInsetsController.OnControllableInsetsChangedListener} to the window
     * insets controller.
     *
     * @param listener The listener to add.
     * @see WindowInsetsControllerCompat.OnControllableInsetsChangedListener
     * @see #removeOnControllableInsetsChangedListener(
     *WindowInsetsControllerCompat.OnControllableInsetsChangedListener)
     */
    public void addOnControllableInsetsChangedListener(
            @NonNull WindowInsetsControllerCompat.OnControllableInsetsChangedListener listener) {
        mImpl.addOnControllableInsetsChangedListener(listener);
    }

    /**
     * Removes a {@link WindowInsetsController.OnControllableInsetsChangedListener} from the
     * window insets controller.
     *
     * @param listener The listener to remove.
     * @see WindowInsetsControllerCompat.OnControllableInsetsChangedListener
     * @see #addOnControllableInsetsChangedListener(
     *WindowInsetsControllerCompat.OnControllableInsetsChangedListener)
     */
    public void removeOnControllableInsetsChangedListener(
            @NonNull WindowInsetsControllerCompat.OnControllableInsetsChangedListener
                    listener) {
        mImpl.removeOnControllableInsetsChangedListener(listener);
    }

    /**
     * Listener to be notified when the set of controllable {@link WindowInsetsCompat.Type}
     * controlled by a {@link WindowInsetsController} changes.
     * <p>
     * Once a {@link WindowInsetsCompat.Type} becomes controllable, the app will be able to
     * control the window that is causing this type of insets by calling
     * {@link #controlWindowInsetsAnimation}.
     * <p>
     * Note: When listening to cancellability of the {@link WindowInsets.Type#ime},
     * {@link #controlWindowInsetsAnimation} may still fail in case the {@link InputMethodService}
     * decides to cancel the show request. This could happen when there is a hardware keyboard
     * attached.
     *
     * @see #addOnControllableInsetsChangedListener(
     *WindowInsetsControllerCompat.OnControllableInsetsChangedListener)
     * @see #removeOnControllableInsetsChangedListener(
     *WindowInsetsControllerCompat.OnControllableInsetsChangedListener)
     */
    public interface OnControllableInsetsChangedListener {

        /**
         * Called when the set of controllable {@link WindowInsetsCompat.Type} changes.
         *
         * @param controller The controller for which the set of controllable
         *                   {@link WindowInsetsCompat.Type}s
         *                   are changing.
         * @param typeMask   Bitwise behavior type-mask of the {@link WindowInsetsCompat.Type}s
         *                   the controller is currently able to control.
         */
        void onControllableInsetsChanged(@NonNull WindowInsetsControllerCompat controller,
                @InsetsType int typeMask);
    }

    private static class Impl {
        Impl() {
            //private
        }

        void show(int types) {
        }

        void hide(int types) {
        }

        void controlWindowInsetsAnimation(int types, long durationMillis,
                Interpolator interpolator, CancellationSignal cancellationSignal,
                WindowInsetsAnimationControlListenerCompat listener) {
        }

        void setSystemBarsBehavior(int behavior) {
        }

        int getSystemBarsBehavior() {
            return 0;
        }

        public boolean isAppearanceLightStatusBars() {
            return false;
        }

        public void setAppearanceLightStatusBars(boolean isLight) {
        }

        public boolean isAppearanceLightNavigationBars() {
            return false;
        }

        public void setAppearanceLightNavigationBars(boolean isLight) {
        }

        void addOnControllableInsetsChangedListener(
                WindowInsetsControllerCompat.OnControllableInsetsChangedListener listener) {
        }

        void removeOnControllableInsetsChangedListener(
                @NonNull WindowInsetsControllerCompat.OnControllableInsetsChangedListener
                        listener) {
        }
    }

    @RequiresApi(20)
    private static class Impl20 extends Impl {

        @NonNull
        protected final View mView;

        Impl20(@NonNull View view) {
            mView = view;
        }

        @Override
        void show(int typeMask) {
            for (int i = WindowInsetsCompat.Type.FIRST; i <= WindowInsetsCompat.Type.LAST;
                    i = i << 1) {
                if ((typeMask & i) == 0) {
                    continue;
                }
                showForType(i);
            }
        }

        private void showForType(int type) {
            switch (type) {
                case WindowInsetsCompat.Type.STATUS_BARS:
                    unsetSystemUiFlag(View.SYSTEM_UI_FLAG_FULLSCREEN);
                    unsetWindowFlag(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    return;
                case WindowInsetsCompat.Type.NAVIGATION_BARS:
                    unsetSystemUiFlag(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                    return;
                case WindowInsetsCompat.Type.IME:
                    // We'll try to find an available textView to focus to show the IME
                    View view = mView;


                    if (view.isInEditMode() || view.onCheckIsTextEditor()) {
                        // The IME needs a text view to be focused to be shown
                        // The view given to retrieve this controller is a textView so we can assume
                        // that we can focus it in order to show the IME
                        view.requestFocus();
                    } else {
                        view = mView.getRootView().findFocus();
                    }

                    if (view != null && view.hasWindowFocus()) {
                        final View finalView = view;
                        finalView.post(() -> {
                            InputMethodManager imm =
                                    (InputMethodManager) finalView.getContext()
                                            .getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.showSoftInput(finalView, 0);

                        });
                    }
            }
        }

        @Override
        void hide(int typeMask) {
            for (int i = WindowInsetsCompat.Type.FIRST; i <= WindowInsetsCompat.Type.LAST;
                    i = i << 1) {
                if ((typeMask & i) == 0) {
                    continue;
                }
                hideForType(i);
            }
        }

        private void hideForType(int type) {
            switch (type) {
                case WindowInsetsCompat.Type.STATUS_BARS:
                    setSystemUiFlag(View.SYSTEM_UI_FLAG_FULLSCREEN);
                    return;
                case WindowInsetsCompat.Type.NAVIGATION_BARS:
                    setSystemUiFlag(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                    return;
                case WindowInsetsCompat.Type.IME:
                    ((InputMethodManager) mView.getContext()
                            .getSystemService(Context.INPUT_METHOD_SERVICE))
                            .hideSoftInputFromWindow(mView.getRootView().getWindowToken(),
                                    0);
            }
        }

        protected void setSystemUiFlag(int systemUiFlag) {
            View decorView = mView.getRootView();
            decorView.setSystemUiVisibility(
                    decorView.getSystemUiVisibility()
                            | systemUiFlag);
        }

        protected void unsetSystemUiFlag(int systemUiFlag) {
            View decorView = mView.getRootView();
            decorView.setSystemUiVisibility(
                    decorView.getSystemUiVisibility()
                            & ~systemUiFlag);
        }

        protected void setWindowFlag(int windowFlag) {
            WindowManager.LayoutParams layoutParams =
                    (WindowManager.LayoutParams) mView.getRootView().getLayoutParams();
            layoutParams.flags |= windowFlag;
            mView.getRootView().setLayoutParams(layoutParams);
        }

        protected void unsetWindowFlag(int windowFlag) {
            WindowManager.LayoutParams layoutParams =
                    (WindowManager.LayoutParams) mView.getRootView().getLayoutParams();
            layoutParams.flags &= ~windowFlag;
            mView.getRootView().setLayoutParams(layoutParams);
        }

        @Override
        void controlWindowInsetsAnimation(int types, long durationMillis,
                Interpolator interpolator, CancellationSignal cancellationSignal,
                WindowInsetsAnimationControlListenerCompat listener) {
        }

        @Override
        void setSystemBarsBehavior(int behavior) {
            switch (behavior) {
                case BEHAVIOR_SHOW_BARS_BY_SWIPE:
                    unsetSystemUiFlag(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                    setSystemUiFlag(View.SYSTEM_UI_FLAG_IMMERSIVE);
                    break;
                case BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE:
                    unsetSystemUiFlag(View.SYSTEM_UI_FLAG_IMMERSIVE);
                    setSystemUiFlag(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                    break;
                case BEHAVIOR_SHOW_BARS_BY_TOUCH:
                    unsetSystemUiFlag(View.SYSTEM_UI_FLAG_IMMERSIVE
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                    break;
            }
        }

        @Override
        int getSystemBarsBehavior() {
            return 0;
        }

        @Override
        void addOnControllableInsetsChangedListener(
                WindowInsetsControllerCompat.OnControllableInsetsChangedListener listener) {
        }

        @Override
        void removeOnControllableInsetsChangedListener(
                @NonNull WindowInsetsControllerCompat.OnControllableInsetsChangedListener
                        listener) {
        }
    }

    @RequiresApi(23)
    private static class Impl23 extends Impl20 {

        Impl23(@NonNull View view) {
            super(view);
        }

        @Override
        public boolean isAppearanceLightStatusBars() {
            return (mView.getRootView().getSystemUiVisibility()
                    & View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) != 0;
        }

        @Override
        public void setAppearanceLightStatusBars(boolean isLight) {
            if (isLight) {
                unsetWindowFlag(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                setWindowFlag(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                setSystemUiFlag(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                unsetSystemUiFlag(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }

    @RequiresApi(26)
    private static class Impl26 extends Impl23 {

        Impl26(@NonNull View view) {
            super(view);
        }

        @Override
        public boolean isAppearanceLightNavigationBars() {
            return (mView.getRootView().getSystemUiVisibility()
                    & View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR) != 0;
        }

        @Override
        public void setAppearanceLightNavigationBars(boolean isLight) {
            if (isLight) {
                unsetWindowFlag(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                setWindowFlag(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                setSystemUiFlag(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
            } else {
                unsetSystemUiFlag(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
            }
        }
    }

    @RequiresApi(30)
    private static class Impl30 extends Impl {

        final WindowInsetsControllerCompat mCompatController;
        final WindowInsetsController mInsetsController;
        private final SimpleArrayMap<
                WindowInsetsControllerCompat.OnControllableInsetsChangedListener,
                WindowInsetsController.OnControllableInsetsChangedListener>
                mListeners = new SimpleArrayMap<>();

        protected View mView;

        Impl30(@NonNull View view, @NonNull WindowInsetsControllerCompat compatController) {
            this(
                    Preconditions.checkNotNull(view.getWindowInsetsController(),
                    "The insets controller is null. "
                            + "The root view might have been detached from its window"),
                    compatController);
            mView = view;
        }

        /**
         * This version fails to workaround
         * <a href="https://issuetracker.google.com/issues/180881870">
         *     https://issuetracker.google.com/issues/180881870
         * </a>, but is present for backwards compatibility.
         *
         * @deprecated Use {@link #Impl30(View, WindowInsetsControllerCompat) } instead
         */
        @Deprecated
        Impl30(@NonNull WindowInsetsController insetsController,
                @NonNull WindowInsetsControllerCompat compatController) {
            mInsetsController = insetsController;
            mCompatController = compatController;
        }

        @Override
        void show(@InsetsType int types) {
            if (mView != null && (types & WindowInsetsCompat.Type.IME) != 0 && SDK_INT < 32) {
                InputMethodManager imm =
                        (InputMethodManager) mView.getContext()
                                .getSystemService(Context.INPUT_METHOD_SERVICE);

                // This is a strange-looking workaround by making a call and ignoring the result.
                // We don't use the return value here, but isActive() has the side-effect of
                // calling a hidden method checkFocus(), which ensures that the IME state has the
                // correct view in some situations (especially when the focused view changes).
                // This is essentially a backport, since an equivalent checkFocus() call was
                // added in API 32 to improve behavior:
                // https://issuetracker.google.com/issues/189858204
                imm.isActive();
            }
            mInsetsController.show(types);
        }

        @Override
        void hide(@InsetsType int types) {
            mInsetsController.hide(types);
        }

        @Override
        public boolean isAppearanceLightStatusBars() {
            return (mInsetsController.getSystemBarsAppearance()
                    & WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS) != 0;
        }

        @Override
        public void setAppearanceLightStatusBars(boolean isLight) {
            if (isLight) {
                if (mView != null) {
                    unsetSystemUiFlag(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                }

                mInsetsController.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
            } else {
                mInsetsController.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
            }
        }

        @Override
        public boolean isAppearanceLightNavigationBars() {
            return (mInsetsController.getSystemBarsAppearance()
                    & WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS) != 0;
        }

        @Override
        public void setAppearanceLightNavigationBars(boolean isLight) {
            if (isLight) {
                mInsetsController.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            } else {
                mInsetsController.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }
        }

        @Override
        void controlWindowInsetsAnimation(@InsetsType int types, long durationMillis,
                @Nullable Interpolator interpolator,
                @Nullable CancellationSignal cancellationSignal,
                @NonNull final WindowInsetsAnimationControlListenerCompat listener) {

            WindowInsetsAnimationControlListener fwListener =
                    new WindowInsetsAnimationControlListener() {

                        private WindowInsetsAnimationControllerCompat mCompatAnimController = null;

                        @Override
                        public void onReady(@NonNull WindowInsetsAnimationController controller,
                                int types) {
                            mCompatAnimController =
                                    new WindowInsetsAnimationControllerCompat(controller);
                            listener.onReady(mCompatAnimController, types);
                        }

                        @Override
                        public void onFinished(
                                @NonNull WindowInsetsAnimationController controller) {
                            listener.onFinished(mCompatAnimController);
                        }

                        @Override
                        public void onCancelled(
                                @Nullable WindowInsetsAnimationController controller) {
                            listener.onCancelled(controller == null ? null : mCompatAnimController);
                        }
                    };

            mInsetsController.controlWindowInsetsAnimation(types,
                    durationMillis,
                    interpolator,
                    cancellationSignal,
                    fwListener);
        }

        /**
         * Controls the behavior of system bars.
         *
         * @param behavior Determines how the bars behave when being hidden by the application.
         * @see #getSystemBarsBehavior
         */
        @Override
        void setSystemBarsBehavior(@Behavior int behavior) {
            mInsetsController.setSystemBarsBehavior(behavior);
        }

        /**
         * Retrieves the requested behavior of system bars.
         *
         * @return the system bar behavior controlled by this window.
         * @see #setSystemBarsBehavior(int)
         */
        @SuppressLint("WrongConstant")
        @Override
        @Behavior
        int getSystemBarsBehavior() {
            return mInsetsController.getSystemBarsBehavior();
        }

        @Override
        void addOnControllableInsetsChangedListener(
                @NonNull final WindowInsetsControllerCompat.OnControllableInsetsChangedListener
                        listener) {

            if (mListeners.containsKey(listener)) {
                // The listener has already been added.
                return;
            }
            WindowInsetsController.OnControllableInsetsChangedListener
                    fwListener = (controller, typeMask) -> {
                        if (mInsetsController == controller) {
                            listener.onControllableInsetsChanged(
                                    mCompatController, typeMask);
                        }
                    };
            mListeners.put(listener, fwListener);
            mInsetsController.addOnControllableInsetsChangedListener(fwListener);
        }

        @Override
        void removeOnControllableInsetsChangedListener(
                @NonNull WindowInsetsControllerCompat.OnControllableInsetsChangedListener
                        listener) {
            WindowInsetsController.OnControllableInsetsChangedListener
                    fwListener = mListeners.remove(listener);
            if (fwListener != null) {
                mInsetsController.removeOnControllableInsetsChangedListener(fwListener);
            }
        }

        protected void unsetSystemUiFlag(int systemUiFlag) {
            View decorView = mView.getRootView();
            decorView.setSystemUiVisibility(
                    decorView.getSystemUiVisibility()
                            & ~systemUiFlag);
        }
    }
}
