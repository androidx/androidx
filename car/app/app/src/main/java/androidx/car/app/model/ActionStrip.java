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

import static androidx.car.app.model.Action.FLAG_PRIMARY;

import static java.util.Objects.requireNonNull;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.model.Action.ActionType;
import androidx.car.app.model.constraints.CarTextConstraints;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a list of {@link Action}s that are used for a template.
 *
 * <p>The {@link Action}s in the {@link ActionStrip} may be displayed differently depending on the
 * template they are used with. For example, a map template may display them as a group of floating
 * action buttons (FABs) over the map background.
 *
 * <p>See the documentation of individual {@link Template}s on restrictions around what actions are
 * supported.
 */
@CarProtocol
public final class ActionStrip {
    @Keep
    @Deprecated
    private final List<Action> mActions;

    @Keep
    private final List<ActionStripAction> mActionStripActions;

    /**
     * Returns the list of {@link Action}s in the strip.
     *
     * @see Builder#addAction(Action)
     */
    @NonNull
    public List<Action> getActions() {
        if (mActions != null && !mActions.isEmpty()) {
            return mActions;
        }

        final List<Action> actions = new ArrayList<>();
        for (ActionStripAction action : mActionStripActions) {
            if (action.getAction() != null) {
                actions.add(action.getAction());
            }
        }
        return actions;
    }

    /**
     * Returns {@code true} if {@link Action} always shows in the strip.
     *
     * @see Builder#addAction(Action, boolean)
     */
    public boolean isActionPersistent(@NonNull Action action) {
        for (int i = 0; i < mActionStripActions.size(); i++) {
            if (action.equals(mActionStripActions.get(i).getAction())) {
                return mActionStripActions.get(i).isPersistent();
            }
        }

        return false;
    }

    /**
     * Returns the first {@link Action} associated with the input {@code actionType} or {@code
     * null} if no matching {@link Action} is found.
     */
    @Nullable
    public Action getFirstActionOfType(@ActionType int actionType) {
        for (Action action : mActions) {
            if (action.getType() == actionType) {
                return action;
            }
        }

        return null;
    }

    @Override
    @NonNull
    public String toString() {
        return "[action count: " + mActions.size() + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mActions, mActionStripActions);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ActionStrip)) {
            return false;
        }
        ActionStrip otherActionStrip = (ActionStrip) other;

        return Objects.equals(mActions, otherActionStrip.mActions)
                && Objects.equals(mActionStripActions, otherActionStrip.mActionStripActions);
    }

    ActionStrip(Builder builder) {
        mActionStripActions = builder.mActions;
        mActions = getActions();
    }

    /** Constructs an empty instance, used by serialization code. */
    private ActionStrip() {
        mActionStripActions = Collections.emptyList();
        mActions = Collections.emptyList();
    }

    /** A builder of {@link ActionStrip}. */
    public static final class Builder {
        final List<ActionStripAction> mActions = new ArrayList<>();
        final Set<Integer> mAddedActionTypes = new HashSet<>();

        /**
         * Adds an {@link Action} to the list which will not always show inside the
         * {@link ActionStrip}.
         *
         * @see #addAction(Action, boolean)
         */
        @NonNull
        public Builder addAction(@NonNull Action action) {
            return addAction(action, /* alwaysShow= */false);
        }

        /**
         * Adds an {@link Action} to the list with a flag that signifies whether the action will
         * always show inside the {@link ActionStrip}.
         *
         * <p>Background colors are not supported on an action inside an {@link ActionStrip}.
         *
         * <p>Primary actions are not supported.
         *
         * <p>Spans are not supported in the title of the action and will be ignored.
         *
         * @param action                    the {@link Action} to be added to {@link ActionStrip}
         * @param alwaysShow                {@code true} if action is to ignore fade in/out
         *                                  animation in {@link ActionStrip}
         * @throws IllegalArgumentException if the background color of the action is specified,
         *                                  or if {@code action} is a standard action and an
         *                                  action of the same type has already been added, of if
         *                                  the {@code action}'s title contains unsupported spans.
         * @throws NullPointerException     if {@code action} is {@code null}
         */
        @NonNull
        public Builder addAction(@NonNull Action action, boolean alwaysShow) {
            Action actionObj = requireNonNull(action);
            int actionType = actionObj.getType();
            if (actionType != Action.TYPE_CUSTOM && mAddedActionTypes.contains(actionType)) {
                throw new IllegalArgumentException(
                        "Duplicated action types are disallowed: " + action);
            }
            if ((action.getFlags() & FLAG_PRIMARY) != 0) {
                throw new IllegalArgumentException(
                        "Primary actions are disallowed: " + action);
            }
            if (!CarColor.DEFAULT.equals(actionObj.getBackgroundColor())) {
                throw new IllegalArgumentException(
                        "Action strip actions don't support background colors");
            }
            CarText title = action.getTitle();
            if (title != null) {
                CarTextConstraints.CONSERVATIVE.validateOrThrow(title);
            }

            mAddedActionTypes.add(actionType);
            mActions.add(new ActionStripAction(action, alwaysShow));
            return this;
        }

        /**
         * Constructs the {@link ActionStrip} defined by this builder.
         *
         * @throws IllegalStateException if the action strip is empty
         */
        @NonNull
        public ActionStrip build() {
            if (mActions.isEmpty()) {
                throw new IllegalStateException("Action strip must contain at least one action");
            }
            return new ActionStrip(this);
        }

        /** Creates an empty {@link Builder} instance. */
        public Builder() {
        }
    }

    /** An {@link Action} wrapper which also holds the always show value for the action. */
    private static final class ActionStripAction {
        @Keep
        @Nullable
        private final Action mAction;
        @Keep
        private final boolean mIsPersistent;

        ActionStripAction() {
            this.mAction = null;
            this.mIsPersistent = false;
        }

        ActionStripAction(@Nullable Action action) {
            this.mAction = action;
            this.mIsPersistent = false;
        }

        ActionStripAction(@Nullable Action action, boolean isPersistent) {
            this.mAction = action;
            this.mIsPersistent = isPersistent;
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ActionStripAction)) {
                return false;
            }
            ActionStripAction otherAction = (ActionStripAction) other;

            return Objects.equals(otherAction.getAction(), getAction())
                    && otherAction.isPersistent() == isPersistent();
        }

        @Override
        public int hashCode() {
            return Objects.hash(mAction, mIsPersistent);
        }

        @Nullable
        Action getAction() {
            return mAction;
        }

        boolean isPersistent() {
            return mIsPersistent;
        }
    }
}
