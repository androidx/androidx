/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.model.constraints.ActionsConstraints;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** A template that contains sections of items like rows, grid items, etc. */
@KeepFields
@CarProtocol
@ExperimentalCarApi
public final class SectionedItemTemplate implements Template {
    private final @NonNull List<Section<?>> mSections;

    private final @NonNull List<Action> mActions;

    private final @Nullable Header mHeader;

    private final boolean mIsLoading;

    private final boolean mIsAlphabeticalIndexingAllowed;

    // Empty constructor for serialization
    private SectionedItemTemplate() {
        mSections = Collections.emptyList();
        mActions = Collections.emptyList();
        mHeader = null;
        mIsLoading = false;
        mIsAlphabeticalIndexingAllowed = false;
    }

    /** Creates a {@link SectionedItemTemplate} from the {@link Builder}. */
    private SectionedItemTemplate(Builder builder) {
        mSections = Collections.unmodifiableList(builder.mSections);
        mActions = Collections.unmodifiableList(builder.mActions);
        mHeader = builder.mHeader;
        mIsLoading = builder.mIsLoading;
        mIsAlphabeticalIndexingAllowed = builder.mIsAlphabeticalIndexingAllowed;
    }

    /** Returns the list of sections within this template. */
    public @NonNull List<Section<?>> getSections() {
        return mSections;
    }

    /** Returns the list of actions that should appear alongside the content of this template. */
    public @NonNull List<Action> getActions() {
        return mActions;
    }

    /** Returns the optional header for this template. */
    public @Nullable Header getHeader() {
        return mHeader;
    }

    /** Returns whether or not this template is in a loading state. */
    public boolean isLoading() {
        return mIsLoading;
    }

