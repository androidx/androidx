/*
 * Copyright 2022 The Android Open Source Project
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

import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_HEADER;

import static java.util.Objects.requireNonNull;

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.constraints.CarTextConstraints;
import androidx.car.app.navigation.model.MapWithContentTemplate;
import androidx.car.app.utils.CollectionUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A component that holds onto data associated with a template's header.
 */
@RequiresCarApi(5)
@CarProtocol
@KeepFields
public final class Header {
    private final @NonNull List<Action> mEndHeaderActions;
    private final @Nullable Action mStartHeaderAction;
    private final @Nullable CarText mTitle;

    Header(Builder builder) {
        mTitle = builder.mTitle;
        mStartHeaderAction = builder.mStartHeaderAction;
        mEndHeaderActions = CollectionUtils.unmodifiableCopy(builder.mEndHeaderActions);
    }

    /** Constructs an empty instance, used by serialization code. */
    private Header() {
        mTitle = null;
        mStartHeaderAction = null;
        mEndHeaderActions = new ArrayList<>();
    }

    /**
     * Returns the title of the component or {@code null} if not set.
     *
     * @see Builder#setTitle(CharSequence)
     */
    public @Nullable CarText getTitle() {
        return mTitle;
    }

    /**
     * Returns the {@link Action}s that are set to be displayed at the end of the header,
     * or {@code null} if not set.
     *
     * @see Builder#addEndHeaderAction(Action)
     */
    public @NonNull List<Action> getEndHeaderActions() {
        return mEndHeaderActions;
    }

    /**
     * Returns the {@link Action} that is set to be displayed at the start of the header,
     * or {@code null} if not set.
     *
     * @see Builder#setStartHeaderAction(Action)
     */
    public @Nullable Action getStartHeaderAction() {
        return mStartHeaderAction;
    }

    @Override
    public @NonNull String toString() {
        return "Header: " + mTitle;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTitle, mEndHeaderActions, mStartHeaderAction);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Header)) {
            return false;
        }
        Header otherComponent = (Header) other;

        return Objects.equals(mTitle, otherComponent.mTitle)
                && Objects.equals(mEndHeaderActions, otherComponent.mEndHeaderActions)
                && Objects.equals(mStartHeaderAction, otherComponent.mStartHeaderAction);
    }

    /** A builder of {@link Header}. */
    public static final class Builder {
        final List<Action> mEndHeaderActions = new ArrayList<>();
        @Nullable Action mStartHeaderAction;
        @Nullable CarText mTitle;

        /**
         * Adds an {@link Action} that will be displayed at the end of a header.
         *
         * <p>Note: End header action will show up differently inside and outside of map-based
         * templates.</p>
         * <ul>
         *  <li>In a Non-map screen (eg. {@link MessageTemplate}), actions appear as is, just as the
         *      app provides. A background color is allowed on the primary action.
         *  <li>In a Map-based screen (eg. Setting Header in the ContentTemplate in a
         *      {@link MapWithContentTemplate}) only actions with custom icons or standard actions
         *      will appear in the Header. The label will be stripped off each action if an app
         *      still provides that. Any tint on the icon will be disabled and default to neutral
         *      token. The background color on the primary action would also be removed.
         * </ul>
         */
        public @NonNull Builder addEndHeaderAction(@NonNull Action headerAction) {
            mEndHeaderActions.add(requireNonNull(headerAction));
            return this;
        }

        /**
         * Sets the {@link Action} that will be displayed at the start of a header.
         *
         * <p>By default, a header will not have a start action.
         *
         * <h4>Requirements</h4>
         *
         * Only one of {@link Action#APP_ICON} or {@link Action#BACK} is supported as a start header
         * {@link Action}.
         *
         * @throws IllegalArgumentException if {@code headerAction} does not meet the requirements
         * @throws NullPointerException     if {@code headerAction} is {@code null}
         */
        public @NonNull Builder setStartHeaderAction(@NonNull Action headerAction) {
            ACTIONS_CONSTRAINTS_HEADER.validateOrThrow(
                    Collections.singletonList(requireNonNull(headerAction)));
            mStartHeaderAction = headerAction;
            return this;
        }

        /**
         * Sets the title of the component.
         *
         * <p>Only {@link DistanceSpan}s and {@link DurationSpan}s are supported in the input
         * string.
         *
         * @throws NullPointerException     if {@code title} is null
         * @throws IllegalArgumentException if {@code title} contains unsupported spans
         * @see CarText
         */
        public @NonNull Builder setTitle(@NonNull CharSequence title) {
            return setTitle(CarText.create(title));
        }

        /**
         * Sets the title of the component, with support for multiple length variants.
         *
         * <p>Only {@link DistanceSpan}s and {@link DurationSpan}s are supported in the input
         * string.
         *
         * @throws NullPointerException     if {@code title} is null
         * @throws IllegalArgumentException if {@code title} contains unsupported spans
         * @see CarText
         */
        public @NonNull Builder setTitle(@NonNull CarText title) {
            mTitle = requireNonNull(title);
            CarTextConstraints.TEXT_ONLY.validateOrThrow(mTitle);
            return this;
        }

        /**
         * Constructs the component defined by this builder.
         *
         * <h4>Requirements</h4>
         *
         * <p>Either a start header {@link Action} or title must be set on the component.
         *
         * @throws IllegalStateException    if neither a title nor start header {@link Action} is
         *                                  set
         * @throws IllegalArgumentException if {@link Action}s towards the end of the header do not
         *                                  meet the template's requirements
         */
        public @NonNull Header build() {
            if (CarText.isNullOrEmpty(mTitle) && mStartHeaderAction == null) {
                throw new IllegalStateException("Either the title or start header action must be "
                        + "set");
            }

            return new Header(this);
        }
    }
}
