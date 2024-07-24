/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.view;

import static androidx.camera.core.ImageCapture.FLASH_MODE_SCREEN;
import static androidx.camera.core.impl.utils.Threads.checkMainThread;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.UiThread;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCapture.ScreenFlash;
import androidx.camera.core.Logger;
import androidx.camera.view.internal.ScreenFlashUiInfo;
import androidx.fragment.app.Fragment;

/**
 * Custom View that implements a basic UI for screen flash photo capture.
 *
 * <p> This class provides an {@link ScreenFlash} implementation with
 * {@link #getScreenFlash()} for the
 * {@link ImageCapture#setScreenFlash(ImageCapture.ScreenFlash)} API. If a
 * {@link CameraController} is used for CameraX operations,{@link #setController(CameraController)}
 * should be used to set the controller to this view. Normally, this view is kept fully
 * transparent. It becomes fully visible for the duration of screen flash photo capture. The
 * screen brightness is also maximized for that duration.
 *
 * <p> The default color of the view is {@link Color#WHITE}, but it can be changed with
 * {@link View#setBackgroundColor(int)} API. The elevation of this view is always set to
 * {@link Float#MAX_VALUE} so that it always appears on top in its view hierarchy during screen
 * flash.
 *
 * <p> This view is also used internally in {@link PreviewView}, so may not be required if user
 * is already using {@link PreviewView}. However, note that the internal instance of
 * {@link PreviewView} has the same dimensions as {@link PreviewView}. So if the
 * {@link PreviewView} does not encompass the full screen, users may want to use this view
 * separately so that whole screen can be encompassed during screen flash operation.
 *
 * @see #getScreenFlash
 * @see ImageCapture#FLASH_MODE_SCREEN
 */
public final class ScreenFlashView extends View {
    private static final String TAG = "ScreenFlashView";
    private CameraController mCameraController;
    private Window mScreenFlashWindow;
    private ImageCapture.ScreenFlash mScreenFlash;

    /** The timeout in seconds for the visibility animation at {@link ScreenFlash#apply}. */
    private static final long ANIMATION_DURATION_MILLIS = 1000;

    @UiThread
    public ScreenFlashView(@NonNull Context context) {
        this(context, null);
    }

    @UiThread
    public ScreenFlashView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @UiThread
    public ScreenFlashView(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    @UiThread
    public ScreenFlashView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        setBackgroundColor(Color.WHITE);
        setAlpha(0f);
        setElevation(Float.MAX_VALUE);
    }

    /**
     * Sets the {@link CameraController}.
     *
     * <p> Once set, the controller will use the {@code ScreenFlashView} for screen flash related UI
     * operations.
     *
     * @throws IllegalStateException If {@link ImageCapture#FLASH_MODE_SCREEN} is set to the
     *                               {@link CameraController}, but a non-null {@link Window}
     *                               instance has not been set with {@link #setScreenFlashWindow}.
     * @see CameraController
     */
    @UiThread
    public void setController(@Nullable CameraController cameraController) {
        checkMainThread();

        if (mCameraController != null && mCameraController != cameraController) {
            // If already bound to a different controller, remove the ScreenFlash instance from the
            // old controller.
            setScreenFlashUiInfo(null);
        }
        mCameraController = cameraController;

        if (cameraController == null) {
            return;
        }

        if (cameraController.getImageCaptureFlashMode() == FLASH_MODE_SCREEN
                && mScreenFlashWindow == null) {
            throw new IllegalStateException(
                    "No window set despite setting FLASH_MODE_SCREEN in CameraController");
        }

        setScreenFlashUiInfo(getScreenFlash());
    }

    private void setScreenFlashUiInfo(ImageCapture.ScreenFlash control) {
        if (mCameraController == null) {
            Logger.d(TAG, "setScreenFlashUiInfo: mCameraController is null!");
            return;
        }
        mCameraController.setScreenFlashUiInfo(new ScreenFlashUiInfo(
                ScreenFlashUiInfo.ProviderType.SCREEN_FLASH_VIEW, control));
    }

    /**
     * Sets a {@link Window} instance for subsequent photo capture requests with
     * {@link ImageCapture} use case when {@link ImageCapture#FLASH_MODE_SCREEN} is set.
     *
     * <p>The calling of this API will take effect for {@code ImageCapture#FLASH_MODE_SCREEN} only
     * and the {@code Window} will be ignored for other flash modes. During screen flash photo
     * capture, the window is used for the purpose of changing screen brightness.
     *
     * <p> If the implementation provided by the user is no longer valid (e.g. due to any
     * {@link android.app.Activity} or {@link android.view.View} reference used in the
     * implementation becoming invalid), user needs to re-set a new valid window or clear the
     * previous one with {@code setScreenFlashWindow(null)}, whichever appropriate.
     *
     * <p>For most app scenarios, a {@code Window} instance can be obtained from
     * {@link Activity#getWindow()}. In case of a fragment, {@link Fragment#getActivity()} can
     * first be used to get the activity instance.
     *
     * @param screenFlashWindow A {@link Window} instance that is used to change the brightness
     *                          during screen flash photo capture.
     */
    @UiThread
    public void setScreenFlashWindow(@Nullable Window screenFlashWindow) {
        checkMainThread();
        updateScreenFlash(screenFlashWindow);
        mScreenFlashWindow = screenFlashWindow;
        setScreenFlashUiInfo(getScreenFlash());
    }

