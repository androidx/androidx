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
import static androidx.car.app.model.constraints.RowListConstraints.ROW_LIST_CONSTRAINTS_PANE;

import static java.util.Objects.requireNonNull;

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.constraints.CarTextConstraints;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Objects;

/**
 * A template that displays a {@link Pane}.
 *
 * <h4>Template Restrictions</h4>
 *
 * In regards to template refreshes, as described in
 * {@link androidx.car.app.Screen#onGetTemplate()}, this template is considered a refresh of a
 * previous one if:
 *
 * <ul>
 *   <li>The previous template is in a loading state (see {@link Pane.Builder#setLoading}, or
 *   <li>The template title has not changed, and the number of rows and the title (not counting
 *       spans) of each row between the previous and new {@link Pane}s have not changed.
 * </ul>
 */
@CarProtocol
@KeepFields
public final class PaneTemplate implements Template {

    /**
     * @deprecated use {@link Header.Builder#setTitle(CarText)}; mHeader replaces the need
     * for this field.
     */
    @Deprecated
    private final @Nullable CarText mTitle;
    private final @Nullable Pane mPane;
    /**
     * @deprecated use {@link Header.Builder#setStartHeaderAction(Action)}; mHeader replaces the
     * need for this field.
     */
    @Deprecated
    private final @Nullable Action mHeaderAction;
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
     * Returns the title of the template or {@code null} if not set.
     *
     * @see Builder#setTitle(CharSequence)
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
     * @see Builder#setHeaderAction(Action)
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
     * @see Builder#setActionStrip(ActionStrip)
     *
     * @deprecated use {@link Header#getEndHeaderActions()} instead.
     */
    @Deprecated
    public @Nullable ActionStrip getActionStrip() {
        return mActionStrip;
    }

    /**
     * Returns the {@link Pane} to display in the template.
     *
     * @see Builder#Builder(Pane)
     */
    public @NonNull Pane getPane() {
        return requireNonNull(mPane);
    }

    /**
     * Returns the {@link Header} to display in this template.
     *
     * <p>This method was introduced in API 7, but is backwards compatible even if the client is
     * using API 6 or below. </p>
     *
     * @see PaneTemplate.Builder#setHeader(Header)
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
        return "PaneTemplate";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTitle, mPane, mHeaderAction, mActionStrip, mHeader);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PaneTemplate)) {
            return false;
        }
        PaneTemplate otherTemplate = (PaneTemplate) other;

        return Objects.equals(mTitle, otherTemplate.mTitle)
                && Objects.equals(mPane, otherTemplate.mPane)
                && Objects.equals(mHeaderAction, otherTemplate.mHeaderAction)
                && Objects.equals(mActionStrip, otherTemplate.mActionStrip)
                && Objects.equals(mHeader, otherTemplate.mHeader);
    }

    PaneTemplate(Builder builder) {
        mTitle = builder.mTitle;
        mPane = builder.mPane;
        mHeaderAction = builder.mHeaderAction;
        mActionStrip = builder.mActionStrip;
        mHeader = builder.mHeader;
    }

    /** Constructs an empty instance, used by serialization code. */
    private PaneTemplate() {
        mTitle = null;
        mPane = null;
        mHeaderAction = null;
        mActionStrip = null;
        mHeader = null;
    }

    /** A builder of {@link PaneTemplate}. */
    public static final class Builder {
        @Nullable CarText mTitle;
        Pane mPane;
        @Nullable Action mHeaderAction;
        @Nullable ActionStrip mActionStrip;
        @Nullable Header mHeader;

        /**
         * Sets the title of the template.
         *
         * <p>Unless set with this method, the template will not have a title.
         *
         * <p>Only {@link DistanceSpan}s, {@link DurationSpan}s and {@link CarIconSpan} are
         * supported in the input string.
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
            CarTextConstraints.TEXT_AND_ICON.validateOrThrow(mTitle);
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
         * Sets the {@link ActionStrip} for this template.
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
        public @NonNull Builder setActionStrip(@NonNull ActionStrip actionStrip) {
            ACTIONS_CONSTRAINTS_SIMPLE.validateOrThrow(requireNonNull(actionStrip).getActions());
            mActionStrip = actionStrip;
            return this;
        }

        /**
         * Constructs the template defined by this builder.
         *
         * <h4>Requirements</h4>
         *
         * The number of items in the {@link ItemList} should be smaller or equal than the limit
         * provided by
         * {@link androidx.car.app.constraints.ConstraintManager#CONTENT_LIMIT_TYPE_PANE}. The host
         * will ignore any rows over that limit. Each {@link Row}s can add up to 2 lines of texts
         * via {@link Row.Builder#addText} and cannot contain either a {@link Toggle} or a {@link
         * OnClickListener}.
         *
         * <p>Up to 2 {@link Action}s are allowed in the {@link Pane}. Each action's title color
         * can be customized with {@link ForegroundCarColorSpan} instances. Any other span is not
         * supported.
         *
         * <p>If none of the header {@link Action}, the header title or the action strip have been
         * set on the template, the header is hidden.
         *
         * @throws IllegalArgumentException if the {@link Pane} does not meet the requirements
         * @see androidx.car.app.constraints.ConstraintManager#getContentLimit(int)
         */
        public @NonNull PaneTemplate build() {
            ROW_LIST_CONSTRAINTS_PANE.validateOrThrow(mPane);
            ACTIONS_CONSTRAINTS_BODY_WITH_PRIMARY_ACTION.validateOrThrow(mPane.getActions());

            return new PaneTemplate(this);
        }

        /**
         * Returns a new instance of a @link Builder}.
         *
         * @throws NullPointerException if {@code pane} is {@code null}
         */
        public Builder(@NonNull Pane pane) {
            mPane = requireNonNull(pane);
        }
    }
}
