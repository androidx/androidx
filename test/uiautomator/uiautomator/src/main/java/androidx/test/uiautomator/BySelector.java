/*
 * Copyright (C) 2014 The Android Open Source Project
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

import static java.util.Objects.requireNonNull;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.test.uiautomator.util.Patterns;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A {@link BySelector} specifies criteria for matching UI elements during a call to
 * {@link UiDevice#findObject(BySelector)}.
 */
public class BySelector {

    // Regex patterns for String criteria
    Pattern mClazz;
    Pattern mDesc;
    Pattern mPkg;
    Pattern mRes;
    Pattern mText;
    Pattern mHint;

    // Boolean criteria
    Boolean mChecked;
    Boolean mCheckable;
    Boolean mClickable;
    Boolean mEnabled;
    Boolean mFocused;
    Boolean mFocusable;
    Boolean mLongClickable;
    Boolean mScrollable;
    Boolean mSelected;

    // Depth restrictions
    Integer mMinDepth;
    Integer mMaxDepth;

    // Display ID
    Integer mDisplayId;

    // Ancestor selector
    BySelector mAncestorSelector;
    Integer mMaxAncestorDistance;

    // Child selectors
    final List<BySelector> mChildSelectors = new LinkedList<>();


    /** Clients should not instanciate this class directly. Use the {@link By} factory class instead. */
    BySelector() { }

    /**
     * Constructs a new {@link BySelector} and copies the criteria from {@code original}.
     *
     * @param original The {@link BySelector} to copy.
     */
    BySelector(BySelector original) {
        mClazz = original.mClazz;
        mDesc  = original.mDesc;
        mPkg   = original.mPkg;
        mRes   = original.mRes;
        mText  = original.mText;
        mHint  = original.mHint;

        mChecked       = original.mChecked;
        mCheckable     = original.mCheckable;
        mClickable     = original.mClickable;
        mEnabled       = original.mEnabled;
        mFocused       = original.mFocused;
        mFocusable     = original.mFocusable;
        mLongClickable = original.mLongClickable;
        mScrollable    = original.mScrollable;
        mSelected      = original.mSelected;

        mMinDepth = original.mMinDepth;
        mMaxDepth = original.mMaxDepth;

        mDisplayId = original.mDisplayId;

        mAncestorSelector = original.mAncestorSelector == null ? null :
                new BySelector(original.mAncestorSelector);
        mMaxAncestorDistance = original.mMaxAncestorDistance;

        for (BySelector childSelector : original.mChildSelectors) {
            mChildSelectors.add(new BySelector(childSelector));
        }
    }

    /**
     * Sets the class name criteria for matching. A UI element will be considered a match if its
     * class name exactly matches the {@code className} parameter and all other criteria for
     * this selector are met. If {@code className} starts with a period, it is assumed to be in the
     * {@link android.widget} package.
     *
     * @param className The full class name value to match.
     * @return A reference to this {@link BySelector}.
     */
    public @NonNull BySelector clazz(@NonNull String className) {
        requireNonNull(className, "className cannot be null");
        // If className starts with a period, assume the package is 'android.widget'
        if (className.charAt(0) == '.') {
            return clazz("android.widget", className.substring(1));
        } else {
            return clazz(Pattern.compile(Pattern.quote(className)));
        }
    }

    /**
     * Sets the class name criteria for matching. A UI element will be considered a match if its
     * package and class name exactly match the {@code packageName} and {@code className} parameters
     * and all other criteria for this selector are met.
     *
     * @param packageName The package value to match.
     * @param className The class name value to match.
     * @return A reference to this {@link BySelector}.
     */
    public @NonNull BySelector clazz(@NonNull String packageName, @NonNull String className) {
        requireNonNull(packageName, "packageName cannot be null");
        requireNonNull(className, "className cannot be null");
        return clazz(Pattern.compile(Pattern.quote(
                String.format("%s.%s", packageName, className))));
    }

