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

package androidx.car.app.model.constraints;

import static java.util.Objects.requireNonNull;

import androidx.annotation.RestrictTo;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Row;

import org.jspecify.annotations.NonNull;

/**
 * Encapsulates the constraints to apply when rendering a {@link Row} in different contexts.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class RowConstraints {
    public static final @NonNull RowConstraints UNCONSTRAINED =
            new RowConstraints.Builder().build();

    /** Conservative constraints for a row. */
    public static final @NonNull RowConstraints ROW_CONSTRAINTS_CONSERVATIVE =
            new RowConstraints.Builder()
                    .setMaxActionsExclusive(0)
                    .setImageAllowed(false)
                    .setMaxTextLinesPerRow(1)
                    .setOnClickListenerAllowed(true)
                    .setToggleAllowed(false)
                    .build();

    /**
     * The constraints for a row in a pane.
     *
     * <p>Note: Toggles are allowed from API >= 6. Old hosts may render bad UI if the
     * toggles are added to hosts that don't support them.
     */
    public static final @NonNull RowConstraints ROW_CONSTRAINTS_PANE =
            new RowConstraints.Builder()
                    .setMaxActionsExclusive(2)
                    .setImageAllowed(true)
                    .setMaxTextLinesPerRow(2)
                    .setToggleAllowed(true)
                    .setOnClickListenerAllowed(false)
                    .build();

    /**
     * The constraints for a simple row (2 rows of text, 1 image, 1 toggle)
     *
     * <p>Note: Toggles are allowed from API >= 6. Old hosts may render bad UI if the
     * toggles are added to hosts that don't support them.
     */
    public static final @NonNull RowConstraints ROW_CONSTRAINTS_SIMPLE =
            new RowConstraints.Builder()
                    .setMaxActionsExclusive(0)
                    .setImageAllowed(true)
                    .setMaxTextLinesPerRow(2)
                    .setToggleAllowed(true)
                    .setOnClickListenerAllowed(true)
                    .build();

    /**
     * Constraints for rows in a full list which enforces the following:
     *
     * <ul>
     *     <li>No actions (note: this is different than the click listener which turns the entire
     *     row into a clickable "action")
     *     <li>Maximum 2 lines of text below the title
     *     <li>Can have an image
     *     <li>Can have a toggle
     *     <li>Can have a click listener
     * </ul>
     */
    public static final @NonNull RowConstraints ROW_CONSTRAINTS_FULL_LIST =
            new RowConstraints.Builder(ROW_CONSTRAINTS_SIMPLE).setToggleAllowed(true).build();

    private final int mMaxTextLinesPerRow;
    private final int mMaxActionsExclusive;
    private final boolean mIsImageAllowed;
    private final boolean mIsToggleAllowed;
    private final boolean mIsOnClickListenerAllowed;
    private final CarIconConstraints mCarIconConstraints;

    /** Returns whether the row can have a click listener associated with it. */
    public boolean isOnClickListenerAllowed() {
        return mIsOnClickListenerAllowed;
    }

    /** Returns the maximum number lines of text, excluding the title, to render in the row. */
    public int getMaxTextLinesPerRow() {
        return mMaxTextLinesPerRow;
    }

    /** Returns the maximum number actions to allowed in a row that consists only of actions. */
    public int getMaxActionsExclusive() {
        return mMaxActionsExclusive;
    }

    /** Returns whether a toggle can be added to the row. */
    public boolean isToggleAllowed() {
        return mIsToggleAllowed;
    }

    /** Returns whether an image can be added to the row. */
    public boolean isImageAllowed() {
        return mIsImageAllowed;
    }

    /** Returns the {@link CarIconConstraints} enforced for the row images. */
    public @NonNull CarIconConstraints getCarIconConstraints() {
        return mCarIconConstraints;
    }

    /**
     * Validates that the given row satisfies this {@link RowConstraints} instance.
     *
     * @throws IllegalArgumentException if the constraints are not met
     */
    public void validateOrThrow(@NonNull Row row) {
        if (!mIsOnClickListenerAllowed && row.getOnClickDelegate() != null) {
            throw new IllegalArgumentException("A click listener is not allowed on the row");
        }

        if (!mIsToggleAllowed && row.getToggle() != null) {
            throw new IllegalArgumentException("A toggle is not allowed on the row");
        }

        CarIcon image = row.getImage();
        if (image != null) {
            if (!mIsImageAllowed) {
                throw new IllegalArgumentException("An image is not allowed on the row");
            }

            mCarIconConstraints.validateOrThrow(image);
        }

        if (row.getTexts().size() > mMaxTextLinesPerRow) {
            throw new IllegalArgumentException(
                    "The number of lines of texts for the row exceeded the supported max of "
                            + mMaxTextLinesPerRow);
        }
    }

    RowConstraints(Builder builder) {
        mIsOnClickListenerAllowed = builder.mIsOnClickListenerAllowed;
        mMaxTextLinesPerRow = builder.mMaxTextLines;
        mMaxActionsExclusive = builder.mMaxActionsExclusive;
        mIsToggleAllowed = builder.mIsToggleAllowed;
        mIsImageAllowed = builder.mIsImageAllowed;
        mCarIconConstraints = builder.mCarIconConstraints;
    }

    /** A builder of {@link RowConstraints}. */
    public static final class Builder {
        boolean mIsOnClickListenerAllowed = true;
        boolean mIsToggleAllowed = true;
        int mMaxTextLines = Integer.MAX_VALUE;
        int mMaxActionsExclusive = Integer.MAX_VALUE;
        boolean mIsImageAllowed = true;
        CarIconConstraints mCarIconConstraints = CarIconConstraints.UNCONSTRAINED;

        /** Sets whether the row can have a click listener associated with it. */
        public @NonNull Builder setOnClickListenerAllowed(boolean isOnClickListenerAllowed) {
            mIsOnClickListenerAllowed = isOnClickListenerAllowed;
            return this;
        }

        /** Sets the maximum number lines of text, excluding the title, to render in the row. */
        public @NonNull Builder setMaxTextLinesPerRow(int maxTextLinesPerRow) {
            mMaxTextLines = maxTextLinesPerRow;
            return this;
        }

        /** Sets the maximum number actions to allowed in a row that consists only of actions. */
        public @NonNull Builder setMaxActionsExclusive(int maxActionsExclusive) {
            mMaxActionsExclusive = maxActionsExclusive;
            return this;
        }

        /** Sets whether an image can be added to the row. */
        public @NonNull Builder setImageAllowed(boolean imageAllowed) {
            mIsImageAllowed = imageAllowed;
            return this;
        }

        /** Sets whether a toggle can be added to the row. */
        public @NonNull Builder setToggleAllowed(boolean toggleAllowed) {
            mIsToggleAllowed = toggleAllowed;
            return this;
        }

        /** Sets the {@link CarIconConstraints} enforced for the row images. */
        public @NonNull Builder setCarIconConstraints(
                @NonNull CarIconConstraints carIconConstraints) {
            mCarIconConstraints = carIconConstraints;
            return this;
        }

        /**
         * Constructs the {@link RowConstraints} defined by this builder.
         */
        public @NonNull RowConstraints build() {
            return new RowConstraints(this);
        }

        /** Returns an empty {@link Builder} instance. */
        public Builder() {
        }

        /**
         * Returns a new builder that contains the same data as the given {@link RowConstraints}
         * instance.
         *
         * @throws NullPointerException if {@code latLng} is {@code null}
         */
        public Builder(@NonNull RowConstraints constraints) {
            requireNonNull(constraints);
            mIsOnClickListenerAllowed = constraints.isOnClickListenerAllowed();
            mMaxTextLines = constraints.getMaxTextLinesPerRow();
            mMaxActionsExclusive = constraints.getMaxActionsExclusive();
            mIsToggleAllowed = constraints.isToggleAllowed();
            mIsImageAllowed = constraints.isImageAllowed();
            mCarIconConstraints = constraints.getCarIconConstraints();
        }
    }
}
