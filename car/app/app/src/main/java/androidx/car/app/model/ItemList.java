/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.car.app.model;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.os.Looper;

import androidx.annotation.IntRange;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.utils.CollectionUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a list of {@link Item} instances. {@link ItemList} instances are used by templates
 * that contain lists of models, such as for example, the list of {@link Row}s in a {@link
 * ListTemplate}.
 */
@CarProtocol
@KeepFields
public final class ItemList {
    /**
     * A listener for handling selection events for lists with selectable items.
     *
     * @see Builder#setOnSelectedListener(OnSelectedListener)
     */
    public interface OnSelectedListener {
        /**
         * Notifies that an item was selected.
         *
         * <p>This event is called even if the selection did not change, for example, if the user
         * selected an already selected item.
         *
         * @param selectedIndex the index of the newly selected item
         */
        void onSelected(int selectedIndex);
    }

    /** A listener for handling item visibility changes. */
    public interface OnItemVisibilityChangedListener {
        /**
         * Notifies that the items in the list within the specified indices have become visible.
         *
         * <p>The start index is inclusive, and the end index is exclusive. For example, if only the
         * first item in a list is visible, the start and end indices would be 0 and 1,
         * respectively. If no items are visible, the indices will be set to -1.
         *
         * @param startIndex the index of the first item that is visible
         * @param endIndex   the index of the first item that is not visible after the visible
         *                   range
         */
        void onItemVisibilityChanged(int startIndex, int endIndex);
    }

    private final int mSelectedIndex;
    private final List<Item> mItems;
    private final @Nullable OnSelectedDelegate mOnSelectedDelegate;
    private final @Nullable OnItemVisibilityChangedDelegate mOnItemVisibilityChangedDelegate;
    private final @Nullable CarText mNoItemsMessage;

    /**
     * Returns the index of the selected item of the list.
     *
     * @see Builder#setSelectedIndex(int)
     */
    public int getSelectedIndex() {
        return mSelectedIndex;
    }

    /**
     * Returns the {@link OnSelectedDelegate} to be called when when an item is selected
     * by the user or {@code null} is the list is non-selectable.
     *
     * @see Builder#setOnSelectedListener(OnSelectedListener)
     */
    public @Nullable OnSelectedDelegate getOnSelectedDelegate() {
        return mOnSelectedDelegate;
    }

    /**
     * Returns the app-supplied text to be displayed if the list is empty or {@code null} if the
     * default text will be used by the host.
     *
     * @see Builder#setNoItemsMessage(CharSequence)
     */
    public @Nullable CarText getNoItemsMessage() {
        return mNoItemsMessage;
    }

    /**
     * Returns the {@link OnItemVisibilityChangedDelegate} to be called when the visible
     * items in the list changes or {@code null} if one hasn't been set.
     *
     * @see Builder#setOnItemsVisibilityChangedListener(OnItemVisibilityChangedListener)
     */
    public @Nullable OnItemVisibilityChangedDelegate getOnItemVisibilityChangedDelegate() {
        return mOnItemVisibilityChangedDelegate;
    }

    /**
     * Returns the list of items in this {@link ItemList}.
     *
     * @see Builder#addItem(Item)
     */
    public @NonNull List<Item> getItems() {
        return CollectionUtils.emptyIfNull(mItems);
    }