    /**
     * Sets the class name criteria for matching. A UI element will be considered a match if its
     * class name matches {@code clazz} and all other criteria for this selector are met.
     *
     * @param clazz The class to match.
     * @return A reference to this {@link BySelector}
     */
    public @NonNull BySelector clazz(@NonNull Class clazz) {
        requireNonNull(clazz, "clazz cannot be null");
        return clazz(Pattern.compile(Pattern.quote(clazz.getName())));
    }

    /**
     * Sets the class name criteria for matching. A UI element will be considered a match if its
     * full class name matches the {@code className} {@link Pattern} and all other criteria for this
     * selector are met.
     *
     * @param className The {@link Pattern} to be used for matching.
     * @return A reference to this {@link BySelector}.
     */
    public @NonNull BySelector clazz(@NonNull Pattern className) {
        requireNonNull(className, "className cannot be null");
        if (mClazz != null) {
            throw new IllegalStateException("Class selector is already defined");
        }
        mClazz = className;
        return this;
    }

    /**
     * Sets the content description criteria for matching. A UI element will be considered a match
     * if its content description exactly matches the {@code contentDescription} parameter and all
     * other criteria for this selector are met.
     *
     * @param contentDescription The exact value to match (case-sensitive).
     * @return A reference to this {@link BySelector}.
     */
    public @NonNull BySelector desc(@NonNull String contentDescription) {
        requireNonNull(contentDescription, "contentDescription cannot be null");
        return desc(Pattern.compile(Pattern.quote(contentDescription)));
    }

    /**
     * Sets the content description criteria for matching. A UI element will be considered a match
     * if its content description contains the {@code substring} parameter and all other criteria
     * for this selector are met.
     *
     * @param substring The substring to match (case-sensitive).
     * @return A reference to this {@link BySelector}.
     */
    public @NonNull BySelector descContains(@NonNull String substring) {
        requireNonNull(substring, "substring cannot be null");
        return desc(Patterns.contains(substring));
    }

    /**
     * Sets the content description criteria for matching. A UI element will be considered a match
     * if its content description starts with the {@code prefix} parameter and all other criteria
     * for this selector are met.
     *
     * @param prefix The prefix to match (case-sensitive).
     * @return A reference to this {@link BySelector}.
     */
    public @NonNull BySelector descStartsWith(@NonNull String prefix) {
        requireNonNull(prefix, "prefix cannot be null");
        return desc(Patterns.startsWith(prefix));
    }

    /**
     * Sets the content description criteria for matching. A UI element will be considered a match
     * if its content description ends with the {@code suffix} parameter and all other criteria
     * for this selector are met.
     *
     * @param suffix The suffix to match (case-sensitive).
     * @return A reference to this {@link BySelector}.
     */
    public @NonNull BySelector descEndsWith(@NonNull String suffix) {
        requireNonNull(suffix, "suffix cannot be null");
        return desc(Patterns.endsWith(suffix));
    }

    /**
     * Sets the content description criteria for matching. A UI element will be considered a match
     * if its content description matches the {@code contentDescription} {@link Pattern} and all
     * other criteria for this selector are met.
     *
     * @param contentDescription The {@link Pattern} to be used for matching.
     * @return A reference to this {@link BySelector}.
     */
    public @NonNull BySelector desc(@NonNull Pattern contentDescription) {
        requireNonNull(contentDescription, "contentDescription cannot be null");
        if (mDesc != null) {
            throw new IllegalStateException("Description selector is already defined");
        }
        mDesc = contentDescription;
        return this;
    }

    /**
     * Sets the application package name criteria for matching. A UI element will be considered a
     * match if its application package name exactly matches the {@code applicationPackage}
     * parameter and all other criteria for this selector are met.
     *
     * @param applicationPackage The exact value to match.
     * @return A reference to this {@link BySelector}.
     */
    public @NonNull BySelector pkg(@NonNull String applicationPackage) {
        requireNonNull(applicationPackage, "applicationPackage cannot be null");
        return pkg(Pattern.compile(Pattern.quote(applicationPackage)));
    }

