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

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarText;
import androidx.car.app.model.constraints.CarIconConstraints;
import androidx.car.app.navigation.model.NavigationTemplate.NavigationInfo;

import java.util.Objects;

/** Represents a message that can be shown in the {@link NavigationTemplate}. */
@CarProtocol
public final class MessageInfo implements NavigationInfo {
    @Keep
    @Nullable
    private final CarText mTitle;
    @Keep
    @Nullable
    private final CarText mText;
    @Keep
    @Nullable
    private final CarIcon mImage;

    /**
     * Returns the title of the message or {@code null} if not set.
     *
     * @see Builder#setTitle(CharSequence)
     */
    @Nullable
    public CarText getTitle() {
        return mTitle;
    }

    /**
     * Returns the text to display with the message or {@code null} if not set.
     *
     * @see Builder#setText(CharSequence)
     */
    @Nullable
    public CarText getText() {
        return mText;
    }

    /**
     * Returns the image to display along with the message or {@code null} if not set.
     *
     * @see Builder#setImage(CarIcon)
     */
    @Nullable
    public CarIcon getImage() {
        return mImage;
    }

    @NonNull
    @Override
    public String toString() {
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
        @Nullable
        CarText mTitle;
        @Nullable
        CarText mText;
        @Nullable
        CarIcon mImage;

        /**
         * Sets the title of the message.
         *
         * <p>Spans are not supported in the input string and will be ignored.
         *
         * @throws NullPointerException if {@code message} is {@code null}
         * @see CarText
         */
        @NonNull
        public Builder setTitle(@NonNull CharSequence title) {
            mTitle = CarText.create(requireNonNull(title));
            return this;
        }

        /**
         * Sets additional text on the message.
         *
         * <p>Spans are not supported in the input string and will be ignored.
         *
         * @throws NullPointerException if {@code text} is {@code null}
         * @see CarText
         */
        @NonNull
        public Builder setText(@NonNull CharSequence text) {
            mText = CarText.create(requireNonNull(text));
            return this;
        }

        /**
         * Sets additional text on the message, with support for multiple length variants.
         *
         * <p>Spans are not supported in the input string and will be ignored.
         *
         * @throws NullPointerException if {@code text} is {@code null}
         * @see CarText
         */
        @NonNull
        public Builder setText(@NonNull CarText text) {
            mText = requireNonNull(text);
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
        @NonNull
        public Builder setImage(@NonNull CarIcon image) {
            CarIconConstraints.DEFAULT.validateOrThrow(requireNonNull(image));
            mImage = image;
            return this;
        }

        /** Constructs the {@link MessageInfo} defined by this builder. */
        @NonNull
        public MessageInfo build() {
            return new MessageInfo(this);
        }

        /**
         * Returns a new instance of a {@link Builder}.
         *
         * <p>Spans are not supported in the input string and will be ignored.
         *
         * @throws NullPointerException if {@code title} is {@code null}
         */
        public Builder(@NonNull CharSequence title) {
            mTitle = CarText.create(requireNonNull(title));
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
