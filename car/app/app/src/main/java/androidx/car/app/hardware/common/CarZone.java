/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.car.app.hardware.common;

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
 * Allows specification of a car zone using rows and columns. For example, CarZone.Driver allows
 * modification of Driver's seat controls without knowing whether it is a left side driving car
 * or right side driving car.
 *
 * This class is different from CarOccupantZoneManager in that CarZone is specifically for
 * AndroidX APIs' implementation to control car features such as temperature based on zones while
 * CarOccupantZoneManager class provides APIs to get displays and users information.
 *
 * <p> {@link CarValue#getCarZones()} returns a list of these {@link CarZone}s. It indicates the
 * {@link CarValue} happens in those {@link CarValue}s of the vehicle.
 */
@CarProtocol
@RequiresCarApi(5)
@ExperimentalCarApi
@KeepFields
public final class CarZone {
    /**
     * Possible row values.
     *
     */
    @IntDef({
            CAR_ZONE_ROW_ALL,
            CAR_ZONE_ROW_FIRST,
            CAR_ZONE_ROW_SECOND,
            CAR_ZONE_ROW_THIRD,
            CAR_ZONE_ROW_EXCLUDE_FIRST,
    })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY)
    public @interface CarZoneRow {
    }

    /** Refers to all the rows in the vehicle. */
    @CarZoneRow
    public static final int CAR_ZONE_ROW_ALL = 0x0;

    /** Refers to the front row of the vehicle only. */
    @CarZoneRow
    public static final int CAR_ZONE_ROW_FIRST = 0x1;

    /** Refers to the second row of the vehicle only. */
    @CarZoneRow
    public static final int CAR_ZONE_ROW_SECOND = 0x2;

    /** Refers to the third row of the vehicle only. */
    @CarZoneRow
    public static final int CAR_ZONE_ROW_THIRD = 0x3;

    /** Refers to the all rows, except for {@link CarZone#CAR_ZONE_ROW_FIRST} of the vehicle. */
    @CarZoneRow
    public static final int CAR_ZONE_ROW_EXCLUDE_FIRST = 0x4;

    /**
     * Possible column values.
     *
     */
    @IntDef({
            CAR_ZONE_COLUMN_ALL,
            CAR_ZONE_COLUMN_LEFT,
            CAR_ZONE_COLUMN_CENTER,
            CAR_ZONE_COLUMN_RIGHT,
            CAR_ZONE_COLUMN_DRIVER,
            CAR_ZONE_COLUMN_PASSENGER,
    })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY)
    public @interface CarZoneColumn {
    }

    /** Refers to all the columns in the vehicle. */
    @CarZoneColumn
    public static final int CAR_ZONE_COLUMN_ALL = 0x10;

    /** Refers to the left-most column of the vehicle only. */
    @CarZoneColumn
    public static final int CAR_ZONE_COLUMN_LEFT = 0x20;

    /** Refers to the center column of the vehicle only. */
    @CarZoneColumn
    public static final int CAR_ZONE_COLUMN_CENTER = 0x30;

    /** Refers to the right-most column of the vehicle only. */
    @CarZoneColumn
    public static final int CAR_ZONE_COLUMN_RIGHT = 0x40;

    /**
     * Refers to either {@link CarZone#CAR_ZONE_COLUMN_LEFT} or
     * {@link CarZone#CAR_ZONE_COLUMN_RIGHT}, depending on the vehicle
     * configuration.
     */
    @CarZoneColumn
    public static final int CAR_ZONE_COLUMN_DRIVER = 0x50;

    /**
     * Refers to either {@link CarZone#CAR_ZONE_COLUMN_LEFT} or
     * {@link CarZone#CAR_ZONE_COLUMN_RIGHT}, depending on the vehicle
     * configuration.
     */
    @CarZoneColumn
    public static final int CAR_ZONE_COLUMN_PASSENGER = 0x60;

    /**
     * Refers to the global zone, represented by {@link CarZone#CAR_ZONE_ROW_ALL},
     * {@link CarZone#CAR_ZONE_COLUMN_ALL}.
     */
    public static final CarZone CAR_ZONE_GLOBAL = new CarZone.Builder().build();

    private final int mRow;
    private final int mColumn;

    /** Returns one of the values in CarZoneRow. */
    public @CarZoneRow int getRow() {
        return mRow;
    }

    /** Returns one of the values in CarZoneColumn. */
    public @CarZoneColumn int getColumn() {
        return mColumn;
    }

    /** Constructs a new instance by using {@link Builder}. */
    CarZone(@NonNull Builder builder) {
        mRow = builder.mRow;
        mColumn = builder.mColumn;
    }

    @Override
    public @NonNull String toString() {
        String rowName;
        switch (mRow) {
            case CAR_ZONE_ROW_ALL:
                rowName = "CAR_ZONE_ROW_ALL";
                break;
            case CAR_ZONE_ROW_FIRST:
                rowName = "CAR_ZONE_ROW_FIRST";
                break;
            case CAR_ZONE_ROW_SECOND:
                rowName = "CAR_ZONE_ROW_SECOND";
                break;
            case CAR_ZONE_ROW_THIRD:
                rowName = "CAR_ZONE_ROW_THIRD";
                break;
            case CAR_ZONE_ROW_EXCLUDE_FIRST:
                rowName = "CAR_ZONE_ROW_EXCLUDE_FIRST";
                break;
            default:
                rowName = "UNKNOWN";
        }
        String columnName;
        switch (mColumn) {
            case CAR_ZONE_COLUMN_ALL:
                columnName = "CAR_ZONE_COLUMN_ALL";
                break;
            case CAR_ZONE_COLUMN_LEFT:
                columnName = "CAR_ZONE_COLUMN_LEFT";
                break;
            case CAR_ZONE_COLUMN_CENTER:
                columnName = "CAR_ZONE_COLUMN_CENTER";
                break;
            case CAR_ZONE_COLUMN_RIGHT:
                columnName = "CAR_ZONE_COLUMN_RIGHT";
                break;
            case CAR_ZONE_COLUMN_DRIVER:
                columnName = "CAR_ZONE_COLUMN_DRIVER";
                break;
            case CAR_ZONE_COLUMN_PASSENGER:
                columnName = "CAR_ZONE_COLUMN_PASSENGER";
                break;
            default:
                columnName = "UNKNOWN";
        }
        return "[CarZone row value: " + rowName + ", column value: " + columnName + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mRow, mColumn);
    }

    @Override
    public boolean equals(@Nullable Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof CarZone)) {
            return false;
        }
        CarZone otherZone = (CarZone) object;
        return Objects.equals(mColumn, otherZone.getColumn())
                && Objects.equals(mRow, otherZone.getRow());
    }

    /** Constructs an empty instance, used by serialization code. */
    private CarZone() {
        mRow = 0;
        mColumn = 0;
    }

    /** A builder for instantiating {@link CarZone}. */
    public static final class Builder {
        int mRow = CAR_ZONE_ROW_ALL;
        int mColumn = CAR_ZONE_COLUMN_ALL;

        /**
         * Sets the row value for the {@link CarZone}.
         *
         * <p> The row value should be in the CarZoneRow list.
         */
        public @NonNull Builder setRow(@CarZoneRow int row) {
            this.mRow = row;
            return this;
        }

        /**
         * Sets the column value for the {@link CarZone}.
         *
         * <p> The column value should be in the CarZoneColumn list.
         */
        public @NonNull Builder setColumn(@CarZoneColumn int column) {
            this.mColumn = column;
            return this;
        }

        /**
         * Constructs the {@link CarZone} defined by this builder.
         *
         * <p> {@link CarZoneRow#CAR_ZONE_ROW_ALL} will be used by default if the row value is not
         * set using {@link Builder#setRow(int)}. {@link CarZoneColumn#CAR_ZONE_COLUMN_ALL} will be
         * used by default if the column value is not set {@link Builder#setColumn(int)}.
         */
        public @NonNull CarZone build() {
            return new CarZone(this);
        }
    }
}
