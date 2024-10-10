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

import static java.util.Objects.requireNonNull;

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.utils.CollectionUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a list of rows used for displaying informational content and a set of {@link Action}s
 * that users can perform based on such content.
 */
@CarProtocol
@KeepFields
public final class Pane {
    private final List<Action> mActionList;
    private final List<Row> mRows;
    private final boolean mIsLoading;
    private final @Nullable CarIcon mImage;

    /**
     * Returns whether the pane is in a loading state.
     *
     * @see Builder#setLoading(boolean)
     */
    public boolean isLoading() {
        return mIsLoading;
    }

    /**
     * Returns the list of {@link Action}s displayed alongside the {@link Row}s in this pane.
     */
    public @NonNull List<Action> getActions() {
        return CollectionUtils.emptyIfNull(mActionList);
    }

    /**
     * Returns the list of {@link Row} objects that make up the {@link Pane}.
     */
    public @NonNull List<Row> getRows() {
        return CollectionUtils.emptyIfNull(mRows);
    }

    /**
     * Returns the optional image to display in this pane.
     */
    @RequiresCarApi(4)
    public @Nullable CarIcon getImage() {
        return mImage;
    }

    @Override
    public @NonNull String toString() {
        return "[ rows: "
                + (mRows != null ? mRows.toString() : null)
                + ", action list: "
                + mActionList
                + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mRows, mActionList, mIsLoading, mImage);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Pane)) {
            return false;
        }
        Pane otherPane = (Pane) other;

        return mIsLoading == otherPane.mIsLoading
                && Objects.equals(mActionList, otherPane.mActionList)
                && Objects.equals(mRows, otherPane.mRows)
                && Objects.equals(mImage, otherPane.mImage);
    }

    Pane(Builder builder) {
        mRows = CollectionUtils.unmodifiableCopy(builder.mRows);
        mActionList = CollectionUtils.unmodifiableCopy(builder.mActionList);
        mImage = builder.mImage;
        mIsLoading = builder.mIsLoading;
    }

    /** Constructs an empty instance, used by serialization code. */
    private Pane() {
        mRows = Collections.emptyList();
        mActionList = Collections.emptyList();
        mIsLoading = false;
        mImage = null;
    }

    /** A builder of {@link Pane}. */
    public static final class Builder {
        final List<Row> mRows = new ArrayList<>();
        List<Action> mActionList = new ArrayList<>();
        boolean mIsLoading;
        @Nullable CarIcon mImage;

        /**
         * Sets whether the {@link Pane} is in a loading state.
         *
         * <p>If set to {@code true}, the UI will display a loading indicator where the list content
         * would be otherwise. The caller is expected to call {@link
         * androidx.car.app.Screen#invalidate()} and send the new template content
         * to the host once the data is ready. If set to {@code false}, the UI shows the actual row
         * contents.
         *
         * @see #build
         */
        public @NonNull Builder setLoading(boolean isLoading) {
            mIsLoading = isLoading;
            return this;
        }

        /**
         * Adds a row to display in the list.
         *
         * @throws NullPointerException if {@code row} is {@code null}
         */
        public @NonNull Builder addRow(@NonNull Row row) {
            mRows.add(requireNonNull(row));
            return this;
        }

        /**
         * Adds an {@link Action} to display alongside the rows in the pane.
         *
         * <p>By default, no actions are displayed.
         *
         * @throws NullPointerException if {@code action} is {@code null}
         */
        public @NonNull Builder addAction(@NonNull Action action) {
            requireNonNull(action);
            mActionList.add(action);
            return this;
        }

        /**
         * Sets an {@link CarIcon} to display alongside the rows in the pane.
         *
         * <h4>Image Sizing Guidance</h4>
         *
         * To minimize scaling artifacts across a wide range of car screens, apps should provide
         * images targeting a 480 x 480 dp bounding box. If the image exceeds this maximum size
         * in either one of the dimensions, it will be scaled down to be centered inside the
         * bounding box while preserving its aspect ratio.
         *
         * @throws NullPointerException if {@code image} is {@code null}
         */
        @RequiresCarApi(4)
        public @NonNull Builder setImage(@NonNull CarIcon image) {
            mImage = requireNonNull(image);
            return this;
        }

        /**
         * Constructs the row list defined by this builder.
         *
         * @throws IllegalStateException if the pane is in loading state and also contains rows, or
         *                               vice versa
         */
        public @NonNull Pane build() {
            int size = size();
            if (size > 0 == mIsLoading) {
                throw new IllegalStateException(
                        "The pane is set to loading but is not empty, or vice versa");
            }

            return new Pane(this);
        }

        private int size() {
            return mRows.size();
        }

        /** Returns an empty {@link Builder} instance. */
        public Builder() {
        }
    }
}
