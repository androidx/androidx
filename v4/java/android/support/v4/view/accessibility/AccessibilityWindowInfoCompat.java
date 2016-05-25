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

import android.graphics.Rect;
import android.os.Build;

/**
 * Helper for accessing {@link android.view.accessibility.AccessibilityWindowInfo}
 * introduced after API level 4 in a backwards compatible fashion.
 */
public class AccessibilityWindowInfoCompat {

    private static interface AccessibilityWindowInfoImpl {
        public Object obtain();
        public Object obtain(Object info);
        public int getType(Object info);
        public int getLayer(Object info);
        public Object getRoot(Object info);
        public Object getParent(Object info);
        public int getId(Object info);
        public void getBoundsInScreen(Object info, Rect outBounds);
        public boolean isActive(Object info);
        public boolean isFocused(Object info);
        public boolean isAccessibilityFocused(Object info);
        public int getChildCount(Object info);
        public Object getChild(Object info, int index);
        public void recycle(Object info);
    }

    private static class AccessibilityWindowInfoStubImpl implements  AccessibilityWindowInfoImpl {

        @Override
        public Object obtain() {
            return null;
        }

        @Override
        public Object obtain(Object info) {
            return null;
        }

        @Override
        public int getType(Object info) {
            return UNDEFINED;
        }

        @Override
        public int getLayer(Object info) {
            return UNDEFINED;
        }

        @Override
        public Object getRoot(Object info) {
            return null;
        }

        @Override
        public Object getParent(Object info) {
            return null;
        }

        @Override
        public int getId(Object info) {
            return UNDEFINED;
        }

        @Override
        public void getBoundsInScreen(Object info, Rect outBounds) {
        }

        @Override
        public boolean isActive(Object info) {
            return true;
        }

        @Override
        public boolean isFocused(Object info) {
            return true;
        }

        @Override
        public boolean isAccessibilityFocused(Object info) {
            return true;
        }

        @Override
        public int getChildCount(Object info) {
            return 0;
        }

        @Override
        public Object getChild(Object info, int index) {
            return null;
        }

        @Override
        public void recycle(Object info) {
        }
    }

    private static class AccessibilityWindowInfoApi21Impl extends AccessibilityWindowInfoStubImpl {
        @Override
        public Object obtain() {
            return AccessibilityWindowInfoCompatApi21.obtain();
        }

        @Override
        public Object obtain(Object info) {
            return AccessibilityWindowInfoCompatApi21.obtain(info);
        }

        @Override
        public int getType(Object info) {
            return AccessibilityWindowInfoCompatApi21.getType(info);
        }

        @Override
        public int getLayer(Object info) {
            return AccessibilityWindowInfoCompatApi21.getLayer(info);
        }

        @Override
        public Object getRoot(Object info) {
            return AccessibilityWindowInfoCompatApi21.getRoot(info);
        }

        @Override
        public Object getParent(Object info) {
            return AccessibilityWindowInfoCompatApi21.getParent(info);
        }

        @Override
        public int getId(Object info) {
            return AccessibilityWindowInfoCompatApi21.getId(info);
        }

        @Override
        public void getBoundsInScreen(Object info, Rect outBounds) {
            AccessibilityWindowInfoCompatApi21.getBoundsInScreen(info, outBounds);
        }

        @Override
        public boolean isActive(Object info) {
            return AccessibilityWindowInfoCompatApi21.isActive(info);
        }

        @Override
        public boolean isFocused(Object info) {
            return AccessibilityWindowInfoCompatApi21.isFocused(info);
        }

        @Override
        public boolean isAccessibilityFocused(Object info) {
            return AccessibilityWindowInfoCompatApi21.isAccessibilityFocused(info);
        }

        @Override
        public int getChildCount(Object info) {
            return AccessibilityWindowInfoCompatApi21.getChildCount(info);
        }

        @Override
        public Object getChild(Object info, int index) {
            return AccessibilityWindowInfoCompatApi21.getChild(info, index);
        }

        @Override
        public void recycle(Object info) {
            AccessibilityWindowInfoCompatApi21.recycle(info);
        }
    }

    static {
        if (Build.VERSION.SDK_INT >= 21) {
            IMPL = new AccessibilityWindowInfoApi21Impl();
        } else {
            IMPL = new AccessibilityWindowInfoStubImpl();
        }
    }

    private static final AccessibilityWindowInfoImpl IMPL;
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
        return IMPL.getType(mInfo);
    }

    /**
     * Gets the layer which determines the Z-order of the window. Windows
     * with greater layer appear on top of windows with lesser layer.
     *
     * @return The window layer.
     */
    public int getLayer() {
        return IMPL.getLayer(mInfo);
    }

    /**
     * Gets the root node in the window's hierarchy.
     *
     * @return The root node.
     */
    public AccessibilityNodeInfoCompat getRoot() {
        return AccessibilityNodeInfoCompat.wrapNonNullInstance(IMPL.getRoot(mInfo));
    }

    /**
     * Gets the parent window if such.
     *
     * @return The parent window.
     */
    public AccessibilityWindowInfoCompat getParent() {
        return wrapNonNullInstance(IMPL.getParent(mInfo));
    }

    /**
     * Gets the unique window id.
     *
     * @return windowId The window id.
     */
    public int getId() {
        return IMPL.getId(mInfo);
    }

    /**
     * Gets the bounds of this window in the screen.
     *
     * @param outBounds The out window bounds.
     */
    public void getBoundsInScreen(Rect outBounds) {
        IMPL.getBoundsInScreen(mInfo, outBounds);
    }

    /**
     * Gets if this window is active. An active window is the one
     * the user is currently touching or the window has input focus
     * and the user is not touching any window.
     *
     * @return Whether this is the active window.
     */
    public boolean isActive() {
        return IMPL.isActive(mInfo);
    }

    /**
     * Gets if this window has input focus.
     *
     * @return Whether has input focus.
     */
    public boolean isFocused() {
        return IMPL.isFocused(mInfo);
    }

    /**
     * Gets if this window has accessibility focus.
     *
     * @return Whether has accessibility focus.
     */
    public boolean isAccessibilityFocused() {
        return IMPL.isAccessibilityFocused(mInfo);
    }

    /**
     * Gets the number of child windows.
     *
     * @return The child count.
     */
    public int getChildCount() {
        return IMPL.getChildCount(mInfo);
    }

    /**
     * Gets the child window at a given index.
     *
     * @param index The index.
     * @return The child.
     */
    public AccessibilityWindowInfoCompat getChild(int index) {
        return wrapNonNullInstance(IMPL.getChild(mInfo, index));
    }

    /**
     * Returns a cached instance if such is available or a new one is
     * created.
     *
     * @return An instance.
     */
    public static AccessibilityWindowInfoCompat obtain() {
        return wrapNonNullInstance(IMPL.obtain());
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
        return wrapNonNullInstance(IMPL.obtain(info.mInfo));
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
        IMPL.recycle(mInfo);
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
