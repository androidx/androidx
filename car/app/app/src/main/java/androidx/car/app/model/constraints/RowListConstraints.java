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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.car.app.model.constraints.RowConstraints.ROW_CONSTRAINTS_CONSERVATIVE;
import static androidx.car.app.model.constraints.RowConstraints.ROW_CONSTRAINTS_FULL_LIST;
import static androidx.car.app.model.constraints.RowConstraints.ROW_CONSTRAINTS_PANE;
import static androidx.car.app.model.constraints.RowConstraints.ROW_CONSTRAINTS_SIMPLE;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.car.app.model.ActionList;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Pane;
import androidx.car.app.model.SectionedItemList;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the constraints to apply when rendering a row list under different contexts.
 */
public class RowListConstraints {
    /**
     * The RowList that is used for max row.
     *
     * @hide
     */
    // TODO(shiufai): investigate how to expose IntDefs if needed.
    @IntDef(value = {DEFAULT_LIST, PANE, ROUTE_PREVIEW})
    @RestrictTo(LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    public @interface ListType {
    }

    @ListType
    public static final int DEFAULT_LIST = 0;

    @ListType
    public static final int PANE = 1;

    @ListType
    public static final int ROUTE_PREVIEW = 2;

    /** Conservative constraints for all types lists. */
    @NonNull
    public static final RowListConstraints ROW_LIST_CONSTRAINTS_CONSERVATIVE =
            RowListConstraints.builder()
                    .setRowListType(DEFAULT_LIST)
                    .setMaxActions(0)
                    .setRowConstraints(ROW_CONSTRAINTS_CONSERVATIVE)
                    .setAllowSelectableLists(false)
                    .build();

    /** Default constraints for heterogeneous pane of items, full width. */
    @NonNull
    public static final RowListConstraints ROW_LIST_CONSTRAINTS_PANE =
            ROW_LIST_CONSTRAINTS_CONSERVATIVE
                    .newBuilder()
                    .setMaxActions(2)
                    .setRowListType(PANE)
                    .setRowConstraints(ROW_CONSTRAINTS_PANE)
                    .setAllowSelectableLists(false)
                    .build();

    /** Default constraints for uniform lists of items, no toggles. */
    @NonNull
    public static final RowListConstraints ROW_LIST_CONSTRAINTS_SIMPLE =
            ROW_LIST_CONSTRAINTS_CONSERVATIVE
                    .newBuilder()
                    .setRowConstraints(ROW_CONSTRAINTS_SIMPLE)
                    .build();

    /** Default constraints for the route preview card. */
    @NonNull
    public static final RowListConstraints ROW_LIST_CONSTRAINTS_ROUTE_PREVIEW =
            ROW_LIST_CONSTRAINTS_CONSERVATIVE
                    .newBuilder()
                    .setRowListType(ROUTE_PREVIEW)
                    .setRowConstraints(ROW_CONSTRAINTS_SIMPLE)
                    .setAllowSelectableLists(true)
                    .build();

    /** Default constraints for uniform lists of items, full width (simple + toggle support). */
    @NonNull
    public static final RowListConstraints ROW_LIST_CONSTRAINTS_FULL_LIST =
            ROW_LIST_CONSTRAINTS_CONSERVATIVE
                    .newBuilder()
                    .setRowConstraints(ROW_CONSTRAINTS_FULL_LIST)
                    .setAllowSelectableLists(true)
                    .build();

    @ListType
    private final int mRowListType;
    private final int mMaxActions;
    private final RowConstraints mRowConstraints;
    private final boolean mAllowSelectableLists;

    /** A builder of {@link RowListConstraints}. */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /** Return a a new builder for this {@link RowListConstraints} instance. */
    @NonNull
    public Builder newBuilder() {
        return new Builder(this);
    }

    /** Returns the row list type for this constraint. */
    @ListType
    public int getRowListType() {
        return mRowListType;
    }

    /** Returns the maximum number of actions allowed to be added alongside the list. */
    public int getMaxActions() {
        return mMaxActions;
    }

    /** Returns the constraints to apply on individual rows. */
    @NonNull
    public RowConstraints getRowConstraints() {
        return mRowConstraints;
    }

    /** Returns whether selectable lists are allowed. */
    public boolean isAllowSelectableLists() {
        return mAllowSelectableLists;
    }

    /**
     * Validates that the {@link ItemList} satisfies this {@link RowListConstraints} instance.
     *
     * @throws IllegalArgumentException if the constraints are not met.
     */
    public void validateOrThrow(@NonNull ItemList itemList) {
        if (itemList.getOnSelectedListener() != null && !mAllowSelectableLists) {
            throw new IllegalArgumentException("Selectable lists are not allowed");
        }

        validateRows(itemList.getItems());
    }

    /**
     * Validates that the list of {@link SectionedItemList}s satisfies this
     * {@link RowListConstraints}
     * instance.
     *
     * @throws IllegalArgumentException if the constraints are not met.
     */
    public void validateOrThrow(@NonNull List<SectionedItemList> sections) {
        List<Object> combinedLists = new ArrayList<>();

        for (SectionedItemList section : sections) {
            ItemList sectionList = section.getItemList();
            if (sectionList.getOnSelectedListener() != null && !mAllowSelectableLists) {
                throw new IllegalArgumentException("Selectable lists are not allowed");
            }

            combinedLists.addAll(sectionList.getItems());
        }

        validateRows(combinedLists);
    }

    /**
     * Validates that the {@link Pane} satisfies this {@link RowListConstraints} instance.
     *
     * @throws IllegalArgumentException if the constraints are not met.
     */
    public void validateOrThrow(@NonNull Pane pane) {
        ActionList actions = pane.getActionList();
        if (actions != null && actions.getList().size() > mMaxActions) {
            throw new IllegalArgumentException(
                    "The number of actions on the pane exceeded the supported max of "
                            + mMaxActions);
        }

        validateRows(pane.getRows());
    }

    private void validateRows(List<Object> rows) {
        for (Object rowObj : rows) {
            mRowConstraints.validateOrThrow(rowObj);
        }
    }

    private RowListConstraints(Builder builder) {
        mMaxActions = builder.mMaxActions;
        mRowConstraints = builder.mRowConstraints;
        mAllowSelectableLists = builder.mAllowSelectableLists;
        mRowListType = builder.mRowListType;
    }

    /**
     * A builder of {@link RowListConstraints}.
     */
    public static final class Builder {
        @ListType
        private int mRowListType;
        private int mMaxActions;
        private RowConstraints mRowConstraints = RowConstraints.UNCONSTRAINED;
        private boolean mAllowSelectableLists;

        /** Sets the row list type for this constraint. */
        @NonNull
        public Builder setRowListType(@ListType int rowListType) {
            this.mRowListType = rowListType;
            return this;
        }

        /** Sets the maximum number of actions allowed to be added alongside the list. */
        @NonNull
        public Builder setMaxActions(int maxActions) {
            this.mMaxActions = maxActions;
            return this;
        }

        /** Sets the constraints to apply on individual rows. */
        @NonNull
        public Builder setRowConstraints(@NonNull RowConstraints rowConstraints) {
            this.mRowConstraints = rowConstraints;
            return this;
        }

        /** Sets whether selectable lists are allowed. */
        @NonNull
        public Builder setAllowSelectableLists(boolean allowSelectableLists) {
            this.mAllowSelectableLists = allowSelectableLists;
            return this;
        }

        /**
         * Constructs the {@link RowListConstraints} defined by this builder.
         */
        @NonNull
        public RowListConstraints build() {
            return new RowListConstraints(this);
        }

        private Builder() {
        }

        private Builder(RowListConstraints constraints) {
            this.mMaxActions = constraints.mMaxActions;
            this.mRowConstraints = constraints.mRowConstraints;
            this.mAllowSelectableLists = constraints.mAllowSelectableLists;
            this.mRowListType = constraints.mRowListType;
        }
    }
}
