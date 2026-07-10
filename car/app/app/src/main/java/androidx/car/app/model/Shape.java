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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * A primitive defining container shapes.
 */
@RequiresCarApi(9)
@ExperimentalCarApi
@CarProtocol
@KeepFields
public final class Shape {
    /**
     * Shape types for {@link Shape}.
     */
    @RestrictTo(LIBRARY)
    @IntDef({
            TYPE_NONE,
            TYPE_CORNER_EXTRA_SMALL,
            TYPE_CORNER_SMALL,
            TYPE_CORNER_MEDIUM,
            TYPE_CORNER_LARGE,
            TYPE_CORNER_EXTRA_LARGE,
            TYPE_CORNER_FULL,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ShapeType {
    }

    /**
     * Represents a rectangular shape with sharp corners
     */
    public static final int TYPE_NONE = 1;

    /**
     * An extra small corner shape.
     */
    public static final int TYPE_CORNER_EXTRA_SMALL = 2;

    /**
     * A small corner shape.
     */
    public static final int TYPE_CORNER_SMALL = 3;

    /**
     * A medium corner shape.
     */
    public static final int TYPE_CORNER_MEDIUM = 4;

    /**
     * A large corner shape.
     */
    public static final int TYPE_CORNER_LARGE = 5;

    /**
     * An extra large corner shape.
     */
    public static final int TYPE_CORNER_EXTRA_LARGE = 6;

    /**
     * A full corner (pill/circle) shape.
     */
    public static final int TYPE_CORNER_FULL = 7;

    /**
     * A {@link Shape} instance with no rounding.
     */
    public static final @NonNull Shape NONE = new Shape(TYPE_NONE);

    /**
     * An extra small cornered {@link Shape} instance.
     */
    public static final @NonNull Shape CORNER_EXTRA_SMALL =
            new Shape(TYPE_CORNER_EXTRA_SMALL);

    /**
     * A small cornered {@link Shape} instance.
     */
    public static final @NonNull Shape CORNER_SMALL = new Shape(TYPE_CORNER_SMALL);

    /**
     * A medium cornered {@link Shape} instance.
     */
    public static final @NonNull Shape CORNER_MEDIUM = new Shape(TYPE_CORNER_MEDIUM);

    /**
     * A large cornered {@link Shape} instance.
     */
    public static final @NonNull Shape CORNER_LARGE = new Shape(TYPE_CORNER_LARGE);

    /**
     * An extra large cornered {@link Shape} instance.
     */
    public static final @NonNull Shape CORNER_EXTRA_LARGE =
            new Shape(TYPE_CORNER_EXTRA_LARGE);

    /**
     * A fully cornered {@link Shape} instance.
     */
    public static final @NonNull Shape CORNER_FULL = new Shape(TYPE_CORNER_FULL);

    @ShapeType
    private final int mShapeType;

    /**
     * Returns the {@link ShapeType} of the shape.
     */
    @ShapeType
    public int getShapeType() {
        return mShapeType;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Shape)) {
            return false;
        }
        Shape that = (Shape) other;
        return mShapeType == that.mShapeType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mShapeType);
    }

    @Override
    public @NonNull String toString() {
        return "Shape { shapeType: " + mShapeType + " }";
    }

    private Shape(@ShapeType int shapeType) {
        mShapeType = shapeType;
    }

    /** For serialization. */
    private Shape() {
        mShapeType = TYPE_NONE;
    }
}
