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

import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_BODY;
import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_HEADER;

import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

import android.util.Log;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.constraints.CarIconConstraints;
import androidx.car.app.utils.CollectionUtils;

import java.util.ArrayList;
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
@CarProtocol
public final class MessageTemplate implements Template {
    @Keep
    private final boolean mIsLoading;
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
    private final List<Action> mActionList;

    /**
     * Returns whether the template is loading.
     *
     * @see Builder#setLoading(boolean)
     */
    @RequiresCarApi(2)
    public boolean isLoading() {
        return mIsLoading;
    }

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
     * Returns the message to display in the template.
     *
     * @see Builder#Builder(CharSequence)
     */
    @NonNull
    public CarText getMessage() {
        return requireNonNull(mMessage);
    }

    /**
     * Returns a debug message to display in the template or {@code null} if not set.
     *
     * @see Builder#setDebugMessage(Throwable)
     * @see Builder#setDebugMessage(String)
     */
    @Nullable
    public CarText getDebugMessage() {
        return mDebugMessage;
    }

    /**
     * Returns the icon to display in the template or {@code null} if not set.
     *
     * @see Builder#setIcon(CarIcon)
     */
    @Nullable
    public CarIcon getIcon() {
        return mIcon;
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
        return "MessageTemplate";
    }

    @Override
    public int hashCode() {
        return hash(mIsLoading, mTitle, mMessage, mDebugMessage, mHeaderAction, mActionList, mIcon);
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

        return mIsLoading == otherTemplate.mIsLoading
                && Objects.equals(mTitle, otherTemplate.mTitle)
                && Objects.equals(mMessage, otherTemplate.mMessage)
                && Objects.equals(mDebugMessage, otherTemplate.mDebugMessage)
                && Objects.equals(mHeaderAction, otherTemplate.mHeaderAction)
                && Objects.equals(mActionList, otherTemplate.mActionList)
                && Objects.equals(mIcon, otherTemplate.mIcon);
    }

    MessageTemplate(Builder builder) {
        mIsLoading = builder.mIsLoading;
        mTitle = builder.mTitle;
        mMessage = builder.mMessage;
        mDebugMessage = builder.mDebugMessage;
        mIcon = builder.mIcon;
        mHeaderAction = builder.mHeaderAction;
        mActionList = CollectionUtils.unmodifiableCopy(builder.mActionList);
    }

    /** Constructs an empty instance, used by serialization code. */
    private MessageTemplate() {
        mIsLoading = false;
        mTitle = null;
        mMessage = null;
        mDebugMessage = null;
        mIcon = null;
        mHeaderAction = null;
        mActionList = Collections.emptyList();
    }

    /** A builder of {@link MessageTemplate}. */
    public static final class Builder {
        boolean mIsLoading;
        @Nullable
        CarText mTitle;
        CarText mMessage;
        @Nullable
        CarText mDebugMessage;
        @Nullable
        CarIcon mIcon;
        @Nullable
        Action mHeaderAction;
        List<Action> mActionList = new ArrayList<>();
        @Nullable
        Throwable mDebugCause;
        @Nullable
        String mDebugString;

        /**
         * Sets whether the template is in a loading state.
         *
         * <p>If set to {@code true}, the UI shows a loading indicator where the icon
         * would be otherwise. The caller is expected to call
         * {@link androidx.car.app.Screen#invalidate()} and send the new template content to the
         * host once the data is ready.
         */
        @RequiresCarApi(2)
        @NonNull
        public Builder setLoading(boolean isLoading) {
            mIsLoading = isLoading;
            return this;
        }

        /**
         * Sets the title of the template.
         *
         * <p>Unless set with this method, the template will not have a title.
         *
         * <p>Spans are not supported in the input string and will be ignored.
         *
         * @throws NullPointerException if {@code title} is {@code null}
         * @see CarText
         */
        @NonNull
        public Builder setTitle(@NonNull CharSequence title) {
            mTitle = CarText.create(requireNonNull(title));
            return this;
        }

