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
import androidx.annotation.RequiresApi;

import org.jspecify.annotations.NonNull;

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
     * @see BySelector#clazz(String) BySelector.clazz(String)
     */
    public static @NonNull BySelector clazz(@NonNull String className) {
        return new BySelector().clazz(className);
    }

    /**
     * Constructs a new {@link BySelector} and sets the class name criteria.
     *
     * @see BySelector#clazz(String, String) BySelector.clazz(String, String)
     */
    public static @NonNull BySelector clazz(@NonNull String packageName,
            @NonNull String className) {
        return new BySelector().clazz(packageName, className);
    }

    /**
     * Constructs a new {@link BySelector} and sets the class name criteria.
     *
     * @see BySelector#clazz(Class) BySelector.clazz(Class)
     */
    public static @NonNull BySelector clazz(@NonNull Class clazz) {
        return new BySelector().clazz(clazz);
    }

    /**
     * Constructs a new {@link BySelector} and sets the class name criteria.
     *
     * @see BySelector#clazz(Pattern) BySelector.clazz(Pattern)
     */
    public static @NonNull BySelector clazz(@NonNull Pattern className) {
        return new BySelector().clazz(className);
    }

    /**
     * Constructs a new {@link BySelector} and sets the content description criteria.
     *
     * @see BySelector#desc(String) BySelector.desc(String)
     */
    public static @NonNull BySelector desc(@NonNull String contentDescription) {
        return new BySelector().desc(contentDescription);
    }

    /**
     * Constructs a new {@link BySelector} and sets the content description criteria.
     *
     * @see BySelector#descContains(String) BySelector.descContains(String)
     */
    public static @NonNull BySelector descContains(@NonNull String substring) {
        return new BySelector().descContains(substring);
    }

    /**
     * Constructs a new {@link BySelector} and sets the content description criteria.
     *
     * @see BySelector#descStartsWith(String) BySelector.descStartsWith(String)
     */
    public static @NonNull BySelector descStartsWith(@NonNull String prefix) {
        return new BySelector().descStartsWith(prefix);
    }

    /**
     * Constructs a new {@link BySelector} and sets the content description criteria.
     *
     * @see BySelector#descEndsWith(String) BySelector.descEndsWith(String)
     */
    public static @NonNull BySelector descEndsWith(@NonNull String suffix) {
        return new BySelector().descEndsWith(suffix);
    }

    /**
     * Constructs a new {@link BySelector} and sets the content description criteria.
     *
     * @see BySelector#desc(Pattern) BySelector.desc(Pattern)
     */
    public static @NonNull BySelector desc(@NonNull Pattern contentDescription) {
        return new BySelector().desc(contentDescription);
    }

    /**
     * Constructs a new {@link BySelector} and sets the application package name criteria.
     *
     * @see BySelector#pkg(String) BySelector.pkg(String)
     */
    public static @NonNull BySelector pkg(@NonNull String applicationPackage) {
        return new BySelector().pkg(applicationPackage);
    }

    /**
     * Constructs a new {@link BySelector} and sets the application package name criteria.
     *
     * @see BySelector#pkg(Pattern) BySelector.pkg(Pattern)
     */
    public static @NonNull BySelector pkg(@NonNull Pattern applicationPackage) {
        return new BySelector().pkg(applicationPackage);
    }

    /**
     * Constructs a new {@link BySelector} and sets the resource name criteria.
     *
     * @see BySelector#res(String) BySelector.res(String)
     */
    public static @NonNull BySelector res(@NonNull String resourceName) {
        return new BySelector().res(resourceName);
    }

    /**
     * Constructs a new {@link BySelector} and sets the resource name criteria.
     *
     * @see BySelector#res(String, String) BySelector.res(String, String)
     */
    public static @NonNull BySelector res(@NonNull String resourcePackage,
            @NonNull String resourceId) {
        return new BySelector().res(resourcePackage, resourceId);
    }

    /**
     * Constructs a new {@link BySelector} and sets the resource id criteria.
     *
     * @see BySelector#res(Pattern) BySelector.res(Pattern)
     */
    public static @NonNull BySelector res(@NonNull Pattern resourceName) {
        return new BySelector().res(resourceName);
    }

    /**
     * Constructs a new {@link BySelector} and sets the text value criteria.
     *
     * @see BySelector#text(String) BySelector.text(String)
     */
    public static @NonNull BySelector text(@NonNull String text) {
        return new BySelector().text(text);
    }

    /**
     * Constructs a new {@link BySelector} and sets the text value criteria.
     *
     * @see BySelector#textContains(String) BySelector.textContains(String)
     */
    public static @NonNull BySelector textContains(@NonNull String substring) {
        return new BySelector().textContains(substring);
    }

    /**
     * Constructs a new {@link BySelector} and sets the text value criteria.
     *
     * @see BySelector#textStartsWith(String) BySelector.textStartsWith(String)
     */
    public static @NonNull BySelector textStartsWith(@NonNull String prefix) {
        return new BySelector().textStartsWith(prefix);
    }

    /**
     * Constructs a new {@link BySelector} and sets the text value criteria.
     *
     * @see BySelector#textEndsWith(String) BySelector.textEndsWith(String)
     */
    public static @NonNull BySelector textEndsWith(@NonNull String suffix) {
        return new BySelector().textEndsWith(suffix);
    }

    /**
     * Constructs a new {@link BySelector} and sets the text value criteria.
     *
     * @see BySelector#text(Pattern) BySelector.text(Pattern)
     */
    public static @NonNull BySelector text(@NonNull Pattern regex) {
        return new BySelector().text(regex);
    }

    /**
     * Constructs a new {@link BySelector} and sets the hint value criteria.
     *
     * @see BySelector#hint(String) BySelector.hint(String)
     */
    @RequiresApi(26)
    public static @NonNull BySelector hint(@NonNull String hint) {
        return new BySelector().hint(hint);
    }

    /**
     * Constructs a new {@link BySelector} and sets the hint value criteria.
     *
     * @see BySelector#hintContains(String) BySelector.hintContains(String)
     */
    @RequiresApi(26)
    public static @NonNull BySelector hintContains(@NonNull String substring) {
        return new BySelector().hintContains(substring);
    }

    /**
     * Constructs a new {@link BySelector} and sets the hint value criteria.
     *
     * @see BySelector#hintStartsWith(String) BySelector.hintStartsWith(String)
     */
    @RequiresApi(26)
    public static @NonNull BySelector hintStartsWith(@NonNull String prefix) {
        return new BySelector().hintStartsWith(prefix);
    }

    /**
     * Constructs a new {@link BySelector} and sets the hint value criteria.
     *
     * @see BySelector#hintEndsWith(String) BySelector.hintEndsWith(String)
     */
    @RequiresApi(26)
    public static @NonNull BySelector hintEndsWith(@NonNull String suffix) {
        return new BySelector().hintEndsWith(suffix);
    }

    /**
     * Constructs a new {@link BySelector} and sets the hint value criteria.
     *
     * @see BySelector#hint(Pattern) BySelector.hint(Pattern)
     */
    @RequiresApi(26)
    public static @NonNull BySelector hint(@NonNull Pattern regex) {
        return new BySelector().hint(regex);
    }

    /**
     * Constructs a new {@link BySelector} and sets the checkable criteria.
     *
     * @see BySelector#checkable(boolean) BySelector.checkable(boolean)
     */
    public static @NonNull BySelector checkable(boolean isCheckable) {
        return new BySelector().checkable(isCheckable);
    }

    /**
     * Constructs a new {@link BySelector} and sets the checked criteria.
     *
     * @see BySelector#checked(boolean) BySelector.checked(boolean)
     */
    public static @NonNull BySelector checked(boolean isChecked) {
        return new BySelector().checked(isChecked);
    }

    /**
     * Constructs a new {@link BySelector} and sets the clickable criteria.
     *
     * @see BySelector#clickable(boolean) BySelector.clickable(boolean)
     */
    public static @NonNull BySelector clickable(boolean isClickable) {
        return new BySelector().clickable(isClickable);
    }

    /**
     * Constructs a new {@link BySelector} and sets the enabled criteria.
     *
     * @see BySelector#enabled(boolean) BySelector.enabled(boolean)
     */
    public static @NonNull BySelector enabled(boolean isEnabled) {
        return new BySelector().enabled(isEnabled);
    }

    /**
     * Constructs a new {@link BySelector} and sets the focusable criteria.
     *
     * @see BySelector#focusable(boolean) BySelector.focusable(boolean)
     */
    public static @NonNull BySelector focusable(boolean isFocusable) {
        return new BySelector().focusable(isFocusable);
    }

    /**
     * Constructs a new {@link BySelector} and sets the focused criteria.
     *
     * @see BySelector#focused(boolean) BySelector.focused(boolean)
     */
    public static @NonNull BySelector focused(boolean isFocused) {
        return new BySelector().focused(isFocused);
    }

    /**
     * Constructs a new {@link BySelector} and sets the long clickable criteria.
     *
     * @see BySelector#longClickable(boolean) BySelector.longClickable(boolean)
     */
    public static @NonNull BySelector longClickable(boolean isLongClickable) {
        return new BySelector().longClickable(isLongClickable);
    }

    /**
     * Constructs a new {@link BySelector} and sets the scrollable criteria.
     *
     * @see BySelector#scrollable(boolean) BySelector.scrollable(boolean)
     */
    public static @NonNull BySelector scrollable(boolean isScrollable) {
        return new BySelector().scrollable(isScrollable);
    }

    /**
     * Constructs a new {@link BySelector} and sets the selected criteria.
     *
     * @see BySelector#selected(boolean) BySelector.selected(boolean)
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
     * Constructs a new {@link BySelector} and sets the display ID criteria.
     *
     * @see BySelector#displayId(int) BySelector.displayId(int)
     */
    @RequiresApi(30)
    public static @NonNull BySelector displayId(int displayId) {
        return new BySelector().displayId(displayId);
    }

    /**
     * Constructs a new {@link BySelector} and adds a parent selector criteria.
     *
     * @see BySelector#hasParent(BySelector) BySelector.hasParent(BySelector)
     */
    public static @NonNull BySelector hasParent(@NonNull BySelector parentSelector) {
        return new BySelector().hasParent(parentSelector);
    }

    /**
     * Constructs a new {@link BySelector} and adds an ancestor selector criteria.
     *
     * @see BySelector#hasAncestor(BySelector) BySelector.hasAncestor(BySelector)
     */
    public static @NonNull BySelector hasAncestor(@NonNull BySelector ancestorSelector) {
        return new BySelector().hasAncestor(ancestorSelector);
    }

    /**
     * Constructs a new {@link BySelector} and adds an ancestor selector criteria.
     *
     * @see BySelector#hasAncestor(BySelector, int) BySelector.hasAncestor(BySelector, int)
     */
    public static @NonNull BySelector hasAncestor(@NonNull BySelector ancestorSelector,
            @IntRange(from = 1) int maxAncestorDistance) {
        return new BySelector().hasAncestor(ancestorSelector, maxAncestorDistance);
    }

    /**
     * Constructs a new {@link BySelector} and adds a child selector criteria.
     *
     * @see BySelector#hasChild(BySelector) BySelector.hasChild(BySelector)
     */
    public static @NonNull BySelector hasChild(@NonNull BySelector childSelector) {
        return new BySelector().hasChild(childSelector);
    }

    /**
     * Constructs a new {@link BySelector} and adds a descendant selector criteria.
     *
     * @see BySelector#hasDescendant(BySelector) BySelector.hasDescendant(BySelector)
     */
    public static @NonNull BySelector hasDescendant(@NonNull BySelector descendantSelector) {
        return new BySelector().hasDescendant(descendantSelector);
    }

    /**
     * Constructs a new {@link BySelector} and adds a descendant selector criteria.
     *
     * @see BySelector#hasDescendant(BySelector, int) BySelector.hasDescendant(BySelector, int)
     */
    public static @NonNull BySelector hasDescendant(@NonNull BySelector descendantSelector,
            int maxDepth) {
        return new BySelector().hasDescendant(descendantSelector, maxDepth);
    }

    /**
     * This nested class is used to create a {@link ByWindowSelector} that matches a window.
     */
    public static class Window {
        private Window() { }

        /**
         * Constructs a new {@link ByWindowSelector} and sets the type criteria.
         *
         * @see ByWindowSelector#type(int) ByWindowSelector.type(int)
         */
        public static @NonNull ByWindowSelector type(@ByWindowSelector.WindowType int type) {
            return new ByWindowSelector().type(type);
        }

        /**
         * Constructs a new {@link ByWindowSelector} and sets the title criteria.
         *
         * @see ByWindowSelector#title(String) ByWindowSelector.title(String)
         */
        public static @NonNull ByWindowSelector title(@NonNull String title) {
            return new ByWindowSelector().title(title);
        }

        /**
         * Constructs a new {@link ByWindowSelector} and sets the title criteria.
         *
         * @see ByWindowSelector#titleContains(String) ByWindowSelector.titleContains(String)
         */
        public static @NonNull ByWindowSelector titleContains(@NonNull String substring) {
            return new ByWindowSelector().titleContains(substring);
        }

        /**
         * Constructs a new {@link ByWindowSelector} and sets the title criteria.
         *
         * @see ByWindowSelector#titleStartsWith(String) ByWindowSelector.titleStartsWith(String)
         */
        public static @NonNull ByWindowSelector titleStartsWith(@NonNull String prefix) {
            return new ByWindowSelector().titleStartsWith(prefix);
        }

        /**
         * Constructs a new {@link ByWindowSelector} and sets the title criteria.
         *
         * @see ByWindowSelector#titleEndsWith(String) ByWindowSelector.titleEndsWith(String)
         */
        public static @NonNull ByWindowSelector titleEndsWith(@NonNull String suffix) {
            return new ByWindowSelector().titleEndsWith(suffix);
        }

        /**
         * Constructs a new {@link ByWindowSelector} and sets the title criteria.
         *
         * @see ByWindowSelector#title(Pattern) ByWindowSelector.title(Pattern)
         */
        public static @NonNull ByWindowSelector title(@NonNull Pattern regex) {
            return new ByWindowSelector().title(regex);
        }

        /**
         * Constructs a new {@link ByWindowSelector} and sets the window id criteria.
         *
         * <p>The window id can be obtained from {@link UiWindow#getId()}.
         *
         * @see ByWindowSelector#id(int) ByWindowSelector.id(int)
         */
        public static @NonNull ByWindowSelector id(int id) {
            return new ByWindowSelector().id(id);
        }

        /**
         * Constructs a new {@link ByWindowSelector} and sets the layer (Z-order) criteria.
         *
         * <p>The layer ID can be obtained from {@link UiWindow#getLayer()}.
         *
         * @see ByWindowSelector#layer(int) ByWindowSelector.layer(int)
         */
        public static @NonNull ByWindowSelector layer(@IntRange(from = 0) int layer) {
            return new ByWindowSelector().layer(layer);
        }

        /**
         * Constructs a new {@link ByWindowSelector} and sets the layer (Z-order) criteria.
         *
         * <p>The referenced layer ID can be obtained from {@link UiWindow#getLayer()}.
         *
         * @see ByWindowSelector#layerAbove(int) ByWindowSelector.layerAbove(int)
         */
        public static @NonNull ByWindowSelector layerAbove(@IntRange(from = 0) int referenceLayer) {
            return new ByWindowSelector().layerAbove(referenceLayer);
        }

        /**
         * Constructs a new {@link ByWindowSelector} and sets the layer (Z-order) criteria.
         *
         * <p>The referenced layer ID can be obtained from {@link UiWindow#getLayer()}.
         *
         * @see ByWindowSelector#layerBelow(int) ByWindowSelector.layerBelow(int)
         */
        public static @NonNull ByWindowSelector layerBelow(@IntRange(from = 1) int referenceLayer) {
            return new ByWindowSelector().layerBelow(referenceLayer);
        }

        /**
         * Constructs a new {@link ByWindowSelector} and sets the display ID criteria.
         *
         * @see ByWindowSelector#displayId(int) ByWindowSelector.displayId(int)
         */
        @RequiresApi(30)
        public static @NonNull ByWindowSelector displayId(int displayId) {
            return new ByWindowSelector().displayId(displayId);
        }

        /**
         * Constructs a new {@link ByWindowSelector} and sets the active criteria.
         *
         * @see ByWindowSelector#active(boolean) ByWindowSelector.active(boolean)
         */
        public static @NonNull ByWindowSelector active(boolean isActive) {
            return new ByWindowSelector().active(isActive);
        }

        /**
         * Constructs a new {@link ByWindowSelector} and sets the focused criteria.
         *
         * @see ByWindowSelector#focused(boolean) ByWindowSelector.focused(boolean)
         */
        public static @NonNull ByWindowSelector focused(boolean isFocused) {
            return new ByWindowSelector().focused(isFocused);
        }

        /**
         * Constructs a new {@link ByWindowSelector} and sets the package criteria.
         *
         * @see ByWindowSelector#pkg(String) ByWindowSelector.pkg(String)
         */
        public static @NonNull ByWindowSelector pkg(@NonNull String packageName) {
            return new ByWindowSelector().pkg(packageName);
        }

        /**
         * Constructs a new {@link ByWindowSelector} and sets the package criteria.
         *
         * @see ByWindowSelector#pkg(Pattern) ByWindowSelector.pkg(Pattern)
         */
        public static @NonNull ByWindowSelector pkg(@NonNull Pattern regex) {
            return new ByWindowSelector().pkg(regex);
        }
    }
}