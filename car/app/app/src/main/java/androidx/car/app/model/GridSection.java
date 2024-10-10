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

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * A section within the {@code SectionedItemTemplate} that contains {@link GridItem}s - good for
 * showcase large artwork or images for every entry.
 */
@ExperimentalCarApi
@CarProtocol
@KeepFields
public final class GridSection extends Section<GridItem> {
    /** Defines possible sizes of the grid items within a grid section. */
    @IntDef(
            value = {
                    ITEM_SIZE_SMALL,
                    ITEM_SIZE_MEDIUM,
                    ITEM_SIZE_LARGE
            })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public @interface ItemSize {
    }

    /** Renders the items within the section in a small format - this is the default. */
    @ItemSize
    public static final int ITEM_SIZE_SMALL = 1;

    /** Renders the items within the section in a medium sized format. */
    @ItemSize
    public static final int ITEM_SIZE_MEDIUM = 2;

    /** Renders the items within the section in a large format. */
    @ItemSize
    public static final int ITEM_SIZE_LARGE = 3;

    /** Defines the possible shapes of the images shown on the grid items within a grid section. */
    @IntDef(
            value = {
                    ITEM_IMAGE_SHAPE_UNSET,
                    ITEM_IMAGE_SHAPE_CIRCLE,
            })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public @interface ItemImageShape {
    }

    /**
     * Renders the image within the grid item as-is without modifying its shape - this is the
     * default.
     */
    @ItemImageShape
    public static final int ITEM_IMAGE_SHAPE_UNSET = 1;

    /** Renders the image within the grid item in the shape of a circle by cropping it. */
    @ItemImageShape
    public static final int ITEM_IMAGE_SHAPE_CIRCLE = 2;

    @ItemSize
    private final int mItemSize;

    @ItemImageShape
    private final int mItemImageShape;

    // Empty constructor for serialization
    private GridSection() {
        super();
        mItemSize = ITEM_SIZE_SMALL;
        mItemImageShape = ITEM_IMAGE_SHAPE_UNSET;
    }

    /** Creates a {@link GridSection} from the {@link Builder}. */
    private GridSection(Builder builder) {
        super(builder);
        mItemSize = builder.mItemSize;
        mItemImageShape = builder.mItemImageShape;
    }

    /** Returns the size which this section's grid items should be rendered at. */
    @ItemSize
    public int getItemSize() {
        return mItemSize;
    }

    /** Returns the shape which this section's grid item images should be rendered to. */
    @ItemImageShape
    public int getItemImageShape() {
        return mItemImageShape;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), mItemImageShape, mItemSize);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (!(other instanceof GridSection)) {
            return false;
        }
        GridSection section = (GridSection) other;
        return super.equals(section)
                && mItemImageShape == section.mItemImageShape
                && mItemSize == section.mItemSize;
    }

    @Override
    public @NonNull String toString() {
        return "GridSection { itemSize: " + mItemSize + ", itemImageShape: " + mItemImageShape
                + ", " + super.toString() + " }";
    }

    /** A builder that constructs {@link GridSection} instances. */
    @ExperimentalCarApi
    public static final class Builder extends BaseBuilder<GridItem, Builder> {
        @ItemSize
        private int mItemSize = ITEM_SIZE_SMALL;

        @ItemImageShape
        private int mItemImageShape = ITEM_IMAGE_SHAPE_UNSET;

        /** Create a new {@link GridSection} builder. */
        public Builder() {
            super();
        }

        /** Sets the size of the items within this section. */
        @CanIgnoreReturnValue
        public @NonNull Builder setItemSize(@ItemSize int itemSize) {
            mItemSize = itemSize;
            return this;
        }

        /**
         * Sets how the images of all grid items within this section should be rendered. Uses
         * {@link #ITEM_IMAGE_SHAPE_UNSET} by default.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setItemImageShape(@ItemImageShape int itemImageShape) {
            mItemImageShape = itemImageShape;
            return this;
        }

        /** Creates a new {@link GridSection} based off the state of this builder. */
        public @NonNull GridSection build() {
            return new GridSection(this);
        }
    }
}
