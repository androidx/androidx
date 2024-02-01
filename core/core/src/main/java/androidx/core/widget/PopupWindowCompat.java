/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.PopupWindow;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Helper for accessing features in {@link PopupWindow}.
 */
public final class PopupWindowCompat {
    private static final String TAG = "PopupWindowCompatApi21";

    private static Method sSetWindowLayoutTypeMethod;
    private static boolean sSetWindowLayoutTypeMethodAttempted;
    private static Method sGetWindowLayoutTypeMethod;
    private static boolean sGetWindowLayoutTypeMethodAttempted;

    private static Field sOverlapAnchorField;
    private static boolean sOverlapAnchorFieldAttempted;

    private PopupWindowCompat() {
        // This class is not publicly instantiable.
    }

    /**
     * <p>Display the content view in a popup window anchored to the bottom-left
     * corner of the anchor view offset by the specified x and y coordinates.
     * If there is not enough room on screen to show
     * the popup in its entirety, this method tries to find a parent scroll
     * view to scroll. If no parent scroll view can be scrolled, the bottom-left
     * corner of the popup is pinned at the top left corner of the anchor view.</p>
     * <p>If the view later scrolls to move <code>anchor</code> to a different
     * location, the popup will be moved correspondingly.</p>
     *
     * @param popup the PopupWindow to show
     * @param anchor the view on which to pin the popup window
     * @param xoff A horizontal offset from the anchor in pixels
     * @param yoff A vertical offset from the anchor in pixels
     * @param gravity Alignment of the popup relative to the anchor
     */
    public static void showAsDropDown(@NonNull PopupWindow popup, @NonNull View anchor,
            int xoff, int yoff, int gravity) {
        popup.showAsDropDown(anchor, xoff, yoff, gravity);
    }

    /**
     * Sets whether the popup window should overlap its anchor view when
     * displayed as a drop-down.
     *
     * @param popupWindow popup window for which to set the anchor.
     * @param overlapAnchor Whether the popup should overlap its anchor.
     */
    @SuppressWarnings("JavaReflectionMemberAccess") // Reflective access to private method
    public static void setOverlapAnchor(@NonNull PopupWindow popupWindow, boolean overlapAnchor) {
        if (Build.VERSION.SDK_INT >= 23) {
            Api23Impl.setOverlapAnchor(popupWindow, overlapAnchor);
        } else if (Build.VERSION.SDK_INT >= 21) {
            if (!sOverlapAnchorFieldAttempted) {
                try {
                    sOverlapAnchorField = PopupWindow.class.getDeclaredField("mOverlapAnchor");
                    sOverlapAnchorField.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    Log.i(TAG, "Could not fetch mOverlapAnchor field from PopupWindow", e);
                }
                sOverlapAnchorFieldAttempted = true;
            }
            if (sOverlapAnchorField != null) {
                try {
                    sOverlapAnchorField.set(popupWindow, overlapAnchor);
                } catch (IllegalAccessException e) {
                    Log.i(TAG, "Could not set overlap anchor field in PopupWindow", e);
                }
            }
        }
    }

    /**
     * Returns whether the popup window should overlap its anchor view when
     * displayed as a drop-down.
     *
     * @return Whether the popup should overlap its anchor.
     */
    @SuppressWarnings({"JavaReflectionMemberAccess", "ConstantConditions"})
    // Private field access via getDeclaredField(), unboxing result of get()
    public static boolean getOverlapAnchor(@NonNull PopupWindow popupWindow) {
        if (Build.VERSION.SDK_INT >= 23) {
            return Api23Impl.getOverlapAnchor(popupWindow);
        } else if (Build.VERSION.SDK_INT >= 21) {
            if (!sOverlapAnchorFieldAttempted) {
                try {
                    sOverlapAnchorField = PopupWindow.class.getDeclaredField("mOverlapAnchor");
                    sOverlapAnchorField.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    Log.i(TAG, "Could not fetch mOverlapAnchor field from PopupWindow", e);
                }
                sOverlapAnchorFieldAttempted = true;
            }
            if (sOverlapAnchorField != null) {
                try {
                    return (Boolean) sOverlapAnchorField.get(popupWindow);
                } catch (IllegalAccessException e) {
                    Log.i(TAG, "Could not get overlap anchor field in PopupWindow", e);
                }
            }
        }
        return false;
    }

    /**
     * Set the layout type for this window. This value will be passed through to
     * {@link android.view.WindowManager.LayoutParams#type} therefore the value should match any
     * value {@link android.view.WindowManager.LayoutParams#type} accepts.
     *
     * @param popupWindow popup window for which to set the layout type.
     * @param layoutType Layout type for this window.
     *
     * @see android.view.WindowManager.LayoutParams#type
     */
    public static void setWindowLayoutType(@NonNull PopupWindow popupWindow, int layoutType) {
        if (Build.VERSION.SDK_INT >= 23) {
            Api23Impl.setWindowLayoutType(popupWindow, layoutType);
        } else {
            if (!sSetWindowLayoutTypeMethodAttempted) {
                try {
                    sSetWindowLayoutTypeMethod = PopupWindow.class.getDeclaredMethod(
                            "setWindowLayoutType", int.class);
                    sSetWindowLayoutTypeMethod.setAccessible(true);
                } catch (Exception e) {
                    // Reflection method fetch failed. Oh well.
                }
                sSetWindowLayoutTypeMethodAttempted = true;
            }
            if (sSetWindowLayoutTypeMethod != null) {
                try {
                    sSetWindowLayoutTypeMethod.invoke(popupWindow, layoutType);
                } catch (Exception e) {
                    // Reflection call failed. Oh well.
                }
            }
        }
    }

    /**
     * Returns the layout type for this window.
     *
     * @see #setWindowLayoutType(PopupWindow popupWindow, int)
     */
    @SuppressWarnings("ConstantConditions") // Unboxing result of invoke()
    public static int getWindowLayoutType(@NonNull PopupWindow popupWindow) {
        if (Build.VERSION.SDK_INT >= 23) {
            return Api23Impl.getWindowLayoutType(popupWindow);
        } else {
            if (!sGetWindowLayoutTypeMethodAttempted) {
                try {
                    sGetWindowLayoutTypeMethod = PopupWindow.class.getDeclaredMethod(
                            "getWindowLayoutType");
                    sGetWindowLayoutTypeMethod.setAccessible(true);
                } catch (Exception e) {
                    // Reflection method fetch failed. Oh well.
                }
                sGetWindowLayoutTypeMethodAttempted = true;
            }
            if (sGetWindowLayoutTypeMethod != null) {
                try {
                    return (Integer) sGetWindowLayoutTypeMethod.invoke(popupWindow);
                } catch (Exception e) {
                    // Reflection call failed. Oh well.
                }
            }
        }
        return 0;
    }

    @RequiresApi(23)
    static class Api23Impl {
        private Api23Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static void setOverlapAnchor(PopupWindow popupWindow, boolean overlapAnchor) {
            popupWindow.setOverlapAnchor(overlapAnchor);
        }

        @DoNotInline
        static boolean getOverlapAnchor(PopupWindow popupWindow) {
            return popupWindow.getOverlapAnchor();
        }

        @DoNotInline
        static void setWindowLayoutType(PopupWindow popupWindow, int layoutType) {
            popupWindow.setWindowLayoutType(layoutType);
        }

        @DoNotInline
        static int getWindowLayoutType(PopupWindow popupWindow) {
            return popupWindow.getWindowLayoutType();
        }
    }
}
