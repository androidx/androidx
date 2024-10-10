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
import androidx.car.app.model.constraints.RowConstraints;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A section within the {@code SectionedItemTemplate} that contains {@link Row}s - good for
 * showcasing small icons and longer text for every entry.
 */
@ExperimentalCarApi
@CarProtocol
@KeepFields
public final class RowSection extends Section<Row> {
    // Set to < 0 when this section is not a selection group; otherwise, a valid index of mItems
    private final int mInitialSelectedIndex;

    // Empty constructor for serialization
    private RowSection() {
        super();
        mInitialSelectedIndex = -1;
    }

    /** Creates a {@link RowSection} from the {@link Builder}. */
    private RowSection(Builder builder) {
        super(builder);
        mInitialSelectedIndex = builder.mInitialSelectedIndex;
    }

    /**
     * When set to a value that correlates to an index in {@link #getItemsDelegate()}, this
     * entire row
     * section should be treated as a selection group (eg. radio group). Otherwise this will be a
     * negative value to denote that this row section should not be transformed into a selection
     * group.
     */
    public int getInitialSelectedIndex() {
        return mInitialSelectedIndex;
    }

    /**
     * Flag denoting whether or not this section of rows should be treated as a selection group
     * (ie. radio button group).
     */
    public boolean isSelectionGroup() {
        return mInitialSelectedIndex >= 0;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (!(other instanceof RowSection)) {
            return false;
        }
        RowSection rowSection = (RowSection) other;
        return super.equals(rowSection)
                && mInitialSelectedIndex == rowSection.mInitialSelectedIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), mInitialSelectedIndex);
    }

    @Override
    public @NonNull String toString() {
        return "RowSection { initialSelectedIndex: " + mInitialSelectedIndex + ", "
                + super.toString() + " }";
    }

    /**
     * A builder that constructs {@link RowSection} instances.
     *
     * <p>Upon building, this class validates the following:
     *
     * <ul>
     *     <li>If this section is set as a selection group, the given {@code initialSelectedIndex}
     *     cannot be greater than the size of the list (unless the list is empty, in which case
     *     this value is ignored)
     *     <li>If this section is set as a selection group, none of the added {@link Row}s can have
     *     a {@link Row.Builder#setToggle(Toggle)} set
     *     <li>Each {@link Row} must conform to {@link RowConstraints#ROW_CONSTRAINTS_FULL_LIST}
     * </ul>
     */
    @ExperimentalCarApi
    public static final class Builder extends BaseBuilder<Row, Builder> {
        private int mInitialSelectedIndex = -1;

        /** Create a new {@link RowSection} builder. */
        public Builder() {
            super();
        }

        /**
         * Sets this entire {@link RowSection} as a selection group when passed a non-negative
         * integer correlating to a valid index within the list of items added. The UI behaves
         * equivalently to a radio button group where a single item is called out (either by an
         * actual radio button, or through some other highlighting). The host will initially
         * highlight the {@code initialSelectedIndex}'s item and automatically update the highlight
         * to other items if selected by the user. The app should handle user selections via
         * {@link Row.Builder#setOnClickListener(OnClickListener)}.
         *
         * <p>This cannot be used in conjunction with {@link Row.Builder#setToggle(Toggle)}.
         *
         * @param initialSelectedIndex the index of the item to be selected when the template
         *                             is first rendered
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setAsSelectionGroup(int initialSelectedIndex) {
            mInitialSelectedIndex = initialSelectedIndex;
            return this;
        }

        /**
         * Unsets this {@link RowSection} from being shown as a selection group. See {@link
         * #setAsSelectionGroup(int)}.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder clearSelectionGroup() {
            mInitialSelectedIndex = -1;
            return this;
        }

        /**
         * Constructs a new {@link RowSection} from the current state of this builder, throwing
         * exceptions for any invalid state.
         *
         * @see Builder for the list of validation logic
         */
        public @NonNull RowSection build() {
            if (mInitialSelectedIndex >= 0) {
                if (!mItems.isEmpty() && mInitialSelectedIndex >= mItems.size()) {
                    throw new IllegalArgumentException(
                            "The set initial selected index (" + mInitialSelectedIndex
                                    + ") cannot be larger than the size of the list ("
                                    + mItems.size() + ")");
                }

                for (Row row : mItems) {
                    if (row.getToggle() != null) {
                        throw new IllegalArgumentException(
                                "A row that has a toggle set cannot be added to a RowSection that"
                                        + " has an onSelectedListener."
                        );
                    }
                }
            }

            for (Row row : mItems) {
                RowConstraints.ROW_CONSTRAINTS_FULL_LIST.validateOrThrow(row);
            }

            return new RowSection(this);
        }
    }
}
