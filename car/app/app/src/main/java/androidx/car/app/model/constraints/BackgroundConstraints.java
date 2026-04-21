/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.car.app.model.constraints;

import static androidx.annotation.RestrictTo.Scope;

import static java.util.Objects.requireNonNull;

import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.Background;

import org.jspecify.annotations.NonNull;

/**
 * Encapsulates the constraints to apply when rendering a {@link Background} on a template.
 */
@RestrictTo(Scope.LIBRARY)
@RequiresCarApi(9)
@ExperimentalCarApi
public final class BackgroundConstraints {

    public static final @NonNull BackgroundConstraints UNCONSTRAINED =
            new BackgroundConstraints.Builder().build();


    public static final @NonNull BackgroundConstraints COLOR_ONLY =
            new BackgroundConstraints.Builder().setImageAllowed(false).build();

    private final boolean mIsImageAllowed;

    /** Returns whether an image is allowed for the background. */
    public boolean isImageAllowed() {
        return mIsImageAllowed;
    }

    /**
     * Validates that the given background satisfies this {@link BackgroundConstraints} instance.
     *
     * @throws IllegalArgumentException if the constraint does not allow images and the background
     *                                  contains an image
     */
    public void validateOrThrow(@NonNull Background background) {
        if (!mIsImageAllowed && background.getImage() != null) {
            throw new IllegalArgumentException("Image backgrounds are not supported");
        }
    }

    BackgroundConstraints(Builder builder) {
        mIsImageAllowed = builder.mIsImageAllowed;
    }

    /** A builder of {@link BackgroundConstraints}. */
    public static final class Builder {
        boolean mIsImageAllowed = true;

        /** Sets whether an image can be used for the background. */
        public @NonNull Builder setImageAllowed(boolean isImageAllowed) {
            mIsImageAllowed = isImageAllowed;
            return this;
        }

        /**
         * Constructs the {@link BackgroundConstraints} defined by this builder.
         */
        public @NonNull BackgroundConstraints build() {
            return new BackgroundConstraints(this);
        }

        /** Returns an empty {@link Builder} instance. */
        public Builder() {
        }

        /**
         * Returns a new builder that contains the same data as the given
         * {@link BackgroundConstraints} instance.
         */
        public Builder(@NonNull BackgroundConstraints constraints) {
            requireNonNull(constraints);
            mIsImageAllowed = constraints.isImageAllowed();
        }
    }
}
