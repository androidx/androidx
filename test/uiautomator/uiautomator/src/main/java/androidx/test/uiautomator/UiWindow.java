/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.test.uiautomator;

import android.annotation.SuppressLint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.view.Display;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import androidx.annotation.DoNotInline;
import androidx.annotation.RequiresApi;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a UI window on the screen and provides methods to access its properties and perform
 * actions.
 *
 * <p>Similar to {@link UiObject2} that wraps {@link AccessibilityNodeInfo}, a {@link UiWindow}
 * wraps an {@link AccessibilityWindowInfo}. This represents a UI window displayed on the screen for
 * accessibility purposes, including interactive windows like dialogs or popup windows.
 * It is important to note that a UiWindow instance caches a snapshot of a window state. If the
 * underlying window closes or changes and it becomes stale, {@link UiWindow} will automatically
 * check and refresh its state internally before executing any methods.
 *
 * <p>Requires API level 24 or higher for multi-window mode.
 */
public final class UiWindow implements Searchable {

    private static final String TAG = UiWindow.class.getSimpleName();

    private final UiDevice mDevice;
    private AccessibilityWindowInfo mCachedWindow;

    private UiWindow(@NonNull UiDevice device, @NonNull AccessibilityWindowInfo cachedWindow) {
        mDevice = device;
        mCachedWindow = cachedWindow;
    }

