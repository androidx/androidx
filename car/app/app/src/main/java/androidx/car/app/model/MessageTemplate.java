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

import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_HEADER;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.model.constraints.CarIconConstraints;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A template for displaying a message and associated actions.
 *
 * <h4>Template Restrictions</h4>
 *
 * In regards to template refreshes, as described in
 * {@link androidx.car.app.Screen#onGetTemplate()}, this template is
 * considered a refresh of a previous one if the title and messages have not changed.
 */
public final class MessageTemplate implements Template {
    @Keep
    @Nullable
    private final CarText mTitle;
    @Keep
    @Nullable
    private final CarText mMessage;
    @Keep
    @Nullable
    private final CarText mDebugMessage;
    @Keep
    @Nullable
    private final CarIcon mIcon;
    @Keep
    @Nullable
    private final Action mHeaderAction;
    @Keep
    @Nullable
    private final ActionList mActionList;

    /** Constructs a new builder of {@link MessageTemplate}. */
    @NonNull
    public static Builder builder(@NonNull CharSequence message) {
        return new Builder(requireNonNull(message));
    }

    @Nullable
    public CarText getTitle() {
        return mTitle;
    }

    @Nullable
    public Action getHeaderAction() {
        return mHeaderAction;
    }

    @NonNull
    public CarText getMessage() {
        return Objects.requireNonNull(mMessage);
    }

    @Nullable
    public CarText getDebugMessage() {
        return mDebugMessage;
    }

    @Nullable
    public CarIcon getIcon() {
        return mIcon;
    }

    @Nullable
    public ActionList getActionList() {
        return mActionList;
    }

    @NonNull
    @Override
    public String toString() {
        return "MessageTemplate";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTitle, mMessage, mDebugMessage, mHeaderAction, mActionList, mIcon);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MessageTemplate)) {
            return false;
        }
        MessageTemplate otherTemplate = (MessageTemplate) other;

        return Objects.equals(mTitle, otherTemplate.mTitle)
                && Objects.equals(mMessage, otherTemplate.mMessage)
                && Objects.equals(mDebugMessage, otherTemplate.mDebugMessage)
                && Objects.equals(mHeaderAction, otherTemplate.mHeaderAction)
                && Objects.equals(mActionList, otherTemplate.mActionList)
                && Objects.equals(mIcon, otherTemplate.mIcon);
    }

    private MessageTemplate(Builder builder) {
        mTitle = builder.mTitle;
        mMessage = builder.mMessage;
        mDebugMessage = builder.mDebugMessage;
        mIcon = builder.mIcon;
        mHeaderAction = builder.mHeaderAction;
        mActionList = builder.mActionList;
    }

    /** Constructs an empty instance, used by serialization code. */
    private MessageTemplate() {
        mTitle = null;
        mMessage = null;
        mDebugMessage = null;
        mIcon = null;
        mHeaderAction = null;
        mActionList = null;
    }

    /** A builder of {@link MessageTemplate}. */
    public static final class Builder {
        @Nullable
        private CarText mTitle;
        private CarText mMessage;
        @Nullable
        private CarText mDebugMessage;
        @Nullable
        private CarIcon mIcon;
        @Nullable
        private Action mHeaderAction;
        @Nullable
        private ActionList mActionList;
        @Nullable
        private Throwable mDebugCause;
        @Nullable
        private String mDebugString;

        /**
         * Sets the {@link CharSequence} to show as the template's title, or {@code null} to not
         * show a
         * title.
         */
        @NonNull
        public Builder setTitle(@Nullable CharSequence title) {
            this.mTitle = title == null ? null : CarText.create(title);
            return this;
        }

        /**
         * Sets the {@link CharSequence} to display as the message in the template.
         *
         * @throws NullPointerException if {@code message} is null.
         */
        @NonNull
        public Builder setMessage(@NonNull CharSequence message) {
            this.mMessage = CarText.create(requireNonNull(message));
            return this;
        }

        /**
         * Sets a {@link Throwable} for debugging purposes, or {@code null} to not show it.
         *
         * <p>The cause will be displayed along with the message set in {@link #setDebugMessage}.
         *
         * <p>The host may choose to not display this debugging information if it doesn't deem it
         * appropriate, for example, when running on a production environment rather than in a
         * simulator
         * such as the Desktop Head Unit.
         */
        @NonNull
        // Suppress as the cause is transformed into a message before transport.
        @SuppressLint("MissingGetterMatchingBuilder")
        public Builder setDebugCause(@Nullable Throwable cause) {
            this.mDebugCause = cause;
            return this;
        }

        /**
         * Sets a debug message for debugging purposes, or {@code null} to not show a debug message.
         *
         * <p>The debug message will be displayed along with the cause set in
         * {@link #setDebugCause}.
         *
         * <p>The host may choose to not display this debugging information if it doesn't deem it
         * appropriate, for example, when running on a production environment rather than in a
         * simulator
         * such as the Desktop Head Unit.
         */
        @NonNull
        public Builder setDebugMessage(@Nullable String debugMessage) {
            this.mDebugString = debugMessage;
            return this;
        }

        /**
         * Sets the icon to be displayed along with the message, or {@code null} to not display any
         * icons.
         *
         * <h4>Icon Sizing Guidance</h4>
         *
         * The provided icon should have a maximum size of 64 x 64 dp. If the icon exceeds this
         * maximum
         * size in either one of the dimensions, it will be scaled down and centered inside the
         * bounding
         * box while preserving the aspect ratio.
         *
         * <p>See {@link CarIcon} for more details related to providing icon and image resources
         * that
         * work with different car screen pixel densities.
         */
        @NonNull
        public Builder setIcon(@Nullable CarIcon icon) {
            CarIconConstraints.DEFAULT.validateOrThrow(icon);
            this.mIcon = icon;
            return this;
        }

        /**
         * Sets the {@link Action} that will be displayed in the header of the template, or
         * {@code null}
         * to not display an action.
         *
         * <h4>Requirements</h4>
         *
         * This template only supports either either one of {@link Action#APP_ICON} and {@link
         * Action#BACK} as a header {@link Action}.
         *
         * @throws IllegalArgumentException if {@code headerAction} does not meet the template's
         *                                  requirements.
         */
        @NonNull
        public Builder setHeaderAction(@Nullable Action headerAction) {
            ACTIONS_CONSTRAINTS_HEADER.validateOrThrow(
                    headerAction == null ? Collections.emptyList()
                            : Collections.singletonList(headerAction));
            this.mHeaderAction = headerAction;
            return this;
        }

        /**
         * Sets a list of {@link Action}s to display along with the message.
         *
         * <p>Any actions above the maximum limit of 2 will be ignored.
         *
         * @throws NullPointerException if {@code actions} is {@code null}.
         */
        @NonNull
        // TODO(shiufai): consider rename to match getter's name (e.g. setActionList or getActions).
        @SuppressLint("MissingGetterMatchingBuilder")
        public Builder setActions(@NonNull List<Action> actions) {
            mActionList = ActionList.create(requireNonNull(actions));
            return this;
        }

        /**
         * Constructs the {@link MessageTemplate} defined by this builder.
         *
         * <h4>Requirements</h4>
         *
         * A non-empty message must be set on the template with {@link
         * Builder#setMessage(CharSequence)}.
         *
         * <p>Either a header {@link Action} or title must be set on the template.
         *
         * @throws IllegalStateException if the message is empty.
         * @throws IllegalStateException if the template does not have either a title or header
         *                               {@link
         *                               Action} set.
         */
        @NonNull
        public MessageTemplate build() {
            if (mMessage.isEmpty()) {
                throw new IllegalStateException("Message cannot be empty");
            }

            String debugString = this.mDebugString == null ? "" : this.mDebugString;
            if (!debugString.isEmpty() && mDebugCause != null) {
                debugString += "\n";
            }
            debugString += Log.getStackTraceString(mDebugCause);
            if (!debugString.isEmpty()) {
                mDebugMessage = CarText.create(debugString);
            }

            if (CarText.isNullOrEmpty(mTitle) && mHeaderAction == null) {
                throw new IllegalStateException("Either the title or header action must be set");
            }

            return new MessageTemplate(this);
        }

        private Builder(CharSequence message) {
            this.mMessage = CarText.create(message);
        }
    }
}
