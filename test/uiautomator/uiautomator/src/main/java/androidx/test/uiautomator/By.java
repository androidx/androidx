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

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import java.util.regex.Pattern;

/**
 * <p>{@link By} is a utility class which enables the creation of {@link BySelector}s in a concise
 * manner.</p>
 *
 * <p>Its primary function is to provide static factory methods for constructing {@link BySelector}s
 * using a shortened syntax. For example, you would use {@code findObject(By.text("foo"))} rather
 * than {@code findObject(new BySelector().text("foo"))} to select UI elements with the text value
 * "foo".</p>
 */
public class By {

    /** This class is not meant to be instanciated */
    private By() { }


    /**
     * Constructs a new {@link BySelector} and copies the criteria from {@code original}.
     */
    public static @NonNull BySelector copy(@NonNull BySelector original) {
        return new BySelector(original);
    }

    /**
     * Constructs a new {@link BySelector} and sets the class name criteria.
     *
     * @see BySelector#clazz(String)
     */
    public static @NonNull BySelector clazz(@NonNull String className) {
        return new BySelector().clazz(className);
    }

    /**
     * Constructs a new {@link BySelector} and sets the class name criteria.
     *
     * @see BySelector#clazz(String, String)
     */
    public static @NonNull BySelector clazz(@NonNull String packageName,
            @NonNull String className) {
        return new BySelector().clazz(packageName, className);
    }

    /**
     * Constructs a new {@link BySelector} and sets the class name criteria.
     *
     * @see BySelector#clazz(Class)
     */
    public static @NonNull BySelector clazz(@NonNull Class clazz) {
        return new BySelector().clazz(clazz);
    }

    /**
     * Constructs a new {@link BySelector} and sets the class name criteria.
     *
     * @see BySelector#clazz(Pattern)
     */
    public static @NonNull BySelector clazz(@NonNull Pattern className) {
        return new BySelector().clazz(className);
    }

    /**
     * Constructs a new {@link BySelector} and sets the content description criteria.
     *
     * @see BySelector#desc(String)
     */
    public static @NonNull BySelector desc(@NonNull String contentDescription) {
        return new BySelector().desc(contentDescription);
    }

    /**
     * Constructs a new {@link BySelector} and sets the content description criteria.
     *
     * @see BySelector#descContains(String)
     */
    public static @NonNull BySelector descContains(@NonNull String substring) {
        return new BySelector().descContains(substring);
    }

    /**
     * Constructs a new {@link BySelector} and sets the content description criteria.
     *
     * @see BySelector#descStartsWith(String)
     */
    public static @NonNull BySelector descStartsWith(@NonNull String substring) {
        return new BySelector().descStartsWith(substring);
    }

    /**
     * Constructs a new {@link BySelector} and sets the content description criteria.
     *
     * @see BySelector#descEndsWith(String)
     */
    public static @NonNull BySelector descEndsWith(@NonNull String substring) {
        return new BySelector().descEndsWith(substring);
    }

    /**
     * Constructs a new {@link BySelector} and sets the content description criteria.
     *
     * @see BySelector#desc(Pattern)
     */
    public static @NonNull BySelector desc(@NonNull Pattern contentDescription) {
        return new BySelector().desc(contentDescription);
    }

    /**
     * Constructs a new {@link BySelector} and sets the application package name criteria.
     *
     * @see BySelector#pkg(String)
     */
    public static @NonNull BySelector pkg(@NonNull String applicationPackage) {
        return new BySelector().pkg(applicationPackage);
    }

    /**
     * Constructs a new {@link BySelector} and sets the application package name criteria.
     *
     * @see BySelector#pkg(Pattern)
     */
    public static @NonNull BySelector pkg(@NonNull Pattern applicationPackage) {
        return new BySelector().pkg(applicationPackage);
    }

    /**
     * Constructs a new {@link BySelector} and sets the resource name criteria.
     *
     * @see BySelector#res(String)
     */
    public static @NonNull BySelector res(@NonNull String resourceName) {
        return new BySelector().res(resourceName);
    }

    /**
     * Constructs a new {@link BySelector} and sets the resource name criteria.
     *
     * @see BySelector#res(String, String)
     */
    public static @NonNull BySelector res(@NonNull String resourcePackage,
            @NonNull String resourceId) {
        return new BySelector().res(resourcePackage, resourceId);
    }

    /**
     * Constructs a new {@link BySelector} and sets the resource id criteria.
     *
     * @see BySelector#res(Pattern)
     */
    public static @NonNull BySelector res(@NonNull Pattern resourceName) {
        return new BySelector().res(resourceName);
    }

    /**
     * Constructs a new {@link BySelector} and sets the text value criteria.
     *
     * @see BySelector#text(String)
     */
    public static @NonNull BySelector text(@NonNull String text) {
        return new BySelector().text(text);
    }

    /**
     * Constructs a new {@link BySelector} and sets the text value criteria.
     *
     * @see BySelector#textContains(String)
     */
    public static @NonNull BySelector textContains(@NonNull String substring) {
        return new BySelector().textContains(substring);
    }

