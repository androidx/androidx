/*
 * Copyright 2026 The Android Open Source Project
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

import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_HEADER;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.utils.CollectionUtils;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a search bar header. This can be added to a {@link SectionedItemTemplate} to support
 * search functionality.
 */
@CarProtocol
@KeepFields
@ExperimentalCarApi
@RequiresCarApi(9)
public final class SearchHeader {
    private final @Nullable SearchCallbackDelegate mSearchCallbackDelegate;
    private final @Nullable String mInitialSearchText;
    private final @Nullable String mSearchHint;
    private final boolean mShowKeyboardByDefault;
    private final @Nullable Action mStartHeaderAction;
    private final @NonNull List<Action> mEndHeaderActions;

    SearchHeader(Builder builder) {
        mSearchCallbackDelegate = builder.mSearchCallbackDelegate;
        mInitialSearchText = builder.mInitialSearchText;
        mSearchHint = builder.mSearchHint;
        mShowKeyboardByDefault = builder.mShowKeyboardByDefault;
        mStartHeaderAction = builder.mStartHeaderAction;
        mEndHeaderActions = CollectionUtils.unmodifiableCopy(builder.mEndHeaderActions);
    }

    /** Constructs an empty instance, used by serialization code. */
    private SearchHeader() {
        mSearchCallbackDelegate = null;
        mInitialSearchText = null;
        mSearchHint = null;
        mShowKeyboardByDefault = true;
        mStartHeaderAction = null;
        mEndHeaderActions = Collections.emptyList();
    }

    /** Returns the {@link SearchCallbackDelegate} for search callbacks. */
    public @NonNull SearchCallbackDelegate getSearchCallbackDelegate() {
        return requireNonNull(mSearchCallbackDelegate);
    }

    /** Returns the initial search text or {@code null} if not set. */
    public @Nullable String getInitialSearchText() {
        return mInitialSearchText;
    }

    /** Returns the search hint or {@code null} if not set. */
    public @Nullable String getSearchHint() {
        return mSearchHint;
    }

    /** Returns whether the keyboard should be displayed by default. */
    public boolean isShowKeyboardByDefault() {
        return mShowKeyboardByDefault;
    }

    /**
     * Returns the action displayed at the start of the search header or {@code null} if not set.
     */
    public @Nullable Action getStartHeaderAction() {
        return mStartHeaderAction;
    }

    /** Returns the list of actions displayed at the end of the search header. */
    public @NonNull List<Action> getEndHeaderActions() {
        return mEndHeaderActions;
    }

    @Override
    public @NonNull String toString() {
        return "SearchHeader";
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mInitialSearchText,
                mSearchHint,
                mShowKeyboardByDefault,
                mStartHeaderAction,
                mEndHeaderActions);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SearchHeader)) {
            return false;
        }
        SearchHeader otherHeader = (SearchHeader) other;
        return mShowKeyboardByDefault == otherHeader.mShowKeyboardByDefault
                && Objects.equals(mInitialSearchText, otherHeader.mInitialSearchText)
                && Objects.equals(mSearchHint, otherHeader.mSearchHint)
                && Objects.equals(mStartHeaderAction, otherHeader.mStartHeaderAction)
                && Objects.equals(mEndHeaderActions, otherHeader.mEndHeaderActions);
    }

    /** A builder of {@link SearchHeader}. */
    public static final class Builder {
        private final @NonNull SearchCallbackDelegate mSearchCallbackDelegate;
        private @Nullable String mInitialSearchText;
        private @Nullable String mSearchHint;
        private boolean mShowKeyboardByDefault = true;
        private @Nullable Action mStartHeaderAction;
        private final @NonNull List<Action> mEndHeaderActions = new ArrayList<>();

        /**
         * Creates a new {@link Builder} with provided search callback {@link SearchCallback}.
         *
         * @param callback the callback to be invoked for search events
         * @throws NullPointerException if {@code callback} is {@code null}
         */
        @SuppressLint("ExecutorRegistration")
        public Builder(@NonNull SearchCallback callback) {
            requireNonNull(callback);
            mSearchCallbackDelegate = SearchCallbackDelegateImpl.create(callback);
        }

        /**
         * Sets the initial search text to display in the search box.
         *
         * @throws NullPointerException if {@code initialSearchText} is {@code null}
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setInitialSearchText(@NonNull String initialSearchText) {
            mInitialSearchText = requireNonNull(initialSearchText);
            return this;
        }

        /**
         * Sets the text hint to display in the search box when it is empty.
         *
         * @throws NullPointerException if {@code searchHint} is {@code null}
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setSearchHint(@NonNull String searchHint) {
            mSearchHint = requireNonNull(searchHint);
            return this;
        }

        /** Sets whether the keyboard should be displayed by default. Defaults to {@code true}. */
        @CanIgnoreReturnValue
        public @NonNull Builder setShowKeyboardByDefault(boolean showKeyboardByDefault) {
            mShowKeyboardByDefault = showKeyboardByDefault;
            return this;
        }

        /**
         * Sets the start header {@link Action}.
         *
         * @throws NullPointerException     if {@code headerAction} is {@code null}
         * @throws IllegalArgumentException if {@code headerAction} does not meet the
         * {@link androidx.car.app.model.constraints.ActionsConstraints#ACTIONS_CONSTRAINTS_HEADER requirements}
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setStartHeaderAction(@NonNull Action headerAction) {
            ACTIONS_CONSTRAINTS_HEADER.validateOrThrow(
                    Collections.singletonList(requireNonNull(headerAction)));
            mStartHeaderAction = headerAction;
            return this;
        }

        /**
         * Adds an {@link Action} to be displayed at the end of the header.
         *
         * @throws NullPointerException if {@code headerAction} is {@code null}
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setEndHeaderActions(@NonNull List<Action> endHeaderActions) {
            mEndHeaderActions.addAll(requireNonNull(endHeaderActions));
            return this;
        }

        /** Constructs the {@link SearchHeader} instance. */
        public @NonNull SearchHeader build() {
            return new SearchHeader(this);
        }
    }
}
