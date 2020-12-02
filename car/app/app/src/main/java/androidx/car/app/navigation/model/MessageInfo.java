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
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarText;
import androidx.car.app.model.constraints.CarIconConstraints;
import androidx.car.app.navigation.model.NavigationTemplate.NavigationInfo;

import java.util.Objects;

/** Represents a message that can be shown in the {@link NavigationTemplate}. */
public class MessageInfo implements NavigationInfo {
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
     * Constructs a new builder of {@link MessageInfo}.
     *
     * @throws NullPointerException if {@code title} is {@code null}.
     */
    @NonNull
    public static Builder builder(@NonNull CharSequence title) {
        return new Builder(title);
    }

    @NonNull
    public CarText getTitle() {
        return requireNonNull(mTitle);
    }

    @Nullable
    public CarText getText() {
        return mText;
    }

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

    private MessageInfo(Builder builder) {
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
        private CarText mTitle;
        @Nullable
        private CarText mText;
        @Nullable
        private CarIcon mImage;

        private Builder(@NonNull CharSequence title) {
            this.mTitle = CarText.create(requireNonNull(title));
        }

        /**
         * Sets the title of the message.
         *
         * @throws NullPointerException if {@code message} is {@code null}.
         */
        @NonNull
        public Builder setTitle(@NonNull CharSequence title) {
            this.mTitle = CarText.create(requireNonNull(title));
            return this;
        }

        /** Sets additional text on the message or {@code null} to not set any additional text. */
        @NonNull
        public Builder setText(@Nullable CharSequence text) {
            this.mText = text == null ? null : CarText.create(text);
            return this;
        }

        /**
         * Sets the image to display along with the message, or {@code null} to not display an
         * image.
         */
        @NonNull
        public Builder setImage(@Nullable CarIcon image) {
            CarIconConstraints.DEFAULT.validateOrThrow(image);
            this.mImage = image;
            return this;
        }

        /** Constructs the {@link MessageInfo} defined by this builder. */
        @NonNull
        public MessageInfo build() {
            return new MessageInfo(this);
        }
    }
}
