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

import android.content.Context;
import android.graphics.Insets;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsAnimationController;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.view.WindowInsetsCompat.Type.InsetsType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Provide simple controls of windows that generate insets.
 *
 * For SDKs >= 30, this class is a simple wrapper around {@link WindowInsetsController}. For
 * lower SDKs, this class aims to behave as close as possible to the original implementation.
 */
public final class WindowInsetsControllerCompat {

    /**
     * The default option for {@link #setSystemBarsBehavior(int)}. System bars will be forcibly
     * shown on any user interaction on the corresponding display if navigation bars are
     * hidden by
     * {@link #hide(int)} or
     * {@link WindowInsetsAnimationController#setInsetsAndAlpha(Insets, float, float)}.
     */
    public static final int BEHAVIOR_SHOW_BARS_BY_TOUCH = 0;

    /**
     * Option for {@link #setSystemBarsBehavior(int)}: Window would like to remain
     * interactive when
     * hiding navigation bars by calling {@link #hide(int)} or
     * {@link WindowInsetsAnimationController#setInsetsAndAlpha(Insets, float, float)}.
     *
     * <p>When system bars are hidden in this mode, they can be revealed with system
     * gestures, such
     * as swiping from the edge of the screen where the bar is hidden from.</p>
     */
    public static final int BEHAVIOR_SHOW_BARS_BY_SWIPE = 1;

    /**
     * Option for {@link #setSystemBarsBehavior(int)}: Window would like to remain
     * interactive when
     * hiding navigation bars by calling {@link #hide(int)} or
     * {@link WindowInsetsAnimationController#setInsetsAndAlpha(Insets, float, float)}.
     *
     * <p>When system bars are hidden in this mode, they can be revealed temporarily with system
     * gestures, such as swiping from the edge of the screen where the bar is hidden from. These
     * transient system bars will overlay app’s content, may have some degree of
     * transparency, and
     * will automatically hide after a short timeout.</p>
     */
    public static final int BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE = 2;

    private final Impl mImpl;

    @RequiresApi(30)
    private WindowInsetsControllerCompat(@NonNull WindowInsetsController insetsController) {
        if (SDK_INT >= 30) {
            mImpl = new Impl30(insetsController);
        } else {
            mImpl = new Impl();
        }
    }

    public WindowInsetsControllerCompat(@NonNull Window window, @NonNull View view) {
        if (SDK_INT >= 30) {
            mImpl = new Impl30(window);
        } else if (SDK_INT >= 26) {
            mImpl = new Impl26(window, view);
        } else if (SDK_INT >= 23) {
            mImpl = new Impl23(window, view);
        } else if (SDK_INT >= 20) {
            mImpl = new Impl20(window, view);
        } else {
            mImpl = new Impl();
        }
    }

    /**
     * Wrap a {@link WindowInsetsController} into a {@link WindowInsetsControllerCompat} for
     * compatibility purpose.
     *
     * @param insetsController The {@link WindowInsetsController} to wrap.
     * @return The provided {@link WindowInsetsControllerCompat} wrapped into a
     * {@link WindowInsetsControllerCompat}
     */
    @NonNull
    @RequiresApi(30)
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
    @Behavior
    public int getSystemBarsBehavior() {
        return mImpl.getSystemBarsBehavior();
    }

    private static class Impl {
        Impl() {
            //privatex
        }

        void show(int types) {
        }

        void hide(int types) {
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
    }

    @RequiresApi(20)
    private static class Impl20 extends Impl {

        @NonNull
        protected final Window mWindow;

        @Nullable
        private final View mView;

        Impl20(@NonNull Window window, @Nullable View view) {
            mWindow = window;
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


                    if (view != null && (view.isInEditMode() || view.onCheckIsTextEditor())) {
                        // The IME needs a text view to be focused to be shown
                        // The view given to retrieve this controller is a textView so we can assume
                        // that we can focus it in order to show the IME
                        view.requestFocus();
                    } else {
                        view = mWindow.getCurrentFocus();
                    }

                    // Fallback on the container view
                    if (view == null) {
                        view = mWindow.findViewById(android.R.id.content);
                    }

                    if (view != null && view.hasWindowFocus()) {
                        final View finalView = view;
                        finalView.post(new Runnable() {
                            @Override
                            public void run() {
                                InputMethodManager imm =
                                        (InputMethodManager) finalView.getContext()
                                                .getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.showSoftInput(finalView, 0);

                            }
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
                    setWindowFlag(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    return;
                case WindowInsetsCompat.Type.NAVIGATION_BARS:
                    setSystemUiFlag(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                    return;
                case WindowInsetsCompat.Type.IME:
                    ((InputMethodManager) mWindow.getContext()
                            .getSystemService(Context.INPUT_METHOD_SERVICE))
                            .hideSoftInputFromWindow(mWindow.getDecorView().getWindowToken(),
                                    0);
            }
        }

        protected void setSystemUiFlag(int systemUiFlag) {
            View decorView = mWindow.getDecorView();
            decorView.setSystemUiVisibility(
                    decorView.getSystemUiVisibility()
                            | systemUiFlag);
        }

        protected void unsetSystemUiFlag(int systemUiFlag) {
            View decorView = mWindow.getDecorView();
            decorView.setSystemUiVisibility(
                    decorView.getSystemUiVisibility()
                            & ~systemUiFlag);
        }

        protected void setWindowFlag(int windowFlag) {
            mWindow.addFlags(windowFlag);
        }

        protected void unsetWindowFlag(int windowFlag) {
            mWindow.clearFlags(windowFlag);
        }

        @Override
        void setSystemBarsBehavior(int behavior) {
        }

        @Override
        int getSystemBarsBehavior() {
            return 0;
        }
    }

    @RequiresApi(23)
    private static class Impl23 extends Impl20 {

        Impl23(@NonNull Window window, @Nullable View view) {
            super(window, view);
        }

        @Override
        public boolean isAppearanceLightStatusBars() {
            return (mWindow.getDecorView().getSystemUiVisibility()
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

        Impl26(@NonNull Window window, @Nullable View view) {
            super(window, view);
        }

        @Override
        public boolean isAppearanceLightNavigationBars() {
            return (mWindow.getDecorView().getSystemUiVisibility()
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

        private final WindowInsetsController mInsetsController;

        Impl30(Window window) {
            mInsetsController = window.getInsetsController();
        }

        Impl30(WindowInsetsController insetsController) {
            mInsetsController = insetsController;
        }

        @Override
        void show(@InsetsType int types) {
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
        @Override
        @Behavior
        int getSystemBarsBehavior() {
            return mInsetsController.getSystemBarsBehavior();
        }
    }
}
