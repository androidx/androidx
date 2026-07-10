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

import static java.util.Objects.requireNonNull;

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A section within the {@link SectionedItemTemplate} that contains a single image and multiple
 * {@link CondensedItem}s. The items are rendered next to the image. Depending on the screen
 * size, only some of the leading items may be placed.
 */
@CarProtocol
@KeepFields
@RequiresCarApi(9)
@ExperimentalCarApi
public final class SpotlightSection extends Section<CondensedItem> {

    private final @Nullable CarIcon mImage;

    private SpotlightSection() {
        super();
        mImage = null;
    }

    private SpotlightSection(Builder builder) {
        super(builder);
        mImage = builder.mImage;
    }

    /** Returns the image associated with the section. */
    public @NonNull CarIcon getImage() {
        return requireNonNull(mImage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), mImage);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (!(other instanceof SpotlightSection)) {
            return false;
        }
        SpotlightSection that = (SpotlightSection) other;
        return super.equals(that) && Objects.equals(mImage, that.mImage);
    }

    @Override
    public @NonNull String toString() {
        return "SpotlightSection { image: " + mImage + ", " + super.toString() + " }";
    }

    /** A builder of {@link SpotlightSection}. */
    public static final class Builder extends BaseBuilder<CondensedItem, Builder> {
        @NonNull CarIcon mImage;

        /** Creates a new {@link SpotlightSection} builder. */
        public Builder(@NonNull CarIcon image) {
            super();
            mImage = requireNonNull(image);
        }

        /**
         * Constructs a new {@link SpotlightSection} from the current state of this builder.
         *
         * @throws IllegalStateException if there are no items.
         */
        public @NonNull SpotlightSection build() {
            if (mItems.isEmpty()) {
                throw new IllegalStateException("A spotlight section must contain at least 1 item");
            }

            return new SpotlightSection(this);
        }
    }
}
