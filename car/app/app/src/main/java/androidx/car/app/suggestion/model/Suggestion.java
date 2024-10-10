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

package androidx.car.app.suggestion.model;

import static java.util.Objects.requireNonNull;

import android.app.PendingIntent;

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarText;
import androidx.car.app.model.constraints.CarIconConstraints;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Suggestion that an application provide to an end user in order to be displayed by the host
 * and allow then to interact with their application.
 *
 * <h4>Examples</h4>
 *
 * When a user opens their navigation app, it can post a suggestion as for the next
 * place to drive to rather than wait for the user to enter or search for it.
 */
@CarProtocol
@KeepFields
public final class Suggestion {
    private final @NonNull String mIdentifier;
    private final @NonNull CarText mTitle;
    private final @Nullable CarText mSubtitle;
    private final @Nullable CarIcon mIcon;
    private final @Nullable PendingIntent mAction;

    /**
     * Returns the identifier of the suggestion.
     *
     * @see Builder#setIdentifier(String)
     */
    public @NonNull String getIdentifier() {
        return mIdentifier;
    }

    /**
     * Returns the title of the suggestion.
     *
     * @see Builder#setTitle(CharSequence)
     */
    public @NonNull CarText getTitle() {
        return mTitle;
    }

    /**
     * Returns the subtitle of the suggestion or {@code null} if not set.
     *
     * @see Builder#setSubtitle(CharSequence)
     */
    public @Nullable CarText getSubtitle() {
        return mSubtitle;
    }

    /**
     * Returns a {@code CarIcon} to display with the suggestion or {@code null} if not set.
     *
     * @see Builder#setIcon(CarIcon)
     */
    public @Nullable CarIcon getIcon() {
        return mIcon;
    }

    /**
     * Returns the {@link PendingIntent} of the suggestion.
     *
     * @see Builder#setAction(PendingIntent)
     */
    public @Nullable PendingIntent getAction() {
        return mAction;
    }

    @Override
    public @NonNull String toString() {
        return "[id: " + mIdentifier
                + ", title: "
                + CarText.toShortString(mTitle)
                + ", subtitle: "
                + CarText.toShortString(mSubtitle)
                + ", pendingIntent: "
                + mAction
                + ", icon: "
                + mIcon
                + "]";
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof Suggestion)) {
            return false;
        }

        Suggestion otherDestination = (Suggestion) other;
        return Objects.equals(mIdentifier, otherDestination.mIdentifier)
                && Objects.equals(mTitle, otherDestination.mTitle)
                && Objects.equals(mSubtitle, otherDestination.mSubtitle)
                && Objects.equals(mAction, otherDestination.mAction)
                && Objects.equals(mIcon, otherDestination.mIcon);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mIdentifier, mTitle, mSubtitle, mIcon, mAction);
    }

    Suggestion(Builder builder) {
        mIdentifier = requireNonNull(builder.mId);
        mTitle = requireNonNull(builder.mTitle);
        mSubtitle = builder.mSubtitle;
        mIcon = builder.mIcon;
        mAction = requireNonNull(builder.mAction);
    }

    /** Constructs an empty instance, used by serialization code. */
    private Suggestion() {
        mIdentifier = "";
        mTitle = CarText.create("");
        mSubtitle = null;
        mIcon = null;
        mAction = null;
    }

    /** A builder of {@link Suggestion}. */
    public static final class Builder {
        @Nullable String mId;
        @Nullable CarText mTitle;
        @Nullable CarText mSubtitle;
        @Nullable CarIcon mIcon;
        @Nullable PendingIntent mAction;

        /**
         * Sets the suggestion identifier.
         *
         * @throws NullPointerException if {@code identifier} is {@code null}
         */
        public @NonNull Builder setIdentifier(@NonNull String identifier) {
            mId = requireNonNull(identifier);
            return this;
        }

        /**
         * Sets the suggestion title formatted for the user's current locale.
         *
         * <p>Spans are not supported in the input string and will be ignored.
         *
         * @throws NullPointerException if {@code title} is {@code null}
         * @see CarText
         */
        public @NonNull Builder setTitle(@NonNull CharSequence title) {
            mTitle = CarText.create(requireNonNull(title));
            return this;
        }

        /**
         * Sets the suggestion subtitle formatted for the user's current locale.
         *
         * <p>Spans are not supported in the input string and will be ignored.
         *
         * @throws NullPointerException if {@code subtitle} is {@code null}
         * @see CarText
         */
        public @NonNull Builder setSubtitle(@NonNull CharSequence subtitle) {
            mSubtitle = CarText.create(requireNonNull(subtitle));
            return this;
        }

        /**
         * Sets the {@link PendingIntent} for the suggestion action.
         *
         * @throws NullPointerException if {@code pendingIntent} is {@code null}
         */
        public @NonNull Builder setAction(@NonNull PendingIntent action) {
            mAction = requireNonNull(action);
            return this;
        }

        /**
         * Sets a suggestion image to display.
         *
         * <h4>Image Sizing Guidance</h4>
         *
         * To minimize scaling artifacts across a wide range of car screens, apps should provide
         * images targeting a 128 x 128 dp bounding box. If the image exceeds this maximum size in
         * either one of the dimensions, it will be scaled down to be centered inside the
         * bounding box while preserving the aspect ratio.
         *
         * Icon images are expected to be tintable.
         *
         * <p>See {@link CarIcon} for more details related to providing icon and image resources
         * that work with different car screen pixel densities.
         *
         * @throws NullPointerException if {@code image} is {@code null}
         */
        public @NonNull Builder setIcon(@NonNull CarIcon icon) {
            CarIconConstraints.DEFAULT.validateOrThrow(requireNonNull(icon));
            mIcon = icon;
            return this;
        }

        /**
         * Constructs the {@link Suggestion} defined by this builder.
         *
         * @throws IllegalStateException if any of the files are {@code null} or if  the title
         *                               and the subtitle are empty.
         */
        public @NonNull Suggestion build() {
            if (mId == null) {
                throw new IllegalStateException("Identifier is a required field");
            }
            if (mTitle == null || mTitle.isEmpty()) {
                throw new IllegalStateException("Title is a required field");
            }

            if (mAction == null) {
                throw new IllegalStateException("Action is a required field");
            }
            return new Suggestion(this);
        }

        /** Returns an empty {@link Builder} instance. */
        public Builder() {
        }
    }
}
