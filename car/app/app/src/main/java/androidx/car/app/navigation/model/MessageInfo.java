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

package androidx.car.app.navigation.model;

import static java.util.Objects.requireNonNull;

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarText;
import androidx.car.app.model.DistanceSpan;
import androidx.car.app.model.DurationSpan;
import androidx.car.app.model.constraints.CarIconConstraints;
import androidx.car.app.model.constraints.CarTextConstraints;
import androidx.car.app.navigation.model.NavigationTemplate.NavigationInfo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/** Represents a message that can be shown in the {@link NavigationTemplate}. */
@CarProtocol
@KeepFields
public final class MessageInfo implements NavigationInfo {
    private final @Nullable CarText mTitle;
    private final @Nullable CarText mText;
    private final @Nullable CarIcon mImage;

    /**
     * Returns the title of the message or {@code null} if not set.
     *
     * @see Builder#setTitle(CharSequence)
     */
    public @Nullable CarText getTitle() {
        return mTitle;
    }

    /**
     * Returns the text to display with the message or {@code null} if not set.
     *
     * @see Builder#setText(CharSequence)
     */
    public @Nullable CarText getText() {
        return mText;
    }

    /**
     * Returns the image to display along with the message or {@code null} if not set.
     *
     * @see Builder#setImage(CarIcon)
     */
    public @Nullable CarIcon getImage() {
        return mImage;
    }

    @Override
    public @NonNull String toString() {
        return "MessageInfo";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTitle, mText, mImage);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MessageInfo)) {
            return false;
        }
        MessageInfo otherInfo = (MessageInfo) other;

        return Objects.equals(mTitle, otherInfo.mTitle)
                && Objects.equals(mText, otherInfo.mText)
                && Objects.equals(mImage, otherInfo.mImage);
    }

    MessageInfo(Builder builder) {
        mTitle = builder.mTitle;
        mText = builder.mText;
        mImage = builder.mImage;
    }

    /** Constructs an empty instance, used by serialization code. */
    private MessageInfo() {
        mTitle = null;
        mText = null;
        mImage = null;
    }

    /** A builder of {@link MessageInfo}. */
    public static final class Builder {
        @Nullable CarText mTitle;
        @Nullable CarText mText;
        @Nullable CarIcon mImage;

        /**
         * Sets the title of the message.
         *
         * <p>Only {@link DistanceSpan}s and {@link DurationSpan}s are supported in the input
         * string.
         *
         * @throws NullPointerException     if {@code title} is {@code null}
         * @throws IllegalArgumentException if {@code title} contains unsupported spans
         * @see CarText
         */
        public @NonNull Builder setTitle(@NonNull CharSequence title) {
            mTitle = CarText.create(requireNonNull(title));
            CarTextConstraints.TEXT_ONLY.validateOrThrow(mTitle);
            return this;
        }

        /**
         * Sets additional text on the message.
         *
         * <p>Only {@link DistanceSpan}s and {@link DurationSpan}s are supported in the input
         * string.
         *
         * @throws NullPointerException     if {@code text} is {@code null}
         * @throws IllegalArgumentException if {@code text} contains unsupported spans
         * @see CarText
         */
        public @NonNull Builder setText(@NonNull CharSequence text) {
            mText = CarText.create(requireNonNull(text));
            CarTextConstraints.TEXT_ONLY.validateOrThrow(mText);
            return this;
        }

        /**
         * Sets additional text on the message, with support for multiple length variants.
         *
         * <p>Only {@link DistanceSpan}s and {@link DurationSpan}s are supported in the input
         * string.
         *
         * @throws NullPointerException     if {@code text} is {@code null}
         * @throws IllegalArgumentException if {@code text} contains unsupported spans
         * @see CarText
         */
        public @NonNull Builder setText(@NonNull CarText text) {
            mText = requireNonNull(text);
            CarTextConstraints.TEXT_ONLY.validateOrThrow(mText);
            return this;
        }

        /**
         * Sets the image to display along with the message.
         *
         * <h4>Image Sizing Guidance</h4>
         *
         * To minimize scaling artifacts across a wide range of car screens, apps should provide
         * images targeting a 128 x 128 dp bounding box. If the image exceeds this maximum size in
         * either one of the dimensions, it will be scaled down to be centered inside the
         * bounding box while preserving the aspect ratio.
         *
         * <p>See {@link CarIcon} for more details related to providing icon and image resources
         * that work with different car screen pixel densities.
         *
         * @throws NullPointerException if {@code image} is {@code null}
         */
        public @NonNull Builder setImage(@NonNull CarIcon image) {
            CarIconConstraints.DEFAULT.validateOrThrow(requireNonNull(image));
            mImage = image;
            return this;
        }

        /** Constructs the {@link MessageInfo} defined by this builder. */
        public @NonNull MessageInfo build() {
            return new MessageInfo(this);
        }

        /**
         * Returns a new instance of a {@link Builder}.
         *
         * <p>Only {@link DistanceSpan}s and {@link DurationSpan}s are supported in the input
         * string.
         *
         * @throws NullPointerException     if {@code title} is {@code null}
         * @throws IllegalArgumentException if {@code title} contains unsupported spans
         */
        public Builder(@NonNull CharSequence title) {
            mTitle = CarText.create(requireNonNull(title));
            CarTextConstraints.TEXT_ONLY.validateOrThrow(mTitle);
        }

        /**
         * Returns a new instance of a {@link Builder}.
         *
         * <p>Spans are not supported in the input string and will be ignored.
         *
         * @throws NullPointerException if {@code title} is {@code null}
         */
        public Builder(@NonNull CarText title) {
            mTitle = requireNonNull(title);
        }
    }
}
