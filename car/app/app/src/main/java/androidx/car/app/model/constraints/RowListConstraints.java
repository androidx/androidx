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

import static androidx.car.app.model.constraints.RowConstraints.ROW_CONSTRAINTS_CONSERVATIVE;
import static androidx.car.app.model.constraints.RowConstraints.ROW_CONSTRAINTS_FULL_LIST;
import static androidx.car.app.model.constraints.RowConstraints.ROW_CONSTRAINTS_PANE;
import static androidx.car.app.model.constraints.RowConstraints.ROW_CONSTRAINTS_SIMPLE;

import static java.util.Objects.requireNonNull;

import androidx.annotation.RestrictTo;
import androidx.car.app.messaging.model.ConversationItem;
import androidx.car.app.model.Action;
import androidx.car.app.model.Item;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Pane;
import androidx.car.app.model.Row;
import androidx.car.app.model.SectionedItemList;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the constraints to apply when rendering a row list under different contexts.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class RowListConstraints {
    /** Conservative constraints for all types lists. */
    public static final @NonNull RowListConstraints ROW_LIST_CONSTRAINTS_CONSERVATIVE =
            new RowListConstraints.Builder()
                    .setMaxActions(0)
                    .setRowConstraints(ROW_CONSTRAINTS_CONSERVATIVE)
                    .setAllowSelectableLists(false)
                    .build();

    /** Default constraints for heterogeneous pane of items, full width. */
    public static final @NonNull RowListConstraints ROW_LIST_CONSTRAINTS_PANE =
            new RowListConstraints.Builder(ROW_LIST_CONSTRAINTS_CONSERVATIVE)
                    .setMaxActions(2)
                    .setRowConstraints(ROW_CONSTRAINTS_PANE)
                    .setAllowSelectableLists(false)
                    .build();

    /** Default constraints for uniform lists of items, no toggles. */
    public static final @NonNull RowListConstraints ROW_LIST_CONSTRAINTS_SIMPLE =
            new RowListConstraints.Builder(ROW_LIST_CONSTRAINTS_CONSERVATIVE)
                    .setRowConstraints(ROW_CONSTRAINTS_SIMPLE)
                    .build();

    /**
     * Default constraints for the route preview card.
     *
     * @deprecated This is deprecated. Use
     * {@link #MAP_ROW_LIST_CONSTRAINTS_ALLOW_SELECTABLE} instead.
     *
     * As more half sheet template lists allow selectable lists, this constraints is
     * too narrow for the only use case at route preview template. Use
     * {@link #MAP_ROW_LIST_CONSTRAINTS_ALLOW_SELECTABLE} for more general half sheet lists.
     */
    @Deprecated
    public static final @NonNull RowListConstraints ROW_LIST_CONSTRAINTS_ROUTE_PREVIEW =
            new RowListConstraints.Builder(ROW_LIST_CONSTRAINTS_CONSERVATIVE)
                    .setRowConstraints(ROW_CONSTRAINTS_SIMPLE)
                    .setAllowSelectableLists(true)
                    .build();

    /** Default constraints for half sheet template lists that allow selectable lists. */
    public static final @NonNull RowListConstraints MAP_ROW_LIST_CONSTRAINTS_ALLOW_SELECTABLE =
            new RowListConstraints.Builder(ROW_LIST_CONSTRAINTS_CONSERVATIVE)
                    .setRowConstraints(ROW_CONSTRAINTS_SIMPLE)
                    .setAllowSelectableLists(true)
                    .build();


    /** Default constraints for uniform lists of items, full width (simple + toggle support). */
    public static final @NonNull RowListConstraints ROW_LIST_CONSTRAINTS_FULL_LIST =
            new RowListConstraints.Builder(ROW_LIST_CONSTRAINTS_CONSERVATIVE)
                    .setRowConstraints(ROW_CONSTRAINTS_FULL_LIST)
                    .setAllowSelectableLists(true)
                    .build();

    private final int mMaxActions;
    private final RowConstraints mRowConstraints;
    private final boolean mAllowSelectableLists;

    /** Returns the maximum number of actions allowed to be added alongside the list. */
    public int getMaxActions() {
        return mMaxActions;
    }

    /** Returns the constraints to apply on individual rows. */
    public @NonNull RowConstraints getRowConstraints() {
        return mRowConstraints;
    }

    /** Returns whether selectable lists are allowed. */
    public boolean isAllowSelectableLists() {
        return mAllowSelectableLists;
    }

    /**
     * Validates that the {@link ItemList} satisfies this {@link RowListConstraints} instance.
     *
     * @throws IllegalArgumentException if the constraints are not met, or if the list contains
     *                                  non-row instances
     */
    public void validateOrThrow(@NonNull ItemList itemList) {
        if (itemList.getOnSelectedDelegate() != null && !mAllowSelectableLists) {
            throw new IllegalArgumentException("Selectable lists are not allowed");
        }

        validateRows(itemList.getItems());
    }

    /**
     * Validates that the list of {@link SectionedItemList}s satisfies this
     * {@link RowListConstraints} instance.
     *
     * @throws IllegalArgumentException if the constraints are not met or if the lists contain
     *                                  any non-row instances
     */
    public void validateOrThrow(@NonNull List<SectionedItemList> sections) {
        List<Item> combinedLists = new ArrayList<>();

        for (SectionedItemList section : sections) {
            ItemList sectionList = section.getItemList();
            if (sectionList.getOnSelectedDelegate() != null && !mAllowSelectableLists) {
                throw new IllegalArgumentException("Selectable lists are not allowed");
            }

            combinedLists.addAll(sectionList.getItems());
        }

        validateRows(combinedLists);
    }

    /**
     * Validates that the {@link Pane} satisfies this {@link RowListConstraints} instance.
     *
     * @throws IllegalArgumentException if the constraints are not met
     */
    public void validateOrThrow(@NonNull Pane pane) {
        List<Action> actions = pane.getActions();
        if (actions.size() > mMaxActions) {
            throw new IllegalArgumentException(
                    "The number of actions on the pane exceeded the supported max of "
                            + mMaxActions);
        }

        validateRows(pane.getRows());
    }

    private void validateRows(List<? extends Item> rows) {
        for (Item rowObj : rows) {
            if (rowObj instanceof Row) {
                mRowConstraints.validateOrThrow((Row) rowObj);
            } else if (rowObj instanceof ConversationItem) {
                // ExperimentalCarApi -- unrestricted for now
            } else {
                throw new IllegalArgumentException(String.format(
                        "Unsupported item type: %s",
                        rowObj.getClass().getSimpleName()
                ));
            }
        }
    }

    RowListConstraints(Builder builder) {
        mMaxActions = builder.mMaxActions;
        mRowConstraints = builder.mRowConstraints;
        mAllowSelectableLists = builder.mAllowSelectableLists;
    }

    /**
     * A builder of {@link RowListConstraints}.
     */
    public static final class Builder {
        int mMaxActions;
        RowConstraints mRowConstraints = RowConstraints.UNCONSTRAINED;
        boolean mAllowSelectableLists;

        /** Sets the maximum number of actions allowed to be added alongside the list. */
        public @NonNull Builder setMaxActions(int maxActions) {
            mMaxActions = maxActions;
            return this;
        }

        /** Sets the constraints to apply on individual rows. */
        public @NonNull Builder setRowConstraints(@NonNull RowConstraints rowConstraints) {
            mRowConstraints = rowConstraints;
            return this;
        }

        /** Sets whether selectable lists are allowed. */
        public @NonNull Builder setAllowSelectableLists(boolean allowSelectableLists) {
            mAllowSelectableLists = allowSelectableLists;
            return this;
        }

        /**
         * Constructs the {@link RowListConstraints} defined by this builder.
         */
        public @NonNull RowListConstraints build() {
            return new RowListConstraints(this);
        }

        /** Returns an empty {@link Builder} instance. */
        public Builder() {
        }

        /**
         * Return a a new builder for the given {@link RowListConstraints} instance.
         *
         * @throws NullPointerException if {@code latLng} is {@code null}
         */
        public Builder(@NonNull RowListConstraints constraints) {
            requireNonNull(constraints);
            mMaxActions = constraints.getMaxActions();
            mRowConstraints = constraints.getRowConstraints();
            mAllowSelectableLists = constraints.isAllowSelectableLists();
        }
    }
}
