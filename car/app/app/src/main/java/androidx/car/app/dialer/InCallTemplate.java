/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.car.app.dialer;

import static androidx.car.app.model.constraints.ActionsConstraints.ACTION_CONSTRAINTS_IN_CALL_CONTENT;
import static androidx.car.app.model.constraints.ActionsConstraints.ACTION_CONSTRAINTS_IN_CALL_HEADER;

import static java.util.Objects.requireNonNull;

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarText;
import androidx.car.app.model.Header;
import androidx.car.app.model.Template;
import androidx.car.app.model.constraints.CarIconConstraints;
import androidx.car.app.model.constraints.CarTextConstraints;
import androidx.car.app.utils.CollectionUtils;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** A {@link Template} showing phone call controls and details. */
@ExperimentalCarApi
@CarProtocol
@KeepFields
public class InCallTemplate implements Template {
    private static final int MAX_TEXTS_SIZE = 2;

    @Nullable private final Header mHeader;
    @Nullable private final CarIcon mIcon;
    @Nullable private final CarText mTitle;
    private final List<CarText> mTexts;
    private final List<Action> mActions;
    private final boolean mIsLoading;

    private InCallTemplate(Builder builder) {
        mHeader = builder.mHeader;
        mIcon = builder.mIcon;
        mTitle = builder.mTitle;
        mTexts = builder.mTexts;
        mActions = CollectionUtils.unmodifiableCopy(builder.mActions);
        mIsLoading = builder.mIsLoading;
    }

    /** Default constructor for deserialization. */
    private InCallTemplate() {
        mHeader = null;
        mIcon = null;
        mTitle = null;
        mTexts = Collections.emptyList();
        mActions = Collections.emptyList();
        mIsLoading = false;
    }

    /**
     * Returns the {@link Header} to show in this template.
     *
     * @see Builder#setHeader(Header)
     */
    @Nullable
    public Header getHeader() {
        return mHeader;
    }

    /**
     * Returns the icon to display in the template.
     *
     * @see Builder#setIcon(CarIcon)
     */
    @Nullable
    public CarIcon getIcon() {
        return mIcon;
    }

    /**
     * Returns the title to display in the template.
     *
     * @see Builder#setTitle(CarText)
     */
    @Nullable
    public CarText getTitle() {
        return mTitle;
    }

    /**
     * Returns the list of text below the title.
     *
     * @see Builder#addText(CarText)
     */
    @NonNull
    public List<CarText> getTexts() {
        return mTexts;
    }

    /**
     * Returns the list of actions to display in the template.
     *
     * @see Builder#addAction(Action)
     */
    @NonNull
    public List<Action> getActions() {
        return mActions;
    }

