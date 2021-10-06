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


import static androidx.annotation.RestrictTo.Scope;
import static androidx.car.app.model.Action.FLAG_PRIMARY;

import static java.util.Objects.requireNonNull;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.model.Action;
import androidx.car.app.model.Action.ActionType;
import androidx.car.app.model.CarText;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Encapsulates the constraints to apply when rendering a list of {@link Action}s on a template.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY)
public final class ActionsConstraints {

    /** Conservative constraints for most template types. */
    @NonNull
    private static final ActionsConstraints ACTIONS_CONSTRAINTS_CONSERVATIVE =
            new ActionsConstraints.Builder()
                    .setTitleTextConstraints(CarTextConstraints.CONSERVATIVE)
                    .setMaxActions(2)
                    .build();

    /**
     * Constraints for actions within the template body.
     */
    @NonNull
    public static final ActionsConstraints ACTIONS_CONSTRAINTS_BODY =
            new ActionsConstraints.Builder(ACTIONS_CONSTRAINTS_CONSERVATIVE)
                    .setTitleTextConstraints(CarTextConstraints.COLOR_ONLY)
                    .setMaxCustomTitles(2)
                    .build();

    /**
     * Constraints for actions within the template body. The one of the action in this body can be
     * primary action.
     */
    @NonNull
    public static final ActionsConstraints ACTIONS_CONSTRAINTS_BODY_WITH_PRIMARY_ACTION =
            new ActionsConstraints.Builder(ACTIONS_CONSTRAINTS_CONSERVATIVE)
                    .setTitleTextConstraints(CarTextConstraints.COLOR_ONLY)
                    .setMaxCustomTitles(2)
                    .setMaxPrimaryActions(1)
                    .build();

    /**
     * Constraints for template headers, where only the special-purpose back and app-icon standard
     * actions are allowed.
     */
    @NonNull
    public static final ActionsConstraints ACTIONS_CONSTRAINTS_HEADER =
            new ActionsConstraints.Builder()
                    .setMaxActions(1)
                    .addDisallowedActionType(Action.TYPE_CUSTOM)
                    .build();

    /**
     * Default constraints that should be applied to most templates (2 actions, 1 can have
     * title)'s {@link androidx.car.app.model.ActionStrip}.
     */
    @NonNull
    public static final ActionsConstraints ACTIONS_CONSTRAINTS_SIMPLE =
            new ActionsConstraints.Builder(ACTIONS_CONSTRAINTS_CONSERVATIVE)
                    .setMaxCustomTitles(1)
                    .setTitleTextConstraints(CarTextConstraints.TEXT_ONLY)
                    .build();

    /** Constraints for navigation templates. */
    @NonNull
    public static final ActionsConstraints ACTIONS_CONSTRAINTS_NAVIGATION =
            new ActionsConstraints.Builder(ACTIONS_CONSTRAINTS_CONSERVATIVE)
                    .setMaxActions(4)
                    .setMaxCustomTitles(1)
                    // Must specify a custom stop action.
                    .addRequiredActionType(Action.TYPE_CUSTOM)
                    .setTitleTextConstraints(CarTextConstraints.TEXT_ONLY)
                    .build();

    /**
     * Constraints for map action buttons.
     *
     * <p>Only buttons with icons are allowed.
     */
    @NonNull
    public static final ActionsConstraints ACTIONS_CONSTRAINTS_MAP =
            new ActionsConstraints.Builder(ACTIONS_CONSTRAINTS_CONSERVATIVE)
                    .setMaxActions(4)
                    .build();

    private final int mMaxActions;
    private final int mMaxPrimaryActions;
    private final int mMaxCustomTitles;
    private final CarTextConstraints mTitleTextConstraints;
    private final Set<Integer> mRequiredActionTypes;
    private final Set<Integer> mDisallowedActionTypes;

    /** Returns the max number of actions allowed. */
    public int getMaxActions() {
        return mMaxActions;
    }

    /** Returns the max number of primary actions allowed. */
    public int getMaxPrimaryActions() {
        return mMaxPrimaryActions;
    }

    /** Returns the max number of actions with custom titles allowed. */
    public int getMaxCustomTitles() {
        return mMaxCustomTitles;
    }

    /** Returns the {@link CarTextConstraints} fpr the title. */
    @NonNull
    public CarTextConstraints getTitleTextConstraints() {
        return mTitleTextConstraints;
    }

    /** Adds the set of required action types. */
    @NonNull
    public Set<Integer> getRequiredActionTypes() {
        return mRequiredActionTypes;
    }

    /** Adds the set of disallowed action types. */
    @NonNull
    public Set<Integer> getDisallowedActionTypes() {
        return mDisallowedActionTypes;
    }

