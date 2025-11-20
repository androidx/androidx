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

import static android.view.Display.INVALID_DISPLAY;
import static android.view.accessibility.AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY;
import static android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION;
import static android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD;
import static android.view.accessibility.AccessibilityWindowInfo.TYPE_MAGNIFICATION_OVERLAY;
import static android.view.accessibility.AccessibilityWindowInfo.TYPE_SPLIT_SCREEN_DIVIDER;
import static android.view.accessibility.AccessibilityWindowInfo.TYPE_SYSTEM;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import static java.util.Objects.requireNonNull;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.test.uiautomator.util.Patterns;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.regex.Pattern;

/**
 * A {@link ByWindowSelector} specifies criteria for matching UI windows during a call to {@link
 * UiDevice#findWindow(ByWindowSelector)}.
 */
public class ByWindowSelector {

    @RestrictTo(LIBRARY_GROUP)
    @IntDef({
            TYPE_APPLICATION,
            TYPE_INPUT_METHOD,
            TYPE_SYSTEM,
            TYPE_ACCESSIBILITY_OVERLAY,
            TYPE_SPLIT_SCREEN_DIVIDER,
            TYPE_MAGNIFICATION_OVERLAY
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface WindowType {
    }

    // Regex patterns for String criteria
    Pattern mTitle;
    Pattern mPkg;

    Integer mId; // Unique ID
    Integer mDisplayId; // Display ID
    Integer mType;

    // Layer (Z-order) criteria
    Integer mMinLayer;
    Integer mMaxLayer;

    Boolean mActive;
    Boolean mFocused;

    /**
     * Clients should not instantiate this class directly. Use the {@link By} factory class instead.
     */
    ByWindowSelector() {
        final int defaultDisplayId = Configurator.getInstance().getDefaultDisplayId();
        mDisplayId = defaultDisplayId == INVALID_DISPLAY ? null : defaultDisplayId;
    }

    /**
     * Constructs a new {@link ByWindowSelector} and copies the criteria from {@code original}.
     *
     * @param original The {@link ByWindowSelector} to copy.
     */
    ByWindowSelector(ByWindowSelector original) {
        mTitle = original.mTitle;
        mPkg = original.mPkg;

        mId = original.mId;
        mDisplayId = original.mDisplayId;
        mType = original.mType;

        mMinLayer = original.mMinLayer;
        mMaxLayer = original.mMaxLayer;

        mActive = original.mActive;
        mFocused = original.mFocused;
    }

    /**
     * Sets the type criteria for window matching. A UI window will be considered a match if
     * its type exactly matches the {@code type} and all other criteria for this
     * selector are met.
     *
     * @param type The exact type to match (e.g.
     *             {@link android.view.accessibility.AccessibilityWindowInfo#TYPE_APPLICATION}).
     * @return A reference to this {@link ByWindowSelector}.
     */
    public @NonNull ByWindowSelector type(@WindowType int type) {
        if (mType != null) {
            throw new IllegalStateException("Type selector is already defined");
        }
        mType = type;
        return this;
    }

    /**
     * Sets the title criteria for matching. A UI window will be considered a match if its title
     * exactly matches the {@code title} parameter and all other criteria for this selector are met.
     *
     * @param title The exact value to match (case-sensitive).
     * @return A reference to this {@link ByWindowSelector}.
     */
    public @NonNull ByWindowSelector title(@NonNull String title) {
        requireNonNull(title, "title cannot be null");
        return title(Pattern.compile(Pattern.quote(title)));
    }

    /**
     * Sets the title criteria for matching. A UI window will be considered a match if its title
     * contains the {@code substring} and all other criteria for this selector are met.
     *
     * @param substring The substring to match (case-sensitive).
     * @return A reference to this {@link ByWindowSelector}.
     */
    public @NonNull ByWindowSelector titleContains(@NonNull String substring) {
        requireNonNull(substring, "substring cannot be null");
        return title(Patterns.contains(substring));
    }

    /**
     * Sets the title criteria for matching. A UI window will be considered a match if its title
     * starts with the {@code prefix} and all other criteria for this selector are met.
     *
     * @param prefix The prefix to match (case-sensitive).
     * @return A reference to this {@link ByWindowSelector}.
     */
    public @NonNull ByWindowSelector titleStartsWith(@NonNull String prefix) {
        requireNonNull(prefix, "prefix cannot be null");
        return title(Patterns.startsWith(prefix));
    }

    /**
     * Sets the title criteria for matching. A UI window will be considered a match if its title
     * ends with the {@code suffix} and all other criteria for this selector are met.
     *
     * @param suffix The suffix to match (case-sensitive).
     * @return A reference to this {@link ByWindowSelector}.
     */
    public @NonNull ByWindowSelector titleEndsWith(@NonNull String suffix) {
        requireNonNull(suffix, "suffix cannot be null");
        return title(Patterns.endsWith(suffix) + "$");
    }

    /**
     * Sets the title criteria for matching. A UI window will be considered a match if its title
     * matches the {@code title} {@link Pattern} and all other criteria for this selector are met.
     *
     * @param title The {@link Pattern} to be used for window matching.
     * @return A reference to this {@link ByWindowSelector}.
     */
    public @NonNull ByWindowSelector title(@NonNull Pattern title) {
        requireNonNull(title, "title cannot be null");
        if (mTitle != null) {
            throw new IllegalStateException("Title selector is already defined");
        }
        mTitle = title;
        return this;
    }

    /**
     * Sets the id criteria for matching. A UI window will be considered a match if its id
     * matches the {@code id} parameter and all other criteria for this selector are met.
     *
     * @param id The id to match.
     * @return A reference to this {@link ByWindowSelector}.
     */
    public @NonNull ByWindowSelector id(int id) {
        if (mId != null) {
            throw new IllegalStateException("Id selector is already defined");
        }
        mId = id;
        return this;
    }

    /**
     * Adds a display ID selector criteria for matching. A UI element will be considered a match
     * if it is within the display with the ID of {@code displayId} and all other criteria for
     * this selector are met.
     *
     * @param displayId The display ID to match. Use Display#getDisplayId() to get the ID.
     * @return A reference to this {@link ByWindowSelector}.
     */
    @RequiresApi(30)
    public @NonNull ByWindowSelector displayId(int displayId) {
        if (mDisplayId != null) {
            throw new IllegalStateException("Display ID selector is already defined");
        }
        mDisplayId = displayId;
        return this;
    }

    /**
     * Sets the layer (Z-order) criteria for matching. A UI window will be considered a match if its
     * layer matches the {@code layer} parameter and all other criteria for this selector are met.
     *
     * @param exactLayer The layer to match.
     * @return A reference to this {@link ByWindowSelector}.
     */
    public @NonNull ByWindowSelector layer(@IntRange(from = 0) int exactLayer) {
        if (exactLayer < 0) {
            throw new IllegalArgumentException("layer cannot be negative");
        }
        mMinLayer = exactLayer;
        mMaxLayer = exactLayer;
        return this;
    }

    /**
     * Sets the layer (Z-order) criteria for finding windows that are layered above the specified
     * reference layer.
     *
     * @param referenceLayer The layer which matching windows must be above.
     * @return A reference to this {@link ByWindowSelector}.
     */
    public @NonNull ByWindowSelector layerAbove(@IntRange(from = 0) int referenceLayer) {
        if (referenceLayer < 0) {
            throw new IllegalArgumentException("referenceLayer cannot be negative");
        }
        if (mMinLayer != null) {
            throw new IllegalStateException("Min Layer selector is already defined");
        }
        mMinLayer = referenceLayer + 1;
        return this;
    }

    /**
     * Sets the layer (Z-order) criteria for finding windows that are layered below the specified
     * reference layer.
     *
     * @param referenceLayer The layer which matching windows must be below. must be positive.
     * @return A reference to this {@link ByWindowSelector}.
     */
    public @NonNull ByWindowSelector layerBelow(@IntRange(from = 1) int referenceLayer) {
        if (referenceLayer < 1) {
            throw new IllegalArgumentException("referenceLayer must be positive");
        }
        if (mMaxLayer != null) {
            throw new IllegalStateException("Max Layer selector is already defined");
        }
        mMaxLayer = referenceLayer - 1;
        return this;
    }

    /**
     * Sets the search criteria to match windows that are active or inactive.
     *
     * @param isActive Whether to match windows that are active or inactive.
     * @return A reference to this {@link ByWindowSelector}.
     */
    public @NonNull ByWindowSelector active(boolean isActive) {
        if (mActive != null) {
            throw new IllegalStateException("active selector is already defined");
        }
        mActive = isActive;
        return this;
    }

    /**
     * Sets the search criteria to match windows that are focused or not.
     *
     * @param isFocused Whether to match windows that are focused or not.
     * @return A reference to this {@link ByWindowSelector}.
     */
    public @NonNull ByWindowSelector focused(boolean isFocused) {
        if (mFocused != null) {
            throw new IllegalStateException("focused selector is already defined");
        }
        mFocused = isFocused;
        return this;
    }

    /**
     * Sets the package name criteria for window matching. A UI window will be considered a match if
     * its package name exactly matches the {@code packageName} and all other criteria for this
     * selector are met.
     *
     * @param packageName The exact package name to match (case-sensitive). e.g. "com.example
     *                    .android"
     * @return A reference to this {@link ByWindowSelector}.
     */
    public @NonNull ByWindowSelector pkg(@NonNull String packageName) {
        requireNonNull(packageName, "packageName cannot be null");
        return pkg(Pattern.compile(Pattern.quote(packageName)));
    }

    /**
     * Sets the package name criteria for window matching. A UI window will be considered a match if
     * its package name exactly matches the {@code packageName} and all other criteria for this
     * selector are met.
     *
     * @param packageName The {@link Pattern} to be used for window matching.
     * @return A reference to this {@link ByWindowSelector}.
     */
    public @NonNull ByWindowSelector pkg(@NonNull Pattern packageName) {
        requireNonNull(packageName, "packageName cannot be null");
        if (mPkg != null) {
            throw new IllegalStateException("Package name selector is already defined");
        }
        mPkg = packageName;
        return this;
    }

    /**
     * Returns a {@link String} representation of this {@link ByWindowSelector}. The format is
     * "ByWindowSelector [&lt;KEY&gt;='&lt;VALUE&gt;', ... ]". Each criteria is listed as a
     * key-value pair where the key is the name of the criteria expressed in all caps (e.g. TYPE,
     * TITLE, etc).
     */
    @Override
    public @NonNull String toString() {
        StringBuilder builder = new StringBuilder("ByWindowSelector [");
        if (mType != null) {
            builder.append("TYPE='").append(windowTypeToString(mType)).append("', ");
        }
        if (mTitle != null) {
            builder.append("TITLE='").append(mTitle).append("', ");
        }
        if (mId != null) {
            builder.append("ID='").append(mId).append("', ");
        }
        // Print the exact layer or range of layers. e.g. "LAYER=3" (exact), "LAYER=3-5" (min-max),
        // "LAYER=3-" (min only), "LAYER=-5" (max only)
        if (mMinLayer != null && mMinLayer.equals(mMaxLayer)) {
            builder.append("LAYER='").append(mMinLayer).append("', ");
        } else if (mMinLayer != null || mMaxLayer != null) {
            builder.append("LAYER='")
                    .append(mMinLayer == null ? "" : mMinLayer).append("-")
                    .append(mMaxLayer == null ? "" : mMaxLayer).append("', ");
        }
        if (mDisplayId != null) {
            builder.append("DISPLAY_ID='").append(mDisplayId).append("', ");
        }
        if (mActive != null) {
            builder.append("ACTIVE='").append(mActive).append("', ");
        }
        if (mFocused != null) {
            builder.append("FOCUSED='").append(mFocused).append("', ");
        }
        if (mPkg != null) {
            builder.append("PKG='").append(mPkg).append("', ");
        }
        builder.setLength(builder.length() - 2);
        builder.append("]");
        return builder.toString();
    }

    // @see AccessibilityWindowInfo#typeToString() that is hide annotated.
    private static String windowTypeToString(int windowType) {
        if (windowType == TYPE_APPLICATION) {
            return "TYPE_APPLICATION";
        } else if (windowType == TYPE_INPUT_METHOD) {
            return "TYPE_INPUT_METHOD";
        } else if (windowType == TYPE_SYSTEM) {
            return "TYPE_SYSTEM";
        } else if (windowType == TYPE_ACCESSIBILITY_OVERLAY) {
            return "TYPE_ACCESSIBILITY_OVERLAY";
        } else if (windowType == TYPE_SPLIT_SCREEN_DIVIDER) {
            return "TYPE_SPLIT_SCREEN_DIVIDER";
        } else if (windowType == TYPE_MAGNIFICATION_OVERLAY) {
            return "TYPE_MAGNIFICATION_OVERLAY";
        }
        return "<UNKNOWN:" + windowType + ">";
    }
}