    /**
     * Sets the package name criteria for matching. A UI element will be considered a match if its
     * application package name matches the {@code applicationPackage} {@link Pattern} and all other
     * criteria for this selector are met.
     *
     * @param applicationPackage The {@link Pattern} to be used for matching.
     * @return A reference to this {@link BySelector}.
     */
    public @NonNull BySelector pkg(@NonNull Pattern applicationPackage) {
        requireNonNull(applicationPackage, "applicationPackage cannot be null");
        if (mPkg != null) {
            throw new IllegalStateException("Package selector is already defined");
        }
        mPkg = applicationPackage;
        return this;
    }

    /**
     * Sets the resource name criteria for matching. A UI element will be considered a match if its
     * resource name exactly matches the {@code resourceName} parameter and all other criteria for
     * this selector are met.
     *
     * @param resourceName The exact value to match.
     * @return A reference to this {@link BySelector}.
     */
    public @NonNull BySelector res(@NonNull String resourceName) {
        requireNonNull(resourceName, "resourceName cannot be null");
        return res(Pattern.compile(Pattern.quote(resourceName)));
    }

    /**
     * Sets the resource name criteria for matching. A UI element will be considered a match if its
     * resource package and resource id exactly match the {@code resourcePackage} and
     * {@code resourceId} parameters and all other criteria for this selector are met.
     *
     * @param resourcePackage The resource package value to match.
     * @param resourceId The resouce-id value to match.
     * @return A reference to this {@link BySelector}.
     */
    public @NonNull BySelector res(@NonNull String resourcePackage, @NonNull String resourceId) {
        requireNonNull(resourcePackage, "resourcePackage cannot be null");
        requireNonNull(resourceId, "resourceId cannot be null");
        return res(Pattern.compile(Pattern.quote(
                String.format("%s:id/%s", resourcePackage, resourceId))));
    }

    /**
     * Sets the resource name criteria for matching. A UI element will be considered a match if its
     * resource name matches the {@code resourceName} {@link Pattern} and all other criteria for
     * this selector are met.
     *
     * @param resourceName The {@link Pattern} to be used for matching.
     * @return A reference to this {@link BySelector}.
     */
    public @NonNull BySelector res(@NonNull Pattern resourceName) {
        requireNonNull(resourceName, "resourceName cannot be null");
        if (mRes != null) {
            throw new IllegalStateException("Resource name selector is already defined");
        }
        mRes = resourceName;
        return this;
    }

    /**
     * Sets the text value criteria for matching. A UI element will be considered a match if its
     * text value exactly matches the {@code textValue} parameter and all other criteria for this
     * selector are met.
     *
     * @param textValue The exact value to match (case-sensitive).
     * @return A reference to this {@link BySelector}.
     */
    public @NonNull BySelector text(@NonNull String textValue) {
        requireNonNull(textValue, "textValue cannot be null");
        return text(Pattern.compile(Pattern.quote(textValue)));
    }

    /**
     * Sets the text value criteria for matching. A UI element will be considered a match if its
     * text value contains the {@code substring} parameter and all other criteria for this selector
     * are met.
     *
     * @param substring The substring to match (case-sensitive).
     * @return A reference to this {@link BySelector}.
     */
    public @NonNull BySelector textContains(@NonNull String substring) {
        requireNonNull(substring, "substring cannot be null");
        return text(Patterns.contains(substring));
    }

    /**
     * Sets the text value criteria for matching. A UI element will be considered a match if its
     * text value starts with the {@code prefix} parameter and all other criteria for this
     * selector are met.
     *
     * @param prefix The prefix to match (case-sensitive).
     * @return A reference to this {@link BySelector}.
     */
    public @NonNull BySelector textStartsWith(@NonNull String prefix) {
        requireNonNull(prefix, "prefix cannot be null");
        return text(Patterns.startsWith(prefix));
    }

    /**
     * Sets the text value criteria for matching. A UI element will be considered a match if its
     * text value ends with the {@code suffix} parameter and all other criteria for this selector
     * are met.
     *
     * @param suffix The suffix to match (case-sensitive).
     * @return A reference to this {@link BySelector}.
     */
    public @NonNull BySelector textEndsWith(@NonNull String suffix) {
        requireNonNull(suffix, "suffix cannot be null");
        return text(Patterns.endsWith(suffix));
    }