    @Override
    public @NonNull String toString() {
        return "[ items: "
                + (mItems != null ? mItems.toString() : null)
                + ", selected: "
                + mSelectedIndex
                + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mSelectedIndex,
                mItems,
                mOnSelectedDelegate == null,
                mOnItemVisibilityChangedDelegate == null,
                mNoItemsMessage);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ItemList)) {
            return false;
        }
        ItemList otherList = (ItemList) other;

        // For listeners only check if they are either both null, or both set.
        return mSelectedIndex == otherList.mSelectedIndex
                && Objects.equals(mItems, otherList.mItems)
                && Objects.equals(mOnSelectedDelegate == null,
                otherList.mOnSelectedDelegate == null)
                && Objects.equals(
                mOnItemVisibilityChangedDelegate == null,
                otherList.mOnItemVisibilityChangedDelegate == null)
                && Objects.equals(mNoItemsMessage, otherList.mNoItemsMessage);
    }

    ItemList(Builder builder) {
        mSelectedIndex = builder.mSelectedIndex;
        mItems = CollectionUtils.unmodifiableCopy(builder.mItems);
        mNoItemsMessage = builder.mNoItemsMessage;
        mOnSelectedDelegate = builder.mOnSelectedDelegate;
        mOnItemVisibilityChangedDelegate = builder.mOnItemVisibilityChangedDelegate;
    }

    /** Constructs an empty instance, used by serialization code. */
    private ItemList() {
        mSelectedIndex = 0;
        mItems = Collections.emptyList();
        mNoItemsMessage = null;
        mOnSelectedDelegate = null;
        mOnItemVisibilityChangedDelegate = null;
    }

    /**
     * Creates and returns a new {@link Builder} initialized with this {@link ItemList}'s data.
     */
    @ExperimentalCarApi
    public @NonNull Builder toBuilder() {
        return new Builder(this);
    }

    static @Nullable OnClickDelegate getOnClickDelegate(Item item) {
        if (item instanceof Row) {
            return ((Row) item).getOnClickDelegate();
        } else if (item instanceof GridItem) {
            return ((GridItem) item).getOnClickDelegate();
        }

        return null;
    }

    static @Nullable Toggle getToggle(Item item) {
        if (item instanceof Row) {
            return ((Row) item).getToggle();
        }

        return null;
    }

    /** A builder of {@link ItemList}. */
    public static final class Builder {
        final List<Item> mItems;
        int mSelectedIndex;
        @Nullable OnSelectedDelegate mOnSelectedDelegate;
        @Nullable OnItemVisibilityChangedDelegate mOnItemVisibilityChangedDelegate;
        @Nullable CarText mNoItemsMessage;

        /**
         * Sets the {@link OnItemVisibilityChangedListener} to call when the visible items in the
         * list changes.
         *
         * <p>Note that the listener relates to UI events and will be executed on the main thread
         * using {@link Looper#getMainLooper()}.
         *
         * @throws NullPointerException if {@code itemVisibilityChangedListener} is {@code null}
         */
        @SuppressLint({"MissingGetterMatchingBuilder", "ExecutorRegistration"})
        public @NonNull Builder setOnItemsVisibilityChangedListener(
                @NonNull OnItemVisibilityChangedListener itemVisibilityChangedListener) {
            mOnItemVisibilityChangedDelegate = OnItemVisibilityChangedDelegateImpl.create(
                    itemVisibilityChangedListener);
            return this;
        }

        /** @see #setOnItemsVisibilityChangedListener(OnItemVisibilityChangedListener) */
        @RestrictTo(LIBRARY)
        public @NonNull Builder setOnItemsVisibilityChangedDelegate(
                @Nullable OnItemVisibilityChangedDelegate onItemVisibilityChangedDelegate) {
            mOnItemVisibilityChangedDelegate = onItemVisibilityChangedDelegate;
            return this;
        }

        /**
         * Marks the list as selectable by setting the {@link OnSelectedListener} to call when an
         * item is selected by the user, or set to {@code null} to mark the list as non-selectable.
         *
         * <p>Selectable lists, where allowed by the template they are added to, automatically
         * display an item in a selected state when selected by the user.
         *
         * <p>The items in the list define a mutually exclusive selection scope: only a single
         * item will be selected at any given time.
         *
         * <p>The specific way in which the selection will be visualized depends on the template
         * and the host implementation. For example, some templates may display the list as a
         * radio button group, while others may highlight the selected item's background.
         *
         * @throws NullPointerException if {@code onSelectedListener} is {@code null}
         * @see #setSelectedIndex(int)
         */
        @SuppressLint({"MissingGetterMatchingBuilder", "ExecutorRegistration"})
        public @NonNull Builder setOnSelectedListener(
                @NonNull OnSelectedListener onSelectedListener) {
            mOnSelectedDelegate = OnSelectedDelegateImpl.create(onSelectedListener);
            return this;
        }

        /** @see #setOnSelectedListener(OnSelectedListener)  */
        @RestrictTo(LIBRARY)
        public @NonNull Builder setOnSelectedDelegate(
                @Nullable OnSelectedDelegate onSelectedDelegate) {
            mOnSelectedDelegate = onSelectedDelegate;
            return this;
        }


        /**
         * Sets the index of the item to show as selected.
         *
         * <p>By default and unless explicitly set with this method, the first item is selected.
         *
         * <p>If the list is not a selectable list set with {@link #setOnSelectedListener}, this
         * value is ignored.
         */
        public @NonNull Builder setSelectedIndex(@IntRange(from = 0) int selectedIndex) {
            if (selectedIndex < 0) {
                throw new IllegalArgumentException(
                        "The item index must be larger than or equal to 0");
            }
            mSelectedIndex = selectedIndex;
            return this;
        }

        /**
         * Sets the text to display if the list is empty.
         *
         * <p>If the list is empty and the app does not explicitly set the message with this
         * method, the host will show a default message.
         *
         * <p>Spans are not supported in the input string and will be ignored.
         *
         * @throws NullPointerException if {@code noItemsMessage} is {@code null}
         */
        public @NonNull Builder setNoItemsMessage(@NonNull CharSequence noItemsMessage) {
            mNoItemsMessage = CarText.create(requireNonNull(noItemsMessage));
            return this;
        }

        /**
         * Adds an item to the list.
         *
         * @throws NullPointerException if {@code item} is {@code null}
         */
        public @NonNull Builder addItem(@NonNull Item item) {
            mItems.add(requireNonNull(item));
            return this;
        }

        /** Removes all {@link Item}s added via {@link #addItem(Item)} */
        @ExperimentalCarApi
        public @NonNull Builder clearItems() {
            mItems.clear();
            return this;
        }

        /**
         * Constructs the item list defined by this builder.
         *
         * @throws IllegalStateException if the list is selectable but does not have any items, if
         *                               the selected index is greater or equal to the size of the
         *                               list, or if the list is selectable and any items have
         *                               either one of their {@link OnClickListener} or
         *                               {@link Toggle} set
         */
        public @NonNull ItemList build() {
            if (mOnSelectedDelegate != null) {
                int listSize = mItems.size();
                if (listSize == 0) {
                    throw new IllegalStateException("A selectable list cannot be empty");
                } else if (mSelectedIndex >= listSize) {
                    throw new IllegalStateException(
                            "The selected item index ("
                                    + mSelectedIndex
                                    + ") is larger than the size of the list ("
                                    + listSize
                                    + ")");
                }

                // Check that no items have disallowed elements if the list is selectable.
                for (Item item : mItems) {
                    if (getOnClickDelegate(item) != null) {
                        throw new IllegalStateException(
                                "Items that belong to selectable lists can't have an "
                                        + "onClickListener. Use the OnSelectedListener of the list "
                                        + "instead");
                    }

                    if (getToggle(item) != null) {
                        throw new IllegalStateException(
                                "Items that belong to selectable lists can't have a toggle");
                    }
                }
            }

            return new ItemList(this);
        }

        /** Returns an empty {@link Builder} instance. */
        public Builder() {
            mItems = new ArrayList<>();
        }

        /** Creates a new {@link Builder}, populated from the input {@link ItemList} */
        Builder(@NonNull ItemList itemList) {
            mSelectedIndex = itemList.getSelectedIndex();
            mOnSelectedDelegate = itemList.getOnSelectedDelegate();
            mOnItemVisibilityChangedDelegate = itemList.getOnItemVisibilityChangedDelegate();
            mNoItemsMessage = itemList.getNoItemsMessage();

            // Must be mutable
            mItems = new ArrayList<>(itemList.getItems());
        }
    }
}