        /**
         * Sets a {@link Throwable} for debugging purposes.
         *
         * <p>The cause will be displayed along with the message set in
         * {@link #setDebugMessage(String)}.
         *
         * <p>The host may choose to not display this debugging information if it doesn't deem it
         * appropriate, for example, when running on a production environment rather than in a
         * simulator such as the Desktop Head Unit.
         *
         * @throws NullPointerException if {@code icon} is {@code null}
         */
        @NonNull
        public Builder setDebugMessage(@NonNull Throwable cause) {
            mDebugCause = requireNonNull(cause);
            return this;
        }

        /**
         * Sets a debug message for debugging purposes.
         *
         * <p>The debug message will be displayed along with the cause set in
         * {@link #setDebugMessage}.
         *
         * <p>The host may choose to not display this debugging information if it doesn't deem it
         * appropriate, for example, when running on a production environment rather than in a
         * simulator such as the Desktop Head Unit.
         *
         * @throws NullPointerException if {@code icon} is {@code null}
         */
        @NonNull
        public Builder setDebugMessage(@NonNull String debugMessage) {
            mDebugString = requireNonNull(debugMessage);
            return this;
        }

        /**
         * Sets the icon to be displayed along with the message.
         *
         * <p>Unless set with this method, an icon will not be displayed.
         *
         * <h4>Icon Sizing Guidance</h4>
         *
         * To minimize scaling artifacts across a wide range of car screens, apps should provide
         * icons targeting a 128 x 128 dp bounding box. If the icon exceeds this maximum size in
         * either one of the dimensions, it will be scaled down to be centered inside the
         * bounding box while preserving its aspect ratio.
         *
         * <p>See {@link CarIcon} for more details related to providing icon and image resources
         * that work with different car screen pixel densities.
         *
         * @throws NullPointerException if {@code icon} is {@code null}
         */
        @NonNull
        public Builder setIcon(@NonNull CarIcon icon) {
            CarIconConstraints.DEFAULT.validateOrThrow(requireNonNull(icon));
            mIcon = icon;
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
         * Adds an {@link Action} to display along with the message.
         *
         * <h4>Requirements</h4>
         *
         * This template allows up to 2 {@link Action}s in its body. The action's title color can
         * be customized with {@link ForegroundCarColorSpan} instances, any other spans will be
         * ignored by the host.
         *
         * @throws NullPointerException     if {@code action} is {@code null}
         * @throws IllegalArgumentException if {@code action} does not meet the requirements
         */
        @NonNull
        public Builder addAction(@NonNull Action action) {
            mActionList.add(requireNonNull(action));
            ACTIONS_CONSTRAINTS_BODY.validateOrThrow(mActionList);
            return this;
        }

        /**
         * Constructs the {@link MessageTemplate} defined by this builder.
         *
         * <h4>Requirements</h4>
         *
         * A non-empty message must be set on the template.
         *
         * <p>Either a header {@link Action} or title must be set on the template.
         *
         * @throws IllegalStateException if the message is empty, if the template does not have
         *                               either a title or header {@link Action} set, or if the
         *                               template is in loading state and an icon is specified.
         */
        @NonNull
        public MessageTemplate build() {
            if (mIsLoading && mIcon != null) {
                throw new IllegalStateException(
                        "Template in a loading state can not have an icon");
            }
            if (mMessage.isEmpty()) {
                throw new IllegalStateException("Message cannot be empty");
            }

            String debugString = mDebugString == null ? "" : mDebugString;
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

        /**
         * Returns a {@link Builder} instance.
         *
         * @param message the text message to display in the template
         * @throws NullPointerException if the {@code message} is {@code null}
         */
        public Builder(@NonNull CharSequence message) {
            mMessage = CarText.create(requireNonNull(message));
        }

        /**
         * Returns a {@link Builder} instance.
         *
         * @param message the text message to display in the template
         * @throws NullPointerException if the {@code message} is {@code null}
         */
        public Builder(@NonNull CarText message) {
            mMessage = requireNonNull(message);
        }
    }
}