    /** Sets the text value criteria for matching. A UI element will be considered a match if its
     * text value matches the {@code textValue} {@link Pattern} and all other criteria for this
     * selector are met.
     *
     * @param textValue The {@link Pattern} to be used for matching.
     * @return A reference to this {@link BySelector}.
     */
    public @NonNull BySelector text(@NonNull Pattern textValue) {
        requireNonNull(textValue, "textValue cannot be null");
        if (mText != null) {
            throw new IllegalStateException("Text selector is already defined");
        }
        mText = textValue;
        return this;
    }

    /**
     * Sets the hint value criteria for matching. A UI element will be considered a match if its
     * hint value exactly matches the {@code hintValue} parameter and all other criteria for this
     * selector are met.
     * <p>Hint text is displayed when there's no user input text.
     *
     * @param hintValue The exact value to match (case-sensitive).
     * @return A reference to this {@link BySelector}.
     */
    @RequiresApi(26)
    public @NonNull BySelector hint(@NonNull String hintValue) {
        requireNonNull(hintValue, "hintValue cannot be null");
        return hint(Pattern.compile(Pattern.quote(hintValue)));
    }

    /**
     * Sets the hint value criteria for matching. A UI element will be considered a match if its
     * hint value contains the {@code substring} parameter and all other criteria for this selector
     * are met.
     * <p>Hint text is displayed when there's no user input text.
     *
     * @param substring The substring to match (case-sensitive).
     * @return A reference to this {@link BySelector}.
     */
    @RequiresApi(26)
    public @NonNull BySelector hintContains(@NonNull String substring) {
        requireNonNull(substring, "substring cannot be null");
        return hint(Patterns.contains(substring));
    }

    /**
     * Sets the hint value criteria for matching. A UI element will be considered a match if its
     * hint value starts with the {@code prefix} parameter and all other criteria for this
     * selector are met.
     * <p>Hint text is displayed when there's no user input text.
     *
     * @param prefix The prefix to match (case-sensitive).
     * @return A reference to this {@link BySelector}.
     */
    @RequiresApi(26)
    public @NonNull BySelector hintStartsWith(@NonNull String prefix) {
        requireNonNull(prefix, "prefix cannot be null");
        return hint(Patterns.startsWith(prefix));
    }

    /**
     * Sets the hint value criteria for matching. A UI element will be considered a match if its
     * hint value ends with the {@code suffix} parameter and all other criteria for this selector
     * are met.
     * <p>Hint text is displayed when there's no user input text.
     *
     * @param suffix The suffix to match (case-sensitive).
     * @return A reference to this {@link BySelector}.
     */
    @RequiresApi(26)
    public @NonNull BySelector hintEndsWith(@NonNull String suffix) {
        requireNonNull(suffix, "suffix cannot be null");
        return hint(Patterns.endsWith(suffix));
    }

    /**
     * Sets the hint value criteria for matching. A UI element will be considered a match if its
     * hint value matches the {@code hintValue} {@link Pattern} and all other criteria for this
     * selector are met.
     * <p>Hint text is displayed when there's no user input text.
     *
     * @param hintValue The {@link Pattern} to be used for matching.
     * @return A reference to this {@link BySelector}.
     */
    @RequiresApi(26)
    public @NonNull BySelector hint(@NonNull Pattern hintValue) {
        requireNonNull(hintValue, "hintValue cannot be null");
        if (mHint != null) {
            throw new IllegalStateException("Hint selector is already defined");
        }
        mHint = hintValue;
        return this;
    }


    /**
     * Sets the search criteria to match elements that are checkable or not checkable.
     *
     * @param isCheckable Whether to match elements that are checkable or elements that are not
     * checkable.
     * @return A reference to this {@link BySelector}.
     */
    public @NonNull BySelector checkable(boolean isCheckable) {
        if (mCheckable != null) {
            throw new IllegalStateException("Checkable selector is already defined");
        }
        mCheckable = isCheckable;
        return this;
    }