    /**
     * Returns whether the template is in a loading state.
     *
     * @see Builder#setLoading(boolean)
     */
    public boolean isLoading() {
        return mIsLoading;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InCallTemplate)) {
            return false;
        }
        InCallTemplate that = (InCallTemplate) o;
        return Objects.equals(mHeader, that.mHeader)
                && Objects.equals(mIcon, that.mIcon)
                && Objects.equals(mTitle, that.mTitle)
                && Objects.equals(mTexts, that.mTexts)
                && Objects.equals(mActions, that.mActions)
                && mIsLoading == that.mIsLoading;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mHeader, mIcon, mTitle, mTexts, mActions, mIsLoading);
    }

    @NonNull
    @Override
    public String toString() {
        return "InCallTemplate";
    }

    /** A builder for {@link InCallTemplate}. */
    public static final class Builder {
        private final List<CarText> mTexts = new ArrayList<>();
        private final List<Action> mActions = new ArrayList<>();
        @Nullable private Header mHeader;
        @Nullable private CarIcon mIcon;
        @Nullable private CarText mTitle;
        private boolean mIsLoading;

        /** Returns a {@link InCallTemplate.Builder} instance with no values set by default. */
        public Builder() {}

        /** Returns a {@link InCallTemplate.Builder} instance with values from the template. */
        public Builder(@NonNull InCallTemplate template) {
            this.mTitle = template.getTitle();
            this.mIcon = template.getIcon();
            this.mHeader = template.getHeader();
            this.mTexts.addAll(template.mTexts);
            this.mActions.addAll(template.mActions);
            this.mIsLoading = template.mIsLoading;
        }

        /**
         * Sets the title shown in the template body.
         *
         * <p>Note that {@link #setHeader} also has title which shows above the template body.
         *
         * @throws IllegalArgumentException if the {@link CarText} does not meet the constraints in
         *     {@link CarTextConstraints#TEXT_ONLY}.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setTitle(@Nullable CarText title) {
            if (title != null) {
                CarTextConstraints.TEXT_ONLY.validateOrThrow(title);
            }
            this.mTitle = title;
            return this;
        }

        /**
         * Sets the title shown in the template body.
         *
         * <p>Note that {@link #setHeader} also has title which shows above the template body.
         *
         * @throws IllegalArgumentException if the {@link CharSequence} does not meet the
         *     constraints in {@link CarTextConstraints#TEXT_ONLY}.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setTitle(@Nullable CharSequence title) {
            if (title != null) {
                setTitle(CarText.create(title));
            }
            return this;
        }

        /**
         * Sets the icon that appears in the body of the template.
         *
         * <p>Note that the {@link Header} may also contain an icon in the form of a start header
         * {@link Action}.
         *
         * @throws IllegalArgumentException if the {@link CarIcon} does not meet the constraints in
         *     {@link CarIconConstraints#DEFAULT}.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setIcon(@Nullable CarIcon icon) {
            if (icon != null) {
                CarIconConstraints.DEFAULT.validateOrThrow(icon);
            }
            this.mIcon = icon;
            return this;
        }

        /**
         * Sets the header to show in the template.
         *
         * <p>If a start {@link Action} is set in the {@link Header}, it must conform to the
         * constraints in {@link
         * androidx.car.app.model.constraints.ActionsConstraints#ACTION_CONSTRAINTS_IN_CALL_HEADER}.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setHeader(@Nullable Header header) {
            if (header != null) {
                Action startHeaderAction = header.getStartHeaderAction();
                if (startHeaderAction != null) {
                    ACTION_CONSTRAINTS_IN_CALL_HEADER.validateOrThrow(
                            Collections.singletonList(startHeaderAction));
                }
            }
            this.mHeader = header;
            return this;
        }

        /**
         * Adds a text string below the title, with support for multiple length variants.
         *
         * <p>A maximum of {@value #MAX_TEXTS_SIZE} text strings can be added.
         *
         * @throws NullPointerException if {@code text} is {@code null}
         * @throws IllegalArgumentException if {@code text} contains unsupported spans
         */
        @NonNull
        @CanIgnoreReturnValue
        public Builder addText(@NonNull CarText text) {
            if (mTexts.size() >= MAX_TEXTS_SIZE) {
                throw new IllegalStateException(
                        "A maximum of " + MAX_TEXTS_SIZE + " texts can be added.");
            }
            CarTextConstraints.TEXT_AND_ICON.validateOrThrow(requireNonNull(text));
            mTexts.add(text);
            return this;
        }

        /**
         * Adds a text string below the title, with support for multiple length variants.
         *
         * <p>A maximum of {@value #MAX_TEXTS_SIZE} text strings can be added.
         *
         * @throws NullPointerException if {@code text} is {@code null}
         * @throws IllegalArgumentException if {@code text} contains unsupported spans
         */
        @NonNull
        @CanIgnoreReturnValue
        public Builder addText(@NonNull CharSequence text) {
            return addText(CarText.create(text));
        }

        /**
         * Adds an {@link Action} to display.
         *
         * <h4>Requirements</h4>
         *
         * This template allows up to 5 {@link Action}s in its body. Each action must have an icon
         * and cannot have a title. A single action may be flagged with {@link Action#FLAG_PRIMARY}.
         *
         * @throws NullPointerException if {@code action} is {@code null}
         * @throws IllegalArgumentException if {@code action} does not meet the requirements
         */
        @NonNull
        @CanIgnoreReturnValue
        public Builder addAction(@NonNull Action action) {
            mActions.add(requireNonNull(action));
            ACTION_CONSTRAINTS_IN_CALL_CONTENT.validateOrThrow(mActions);
            return this;
        }

        /**
         * Sets whether the template is in a loading state.
         *
         * <p>If set to {@code true}, the UI shows a loading indicator where text list and actions
         * would be otherwise. The caller is expected to call {@link
         * androidx.car.app.Screen#invalidate()} and send the new template content to the host once
         * the data is ready. If template content is set this must be {@code false} or an exception
         * will be thrown in {@link #build()}.
         */
        @NonNull
        @CanIgnoreReturnValue
        public Builder setLoading(boolean isLoading) {
            this.mIsLoading = isLoading;
            return this;
        }

        /** Constructs the {@link InCallTemplate} defined by this builder. */
        @NonNull
        public InCallTemplate build() {
            boolean hasContent =
                    !mTexts.isEmpty() || !mActions.isEmpty() || mIcon != null || mTitle != null;
            if (hasContent == mIsLoading) {
                throw new IllegalStateException(
                        "Must have either content or be loading but not both");
            }
            return new InCallTemplate(this);
        }
    }
}
