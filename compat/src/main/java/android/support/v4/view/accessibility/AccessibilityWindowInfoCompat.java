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

package android.support.v4.view.accessibility;

import static android.os.Build.VERSION.SDK_INT;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityWindowInfo;

/**
 * Helper for accessing {@link android.view.accessibility.AccessibilityWindowInfo}.
 */
public class AccessibilityWindowInfoCompat {
    private Object mInfo;

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

    private AccessibilityWindowInfoCompat(Object info) {
        mInfo = info;
    }

    /**
     * Gets the type of the window.
     *
     * @return The type.
     *
     * @see #TYPE_APPLICATION
     * @see #TYPE_INPUT_METHOD
     * @see #TYPE_SYSTEM
     * @see #TYPE_ACCESSIBILITY_OVERLAY
     */
    public int getType() {
        if (SDK_INT >= 21) {
            return ((AccessibilityWindowInfo) mInfo).getType();
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
            return ((AccessibilityWindowInfo) mInfo).getLayer();
        } else {
            return UNDEFINED;
        }
    }

    /**
     * Gets the root node in the window's hierarchy.
     *
     * @return The root node.
     */
    public AccessibilityNodeInfoCompat getRoot() {
        if (SDK_INT >= 21) {
            return AccessibilityNodeInfoCompat.wrapNonNullInstance(
                    ((AccessibilityWindowInfo) mInfo).getRoot());
        } else {
            return null;
        }
    }

    /**
     * Gets the parent window if such.
     *
     * @return The parent window.
     */
    public AccessibilityWindowInfoCompat getParent() {
        if (SDK_INT >= 21) {
            return wrapNonNullInstance(((AccessibilityWindowInfo) mInfo).getParent());
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
            return ((AccessibilityWindowInfo) mInfo).getId();
        } else {
            return UNDEFINED;
        }
    }

    /**
     * Gets the bounds of this window in the screen.
     *
     * @param outBounds The out window bounds.
     */
    public void getBoundsInScreen(Rect outBounds) {
        if (SDK_INT >= 21) {
            ((AccessibilityWindowInfo) mInfo).getBoundsInScreen(outBounds);
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
            return ((AccessibilityWindowInfo) mInfo).isActive();
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
            return ((AccessibilityWindowInfo) mInfo).isFocused();
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
            return ((AccessibilityWindowInfo) mInfo).isAccessibilityFocused();
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
            return ((AccessibilityWindowInfo) mInfo).getChildCount();
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
    public AccessibilityWindowInfoCompat getChild(int index) {
        if (SDK_INT >= 21) {
            return wrapNonNullInstance(((AccessibilityWindowInfo) mInfo).getChild(index));
        } else {
            return null;
        }
    }

    /**
     * Gets the title of the window.
     *
     * @return The title of the window, or the application label for the window if no title was
     * explicitly set, or {@code null} if neither is available.
     */
    public CharSequence getTitle() {
        if (SDK_INT >= 24) {
            return ((AccessibilityWindowInfo) mInfo).getTitle();
        } else {
            return null;
        }
    }

    /**
     * Gets the node that anchors this window to another.
     *
     * @return The anchor node, or {@code null} if none exists.
     */
    public AccessibilityNodeInfoCompat getAnchor() {
        if (SDK_INT >= 24) {
            return AccessibilityNodeInfoCompat.wrapNonNullInstance(
                    ((AccessibilityWindowInfo) mInfo).getAnchor());
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
    public static AccessibilityWindowInfoCompat obtain() {
        if (SDK_INT >= 21) {
            return wrapNonNullInstance(AccessibilityWindowInfo.obtain());
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
    public static AccessibilityWindowInfoCompat obtain(AccessibilityWindowInfoCompat info) {
        if (SDK_INT >= 21) {
            return info == null
                    ? null
                    : wrapNonNullInstance(
                            AccessibilityWindowInfo.obtain((AccessibilityWindowInfo) info.mInfo));
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
     * @throws IllegalStateException If the info is already recycled.
     */
    public void recycle() {
        if (SDK_INT >= 21) {
            ((AccessibilityWindowInfo) mInfo).recycle();
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
        if (getClass() != obj.getClass()) {
            return false;
        }
        AccessibilityWindowInfoCompat other = (AccessibilityWindowInfoCompat) obj;
        if (mInfo == null) {
            if (other.mInfo != null) {
                return false;
            }
        } else if (!mInfo.equals(other.mInfo)) {
            return false;
        }
        return true;
    }

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
}