    /**
     * Sets the search criteria to match elements that are checked or unchecked.
     *
     * @param isChecked Whether to match elements that are checked or elements that are unchecked.
     * @return A reference to this {@link BySelector}.
     */
    public @NonNull BySelector checked(boolean isChecked) {
        if (mChecked != null) {
            throw new IllegalStateException("Checked selector is already defined");
        }
        mChecked = isChecked;
        return this;
    }

    /**
     * Sets the search criteria to match elements that are clickable or not clickable.
     *
     * @param isClickable Whether to match elements that are clickable or elements that are not
     * clickable.
     * @return A reference to this {@link BySelector}.
     */
    public @NonNull BySelector clickable(boolean isClickable) {
        if (mClickable != null) {
            throw new IllegalStateException("Clickable selector is already defined");
        }
        mClickable = isClickable;
        return this;
    }
    /**
     * Sets the search criteria to match elements that are enabled or disabled.
     *
     * @param isEnabled Whether to match elements that are enabled or elements that are disabled.
     * @return A reference to this {@link BySelector}.
     */
    public @NonNull BySelector enabled(boolean isEnabled) {
        if (mEnabled != null) {
            throw new IllegalStateException("Enabled selector is already defined");
        }
        mEnabled = isEnabled;
        return this;
    }

    /**
     * Sets the search criteria to match elements that are focusable or not focusable.
     *
     * @param isFocusable Whether to match elements that are focusable or elements that are not
     * focusable.
     * @return A reference to this {@link BySelector}.
     */
    public @NonNull BySelector focusable(boolean isFocusable) {
        if (mFocusable != null) {
            throw new IllegalStateException("Focusable selector is already defined");
        }
        mFocusable = isFocusable;
        return this;
    }

    /**
     * Sets the search criteria to match elements that are focused or unfocused.
     *
     * @param isFocused Whether to match elements that are focused or elements that are unfocused.
     * @return A reference to this {@link BySelector}.
     */
    public @NonNull BySelector focused(boolean isFocused) {
        if (mFocused != null) {
            throw new IllegalStateException("Focused selector is already defined");
        }
        mFocused = isFocused;
        return this;
    }

    /**
     * Sets the search criteria to match elements that are long clickable or not long clickable.
     *
     * @param isLongClickable Whether to match elements that are long clickable or elements that are
     * not long clickable.
     * @return A reference to this {@link BySelector}.
     */
    public @NonNull BySelector longClickable(boolean isLongClickable) {
        if (mLongClickable != null) {
            throw new IllegalStateException("Long Clickable selector is already defined");
        }
        mLongClickable = isLongClickable;
        return this;
    }

    /**
     * Sets the search criteria to match elements that are scrollable or not scrollable.
     *
     * @param isScrollable Whether to match elements that are scrollable or elements that are not
     * scrollable.
     * @return A reference to this {@link BySelector}.
     */
    public @NonNull BySelector scrollable(boolean isScrollable) {
        if (mScrollable != null) {
            throw new IllegalStateException("Scrollable selector is already defined");
        }
        mScrollable = isScrollable;
        return this;
    }

    /**
     * Sets the search criteria to match elements that are selected or not selected.
     *
     * @param isSelected Whether to match elements that are selected or elements that are not
     * selected.
     * @return A reference to this {@link BySelector}.
     */
    public @NonNull BySelector selected(boolean isSelected) {
        if (mSelected != null) {
            throw new IllegalStateException("Selected selector is already defined");
        }
        mSelected = isSelected;
        return this;
    }

    /** Sets the search criteria to match elements that are at a certain depth. */
    public @NonNull BySelector depth(int exactDepth) {
        return depth(exactDepth, exactDepth);
    }

    /** Sets the search criteria to match elements that are in a range of depths. */
    public @NonNull BySelector depth(int min, int max) {
        if (min < 0) {
            throw new IllegalArgumentException("min cannot be negative");
        }
        if (max < 0) {
            throw new IllegalArgumentException("max cannot be negative");
        }
        if (mMinDepth != null) {
            throw new IllegalStateException("Minimum Depth selector is already defined");
        }
        if (mMaxDepth != null) {
            throw new IllegalStateException("Maximum Depth selector is already defined");
        }
        mMinDepth = min;
        mMaxDepth = max;
        return this;
    }

