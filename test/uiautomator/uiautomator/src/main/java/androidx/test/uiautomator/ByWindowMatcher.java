/*
 * Copyright (C) 2025 The Android Open Source Project
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

import android.os.Build;
import android.view.Display;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import androidx.annotation.DoNotInline;
import androidx.annotation.RequiresApi;
import androidx.test.uiautomator.util.Traces;
import androidx.test.uiautomator.util.Traces.Section;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Provides utility methods for searching the {@link AccessibilityWindowInfo} hierarchy for windows
 * that match {@link ByWindowSelector}s.
 */
class ByWindowMatcher {

    private static final String TAG = ByWindowMatcher.class.getSimpleName();

    /**
     * This class should not be instantiated directly. Use the static search methods instead.
     */
    private ByWindowMatcher() {
    }

    /**
     * Searches the window hierarchy for the first window that matches the selector.
     *
     * <p>Note: For convenience, it searches in descending Z-order, ensuring the topmost window
     * that meets all other criteria is returned first. If no match is found on the first
     * attempt, it will run watchers and retry the search once.
     */
    static AccessibilityWindowInfo findMatch(UiDevice device, ByWindowSelector selector,
            AccessibilityWindowInfo... windows) {
        try (Section ignored = Traces.trace("ByWindowMatcher.findMatch")) {
            final int maxRetries = 1;
            for (int i = 0; i <= maxRetries; i++) {
                for (AccessibilityWindowInfo window : windows) {
                    if (matchesSelector(selector, window)) {
                        return window;
                    }
                }
                // No matches found, run watchers and try again if this is not the last attempt.
                if (i != maxRetries) {
                    device.runWatchers();
                }
            }
            return null;
        }
    }

    /**
     * Searches the window hierarchy for windows that match the selector.
     *
     * <p>Note: For convenience, it will return the windows in descending Z-order, ensuring the
     * topmost window comes first. If no matches are found on the first attempt, it will run
     * watchers and retry the search once.
     */
    static List<AccessibilityWindowInfo> findMatches(UiDevice device, ByWindowSelector selector,
            AccessibilityWindowInfo... windows) {
        try (Section ignored = Traces.trace("ByWindowMatcher.findMatches")) {
            List<AccessibilityWindowInfo> ret = new ArrayList<>();
            final int maxRetries = 1;
            for (int i = 0; i <= maxRetries; i++) {
                ret.clear();
                for (AccessibilityWindowInfo window : windows) {
                    if (matchesSelector(selector, window)) {
                        ret.add(window);
                    }
                }
                // If matches are found, or this is the last attempt, return the results.
                if (!ret.isEmpty() || i == maxRetries) {
                    return ret;
                }
                // No matches found, run watchers and try again.
                device.runWatchers();
            }
            return ret; // Should not be reached.
        }
    }

    /**
     * Returns true if the window matches the selector.
     *
     * @param selector window search criteria to match
     * @param window   window to check
     */
    private static boolean matchesSelector(ByWindowSelector selector,
            AccessibilityWindowInfo window) {
        return (selector.mMinLayer == null || window.getLayer() >= selector.mMinLayer)
                && (selector.mMaxLayer == null || window.getLayer() <= selector.mMaxLayer)
                && matchesTitle(selector.mTitle, window)
                && matchesCriteria(selector.mId, window.getId())
                && matchesDisplayId(selector.mDisplayId, window)
                && matchesCriteria(selector.mType, window.getType())
                && matchesCriteria(selector.mActive, window.isActive())
                && matchesCriteria(selector.mFocused, window.isFocused())
                && matchesPkg(selector.mPkg, window);
    }

    /** Returns true if the criteria is null or matches the value. */
    private static boolean matchesCriteria(Pattern criteria, CharSequence value) {
        if (criteria == null) {
            return true;
        }
        return criteria.matcher(value != null ? value : "").matches();
    }

    /** Returns true if the criteria is null or equal to the value. */
    private static boolean matchesCriteria(Boolean criteria, boolean value) {
        return criteria == null || criteria.equals(value);
    }

    /** Returns true if the criteria is null or equal to the value. */
    private static boolean matchesCriteria(Integer criteria, int value) {
        return criteria == null || criteria.equals(value);
    }

    /** Returns true if the criteria is null or equal to the title of window. */
    private static boolean matchesTitle(Pattern criteria, AccessibilityWindowInfo window) {
        if (criteria == null) {
            return true;
        }
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && matchesCriteria(criteria,
                Api24Impl.getTitle(window));
    }

    /** Returns true if the display ID criteria is null or equal to that of the window. */
    private static boolean matchesDisplayId(Integer criteria, AccessibilityWindowInfo window) {
        if (criteria == null) {
            return true;
        }
        int displayId = Display.DEFAULT_DISPLAY;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            displayId = Api30Impl.getDisplayId(window);
        }
        return matchesCriteria(criteria, displayId);
    }

    /** Returns true if the package name criteria is null or equal to that of the window. */
    private static boolean matchesPkg(Pattern criteria, AccessibilityWindowInfo window) {
        if (criteria == null) {
            return true;
        }
        AccessibilityNodeInfo root = window.getRoot();
        return root != null && matchesCriteria(criteria, root.getPackageName());
    }

    @RequiresApi(24)
    static class Api24Impl {
        private Api24Impl() {
        }

        @DoNotInline
        static CharSequence getTitle(AccessibilityWindowInfo window) {
            return window.getTitle();
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
