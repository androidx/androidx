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

import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_BODY_WITH_PRIMARY_ACTION;
import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_HEADER;
import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_SIMPLE;

import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

import android.util.Log;

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.constraints.CarIconConstraints;
import androidx.car.app.model.constraints.CarTextConstraints;
import androidx.car.app.utils.CollectionUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
@KeepFields
public final class MessageTemplate implements Template {
    private final boolean mIsLoading;
    /**
     * @deprecated use {@link Header.Builder#setTitle(CarText)}; mHeader replaces the need
     * for this field.
     */
    @Deprecated
    private final @Nullable CarText mTitle;
    private final @Nullable CarText mMessage;
    private final @Nullable CarText mDebugMessage;
    private final @Nullable CarIcon mIcon;
    /**
     * @deprecated use {@link Header.Builder#setStartHeaderAction(Action)}; mHeader replaces the
     * need for this field.
     */
    @Deprecated
    private final @Nullable Action mHeaderAction;
    private final List<Action> mActionList;
    /**
     * @deprecated use {@link Header.Builder#addEndHeaderAction(Action)} for each action; mHeader
     * replaces the need for this field.
     */
    @Deprecated
    private final @Nullable ActionStrip mActionStrip;

    /**
     * Represents a Header object to set the startHeaderAction, the title and the endHeaderActions
     *
     * @see MessageTemplate.Builder#setHeader(Header)
     */
    @RequiresCarApi(7)
    private final @Nullable Header mHeader;

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
     * @deprecated use {@link Header#getTitle()} instead.
     */
    @Deprecated
    public @Nullable CarText getTitle() {
        return mTitle;
    }

    /**
     * Returns the {@link Action} that is set to be displayed in the header of the template, or
     * {@code null} if not set.
     *
     * @deprecated use {@link Header#getStartHeaderAction()} instead.
     */
    @Deprecated
    public @Nullable Action getHeaderAction() {
        return mHeaderAction;
    }

    /**
     * Returns the {@link ActionStrip} for this template or {@code null} if not set.
     *
     * @deprecated use {@link Header#getEndHeaderActions()} instead.
     */
    @Deprecated
    @RequiresCarApi(2)
    public @Nullable ActionStrip getActionStrip() {
        return mActionStrip;
    }

    /**
     * Returns the message to display in the template.
     *
     * @see Builder#Builder(CharSequence)
     */
    public @NonNull CarText getMessage() {
        return requireNonNull(mMessage);
    }

    /**
     * Returns a debug message to display in the template or {@code null} if not set.
     *
     * @see Builder#setDebugMessage(Throwable)
     * @see Builder#setDebugMessage(String)
     */
    public @Nullable CarText getDebugMessage() {
        return mDebugMessage;
    }

    /**
     * Returns the icon to display in the template or {@code null} if not set.
     *
     * @see Builder#setIcon(CarIcon)
     */
    public @Nullable CarIcon getIcon() {
        return mIcon;
    }

    /**
     * Returns the list of actions to display in the template.
     *
     * @see Builder#addAction(Action)
     */
    public @NonNull List<Action> getActions() {
        return CollectionUtils.emptyIfNull(mActionList);
    }

    /**
     * Returns the {@link Header} to display in this template.
     *
     * <p>This method was introduced in API 7, but is backwards compatible even if the client is
     * using API 6 or below. </p>
     *
     * @see MessageTemplate.Builder#setHeader(Header)
     */
    public @Nullable Header getHeader() {
        if (mHeader != null) {
            return mHeader;
        }
        if (mTitle == null && mHeaderAction == null && mActionStrip == null) {
            return null;
        }
        Header.Builder headerBuilder = new Header.Builder();
        if (mTitle != null) {
            headerBuilder.setTitle(mTitle);
        }
        if (mHeaderAction != null) {
            headerBuilder.setStartHeaderAction(mHeaderAction);
        }
        if (mActionStrip != null) {
            for (Action action: mActionStrip.getActions()) {
                headerBuilder.addEndHeaderAction(action);
            }
        }
        return headerBuilder.build();
    }

    @Override
    public @NonNull String toString() {
        return "MessageTemplate";
    }

    @Override
    public int hashCode() {
        return hash(mIsLoading, mTitle, mMessage, mDebugMessage, mHeaderAction, mActionList, mIcon,
                mActionStrip, mHeader);
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
                && Objects.equals(mIcon, otherTemplate.mIcon)
                && Objects.equals(mActionStrip, otherTemplate.mActionStrip)
                && Objects.equals(mHeader, otherTemplate.mHeader);
    }

    MessageTemplate(Builder builder) {
        mIsLoading = builder.mIsLoading;
        mTitle = builder.mTitle;
        mMessage = builder.mMessage;
        mDebugMessage = builder.mDebugMessage;
        mIcon = builder.mIcon;
        mHeaderAction = builder.mHeaderAction;
        mActionStrip = builder.mActionStrip;
        mActionList = CollectionUtils.unmodifiableCopy(builder.mActionList);
        mHeader = builder.mHeader;
    }

    /** Constructs an empty instance, used by serialization code. */
    private MessageTemplate() {
        mIsLoading = false;
        mTitle = null;
        mMessage = null;
        mDebugMessage = null;
        mIcon = null;
        mHeaderAction = null;
        mActionStrip = null;
        mActionList = Collections.emptyList();
        mHeader = null;
    }

    /** A builder of {@link MessageTemplate}. */
    public static final class Builder {
        boolean mIsLoading;
        @Nullable CarText mTitle;
        CarText mMessage;
        @Nullable CarText mDebugMessage;
        @Nullable CarIcon mIcon;
        @Nullable Action mHeaderAction;
        @Nullable ActionStrip mActionStrip;
        List<Action> mActionList = new ArrayList<>();
        @Nullable Throwable mDebugCause;
        @Nullable String mDebugString;
        @Nullable Header mHeader;

        /**
         * Sets whether the template is in a loading state.
         *
         * <p>If set to {@code true}, the UI shows a loading indicator where the icon
         * would be otherwise. The caller is expected to call
         * {@link androidx.car.app.Screen#invalidate()} and send the new template content to the
         * host once the data is ready.
         */
        @RequiresCarApi(2)
        public @NonNull Builder setLoading(boolean isLoading) {
            mIsLoading = isLoading;
            return this;
        }

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
         *
         * @deprecated Use {@link Header.Builder#setTitle(CarText)}
         */
        @Deprecated
        public @NonNull Builder setTitle(@NonNull CharSequence title) {
            mTitle = CarText.create(requireNonNull(title));
            CarTextConstraints.TEXT_ONLY.validateOrThrow(mTitle);
            return this;
        }


        /**
         * Sets the {@link Header} for this template.
         *
         * <p>The end header actions will show up differently inside vs outside of a map template.
         * See {@link Header.Builder#addEndHeaderAction} for more details.</p>
         *
         * @throws NullPointerException if {@code header} is null
         */
        @RequiresCarApi(7)
        public @NonNull Builder setHeader(@NonNull Header header) {
            if (header.getStartHeaderAction() != null) {
                mHeaderAction = header.getStartHeaderAction();
            }
            if (header.getTitle() != null) {
                mTitle = header.getTitle();
            }
            if (!header.getEndHeaderActions().isEmpty()) {
                ActionStrip.Builder actionStripBuilder = new ActionStrip.Builder();
                for (Action action: header.getEndHeaderActions()) {
                    actionStripBuilder.addAction(action);
                }
                mActionStrip = actionStripBuilder.build();
            }
            mHeader = header;
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
        public @NonNull Builder setDebugMessage(@NonNull Throwable cause) {
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
        public @NonNull Builder setDebugMessage(@NonNull String debugMessage) {
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
        public @NonNull Builder setIcon(@NonNull CarIcon icon) {
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
         *
         * @deprecated Use {@link Header.Builder#setStartHeaderAction(Action)}
         */
        @Deprecated
        public @NonNull Builder setHeaderAction(@NonNull Action headerAction) {
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
         *
         * @deprecated Use {@link Header.Builder#addEndHeaderAction(Action) for each action}
         */
        @Deprecated
        @RequiresCarApi(2)
        public @NonNull Builder setActionStrip(@NonNull ActionStrip actionStrip) {
            ACTIONS_CONSTRAINTS_SIMPLE.validateOrThrow(requireNonNull(actionStrip).getActions());
            mActionStrip = actionStrip;
            return this;
        }

        /**
         * Adds an {@link Action} to display along with the message.
         *
         * <h4>Requirements</h4>
         *
         * This template allows up to 2 {@link Action}s in its body. Each action's title color
         * can be customized with {@link ForegroundCarColorSpan} instances. Any other span is not
         * supported.
         *
         * @throws NullPointerException     if {@code action} is {@code null}
         * @throws IllegalArgumentException if {@code action} does not meet the requirements
         */
        public @NonNull Builder addAction(@NonNull Action action) {
            mActionList.add(requireNonNull(action));
            ACTIONS_CONSTRAINTS_BODY_WITH_PRIMARY_ACTION.validateOrThrow(mActionList);
            return this;
        }

        /**
         * Constructs the {@link MessageTemplate} defined by this builder.
         *
         * <h4>Requirements</h4>
         *
         * A non-empty message must be set on the template.
         *
         * <p>If none of the header {@link Action}, the header title or the action strip have been
         * set on the template, the header is hidden.
         *
         * @throws IllegalStateException if the message is empty, or if the
         *                               template is in loading state and an icon is specified.
         */
        public @NonNull MessageTemplate build() {
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