    /** Sets the search criteria to match elements that are at least a certain depth. */
    public @NonNull BySelector minDepth(int min) {
        if (min < 0) {
            throw new IllegalArgumentException("min cannot be negative");
        }
        if (mMinDepth != null) {
            throw new IllegalStateException("Depth selector is already defined");
        }
        mMinDepth = min;
        return this;
    }

    /** Sets the search criteria to match elements that are no more than a certain depth. */
    public @NonNull BySelector maxDepth(int max) {
        if (max < 0) {
            throw new IllegalArgumentException("max cannot be negative");
        }
        if (mMaxDepth != null) {
            throw new IllegalStateException("Depth selector is already defined");
        }
        mMaxDepth = max;
        return this;
    }

    /**
     * Adds a display ID selector criteria for matching. A UI element will be considered a match
     * if it is within the display with the ID of {@code displayId} and all other criteria for
     * this selector are met.
     *
     * @param displayId The display ID to match. Use {@link Display#getDisplayId()} to get the ID.
     * @return A reference to this {@link BySelector}.
     */
    @RequiresApi(30)
    public @NonNull BySelector displayId(int displayId) {
        if (mDisplayId != null) {
            throw new IllegalStateException("Display ID selector is already defined");
        }
        mDisplayId = displayId;
        return this;
    }

    /**
     * Adds a parent selector criteria for matching. A UI element will be considered a match if it
     * has a parent element (direct ancestor) which matches the {@code parentSelector} and all
     * other criteria for this selector are met.
     *
     * @param parentSelector The selector used to find a matching parent element.
     * @return A reference to this {@link BySelector}.
     */
    public @NonNull BySelector hasParent(@NonNull BySelector parentSelector) {
        requireNonNull(parentSelector, "parentSelector cannot be null");
        return hasAncestor(parentSelector, 1);
    }

    /**
     * Adds an ancestor selector criteria for matching. A UI element will be considered a match if
     * it has an ancestor element which matches the {@code ancestorSelector} and all other
     * criteria for this selector are met.
     *
     * @param ancestorSelector The selector used to find a matching ancestor element.
     * @return A reference to this {@link BySelector}.
     */
    public @NonNull BySelector hasAncestor(@NonNull BySelector ancestorSelector) {
        requireNonNull(ancestorSelector, "ancestorSelector cannot be null");
        if (mAncestorSelector != null) {
            throw new IllegalStateException("Parent/ancestor selector is already defined");
        }
        mAncestorSelector = ancestorSelector;
        return this;
    }

    /**
     * Adds an ancestor selector criteria for matching. A UI element will be considered a match if
     * it has an ancestor element which matches the {@code ancestorSelector} and all other
     * criteria for this selector are met.
     *
     * @param ancestorSelector    The selector used to find a matching ancestor element.
     * @param maxAncestorDistance The maximum distance between the element and its relevant
     *                            ancestor in the view hierarchy, e.g. 1 only matches the parent
     *                            element, 2 matches parent or grandparent.
     * @return A reference to this {@link BySelector}.
     */
    public @NonNull BySelector hasAncestor(@NonNull BySelector ancestorSelector,
            @IntRange(from = 1) int maxAncestorDistance) {
        hasAncestor(ancestorSelector);
        mMaxAncestorDistance = maxAncestorDistance;
        return this;
    }

    /**
     * Adds a child selector criteria for matching. A UI element will be considered a match if it
     * has a child element (direct descendant) which matches the {@code childSelector} and all
     * other criteria for this selector are met. If specified more than once, matches must be found
     * for all {@code childSelector}s.
     *
     * @param childSelector The selector used to find a matching child element.
     * @return A reference to this {@link BySelector}.
     * @throws IllegalArgumentException if the selector has a parent/ancestor selector
     */
    public @NonNull BySelector hasChild(@NonNull BySelector childSelector) {
        requireNonNull(childSelector, "childSelector cannot be null");
        return hasDescendant(childSelector, 1);
    }