    /**
     * Constructs a new {@link BySelector} and sets the text value criteria.
     *
     * @see BySelector#textStartsWith(String)
     */
    public static @NonNull BySelector textStartsWith(@NonNull String substring) {
        return new BySelector().textStartsWith(substring);
    }

    /**
     * Constructs a new {@link BySelector} and sets the text value criteria.
     *
     * @see BySelector#textEndsWith(String)
     */
    public static @NonNull BySelector textEndsWith(@NonNull String substring) {
        return new BySelector().textEndsWith(substring);
    }

    /**
     * Constructs a new {@link BySelector} and sets the text value criteria.
     *
     * @see BySelector#text(Pattern)
     */
    public static @NonNull BySelector text(@NonNull Pattern regex) {
        return new BySelector().text(regex);
    }

    /**
     * Constructs a new {@link BySelector} and sets the checkable criteria.
     *
     * @see BySelector#checkable(boolean)
     */
    public static @NonNull BySelector checkable(boolean isCheckable) {
        return new BySelector().checkable(isCheckable);
    }

    /**
     * Constructs a new {@link BySelector} and sets the checked criteria.
     *
     * @see BySelector#checked(boolean)
     */
    public static @NonNull BySelector checked(boolean isChecked) {
        return new BySelector().checked(isChecked);
    }

    /**
     * Constructs a new {@link BySelector} and sets the clickable criteria.
     *
     * @see BySelector#clickable(boolean)
     */
    public static @NonNull BySelector clickable(boolean isClickable) {
        return new BySelector().clickable(isClickable);
    }

    /**
     * Constructs a new {@link BySelector} and sets the enabled criteria.
     *
     * @see BySelector#enabled(boolean)
     */
    public static @NonNull BySelector enabled(boolean isEnabled) {
        return new BySelector().enabled(isEnabled);
    }

    /**
     * Constructs a new {@link BySelector} and sets the focusable criteria.
     *
     * @see BySelector#focusable(boolean)
     */
    public static @NonNull BySelector focusable(boolean isFocusable) {
        return new BySelector().focusable(isFocusable);
    }

    /**
     * Constructs a new {@link BySelector} and sets the focused criteria.
     *
     * @see BySelector#focused(boolean)
     */
    public static @NonNull BySelector focused(boolean isFocused) {
        return new BySelector().focused(isFocused);
    }

    /**
     * Constructs a new {@link BySelector} and sets the long clickable criteria.
     *
     * @see BySelector#longClickable(boolean)
     */
    public static @NonNull BySelector longClickable(boolean isLongClickable) {
        return new BySelector().longClickable(isLongClickable);
    }

    /**
     * Constructs a new {@link BySelector} and sets the scrollable criteria.
     *
     * @see BySelector#scrollable(boolean)
     */
    public static @NonNull BySelector scrollable(boolean isScrollable) {
        return new BySelector().scrollable(isScrollable);
    }

    /**
     * Constructs a new {@link BySelector} and sets the selected criteria.
     *
     * @see BySelector#selected(boolean)
     */
    public static @NonNull BySelector selected(boolean isSelected) {
        return new BySelector().selected(isSelected);
    }

    /**
     * Constructs a new {@link BySelector} and sets the depth criteria.
     */
    public static @NonNull BySelector depth(int depth) {
        return new BySelector().depth(depth);
    }

    /**
     * Constructs a new {@link BySelector} and adds a parent selector criteria.
     *
     * @see BySelector#hasParent(BySelector)
     */
    public static @NonNull BySelector hasParent(@NonNull BySelector parentSelector) {
        return new BySelector().hasParent(parentSelector);
    }

    /**
     * Constructs a new {@link BySelector} and adds an ancestor selector criteria.
     *
     * @see BySelector#hasAncestor(BySelector)
     */
    public static @NonNull BySelector hasAncestor(@NonNull BySelector ancestorSelector) {
        return new BySelector().hasAncestor(ancestorSelector);
    }

    /**
     * Constructs a new {@link BySelector} and adds an ancestor selector criteria.
     *
     * @see BySelector#hasAncestor(BySelector, int)
     */
    public static @NonNull BySelector hasAncestor(@NonNull BySelector ancestorSelector,
            @IntRange(from = 1) int maxHeight) {
        return new BySelector().hasAncestor(ancestorSelector, maxHeight);
    }

    /**
     * Constructs a new {@link BySelector} and adds a child selector criteria.
     *
     * @see BySelector#hasChild(BySelector)
     */
    public static @NonNull BySelector hasChild(@NonNull BySelector childSelector) {
        return new BySelector().hasChild(childSelector);
    }

    /**
     * Constructs a new {@link BySelector} and adds a descendant selector criteria.
     *
     * @see BySelector#hasDescendant(BySelector)
     */
    public static @NonNull BySelector hasDescendant(@NonNull BySelector descendantSelector) {
        return new BySelector().hasDescendant(descendantSelector);
    }

    /**
     * Constructs a new {@link BySelector} and adds a descendant selector criteria.
     *
     * @see BySelector#hasDescendant(BySelector, int)
     */
    public static @NonNull BySelector hasDescendant(@NonNull BySelector descendantSelector,
            int maxDepth) {
        return new BySelector().hasDescendant(descendantSelector, maxDepth);
    }

}
