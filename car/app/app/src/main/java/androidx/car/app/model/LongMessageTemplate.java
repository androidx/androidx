/*
 * Copyright 2021 The Android Open Source Project
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

import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_BODY_WITH_PRIMARY_ACTION;
import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_HEADER;
import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_SIMPLE;

import static java.util.Objects.requireNonNull;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.Screen;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.constraints.CarTextConstraints;
import androidx.car.app.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A template for displaying a long text, which could be several paragraphs long, with associated
 * actions.
 *
 * <h4>Template Restrictions</h4>
 *
 * This template's body is only available to the user while the car is parked and does not count
 * against the template quota.
 *
 * @see Screen#onGetTemplate()
 */
@RequiresCarApi(2)
public final class LongMessageTemplate implements Template {
    @Keep
    @Nullable
    private final CarText mTitle;
    @Keep
    @Nullable
    private final CarText mMessage;
    @Keep
    @Nullable
    private final Action mHeaderAction;
    @Keep
    private final List<Action> mActionList;
    @Keep
    @Nullable
    private final ActionStrip mActionStrip;

    /**
     * Returns the title of the template or {@code null} if not set.
     *
     * @see Builder#setTitle(CharSequence)
     */
    @Nullable
    public CarText getTitle() {
        return mTitle;
    }

    /**
     * Returns the {@link Action} that is set to be displayed in the header of the template, or
     * {@code null} if not set.
     *
     * @see Builder#setHeaderAction(Action)
     */
    @Nullable
    public Action getHeaderAction() {
        return mHeaderAction;
    }

    /**
     * Returns the {@link ActionStrip} for this template or {@code null} if not set.
     *
     * @see Builder#setActionStrip(ActionStrip)
     */
    @Nullable
    public ActionStrip getActionStrip() {
        return mActionStrip;
    }

    /**
     * Returns the message to display in the template, which could be several paragraphs long.
     *
     * @see Builder#Builder(CharSequence)
     */
    @NonNull
    public CarText getMessage() {
        return requireNonNull(mMessage);
    }

    /**
     * Returns the list of actions to display in the template.
     *
     * @see Builder#addAction(Action)
     */
    @NonNull
    public List<Action> getActions() {
        return CollectionUtils.emptyIfNull(mActionList);
    }

