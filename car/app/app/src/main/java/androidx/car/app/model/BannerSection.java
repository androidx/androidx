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

package androidx.car.app.model;

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A section within the {@code SectionedItemTemplate} that contains a {@link Banner}.
 *
 * <p>This section is good for displaying transient, high-priority information or alerts with
 * optional actions.
 */
@CarProtocol
@ExperimentalCarApi
@KeepFields
@RequiresCarApi(9)
public final class BannerSection extends Section<Banner> {
    // Empty constructor for serialization
    BannerSection() {
        super();
    }

    /** Creates a {@link BannerSection} from the {@link Builder}. */
    BannerSection(Builder builder) {
        super(builder);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (!(other instanceof BannerSection)) {
            return false;
        }
        BannerSection section = (BannerSection) other;
        return super.equals(section);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode());
    }

    @Override
    public @NonNull String toString() {
        return "BannerSection { " + super.toString() + " }";
    }

    /** A builder that constructs {@link BannerSection} instances. */
    public static final class Builder extends BaseBuilder<Banner, Builder> {
        /** Create a new {@link BannerSection} builder. */
        public Builder() {
            super();
        }

        /**
         * Constructs a new {@link BannerSection} from the current state of this builder, throwing
         * exceptions for any invalid state.
         *
         * <p>Upon building, this class validates that exactly one Banner is present.
         *
         * @throws IllegalStateException if the section does not contain exactly one {@link Banner}
         */
        public @NonNull BannerSection build() {
            if (mItems.size() != 1) {
                throw new IllegalStateException("A BannerSection must contain exactly one Banner");
            }
            return new BannerSection(this);
        }
    }
}
