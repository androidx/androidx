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
 * A section of {@link Chip}s. Only 1 ChipSection is allowed in the
 * {@link SectionedItemTemplate}.
 *
 * <p>A group of {@link Chip}s that typically help a user filter a template's content down to
 * more specific items or dynamically suggest relevant actions for a user to take.
 *
 * <p>The host may limit the number of chips displayed to ensure driver safety.
 */
@ExperimentalCarApi
@CarProtocol
@RequiresCarApi(9)
@KeepFields
public final class ChipSection extends Section<Chip> {
    @Nullable
    private final ChipStyle mStyle;

    /**
     * Returns the style of the section, or {@code null} if not set.
     */
    @Nullable
    public ChipStyle getStyle() {
        return mStyle;
    }

    @Override
    @NonNull
    public String toString() {
        return "ChipSection{"
                + "items="
                + getItemsDelegate()
                + ", title="
                + getTitle()
                + ", style="
                + mStyle
                + "}";
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), mStyle);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ChipSection)) {
            return false;
        }
        ChipSection otherSection = (ChipSection) other;
        return super.equals(otherSection) && Objects.equals(mStyle, otherSection.mStyle);
    }

    ChipSection(Builder builder) {
        super(builder);
        mStyle = builder.mStyle;
    }

    /** Constructs an empty instance, used by serialization code. */
    private ChipSection() {
        super();
        mStyle = null;
    }

    /** A builder of {@link ChipSection}. */
    public static final class Builder extends BaseBuilder<Chip, Builder> {
        @Nullable
        ChipStyle mStyle;

        /**
         * Sets the style for all chips in this section.
         *
         * <p>Any fields not explicitly set here or in the
         * individual chip styling of {@link Chip.Builder#setStyle} will fall back to host
         * defaults. If the colors do not meet the contrast requirements, the host will set the
         * chip styling to defaults based on the {@link Chip.Builder#setSelected(boolean)}
         * state.
         *
         * @throws NullPointerException if {@code style} is {@code null}
         */
        @NonNull
        public Builder setStyle(@NonNull ChipStyle style) {
            mStyle = requireNonNull(style);
            return this;
        }

        /**
         * Constructs the {@link ChipSection} defined by this builder.
         *
         * @throws IllegalArgumentException if the section does not contain at least one item.
         */
        @NonNull
        public ChipSection build() {
            if (mItems.isEmpty()) {
                throw new IllegalArgumentException("ChipSection must contain at least one "
                        + "item");
            }
            return new ChipSection(this);
        }

        /**
         * Creates a new instance of the builder.
         */
        public Builder() {
            super();
        }
    }
}
