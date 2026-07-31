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

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * A {@link Section} within the {@code SectionedItemTemplate} that contains {@link CondensedItem}s.
 */
@RequiresCarApi(9)
@ExperimentalCarApi
@CarProtocol
@KeepFields
public final class CondensedSection extends Section<CondensedItem> {
    /**
     * Defines the strategy to use for handling the incomplete last row
     */
    @IntDef(
            value = {
                    INCOMPLETE_LAST_ROW_AS_IS,
                    INCOMPLETE_LAST_ROW_TRUNCATE,
            })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public @interface IncompleteLastRowStrategy {
    }

    /**
     * The last row will be shown as-is. This is a default behavior
     */
    @IncompleteLastRowStrategy
    public static final int INCOMPLETE_LAST_ROW_AS_IS = 0;

    /**
     * Truncates the last row if that row is not completely filled
     */
    @IncompleteLastRowStrategy
    public static final int INCOMPLETE_LAST_ROW_TRUNCATE = 1;

    @IncompleteLastRowStrategy
    private final int mIncompleteLastRowStrategy;

    /**
     * Creates a {@link CondensedSection} from the {@link Builder}.
     */
    private CondensedSection(@NonNull Builder builder) {
        super(builder);
        mIncompleteLastRowStrategy = builder.mIncompleteLastRowStrategy;
    }

    /** For serialization. */
    private CondensedSection() {
        super();
        mIncompleteLastRowStrategy = INCOMPLETE_LAST_ROW_AS_IS;
    }

    /**
     * Returns the strategy for handling incomplete last row
     */
    @IncompleteLastRowStrategy
    public int getIncompleteLastRowStrategy() {
        return mIncompleteLastRowStrategy;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CondensedSection)) {
            return false;
        }
        CondensedSection section = (CondensedSection) other;
        return super.equals(other)
                && mIncompleteLastRowStrategy == section.mIncompleteLastRowStrategy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), mIncompleteLastRowStrategy);
    }

    @Override
    public @NonNull String toString() {
        return "CondensedSection { incompleteLastRowStrategy: " + mIncompleteLastRowStrategy
                + ", " + super.toString() + " }";
    }

    /** A builder for {@link CondensedSection}. */
    @RequiresCarApi(9)
    @ExperimentalCarApi
    public static final class Builder extends BaseBuilder<CondensedItem, Builder> {
        @IncompleteLastRowStrategy
        private int mIncompleteLastRowStrategy = INCOMPLETE_LAST_ROW_AS_IS;

        /**
         * Create a new {@link CondensedSection} builder.
         */
        public Builder() {
            super();
        }

        /**
         * Sets the strategy to use for handling the incomplete last row
         *
         * <p>By default, {@link #INCOMPLETE_LAST_ROW_AS_IS} is used.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setIncompleteLastRowStrategy(
                @IncompleteLastRowStrategy int incompleteLastRowStrategy) {
            mIncompleteLastRowStrategy = incompleteLastRowStrategy;
            return this;
        }

        /**
         * Constructs a {@link CondensedSection} from the current state of this builder.
         */
        public @NonNull CondensedSection build() {
            return new CondensedSection(this);
        }
    }
}
