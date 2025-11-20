/*
 * Copyright (C) 2015 The Android Open Source Project
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

package androidx.core.view.accessibility;

import static android.os.Build.VERSION.SDK_INT;
import static android.view.Display.DEFAULT_DISPLAY;

import android.graphics.Rect;
import android.graphics.Region;
import android.os.Build;
import android.os.LocaleList;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import androidx.annotation.RequiresApi;
import androidx.core.os.LocaleListCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Helper for accessing {@link android.view.accessibility.AccessibilityWindowInfo}.
 */
public class AccessibilityWindowInfoCompat {
    private final AccessibilityWindowInfo mInfo;

    /**
     * Window type: This is an application window. Such a window shows UI for
     * interacting with an application.
     */
    public static final int TYPE_APPLICATION = 1;

    /**
     * Window type: This is an input method window. Such a window shows UI for
     * inputting text such as keyboard, suggestions, etc.
     */
    public static final int TYPE_INPUT_METHOD = 2;

    /**
     * Window type: This is an system window. Such a window shows UI for
     * interacting with the system.
     */
    public static final int TYPE_SYSTEM = 3;

    /**
     * Window type: Windows that are overlaid <em>only</em> by an {@link
     * android.accessibilityservice.AccessibilityService} for interception of
     * user interactions without changing the windows an accessibility service
     * can introspect. In particular, an accessibility service can introspect
     * only windows that a sighted user can interact with which they can touch
     * these windows or can type into these windows. For example, if there
     * is a full screen accessibility overlay that is touchable, the windows
     * below it will be introspectable by an accessibility service regardless
     * they are covered by a touchable window.
     */
    public static final int TYPE_ACCESSIBILITY_OVERLAY = 4;

    /**
     * Window type: A system window used to divide the screen in split-screen mode.
     * This type of window is present only in split-screen mode.
     */
    public static final int TYPE_SPLIT_SCREEN_DIVIDER = 5;

    /**
     * Window type: A system window used to show the UI for the interaction with
     * window-based magnification, which includes the magnified content and the option menu.
     */
    public static final int TYPE_MAGNIFICATION_OVERLAY = 6;

    /**
     * Creates a wrapper for info implementation.
     *
     * @param object The info to wrap.
     * @return A wrapper for if the object is not null, null otherwise.
     */
    static AccessibilityWindowInfoCompat wrapNonNullInstance(AccessibilityWindowInfo object) {
        if (object != null) {
            return new AccessibilityWindowInfoCompat(object);
        }
        return null;
    }

    /**
     * Creates a new AccessibilityWindowInfoCompat.
     * <p>
     * Compatibility:
     *  <ul>
     *      <li>Api &lt; 30: Will not wrap an
     *      {@link android.view.accessibility.AccessibilityWindowInfo} instance.</li>
     *  </ul>
     * </p>
     *
     */
    public AccessibilityWindowInfoCompat() {
        if (SDK_INT >= 30) {
            mInfo = Api30Impl.instantiateAccessibilityWindowInfo();
        } else {
            mInfo = null;
        }
    }

    private AccessibilityWindowInfoCompat(AccessibilityWindowInfo info) {
        mInfo = info;
    }

    /**
     * Gets the type of the window.
     *
     * @return The type.
     * @see #TYPE_APPLICATION
     * @see #TYPE_INPUT_METHOD
     * @see #TYPE_SYSTEM
     * @see #TYPE_ACCESSIBILITY_OVERLAY
     */
    public int getType() {
        return mInfo.getType();
    }

    /**
     * Gets the layer which determines the Z-order of the window. Windows
     * with greater layer appear on top of windows with lesser layer.
     *
     * @return The window layer.
     */
    public int getLayer() {
        return mInfo.getLayer();
    }

    /**
     * Gets the root node in the window's hierarchy.
     *
     * @return The root node.
     */
    public @Nullable AccessibilityNodeInfoCompat getRoot() {
        return AccessibilityNodeInfoCompat.wrapNonNullInstance(mInfo.getRoot());
    }

    /**
     * Gets the root node in the window's hierarchy.
     *
     * @param prefetchingStrategy the prefetching strategy.
     * @return The root node.
     *
     * @see AccessibilityNodeInfoCompat#getParent(int) for a description of prefetching.
     */
    public @Nullable AccessibilityNodeInfoCompat getRoot(int prefetchingStrategy) {
        if (Build.VERSION.SDK_INT >= 33) {
            return Api33Impl.getRoot(mInfo, prefetchingStrategy);
        }
        return getRoot();
    }

    /**
     * Check if the window is in picture-in-picture mode.
     * <p>
     * Compatibility:
     * <ul>
     *     <li>API &lt; 26: Returns false.</li>
     * </ul>
     * @return {@code true} if the window is in picture-in-picture mode, {@code false} otherwise.
     */
    public boolean isInPictureInPictureMode() {
        if (SDK_INT >= 26) {
            return Api26Impl.isInPictureInPictureMode((AccessibilityWindowInfo) mInfo);
        } else {
            return false;
        }
    }