    /**
     * Validates the input list of {@link Action}s against this {@link ActionsConstraints} instance.
     *
     * @throws IllegalArgumentException if the actions has more actions than allowed, if it has
     *                                  more actions with custom titles than allowed, if the
     *                                  actions do not contain all required types, or if the
     *                                  actions contain any disallowed types
     */
    // TODO(b/201548973): Remove this annotation once set/getFlags are ready
    @androidx.annotation.OptIn(markerClass = ExperimentalCarApi.class)
    public void validateOrThrow(@NonNull List<Action> actions) {
        int maxAllowedActions = mMaxActions;
        int maxAllowedPrimaryActions = mMaxPrimaryActions;
        int maxAllowedCustomTitles = mMaxCustomTitles;

        Set<Integer> requiredTypes =
                mRequiredActionTypes.isEmpty()
                        ? Collections.emptySet()
                        : new HashSet<>(mRequiredActionTypes);

        for (Action action : actions) {
            if (mDisallowedActionTypes.contains(action.getType())) {
                throw new IllegalArgumentException(
                        Action.typeToString(action.getType()) + " is disallowed");
            }

            requiredTypes.remove(action.getType());

            CarText title = action.getTitle();
            if (title != null && !title.isEmpty()) {
                if (--maxAllowedCustomTitles < 0) {
                    throw new IllegalArgumentException(
                            "Action list exceeded max number of "
                                    + mMaxCustomTitles
                                    + " actions with custom titles");
                }

                mTitleTextConstraints.validateOrThrow(title);
            }

            if (--maxAllowedActions < 0) {
                throw new IllegalArgumentException(
                        "Action list exceeded max number of " + mMaxActions + " actions");
            }

            if ((action.getFlags() & FLAG_PRIMARY) != 0) {
                if (--maxAllowedPrimaryActions < 0) {
                    throw new IllegalArgumentException(
                            "Action list exceeded max number of "
                                    + mMaxPrimaryActions
                                    + " primary actions");
                }
            }
        }

        if (!requiredTypes.isEmpty()) {
            StringBuilder missingTypeError = new StringBuilder();
            for (@ActionType int type : requiredTypes) {
                missingTypeError.append(Action.typeToString(type)).append(",");
            }
            throw new IllegalArgumentException(
                    "Missing required action types: " + missingTypeError);
        }
    }

    ActionsConstraints(Builder builder) {
        mMaxActions = builder.mMaxActions;
        mMaxPrimaryActions = builder.mMaxPrimaryActions;
        mMaxCustomTitles = builder.mMaxCustomTitles;
        mTitleTextConstraints = builder.mTitleTextConstraints;
        mRequiredActionTypes = new HashSet<>(builder.mRequiredActionTypes);

        if (!builder.mDisallowedActionTypes.isEmpty()) {
            Set<Integer> disallowedActionTypes = new HashSet<>(builder.mDisallowedActionTypes);
            disallowedActionTypes.retainAll(mRequiredActionTypes);
            if (!disallowedActionTypes.isEmpty()) {
                throw new IllegalArgumentException(
                        "Disallowed action types cannot also be in the required set");
            }
        }
        mDisallowedActionTypes = new HashSet<>(builder.mDisallowedActionTypes);

        if (mRequiredActionTypes.size() > mMaxActions) {
            throw new IllegalArgumentException(
                    "Required action types exceeded max allowed actions");
        }
    }

    /**
     * A builder of {@link ActionsConstraints}.
     */
    @VisibleForTesting
    public static final class Builder {
        int mMaxActions = Integer.MAX_VALUE;
        int mMaxPrimaryActions = 0;
        int mMaxCustomTitles;
        CarTextConstraints mTitleTextConstraints = CarTextConstraints.UNCONSTRAINED;
        final Set<Integer> mRequiredActionTypes = new HashSet<>();
        final Set<Integer> mDisallowedActionTypes = new HashSet<>();

        /** Sets the maximum number of actions allowed. */
        @NonNull
        public Builder setMaxActions(int maxActions) {
            mMaxActions = maxActions;
            return this;
        }

        /** Sets the maximum number of primary actions allowed. */
        @NonNull
        public Builder setMaxPrimaryActions(int maxPrimaryActions) {
            mMaxPrimaryActions = maxPrimaryActions;
            return this;
        }

        /** Sets the maximum number of actions with custom titles allowed. */
        @NonNull
        public Builder setMaxCustomTitles(int maxCustomTitles) {
            mMaxCustomTitles = maxCustomTitles;
            return this;
        }

        /** Sets the {@link CarTextConstraints} for the title. */
        @NonNull
        public Builder setTitleTextConstraints(@NonNull CarTextConstraints carTextConstraints) {
            mTitleTextConstraints = carTextConstraints;
            return this;
        }

        /** Adds an action type to the set of required types. */
        @NonNull
        public Builder addRequiredActionType(@ActionType int actionType) {
            mRequiredActionTypes.add(actionType);
            return this;
        }

        /** Adds an action type to the set of disallowed types. */
        @NonNull
        public Builder addDisallowedActionType(@ActionType int actionType) {
            mDisallowedActionTypes.add(actionType);
            return this;
        }

        /**
         * Returns an {@link ActionsConstraints} instance defined by this builder.
         */
        @NonNull
        public ActionsConstraints build() {
            return new ActionsConstraints(this);
        }

        /** Returns an empty {@link Builder} instance. */
        public Builder() {
        }

        /**
         * Returns a new builder that contains the same data as the given {@link ActionsConstraints}
         * instance.
         *
         * @throws NullPointerException if {@code latLng} is {@code null}
         */
        public Builder(@NonNull ActionsConstraints constraints) {
            requireNonNull(constraints);
            mMaxActions = constraints.getMaxActions();
            mMaxPrimaryActions = constraints.getMaxPrimaryActions();
            mMaxCustomTitles = constraints.getMaxCustomTitles();
            mTitleTextConstraints = constraints.getTitleTextConstraints();
            mRequiredActionTypes.addAll(constraints.getRequiredActionTypes());
            mDisallowedActionTypes.addAll(constraints.getDisallowedActionTypes());
        }
    }
}
