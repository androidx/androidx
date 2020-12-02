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

package androidx.window;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.N;
import static android.os.Build.VERSION_CODES.P;
import static android.os.Build.VERSION_CODES.Q;
import static android.os.Build.VERSION_CODES.R;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.View;
import android.view.WindowInsets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Helper class used to compute window bounds across Android versions. Obtain an instance with
 * {@link #getInstance()}.
 */
class WindowBoundsHelper {
    private static final String TAG = "WindowBoundsHelper";

    private static WindowBoundsHelper sInstance = new WindowBoundsHelper();
    @Nullable
    private static WindowBoundsHelper sTestInstance;

    static WindowBoundsHelper getInstance() {
        if (sTestInstance != null) {
            return sTestInstance;
        }
        return sInstance;
    }

    @VisibleForTesting
    static void setForTesting(@Nullable WindowBoundsHelper helper) {
        sTestInstance = helper;
    }

    WindowBoundsHelper() {}

    /**
     * Computes the size and position of the area the window would occupy with
     * {@link android.view.WindowManager.LayoutParams#MATCH_PARENT MATCH_PARENT} width and height
     * and any combination of flags that would allow the window to extend behind display cutouts.
     * <p>
     * For example, {@link android.view.WindowManager.LayoutParams#layoutInDisplayCutoutMode} set to
     * {@link android.view.WindowManager.LayoutParams#LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS} or the
     * {@link android.view.WindowManager.LayoutParams#FLAG_LAYOUT_NO_LIMITS} flag set.
     * <p>
     * The value returned from this method may be different from platform API(s) used to determine
     * the size and position of the visible area a given context occupies. For example:
     * <ul>
     *     <li>{@link Display#getSize(Point)} can be used to determine the size of the visible area
     *     a window occupies, but may be subtracted to exclude certain system decorations that
     *     always appear on screen, notably the navigation bar.
     *     <li>The decor view's {@link View#getWidth()} and {@link View#getHeight()} can be used to
     *     determine the size of the top level view in the view hierarchy, but this size is
     *     determined through a combination of {@link android.view.WindowManager.LayoutParams}
     *     flags and may not represent the true window size. For example, a window that does not
     *     indicate it can be displayed behind a display cutout will have the size of the decor
     *     view offset to exclude this region unless this region overlaps with the status bar, while
     *     the value returned from this method will include this region.
     * </ul>
     * <p>
     * The value returned from this method is guaranteed to be correct on platforms
     * {@link Build.VERSION_CODES#Q Q} and above. For older platforms the value may be invalid if
     * the activity is in multi-window mode or if the navigation bar offset can not be accounted
     * for, though a best effort is made to ensure the returned value is as close as possible to
     * the true value. See {@link #computeWindowBoundsP(Activity)} and
     * {@link #computeWindowBoundsN(Activity)}.
     * <p>
     * Note: The value of this is based on the last windowing state reported to the client.
     *
     * @see android.view.WindowManager#getCurrentWindowMetrics()
     * @see android.view.WindowMetrics#getBounds()
     */
    @NonNull
    Rect computeCurrentWindowBounds(Activity activity) {
        if (Build.VERSION.SDK_INT >= R) {
            return activity.getWindowManager().getCurrentWindowMetrics().getBounds();
        } else if (Build.VERSION.SDK_INT >= Q) {
            return computeWindowBoundsQ(activity);
        } else if (Build.VERSION.SDK_INT >= P) {
            return computeWindowBoundsP(activity);
        } else if (Build.VERSION.SDK_INT >= N) {
            return computeWindowBoundsN(activity);
        } else {
            return computeWindowBoundsJellyBean(activity);
        }
    }

    /**
     * Computes the maximum size and position of the area the window can expect with
     * {@link android.view.WindowManager.LayoutParams#MATCH_PARENT MATCH_PARENT} width and height
     * and any combination of flags that would allow the window to extend behind display cutouts.
     * <p>
     * The value returned from this method will always match {@link Display#getRealSize(Point)} on
     * {@link Build.VERSION_CODES#Q Android 10} and below.
     *
     * @see android.view.WindowManager#getMaximumWindowMetrics()
     */
    @NonNull
    Rect computeMaximumWindowBounds(Activity activity) {
        if (Build.VERSION.SDK_INT >= R) {
            return activity.getWindowManager().getMaximumWindowMetrics().getBounds();
        } else {
            Display display = activity.getWindowManager().getDefaultDisplay();
            Point displaySize = getRealSizeForDisplay(display);
            return new Rect(0, 0, displaySize.x, displaySize.y);
        }
    }

    /** Computes the window bounds for {@link Build.VERSION_CODES#Q}. */
    @NonNull
    @RequiresApi(Q)
    private static Rect computeWindowBoundsQ(Activity activity) {
        Rect bounds;
        Configuration config = activity.getResources().getConfiguration();
        try {
            Field windowConfigField = Configuration.class.getDeclaredField("windowConfiguration");
            windowConfigField.setAccessible(true);
            Object windowConfig = windowConfigField.get(config);

            Method getBoundsMethod = windowConfig.getClass().getDeclaredMethod("getBounds");
            bounds = new Rect((Rect) getBoundsMethod.invoke(windowConfig));
        } catch (NoSuchFieldException | NoSuchMethodException | IllegalAccessException
                | InvocationTargetException e) {
            Log.w(TAG, e);
            // If reflection fails for some reason default to the P implementation which still has
            // the ability to account for display cutouts.
            bounds = computeWindowBoundsP(activity);
        }

        return bounds;
    }

    /**
     * Computes the window bounds for {@link Build.VERSION_CODES#P}.
     * <p>
     * NOTE: This method may result in incorrect values if the {@link Resources} value stored at
     * 'navigation_bar_height' does not match the true navigation bar inset on the window.
     * </ul>
     */
    @NonNull
    @RequiresApi(P)
    private static Rect computeWindowBoundsP(Activity activity) {
        Rect bounds = new Rect();
        Configuration config = activity.getResources().getConfiguration();
        try {
            Field windowConfigField = Configuration.class.getDeclaredField("windowConfiguration");
            windowConfigField.setAccessible(true);
            Object windowConfig = windowConfigField.get(config);

            // In multi-window mode we'll use the WindowConfiguration#mBounds property which
            // should match the window size. Otherwise we'll use the mAppBounds property and will
            // adjust it below.
            if (activity.isInMultiWindowMode()) {
                Method getAppBounds = windowConfig.getClass().getDeclaredMethod("getBounds");
                bounds.set((Rect) getAppBounds.invoke(windowConfig));
            } else {
                Method getAppBounds = windowConfig.getClass().getDeclaredMethod("getAppBounds");
                bounds.set((Rect) getAppBounds.invoke(windowConfig));
            }
        } catch (NoSuchFieldException | NoSuchMethodException | IllegalAccessException
                | InvocationTargetException e) {
            Log.w(TAG, e);
            Display defaultDisplay = activity.getWindowManager().getDefaultDisplay();
            defaultDisplay.getRectSize(bounds);
        }

        android.view.WindowManager platformWindowManager = activity.getWindowManager();
        Display currentDisplay = platformWindowManager.getDefaultDisplay();
        Point realDisplaySize = new Point();
        currentDisplay.getRealSize(realDisplaySize);

        if (!activity.isInMultiWindowMode()) {
            // The activity is not in multi-window mode. Check if the addition of the navigation
            // bar size to mAppBounds results in the real display size and if so assume the nav
            // bar height should be added to the result.
            int navigationBarHeight = getNavigationBarHeight(activity);

            if (bounds.bottom + navigationBarHeight == realDisplaySize.y) {
                bounds.bottom += navigationBarHeight;
            } else if (bounds.right + navigationBarHeight == realDisplaySize.x) {
                bounds.right += navigationBarHeight;
            } else if (bounds.left == navigationBarHeight) {
                bounds.left = 0;
            }
        }

        if ((bounds.width() < realDisplaySize.x || bounds.height() < realDisplaySize.y)
                && !activity.isInMultiWindowMode()) {
            // If the corrected bounds are not the same as the display size and the activity is not
            // in multi-window mode it is possible there are unreported cutouts inset-ing the
            // window depending on the layoutInCutoutMode. Check for them here by getting the
            // cutout from the display itself.
            DisplayCutout displayCutout = getCutoutForDisplay(currentDisplay);
            if (displayCutout != null) {
                if (bounds.left == displayCutout.getSafeInsetLeft()) {
                    bounds.left = 0;
                }

                if (realDisplaySize.x - bounds.right == displayCutout.getSafeInsetRight()) {
                    bounds.right += displayCutout.getSafeInsetRight();
                }

                if (bounds.top == displayCutout.getSafeInsetTop()) {
                    bounds.top = 0;
                }

                if (realDisplaySize.y - bounds.bottom == displayCutout.getSafeInsetBottom()) {
                    bounds.bottom += displayCutout.getSafeInsetBottom();
                }
            }
        }

        return bounds;
    }

    /**
     * Computes the window bounds for platforms between {@link Build.VERSION_CODES#N}
     * and {@link Build.VERSION_CODES#O_MR1}, inclusive.
     * <p>
     * NOTE: This method may result in incorrect values under the following conditions:
     * <ul>
     *     <li>If the activity is in multi-window mode the origin of the returned bounds will
     *     always be anchored at (0, 0).
     *     <li>If the {@link Resources} value stored at 'navigation_bar_height' does not match the
     *     true navigation bar size the returned bounds will not take into account the navigation
     *     bar.
     * </ul>
     */
    @NonNull
    @RequiresApi(N)
    private static Rect computeWindowBoundsN(Activity activity) {
        Rect bounds = new Rect();

        Display defaultDisplay = activity.getWindowManager().getDefaultDisplay();
        defaultDisplay.getRectSize(bounds);

        if (!activity.isInMultiWindowMode()) {
            // The activity is not in multi-window mode. Check if the addition of the navigation
            // bar size to Display#getSize() results in the real display size and if so return
            // this value. If not, return the result of Display#getSize().
            Point realDisplaySize = getRealSizeForDisplay(defaultDisplay);
            int navigationBarHeight = getNavigationBarHeight(activity);

            if (bounds.bottom + navigationBarHeight == realDisplaySize.y) {
                bounds.bottom += navigationBarHeight;
            } else if (bounds.right + navigationBarHeight == realDisplaySize.x) {
                bounds.right += navigationBarHeight;
            }
        }

        return bounds;
    }

    /**
     * Computes the window bounds for platforms between {@link Build.VERSION_CODES#JELLY_BEAN}
     * and {@link Build.VERSION_CODES#M}, inclusive.
     * <p>
     * Given that multi-window mode isn't supported before N we simply return the real display
     * size which should match the window size of a full-screen app.
     */
    @NonNull
    @RequiresApi(JELLY_BEAN)
    private static Rect computeWindowBoundsJellyBean(Activity activity) {
        Display defaultDisplay = activity.getWindowManager().getDefaultDisplay();
        Point realDisplaySize = getRealSizeForDisplay(defaultDisplay);

        Rect bounds = new Rect();
        bounds.right = realDisplaySize.x;
        bounds.bottom = realDisplaySize.y;
        return bounds;
    }

    /**
     * Returns the full (real) size of the display, in pixels, without subtracting any window
     * decor or applying any compatibility scale factors.
     * <p>
     * The size is adjusted based on the current rotation of the display.
     *
     * @return a point representing the real display size in pixels.
     *
     * @see Display#getRealSize(Point)
     */
    @NonNull
    @VisibleForTesting
    @RequiresApi(JELLY_BEAN)
    static Point getRealSizeForDisplay(Display display) {
        Point size = new Point();
        if (Build.VERSION.SDK_INT >= JELLY_BEAN_MR1) {
            display.getRealSize(size);
        } else {
            try {
                Method getRealSizeMethod = Display.class.getDeclaredMethod("getRealSize",
                        Point.class);
                getRealSizeMethod.setAccessible(true);
                getRealSizeMethod.invoke(display, size);
            } catch (NoSuchMethodException e) {
                Log.w(TAG, e);
            } catch (IllegalAccessException e) {
                Log.w(TAG, e);
            } catch (InvocationTargetException e) {
                Log.w(TAG, e);
            }
        }
        return size;
    }

    /**
     * Returns the {@link Resources} value stored as 'navigation_bar_height'.
     * <p>
     * Note: This is error-prone and is <b>not</b> the recommended way to determine the size
     * of the overlapping region between the navigation bar and a given window. The best approach
     * is to acquire the {@link WindowInsets}.
     */
    private static int getNavigationBarHeight(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    /**
     * Returns the {@link DisplayCutout} for the given display. Note that display cutout returned
     * here is for the display and the insets provided are in the display coordinate system.
     *
     * @return the display cutout for the given display.
     */
    @Nullable
    @RequiresApi(P)
    private static DisplayCutout getCutoutForDisplay(Display display) {
        DisplayCutout displayCutout = null;
        try {
            Class<?> displayInfoClass = Class.forName("android.view.DisplayInfo");
            Constructor<?> displayInfoConstructor = displayInfoClass.getConstructor();
            displayInfoConstructor.setAccessible(true);
            Object displayInfo = displayInfoConstructor.newInstance();

            Method getDisplayInfoMethod = display.getClass().getDeclaredMethod(
                    "getDisplayInfo", displayInfo.getClass());
            getDisplayInfoMethod.setAccessible(true);
            getDisplayInfoMethod.invoke(display, displayInfo);

            Field displayCutoutField = displayInfo.getClass().getDeclaredField("displayCutout");
            displayCutoutField.setAccessible(true);
            Object cutout = displayCutoutField.get(displayInfo);
            if (cutout instanceof DisplayCutout) {
                displayCutout = (DisplayCutout) cutout;
            }
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException
                | IllegalAccessException | InvocationTargetException
                | InstantiationException e) {
            Log.w(TAG, e);
        }
        return displayCutout;
    }
}