    /**
     * Gets the parent window if such.
     *
     * @return The parent window.
     */
    public @Nullable AccessibilityWindowInfoCompat getParent() {
        return wrapNonNullInstance(((AccessibilityWindowInfo) mInfo).getParent());
    }

    /**
     * Gets the unique window id.
     *
     * @return windowId The window id.
     */
    public int getId() {
        return mInfo.getId();
    }

    /**
     * Gets the touchable region of this window in the screen.
     * <p>
     * Compatibility:
     * <ul>
     *     <li>API &lt; 33: Gets the bounds of this window in the screen. </li>
     *     <li>API &lt; 21: Does not operate. </li>
     * </ul>
     *
     * @param outRegion The out window region.
     */
    public void getRegionInScreen(@NonNull Region outRegion) {
        if (SDK_INT >= 33) {
            Api33Impl.getRegionInScreen((AccessibilityWindowInfo) mInfo, outRegion);
        } else {
            Rect outBounds = new Rect();
            mInfo.getBoundsInScreen(outBounds);
            outRegion.set(outBounds);
        }
    }

    /**
     * Gets the bounds of this window in the screen.
     * <p>
     * Compatibility:
     * <ul>
     *   <li>API &lt; 21: Does not operate. </li>
     * </ul>
     *
     * @param outBounds The out window bounds.
     */
    public void getBoundsInScreen(@NonNull Rect outBounds) {
        mInfo.getBoundsInScreen(outBounds);
    }

    /**
     * Gets if this window is active. An active window is the one
     * the user is currently touching or the window has input focus
     * and the user is not touching any window.
     *
     * @return Whether this is the active window.
     */
    public boolean isActive() {
        return mInfo.isActive();
    }

    /**
     * Gets if this window has input focus.
     *
     * @return Whether has input focus.
     */
    public boolean isFocused() {
        return mInfo.isFocused();
    }

    /**
     * Gets if this window has accessibility focus.
     *
     * @return Whether has accessibility focus.
     */
    public boolean isAccessibilityFocused() {
        return mInfo.isAccessibilityFocused();
    }

    /**
     * Gets the number of child windows.
     *
     * @return The child count.
     */
    public int getChildCount() {
        return mInfo.getChildCount();
    }

    /**
     * Gets the child window at a given index.
     *
     * @param index The index.
     * @return The child.
     */
    public @Nullable AccessibilityWindowInfoCompat getChild(int index) {
        return wrapNonNullInstance(mInfo.getChild(index));
    }

    /**
     * Returns the ID of the display this window is on, for use with
     * {@link android.hardware.display.DisplayManager#getDisplay(int)}.
     * <p>
     * Compatibility:
     * <ul>
     *   <li>Api &lt; 33: Will return {@link android.view.Display.DEFAULT_DISPLAY}.</li>
     * </ul>
     *
     * @return the logical display id.
     */
    public int getDisplayId() {
        if (SDK_INT >= 33) {
            return Api33Impl.getDisplayId((AccessibilityWindowInfo) mInfo);
        } else {
            return DEFAULT_DISPLAY;
        }
    }

    /**
     * Returns the {@link SystemClock#uptimeMillis()} at which the last transition happens.
     * A transition happens when {@link #getBoundsInScreen(Rect)} is changed.
     * <p>
     * Compatibility:
     * <ul>
     *   <li>Api &lt; 34: Will return 0.</li>
     * </ul>
     * @return The transition timestamp.
     */
    public long getTransitionTimeMillis() {
        if (SDK_INT >= 34) {
            return Api34Impl.getTransitionTimeMillis((AccessibilityWindowInfo) mInfo);
        }
        return 0;
    }

    /**
     * Returns the {@link android.os.LocaleList} of the window.
     * <p>
     * Compatibility:
     * <ul>
     *   <li>Api &lt; 34: Will return {@link LocaleListCompat#getEmptyLocaleList()}.</li>
     * </ul>
     * @return the locales of the window.
     */
    public @NonNull LocaleListCompat getLocales() {
        if (SDK_INT >= 34) {
            return LocaleListCompat.wrap(Api34Impl.getLocales((AccessibilityWindowInfo) mInfo));
        } else {
            return LocaleListCompat.getEmptyLocaleList();
        }
    }

    /**
     * Gets the title of the window.
     *
     * @return The title of the window, or the application label for the window if no title was
     * explicitly set, or {@code null} if neither is available.
     */
    public @Nullable CharSequence getTitle() {
        if (SDK_INT >= 24) {
            return Api24Impl.getTitle((AccessibilityWindowInfo) mInfo);
        } else {
            return null;
        }
    }

    /**
     * Gets the node that anchors this window to another.
     *
     * @return The anchor node, or {@code null} if none exists.
     */
    public @Nullable AccessibilityNodeInfoCompat getAnchor() {
        if (SDK_INT >= 24) {
            return AccessibilityNodeInfoCompat.wrapNonNullInstance(
                    Api24Impl.getAnchor((AccessibilityWindowInfo) mInfo));
        } else {
            return null;
        }
    }

