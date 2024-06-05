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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.model.constraints.ActionsConstraints;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** A template that contains sections of items like rows, grid items, etc. */
@KeepFields
@CarProtocol
@ExperimentalCarApi
public final class SectionedItemTemplate implements Template {
    @NonNull
    private final List<Section<?>> mSections;

    @NonNull
    private final List<Action> mActions;

    @Nullable
    private final Header mHeader;

    private final boolean mIsLoading;

    // Empty constructor for serialization
    private SectionedItemTemplate() {
        mSections = Collections.emptyList();
        mActions = Collections.emptyList();
        mHeader = null;
        mIsLoading = false;
    }

    /** Creates a {@link SectionedItemTemplate} from the {@link Builder}. */
    private SectionedItemTemplate(Builder builder) {
        mSections = Collections.unmodifiableList(builder.mSections);
        mActions = Collections.unmodifiableList(builder.mActions);
        mHeader = builder.mHeader;
        mIsLoading = builder.mIsLoading;
    }

    /** Returns the list of sections within this template. */
    @NonNull
    public List<Section<?>> getSections() {
        return mSections;
    }

    /** Returns the list of actions that should appear alongside the content of this template. */
    @NonNull
    public List<Action> getActions() {
        return mActions;
    }

    /** Returns the optional header for this template. */
    @Nullable
    public Header getHeader() {
        return mHeader;
    }

    /** Returns whether or not this template is in a loading state. */
    public boolean isLoading() {
        return mIsLoading;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mSections, mActions, mHeader, mIsLoading);
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
                && mIsLoading == template.mIsLoading;
    }

    @NonNull
    @Override
    public String toString() {
        return "SectionedItemTemplate { sections: " + mSections + ", actions: " + mActions
                + ", header: " + mHeader + ", isLoading: " + mIsLoading + " }";
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
        @NonNull
        private List<Section<?>> mSections = new ArrayList<>();

        @NonNull
        private List<Action> mActions = new ArrayList<>();

        @Nullable
        private Header mHeader = null;

        private boolean mIsLoading = false;

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
        }

        /**
         * Sets the sections in this template, overwriting any other previously set sections. Only
         * sections listed in {@link Builder} can be added.
         *
         * @see Builder for a list of allowed section types
         */
        @NonNull
        @CanIgnoreReturnValue
        public Builder setSections(@NonNull List<Section<?>> sections) {
            mSections = sections;
            return this;
        }

        /**
         * Adds a single {@link Section} to this template, appending to the existing list of
         * sections. Only sections listed in {@link Builder} can be added.
         *
         * @see Builder for a list of allowed section types
         */
        @NonNull
        @CanIgnoreReturnValue
        public Builder addSection(@NonNull Section<?> section) {
            mSections.add(section);
            return this;
        }

        /** Removes all sections from this template. */
        @NonNull
        @CanIgnoreReturnValue
        public Builder clearSections() {
            mSections.clear();
            return this;
        }

        /**
         * Sets the actions that show up alongside the sections of this template (as opposed to the
         * actions in the header), overwriting any other previously set actions from {@link
         * #addAction(Action)} or {@link #setActions(List)}. All actions must conform to the
         * {@link ActionsConstraints#ACTIONS_CONSTRAINTS_FAB} constraints.
         */
        @NonNull
        @CanIgnoreReturnValue
        public Builder setActions(@NonNull List<Action> actions) {
            ActionsConstraints.ACTIONS_CONSTRAINTS_FAB.validateOrThrow(actions);
            mActions = actions;
            return this;
        }

        /**
         * Adds a single {@link Action} to this template, appending to the existing list of
         * actions. All actions must conform to the
         * {@link ActionsConstraints#ACTIONS_CONSTRAINTS_FAB} constraints.
         */
        @NonNull
        @CanIgnoreReturnValue
        public Builder addAction(@NonNull Action action) {
            List<Action> actionsCopy = new ArrayList<>(mActions);
            actionsCopy.add(action);
            ActionsConstraints.ACTIONS_CONSTRAINTS_FAB.validateOrThrow(actionsCopy);

            mActions.add(action);
            return this;
        }

        /** Removes all actions in this template. */
        @NonNull
        @CanIgnoreReturnValue
        public Builder clearActions() {
            mActions.clear();
            return this;
        }

        /** Sets or clears the optional header for this template. */
        @NonNull
        @CanIgnoreReturnValue
        public Builder setHeader(@Nullable Header header) {
            mHeader = header;
            return this;
        }

        /**
         * Sets whether or not this template is in a loading state. If passed {@code true}, sections
         * cannot be added to the template.
         */
        @NonNull
        @CanIgnoreReturnValue
        public Builder setLoading(boolean isLoading) {
            mIsLoading = isLoading;
            return this;
        }

        /**
         * Constructs a new {@link SectionedItemTemplate} from the current state of this builder,
         * throwing exceptions for any invalid state.
         *
         * @see Builder for the list of validation logic
         */
        @NonNull
        public SectionedItemTemplate build() {
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