    /**
     * Returns whether this list can be indexed alphabetically, by item title.
     *
     * <p>"Indexing" refers to the process of examining list contents (e.g. item titles) to sort,
     * partition, or filter a list. Indexing is generally used for features called "Accelerators",
     * which allow a user to quickly find a particular {@link Item} in a long list.
     *
     * <p>To exclude a single item from indexing, see the relevant item's API.
     *
     * <p>To enable/disable accelerators for the entire list, see
     * {@link SectionedItemTemplate.Builder#setAlphabeticalIndexingAllowed(boolean)}
     */
    public boolean isAlphabeticalIndexingAllowed() {
        return mIsAlphabeticalIndexingAllowed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mSections,
                mActions,
                mHeader,
                mIsLoading,
                mIsAlphabeticalIndexingAllowed
        );
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (!(other instanceof SectionedItemTemplate)) {
            return false;
        }
        SectionedItemTemplate template = (SectionedItemTemplate) other;
        return Objects.equals(mSections, template.mSections)
                && Objects.equals(mActions, template.mActions)
                && Objects.equals(mHeader, template.mHeader)
                && mIsLoading == template.mIsLoading
                && mIsAlphabeticalIndexingAllowed == template.mIsAlphabeticalIndexingAllowed;
    }

    @Override
    public @NonNull String toString() {
        return "SectionedItemTemplate";
    }

    /**
     * A builder that constructs {@link SectionedItemTemplate} instances.
     *
     * <p>Upon building, this class validates the following:
     *
     * <ul>
     *     <li>The template is not both loading and populated with sections
     *     <li>Only {@link RowSection} and/or {@link GridSection} are added as sections
     * </ul>
     */
    @ExperimentalCarApi
    public static final class Builder {
        private @NonNull List<Section<?>> mSections = new ArrayList<>();

        private @NonNull List<Action> mActions = new ArrayList<>();

        private @Nullable Header mHeader = null;

        private boolean mIsLoading = false;
        private boolean mIsAlphabeticalIndexingAllowed = false;

        /** Create a new {@link SectionedItemTemplate} builder. */
        public Builder() {
        }

        /**
         * Create a new {@link SectionedItemTemplate} builder, copying the values from an existing
         * instance.
         */
        public Builder(@NonNull SectionedItemTemplate template) {
            mSections = template.mSections;
            mActions = template.mActions;
            mHeader = template.mHeader;
            mIsLoading = template.mIsLoading;
            mIsAlphabeticalIndexingAllowed = template.mIsAlphabeticalIndexingAllowed;
        }

        /**
         * Sets the sections in this template, overwriting any other previously set sections. Only
         * sections listed in {@link Builder} can be added.
         *
         * @see Builder for a list of allowed section types
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setSections(@NonNull List<Section<?>> sections) {
            mSections = sections;
            return this;
        }

        /**
         * Adds a single {@link Section} to this template, appending to the existing list of
         * sections. Only sections listed in {@link Builder} can be added.
         *
         * @see Builder for a list of allowed section types
         */
        @CanIgnoreReturnValue
        public @NonNull Builder addSection(@NonNull Section<?> section) {
            mSections.add(section);
            return this;
        }

        /** Removes all sections from this template. */
        @CanIgnoreReturnValue
        public @NonNull Builder clearSections() {
            mSections.clear();
            return this;
        }

        /**
         * Sets the actions that show up alongside the sections of this template (as opposed to the
         * actions in the header), overwriting any other previously set actions from {@link
         * #addAction(Action)} or {@link #setActions(List)}. All actions must conform to the
         * {@link ActionsConstraints#ACTIONS_CONSTRAINTS_FAB} constraints.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setActions(@NonNull List<Action> actions) {
            ActionsConstraints.ACTIONS_CONSTRAINTS_FAB.validateOrThrow(actions);
            mActions = actions;
            return this;
        }

        /**
         * Adds a single {@link Action} to this template, appending to the existing list of
         * actions. All actions must conform to the
         * {@link ActionsConstraints#ACTIONS_CONSTRAINTS_FAB} constraints.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder addAction(@NonNull Action action) {
            List<Action> actionsCopy = new ArrayList<>(mActions);
            actionsCopy.add(action);
            ActionsConstraints.ACTIONS_CONSTRAINTS_FAB.validateOrThrow(actionsCopy);

            mActions.add(action);
            return this;
        }

        /** Removes all actions in this template. */
        @CanIgnoreReturnValue
        public @NonNull Builder clearActions() {
            mActions.clear();
            return this;
        }

        /** Sets or clears the optional header for this template. */
        @CanIgnoreReturnValue
        public @NonNull Builder setHeader(@Nullable Header header) {
            mHeader = header;
            return this;
        }

        /**
         * Sets whether or not this template is in a loading state. If passed {@code true}, sections
         * cannot be added to the template. By default, this is {@code false}.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setLoading(boolean isLoading) {
            mIsLoading = isLoading;
            return this;
        }

        /**
         * Sets whether this list can be indexed alphabetically, by item title. By default, this
         * is {@code false}.
         *
         * <p>"Indexing" refers to the process of examining list contents (e.g. item titles) to
         * sort, partition, or filter a list. Indexing is generally used for features called
         * "Accelerators", which allow a user to quickly find a particular {@link Item} in a long
         * list.
         *
         * <p>For example, a media app may, by default, show a user's playlists sorted by date
         * created. If the app provides these playlists via the {@code SectionedItemTemplate} and
         * enables {@link #isAlphabeticalIndexingAllowed}, the user will be able to jump to their
         * playlists that start with the letter "H". When this happens, the list is reconstructed
         * and sorted alphabetically, then shown to the user, jumping down to the letter "H".
         *
         * <p>Individual items may be excluded from the list by setting their {@code #isIndexable}
         * field to {@code false}.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setAlphabeticalIndexingAllowed(
                boolean alphabeticalIndexingAllowed) {
            mIsAlphabeticalIndexingAllowed = alphabeticalIndexingAllowed;
            return this;
        }

        /**
         * Constructs a new {@link SectionedItemTemplate} from the current state of this builder,
         * throwing exceptions for any invalid state.
         *
         * @see Builder for the list of validation logic
         */
        public @NonNull SectionedItemTemplate build() {
            if (mIsLoading) {
                if (!mSections.isEmpty()) {
                    throw new IllegalArgumentException(
                            "A template cannot both be in a loading state and have sections added");
                }
            }

            for (Section<?> section : mSections) {
                if (!(section instanceof RowSection) && !(section instanceof GridSection)) {
                    throw new IllegalArgumentException(
                            "Only RowSections and GridSections are allowed in "
                                    + "SectionedItemTemplate.");
                }
            }

            return new SectionedItemTemplate(this);
        }
    }
}