    /**
     * Returns a cached instance if such is available or a new one is
     * created.
     *
     * @return An instance.
     */
    public static @Nullable AccessibilityWindowInfoCompat obtain() {
        return wrapNonNullInstance(AccessibilityWindowInfo.obtain());
    }

    /**
     * Returns a cached instance if such is available or a new one is
     * created. The returned instance is initialized from the given
     * <code>info</code>.
     *
     * @param info The other info.
     * @return An instance.
     */
    public static @Nullable AccessibilityWindowInfoCompat obtain(
            @Nullable AccessibilityWindowInfoCompat info) {
        return info == null
                ? null
                : wrapNonNullInstance(AccessibilityWindowInfo.obtain(info.mInfo));
    }

    /**
     * Return an instance back to be reused.
     * <p>
     * <strong>Note:</strong> You must not touch the object after calling this function.
     * </p>
     *
     * @deprecated Accessibility Object recycling is no longer necessary or functional.
     */
    @Deprecated
    public void recycle() { }

    /**
     * @return The unwrapped {@link android.view.accessibility.AccessibilityWindowInfo}.
     */
    public @Nullable AccessibilityWindowInfo unwrap() {
        return mInfo;
    }

    @Override
    public int hashCode() {
        return (mInfo == null) ? 0 : mInfo.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof AccessibilityWindowInfoCompat)) {
            return false;
        }
        AccessibilityWindowInfoCompat other = (AccessibilityWindowInfoCompat) obj;
        if (mInfo == null) {
            return other.mInfo == null;
        }
        return mInfo.equals(other.mInfo);
    }

    @Override
    public @NonNull String toString() {
        StringBuilder builder = new StringBuilder();
        Rect bounds = new Rect();
        getBoundsInScreen(bounds);
        builder.append("AccessibilityWindowInfo[");
        builder.append("id=").append(getId());
        builder.append(", type=").append(typeToString(getType()));
        builder.append(", layer=").append(getLayer());
        builder.append(", bounds=").append(bounds);
        builder.append(", focused=").append(isFocused());
        builder.append(", active=").append(isActive());
        builder.append(", hasParent=").append(getParent() != null);
        builder.append(", hasChildren=").append(getChildCount() > 0);
        builder.append(", transitionTime=").append(getTransitionTimeMillis());
        builder.append(", locales=").append(getLocales());
        builder.append(']');
        return builder.toString();
    }

    private static String typeToString(int type) {
        switch (type) {
            case TYPE_APPLICATION: {
                return "TYPE_APPLICATION";
            }
            case TYPE_INPUT_METHOD: {
                return "TYPE_INPUT_METHOD";
            }
            case TYPE_SYSTEM: {
                return "TYPE_SYSTEM";
            }
            case TYPE_ACCESSIBILITY_OVERLAY: {
                return "TYPE_ACCESSIBILITY_OVERLAY";
            }
            default:
                return "<UNKNOWN>";
        }
    }

    @RequiresApi(24)
    private static class Api24Impl {
        private Api24Impl() {
            // This class is not instantiable.
        }

        static AccessibilityNodeInfo getAnchor(AccessibilityWindowInfo info) {
            return info.getAnchor();
        }

        static CharSequence getTitle(AccessibilityWindowInfo info) {
            return info.getTitle();
        }
    }

    @RequiresApi(26)
    private static class Api26Impl {
        private Api26Impl() {
            // This class is non instantiable.
        }

        static boolean isInPictureInPictureMode(AccessibilityWindowInfo info) {
            return info.isInPictureInPictureMode();
        }
    }

    @RequiresApi(30)
    private static class Api30Impl {
        private Api30Impl() {
            // This class is non instantiable.
        }

        static AccessibilityWindowInfo instantiateAccessibilityWindowInfo() {
            return new AccessibilityWindowInfo();
        }
    }

    @RequiresApi(33)
    private static class Api33Impl {
        private Api33Impl() {
            // This class is non instantiable.
        }

        static int getDisplayId(AccessibilityWindowInfo info) {
            return info.getDisplayId();
        }

        static void getRegionInScreen(AccessibilityWindowInfo info, Region outRegion) {
            info.getRegionInScreen(outRegion);
        }

        public static AccessibilityNodeInfoCompat getRoot(Object info, int prefetchingStrategy) {
            return AccessibilityNodeInfoCompat.wrapNonNullInstance(
                    ((AccessibilityWindowInfo) info).getRoot(prefetchingStrategy));
        }
    }

    @RequiresApi(34)
    private static class Api34Impl {
        private Api34Impl() {
            // This class is non instantiable.
        }

        public static long getTransitionTimeMillis(AccessibilityWindowInfo info) {
            return info.getTransitionTimeMillis();
        }

        static LocaleList getLocales(AccessibilityWindowInfo info) {
            return info.getLocales();
        }
    }
}