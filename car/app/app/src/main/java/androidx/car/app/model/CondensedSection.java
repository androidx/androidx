/*
 * Copyright 2024 The Android Open Source Project
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

/**
 * A {@link Section} within the {@code SectionedItemTemplate} that contains {@link CondensedItem}s.
 */
@RequiresCarApi(9)
@ExperimentalCarApi
@CarProtocol
@KeepFields
public final class CondensedSection extends Section<CondensedItem> {
    /**
     * Creates a {@link CondensedSection} from the {@link Builder}.
     */
    private CondensedSection(@NonNull Builder builder) {
        super(builder);
    }

    /** For serialization. */
    private CondensedSection() {
        super();
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CondensedSection)) {
            return false;
        }
        return super.equals(other);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public @NonNull String toString() {
        return "CondensedSection { " + super.toString() + " }";
    }

    /** A builder for {@link CondensedSection}. */
    @RequiresCarApi(9)
    @ExperimentalCarApi
    public static final class Builder extends BaseBuilder<CondensedItem, Builder> {
        /**
         * Create a new {@link CondensedSection} builder.
         */
        public Builder() {
            super();
        }

        /**
         * Constructs a {@link CondensedSection} from the current state of this builder.
         */
        public @NonNull CondensedSection build() {
            return new CondensedSection(this);
        }
    }
}
