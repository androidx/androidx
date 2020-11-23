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
import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_HEADER;
import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_SIMPLE;
import static androidx.car.app.model.constraints.RowListConstraints.ROW_LIST_CONSTRAINTS_FULL_LIST;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.car.app.Screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A template representing a list of items.
 *
 * <h4>Template Restrictions</h4>
 *
 * In regards to template refreshes, as described in {@link Screen#onGetTemplate()}, this
 * template is considered a refresh of a previous one if:
 *
 * <ul>
 *   <li>The template title has not changed, and
 *   <li>The previous template is in a loading state (see {@link Builder#setLoading}}, or the
 *       {@link ItemList} structure between the templates have not changed. This means that if the
 *       previous template has multiple {@link ItemList} sections, the new template must have the
 *       same number of sections with the same headers. Further, the number of rows and the string
 *       contents (title, texts, not counting spans) of each row must not have changed.
 *   <li>For rows that contain a {@link Toggle}, updates to the title or texts are also allowed if
 *       the toggle state has changed between the previous and new templates.
 * </ul>
 */
public final class ListTemplate implements Template {
    @Keep
    private final boolean mIsLoading;
    @Keep
    @Nullable
    private final CarText mTitle;
    @Keep
    @Nullable
    private final Action mHeaderAction;
    @Keep
    @Nullable
    private final ItemList mSingleList;
    @Keep
    private final List<SectionedItemList> mSectionLists;
    @Keep
    @Nullable
    private final ActionStrip mActionStrip;

    /** Constructs a new builder of {@link ListTemplate}. */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    public boolean isLoading() {
        return mIsLoading;
    }

    @Nullable
    public CarText getTitle() {
        return mTitle;
    }

    @Nullable
    public Action getHeaderAction() {
        return mHeaderAction;
    }

    @Nullable
    public ItemList getSingleList() {
        return mSingleList;
    }

    @NonNull
    public List<SectionedItemList> getSectionLists() {
        return mSectionLists;
    }

    @Nullable
    public ActionStrip getActionStrip() {
        return mActionStrip;
    }

    @NonNull
    @Override
    public String toString() {
        return "ListTemplate";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mIsLoading, mTitle, mHeaderAction, mSingleList, mSectionLists,
                mActionStrip);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ListTemplate)) {
            return false;
        }
        ListTemplate otherTemplate = (ListTemplate) other;

        return mIsLoading == otherTemplate.mIsLoading
                && Objects.equals(mTitle, otherTemplate.mTitle)
                && Objects.equals(mHeaderAction, otherTemplate.mHeaderAction)
                && Objects.equals(mSingleList, otherTemplate.mSingleList)
                && Objects.equals(mSectionLists, otherTemplate.mSectionLists)
                && Objects.equals(mActionStrip, otherTemplate.mActionStrip);
    }

    private ListTemplate(Builder builder) {
        mIsLoading = builder.mIsLoading;
        mTitle = builder.mTitle;
        mHeaderAction = builder.mHeaderAction;
        mSingleList = builder.mSingleList;
        mSectionLists = new ArrayList<>(builder.mSectionLists);
        mActionStrip = builder.mActionStrip;
    }

    /** Constructs an empty instance, used by serialization code. */
    private ListTemplate() {
        mIsLoading = false;
        mTitle = null;
        mHeaderAction = null;
        mSingleList = null;
        mSectionLists = Collections.emptyList();
        mActionStrip = null;
    }

    /** A builder of {@link ListTemplate}. */
    public static final class Builder {
        private boolean mIsLoading;
        @Nullable
        private ItemList mSingleList;
        private final List<SectionedItemList> mSectionLists = new ArrayList<>();
        @Nullable
        private CarText mTitle;
        @Nullable
        private Action mHeaderAction;
        @Nullable
        private ActionStrip mActionStrip;
        private boolean mHasSelectableList;

        /**
         * Sets whether the template is in a loading state.
         *
         * <p>If set to {@code true}, the UI will display a loading indicator where the list content
         * would be otherwise. The caller is expected to call {@link
         * androidx.car.app.Screen#invalidate()} and send the new template content
         * to the host once the data is ready.
         *
         * <p>If set to {@code false}, the UI will display the contents of the {@link ItemList}
         * instance(s) added via {@link #setSingleList} or {@link #addList}.
         */
        @NonNull
        public Builder setLoading(boolean isLoading) {
            this.mIsLoading = isLoading;
            return this;
        }

        /**
         * Sets the {@link Action} that will be displayed in the header of the template, or
         * {@code null}
         * to not display an action.
         *
         * <h4>Requirements</h4>
         *
         * This template only supports either one of {@link Action#APP_ICON} and
         * {@link Action#BACK} as
         * a header {@link Action}.
         *
         * @throws IllegalArgumentException if {@code headerAction} does not meet the template's
         *                                  requirements.
         */
        @NonNull
        public Builder setHeaderAction(@Nullable Action headerAction) {
            ACTIONS_CONSTRAINTS_HEADER.validateOrThrow(
                    headerAction == null ? Collections.emptyList()
                            : Collections.singletonList(headerAction));
            this.mHeaderAction = headerAction;
            return this;
        }

        /**
         * Sets the {@link CharSequence} to show as the template's title, or {@code null} to not
         * show a
         * title.
         */
        @NonNull
        public Builder setTitle(@Nullable CharSequence title) {
            this.mTitle = title == null ? null : CarText.create(title);
            return this;
        }

        /** Resets any list(s) that were added via {@link #setSingleList} or {@link #addList}. */
        @NonNull
        public Builder clearAllLists() {
            mSingleList = null;
            mSectionLists.clear();
            mHasSelectableList = false;
            return this;
        }

        /**
         * Sets a single {@link ItemList} to show in the template.
         *
         * <p>Note that this list cannot be mixed with others added via {@link #addList}. If
         * multiple
         * lists were previously added, they will be cleared.
         *
         * @throws NullPointerException if {@code list} is null.
         * @see #addList(ItemList, CharSequence)
         */
        @NonNull
        public Builder setSingleList(@NonNull ItemList list) {
            mSingleList = requireNonNull(list);
            mSectionLists.clear();
            mHasSelectableList = false;
            return this;
        }

        /**
         * Adds an {@link ItemList} to display in the template.
         *
         * <p>Use this method to add multiple {@link ItemList}s to the template. Each
         * {@link ItemList}
         * will be grouped under the given {@code header}. These lists cannot be mixed with an
         * {@link
         * ItemList} added via {@link #setSingleList}. If a single list was previously added, it
         * will be
         * cleared.
         *
         * <p>If the added {@link ItemList} contains a {@link ItemList.OnSelectedListener}, then it
         * cannot be added alongside other {@link ItemList}(s).
         *
         * @throws NullPointerException     if {@code list} is null.
         * @throws IllegalArgumentException if {@code list} is empty.
         * @throws IllegalArgumentException if {@code list}'s {@link
         *                                  ItemList.OnItemVisibilityChangedListener} is set.
         * @throws NullPointerException     if {@code header} is null.
         * @throws IllegalArgumentException if {@code header} is empty.
         * @throws IllegalArgumentException if a selectable list is added alongside other lists.
         */
        @NonNull
        // TODO(shiufai): consider rename to match getter's name.
        @SuppressLint("MissingGetterMatchingBuilder")
        public Builder addList(@NonNull ItemList list, @NonNull CharSequence header) {
            if (requireNonNull(header).length() == 0) {
                throw new IllegalArgumentException("Header cannot be empty");
            }
            CarText headerText = CarText.create(header);

            boolean isSelectableList = list.getOnSelectedListener() != null;
            if (mHasSelectableList || (isSelectableList && !mSectionLists.isEmpty())) {
                throw new IllegalArgumentException(
                        "A selectable list cannot be added alongside any other lists");
            }
            mHasSelectableList = isSelectableList;

            if (list.getItems().isEmpty()) {
                throw new IllegalArgumentException("List cannot be empty");
            }

            if (list.getOnItemsVisibilityChangedListener() != null) {
                throw new IllegalArgumentException(
                        "OnItemVisibilityChangedListener in the list is disallowed");
            }

            mSingleList = null;
            mSectionLists.add(SectionedItemList.create(list, headerText));
            return this;
        }

        /** @hide */
        @RestrictTo(LIBRARY)
        @NonNull
        public Builder addListForTesting(@NonNull ItemList list, @NonNull CharSequence header) {
            mSingleList = null;
            mSectionLists.add(SectionedItemList.create(list, CarText.create(header)));
            return this;
        }

        /**
         * Sets the {@link ActionStrip} for this template, or {@code null} to not display an {@link
         * ActionStrip}.
         *
         * <h4>Requirements</h4>
         *
         * This template allows up to 2 {@link Action}s in its {@link ActionStrip}. Of the 2 allowed
         * {@link Action}s, one of them can contain a title as set via
         * {@link Action.Builder#setTitle}. Otherwise, only {@link Action}s with icons are allowed.
         *
         * @throws IllegalArgumentException if {@code actionStrip} does not meet the requirements.
         */
        @NonNull
        public Builder setActionStrip(@Nullable ActionStrip actionStrip) {
            ACTIONS_CONSTRAINTS_SIMPLE.validateOrThrow(
                    actionStrip == null ? Collections.emptyList() : actionStrip.getActions());
            this.mActionStrip = actionStrip;
            return this;
        }

        /**
         * Constructs the template defined by this builder.
         *
         * <h4>Requirements</h4>
         *
         * This template allows up to 6 {@link Row}s total in the {@link ItemList}(s). The host will
         * ignore any items over that limit. Each {@link Row}s can add up to 2 lines of texts via
         * {@link Row.Builder#addText}.
         *
         * <p>Either a header {@link Action} or the title must be set on the template.
         *
         * @throws IllegalStateException    if the template is in a loading state but there are
         *                                  lists added, or vice versa.
         * @throws IllegalArgumentException if the added {@link ItemList}(s) do not meet the
         *                                  template's requirements.
         * @throws IllegalStateException    if the template does not have either a title or header
         *                                  {@link Action} set.
         */
        @NonNull
        public ListTemplate build() {
            boolean hasList = mSingleList != null || !mSectionLists.isEmpty();
            if (mIsLoading == hasList) {
                throw new IllegalStateException(
                        "Template is in a loading state but lists are added, or vice versa");
            }

            if (hasList) {
                if (!mSectionLists.isEmpty()) {
                    ROW_LIST_CONSTRAINTS_FULL_LIST.validateOrThrow(mSectionLists);
                } else if (mSingleList != null) {
                    ROW_LIST_CONSTRAINTS_FULL_LIST.validateOrThrow(mSingleList);
                }
            }

            if (CarText.isNullOrEmpty(mTitle) && mHeaderAction == null) {
                throw new IllegalStateException("Either the title or header action must be set");
            }

            return new ListTemplate(this);
        }

        /** @hide */
        @RestrictTo(LIBRARY)
        @NonNull
        public ListTemplate buildForTesting() {
            return new ListTemplate(this);
        }

        private Builder() {
        }
    }
}
