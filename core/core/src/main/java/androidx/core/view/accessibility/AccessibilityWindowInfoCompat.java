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

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.os.LocaleListCompat;

/**
 * Helper for accessing {@link android.view.accessibility.AccessibilityWindowInfo}.
 */
public class AccessibilityWindowInfoCompat {
    private final Object mInfo;

    private static final int UNDEFINED = -1;

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
    static AccessibilityWindowInfoCompat wrapNonNullInstance(Object object) {
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

    private AccessibilityWindowInfoCompat(Object info) {
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
        if (SDK_INT >= 21) {
            return Api21Impl.getType((AccessibilityWindowInfo) mInfo);
        } else {
            return UNDEFINED;
        }
    }

    /**
     * Gets the layer which determines the Z-order of the window. Windows
     * with greater layer appear on top of windows with lesser layer.
     *
     * @return The window layer.
     */
    public int getLayer() {
        if (SDK_INT >= 21) {
            return Api21Impl.getLayer((AccessibilityWindowInfo) mInfo);
        } else {
            return UNDEFINED;
        }
    }

    /**
     * Gets the root node in the window's hierarchy.
     *
     * @return The root node.
     */
    @Nullable
    public AccessibilityNodeInfoCompat getRoot() {
        if (SDK_INT >= 21) {
            return AccessibilityNodeInfoCompat.wrapNonNullInstance(
                    Api21Impl.getRoot((AccessibilityWindowInfo) mInfo));
        } else {
            return null;
        }
    }

    /**
     * Gets the root node in the window's hierarchy.
     *
     * @param prefetchingStrategy the prefetching strategy.
     * @return The root node.
     *
     * @see AccessibilityNodeInfoCompat#getParent(int) for a description of prefetching.
     */
    @Nullable
    public AccessibilityNodeInfoCompat getRoot(int prefetchingStrategy) {
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
    @Nullable
    public AccessibilityWindowInfoCompat getParent() {
        if (SDK_INT >= 21) {
            return wrapNonNullInstance(Api21Impl.getParent((AccessibilityWindowInfo) mInfo));
        } else {
            return null;
        }
    }

    /**
     * Gets the unique window id.
     *
     * @return windowId The window id.
     */
    public int getId() {
        if (SDK_INT >= 21) {
            return Api21Impl.getId((AccessibilityWindowInfo) mInfo);
        } else {
            return UNDEFINED;
        }
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
        } else if (SDK_INT >= 21) {
            Rect outBounds = new Rect();
            Api21Impl.getBoundsInScreen((AccessibilityWindowInfo) mInfo, outBounds);
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
        if (SDK_INT >= 21) {
            Api21Impl.getBoundsInScreen((AccessibilityWindowInfo) mInfo, outBounds);
        }
    }

    /**
     * Gets if this window is active. An active window is the one
     * the user is currently touching or the window has input focus
     * and the user is not touching any window.
     *
     * @return Whether this is the active window.
     */
    public boolean isActive() {
        if (SDK_INT >= 21) {
            return Api21Impl.isActive((AccessibilityWindowInfo) mInfo);
        } else {
            return true;
        }
    }

    /**
     * Gets if this window has input focus.
     *
     * @return Whether has input focus.
     */
    public boolean isFocused() {
        if (SDK_INT >= 21) {
            return Api21Impl.isFocused((AccessibilityWindowInfo) mInfo);
        } else {
            return true;
        }
    }

    /**
     * Gets if this window has accessibility focus.
     *
     * @return Whether has accessibility focus.
     */
    public boolean isAccessibilityFocused() {
        if (SDK_INT >= 21) {
            return Api21Impl.isAccessibilityFocused((AccessibilityWindowInfo) mInfo);
        } else {
            return true;
        }
    }

    /**
     * Gets the number of child windows.
     *
     * @return The child count.
     */
    public int getChildCount() {
        if (SDK_INT >= 21) {
            return Api21Impl.getChildCount((AccessibilityWindowInfo) mInfo);
        } else {
            return 0;
        }
    }

    /**
     * Gets the child window at a given index.
     *
     * @param index The index.
     * @return The child.
     */
    @Nullable
    public AccessibilityWindowInfoCompat getChild(int index) {
        if (SDK_INT >= 21) {
            return wrapNonNullInstance(Api21Impl.getChild((AccessibilityWindowInfo) mInfo, index));
        } else {
            return null;
        }
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
    @Nullable
    public CharSequence getTitle() {
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
    @Nullable
    public AccessibilityNodeInfoCompat getAnchor() {
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
    @Nullable
    public static AccessibilityWindowInfoCompat obtain() {
        if (SDK_INT >= 21) {
            return wrapNonNullInstance(Api21Impl.obtain());
        } else {
            return null;
        }
    }

    /**
     * Returns a cached instance if such is available or a new one is
     * created. The returned instance is initialized from the given
     * <code>info</code>.
     *
     * @param info The other info.
     * @return An instance.
     */
    @Nullable
    public static AccessibilityWindowInfoCompat obtain(
            @Nullable AccessibilityWindowInfoCompat info) {
        if (SDK_INT >= 21) {
            return info == null
                    ? null
                    : wrapNonNullInstance(
                            Api21Impl.obtain((AccessibilityWindowInfo) info.mInfo));
        } else {
            return null;
        }
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
    @Nullable
    public AccessibilityWindowInfo unwrap() {
        if (SDK_INT >= 21) {
            return (AccessibilityWindowInfo) mInfo;
        } else {
            return null;
        }
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

    @NonNull
    @Override
    public String toString() {
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

    @RequiresApi(21)
    private static class Api21Impl {
        private Api21Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static void getBoundsInScreen(AccessibilityWindowInfo info, Rect outBounds) {
            info.getBoundsInScreen(outBounds);
        }

        @DoNotInline
        static AccessibilityWindowInfo getChild(AccessibilityWindowInfo info, int index) {
            return info.getChild(index);
        }

        @DoNotInline
        static int getChildCount(AccessibilityWindowInfo info) {
            return info.getChildCount();
        }

        @DoNotInline
        static int getId(AccessibilityWindowInfo info) {
            return info.getId();
        }

        @DoNotInline
        static int getLayer(AccessibilityWindowInfo info) {
            return info.getLayer();
        }

        @DoNotInline
        static AccessibilityWindowInfo getParent(AccessibilityWindowInfo info) {
            return info.getParent();
        }

        @DoNotInline
        static AccessibilityNodeInfo getRoot(AccessibilityWindowInfo info) {
            return info.getRoot();
        }

        @DoNotInline
        static int getType(AccessibilityWindowInfo info) {
            return info.getType();
        }

        @DoNotInline
        static boolean isAccessibilityFocused(AccessibilityWindowInfo info) {
            return info.isAccessibilityFocused();
        }

        @DoNotInline
        static boolean isActive(AccessibilityWindowInfo info) {
            return info.isActive();
        }

        @DoNotInline
        static boolean isFocused(AccessibilityWindowInfo info) {
            return info.isFocused();
        }

        @DoNotInline
        static AccessibilityWindowInfo obtain() {
            return AccessibilityWindowInfo.obtain();
        }

        @DoNotInline
        static AccessibilityWindowInfo obtain(AccessibilityWindowInfo info) {
            return AccessibilityWindowInfo.obtain(info);
        }
    }

    @RequiresApi(24)
    private static class Api24Impl {
        private Api24Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static AccessibilityNodeInfo getAnchor(AccessibilityWindowInfo info) {
            return info.getAnchor();
        }

        @DoNotInline
        static CharSequence getTitle(AccessibilityWindowInfo info) {
            return info.getTitle();
        }
    }

    @RequiresApi(26)
    private static class Api26Impl {
        private Api26Impl() {
            // This class is non instantiable.
        }

        @DoNotInline
        static boolean isInPictureInPictureMode(AccessibilityWindowInfo info) {
            return info.isInPictureInPictureMode();
        }
    }

    @RequiresApi(30)
    private static class Api30Impl {
        private Api30Impl() {
            // This class is non instantiable.
        }

        @DoNotInline
        static AccessibilityWindowInfo instantiateAccessibilityWindowInfo() {
            return new AccessibilityWindowInfo();
        }
    }

    @RequiresApi(33)
    private static class Api33Impl {
        private Api33Impl() {
            // This class is non instantiable.
        }

        @DoNotInline
        static int getDisplayId(AccessibilityWindowInfo info) {
            return info.getDisplayId();
        }

        @DoNotInline
        static void getRegionInScreen(AccessibilityWindowInfo info, Region outRegion) {
            info.getRegionInScreen(outRegion);
        }

        @DoNotInline
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

        @DoNotInline
        public static long getTransitionTimeMillis(AccessibilityWindowInfo info) {
            return info.getTransitionTimeMillis();
        }

        @DoNotInline
        static LocaleList getLocales(AccessibilityWindowInfo info) {
            return info.getLocales();
        }
    }
}