    /**
     * Adds a descendant selector criteria for matching. A UI element will be considered a match if
     * it has a descendant element which matches the {@code descendantSelector} and all other
     * criteria for this selector are met. If specified more than once, matches must be found for
     * all {@code descendantSelector}s.
     *
     * @param descendantSelector The selector used to find a matching descendant element.
     * @return A reference to this {@link BySelector}.
     * @throws IllegalArgumentException if the selector has a parent/ancestor selector
     */
    public @NonNull BySelector hasDescendant(@NonNull BySelector descendantSelector) {
        requireNonNull(descendantSelector, "descendantSelector cannot be null");
        if (descendantSelector.mAncestorSelector != null) {
            // Search root is ambiguous with nested parent selectors.
            throw new IllegalArgumentException(
                    "Nested parent/ancestor selectors are not supported");
        }
        mChildSelectors.add(descendantSelector);
        return this;
    }

    /**
     * Adds a descendant selector criteria for matching. A UI element will be considered a match if
     * it has a descendant element which matches the {@code descendantSelector} and all other
     * criteria for this selector are met. If specified more than once, matches must be found for
     * all {@code descendantSelector}s.
     *
     * @param descendantSelector The selector used to find a matching descendant element.
     * @param maxDepth The maximum depth under the element to search the descendant.
     * @return A reference to this {@link BySelector}.
     * @throws IllegalArgumentException if the selector has a parent/ancestor selector
     */
    public @NonNull BySelector hasDescendant(@NonNull BySelector descendantSelector, int maxDepth) {
        hasDescendant(descendantSelector);
        descendantSelector.mMaxDepth = maxDepth;
        return this;
    }

    /**
     * Returns a {@link String} representation of this {@link BySelector}. The format is
     * "BySelector [&lt;KEY&gt;='&lt;VALUE&gt; ... ]". Each criteria is listed as a key-value pair
     * where the key is the name of the criteria expressed in all caps (e.g. CLAZZ, RES, etc).
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("BySelector [");
        if (mClazz != null) {
            builder.append("CLASS='").append(mClazz).append("', ");
        }
        if (mDesc != null) {
            builder.append("DESC='").append(mDesc).append("', ");
        }
        if (mPkg != null) {
            builder.append("PKG='").append(mPkg).append("', ");
        }
        if (mRes != null) {
            builder.append("RES='").append(mRes).append("', ");
        }
        if (mText != null) {
            builder.append("TEXT='").append(mText).append("', ");
        }
        if (mHint != null) {
            builder.append("HINT='").append(mHint).append("', ");
        }
        if (mChecked != null) {
            builder.append("CHECKED='").append(mChecked).append("', ");
        }
        if (mCheckable != null) {
            builder.append("CHECKABLE='").append(mCheckable).append("', ");
        }
        if (mClickable != null) {
            builder.append("CLICKABLE='").append(mClickable).append("', ");
        }
        if (mEnabled != null) {
            builder.append("ENABLED='").append(mEnabled).append("', ");
        }
        if (mFocused != null) {
            builder.append("FOCUSED='").append(mFocused).append("', ");
        }
        if (mFocusable != null) {
            builder.append("FOCUSABLE='").append(mFocusable).append("', ");
        }
        if (mLongClickable != null) {
            builder.append("LONGCLICKABLE='").append(mLongClickable).append("', ");
        }
        if (mScrollable != null) {
            builder.append("SCROLLABLE='").append(mScrollable).append("', ");
        }
        if (mSelected != null) {
            builder.append("SELECTED='").append(mSelected).append("', ");
        }
        if (mDisplayId != null) {
            builder.append("DISPLAY_ID='").append(mDisplayId).append("', ");
        }
        if (mAncestorSelector != null) {
            builder.append("ANCESTOR='")
                    .append(mAncestorSelector.toString().substring(11))
                    .append("', ");
        }
        for (BySelector childSelector : mChildSelectors) {
            builder.append("CHILD='").append(childSelector.toString().substring(11)).append("', ");
        }
        builder.setLength(builder.length() - 2);
        builder.append("]");
        return builder.toString();
    }
}