    /** Creates a new {@link UiWindow} instance in the package. `cachedWindow` must not be null. */
    static UiWindow create(@NonNull UiDevice device,
            @NonNull AccessibilityWindowInfo cachedWindow) {
        return new UiWindow(device, cachedWindow);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof UiWindow)) {
            return false;
        }
        try {
            UiWindow other = (UiWindow) object;
            return getAccessibilityWindowInfo().equals(other.getAccessibilityWindowInfo());
        } catch (StaleObjectException e) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        try {
            return getAccessibilityWindowInfo().hashCode();
        } catch (StaleObjectException e) {
            return mCachedWindow.hashCode();
        }
    }

    // Search functions

    /**
     * Returns {@code true} if there is an element in the window which matches the {@code selector}.
     */
    @Override
    public boolean hasObject(@NonNull BySelector selector) {
        UiObject2 root = getRootObject();
        if (root == null) {
            return false;
        }
        return root.hasObject(selector);
    }

    /**
     * Searches all elements in the window root and returns the first one to match the {@code
     * selector}, or {@code null} if no matching objects are found in this window.
     */
    // TODO(b/430126405): Check if the nullability check could be enabled.
    @Override
    @SuppressLint("UnknownNullness")
    public UiObject2 findObject(@NonNull BySelector selector) {
        UiObject2 root = getRootObject();
        if (root == null) {
            return null;
        }
        return root.findObject(selector);
    }

    /**
     * Searches all elements under in the window root and returns those that match the {@code
     * selector}.
     */
    @Override
    public @NonNull List<UiObject2> findObjects(@NonNull BySelector selector) {
        UiObject2 root = getRootObject();
        if (root == null) {
            return new ArrayList<>();
        }
        return root.findObjects(selector);
    }

    // Attribute accessors

    /**
     * Returns the underlying {@link AccessibilityWindowInfo}.
     *
     * <p>Note: The returned info reflects the state at the time the {@link UiWindow} was created
     * . To avoid access to stale window, it refreshes the window info before returning it.
     *
     * @throws IllegalStateException if the underlying {@link AccessibilityWindowInfo} has not
     *                               been set up.
     */
    @NonNull
    private AccessibilityWindowInfo getAccessibilityWindowInfo() {
        if (mCachedWindow == null) {
            throw new IllegalStateException("This window has already been recycled.");
        }

        getDevice().waitForIdle();
        if (!refresh()) {
            getDevice().runWatchers();
            if (!refresh()) {
                throw new StaleObjectException();
            }
        }
        return mCachedWindow;
    }

    /**
     * Refreshes this window's state if it is stale.
     *
     * <p>Ideally, this would call the hidden {@code AccessibilityWindowInfo.refresh()} method
     * for an in-place update. Since it is unavailable, for older API levels, it uses a fallback
     * ({@link #refreshFromPool()}) that finds the window info from the refreshed root node.
     *
     * @return {@code true} if the refresh succeeded, {@code false} if this window is stale and
     * no longer exists.
     */
    private boolean refresh() {
        // TODO(b/405371739): Extend this to handle different API level behaviors when the
        //  refresh API is available in the future SDK.
        return refreshFromPool();
    }

    /**
     * Refreshes this window by replacing it with an updated instance from the pool.
     *
     * <p>This is a fallback for SDKs where the refresh system API is unavailable. To refresh the
     * info, it returns the old cached window to the pool, then obtains a new one (which may or
     * may not be the one just recycled) from the pool based on the refreshed root node.
     * <p>Note: This will effectively update the window but not in-place, so the underlying cached
     * window may reference a new object each time this method is called.
     */
    private boolean refreshFromPool() {
        AccessibilityNodeInfo rootNode = mCachedWindow.getRoot();
        if (rootNode == null) {
            return false;
        }
        AccessibilityWindowInfo refreshedWindow = null;
        try {
            if (!rootNode.refresh()) {
                return false;
            }
            refreshedWindow = rootNode.getWindow();
            if (refreshedWindow == null || refreshedWindow.getId() != mCachedWindow.getId()) {
                return false;
            }
            // This is a best effort to keep the cached window referencing the same object from
            // the pool by recycling the old info then immediately acquiring it back for the
            // refreshed info.
            mCachedWindow.recycle();
            mCachedWindow = AccessibilityWindowInfo.obtain(refreshedWindow);
            return true;
        } finally {
            if (rootNode != null) {
                rootNode.recycle();
            }
            if (refreshedWindow != null) {
                refreshedWindow.recycle();
            }
        }
    }

    /**
     * Gets the root node in the window as a {@link UiObject2}.
     *
     * @return The root {@link UiObject2}, or {@code null} if the root node is not available or
     * cannot be wrapped. The returned {@link UiObject2} should be recycled by the caller if it
     * holds onto the reference.
     * @see AccessibilityWindowInfo#getRoot()
     */
    @Nullable
    public UiObject2 getRootObject() {
        AccessibilityNodeInfo rootNode = getAccessibilityWindowInfo().getRoot();
        if (rootNode == null) {
            return null;
        }

        // Create a UiObject2 instance for the root node.
        // A null root selector is used here. This is acceptable because this selector is used
        // only for instance creation and parent retrieval, which the root node doesn't need.
        return UiObject2.create(mDevice, null, rootNode);
    }

    /** Returns the ID of this window. */
    public int getId() {
        return getAccessibilityWindowInfo().getId();
    }

    /**
     * Gets the ID of the display containing this window.
     *
     * @return The current display ID, or {@link Display#DEFAULT_DISPLAY} if the API level is < 30.
     * @see AccessibilityWindowInfo#getDisplayId()
     */
    public int getDisplayId() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Api30Impl.getDisplayId(getAccessibilityWindowInfo());
        } else {
            return Display.DEFAULT_DISPLAY;
        }
    }

    /**
     * Returns the type of the window (e.g., {@link AccessibilityWindowInfo#TYPE_APPLICATION}).
     *
     * @return The type.
     * @see AccessibilityWindowInfo#getType()
     */
    public int getType() {
        return getAccessibilityWindowInfo().getType();
    }

    /**
     * Returns the layer (Z-order) of the window. Higher layers are drawn on top of lower layers.
     *
     * @return The layer rank.
     * @see AccessibilityWindowInfo#getLayer()
     */
    public int getLayer() {
        return getAccessibilityWindowInfo().getLayer();
    }

    /**
     * Returns the title of the window as a String.
     *
     * @return The title as a String, or null if the window has no title, or if API level < 24.
     * @see AccessibilityWindowInfo#getTitle()
     */
    public @Nullable String getTitle() {
        CharSequence title = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            title = Api24Impl.getTitle(getAccessibilityWindowInfo());
        }
        return title != null ? title.toString() : null;
    }

    /** Returns this window's visible bounds clipped to the size of the display. */
    public @NonNull Rect getVisibleBounds() {
        return getVisibleBounds(getAccessibilityWindowInfo());
    }

    /**
     * Returns this window's visible bounds clipped to the size of the display.
     *
     * @param window The {@link AccessibilityWindowInfo} of the window.
     * @return The visible bounds of the window.
     */
    private Rect getVisibleBounds(@NonNull AccessibilityWindowInfo window) {
        Rect windowBounds = new Rect();
        window.getBoundsInScreen(windowBounds);

        final boolean isDisplayAccessible = getDevice().getDisplayById(getDisplayId()) != null;
        // Trim the window bounds to the display bounds if the display is accessible.
        if (isDisplayAccessible) {
            Point displaySize = getDevice().getDisplaySize(getDisplayId());
            Rect screen = new Rect(0, 0, displaySize.x, displaySize.y);
            if (!windowBounds.intersect(screen)) {
                // If no overlap between the window and display bounds, ignoring.
            }
        }
        return windowBounds;
    }

    /**
     * Returns the name of the package that this window belongs to.
     *
     * @return The name of the package, or {@code null} if the root object is not available.
     */
    public @Nullable String getPackageName() {
        UiObject2 root = getRootObject();
        if (root == null) {
            return null;
        }
        return root.getApplicationPackage();
    }

    /**
     * Returns if this window is active (the one the user is currently interacting with).
     *
     * @return {@code true} if the window is active.
     * @see AccessibilityWindowInfo#isActive()
     */
    public boolean isActive() {
        return getAccessibilityWindowInfo().isActive();
    }

    /**
     * Returns if this window has input focus.
     *
     * @return {@code true} if this window has input focus.
     * @see AccessibilityWindowInfo#isFocused()
     */
    public boolean isFocused() {
        return getAccessibilityWindowInfo().isFocused();
    }

    /**
     * Returns if the window is in picture-in-picture mode.
     *
     * @return {@code true} if the window is in picture-in-picture mode, {@code false} otherwise
     * or if
     * API level is < 26.
     * @see AccessibilityWindowInfo#isInPictureInPictureMode()
     */
    public boolean isInPictureInPictureMode() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Api26Impl.isInPictureInPictureMode(
                getAccessibilityWindowInfo());
    }

    UiDevice getDevice() {
        return mDevice;
    }

    @Override
    public @NonNull String toString() {
        if (mCachedWindow == null) {
            throw new IllegalStateException("UiWindow has not been set up.");
        }
        // Print the cached window info, avoiding refreshing the window info.
        return mCachedWindow + ", root=[" + (mCachedWindow.getRoot() == null ? "null"
                : mCachedWindow.getRoot()) + "]";
    }

    // Inner classes for API level compatibility

    @RequiresApi(24)
    static class Api24Impl {
        private Api24Impl() {
        }

        @DoNotInline
        static CharSequence getTitle(AccessibilityWindowInfo window) {
            return window.getTitle();
        }
    }

    @RequiresApi(26)
    static class Api26Impl {
        private Api26Impl() {
        }

        @DoNotInline
        static boolean isInPictureInPictureMode(AccessibilityWindowInfo window) {
            return window.isInPictureInPictureMode();
        }
    }

    @RequiresApi(30)
    static class Api30Impl {
        private Api30Impl() {
        }

        @DoNotInline
        static int getDisplayId(AccessibilityWindowInfo window) {
            return window.getDisplayId();
        }
    }
}