    /** Update {@link #mScreenFlash} if required. */
    private void updateScreenFlash(Window window) {
        if (mScreenFlashWindow != window) {
            mScreenFlash = window == null ? null : new ScreenFlash() {
                private float mPreviousBrightness;
                private ValueAnimator mAnimator;

                @Override
                public void apply(long expirationTimeMillis,
                        @NonNull ImageCapture.ScreenFlashListener screenFlashListener) {
                    Logger.d(TAG, "ScreenFlash#apply");

                    mPreviousBrightness = getBrightness();
                    setBrightness(1.0f);

                    if (mAnimator != null) {
                        mAnimator.cancel();
                    }
                    mAnimator = animateToFullOpacity(screenFlashListener::onCompleted);
                }

                @Override
                public void clear() {
                    Logger.d(TAG, "ScreenFlash#clearScreenFlashUi");

                    if (mAnimator != null) {
                        mAnimator.cancel();
                        mAnimator = null;
                    }

                    setAlpha(0f);

                    // Restore screen brightness
                    setBrightness(mPreviousBrightness);
                }
            };
        }
    }

    private ValueAnimator animateToFullOpacity(@Nullable Runnable onAnimationEnd) {
        Logger.d(TAG, "animateToFullOpacity");

        ValueAnimator animator = ValueAnimator.ofFloat(0F, 1F);

        // TODO: b/355168952 - Allow users to overwrite the animation duration.
        animator.setDuration(getVisibilityRampUpAnimationDurationMillis());

        animator.addUpdateListener(animation -> {
            Logger.d(TAG, "animateToFullOpacity: value = " + (float) animation.getAnimatedValue());
            setAlpha((float) animation.getAnimatedValue());
        });

        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(@NonNull Animator animation) {

            }

            @Override
            public void onAnimationEnd(@NonNull Animator animation) {
                Logger.d(TAG, "ScreenFlash#apply: onAnimationEnd");
                if (onAnimationEnd != null) {
                    onAnimationEnd.run();
                }
            }

            @Override
            public void onAnimationCancel(@NonNull Animator animation) {

            }

            @Override
            public void onAnimationRepeat(@NonNull Animator animation) {

            }
        });

        animator.start();

        return animator;
    }

    private float getBrightness() {
        if (mScreenFlashWindow == null) {
            Logger.e(TAG, "setBrightness: mScreenFlashWindow is null!");
            return Float.NaN;
        }

        WindowManager.LayoutParams layoutParam = mScreenFlashWindow.getAttributes();
        return layoutParam.screenBrightness;
    }

    private void setBrightness(float value) {
        if (mScreenFlashWindow == null) {
            Logger.e(TAG, "setBrightness: mScreenFlashWindow is null!");
            return;
        }

        if (Float.isNaN(value)) {
            Logger.e(TAG, "setBrightness: value is NaN!");
            return;
        }

        WindowManager.LayoutParams layoutParam = mScreenFlashWindow.getAttributes();
        layoutParam.screenBrightness = value;
        mScreenFlashWindow.setAttributes(layoutParam);
        Logger.d(TAG, "Brightness set to " + layoutParam.screenBrightness);
    }

    /**
     * Returns an {@link ScreenFlash} implementation based on the {@link Window} instance
     * set via {@link #setScreenFlashWindow(Window)}.
     *
     * <p> When {@link ScreenFlash#apply(long, ImageCapture.ScreenFlashListener)} is invoked,
     * this view becomes fully visible gradually with an animation and screen brightness is
     * maximized using the provided {@code Window}. Since brightness change of the display happens
     * asynchronously and may take some time to be completed, the animation to ramp up visibility
     * may require a duration of sufficient delay (decided internally) before
     * {@link ImageCapture.ScreenFlashListener#onCompleted()} is invoked.
     *
     * <p> The default color of the overlay view is {@link Color#WHITE}. To change
     * the color, use {@link #setBackgroundColor(int)}.
     *
     * <p> When {@link ScreenFlash#clear()} is invoked, the view
     * becomes transparent and screen brightness is restored.
     *
     * <p> The {@code Window} instance parameter can usually be provided from the activity using
     * the {@link PreviewView}, see {@link Activity#getWindow()} for details. If a null {@code
     * Window} is set or none set at all, a null value will be returned by this method.
     *
     * @return A simple {@link ScreenFlash} implementation, or null value if a non-null
     * {@code Window} instance hasn't been set.
     */
    @UiThread
    @Nullable
    public ScreenFlash getScreenFlash() {
        return mScreenFlash;
    }

    /**
     * Returns the duration of the visibility ramp-up animation.
     *
     * <p> This is currently used in {@link ScreenFlash#apply}.
     *
     * @see #getScreenFlash()
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public long getVisibilityRampUpAnimationDurationMillis() {
        return ANIMATION_DURATION_MILLIS;
    }
}
