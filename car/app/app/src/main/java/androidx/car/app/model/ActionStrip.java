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

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.model.Action.ActionType;

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
public class ActionStrip {
    @Keep
    private final List<Object> mActions;

    /** Constructs a new builder of {@link ActionStrip}. */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the list of {@link Action}'s.
     */
    @NonNull
    public List<Object> getActions() {
        return mActions;
    }

    /**
     * Returns the {@link Action} associated with the input {@code actionType}, or {@code null} if
     * no matching {@link Action} is found.
     */
    @Nullable
    public Action getActionOfType(@ActionType int actionType) {
        for (Object object : mActions) {
            if (object instanceof Action) {
                Action action = (Action) object;
                if (action.getType() == actionType) {
                    return action;
                }
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
        return Objects.hashCode(mActions);
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

        return Objects.equals(mActions, otherActionStrip.mActions);
    }

    private ActionStrip(Builder builder) {
        mActions = builder.mActions;
    }

    /** Constructs an empty instance, used by serialization code. */
    private ActionStrip() {
        mActions = Collections.emptyList();
    }

    /** A builder of {@link ActionStrip}. */
    public static final class Builder {
        private final List<Object> mActions = new ArrayList<>();
        private final Set<Integer> mAddedActionTypes = new HashSet<>();

        /**
         * Adds an {@link Action} to the list.
         *
         * @throws IllegalArgumentException if the background color of the action is specified.
         * @throws IllegalArgumentException if {@code action} is a standard action and an action of
         *                                  the same type has already been added.
         * @throws NullPointerException     if {@code action} is {@code null}.
         */
        @NonNull
        public Builder addAction(@NonNull Action action) {
            Action actionObj = requireNonNull(action);
            int actionType = actionObj.getType();
            if (actionType != Action.TYPE_CUSTOM && mAddedActionTypes.contains(actionType)) {
                throw new IllegalArgumentException(
                        "Duplicated action types are disallowed: " + action);
            }
            if (!CarColor.DEFAULT.equals(actionObj.getBackgroundColor())) {
                throw new IllegalArgumentException(
                        "Action strip actions don't support background colors");
            }
            mAddedActionTypes.add(actionType);
            mActions.add(action);
            return this;
        }

        /**
         * Clears any actions that may have been added with {@link #addAction(Action)} up to this
         * point.
         */
        @NonNull
        public Builder clearActions() {
            mActions.clear();
            mAddedActionTypes.clear();
            return this;
        }

        /**
         * Constructs the {@link ActionStrip} defined by this builder.
         *
         * @throws IllegalStateException if the action strip is empty.
         */
        @NonNull
        public ActionStrip build() {
            if (mActions.isEmpty()) {
                throw new IllegalStateException("Action strip must contain at least one action");
            }
            return new ActionStrip(this);
        }
    }
}