    @NonNull
    @Override
    public String toString() {
        return "LongMessageTemplate";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTitle, mMessage, mHeaderAction, mActionList, mActionStrip);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LongMessageTemplate)) {
            return false;
        }
        LongMessageTemplate otherTemplate = (LongMessageTemplate) other;

        return Objects.equals(mTitle, otherTemplate.mTitle)
                && Objects.equals(mMessage, otherTemplate.mMessage)
                && Objects.equals(mHeaderAction, otherTemplate.mHeaderAction)
                && Objects.equals(mActionList, otherTemplate.mActionList)
                && Objects.equals(mActionStrip, otherTemplate.mActionStrip);
    }

    LongMessageTemplate(Builder builder) {
        mTitle = builder.mTitle;
        mMessage = builder.mMessage;
        mActionStrip = builder.mActionStrip;
        mHeaderAction = builder.mHeaderAction;
        mActionList = CollectionUtils.unmodifiableCopy(builder.mActionList);
    }

    /** Constructs an empty instance, used by serialization code. */
    private LongMessageTemplate() {
        mTitle = null;
        mMessage = null;
        mActionStrip = null;
        mHeaderAction = null;
        mActionList = Collections.emptyList();
    }

    /** A builder of {@link LongMessageTemplate}. */
    @RequiresCarApi(2)
    public static final class Builder {
        @Nullable
        CarText mTitle;
        final CarText mMessage;
        @Nullable
        ActionStrip mActionStrip;
        @Nullable
        Action mHeaderAction;
        List<Action> mActionList = new ArrayList<>();

        /**
         * Sets the title of the template.
         *
         * <p>Unless set with this method, the template will not have a title.
         *
         * <p>Only {@link DistanceSpan}s and {@link DurationSpan}s are supported in the input
         * string.
         *
         * @throws NullPointerException     if {@code title} is {@code null}
         * @throws IllegalArgumentException if {@code title} contains unsupported spans
         * @see CarText
         */
        @NonNull
        public Builder setTitle(@NonNull CharSequence title) {
            mTitle = CarText.create(requireNonNull(title));
            CarTextConstraints.TEXT_ONLY.validateOrThrow(mTitle);
            return this;
        }

        /**
         * Sets the {@link Action} that will be displayed in the header of the template.
         *
         * <p>Unless set with this method, the template will not have a header action.
         *
         * <h4>Requirements</h4>
         *
         * This template only supports either one of {@link Action#APP_ICON} and
         * {@link Action#BACK} as a header {@link Action}.
         *
         * @throws IllegalArgumentException if {@code headerAction} does not meet the template's
         *                                  requirements
         * @throws NullPointerException     if {@code headerAction} is {@code null}
         */
        @NonNull
        public Builder setHeaderAction(@NonNull Action headerAction) {
            ACTIONS_CONSTRAINTS_HEADER.validateOrThrow(
                    Collections.singletonList(requireNonNull(headerAction)));
            mHeaderAction = headerAction;
            return this;
        }

        /**
         * Sets the {@link ActionStrip} for this template or {@code null} to not display an {@link
         * ActionStrip}.
         *
         * <p>Unless set with this method, the template will not have an action strip.
         *
         * <h4>Requirements</h4>
         *
         * This template allows up to 2 {@link Action}s in its {@link ActionStrip}. Of the 2 allowed
         * {@link Action}s, one of them can contain a title as set via
         * {@link Action.Builder#setTitle}. Otherwise, only {@link Action}s with icons are allowed.
         *
         * @throws IllegalArgumentException if {@code actionStrip} does not meet the requirements
         * @throws NullPointerException     if {@code actionStrip} is {@code null}
         */
        @NonNull
        public Builder setActionStrip(@NonNull ActionStrip actionStrip) {
            ACTIONS_CONSTRAINTS_SIMPLE.validateOrThrow(requireNonNull(actionStrip).getActions());
            mActionStrip = actionStrip;
            return this;
        }

        /**
         * Adds an {@link Action} to display along with the message.
         *
         * <h4>Requirements</h4>
         *
         * This template allows up to 2 {@link Action}s in its body, and they must use a
         * {@link androidx.car.app.model.ParkedOnlyOnClickListener}. One of these actions can be
         * declared as primary via {@link Action.Builder#setFlags}.
         *
         * Each action's title color can be customized with {@link ForegroundCarColorSpan}
         * instances. Any other span is not supported.
         *
         * @throws IllegalArgumentException if {@code action} does not meet the requirements
         * @throws NullPointerException     if {@code action} is {@code null}
         */
        @NonNull
        public Builder addAction(@NonNull Action action) {
            requireNonNull(action);
            if (!requireNonNull(action.getOnClickDelegate()).isParkedOnly()) {
                throw new IllegalArgumentException("The action must use a "
                        + "ParkedOnlyOnClickListener");
            }

            mActionList.add(action);
            ACTIONS_CONSTRAINTS_BODY_WITH_PRIMARY_ACTION.validateOrThrow(mActionList);
            return this;
        }

        /**
         * Constructs the {@link LongMessageTemplate} defined by this builder.
         *
         * <h4>Requirements</h4>
         *
         * A non-empty message must be set on the template.
         *
         * <p>Either a header {@link Action} or title must be set on the template.
         *
         * @throws IllegalStateException if the message is empty, or if the template does not have
         *                               either a title or header {@link Action} set
         */
        @NonNull
        public LongMessageTemplate build() {
            if (mMessage.isEmpty()) {
                throw new IllegalStateException("Message cannot be empty");
            }

            if (CarText.isNullOrEmpty(mTitle) && mHeaderAction == null) {
                throw new IllegalStateException("Either the title or header action must be set");
            }

            return new LongMessageTemplate(this);
        }

        /**
         * Returns a {@link Builder} instance.
         *
         * @param message the text message to display in the template. This message will only be
         *                displayed when the car is parked.
         * @throws NullPointerException if the {@code message} is {@code null}
         */
        public Builder(@NonNull CharSequence message) {
            mMessage = CarText.create(requireNonNull(message));
        }
    }
}
