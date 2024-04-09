/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.protolayout.expression;

import static androidx.wear.protolayout.expression.Preconditions.checkNotNull;

import android.annotation.SuppressLint;
import android.os.Build;

import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.expression.AnimationParameterBuilders.AnimationSpec;
import androidx.wear.protolayout.expression.ConditionScopes.ConditionScope;
import androidx.wear.protolayout.expression.DynamicDataBuilders.DynamicDataValue;
import androidx.wear.protolayout.expression.FixedValueBuilders.FixedBool;
import androidx.wear.protolayout.expression.FixedValueBuilders.FixedColor;
import androidx.wear.protolayout.expression.FixedValueBuilders.FixedDuration;
import androidx.wear.protolayout.expression.FixedValueBuilders.FixedFloat;
import androidx.wear.protolayout.expression.FixedValueBuilders.FixedInstant;
import androidx.wear.protolayout.expression.FixedValueBuilders.FixedInt32;
import androidx.wear.protolayout.expression.FixedValueBuilders.FixedString;
import androidx.wear.protolayout.expression.proto.DynamicProto;
import androidx.wear.protolayout.protobuf.CodedInputStream;
import androidx.wear.protolayout.protobuf.CodedOutputStream;
import androidx.wear.protolayout.protobuf.ExtensionRegistryLite;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/** Builders for dynamic primitive types used by layout elements. */
public final class DynamicBuilders {
    private DynamicBuilders() {}

    /** The type of data to provide to a {@link PlatformInt32Source}. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
        PLATFORM_INT32_SOURCE_TYPE_UNDEFINED,
        PLATFORM_INT32_SOURCE_TYPE_CURRENT_HEART_RATE,
        PLATFORM_INT32_SOURCE_TYPE_DAILY_STEP_COUNT
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface PlatformInt32SourceType {}

    /** Undefined source. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int PLATFORM_INT32_SOURCE_TYPE_UNDEFINED = 0;

    /**
     * The user's current heart rate. Note that to use this data source, your app must already have
     * the "BODY_SENSORS" permission granted to it. If this permission is not present, this source
     * type will never yield any data.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int PLATFORM_INT32_SOURCE_TYPE_CURRENT_HEART_RATE = 1;

    /**
     * The user's current daily steps. This is the number of steps they have taken since midnight,
     * and will reset to zero at midnight. Note that to use this data source, your app must already
     * have the "ACTIVITY_RECOGNITION" permission granted to it. If this permission is not present,
     * this source type will never yield any data.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int PLATFORM_INT32_SOURCE_TYPE_DAILY_STEP_COUNT = 2;

    /**
     * The type of arithmetic operation used in {@link ArithmeticInt32Op} and {@link
     * ArithmeticFloatOp}.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
        ARITHMETIC_OP_TYPE_UNDEFINED,
        ARITHMETIC_OP_TYPE_ADD,
        ARITHMETIC_OP_TYPE_SUBTRACT,
        ARITHMETIC_OP_TYPE_MULTIPLY,
        ARITHMETIC_OP_TYPE_DIVIDE,
        ARITHMETIC_OP_TYPE_MODULO
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface ArithmeticOpType {}

    /** Undefined operation type. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int ARITHMETIC_OP_TYPE_UNDEFINED = 0;

    /** Addition. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int ARITHMETIC_OP_TYPE_ADD = 1;

    /** Subtraction. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int ARITHMETIC_OP_TYPE_SUBTRACT = 2;

    /** Multiplication. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int ARITHMETIC_OP_TYPE_MULTIPLY = 3;

    /** Division. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int ARITHMETIC_OP_TYPE_DIVIDE = 4;

    /** Modulus. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int ARITHMETIC_OP_TYPE_MODULO = 5;

    /**
     * Rounding mode to use when converting a float to an int32. If the value is larger than {@link
     * Integer#MAX_VALUE} or smaller than {@link Integer#MIN_VALUE}, the result of this operation
     * will be invalid and will have an invalid value delivered via {@link
     * DynamicTypeValueReceiver<T>#onInvalidate()}.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({ROUND_MODE_UNDEFINED, ROUND_MODE_FLOOR, ROUND_MODE_ROUND, ROUND_MODE_CEILING})
    @Retention(RetentionPolicy.SOURCE)
    @interface FloatToInt32RoundMode {}

    /** An undefined rounding mode. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int ROUND_MODE_UNDEFINED = 0;

    /** Use floor(x) when rounding. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int ROUND_MODE_FLOOR = 1;

    /** Use round(x) when rounding (i.e. rounds to the closest int). */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int ROUND_MODE_ROUND = 2;

    /** Use ceil(x) when rounding. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int ROUND_MODE_CEILING = 3;

    /** The type of comparison used in {@link ComparisonInt32Op} and {@link ComparisonFloatOp}. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
        COMPARISON_OP_TYPE_UNDEFINED,
        COMPARISON_OP_TYPE_EQUALS,
        COMPARISON_OP_TYPE_NOT_EQUALS,
        COMPARISON_OP_TYPE_LESS_THAN,
        COMPARISON_OP_TYPE_LESS_THAN_OR_EQUAL_TO,
        COMPARISON_OP_TYPE_GREATER_THAN,
        COMPARISON_OP_TYPE_GREATER_THAN_OR_EQUAL_TO
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface ComparisonOpType {}

    /** Undefined operation type. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int COMPARISON_OP_TYPE_UNDEFINED = 0;

    /**
     * Equality check (result = LHS == RHS). For floats, for equality check, small epsilon is used,
     * i.e.: (result = abs(LHS - RHS) < epsilon).
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int COMPARISON_OP_TYPE_EQUALS = 1;

    /** Not equal check (result = LHS != RHS). */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int COMPARISON_OP_TYPE_NOT_EQUALS = 2;

    /** Strictly less than (result = LHS < RHS). */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int COMPARISON_OP_TYPE_LESS_THAN = 3;

    /** Less than or equal to (result = LHS <= RHS). */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int COMPARISON_OP_TYPE_LESS_THAN_OR_EQUAL_TO = 4;

    /** Strictly greater than (result = LHS > RHS). */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int COMPARISON_OP_TYPE_GREATER_THAN = 5;

    /** Greater than or equal to (result = LHS >= RHS). */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int COMPARISON_OP_TYPE_GREATER_THAN_OR_EQUAL_TO = 6;

    /** The type of logical operation to carry out in a {@link LogicalBoolOp} operation. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
        LOGICAL_OP_TYPE_UNDEFINED,
        LOGICAL_OP_TYPE_AND,
        LOGICAL_OP_TYPE_OR,
        LOGICAL_OP_TYPE_EQUAL,
        LOGICAL_OP_TYPE_NOT_EQUAL
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface LogicalOpType {}

    /** Undefined operation type. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int LOGICAL_OP_TYPE_UNDEFINED = 0;

    /** Logical AND. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int LOGICAL_OP_TYPE_AND = 1;

    /** Logical OR. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int LOGICAL_OP_TYPE_OR = 2;

    /** Equal check. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int LOGICAL_OP_TYPE_EQUAL = 3;

    /** Not Equal check. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int LOGICAL_OP_TYPE_NOT_EQUAL = 4;

    /** The date-time part to retrieve using ZonedDateTimePartOp. */
    @RequiresSchemaVersion(major = 1, minor = 300)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
        ZONED_DATE_TIME_PART_UNDEFINED,
        ZONED_DATE_TIME_PART_SECOND,
        ZONED_DATE_TIME_PART_MINUTE,
        ZONED_DATE_TIME_PART_HOUR_24H,
        ZONED_DATE_TIME_PART_DAY_OF_WEEK,
        ZONED_DATE_TIME_PART_DAY_OF_MONTH,
        ZONED_DATE_TIME_PART_MONTH,
        ZONED_DATE_TIME_PART_YEAR
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface ZonedDateTimePartType {}

    /** Undefined date-time part type. */
    @RequiresSchemaVersion(major = 1, minor = 300)
    static final int ZONED_DATE_TIME_PART_UNDEFINED = 0;

    /** The second-of-minute field from 0 to 59. */
    @RequiresSchemaVersion(major = 1, minor = 300)
    static final int ZONED_DATE_TIME_PART_SECOND = 1;

    /** The minute-of-hour field from 0 to 59. */
    @RequiresSchemaVersion(major = 1, minor = 300)
    static final int ZONED_DATE_TIME_PART_MINUTE = 2;

    /** The hour-of-day field from 0 to 23. */
    @RequiresSchemaVersion(major = 1, minor = 300)
    static final int ZONED_DATE_TIME_PART_HOUR_24H = 3;

    /** The day-of-week field going from MONDAY (1) to SUNDAY (7). */
    @RequiresSchemaVersion(major = 1, minor = 300)
    static final int ZONED_DATE_TIME_PART_DAY_OF_WEEK = 4;

    /** The day-of-month field from 1 to 31. */
    @RequiresSchemaVersion(major = 1, minor = 300)
    static final int ZONED_DATE_TIME_PART_DAY_OF_MONTH = 5;

    /** The month-of-year field from 1 to 12. */
    @RequiresSchemaVersion(major = 1, minor = 300)
    static final int ZONED_DATE_TIME_PART_MONTH = 6;

    /** The year field. */
    @RequiresSchemaVersion(major = 1, minor = 300)
    static final int ZONED_DATE_TIME_PART_YEAR = 7;

    /** The duration part to retrieve using {@link GetDurationPartOp}. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
        DURATION_PART_TYPE_UNDEFINED,
        DURATION_PART_TYPE_TOTAL_DAYS,
        DURATION_PART_TYPE_TOTAL_HOURS,
        DURATION_PART_TYPE_TOTAL_MINUTES,
        DURATION_PART_TYPE_TOTAL_SECONDS,
        DURATION_PART_TYPE_DAYS,
        DURATION_PART_TYPE_HOURS,
        DURATION_PART_TYPE_MINUTES,
        DURATION_PART_TYPE_SECONDS
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface DurationPartType {}

    /** Undefined duration part type. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int DURATION_PART_TYPE_UNDEFINED = 0;

    /**
     * Total number of days in a duration. The fraction part of the result will be truncated. This
     * is based on the standard definition of a day as 24 hours. Notice that the duration can be
     * negative, in which case total number of days will be also negative.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int DURATION_PART_TYPE_TOTAL_DAYS = 1;

    /**
     * Total number of hours in a duration. The fraction part of the result will be truncated.
     * Notice that the duration can be negative, in which case total number of hours will be also
     * negative.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int DURATION_PART_TYPE_TOTAL_HOURS = 2;

    /**
     * Total number of minutes in a duration. The fraction part of the result will be truncated.
     * Notice that the duration can be negative, in which case total number of minutes will be also
     * negative.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int DURATION_PART_TYPE_TOTAL_MINUTES = 3;

    /**
     * Total number of seconds in a duration. Notice that the duration can be negative, in which
     * case total number of seconds will be also negative.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int DURATION_PART_TYPE_TOTAL_SECONDS = 4;

    /**
     * Number of days part in the duration. This represents the absolute value of the total number
     * of days in the duration based on the 24 hours day definition. The fraction part of the result
     * will be truncated.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int DURATION_PART_TYPE_DAYS = 5;

    /**
     * Number of hours part in the duration. This represents the absolute value of remaining hours
     * when dividing total hours by hours in a day (24 hours).
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int DURATION_PART_TYPE_HOURS = 6;

    /**
     * Number of minutes part in the duration. This represents the absolute value of remaining
     * minutes when dividing total minutes by minutes in an hour (60 minutes).
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int DURATION_PART_TYPE_MINUTES = 7;

    /**
     * Number of seconds part in the duration. This represents the absolute value of remaining
     * seconds when dividing total seconds by seconds in a minute (60 seconds).
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final int DURATION_PART_TYPE_SECONDS = 8;

    /**
     * A dynamic Int32 which sources its data from some platform data source, e.g. from sensors, or
     * the current time.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class PlatformInt32Source implements DynamicInt32 {
        private final DynamicProto.PlatformInt32Source mImpl;
        @Nullable private final Fingerprint mFingerprint;

        PlatformInt32Source(
                DynamicProto.PlatformInt32Source impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the source to load data from. */
        @PlatformInt32SourceType
        public int getSourceType() {
            return mImpl.getSourceType().getNumber();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static PlatformInt32Source fromProto(
                @NonNull DynamicProto.PlatformInt32Source proto,
                @Nullable Fingerprint fingerprint) {
            return new PlatformInt32Source(proto, fingerprint);
        }

        @NonNull
        static PlatformInt32Source fromProto(@NonNull DynamicProto.PlatformInt32Source proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.PlatformInt32Source toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicInt32 toDynamicInt32Proto() {
            return DynamicProto.DynamicInt32.newBuilder().setPlatformSource(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicInt32 toDynamicInt32Proto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicInt32.newBuilder()
                        .setPlatformSource(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicInt32Proto();
        }

        @Override
        @NonNull
        public String toString() {
            return "PlatformInt32Source{" + "sourceType=" + getSourceType() + "}";
        }

        /** Builder for {@link PlatformInt32Source}. */
        public static final class Builder implements DynamicInt32.Builder {
            private final DynamicProto.PlatformInt32Source.Builder mImpl =
                    DynamicProto.PlatformInt32Source.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1355180718);

            public Builder() {}

            /** Sets the source to load data from. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setSourceType(@PlatformInt32SourceType int sourceType) {
                mImpl.setSourceType(DynamicProto.PlatformInt32SourceType.forNumber(sourceType));
                mFingerprint.recordPropertyUpdate(1, sourceType);
                return this;
            }

            @Override
            @NonNull
            public PlatformInt32Source build() {
                return new PlatformInt32Source(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * An arithmetic operation, operating on two Int32 instances. This implements simple binary
     * operations of the form "result = LHS <op> RHS", where the available operation types are
     * described in {@code ArithmeticOpType}.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class ArithmeticInt32Op implements DynamicInt32 {

        private final DynamicProto.ArithmeticInt32Op mImpl;
        @Nullable private final Fingerprint mFingerprint;

        ArithmeticInt32Op(DynamicProto.ArithmeticInt32Op impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets left hand side of the arithmetic operation. */
        @Nullable
        public DynamicInt32 getInputLhs() {
            if (mImpl.hasInputLhs()) {
                return DynamicBuilders.dynamicInt32FromProto(mImpl.getInputLhs());
            } else {
                return null;
            }
        }

        /** Gets right hand side of the arithmetic operation. */
        @Nullable
        public DynamicInt32 getInputRhs() {
            if (mImpl.hasInputRhs()) {
                return DynamicBuilders.dynamicInt32FromProto(mImpl.getInputRhs());
            } else {
                return null;
            }
        }

        /** Gets the type of operation to carry out. */
        @ArithmeticOpType
        public int getOperationType() {
            return mImpl.getOperationType().getNumber();
        }

        /** */
        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static ArithmeticInt32Op fromProto(
                @NonNull DynamicProto.ArithmeticInt32Op proto, @Nullable Fingerprint fingerprint) {
            return new ArithmeticInt32Op(proto, fingerprint);
        }

        @NonNull
        static ArithmeticInt32Op fromProto(@NonNull DynamicProto.ArithmeticInt32Op proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.ArithmeticInt32Op toProto() {
            return mImpl;
        }

        /** */
        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicInt32 toDynamicInt32Proto() {
            return DynamicProto.DynamicInt32.newBuilder().setArithmeticOperation(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicInt32 toDynamicInt32Proto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicInt32.newBuilder()
                        .setArithmeticOperation(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicInt32Proto();
        }

        @Override
        @NonNull
        public String toString() {
            return "ArithmeticInt32Op{"
                    + "inputLhs="
                    + getInputLhs()
                    + ", inputRhs="
                    + getInputRhs()
                    + ", operationType="
                    + getOperationType()
                    + "}";
        }

        /** Builder for {@link ArithmeticInt32Op}. */
        public static final class Builder implements DynamicInt32.Builder {

            private final DynamicProto.ArithmeticInt32Op.Builder mImpl =
                    DynamicProto.ArithmeticInt32Op.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-2012727925);

            public Builder() {}

            /** Sets left hand side of the arithmetic operation. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setInputLhs(@NonNull DynamicInt32 inputLhs) {
                mImpl.setInputLhs(inputLhs.toDynamicInt32Proto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(inputLhs.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets right hand side of the arithmetic operation. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setInputRhs(@NonNull DynamicInt32 inputRhs) {
                mImpl.setInputRhs(inputRhs.toDynamicInt32Proto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(inputRhs.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the type of operation to carry out. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setOperationType(@ArithmeticOpType int operationType) {
                mImpl.setOperationType(DynamicProto.ArithmeticOpType.forNumber(operationType));
                mFingerprint.recordPropertyUpdate(3, operationType);
                return this;
            }

            @Override
            @NonNull
            public ArithmeticInt32Op build() {
                return new ArithmeticInt32Op(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A dynamic Int32 which sources its data from the tile's state. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class StateInt32Source implements DynamicInt32 {
        private final DynamicProto.StateInt32Source mImpl;
        @Nullable private final Fingerprint mFingerprint;

        StateInt32Source(DynamicProto.StateInt32Source impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the key in the state to bind to. */
        @NonNull
        public String getSourceKey() {
            return mImpl.getSourceKey();
        }

        /** Gets the namespace for the state key. */
        @NonNull
        public String getSourceNamespace() {
            return mImpl.getSourceNamespace();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static StateInt32Source fromProto(
                @NonNull DynamicProto.StateInt32Source proto, @Nullable Fingerprint fingerprint) {
            return new StateInt32Source(proto, fingerprint);
        }

        @NonNull
        static StateInt32Source fromProto(@NonNull DynamicProto.StateInt32Source proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.StateInt32Source toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicInt32 toDynamicInt32Proto() {
            return DynamicProto.DynamicInt32.newBuilder().setStateSource(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicInt32 toDynamicInt32Proto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicInt32.newBuilder()
                        .setStateSource(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicInt32Proto();
        }

        @Override
        @NonNull
        public String toString() {
            return "StateInt32Source{"
                    + "sourceKey="
                    + getSourceKey()
                    + ", sourceNamespace="
                    + getSourceNamespace()
                    + "}";
        }

        /** Builder for {@link StateInt32Source}. */
        public static final class Builder implements DynamicInt32.Builder {
            private final DynamicProto.StateInt32Source.Builder mImpl =
                    DynamicProto.StateInt32Source.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(58614749);

            public Builder() {}

            /** Sets the key in the state to bind to. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setSourceKey(@NonNull String sourceKey) {
                mImpl.setSourceKey(sourceKey);
                mFingerprint.recordPropertyUpdate(1, sourceKey.hashCode());
                return this;
            }

            /** Sets the name space for the state key. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setSourceNamespace(@NonNull String sourceNamespace) {
                mImpl.setSourceNamespace(sourceNamespace);
                mFingerprint.recordPropertyUpdate(2, sourceNamespace.hashCode());
                return this;
            }

            @Override
            @NonNull
            public StateInt32Source build() {
                return new StateInt32Source(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A conditional operator which yields an integer depending on the boolean operand. This
     * implements "int result = condition ? value_if_true : value_if_false".
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class ConditionalInt32Op implements DynamicInt32 {

        private final DynamicProto.ConditionalInt32Op mImpl;
        @Nullable private final Fingerprint mFingerprint;

        ConditionalInt32Op(
                DynamicProto.ConditionalInt32Op impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the condition to use. */
        @Nullable
        public DynamicBool getCondition() {
            if (mImpl.hasCondition()) {
                return DynamicBuilders.dynamicBoolFromProto(mImpl.getCondition());
            } else {
                return null;
            }
        }

        /** Gets the integer to yield if condition is true. */
        @Nullable
        public DynamicInt32 getValueIfTrue() {
            if (mImpl.hasValueIfTrue()) {
                return DynamicBuilders.dynamicInt32FromProto(mImpl.getValueIfTrue());
            } else {
                return null;
            }
        }

        /** Gets the integer to yield if condition is false. */
        @Nullable
        public DynamicInt32 getValueIfFalse() {
            if (mImpl.hasValueIfFalse()) {
                return DynamicBuilders.dynamicInt32FromProto(mImpl.getValueIfFalse());
            } else {
                return null;
            }
        }

        /** */
        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static ConditionalInt32Op fromProto(
                @NonNull DynamicProto.ConditionalInt32Op proto, @Nullable Fingerprint fingerprint) {
            return new ConditionalInt32Op(proto, fingerprint);
        }

        @NonNull
        static ConditionalInt32Op fromProto(@NonNull DynamicProto.ConditionalInt32Op proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.ConditionalInt32Op toProto() {
            return mImpl;
        }

        /** */
        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicInt32 toDynamicInt32Proto() {
            return DynamicProto.DynamicInt32.newBuilder().setConditionalOp(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicInt32 toDynamicInt32Proto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicInt32.newBuilder()
                        .setConditionalOp(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicInt32Proto();
        }

        @Override
        @NonNull
        public String toString() {
            return "ConditionalInt32Op{"
                    + "condition="
                    + getCondition()
                    + ", valueIfTrue="
                    + getValueIfTrue()
                    + ", valueIfFalse="
                    + getValueIfFalse()
                    + "}";
        }

        /** Builder for {@link ConditionalInt32Op}. */
        public static final class Builder implements DynamicInt32.Builder {

            private final DynamicProto.ConditionalInt32Op.Builder mImpl =
                    DynamicProto.ConditionalInt32Op.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1444834226);

            public Builder() {}

            /** Sets the condition to use. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setCondition(@NonNull DynamicBool condition) {
                mImpl.setCondition(condition.toDynamicBoolProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(condition.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the integer to yield if condition is true. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setValueIfTrue(@NonNull DynamicInt32 valueIfTrue) {
                mImpl.setValueIfTrue(valueIfTrue.toDynamicInt32Proto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(valueIfTrue.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the integer to yield if condition is false. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setValueIfFalse(@NonNull DynamicInt32 valueIfFalse) {
                mImpl.setValueIfFalse(valueIfFalse.toDynamicInt32Proto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(valueIfFalse.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public ConditionalInt32Op build() {
                return new ConditionalInt32Op(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A conditional operator which yields a float depending on the boolean operand. This implements
     * "float result = condition ? value_if_true : value_if_false".
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class ConditionalFloatOp implements DynamicFloat {

        private final DynamicProto.ConditionalFloatOp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        ConditionalFloatOp(
                DynamicProto.ConditionalFloatOp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the condition to use. */
        @Nullable
        public DynamicBool getCondition() {
            if (mImpl.hasCondition()) {
                return DynamicBuilders.dynamicBoolFromProto(mImpl.getCondition());
            } else {
                return null;
            }
        }

        /** Gets the float to yield if condition is true. */
        @Nullable
        public DynamicFloat getValueIfTrue() {
            if (mImpl.hasValueIfTrue()) {
                return DynamicBuilders.dynamicFloatFromProto(mImpl.getValueIfTrue());
            } else {
                return null;
            }
        }

        /** Gets the float to yield if condition is false. */
        @Nullable
        public DynamicFloat getValueIfFalse() {
            if (mImpl.hasValueIfFalse()) {
                return DynamicBuilders.dynamicFloatFromProto(mImpl.getValueIfFalse());
            } else {
                return null;
            }
        }

        /** */
        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static ConditionalFloatOp fromProto(
                @NonNull DynamicProto.ConditionalFloatOp proto, @Nullable Fingerprint fingerprint) {
            return new ConditionalFloatOp(proto, fingerprint);
        }

        @NonNull
        static ConditionalFloatOp fromProto(@NonNull DynamicProto.ConditionalFloatOp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.ConditionalFloatOp toProto() {
            return mImpl;
        }

        /** */
        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicFloat toDynamicFloatProto() {
            return DynamicProto.DynamicFloat.newBuilder().setConditionalOp(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicFloat toDynamicFloatProto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicFloat.newBuilder()
                        .setConditionalOp(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicFloatProto();
        }

        @Override
        @NonNull
        public String toString() {
            return "ConditionalFloatOp{"
                    + "condition="
                    + getCondition()
                    + ", valueIfTrue="
                    + getValueIfTrue()
                    + ", valueIfFalse="
                    + getValueIfFalse()
                    + "}";
        }

        /** Builder for {@link ConditionalFloatOp}. */
        public static final class Builder implements DynamicFloat.Builder {

            private final DynamicProto.ConditionalFloatOp.Builder mImpl =
                    DynamicProto.ConditionalFloatOp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1968171153);

            public Builder() {}

            /** Sets the condition to use. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setCondition(@NonNull DynamicBool condition) {
                mImpl.setCondition(condition.toDynamicBoolProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(condition.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the float to yield if condition is true. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setValueIfTrue(@NonNull DynamicFloat valueIfTrue) {
                mImpl.setValueIfTrue(valueIfTrue.toDynamicFloatProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(valueIfTrue.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the float to yield if condition is false. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setValueIfFalse(@NonNull DynamicFloat valueIfFalse) {
                mImpl.setValueIfFalse(valueIfFalse.toDynamicFloatProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(valueIfFalse.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public ConditionalFloatOp build() {
                return new ConditionalFloatOp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** Converts a Float to an Int32, with a customizable rounding mode. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class FloatToInt32Op implements DynamicInt32 {
        private final DynamicProto.FloatToInt32Op mImpl;
        @Nullable private final Fingerprint mFingerprint;

        FloatToInt32Op(DynamicProto.FloatToInt32Op impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the float to round. */
        @Nullable
        public DynamicFloat getInput() {
            if (mImpl.hasInput()) {
                return DynamicBuilders.dynamicFloatFromProto(mImpl.getInput());
            } else {
                return null;
            }
        }

        /** Gets the rounding mode to use. Defaults to ROUND_MODE_FLOOR if not specified. */
        @FloatToInt32RoundMode
        public int getRoundMode() {
            return mImpl.getRoundMode().getNumber();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static FloatToInt32Op fromProto(
                @NonNull DynamicProto.FloatToInt32Op proto, @Nullable Fingerprint fingerprint) {
            return new FloatToInt32Op(proto, fingerprint);
        }

        @NonNull
        static FloatToInt32Op fromProto(@NonNull DynamicProto.FloatToInt32Op proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.FloatToInt32Op toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicInt32 toDynamicInt32Proto() {
            return DynamicProto.DynamicInt32.newBuilder().setFloatToInt(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicInt32 toDynamicInt32Proto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicInt32.newBuilder()
                        .setFloatToInt(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicInt32Proto();
        }

        @Override
        @NonNull
        public String toString() {
            return "FloatToInt32Op{"
                    + "input="
                    + getInput()
                    + ", roundMode="
                    + getRoundMode()
                    + "}";
        }

        /** Builder for {@link FloatToInt32Op}. */
        public static final class Builder implements DynamicInt32.Builder {
            private final DynamicProto.FloatToInt32Op.Builder mImpl =
                    DynamicProto.FloatToInt32Op.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1272973414);

            public Builder() {}

            /** Sets the float to round. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setInput(@NonNull DynamicFloat input) {
                mImpl.setInput(input.toDynamicFloatProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(input.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the rounding mode to use. Defaults to ROUND_MODE_FLOOR if not specified. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setRoundMode(@FloatToInt32RoundMode int roundMode) {
                mImpl.setRoundMode(DynamicProto.FloatToInt32RoundMode.forNumber(roundMode));
                mFingerprint.recordPropertyUpdate(2, roundMode);
                return this;
            }

            @Override
            @NonNull
            public FloatToInt32Op build() {
                return new FloatToInt32Op(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A static interpolation node, between two fixed int32 values. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class AnimatableFixedInt32 implements DynamicInt32 {
        private final DynamicProto.AnimatableFixedInt32 mImpl;
        @Nullable private final Fingerprint mFingerprint;

        AnimatableFixedInt32(
                DynamicProto.AnimatableFixedInt32 impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value to start animating from. */
        public int getFromValue() {
            return mImpl.getFromValue();
        }

        /** Gets the value to animate to. */
        public int getToValue() {
            return mImpl.getToValue();
        }

        /** Gets the animation parameters for duration, delay, etc. */
        @Nullable
        public AnimationSpec getAnimationSpec() {
            if (mImpl.hasAnimationSpec()) {
                return AnimationSpec.fromProto(mImpl.getAnimationSpec());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static AnimatableFixedInt32 fromProto(
                @NonNull DynamicProto.AnimatableFixedInt32 proto,
                @Nullable Fingerprint fingerprint) {
            return new AnimatableFixedInt32(proto, fingerprint);
        }

        @NonNull
        static AnimatableFixedInt32 fromProto(@NonNull DynamicProto.AnimatableFixedInt32 proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.AnimatableFixedInt32 toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicInt32 toDynamicInt32Proto() {
            return DynamicProto.DynamicInt32.newBuilder().setAnimatableFixed(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicInt32 toDynamicInt32Proto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicInt32.newBuilder()
                        .setAnimatableFixed(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicInt32Proto();
        }

        @Override
        @NonNull
        public String toString() {
            return "AnimatableFixedInt32{"
                    + "fromValue="
                    + getFromValue()
                    + ", toValue="
                    + getToValue()
                    + ", animationSpec="
                    + getAnimationSpec()
                    + "}";
        }

        /** Builder for {@link AnimatableFixedInt32}. */
        public static final class Builder implements DynamicInt32.Builder {
            private final DynamicProto.AnimatableFixedInt32.Builder mImpl =
                    DynamicProto.AnimatableFixedInt32.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1831435966);

            public Builder() {}

            /** Sets the value to start animating from. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public AnimatableFixedInt32.Builder setFromValue(int fromValue) {
                mImpl.setFromValue(fromValue);
                mFingerprint.recordPropertyUpdate(1, fromValue);
                return this;
            }

            /** Sets the value to animate to. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public AnimatableFixedInt32.Builder setToValue(int toValue) {
                mImpl.setToValue(toValue);
                mFingerprint.recordPropertyUpdate(2, toValue);
                return this;
            }

            /** Sets the animation parameters for duration, delay, etc. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setAnimationSpec(@NonNull AnimationSpec animationSpec) {
                mImpl.setAnimationSpec(animationSpec.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(animationSpec.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public AnimatableFixedInt32 build() {
                return new AnimatableFixedInt32(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A dynamic interpolation node. This will watch the value of its input and, when the first
     * update arrives, immediately emit that value. On subsequent updates, it will animate between
     * the old and new values.
     *
     * <p>If this node receives an invalid value (e.g. as a result of an upstream node having no
     * value), then it will emit a single invalid value, and forget its "stored" value. The next
     * valid value that arrives is then used as the "first" value again.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class AnimatableDynamicInt32 implements DynamicInt32 {
        private final DynamicProto.AnimatableDynamicInt32 mImpl;
        @Nullable private final Fingerprint mFingerprint;

        AnimatableDynamicInt32(
                DynamicProto.AnimatableDynamicInt32 impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value to watch, and animate when it changes. */
        @Nullable
        public DynamicInt32 getInput() {
            if (mImpl.hasInput()) {
                return dynamicInt32FromProto(mImpl.getInput());
            } else {
                return null;
            }
        }

        /** Gets the animation parameters for duration, delay, etc. */
        @Nullable
        public AnimationSpec getAnimationSpec() {
            if (mImpl.hasAnimationSpec()) {
                return AnimationSpec.fromProto(mImpl.getAnimationSpec());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static AnimatableDynamicInt32 fromProto(
                @NonNull DynamicProto.AnimatableDynamicInt32 proto,
                @Nullable Fingerprint fingerprint) {
            return new AnimatableDynamicInt32(proto, fingerprint);
        }

        @NonNull
        static AnimatableDynamicInt32 fromProto(
                @NonNull DynamicProto.AnimatableDynamicInt32 proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.AnimatableDynamicInt32 toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicInt32 toDynamicInt32Proto() {
            return DynamicProto.DynamicInt32.newBuilder().setAnimatableDynamic(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicInt32 toDynamicInt32Proto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicInt32.newBuilder()
                        .setAnimatableDynamic(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicInt32Proto();
        }

        @Override
        @NonNull
        public String toString() {
            return "AnimatableDynamicInt32{"
                    + "input="
                    + getInput()
                    + ", animationSpec="
                    + getAnimationSpec()
                    + "}";
        }

        /** Builder for {@link AnimatableDynamicInt32}. */
        public static final class Builder implements DynamicInt32.Builder {
            private final DynamicProto.AnimatableDynamicInt32.Builder mImpl =
                    DynamicProto.AnimatableDynamicInt32.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1554674954);

            public Builder() {}

            /** Sets the value to watch, and animate when it changes. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public AnimatableDynamicInt32.Builder setInput(@NonNull DynamicInt32 input) {
                mImpl.setInput(input.toDynamicInt32Proto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(input.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the animation parameters for duration, delay, etc. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setAnimationSpec(@NonNull AnimationSpec animationSpec) {
                mImpl.setAnimationSpec(animationSpec.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(animationSpec.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public AnimatableDynamicInt32 build() {
                return new AnimatableDynamicInt32(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * Interface defining a dynamic int32 type.
     *
     * <p>It offers a set of helper methods for creating arithmetic and logical expressions, e.g.
     * {@link #plus(int)}, {@link #times(int)}, {@link #eq(int)}, etc. These helper methods produce
     * expression trees based on the order in which they were called in an expression. Thus, no
     * operator precedence rules are applied.
     *
     * <p>For example the following expression is equivalent to {@code result = ((a + b)*c)/d }:
     *
     * <pre>{@code
     * a.plus(b).times(c).div(d);
     * }</pre>
     *
     * More complex expressions can be created by nesting expressions. For example the following
     * expression is equivalent to {@code result = (a + b)*(c - d) }:
     *
     * <pre>{@code
     * (a.plus(b)).times(c.minus(d));
     * }</pre>
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public interface DynamicInt32 extends DynamicType {
        /** Get the protocol buffer representation of this object, without fingerprint. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        DynamicProto.DynamicInt32 toDynamicInt32Proto();

        /** Get the protocol buffer representation of this object, with or without fingerprint. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        DynamicProto.DynamicInt32 toDynamicInt32Proto(boolean withFingerprint);

        /**
         * Creates a {@link DynamicInt32} from a byte array generated by {@link
         * #toDynamicInt32ByteArray()}.
         *
         * @throws IllegalArgumentException if the byte array does not contain a valid serialization
         */
        @NonNull
        static DynamicInt32 fromByteArray(@NonNull byte[] byteArray) {
            return fromByteArray(byteArray, 0, byteArray.length);
        }

        /**
         * Creates a {@link DynamicInt32} from the provided byte array at the provided offset and
         * length, that was generated by one of the {@link #toDynamicInt32ByteArray} overloads.
         *
         * @throws IllegalArgumentException if the byte array does not contain a valid serialization
         *     in the provided offset and length
         */
        @NonNull
        static DynamicInt32 fromByteArray(@NonNull byte[] byteArray, int offset, int length) {
            try {
                return dynamicInt32FromProto(
                        DynamicProto.DynamicInt32.parseFrom(
                                CodedInputStream.newInstance(byteArray, offset, length),
                                ExtensionRegistryLite.getEmptyRegistry()));
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        "Byte array could not be parsed into DynamicInt32", e);
            }
        }

        /**
         * Serializes the {@link DynamicInt32} into a new byte array that can later be used with
         * {@link #fromByteArray(byte[])}.
         */
        @NonNull
        default byte[] toDynamicInt32ByteArray() {
            return toDynamicInt32Proto(/* withFingerprint= */ true).toByteArray();
        }

        /**
         * Serializes the {@link DynamicInt32} into the provided byte array, returning the amount of
         * bytes written, that can later be used with {@code DynamicInt32.fromByteArray(byteArray,
         * 0, bytesWritten)}.
         *
         * @throws IllegalArgumentException if the byte array is too small
         */
        default int toDynamicInt32ByteArray(@NonNull byte[] byteArray) {
            return toDynamicInt32ByteArray(byteArray, 0, byteArray.length);
        }

        /**
         * Serializes the {@link DynamicInt32} into the provided byte array, returning the amount of
         * bytes written, limited by the provided offset and length, that can later be used with
         * {@code DynamicInt32.fromByteArray(byteArray, offset, bytesWritten)}.
         *
         * @throws IllegalArgumentException if the byte array is too small
         */
        default int toDynamicInt32ByteArray(@NonNull byte[] byteArray, int offset, int length) {
            CodedOutputStream stream = CodedOutputStream.newInstance(byteArray, offset, length);
            try {
                toDynamicInt32Proto(/* withFingerprint= */ true).writeTo(stream);
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        "Provided byte array not large enough to contain this DynamicInt32", e);
            }
            return stream.getTotalBytesWritten();
        }

        /** Creates a constant-valued {@link DynamicInt32}. */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        static DynamicInt32 constant(int constant) {
            return new FixedInt32.Builder().setValue(constant).build();
        }

        /**
         * Creates a {@link DynamicInt32} that is bound to the value of an item of the State.
         *
         * @param dynamicDataKey The source key to a {@link DynamicDataValue} with an int value.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        static DynamicInt32 from(@NonNull DynamicDataKey<DynamicInt32> dynamicDataKey) {
            return new StateInt32Source.Builder()
                    .setSourceKey(dynamicDataKey.getKey())
                    .setSourceNamespace(dynamicDataKey.getNamespace())
                    .build();
        }

        /**
         * Creates a {@link DynamicInt32} which will animate from {@code start} to {@code end}.
         *
         * @param start The start value of the range.
         * @param end The end value of the range.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @NonNull
        static DynamicInt32 animate(int start, int end) {
            return new AnimatableFixedInt32.Builder().setFromValue(start).setToValue(end).build();
        }

        /**
         * Creates a {@link DynamicInt32} which will animate from {@code start} to {@code end} with
         * the given animation parameters.
         *
         * @param start The start value of the range.
         * @param end The end value of the range.
         * @param animationSpec The animation parameters.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @NonNull
        static DynamicInt32 animate(int start, int end, @NonNull AnimationSpec animationSpec) {
            return new AnimatableFixedInt32.Builder()
                    .setFromValue(start)
                    .setToValue(end)
                    .setAnimationSpec(animationSpec)
                    .build();
        }

        /**
         * Creates a {@link DynamicInt32} that is bound to the value of an item of the State. Every
         * time the state value changes, this {@link DynamicInt32} will animate from its current
         * value to the new value (from the state).
         *
         * @param dynamicDataKey The source key to a {@link DynamicDataValue} with an int value.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @NonNull
        static DynamicInt32 animate(@NonNull DynamicDataKey<DynamicInt32> dynamicDataKey) {
            return new AnimatableDynamicInt32.Builder().setInput(from(dynamicDataKey)).build();
        }

        /**
         * Creates a {@link DynamicInt32} that is bound to the value of an item of the State. Every
         * time the state value changes, this {@link DynamicInt32} will animate from its current
         * value to the new value (from the state).
         *
         * @param dynamicDataKey The source key to a {@link DynamicDataValue} with an int value
         * @param animationSpec The animation parameters.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @NonNull
        static DynamicInt32 animate(
                @NonNull DynamicDataKey<DynamicInt32> dynamicDataKey,
                @NonNull AnimationSpec animationSpec) {
            return new AnimatableDynamicInt32.Builder()
                    .setInput(from(dynamicDataKey))
                    .setAnimationSpec(animationSpec)
                    .build();
        }

        /**
         * Returns a {@link DynamicInt32} that is bound to the value of this {@link DynamicInt32}
         * and every time its value is changing, it animates from its current value to the new
         * value.
         *
         * @param animationSpec The animation parameters.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @NonNull
        default DynamicInt32 animate(@NonNull AnimationSpec animationSpec) {
            return new AnimatableDynamicInt32.Builder()
                    .setInput(this)
                    .setAnimationSpec(animationSpec)
                    .build();
        }

        /**
         * Returns a {@link DynamicInt32} that is bound to the value of this {@link DynamicInt32}
         * and every time its value is changing, it animates from its current value to the new
         * value.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @NonNull
        default DynamicInt32 animate() {
            return new AnimatableDynamicInt32.Builder().setInput(this).build();
        }

        /**
         * Convert the value represented by this {@link DynamicInt32} into a {@link DynamicFloat}.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @NonNull
        default DynamicFloat asFloat() {
            return new Int32ToFloatOp.Builder().setInput(this).build();
        }

        /**
         * Bind the value of this {@link DynamicInt32} to the result of a conditional expression.
         * This will use the value given in either {@link ConditionScope#use} or {@link
         * ConditionScopes.IfTrueScope#elseUse} depending on the value yielded from {@code
         * condition}.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @NonNull
        static ConditionScope<DynamicInt32, Integer> onCondition(@NonNull DynamicBool condition) {
            return new ConditionScopes.ConditionScope<>(
                    (trueValue, falseValue) ->
                            new ConditionalInt32Op.Builder()
                                    .setCondition(condition)
                                    .setValueIfTrue(trueValue)
                                    .setValueIfFalse(falseValue)
                                    .build(),
                    DynamicInt32::constant);
        }

        /**
         * Creates a {@link DynamicInt32} containing the result of adding another {@link
         * DynamicInt32} to this {@link DynamicInt32}; As an example, the following is equal to
         * {@code DynamicInt32.constant(13)}
         *
         * <pre>
         *   DynamicInt32.constant(7).plus(DynamicInt32.constant(6));
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicInt32} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicInt32} containing the result of the operation.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @SuppressWarnings("KotlinOperator")
        @NonNull
        default DynamicInt32 plus(@NonNull DynamicInt32 other) {
            return new ArithmeticInt32Op.Builder()
                    .setInputLhs(this)
                    .setInputRhs(other)
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_ADD)
                    .build();
        }

        /**
         * Creates a {@link DynamicFlaot} containing the result of adding a {@link DynamicFloat} to
         * this {@link DynamicInt32}; As an example, the following is equal to {@code
         * DynamicFloat.constant(13.5f)}
         *
         * <pre>
         *   DynamicInt32.constant(7).plus(DynamicFloat.constant(6.5f));
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicFloat} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicFloat} containing the result of the operation.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @SuppressWarnings("KotlinOperator")
        @NonNull
        default DynamicFloat plus(@NonNull DynamicFloat other) {
            return new ArithmeticFloatOp.Builder()
                    .setInputLhs(this.asFloat())
                    .setInputRhs(other)
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_ADD)
                    .build();
        }

        /**
         * Creates a {@link DynamicInt32} containing the result of adding an integer to this {@link
         * DynamicInt32}; As an example, the following is equal to {@code DynamicInt32.constant(13)}
         *
         * <pre>
         *   DynamicInt32.constant(7).plus(6);
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicInt32} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicInt32} containing the result of the operation.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @SuppressWarnings("KotlinOperator")
        @NonNull
        default DynamicInt32 plus(int other) {
            return new ArithmeticInt32Op.Builder()
                    .setInputLhs(this)
                    .setInputRhs(constant(other))
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_ADD)
                    .build();
        }

        /**
         * Creates a {@link DynamicFlaot} containing the result of adding a float to this {@link
         * DynamicInt32}; As an example, the following is equal to {@code
         * DynamicFloat.constant(13.5f)}
         *
         * <pre>
         *   DynamicInt32.constant(7).plus(6.5f);
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicFloat} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicFloat} containing the result of the operation.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @SuppressWarnings("KotlinOperator")
        @NonNull
        default DynamicFloat plus(float other) {
            return new ArithmeticFloatOp.Builder()
                    .setInputLhs(this.asFloat())
                    .setInputRhs(DynamicFloat.constant(other))
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_ADD)
                    .build();
        }

        /**
         * Creates a {@link DynamicInt32} containing the result of subtracting another {@link
         * DynamicInt32} from this {@link DynamicInt32}; As an example, the following is equal to
         * {@code DynamicInt32.constant(2)}
         *
         * <pre>
         *   DynamicInt32.constant(7).minus(DynamicInt32.constant(5));
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicInt32} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicInt32} containing the result of the operation.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @SuppressWarnings("KotlinOperator")
        @NonNull
        default DynamicInt32 minus(@NonNull DynamicInt32 other) {
            return new ArithmeticInt32Op.Builder()
                    .setInputLhs(this)
                    .setInputRhs(other)
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_SUBTRACT)
                    .build();
        }

        /**
         * Creates a {@link DynamicFloat} containing the result of subtracting a {@link
         * DynamicFloat} from this {@link DynamicInt32}; As an example, the following is equal to
         * {@code DynamicFloat.constant(1.5f)}
         *
         * <pre>
         *   DynamicInt32.constant(7).minus(DynamicFloat.constant(5.5f));
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicFloat} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicFloat} containing the result of the operation.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @SuppressWarnings("KotlinOperator")
        @NonNull
        default DynamicFloat minus(@NonNull DynamicFloat other) {
            return new ArithmeticFloatOp.Builder()
                    .setInputLhs(this.asFloat())
                    .setInputRhs(other)
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_SUBTRACT)
                    .build();
        }

        /**
         * Creates a {@link DynamicInt32} containing the result of subtracting an integer from this
         * {@link DynamicInt32}; As an example, the following is equal to {@code
         * DynamicInt32.constant(2)}
         *
         * <pre>
         *   DynamicInt32.constant(7).minus(5);
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicInt32} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicInt32} containing the result of the operation.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @SuppressWarnings("KotlinOperator")
        @NonNull
        default DynamicInt32 minus(int other) {
            return new ArithmeticInt32Op.Builder()
                    .setInputLhs(this)
                    .setInputRhs(constant(other))
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_SUBTRACT)
                    .build();
        }

        /**
         * Creates a {@link DynamicFloat} containing the result of subtracting a float from this
         * {@link DynamicInt32}; As an example, the following is equal to {@code
         * DynamicFloat.constant(1.5f)}
         *
         * <pre>
         *   DynamicInt32.constant(7).minus(5.5f);
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicFloat} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicFloat} containing the result of the operation.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @SuppressWarnings("KotlinOperator")
        @NonNull
        default DynamicFloat minus(float other) {
            return new ArithmeticFloatOp.Builder()
                    .setInputLhs(this.asFloat())
                    .setInputRhs(DynamicFloat.constant(other))
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_SUBTRACT)
                    .build();
        }

        /**
         * Creates a {@link DynamicInt32} containing the result of multiplying this {@link
         * DynamicInt32} by another {@link DynamicInt32}; As an example, the following is equal to
         * {@code DynamicInt32.constant(35)}
         *
         * <pre>
         *   DynamicInt32.constant(7).times(DynamicInt32.constant(5));
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicInt32} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicInt32} containing the result of the operation.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @SuppressWarnings("KotlinOperator")
        @NonNull
        default DynamicInt32 times(@NonNull DynamicInt32 other) {
            return new ArithmeticInt32Op.Builder()
                    .setInputLhs(this)
                    .setInputRhs(other)
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_MULTIPLY)
                    .build();
        }

        /**
         * Creates a {@link DynamicFloat} containing the result of multiplying this {@link
         * DynamicInt32} by a {@link DynamicFloat}; As an example, the following is equal to {@code
         * DynamicFloat.constant(38.5f)}
         *
         * <pre>
         *   DynamicInt32.constant(7).times(DynamicFloat.constant(5.5f));
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicFloat} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicFloat} containing the result of the operation.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @SuppressWarnings("KotlinOperator")
        @NonNull
        default DynamicFloat times(@NonNull DynamicFloat other) {
            return new ArithmeticFloatOp.Builder()
                    .setInputLhs(this.asFloat())
                    .setInputRhs(other)
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_MULTIPLY)
                    .build();
        }

        /**
         * Creates a {@link DynamicInt32} containing the result of multiplying this {@link
         * DynamicInt32} by an integer; As an example, the following is equal to {@code
         * DynamicInt32.constant(35)}
         *
         * <pre>
         *   DynamicInt32.constant(7).times(5);
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicInt32} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicInt32} containing the result of the operation.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @SuppressWarnings("KotlinOperator")
        @NonNull
        default DynamicInt32 times(int other) {
            return new ArithmeticInt32Op.Builder()
                    .setInputLhs(this)
                    .setInputRhs(constant(other))
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_MULTIPLY)
                    .build();
        }

        /**
         * Creates a {@link DynamicFloat} containing the result of multiplying this {@link
         * DynamicInt32} by a float; As an example, the following is equal to {@code
         * DynamicFloat.constant(38.5f)}
         *
         * <pre>
         *   DynamicInt32.constant(7).times(5.5f);
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicFloat} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicFloat} containing the result of the operation.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @SuppressWarnings("KotlinOperator")
        @NonNull
        default DynamicFloat times(float other) {
            return new ArithmeticFloatOp.Builder()
                    .setInputLhs(this.asFloat())
                    .setInputRhs(DynamicFloat.constant(other))
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_MULTIPLY)
                    .build();
        }

        /**
         * Creates a {@link DynamicInt32} containing the result of dividing this {@link
         * DynamicInt32} by another {@link DynamicInt32}; As an example, the following is equal to
         * {@code DynamicInt32.constant(1)}
         *
         * <pre>
         *   DynamicInt32.constant(7).div(DynamicInt32.constant(5));
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicInt32} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicInt32} containing the result of the operation.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @SuppressWarnings("KotlinOperator")
        @NonNull
        default DynamicInt32 div(@NonNull DynamicInt32 other) {
            return new ArithmeticInt32Op.Builder()
                    .setInputLhs(this)
                    .setInputRhs(other)
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_DIVIDE)
                    .build();
        }

        /**
         * Creates a {@link DynamicFloat} containing the result of dividing this {@link
         * DynamicInt32} by a {@link DynamicFloat}; As an example, the following is equal to {@code
         * DynamicFloat.constant(1.4f)}
         *
         * <pre>
         *   DynamicInt32.constant(7).div(DynamicFloat.constant(5f));
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicFloat} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicFloat} containing the result of the operation.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @SuppressWarnings("KotlinOperator")
        @NonNull
        default DynamicFloat div(@NonNull DynamicFloat other) {
            return new ArithmeticFloatOp.Builder()
                    .setInputLhs(this.asFloat())
                    .setInputRhs(other)
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_DIVIDE)
                    .build();
        }

        /**
         * Creates a {@link DynamicInt32} containing the result of dividing this {@link
         * DynamicInt32} by an integer; As an example, the following is equal to {@code
         * DynamicInt32.constant(1)}
         *
         * <pre>
         *   DynamicInt32.constant(7).div(5);
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicInt32} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicInt32} containing the result of the operation.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @SuppressWarnings("KotlinOperator")
        @NonNull
        default DynamicInt32 div(int other) {
            return new ArithmeticInt32Op.Builder()
                    .setInputLhs(this)
                    .setInputRhs(constant(other))
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_DIVIDE)
                    .build();
        }

        /**
         * Creates a {@link DynamicFloat} containing the result of dividing this {@link
         * DynamicInt32} by a float; As an example, the following is equal to {@code
         * DynamicFloat.constant(1.4f)}
         *
         * <pre>
         *   DynamicInt32.constant(7).div(5f);
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicFloat} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicFloat} containing the result of the operation.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @SuppressWarnings("KotlinOperator")
        @NonNull
        default DynamicFloat div(float other) {
            return new ArithmeticFloatOp.Builder()
                    .setInputLhs(this.asFloat())
                    .setInputRhs(DynamicFloat.constant(other))
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_DIVIDE)
                    .build();
        }

        /**
         * Creates a {@link DynamicInt32} containing the reminder of dividing this {@link
         * DynamicInt32} by another {@link DynamicInt32}; As an example, the following is equal to
         * {@code DynamicInt32.constant(2)}
         *
         * <pre>
         *   DynamicInt32.constant(7).rem(DynamicInt32.constant(5));
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicInt32} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicInt32} containing the result of the operation.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @SuppressWarnings("KotlinOperator")
        @NonNull
        default DynamicInt32 rem(@NonNull DynamicInt32 other) {
            return new ArithmeticInt32Op.Builder()
                    .setInputLhs(this)
                    .setInputRhs(other)
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_MODULO)
                    .build();
        }

        /**
         * Creates a {@link DynamicFloat} containing the reminder of dividing this {@link
         * DynamicInt32} by a {@link DynamicFloat}; As an example, the following is equal to {@code
         * DynamicFloat.constant(1.5f)}
         *
         * <pre>
         *   DynamicInt32.constant(7).rem(DynamicInt32.constant(5.5f));
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicFloat} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicFloat} containing the result of the operation.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @SuppressWarnings("KotlinOperator")
        @NonNull
        default DynamicFloat rem(@NonNull DynamicFloat other) {
            return new ArithmeticFloatOp.Builder()
                    .setInputLhs(this.asFloat())
                    .setInputRhs(other)
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_MODULO)
                    .build();
        }

        /**
         * Creates a {@link DynamicInt32} containing the reminder of dividing this {@link
         * DynamicInt32} by an integer; As an example, the following is equal to {@code
         * DynamicInt32.constant(2)}
         *
         * <pre>
         *   DynamicInt32.constant(7).rem(5);
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicInt32} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicInt32} containing the result of the operation.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @SuppressWarnings("KotlinOperator")
        @NonNull
        default DynamicInt32 rem(int other) {
            return new ArithmeticInt32Op.Builder()
                    .setInputLhs(this)
                    .setInputRhs(constant(other))
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_MODULO)
                    .build();
        }

        /**
         * Creates a {@link DynamicInt32} containing the reminder of dividing this {@link
         * DynamicInt32} by a float; As an example, the following is equal to {@code
         * DynamicFloat.constant(1.5f)}
         *
         * <pre>
         *   DynamicInt32.constant(7).rem(5.5f);
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicFloat} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicFloat} containing the result of the operation.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @SuppressWarnings("KotlinOperator")
        @NonNull
        default DynamicFloat rem(float other) {
            return new ArithmeticFloatOp.Builder()
                    .setInputLhs(this.asFloat())
                    .setInputRhs(DynamicFloat.constant(other))
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_MODULO)
                    .build();
        }

        /**
         * Returns a {@link DynamicBool} that is true if the value of this {@link DynamicInt32} and
         * {@code other} are equal, otherwise it's false.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @NonNull
        default DynamicBool eq(@NonNull DynamicInt32 other) {
            return new ComparisonInt32Op.Builder()
                    .setInputLhs(this)
                    .setInputRhs(other)
                    .setOperationType(DynamicBuilders.COMPARISON_OP_TYPE_EQUALS)
                    .build();
        }

        /**
         * Returns a {@link DynamicBool} that is true if the value of this {@link DynamicInt32} and
         * {@code other} are equal, otherwise it's false.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @NonNull
        default DynamicBool eq(int other) {
            return new ComparisonInt32Op.Builder()
                    .setInputLhs(this)
                    .setInputRhs(constant(other))
                    .setOperationType(DynamicBuilders.COMPARISON_OP_TYPE_EQUALS)
                    .build();
        }

        /**
         * Returns a {@link DynamicBool} that is true if the value of this {@link DynamicInt32} and
         * {@code other} are not equal, otherwise it's false.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @NonNull
        default DynamicBool ne(@NonNull DynamicInt32 other) {
            return new ComparisonInt32Op.Builder()
                    .setInputLhs(this)
                    .setInputRhs(other)
                    .setOperationType(DynamicBuilders.COMPARISON_OP_TYPE_NOT_EQUALS)
                    .build();
        }

        /**
         * Returns a {@link DynamicBool} that is true if the value of this {@link DynamicInt32} and
         * {@code other} are not equal, otherwise it's false.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @NonNull
        default DynamicBool ne(int other) {
            return new ComparisonInt32Op.Builder()
                    .setInputLhs(this)
                    .setInputRhs(constant(other))
                    .setOperationType(DynamicBuilders.COMPARISON_OP_TYPE_NOT_EQUALS)
                    .build();
        }

        /**
         * Returns a {@link DynamicBool} that is true if the value of this {@link DynamicInt32} is
         * less than {@code other}, otherwise it's false.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @NonNull
        default DynamicBool lt(@NonNull DynamicInt32 other) {
            return new ComparisonInt32Op.Builder()
                    .setInputLhs(this)
                    .setInputRhs(other)
                    .setOperationType(DynamicBuilders.COMPARISON_OP_TYPE_LESS_THAN)
                    .build();
        }

        /**
         * Returns a {@link DynamicBool} that is true if the value of this {@link DynamicInt32} is
         * less than {@code other}, otherwise it's false.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @NonNull
        default DynamicBool lt(int other) {
            return new ComparisonInt32Op.Builder()
                    .setInputLhs(this)
                    .setInputRhs(constant(other))
                    .setOperationType(DynamicBuilders.COMPARISON_OP_TYPE_LESS_THAN)
                    .build();
        }

        /**
         * Returns a {@link DynamicBool} that is true if the value of this {@link DynamicInt32} is
         * less than or equal to {@code other}, otherwise it's false.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @NonNull
        default DynamicBool lte(@NonNull DynamicInt32 other) {
            return new ComparisonInt32Op.Builder()
                    .setInputLhs(this)
                    .setInputRhs(other)
                    .setOperationType(DynamicBuilders.COMPARISON_OP_TYPE_LESS_THAN_OR_EQUAL_TO)
                    .build();
        }

        /**
         * Returns a {@link DynamicBool} that is true if the value of this {@link DynamicInt32} is
         * less than or equal to {@code other}, otherwise it's false.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @NonNull
        default DynamicBool lte(int other) {
            return new ComparisonInt32Op.Builder()
                    .setInputLhs(this)
                    .setInputRhs(constant(other))
                    .setOperationType(DynamicBuilders.COMPARISON_OP_TYPE_LESS_THAN_OR_EQUAL_TO)
                    .build();
        }

        /**
         * Returns a {@link DynamicBool} that is true if the value of this {@link DynamicInt32} is
         * greater than {@code other}, otherwise it's false.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @NonNull
        default DynamicBool gt(@NonNull DynamicInt32 other) {
            return new ComparisonInt32Op.Builder()
                    .setInputLhs(this)
                    .setInputRhs(other)
                    .setOperationType(DynamicBuilders.COMPARISON_OP_TYPE_GREATER_THAN)
                    .build();
        }

        /**
         * Returns a {@link DynamicBool} that is true if the value of this {@link DynamicInt32} is
         * greater than {@code other}, otherwise it's false.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @NonNull
        default DynamicBool gt(int other) {
            return new ComparisonInt32Op.Builder()
                    .setInputLhs(this)
                    .setInputRhs(constant(other))
                    .setOperationType(DynamicBuilders.COMPARISON_OP_TYPE_GREATER_THAN)
                    .build();
        }

        /**
         * Returns a {@link DynamicBool} that is true if the value of this {@link DynamicInt32} is
         * greater than or equal to {@code other}, otherwise it's false.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @NonNull
        default DynamicBool gte(@NonNull DynamicInt32 other) {
            return new ComparisonInt32Op.Builder()
                    .setInputLhs(this)
                    .setInputRhs(other)
                    .setOperationType(DynamicBuilders.COMPARISON_OP_TYPE_GREATER_THAN_OR_EQUAL_TO)
                    .build();
        }

        /**
         * Returns a {@link DynamicBool} that is true if the value of this {@link DynamicInt32} is
         * greater than or equal to {@code other}, otherwise it's false.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @NonNull
        default DynamicBool gte(int other) {
            return new ComparisonInt32Op.Builder()
                    .setInputLhs(this)
                    .setInputRhs(constant(other))
                    .setOperationType(DynamicBuilders.COMPARISON_OP_TYPE_GREATER_THAN_OR_EQUAL_TO)
                    .build();
        }

        /**
         * Returns a {@link DynamicString} that contains the formatted value of this {@link
         * DynamicInt32} (with default formatting parameters). As an example, for locale en_US, the
         * following is equal to {@code DynamicString.constant("12")}
         *
         * <pre>
         *   DynamicInt32.constant(12).format()
         * </pre>
         *
         * The resulted {@link DynamicString} is subject to being truncated if it's too long.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @NonNull
        default DynamicString format() {
            return new IntFormatter.Builder().build().getInt32FormatOp(this);
        }

        /**
         * Returns a {@link DynamicString} that contains the formatted value of this {@link
         * DynamicInt32}. As an example, for locale en_US, the following is equal to {@code
         * DynamicString.constant("0,012")}
         *
         * <pre>
         *   DynamicInt32.constant(12)
         *            .format(
         *                new IntFormatter.Builder()
         *                                .setMinIntegerDigits(4)
         *                                .setGroupingUsed(true)
         *                                .build());
         * </pre>
         *
         * @param formatter The formatting parameter.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        @NonNull
        default DynamicString format(@NonNull IntFormatter formatter) {
            return formatter.getInt32FormatOp(this);
        }

        /** Allows formatting {@link DynamicInt32} into a {@link DynamicString}. */
        class IntFormatter {
            private final Int32FormatOp.Builder mInt32FormatOpBuilder;
            private final Int32FormatOp mInt32FormatOp;

            IntFormatter(@NonNull Int32FormatOp.Builder int32FormatOpBuilder) {
                mInt32FormatOpBuilder = int32FormatOpBuilder;
                mInt32FormatOp = int32FormatOpBuilder.build();
            }

            @NonNull
            @RequiresSchemaVersion(major = 1, minor = 200)
            Int32FormatOp getInt32FormatOp(@NonNull DynamicInt32 dynamicInt32) {
                return mInt32FormatOpBuilder.setInput(dynamicInt32).build();
            }

            /** Returns the minimum number of digits allowed in the integer portion of a number. */
            @IntRange(from = 0)
            public int getMinIntegerDigits() {
                return mInt32FormatOp.getMinIntegerDigits();
            }

            /** Returns whether digit grouping is used or not. */
            public boolean isGroupingUsed() {
                return mInt32FormatOp.getGroupingUsed();
            }

            /** Builder to create {@link IntFormatter} objects. */
            public static final class Builder {
                private static final int MAX_INTEGER_PART_LENGTH = 15;
                final Int32FormatOp.Builder mBuilder;

                public Builder() {
                    mBuilder = new Int32FormatOp.Builder();
                }

                /**
                 * Sets minimum number of integer digits for the formatter. Defaults to one if not
                 * specified. If minIntegerDigits is zero and the -1 < input < 1, the Integer part
                 * will not appear.
                 */
                @NonNull
                @RequiresSchemaVersion(major = 1, minor = 200)
                public Builder setMinIntegerDigits(@IntRange(from = 0) int minIntegerDigits) {
                    mBuilder.setMinIntegerDigits(minIntegerDigits);
                    return this;
                }

                /**
                 * Sets whether grouping is used for the formatter. Defaults to false if not
                 * specified. If grouping is used, digits will be grouped into digit groups using a
                 * separator. Digit group size and used separator can vary in different
                 * countries/regions. As an example, for locale en_US, the following is equal to
                 * {@code * DynamicString.constant("1,234")}
                 *
                 * <pre>
                 *   DynamicInt32.constant(1234)
                 *       .format(
                 *           new IntFormatter.Builder()
                 *                           .setGroupingUsed(true).build());
                 * </pre>
                 */
                @NonNull
                @RequiresSchemaVersion(major = 1, minor = 200)
                public Builder setGroupingUsed(boolean groupingUsed) {
                    mBuilder.setGroupingUsed(groupingUsed);
                    return this;
                }

                /** Builds an instance with values accumulated in this Builder. */
                @NonNull
                public IntFormatter build() {
                    throwIfExceedingMaxValue(
                            "MinIntegerDigits",
                            mBuilder.build().getMinIntegerDigits(),
                            MAX_INTEGER_PART_LENGTH);
                    return new IntFormatter(mBuilder);
                }

                private static void throwIfExceedingMaxValue(
                        String paramName, int value, int maxValue) {
                    if (value > maxValue) {
                        throw new IllegalArgumentException(
                                String.format(
                                        "%s (%d) is too large. Maximum value for %s is %d",
                                        paramName, value, paramName, maxValue));
                    }
                }
            }
        }

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        Fingerprint getFingerprint();

        /** Builder to create {@link DynamicInt32} objects. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull
            DynamicInt32 build();
        }
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static DynamicInt32 dynamicInt32FromProto(
            @NonNull DynamicProto.DynamicInt32 proto, @Nullable Fingerprint fingerprint) {
        if (proto.hasFixed()) {
            return FixedInt32.fromProto(proto.getFixed(), fingerprint);
        }
        if (proto.hasPlatformSource()) {
            return PlatformInt32Source.fromProto(proto.getPlatformSource(), fingerprint);
        }
        if (proto.hasArithmeticOperation()) {
            return ArithmeticInt32Op.fromProto(proto.getArithmeticOperation(), fingerprint);
        }
        if (proto.hasStateSource()) {
            return StateInt32Source.fromProto(proto.getStateSource(), fingerprint);
        }
        if (proto.hasConditionalOp()) {
            return ConditionalInt32Op.fromProto(proto.getConditionalOp(), fingerprint);
        }
        if (proto.hasFloatToInt()) {
            return FloatToInt32Op.fromProto(proto.getFloatToInt(), fingerprint);
        }
        if (proto.hasDurationPart()) {
            return GetDurationPartOp.fromProto(proto.getDurationPart(), fingerprint);
        }
        if (proto.hasAnimatableFixed()) {
            return AnimatableFixedInt32.fromProto(proto.getAnimatableFixed(), fingerprint);
        }
        if (proto.hasAnimatableDynamic()) {
            return AnimatableDynamicInt32.fromProto(proto.getAnimatableDynamic(), fingerprint);
        }
        if (proto.hasZonedDateTimePart()) {
            return GetZonedDateTimePartOp.fromProto(proto.getZonedDateTimePart(), fingerprint);
        }
        throw new IllegalStateException("Proto was not a recognised instance of DynamicInt32");
    }

    /**
     * Creates a new wrapper instance from the proto. Intended for testing purposes only. An object
     * created using this method can't be added to any other wrapper.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static DynamicInt32 dynamicInt32FromProto(@NonNull DynamicProto.DynamicInt32 proto) {
        return dynamicInt32FromProto(proto, new Fingerprint(proto.getFingerprint()));
    }

    /** Simple formatting for dynamic int32. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class Int32FormatOp implements DynamicString {
        private final DynamicProto.Int32FormatOp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        Int32FormatOp(DynamicProto.Int32FormatOp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the source of Int32 data to convert to a string. */
        @Nullable
        public DynamicInt32 getInput() {
            if (mImpl.hasInput()) {
                return DynamicBuilders.dynamicInt32FromProto(mImpl.getInput());
            } else {
                return null;
            }
        }

        /**
         * Gets minimum integer digits. Sign and grouping characters are not considered when
         * applying minIntegerDigits constraint. If not defined, defaults to one. For example, for
         * locale en_US, applying minIntegerDigit=4 to 12 would yield "0012".
         */
        @IntRange(from = 0)
        public int getMinIntegerDigits() {
            return mImpl.getMinIntegerDigits();
        }

        /**
         * Gets digit grouping used. Grouping size and grouping character depend on the current
         * locale. If not defined, defaults to false. For example, for locale en_US, using grouping
         * with 1234 would yield "1,234".
         */
        public boolean getGroupingUsed() {
            return mImpl.getGroupingUsed();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Int32FormatOp fromProto(
                @NonNull DynamicProto.Int32FormatOp proto, @Nullable Fingerprint fingerprint) {
            return new Int32FormatOp(proto, fingerprint);
        }

        @NonNull
        static Int32FormatOp fromProto(@NonNull DynamicProto.Int32FormatOp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.Int32FormatOp toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicString toDynamicStringProto() {
            return DynamicProto.DynamicString.newBuilder().setInt32FormatOp(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicString toDynamicStringProto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicString.newBuilder()
                        .setInt32FormatOp(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicStringProto();
        }

        @Override
        @NonNull
        public String toString() {
            return "Int32FormatOp{"
                    + "input="
                    + getInput()
                    + ", minIntegerDigits="
                    + getMinIntegerDigits()
                    + ", groupingUsed="
                    + getGroupingUsed()
                    + "}";
        }

        /** Builder for {@link Int32FormatOp}. */
        public static final class Builder implements DynamicString.Builder {
            final DynamicProto.Int32FormatOp.Builder mImpl =
                    DynamicProto.Int32FormatOp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(196209833);

            public Builder() {}

            /** Sets the source of Int32 data to convert to a string. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setInput(@NonNull DynamicInt32 input) {
                mImpl.setInput(input.toDynamicInt32Proto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(input.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets minimum integer digits. Sign and grouping characters are not considered when
             * applying minIntegerDigits constraint. If not defined, defaults to one. For example,
             * for locale en_US, applying minIntegerDigit=4 to 12 would yield "0012".
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setMinIntegerDigits(@IntRange(from = 0) int minIntegerDigits) {
                mImpl.setMinIntegerDigits(minIntegerDigits);
                mFingerprint.recordPropertyUpdate(4, minIntegerDigits);
                return this;
            }

            /**
             * Sets digit grouping used. Grouping size and grouping character depend on the current
             * locale. If not defined, defaults to false. For example, for locale en_US, using
             * grouping with 1234 would yield "1,234".
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setGroupingUsed(boolean groupingUsed) {
                mImpl.setGroupingUsed(groupingUsed);
                mFingerprint.recordPropertyUpdate(5, Boolean.hashCode(groupingUsed));
                return this;
            }

            @Override
            @NonNull
            public Int32FormatOp build() {
                return new Int32FormatOp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A dynamic String which sources its data from the tile's state. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class StateStringSource implements DynamicString {
        private final DynamicProto.StateStringSource mImpl;
        @Nullable private final Fingerprint mFingerprint;

        StateStringSource(DynamicProto.StateStringSource impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the key in the state to bind to. */
        @NonNull
        public String getSourceKey() {
            return mImpl.getSourceKey();
        }

        /** Gets the namespace for the state key. */
        @NonNull
        public String getSourceNamespace() {
            return mImpl.getSourceNamespace();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static StateStringSource fromProto(
                @NonNull DynamicProto.StateStringSource proto, @Nullable Fingerprint fingerprint) {
            return new StateStringSource(proto, fingerprint);
        }

        @NonNull
        static StateStringSource fromProto(@NonNull DynamicProto.StateStringSource proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.StateStringSource toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicString toDynamicStringProto() {
            return DynamicProto.DynamicString.newBuilder().setStateSource(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicString toDynamicStringProto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicString.newBuilder()
                        .setStateSource(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicStringProto();
        }

        @Override
        @NonNull
        public String toString() {
            return "StateStringSource{"
                    + "sourceKey="
                    + getSourceKey()
                    + ", sourceNamespace="
                    + getSourceNamespace()
                    + "}";
        }

        /** Builder for {@link StateStringSource}. */
        public static final class Builder implements DynamicString.Builder {
            private final DynamicProto.StateStringSource.Builder mImpl =
                    DynamicProto.StateStringSource.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1261652090);

            public Builder() {}

            /** Sets the key in the state to bind to. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setSourceKey(@NonNull String sourceKey) {
                mImpl.setSourceKey(sourceKey);
                mFingerprint.recordPropertyUpdate(1, sourceKey.hashCode());
                return this;
            }

            /** Sets the name space for the state key. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setSourceNamespace(@NonNull String sourceNamespace) {
                mImpl.setSourceNamespace(sourceNamespace);
                mFingerprint.recordPropertyUpdate(2, sourceNamespace.hashCode());
                return this;
            }

            @Override
            @NonNull
            public StateStringSource build() {
                return new StateStringSource(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A conditional operator which yields an string depending on the boolean operand. This
     * implements "string result = condition ? value_if_true : value_if_false".
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class ConditionalStringOp implements DynamicString {
        private final DynamicProto.ConditionalStringOp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        ConditionalStringOp(
                DynamicProto.ConditionalStringOp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the condition to use. */
        @Nullable
        public DynamicBool getCondition() {
            if (mImpl.hasCondition()) {
                return DynamicBuilders.dynamicBoolFromProto(mImpl.getCondition());
            } else {
                return null;
            }
        }

        /** Gets the string to yield if condition is true. */
        @Nullable
        public DynamicString getValueIfTrue() {
            if (mImpl.hasValueIfTrue()) {
                return DynamicBuilders.dynamicStringFromProto(mImpl.getValueIfTrue());
            } else {
                return null;
            }
        }

        /** Gets the string to yield if condition is false. */
        @Nullable
        public DynamicString getValueIfFalse() {
            if (mImpl.hasValueIfFalse()) {
                return DynamicBuilders.dynamicStringFromProto(mImpl.getValueIfFalse());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static ConditionalStringOp fromProto(
                @NonNull DynamicProto.ConditionalStringOp proto,
                @Nullable Fingerprint fingerprint) {
            return new ConditionalStringOp(proto, fingerprint);
        }

        @NonNull
        static ConditionalStringOp fromProto(@NonNull DynamicProto.ConditionalStringOp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.ConditionalStringOp toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicString toDynamicStringProto() {
            return DynamicProto.DynamicString.newBuilder().setConditionalOp(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicString toDynamicStringProto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicString.newBuilder()
                        .setConditionalOp(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicStringProto();
        }

        @Override
        @NonNull
        public String toString() {
            return "ConditionalStringOp{"
                    + "condition="
                    + getCondition()
                    + ", valueIfTrue="
                    + getValueIfTrue()
                    + ", valueIfFalse="
                    + getValueIfFalse()
                    + "}";
        }

        /** Builder for {@link ConditionalStringOp}. */
        public static final class Builder implements DynamicString.Builder {
            private final DynamicProto.ConditionalStringOp.Builder mImpl =
                    DynamicProto.ConditionalStringOp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1535849633);

            public Builder() {}

            /** Sets the condition to use. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setCondition(@NonNull DynamicBool condition) {
                mImpl.setCondition(condition.toDynamicBoolProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(condition.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the string to yield if condition is true. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setValueIfTrue(@NonNull DynamicString valueIfTrue) {
                mImpl.setValueIfTrue(valueIfTrue.toDynamicStringProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(valueIfTrue.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the string to yield if condition is false. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setValueIfFalse(@NonNull DynamicString valueIfFalse) {
                mImpl.setValueIfFalse(valueIfFalse.toDynamicStringProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(valueIfFalse.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public ConditionalStringOp build() {
                return new ConditionalStringOp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** This implements simple string concatenation "result = LHS+RHS". */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class ConcatStringOp implements DynamicString {

        private final DynamicProto.ConcatStringOp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        ConcatStringOp(DynamicProto.ConcatStringOp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets left hand side of the concatenation operation. */
        @Nullable
        public DynamicString getInputLhs() {
            if (mImpl.hasInputLhs()) {
                return DynamicBuilders.dynamicStringFromProto(mImpl.getInputLhs());
            } else {
                return null;
            }
        }

        /** Gets right hand side of the concatenation operation. */
        @Nullable
        public DynamicString getInputRhs() {
            if (mImpl.hasInputRhs()) {
                return DynamicBuilders.dynamicStringFromProto(mImpl.getInputRhs());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static ConcatStringOp fromProto(
                @NonNull DynamicProto.ConcatStringOp proto, @Nullable Fingerprint fingerprint) {
            return new ConcatStringOp(proto, fingerprint);
        }

        @NonNull
        static ConcatStringOp fromProto(@NonNull DynamicProto.ConcatStringOp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.ConcatStringOp toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicString toDynamicStringProto() {
            return DynamicProto.DynamicString.newBuilder().setConcatOp(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicString toDynamicStringProto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicString.newBuilder()
                        .setConcatOp(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicStringProto();
        }

        @Override
        @NonNull
        public String toString() {
            return "ConcatStringOp{"
                    + "inputLhs="
                    + getInputLhs()
                    + ", inputRhs="
                    + getInputRhs()
                    + "}";
        }

        /** Builder for {@link ConcatStringOp}. */
        public static final class Builder implements DynamicString.Builder {
            private final DynamicProto.ConcatStringOp.Builder mImpl =
                    DynamicProto.ConcatStringOp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1516620377);

            public Builder() {}

            /** Sets left hand side of the concatenation operation. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setInputLhs(@NonNull DynamicString inputLhs) {
                mImpl.setInputLhs(inputLhs.toDynamicStringProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(inputLhs.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets right hand side of the concatenation operation. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setInputRhs(@NonNull DynamicString inputRhs) {
                mImpl.setInputRhs(inputRhs.toDynamicStringProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(inputRhs.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public ConcatStringOp build() {
                return new ConcatStringOp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** Simple formatting for dynamic floats. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class FloatFormatOp implements DynamicString {
        private final DynamicProto.FloatFormatOp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        FloatFormatOp(DynamicProto.FloatFormatOp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the source of Float data to convert to a string. */
        @Nullable
        public DynamicFloat getInput() {
            if (mImpl.hasInput()) {
                return DynamicBuilders.dynamicFloatFromProto(mImpl.getInput());
            } else {
                return null;
            }
        }

        /**
         * Gets maximum fraction digits. Rounding will be applied if maxFractionDigits is smaller
         * than number of fraction digits. If not defined, defaults to three. minimumFractionDigits
         * must be <= maximumFractionDigits. If the condition is not satisfied, then
         * minimumFractionDigits will be used for both fields.
         */
        @IntRange(from = 0)
        public int getMaxFractionDigits() {
            return mImpl.getMaxFractionDigits();
        }

        /**
         * Gets minimum fraction digits. Zeros will be appended to the end to satisfy this
         * constraint. If not defined, defaults to zero. minimumFractionDigits must be <=
         * maximumFractionDigits. If the condition is not satisfied, then minimumFractionDigits will
         * be used for both fields.
         */
        @IntRange(from = 0)
        public int getMinFractionDigits() {
            return mImpl.getMinFractionDigits();
        }

        /**
         * Gets minimum integer digits. Sign and grouping characters are not considered when
         * applying minIntegerDigits constraint. If not defined, defaults to one. For example, for
         * locale en_US, applying minIntegerDigit=4 to 12.34 would yield "0012.34".
         */
        @IntRange(from = 0)
        public int getMinIntegerDigits() {
            return mImpl.getMinIntegerDigits();
        }

        /**
         * Gets digit grouping used. Grouping size and grouping character depend on the current
         * locale. If not defined, defaults to false. For example, for locale en_US, using grouping
         * with 1234.56 would yield "1,234.56".
         */
        public boolean getGroupingUsed() {
            return mImpl.getGroupingUsed();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static FloatFormatOp fromProto(
                @NonNull DynamicProto.FloatFormatOp proto, @Nullable Fingerprint fingerprint) {
            return new FloatFormatOp(proto, fingerprint);
        }

        @NonNull
        static FloatFormatOp fromProto(@NonNull DynamicProto.FloatFormatOp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.FloatFormatOp toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicString toDynamicStringProto() {
            return DynamicProto.DynamicString.newBuilder().setFloatFormatOp(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicString toDynamicStringProto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicString.newBuilder()
                        .setFloatFormatOp(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicStringProto();
        }

        @Override
        @NonNull
        public String toString() {
            return "FloatFormatOp{"
                    + "input="
                    + getInput()
                    + ", maxFractionDigits="
                    + getMaxFractionDigits()
                    + ", minFractionDigits="
                    + getMinFractionDigits()
                    + ", minIntegerDigits="
                    + getMinIntegerDigits()
                    + ", groupingUsed="
                    + getGroupingUsed()
                    + "}";
        }

        /** Builder for {@link FloatFormatOp}. */
        public static final class Builder implements DynamicString.Builder {
            private final DynamicProto.FloatFormatOp.Builder mImpl =
                    DynamicProto.FloatFormatOp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-5150153);

            public Builder() {}

            /** Sets the source of Float data to convert to a string. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setInput(@NonNull DynamicFloat input) {
                mImpl.setInput(input.toDynamicFloatProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(input.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets maximum fraction digits. Rounding will be applied if maxFractionDigits is
             * smaller than number of fraction digits. If not defined, defaults to three.
             * minimumFractionDigits must be <= maximumFractionDigits. If the condition is not
             * satisfied, then minimumFractionDigits will be used for both fields.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setMaxFractionDigits(@IntRange(from = 0) int maxFractionDigits) {
                mImpl.setMaxFractionDigits(maxFractionDigits);
                mFingerprint.recordPropertyUpdate(2, maxFractionDigits);
                return this;
            }

            /**
             * Sets minimum fraction digits. Zeros will be appended to the end to satisfy this
             * constraint. If not defined, defaults to zero. minimumFractionDigits must be <=
             * maximumFractionDigits. If the condition is not satisfied, then minimumFractionDigits
             * will be used for both fields.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setMinFractionDigits(@IntRange(from = 0) int minFractionDigits) {
                mImpl.setMinFractionDigits(minFractionDigits);
                mFingerprint.recordPropertyUpdate(3, minFractionDigits);
                return this;
            }

            /**
             * Sets minimum integer digits. Sign and grouping characters are not considered when
             * applying minIntegerDigits constraint. If not defined, defaults to one. For example,
             * for locale en_US, applying minIntegerDigit=4 to 12.34 would yield "0012.34".
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setMinIntegerDigits(@IntRange(from = 0) int minIntegerDigits) {
                mImpl.setMinIntegerDigits(minIntegerDigits);
                mFingerprint.recordPropertyUpdate(4, minIntegerDigits);
                return this;
            }

            /**
             * Sets digit grouping used. Grouping size and grouping character depend on the current
             * locale. If not defined, defaults to false. For example, for locale en_US, using
             * grouping with 1234.56 would yield "1,234.56".
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setGroupingUsed(boolean groupingUsed) {
                mImpl.setGroupingUsed(groupingUsed);
                mFingerprint.recordPropertyUpdate(5, Boolean.hashCode(groupingUsed));
                return this;
            }

            @Override
            @NonNull
            public FloatFormatOp build() {
                return new FloatFormatOp(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * Interface defining a dynamic string type.
     *
     * <p>{@link DynamicString} string value is subject to being truncated if it's too long.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public interface DynamicString extends DynamicType {
        /** Get the protocol buffer representation of this object, without fingerprint. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        DynamicProto.DynamicString toDynamicStringProto();

        /** Get the protocol buffer representation of this object, with or without fingerprint. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        DynamicProto.DynamicString toDynamicStringProto(boolean withFingerprint);

        /**
         * Creates a {@link DynamicString} from a byte array generated by {@link
         * #toDynamicStringByteArray()}.
         *
         * @throws IllegalArgumentException if the byte array does not contain a valid serialization
         */
        @NonNull
        static DynamicString fromByteArray(@NonNull byte[] byteArray) {
            return fromByteArray(byteArray, 0, byteArray.length);
        }

        /**
         * Creates a {@link DynamicString} from the provided byte array at the provided offset and
         * length, that was generated by one of the {@link #toDynamicStringByteArray} overloads.
         *
         * @throws IllegalArgumentException if the byte array does not contain a valid serialization
         *     in the provided offset and length
         */
        @NonNull
        static DynamicString fromByteArray(@NonNull byte[] byteArray, int offset, int length) {
            try {
                return dynamicStringFromProto(
                        DynamicProto.DynamicString.parseFrom(
                                CodedInputStream.newInstance(byteArray, offset, length),
                                ExtensionRegistryLite.getEmptyRegistry()));
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        "Byte array could not be parsed into DynamicString", e);
            }
        }

        /**
         * Serializes the {@link DynamicString} into a new byte array that can later be used with
         * {@link #fromByteArray(byte[])}.
         */
        @NonNull
        default byte[] toDynamicStringByteArray() {
            return toDynamicStringProto(/* withFingerprint= */ true).toByteArray();
        }

        /**
         * Serializes the {@link DynamicString} into the provided byte array, returning the amount
         * of bytes written, that can later be used with {@code DynamicString.fromByteArray(
         * byteArray, 0, bytesWritten)}.
         *
         * @throws IllegalArgumentException if the byte array is too small
         */
        default int toDynamicStringByteArray(@NonNull byte[] byteArray) {
            return toDynamicStringByteArray(byteArray, 0, byteArray.length);
        }

        /**
         * Serializes the {@link DynamicString} into the provided byte array, returning the amount
         * of bytes written, limited by the provided offset and length, that can later be used with
         * {@code DynamicString.fromByteArray(byteArray, offset, bytesWritten)}.
         *
         * @throws IllegalArgumentException if the byte array is too small
         */
        default int toDynamicStringByteArray(@NonNull byte[] byteArray, int offset, int length) {
            CodedOutputStream stream = CodedOutputStream.newInstance(byteArray, offset, length);
            try {
                toDynamicStringProto(/* withFingerprint= */ true).writeTo(stream);
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        "Provided byte array not large enough to contain this DynamicString", e);
            }
            return stream.getTotalBytesWritten();
        }

        /**
         * Creates a constant-valued {@link DynamicString}. The resulted {@link DynamicString} is
         * subject to being truncated if it's too long.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        static DynamicString constant(@NonNull String constant) {
            return new FixedString.Builder().setValue(constant).build();
        }

        /**
         * Creates a {@link DynamicString} that is bound to the value of an item of the State. The
         * resulted {@link DynamicString} is subject to being truncated if it's too long.
         *
         * @param dynamicDataKey The source key to a {@link DynamicDataValue} with a string value.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        static DynamicString from(@NonNull DynamicDataKey<DynamicString> dynamicDataKey) {
            return new StateStringSource.Builder()
                    .setSourceKey(dynamicDataKey.getKey())
                    .setSourceNamespace(dynamicDataKey.getNamespace())
                    .build();
        }

        /**
         * Creates a {@link DynamicString} that is bound to the result of a conditional expression.
         * It will use the value given in either {@link ConditionScope#use} or {@link
         * ConditionScopes.IfTrueScope#elseUse} depending on the value yielded from {@code
         * condition}.
         *
         * @param condition The value used for evaluting this condition.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        static ConditionScope<DynamicString, String> onCondition(@NonNull DynamicBool condition) {
            return new ConditionScopes.ConditionScope<>(
                    (trueValue, falseValue) ->
                            new ConditionalStringOp.Builder()
                                    .setCondition(condition)
                                    .setValueIfTrue(trueValue)
                                    .setValueIfFalse(falseValue)
                                    .build(),
                    DynamicString::constant);
        }

        /**
         * Returns a new {@link DynamicString} that has the result of concatenating this {@link
         * DynamicString} with {@code other}. i.e. {@code result = this + other} The resulted {@link
         * DynamicString} is subject to being truncated if it's too long.
         *
         * @param other The right hand side operand of the concatenation.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicString concat(@NonNull DynamicString other) {
            return new DynamicBuilders.ConcatStringOp.Builder()
                    .setInputLhs(this)
                    .setInputRhs(other)
                    .build();
        }

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        Fingerprint getFingerprint();

        /** Builder to create {@link DynamicString} objects. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull
            DynamicString build();
        }
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static DynamicString dynamicStringFromProto(
            @NonNull DynamicProto.DynamicString proto, @Nullable Fingerprint fingerprint) {
        if (proto.hasFixed()) {
            return FixedString.fromProto(proto.getFixed(), fingerprint);
        }
        if (proto.hasInt32FormatOp()) {
            return Int32FormatOp.fromProto(proto.getInt32FormatOp(), fingerprint);
        }
        if (proto.hasStateSource()) {
            return StateStringSource.fromProto(proto.getStateSource(), fingerprint);
        }
        if (proto.hasConditionalOp()) {
            return ConditionalStringOp.fromProto(proto.getConditionalOp(), fingerprint);
        }
        if (proto.hasConcatOp()) {
            return ConcatStringOp.fromProto(proto.getConcatOp(), fingerprint);
        }
        if (proto.hasFloatFormatOp()) {
            return FloatFormatOp.fromProto(proto.getFloatFormatOp(), fingerprint);
        }
        throw new IllegalStateException("Proto was not a recognised instance of DynamicString");
    }

    /**
     * Creates a new wrapper instance from the proto. Intended for testing purposes only. An object
     * created using this method can't be added to any other wrapper.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static DynamicString dynamicStringFromProto(@NonNull DynamicProto.DynamicString proto) {
        return dynamicStringFromProto(proto, new Fingerprint(proto.getFingerprint()));
    }

    /**
     * An arithmetic operation, operating on two Float instances. This implements simple binary
     * operations of the form "result = LHS <op> RHS", where the available operation types are
     * described in {@code ArithmeticOpType}.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class ArithmeticFloatOp implements DynamicFloat {

        private final DynamicProto.ArithmeticFloatOp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        ArithmeticFloatOp(DynamicProto.ArithmeticFloatOp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets left hand side of the arithmetic operation. */
        @Nullable
        public DynamicFloat getInputLhs() {
            if (mImpl.hasInputLhs()) {
                return DynamicBuilders.dynamicFloatFromProto(mImpl.getInputLhs());
            } else {
                return null;
            }
        }

        /** Gets right hand side of the arithmetic operation. */
        @Nullable
        public DynamicFloat getInputRhs() {
            if (mImpl.hasInputRhs()) {
                return DynamicBuilders.dynamicFloatFromProto(mImpl.getInputRhs());
            } else {
                return null;
            }
        }

        /** Gets the type of operation to carry out. */
        @ArithmeticOpType
        public int getOperationType() {
            return mImpl.getOperationType().getNumber();
        }

        /** */
        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static ArithmeticFloatOp fromProto(
                @NonNull DynamicProto.ArithmeticFloatOp proto, @Nullable Fingerprint fingerprint) {
            return new ArithmeticFloatOp(proto, fingerprint);
        }

        @NonNull
        static ArithmeticFloatOp fromProto(@NonNull DynamicProto.ArithmeticFloatOp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.ArithmeticFloatOp toProto() {
            return mImpl;
        }

        /** */
        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicFloat toDynamicFloatProto() {
            return DynamicProto.DynamicFloat.newBuilder().setArithmeticOperation(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicFloat toDynamicFloatProto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicFloat.newBuilder()
                        .setArithmeticOperation(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicFloatProto();
        }

        @Override
        @NonNull
        public String toString() {
            return "ArithmeticFloatOp{"
                    + "inputLhs="
                    + getInputLhs()
                    + ", inputRhs="
                    + getInputRhs()
                    + ", operationType="
                    + getOperationType()
                    + "}";
        }

        /** Builder for {@link ArithmeticFloatOp}. */
        public static final class Builder implements DynamicFloat.Builder {

            private final DynamicProto.ArithmeticFloatOp.Builder mImpl =
                    DynamicProto.ArithmeticFloatOp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1818249334);

            public Builder() {}

            /** Sets left hand side of the arithmetic operation. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setInputLhs(@NonNull DynamicFloat inputLhs) {
                mImpl.setInputLhs(inputLhs.toDynamicFloatProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(inputLhs.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets right hand side of the arithmetic operation. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setInputRhs(@NonNull DynamicFloat inputRhs) {
                mImpl.setInputRhs(inputRhs.toDynamicFloatProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(inputRhs.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the type of operation to carry out. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setOperationType(@ArithmeticOpType int operationType) {
                mImpl.setOperationType(DynamicProto.ArithmeticOpType.forNumber(operationType));
                mFingerprint.recordPropertyUpdate(3, operationType);
                return this;
            }

            @Override
            @NonNull
            public ArithmeticFloatOp build() {
                return new ArithmeticFloatOp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A dynamic Float which sources its data from the tile's state. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class StateFloatSource implements DynamicFloat {
        private final DynamicProto.StateFloatSource mImpl;
        @Nullable private final Fingerprint mFingerprint;

        StateFloatSource(DynamicProto.StateFloatSource impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the key in the state to bind to. */
        @NonNull
        public String getSourceKey() {
            return mImpl.getSourceKey();
        }

        /** Gets the namespace for the state key. */
        @NonNull
        public String getSourceNamespace() {
            return mImpl.getSourceNamespace();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static StateFloatSource fromProto(
                @NonNull DynamicProto.StateFloatSource proto, @Nullable Fingerprint fingerprint) {
            return new StateFloatSource(proto, fingerprint);
        }

        @NonNull
        static StateFloatSource fromProto(@NonNull DynamicProto.StateFloatSource proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.StateFloatSource toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicFloat toDynamicFloatProto() {
            return DynamicProto.DynamicFloat.newBuilder().setStateSource(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicFloat toDynamicFloatProto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicFloat.newBuilder()
                        .setStateSource(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicFloatProto();
        }

        @Override
        @NonNull
        public String toString() {
            return "StateFloatSource{"
                    + "sourceKey="
                    + getSourceKey()
                    + ", sourceNamespace="
                    + getSourceNamespace()
                    + "}";
        }

        /** Builder for {@link StateFloatSource}. */
        public static final class Builder implements DynamicFloat.Builder {
            private final DynamicProto.StateFloatSource.Builder mImpl =
                    DynamicProto.StateFloatSource.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(384370154);

            public Builder() {}

            /** Sets the key in the state to bind to. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setSourceKey(@NonNull String sourceKey) {
                mImpl.setSourceKey(sourceKey);
                mFingerprint.recordPropertyUpdate(1, sourceKey.hashCode());
                return this;
            }

            /** Sets the name space for the state key. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setSourceNamespace(@NonNull String sourceNamespace) {
                mImpl.setSourceNamespace(sourceNamespace);
                mFingerprint.recordPropertyUpdate(2, sourceNamespace.hashCode());
                return this;
            }

            @Override
            @NonNull
            public StateFloatSource build() {
                return new StateFloatSource(mImpl.build(), mFingerprint);
            }
        }
    }

    /** An operation to convert an Int32 value in the dynamic data pipeline to a Float value. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class Int32ToFloatOp implements DynamicFloat {
        private final DynamicProto.Int32ToFloatOp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        Int32ToFloatOp(DynamicProto.Int32ToFloatOp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the input Int32 to convert to a Float. */
        @Nullable
        public DynamicInt32 getInput() {
            if (mImpl.hasInput()) {
                return DynamicBuilders.dynamicInt32FromProto(mImpl.getInput());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Int32ToFloatOp fromProto(
                @NonNull DynamicProto.Int32ToFloatOp proto, @Nullable Fingerprint fingerprint) {
            return new Int32ToFloatOp(proto, fingerprint);
        }

        @NonNull
        static Int32ToFloatOp fromProto(@NonNull DynamicProto.Int32ToFloatOp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.Int32ToFloatOp toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicFloat toDynamicFloatProto() {
            return DynamicProto.DynamicFloat.newBuilder().setInt32ToFloatOperation(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicFloat toDynamicFloatProto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicFloat.newBuilder()
                        .setInt32ToFloatOperation(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicFloatProto();
        }

        @Override
        @NonNull
        public String toString() {
            return "Int32ToFloatOp{" + "input=" + getInput() + "}";
        }

        /** Builder for {@link Int32ToFloatOp}. */
        public static final class Builder implements DynamicFloat.Builder {
            private final DynamicProto.Int32ToFloatOp.Builder mImpl =
                    DynamicProto.Int32ToFloatOp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-619592745);

            public Builder() {}

            /** Sets the input Int32 to convert to a Float. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setInput(@NonNull DynamicInt32 input) {
                mImpl.setInput(input.toDynamicInt32Proto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(input.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public Int32ToFloatOp build() {
                return new Int32ToFloatOp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A static interpolation node, between two fixed floating point values. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class AnimatableFixedFloat implements DynamicFloat {
        private final DynamicProto.AnimatableFixedFloat mImpl;
        @Nullable private final Fingerprint mFingerprint;

        AnimatableFixedFloat(
                DynamicProto.AnimatableFixedFloat impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the number to start animating from. */
        public float getFromValue() {
            return mImpl.getFromValue();
        }

        /** Gets the number to animate to. */
        public float getToValue() {
            return mImpl.getToValue();
        }

        /** Gets the animation parameters for duration, delay, etc. */
        @Nullable
        public AnimationSpec getAnimationSpec() {
            if (mImpl.hasAnimationSpec()) {
                return AnimationSpec.fromProto(mImpl.getAnimationSpec());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static AnimatableFixedFloat fromProto(
                @NonNull DynamicProto.AnimatableFixedFloat proto,
                @Nullable Fingerprint fingerprint) {
            return new AnimatableFixedFloat(proto, fingerprint);
        }

        @NonNull
        static AnimatableFixedFloat fromProto(@NonNull DynamicProto.AnimatableFixedFloat proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.AnimatableFixedFloat toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicFloat toDynamicFloatProto() {
            return DynamicProto.DynamicFloat.newBuilder().setAnimatableFixed(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicFloat toDynamicFloatProto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicFloat.newBuilder()
                        .setAnimatableFixed(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicFloatProto();
        }

        @Override
        @NonNull
        public String toString() {
            return "AnimatableFixedFloat{"
                    + "fromValue="
                    + getFromValue()
                    + ", toValue="
                    + getToValue()
                    + ", animationSpec="
                    + getAnimationSpec()
                    + "}";
        }

        /** Builder for {@link AnimatableFixedFloat}. */
        public static final class Builder implements DynamicFloat.Builder {
            private final DynamicProto.AnimatableFixedFloat.Builder mImpl =
                    DynamicProto.AnimatableFixedFloat.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1964707538);

            public Builder() {}

            /** Sets the number to start animating from. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setFromValue(float fromValue) {
                mImpl.setFromValue(fromValue);
                mFingerprint.recordPropertyUpdate(1, Float.floatToIntBits(fromValue));
                return this;
            }

            /** Sets the number to animate to. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setToValue(float toValue) {
                mImpl.setToValue(toValue);
                mFingerprint.recordPropertyUpdate(2, Float.floatToIntBits(toValue));
                return this;
            }

            /** Sets the animation parameters for duration, delay, etc. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setAnimationSpec(@NonNull AnimationSpec animationSpec) {
                mImpl.setAnimationSpec(animationSpec.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(animationSpec.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public AnimatableFixedFloat build() {
                return new AnimatableFixedFloat(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A dynamic interpolation node. This will watch the value of its input and, when the first
     * update arrives, immediately emit that value. On subsequent updates, it will animate between
     * the old and new values.
     *
     * <p>If this node receives an invalid value (e.g. as a result of an upstream node having no
     * value), then it will emit a single invalid value, and forget its "stored" value. The next
     * valid value that arrives is then used as the "first" value again.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class AnimatableDynamicFloat implements DynamicFloat {
        private final DynamicProto.AnimatableDynamicFloat mImpl;
        @Nullable private final Fingerprint mFingerprint;

        AnimatableDynamicFloat(
                DynamicProto.AnimatableDynamicFloat impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value to watch, and animate when it changes. */
        @Nullable
        public DynamicFloat getInput() {
            if (mImpl.hasInput()) {
                return DynamicBuilders.dynamicFloatFromProto(mImpl.getInput());
            } else {
                return null;
            }
        }

        /** Gets the animation parameters for duration, delay, etc. */
        @Nullable
        public AnimationSpec getAnimationSpec() {
            if (mImpl.hasAnimationSpec()) {
                return AnimationSpec.fromProto(mImpl.getAnimationSpec());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static AnimatableDynamicFloat fromProto(
                @NonNull DynamicProto.AnimatableDynamicFloat proto,
                @Nullable Fingerprint fingerprint) {
            return new AnimatableDynamicFloat(proto, fingerprint);
        }

        @NonNull
        static AnimatableDynamicFloat fromProto(
                @NonNull DynamicProto.AnimatableDynamicFloat proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.AnimatableDynamicFloat toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicFloat toDynamicFloatProto() {
            return DynamicProto.DynamicFloat.newBuilder().setAnimatableDynamic(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicFloat toDynamicFloatProto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicFloat.newBuilder()
                        .setAnimatableDynamic(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicFloatProto();
        }

        @Override
        @NonNull
        public String toString() {
            return "AnimatableDynamicFloat{"
                    + "input="
                    + getInput()
                    + ", animationSpec="
                    + getAnimationSpec()
                    + "}";
        }

        /** Builder for {@link AnimatableDynamicFloat}. */
        public static final class Builder implements DynamicFloat.Builder {
            private final DynamicProto.AnimatableDynamicFloat.Builder mImpl =
                    DynamicProto.AnimatableDynamicFloat.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1543182280);

            public Builder() {}

            /** Sets the value to watch, and animate when it changes. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setInput(@NonNull DynamicFloat input) {
                mImpl.setInput(input.toDynamicFloatProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(input.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the animation parameters for duration, delay, etc. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setAnimationSpec(@NonNull AnimationSpec animationSpec) {
                mImpl.setAnimationSpec(animationSpec.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(animationSpec.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public AnimatableDynamicFloat build() {
                return new AnimatableDynamicFloat(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * Interface defining a dynamic float type.
     *
     * <p>It offers a set of helper methods for creating arithmetic and logical expressions, e.g.
     * {@link #plus(float)}, {@link #times(float)}, {@link #eq(float)}, etc. These helper methods
     * produce expression trees based on the order in which they were called in an expression. Thus,
     * no operator precedence rules are applied.
     *
     * <p>For example the following expression is equivalent to {@code result = ((a + b)*c)/d }:
     *
     * <pre>{@code
     * a.plus(b).times(c).div(d);
     * }</pre>
     *
     * More complex expressions can be created by nesting expressions. For example the following
     * expression is equivalent to {@code result = (a + b)*(c - d) }:
     *
     * <pre>{@code
     * (a.plus(b)).times(c.minus(d));
     * }</pre>
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public interface DynamicFloat extends DynamicType {
        /** Get the protocol buffer representation of this object, without fingerprint. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        DynamicProto.DynamicFloat toDynamicFloatProto();

        /** Get the protocol buffer representation of this object, with or without fingerprint. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        DynamicProto.DynamicFloat toDynamicFloatProto(boolean withFingerprint);

        /**
         * Creates a {@link DynamicFloat} from a byte array generated by {@link
         * #toDynamicFloatByteArray()}.
         *
         * @throws IllegalArgumentException if the byte array does not contain a valid serialization
         */
        @NonNull
        static DynamicFloat fromByteArray(@NonNull byte[] byteArray) {
            return fromByteArray(byteArray, 0, byteArray.length);
        }

        /**
         * Creates a {@link DynamicFloat} from the provided byte array at the provided offset and
         * length, that was generated by one of the {@link #toDynamicFloatByteArray} overloads.
         *
         * @throws IllegalArgumentException if the byte array does not contain a valid serialization
         *     in the provided offset and length
         */
        @NonNull
        static DynamicFloat fromByteArray(@NonNull byte[] byteArray, int offset, int length) {
            try {
                return dynamicFloatFromProto(
                        DynamicProto.DynamicFloat.parseFrom(
                                CodedInputStream.newInstance(byteArray, offset, length),
                                ExtensionRegistryLite.getEmptyRegistry()));
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        "Byte array could not be parsed into DynamicFloat", e);
            }
        }

        /**
         * Serializes the {@link DynamicFloat} into a new byte array that can later be used with
         * {@link #fromByteArray(byte[])}.
         */
        @NonNull
        default byte[] toDynamicFloatByteArray() {
            return toDynamicFloatProto(/* withFingerprint= */ true).toByteArray();
        }

        /**
         * Serializes the {@link DynamicFloat} into the provided byte array, returning the amount of
         * bytes written, that can later be used with {@code DynamicFloat.fromByteArray(byteArray,
         * 0, bytesWritten)}.
         *
         * @throws IllegalArgumentException if the byte array is too small
         */
        default int toDynamicFloatByteArray(@NonNull byte[] byteArray) {
            return toDynamicFloatByteArray(byteArray, 0, byteArray.length);
        }

        /**
         * Serializes the {@link DynamicFloat} into the provided byte array, returning the amount of
         * bytes written, limited by the provided offset and length, that can later be used with
         * {@code DynamicFloat.fromByteArray(byteArray, offset, bytesWritten)}.
         *
         * @throws IllegalArgumentException if the byte array is too small
         */
        default int toDynamicFloatByteArray(@NonNull byte[] byteArray, int offset, int length) {
            CodedOutputStream stream = CodedOutputStream.newInstance(byteArray, offset, length);
            try {
                toDynamicFloatProto(/* withFingerprint= */ true).writeTo(stream);
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        "Provided byte array not large enough to contain this DynamicFloat", e);
            }
            return stream.getTotalBytesWritten();
        }

        /**
         * Creates a constant-valued {@link DynamicFloat}.
         *
         * <p>If {@code Float.isNan(constant)} is true, the value will be invalid. And any
         * expression that uses this {@link DynamicFloat} will have an invalid result (which will be
         * delivered through {@link DynamicTypeValueReceiver<T>#onInvalidate()}.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        static DynamicFloat constant(float constant) {
            return new FixedFloat.Builder().setValue(constant).build();
        }

        /**
         * Creates a {@link DynamicFloat} that is bound to the value of an item of the State.
         *
         * @param dynamicDataKey The data source to a {@link DynamicDataValue} with a float value.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        static DynamicFloat from(@NonNull DynamicDataKey<DynamicFloat> dynamicDataKey) {
            return new StateFloatSource.Builder()
                    .setSourceKey(dynamicDataKey.getKey())
                    .setSourceNamespace(dynamicDataKey.getNamespace())
                    .build();
        }

        /**
         * Creates a {@link DynamicFloat} which will animate over the range of floats from {@code
         * start} to {@code end}.
         *
         * @param start The start value of the range.
         * @param end The end value of the range.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        static DynamicFloat animate(float start, float end) {
            return new AnimatableFixedFloat.Builder().setFromValue(start).setToValue(end).build();
        }

        /**
         * Creates a {@link DynamicFloat} which will animate over the range of floats from {@code
         * start} to {@code end} with the given animation parameters.
         *
         * @param start The start value of the range.
         * @param end The end value of the range.
         * @param animationSpec The animation parameters.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        static DynamicFloat animate(float start, float end, @NonNull AnimationSpec animationSpec) {
            return new AnimatableFixedFloat.Builder()
                    .setFromValue(start)
                    .setToValue(end)
                    .setAnimationSpec(animationSpec)
                    .build();
        }

        /**
         * Creates a {@link DynamicFloat} that is bound to the value of an item of the State. Every
         * time the state value changes, this {@link DynamicFloat} will animate from its current
         * value to the new value (from the state).
         *
         * @param dynamicDataKey The data source to a {@link DynamicDataValue} with a float value.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        static DynamicFloat animate(@NonNull DynamicDataKey<DynamicFloat> dynamicDataKey) {
            return new AnimatableDynamicFloat.Builder().setInput(from(dynamicDataKey)).build();
        }

        /**
         * Creates a {@link DynamicFloat} that is bound to the value of an item of the State. Every
         * time the state value changes, this {@link DynamicFloat} will animate from its current
         * value to the new value (from the state).
         *
         * @param dynamicDataKey The source key to a {@link DynamicDataValue} with a float value.
         * @param animationSpec The animation parameters.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        static DynamicFloat animate(
                @NonNull DynamicDataKey<DynamicFloat> dynamicDataKey,
                @NonNull AnimationSpec animationSpec) {
            return new AnimatableDynamicFloat.Builder()
                    .setInput(from(dynamicDataKey))
                    .setAnimationSpec(animationSpec)
                    .build();
        }

        /**
         * Returns a {@link DynamicFloat} that is bound to the value of this {@link DynamicFloat}
         * and every time its value is changing, it animates from its current value to the new
         * value.
         *
         * @param animationSpec The animation parameters.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicFloat animate(@NonNull AnimationSpec animationSpec) {
            return new AnimatableDynamicFloat.Builder()
                    .setInput(this)
                    .setAnimationSpec(animationSpec)
                    .build();
        }

        /**
         * Returns a {@link DynamicFloat} that is bound to the value of this {@link DynamicFloat}
         * and every time its value is changing, it animates from its current value to the new
         * value.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicFloat animate() {
            return new AnimatableDynamicFloat.Builder().setInput(this).build();
        }

        /**
         * Returns a {@link DynamicInt32} which holds the largest integer value that is smaller than
         * or equal to this {@link DynamicFloat}, i.e. {@code int result = (int) Math.floor(this)}
         *
         * <p>If the float value is larger than {@link Integer#MAX_VALUE} or smaller than {@link
         * Integer#MIN_VALUE}, the result of this operation will be invalid and any expression that
         * uses the {@link DynamicInt32} will have an invalid result (which will be delivered
         * through {@link DynamicTypeValueReceiver<T>#onInvalidate()}.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicInt32 asInt() {
            return new FloatToInt32Op.Builder()
                    .setRoundMode(DynamicBuilders.ROUND_MODE_FLOOR)
                    .setInput(this)
                    .build();
        }

        /**
         * Creates a {@link DynamicFloat} containing the result of adding another {@link
         * DynamicFloat} to this {@link DynamicFloat}; As an example, the following is equal to
         * {@code DynamicFloat.constant(13f)}
         *
         * <pre>
         *   DynamicFloat.constant(7f).plus(DynamicFloat.constant(5f));
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicFloat} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicFloat} containing the result of the operation.
         */
        @SuppressWarnings("KotlinOperator")
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicFloat plus(@NonNull DynamicFloat other) {
            return new ArithmeticFloatOp.Builder()
                    .setInputLhs(this)
                    .setInputRhs(other)
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_ADD)
                    .build();
        }

        /**
         * Creates a {@link DynamicFloat} containing the result of adding a float to this {@link
         * DynamicFloat}; As an example, the following is equal to {@code
         * DynamicFloat.constant(13f)}
         *
         * <pre>
         *   DynamicFloat.constant(7f).plus(5f);
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicFloat} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicFloat} containing the result of the operation.
         */
        @SuppressWarnings("KotlinOperator")
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicFloat plus(float other) {
            return new ArithmeticFloatOp.Builder()
                    .setInputLhs(this)
                    .setInputRhs(constant(other))
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_ADD)
                    .build();
        }

        /**
         * Creates a {@link DynamicFloat} containing the result of adding a {@link DynamicInt32} to
         * this {@link DynamicFloat}; As an example, the following is equal to {@code
         * DynamicFloat.constant(13f)}
         *
         * <pre>
         *   DynamicFloat.constant(7f).plus(DynamicInt32.constant(5));
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicFloat} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicFloat} containing the result of the operation.
         */
        @SuppressWarnings("KotlinOperator")
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicFloat plus(@NonNull DynamicInt32 other) {
            return new ArithmeticFloatOp.Builder()
                    .setInputLhs(this)
                    .setInputRhs(other.asFloat())
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_ADD)
                    .build();
        }

        /**
         * Creates a {@link DynamicFloat} containing the result of subtracting another {@link
         * DynamicFloat} from this {@link DynamicFloat}; As an example, the following is equal to
         * {@code DynamicFloat.constant(2f)}
         *
         * <pre>
         *   DynamicFloat.constant(7f).minus(DynamicFloat.constant(5f));
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicFloat} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicFloat} containing the result of the operation.
         */
        @SuppressWarnings("KotlinOperator")
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicFloat minus(@NonNull DynamicFloat other) {
            return new ArithmeticFloatOp.Builder()
                    .setInputLhs(this)
                    .setInputRhs(other)
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_SUBTRACT)
                    .build();
        }

        /**
         * Creates a {@link DynamicFloat} containing the result of subtracting a flaot from this
         * {@link DynamicFloat}; As an example, the following is equal to {@code
         * DynamicFloat.constant(2f)}
         *
         * <pre>
         *   DynamicFloat.constant(7f).minus(5f);
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicFloat} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicFloat} containing the result of the operation.
         */
        @SuppressWarnings("KotlinOperator")
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicFloat minus(float other) {
            return new ArithmeticFloatOp.Builder()
                    .setInputLhs(this)
                    .setInputRhs(constant(other))
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_SUBTRACT)
                    .build();
        }

        /**
         * Creates a {@link DynamicFloat} containing the result of subtracting a {@link
         * DynamicInt32} from this {@link DynamicFloat}; As an example, the following is equal to
         * {@code DynamicFloat.constant(2f)}
         *
         * <pre>
         *   DynamicFloat.constant(7f).minus(DynamicInt32.constant(5));
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicFloat} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicFloat} containing the result of the operation.
         */
        @SuppressWarnings("KotlinOperator")
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicFloat minus(@NonNull DynamicInt32 other) {
            return new ArithmeticFloatOp.Builder()
                    .setInputLhs(this)
                    .setInputRhs(other.asFloat())
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_SUBTRACT)
                    .build();
        }

        /**
         * Creates a {@link DynamicFloat} containing the result of multiplying this {@link
         * DynamicFloat} by another {@link DynamicFloat}; As an example, the following is equal to
         * {@code DynamicFloat.constant(35f)}
         *
         * <pre>
         *   DynamicFloat.constant(7f).times(DynamicFloat.constant(5f));
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicFloat} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicFloat} containing the result of the operation.
         */
        @SuppressWarnings("KotlinOperator")
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicFloat times(@NonNull DynamicFloat other) {
            return new ArithmeticFloatOp.Builder()
                    .setInputLhs(this)
                    .setInputRhs(other)
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_MULTIPLY)
                    .build();
        }

        /**
         * Creates a {@link DynamicFloat} containing the result of multiplying this {@link
         * DynamicFloat} by a flaot; As an example, the following is equal to {@code
         * DynamicFloat.constant(35f)}
         *
         * <pre>
         *   DynamicFloat.constant(7f).times(5f);
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicFloat} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicFloat} containing the result of the operation.
         */
        @SuppressWarnings("KotlinOperator")
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicFloat times(float other) {
            return new ArithmeticFloatOp.Builder()
                    .setInputLhs(this)
                    .setInputRhs(constant(other))
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_MULTIPLY)
                    .build();
        }

        /**
         * Creates a {@link DynamicFloat} containing the result of multiplying this {@link
         * DynamicFloat} by a {@link DynamicInt32}; As an example, the following is equal to {@code
         * DynamicFloat.constant(35f)}
         *
         * <pre>
         *   DynamicFloat.constant(7f).times(DynamicInt32.constant(5));
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicFloat} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicFloat} containing the result of the operation.
         */
        @SuppressWarnings("KotlinOperator")
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicFloat times(@NonNull DynamicInt32 other) {
            return new ArithmeticFloatOp.Builder()
                    .setInputLhs(this)
                    .setInputRhs(other.asFloat())
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_MULTIPLY)
                    .build();
        }

        /**
         * Creates a {@link DynamicFloat} containing the result of dividing this {@link
         * DynamicFloat} by another {@link DynamicFloat}; As an example, the following is equal to
         * {@code DynamicFloat.constant(1.4f)}
         *
         * <pre>
         *   DynamicFloat.constant(7f).div(DynamicFloat.constant(5f));
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicFloat} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicFloat} containing the result of the operation.
         */
        @SuppressWarnings("KotlinOperator")
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicFloat div(@NonNull DynamicFloat other) {
            return new ArithmeticFloatOp.Builder()
                    .setInputLhs(this)
                    .setInputRhs(other)
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_DIVIDE)
                    .build();
        }

        /**
         * Creates a {@link DynamicFloat} containing the result of dividing this {@link
         * DynamicFloat} by a float; As an example, the following is equal to {@code
         * DynamicFloat.constant(1.4f)}
         *
         * <pre>
         *   DynamicFloat.constant(7f).div(5f);
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicFloat} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicFloat} containing the result of the operation.
         */
        @SuppressWarnings("KotlinOperator")
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicFloat div(float other) {
            return new ArithmeticFloatOp.Builder()
                    .setInputLhs(this)
                    .setInputRhs(constant(other))
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_DIVIDE)
                    .build();
        }

        /**
         * Creates a {@link DynamicFloat} containing the result of dividing this {@link
         * DynamicFloat} by a {@link DynamicInt32}; As an example, the following is equal to {@code
         * DynamicFloat.constant(1.4f)}
         *
         * <pre>
         *   DynamicFloat.constant(7f).div(DynamicInt32.constant(5));
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicFloat} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicFloat} containing the result of the operation.
         */
        @SuppressWarnings("KotlinOperator")
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicFloat div(@NonNull DynamicInt32 other) {
            return new ArithmeticFloatOp.Builder()
                    .setInputLhs(this)
                    .setInputRhs(other.asFloat())
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_DIVIDE)
                    .build();
        }

        /**
         * Creates a {@link DynamicFloat} containing the reminder of dividing this {@link
         * DynamicFloat} by another {@link DynamicFloat}; As an example, the following is equal to
         * {@code DynamicFloat.constant(1.5f)}
         *
         * <pre>
         *   DynamicFloat.constant(7f).rem(DynamicFloat.constant(5.5f));
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicFloat} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicFloat} containing the result of the operation.
         */
        @SuppressWarnings("KotlinOperator")
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicFloat rem(@NonNull DynamicFloat other) {
            return new ArithmeticFloatOp.Builder()
                    .setInputLhs(this)
                    .setInputRhs(other)
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_MODULO)
                    .build();
        }

        /**
         * Creates a {@link DynamicFloat} containing the reminder of dividing this {@link
         * DynamicFloat} by a float; As an example, the following is equal to {@code
         * DynamicFloat.constant(1.5f)}
         *
         * <pre>
         *   DynamicFloat.constant(7f).rem(5.5f);
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicFloat} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicFloat} containing the result of the operation.
         */
        @SuppressWarnings("KotlinOperator")
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicFloat rem(float other) {
            return new ArithmeticFloatOp.Builder()
                    .setInputLhs(this)
                    .setInputRhs(constant(other))
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_MODULO)
                    .build();
        }

        /**
         * Creates a {@link DynamicFloat} containing the reminder of dividing this {@link
         * DynamicFloat} by a {@link DynamicInt32}; As an example, the following is equal to {@code
         * DynamicFloat.constant(2f)}
         *
         * <pre>
         *   DynamicFloat.constant(7f).rem(DynamicInt32.constant(5));
         * </pre>
         *
         * The operation's evaluation order depends only on its position in the expression; no
         * operator precedence rules are applied. See {@link DynamicFloat} for more information on
         * operation evaluation order.
         *
         * @return a new instance of {@link DynamicFloat} containing the result of the operation.
         */
        @SuppressWarnings("KotlinOperator")
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicFloat rem(@NonNull DynamicInt32 other) {
            return new ArithmeticFloatOp.Builder()
                    .setInputLhs(this)
                    .setInputRhs(other.asFloat())
                    .setOperationType(DynamicBuilders.ARITHMETIC_OP_TYPE_MODULO)
                    .build();
        }

        /**
         * Returns a {@link DynamicBool} that is true if the value of this {@link DynamicFloat} and
         * {@code other} are equal, otherwise it's false.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicBool eq(@NonNull DynamicFloat other) {
            return new ComparisonFloatOp.Builder()
                    .setInputLhs(this)
                    .setInputRhs(other)
                    .setOperationType(DynamicBuilders.COMPARISON_OP_TYPE_EQUALS)
                    .build();
        }

        /**
         * Returns a {@link DynamicBool} that is true if the value of this {@link DynamicFloat} and
         * {@code other} are equal, otherwise it's false.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicBool eq(float other) {
            return new ComparisonFloatOp.Builder()
                    .setInputLhs(this)
                    .setInputRhs(constant(other))
                    .setOperationType(DynamicBuilders.COMPARISON_OP_TYPE_EQUALS)
                    .build();
        }

        /**
         * Returns a {@link DynamicBool} that is true if the value of this {@link DynamicFloat} and
         * {@code other} are not equal, otherwise it's false.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicBool ne(@NonNull DynamicFloat other) {
            return new ComparisonFloatOp.Builder()
                    .setInputLhs(this)
                    .setInputRhs(other)
                    .setOperationType(DynamicBuilders.COMPARISON_OP_TYPE_NOT_EQUALS)
                    .build();
        }

        /**
         * Returns a {@link DynamicBool} that is true if the value of this {@link DynamicFloat} and
         * {@code other} are not equal, otherwise it's false.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicBool ne(float other) {
            return new ComparisonFloatOp.Builder()
                    .setInputLhs(this)
                    .setInputRhs(constant(other))
                    .setOperationType(DynamicBuilders.COMPARISON_OP_TYPE_NOT_EQUALS)
                    .build();
        }

        /**
         * Returns a {@link DynamicBool} that is true if the value of this {@link DynamicFloat} is
         * less than {@code other}, otherwise it's false.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicBool lt(@NonNull DynamicFloat other) {
            return new ComparisonFloatOp.Builder()
                    .setInputLhs(this)
                    .setInputRhs(other)
                    .setOperationType(DynamicBuilders.COMPARISON_OP_TYPE_LESS_THAN)
                    .build();
        }

        /**
         * Returns a {@link DynamicBool} that is true if the value of this {@link DynamicFloat} is
         * less than {@code other}, otherwise it's false.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicBool lt(float other) {
            return new ComparisonFloatOp.Builder()
                    .setInputLhs(this)
                    .setInputRhs(constant(other))
                    .setOperationType(DynamicBuilders.COMPARISON_OP_TYPE_LESS_THAN)
                    .build();
        }

        /**
         * Returns a {@link DynamicBool} that is true if the value of this {@link DynamicFloat} is
         * less than or equal to {@code other}, otherwise it's false.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicBool lte(@NonNull DynamicFloat other) {
            return new ComparisonFloatOp.Builder()
                    .setInputLhs(this)
                    .setInputRhs(other)
                    .setOperationType(DynamicBuilders.COMPARISON_OP_TYPE_LESS_THAN_OR_EQUAL_TO)
                    .build();
        }

        /**
         * Returns a {@link DynamicBool} that is true if the value of this {@link DynamicFloat} is
         * less than or equal to {@code other}, otherwise it's false.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicBool lte(float other) {
            return new ComparisonFloatOp.Builder()
                    .setInputLhs(this)
                    .setInputRhs(constant(other))
                    .setOperationType(DynamicBuilders.COMPARISON_OP_TYPE_LESS_THAN_OR_EQUAL_TO)
                    .build();
        }

        /**
         * Returns a {@link DynamicBool} that is true if the value of this {@link DynamicFloat} is
         * greater than {@code other}, otherwise it's false.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicBool gt(@NonNull DynamicFloat other) {
            return new ComparisonFloatOp.Builder()
                    .setInputLhs(this)
                    .setInputRhs(other)
                    .setOperationType(DynamicBuilders.COMPARISON_OP_TYPE_GREATER_THAN)
                    .build();
        }

        /**
         * Returns a {@link DynamicBool} that is true if the value of this {@link DynamicFloat} is
         * greater than {@code other}, otherwise it's false.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicBool gt(float other) {
            return new ComparisonFloatOp.Builder()
                    .setInputLhs(this)
                    .setInputRhs(constant(other))
                    .setOperationType(DynamicBuilders.COMPARISON_OP_TYPE_GREATER_THAN)
                    .build();
        }

        /**
         * Returns a {@link DynamicBool} that is true if the value of this {@link DynamicFloat} is
         * greater than or equal to {@code other}, otherwise it's false.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicBool gte(@NonNull DynamicFloat other) {
            return new ComparisonFloatOp.Builder()
                    .setInputLhs(this)
                    .setInputRhs(other)
                    .setOperationType(DynamicBuilders.COMPARISON_OP_TYPE_GREATER_THAN_OR_EQUAL_TO)
                    .build();
        }

        /**
         * Returns a {@link DynamicBool} that is true if the value of this {@link DynamicFloat} is
         * greater than or equal to {@code other}, otherwise it's false.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicBool gte(float other) {
            return new ComparisonFloatOp.Builder()
                    .setInputLhs(this)
                    .setInputRhs(constant(other))
                    .setOperationType(DynamicBuilders.COMPARISON_OP_TYPE_GREATER_THAN_OR_EQUAL_TO)
                    .build();
        }

        /**
         * Bind the value of this {@link DynamicFloat} to the result of a conditional expression.
         * This will use the value given in either {@link ConditionScope#use} or {@link
         * ConditionScopes.IfTrueScope#elseUse} depending on the value yielded from {@code
         * condition}.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        static ConditionScope<DynamicFloat, Float> onCondition(@NonNull DynamicBool condition) {
            return new ConditionScopes.ConditionScope<>(
                    (trueValue, falseValue) ->
                            new ConditionalFloatOp.Builder()
                                    .setCondition(condition)
                                    .setValueIfTrue(trueValue)
                                    .setValueIfFalse(falseValue)
                                    .build(),
                    DynamicFloat::constant);
        }

        /**
         * Returns a {@link DynamicString} that contains the formatted value of this {@link
         * DynamicFloat} (with default formatting parameters). As an example, for locale en_US, the
         * following is equal to {@code DynamicString.constant("12.346")}
         *
         * <pre>
         *   DynamicFloat.constant(12.34567f).format();
         * </pre>
         *
         * The resulted {@link DynamicString} is subject to being truncated if it's too long.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicString format() {
            return new FloatFormatter.Builder().build().getFloatFormatOp(this);
        }

        /**
         * Returns a {@link DynamicString} that contains the formatted value of this {@link
         * DynamicFloat}. As an example, for locale en_US, the following is equal to {@code
         * DynamicString.constant("0,012.34")}
         *
         * <pre>
         *   DynamicFloat.constant(12.345f)
         *       .format(
         *           new FloatFormatter.Builder().setMaxFractionDigits(2).setMinIntegerDigits(4)
         *                             .setGroupingUsed(true).build());
         * </pre>
         *
         * The resulted {@link DynamicString} is subject to being truncated if it's too long.
         *
         * @param formatter The formatting parameter.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicString format(@NonNull FloatFormatter formatter) {
            return formatter.getFloatFormatOp(this);
        }

        /** Allows formatting {@link DynamicFloat} into a {@link DynamicString}. */
        class FloatFormatter {
            private final FloatFormatOp.Builder mFloatFormatOpBuilder;
            private final FloatFormatOp mFloatFormatOp;

            FloatFormatter(FloatFormatOp.Builder floatFormatOpBuilder) {
                mFloatFormatOpBuilder = floatFormatOpBuilder;
                mFloatFormatOp = floatFormatOpBuilder.build();
            }

            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            FloatFormatOp getFloatFormatOp(@NonNull DynamicFloat dynamicFloat) {
                return mFloatFormatOpBuilder.setInput(dynamicFloat).build();
            }

            /** Returns the minimum number of digits allowed in the fraction portion of a number. */
            @IntRange(from = 0)
            public int getMinFractionDigits() {
                return mFloatFormatOp.getMinFractionDigits();
            }

            /** Returns the maximum number of digits allowed in the fraction portion of a number. */
            @IntRange(from = 0)
            public int getMaxFractionDigits() {
                return mFloatFormatOp.getMaxFractionDigits();
            }

            /** Returns the minimum number of digits allowed in the integer portion of a number. */
            @IntRange(from = 0)
            public int getMinIntegerDigits() {
                return mFloatFormatOp.getMinIntegerDigits();
            }

            /** Returns whether digit grouping is used or not. */
            public boolean isGroupingUsed() {
                return mFloatFormatOp.getGroupingUsed();
            }

            /** Builder to create {@link FloatFormatter} objects. */
            public static final class Builder {
                private static final int MAX_INTEGER_PART_LENGTH = 15;
                private static final int MAX_FRACTION_PART_LENGTH = 15;
                final FloatFormatOp.Builder mBuilder;

                public Builder() {
                    mBuilder = new FloatFormatOp.Builder();
                }

                /**
                 * Sets minimum number of fraction digits for the formatter. Defaults to zero if not
                 * specified. minimumFractionDigits must be <= maximumFractionDigits. If the
                 * condition is not satisfied, then minimumFractionDigits will be used for both
                 * fields.
                 */
                @NonNull
                @RequiresSchemaVersion(major = 1, minor = 200)
                public Builder setMinFractionDigits(@IntRange(from = 0) int minFractionDigits) {
                    mBuilder.setMinFractionDigits(minFractionDigits);
                    return this;
                }

                /**
                 * Sets maximum number of fraction digits for the formatter. Defaults to three if
                 * not specified. minimumFractionDigits must be <= maximumFractionDigits. If the
                 * condition is not satisfied, then minimumFractionDigits will be used for both
                 * fields.
                 */
                @NonNull
                @RequiresSchemaVersion(major = 1, minor = 200)
                public Builder setMaxFractionDigits(@IntRange(from = 0) int maxFractionDigits) {
                    mBuilder.setMaxFractionDigits(maxFractionDigits);
                    return this;
                }

                /**
                 * Sets minimum number of integer digits for the formatter. Defaults to one if not
                 * specified. If minIntegerDigits is zero and the -1 < input < 1, the Integer part
                 * will not appear.
                 */
                @NonNull
                @RequiresSchemaVersion(major = 1, minor = 200)
                public Builder setMinIntegerDigits(@IntRange(from = 0) int minIntegerDigits) {
                    mBuilder.setMinIntegerDigits(minIntegerDigits);
                    return this;
                }

                /**
                 * Sets whether grouping is used for the formatter. Defaults to false if not
                 * specified. If grouping is used, digits will be grouped into digit groups using a
                 * separator. Digit group size and used separator can vary in different
                 * countries/regions. As an example, for locale en_US, the following is equal to
                 * {@code * DynamicString.constant("1,234")}
                 *
                 * <pre>
                 *   DynamicFloat.constant(1234)
                 *       .format(
                 *           new FloatFormatter.Builder()
                 *                           .setGroupingUsed(true).build());
                 * </pre>
                 */
                @NonNull
                @RequiresSchemaVersion(major = 1, minor = 200)
                public Builder setGroupingUsed(boolean groupingUsed) {
                    mBuilder.setGroupingUsed(groupingUsed);
                    return this;
                }

                /** Builds an instance with values accumulated in this Builder. */
                @NonNull
                public FloatFormatter build() {
                    FloatFormatOp op = mBuilder.build();
                    throwIfExceedingMaxValue(
                            "MinFractionDigits",
                            op.getMinFractionDigits(),
                            MAX_FRACTION_PART_LENGTH);
                    throwIfExceedingMaxValue(
                            "MaxFractionDigits",
                            op.getMaxFractionDigits(),
                            MAX_FRACTION_PART_LENGTH);
                    throwIfExceedingMaxValue(
                            "MinIntegerDigits", op.getMinIntegerDigits(), MAX_INTEGER_PART_LENGTH);
                    return new FloatFormatter(mBuilder);
                }

                private static void throwIfExceedingMaxValue(
                        String paramName, int value, int maxValue) {
                    if (value > maxValue) {
                        throw new IllegalArgumentException(
                                String.format(
                                        "%s (%d) is too large. Maximum value for %s is %d",
                                        paramName, value, paramName, maxValue));
                    }
                }
            }
        }

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        Fingerprint getFingerprint();

        /** Builder to create {@link DynamicFloat} objects. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull
            DynamicFloat build();
        }
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static DynamicFloat dynamicFloatFromProto(
            @NonNull DynamicProto.DynamicFloat proto, @Nullable Fingerprint fingerprint) {
        if (proto.hasFixed()) {
            return FixedFloat.fromProto(proto.getFixed(), fingerprint);
        }
        if (proto.hasArithmeticOperation()) {
            return ArithmeticFloatOp.fromProto(proto.getArithmeticOperation(), fingerprint);
        }
        if (proto.hasInt32ToFloatOperation()) {
            return Int32ToFloatOp.fromProto(proto.getInt32ToFloatOperation(), fingerprint);
        }
        if (proto.hasStateSource()) {
            return StateFloatSource.fromProto(proto.getStateSource(), fingerprint);
        }
        if (proto.hasConditionalOp()) {
            return ConditionalFloatOp.fromProto(proto.getConditionalOp(), fingerprint);
        }
        if (proto.hasAnimatableFixed()) {
            return AnimatableFixedFloat.fromProto(proto.getAnimatableFixed(), fingerprint);
        }
        if (proto.hasAnimatableDynamic()) {
            return AnimatableDynamicFloat.fromProto(proto.getAnimatableDynamic(), fingerprint);
        }
        throw new IllegalStateException("Proto was not a recognised instance of DynamicFloat");
    }

    /**
     * Creates a new wrapper instance from the proto. Intended for testing purposes only. An object
     * created using this method can't be added to any other wrapper.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static DynamicFloat dynamicFloatFromProto(@NonNull DynamicProto.DynamicFloat proto) {
        return dynamicFloatFromProto(proto, new Fingerprint(proto.getFingerprint()));
    }

    /** A dynamic boolean type which sources its data from the tile's state. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class StateBoolSource implements DynamicBool {
        private final DynamicProto.StateBoolSource mImpl;
        @Nullable private final Fingerprint mFingerprint;

        StateBoolSource(DynamicProto.StateBoolSource impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the key in the state to bind to. */
        @NonNull
        public String getSourceKey() {
            return mImpl.getSourceKey();
        }

        /** Gets the namespace for the state key. */
        @NonNull
        public String getSourceNamespace() {
            return mImpl.getSourceNamespace();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static StateBoolSource fromProto(
                @NonNull DynamicProto.StateBoolSource proto, @Nullable Fingerprint fingerprint) {
            return new StateBoolSource(proto, fingerprint);
        }

        @NonNull
        static StateBoolSource fromProto(@NonNull DynamicProto.StateBoolSource proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.StateBoolSource toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicBool toDynamicBoolProto() {
            return DynamicProto.DynamicBool.newBuilder().setStateSource(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicBool toDynamicBoolProto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicBool.newBuilder().
                        setStateSource(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicBoolProto();
        }

        @Override
        @NonNull
        public String toString() {
            return "StateBoolSource{"
                    + "sourceKey="
                    + getSourceKey()
                    + ", sourceNamespace="
                    + getSourceNamespace()
                    + "}";
        }

        /** Builder for {@link StateBoolSource}. */
        public static final class Builder implements DynamicBool.Builder {

            private final DynamicProto.StateBoolSource.Builder mImpl =
                    DynamicProto.StateBoolSource.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1818702779);

            public Builder() {}

            /** Sets the key in the state to bind to. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setSourceKey(@NonNull String sourceKey) {
                mImpl.setSourceKey(sourceKey);
                mFingerprint.recordPropertyUpdate(1, sourceKey.hashCode());
                return this;
            }

            /** Sets the name space for the state key. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setSourceNamespace(@NonNull String sourceNamespace) {
                mImpl.setSourceNamespace(sourceNamespace);
                mFingerprint.recordPropertyUpdate(2, sourceNamespace.hashCode());
                return this;
            }

            @Override
            @NonNull
            public StateBoolSource build() {
                return new StateBoolSource(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A comparison operation, operating on two Int32 instances. This implements various comparison
     * operations of the form "boolean result = LHS <op> RHS", where the available operation types
     * are described in {@code ComparisonOpType}.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class ComparisonInt32Op implements DynamicBool {

        private final DynamicProto.ComparisonInt32Op mImpl;
        @Nullable private final Fingerprint mFingerprint;

        ComparisonInt32Op(DynamicProto.ComparisonInt32Op impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the left hand side of the comparison operation. */
        @Nullable
        public DynamicInt32 getInputLhs() {
            if (mImpl.hasInputLhs()) {
                return DynamicBuilders.dynamicInt32FromProto(mImpl.getInputLhs());
            } else {
                return null;
            }
        }

        /** Gets the right hand side of the comparison operation. */
        @Nullable
        public DynamicInt32 getInputRhs() {
            if (mImpl.hasInputRhs()) {
                return DynamicBuilders.dynamicInt32FromProto(mImpl.getInputRhs());
            } else {
                return null;
            }
        }

        /** Gets the type of the operation. */
        @ComparisonOpType
        public int getOperationType() {
            return mImpl.getOperationType().getNumber();
        }

        /** */
        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static ComparisonInt32Op fromProto(
                @NonNull DynamicProto.ComparisonInt32Op proto, @Nullable Fingerprint fingerprint) {
            return new ComparisonInt32Op(proto, fingerprint);
        }

        @NonNull
        static ComparisonInt32Op fromProto(@NonNull DynamicProto.ComparisonInt32Op proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.ComparisonInt32Op toProto() {
            return mImpl;
        }

        /** */
        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicBool toDynamicBoolProto() {
            return DynamicProto.DynamicBool.newBuilder().setInt32Comparison(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicBool toDynamicBoolProto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicBool.newBuilder()
                        .setInt32Comparison(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicBoolProto();
        }

        @Override
        @NonNull
        public String toString() {
            return "ComparisonInt32Op{"
                    + "inputLhs="
                    + getInputLhs()
                    + ", inputRhs="
                    + getInputRhs()
                    + ", operationType="
                    + getOperationType()
                    + "}";
        }

        /** Builder for {@link ComparisonInt32Op}. */
        public static final class Builder implements DynamicBool.Builder {

            private final DynamicProto.ComparisonInt32Op.Builder mImpl =
                    DynamicProto.ComparisonInt32Op.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1112207999);

            public Builder() {}

            /** Sets the left hand side of the comparison operation. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setInputLhs(@NonNull DynamicInt32 inputLhs) {
                mImpl.setInputLhs(inputLhs.toDynamicInt32Proto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(inputLhs.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the right hand side of the comparison operation. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setInputRhs(@NonNull DynamicInt32 inputRhs) {
                mImpl.setInputRhs(inputRhs.toDynamicInt32Proto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(inputRhs.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the type of the operation. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setOperationType(@ComparisonOpType int operationType) {
                mImpl.setOperationType(DynamicProto.ComparisonOpType.forNumber(operationType));
                mFingerprint.recordPropertyUpdate(3, operationType);
                return this;
            }

            @Override
            @NonNull
            public ComparisonInt32Op build() {
                return new ComparisonInt32Op(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A comparison operation, operating on two Float instances. This implements various comparison
     * operations of the form "boolean result = LHS <op> RHS", where the available operation types
     * are described in {@code ComparisonOpType}.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class ComparisonFloatOp implements DynamicBool {

        private final DynamicProto.ComparisonFloatOp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        ComparisonFloatOp(DynamicProto.ComparisonFloatOp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the left hand side of the comparison operation. */
        @Nullable
        public DynamicFloat getInputLhs() {
            if (mImpl.hasInputLhs()) {
                return DynamicBuilders.dynamicFloatFromProto(mImpl.getInputLhs());
            } else {
                return null;
            }
        }

        /** Gets the right hand side of the comparison operation. */
        @Nullable
        public DynamicFloat getInputRhs() {
            if (mImpl.hasInputRhs()) {
                return DynamicBuilders.dynamicFloatFromProto(mImpl.getInputRhs());
            } else {
                return null;
            }
        }

        /** Gets the type of the operation. */
        @ComparisonOpType
        public int getOperationType() {
            return mImpl.getOperationType().getNumber();
        }

        /** */
        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static ComparisonFloatOp fromProto(
                @NonNull DynamicProto.ComparisonFloatOp proto, @Nullable Fingerprint fingerprint) {
            return new ComparisonFloatOp(proto, fingerprint);
        }

        @NonNull
        static ComparisonFloatOp fromProto(@NonNull DynamicProto.ComparisonFloatOp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.ComparisonFloatOp toProto() {
            return mImpl;
        }

        /** */
        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicBool toDynamicBoolProto() {
            return DynamicProto.DynamicBool.newBuilder().setFloatComparison(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicBool toDynamicBoolProto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicBool.newBuilder()
                        .setFloatComparison(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicBoolProto();
        }

        @Override
        @NonNull
        public String toString() {
            return "ComparisonFloatOp{"
                    + "inputLhs="
                    + getInputLhs()
                    + ", inputRhs="
                    + getInputRhs()
                    + ", operationType="
                    + getOperationType()
                    + "}";
        }

        /** Builder for {@link ComparisonFloatOp}. */
        public static final class Builder implements DynamicBool.Builder {

            private final DynamicProto.ComparisonFloatOp.Builder mImpl =
                    DynamicProto.ComparisonFloatOp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1679565270);

            public Builder() {}

            /** Sets the left hand side of the comparison operation. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setInputLhs(@NonNull DynamicFloat inputLhs) {
                mImpl.setInputLhs(inputLhs.toDynamicFloatProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(inputLhs.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the right hand side of the comparison operation. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setInputRhs(@NonNull DynamicFloat inputRhs) {
                mImpl.setInputRhs(inputRhs.toDynamicFloatProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(inputRhs.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the type of the operation. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setOperationType(@ComparisonOpType int operationType) {
                mImpl.setOperationType(DynamicProto.ComparisonOpType.forNumber(operationType));
                mFingerprint.recordPropertyUpdate(3, operationType);
                return this;
            }

            @Override
            @NonNull
            public ComparisonFloatOp build() {
                return new ComparisonFloatOp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A boolean operation which implements a "NOT" operator, i.e. "boolean result = !input". */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class NotBoolOp implements DynamicBool {
        private final DynamicProto.NotBoolOp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        NotBoolOp(DynamicProto.NotBoolOp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the input, whose value to negate. */
        @Nullable
        public DynamicBool getInput() {
            if (mImpl.hasInput()) {
                return DynamicBuilders.dynamicBoolFromProto(mImpl.getInput());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static NotBoolOp fromProto(
                @NonNull DynamicProto.NotBoolOp proto, @Nullable Fingerprint fingerprint) {
            return new NotBoolOp(proto, fingerprint);
        }

        @NonNull
        static NotBoolOp fromProto(@NonNull DynamicProto.NotBoolOp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.NotBoolOp toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicBool toDynamicBoolProto() {
            return DynamicProto.DynamicBool.newBuilder().setNotOp(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicBool toDynamicBoolProto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicBool.newBuilder()
                        .setNotOp(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicBoolProto();
        }

        @Override
        @NonNull
        public String toString() {
            return "NotBoolOp{" + "input=" + getInput() + "}";
        }

        /** Builder for {@link NotBoolOp}. */
        public static final class Builder implements DynamicBool.Builder {
            private final DynamicProto.NotBoolOp.Builder mImpl =
                    DynamicProto.NotBoolOp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(91300638);

            public Builder() {}

            /** Sets the input, whose value to negate. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setInput(@NonNull DynamicBool input) {
                mImpl.setInput(input.toDynamicBoolProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(input.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public NotBoolOp build() {
                return new NotBoolOp(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A logical boolean operator, implementing "boolean result = LHS <op> RHS", for various boolean
     * operators (i.e. AND/OR).
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class LogicalBoolOp implements DynamicBool {
        private final DynamicProto.LogicalBoolOp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        LogicalBoolOp(DynamicProto.LogicalBoolOp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the left hand side of the logical operation. */
        @Nullable
        public DynamicBool getInputLhs() {
            if (mImpl.hasInputLhs()) {
                return DynamicBuilders.dynamicBoolFromProto(mImpl.getInputLhs());
            } else {
                return null;
            }
        }

        /** Gets the right hand side of the logical operation. */
        @Nullable
        public DynamicBool getInputRhs() {
            if (mImpl.hasInputRhs()) {
                return DynamicBuilders.dynamicBoolFromProto(mImpl.getInputRhs());
            } else {
                return null;
            }
        }

        /** Gets the operation type to apply to LHS/RHS. */
        @LogicalOpType
        public int getOperationType() {
            return mImpl.getOperationType().getNumber();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static LogicalBoolOp fromProto(
                @NonNull DynamicProto.LogicalBoolOp proto, @Nullable Fingerprint fingerprint) {
            return new LogicalBoolOp(proto, fingerprint);
        }

        @NonNull
        static LogicalBoolOp fromProto(@NonNull DynamicProto.LogicalBoolOp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.LogicalBoolOp toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicBool toDynamicBoolProto() {
            return DynamicProto.DynamicBool.newBuilder().setLogicalOp(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicBool toDynamicBoolProto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicBool.newBuilder()
                        .setLogicalOp(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicBoolProto();
        }

        @Override
        @NonNull
        public String toString() {
            return "LogicalBoolOp{"
                    + "inputLhs="
                    + getInputLhs()
                    + ", inputRhs="
                    + getInputRhs()
                    + ", operationType="
                    + getOperationType()
                    + "}";
        }

        /** Builder for {@link LogicalBoolOp}. */
        public static final class Builder implements DynamicBool.Builder {
            private final DynamicProto.LogicalBoolOp.Builder mImpl =
                    DynamicProto.LogicalBoolOp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1067523409);

            public Builder() {}

            /** Sets the left hand side of the logical operation. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setInputLhs(@NonNull DynamicBool inputLhs) {
                mImpl.setInputLhs(inputLhs.toDynamicBoolProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(inputLhs.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the right hand side of the logical operation. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setInputRhs(@NonNull DynamicBool inputRhs) {
                mImpl.setInputRhs(inputRhs.toDynamicBoolProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(inputRhs.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the operation type to apply to LHS/RHS. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setOperationType(@LogicalOpType int operationType) {
                mImpl.setOperationType(DynamicProto.LogicalOpType.forNumber(operationType));
                mFingerprint.recordPropertyUpdate(3, operationType);
                return this;
            }

            @Override
            @NonNull
            public LogicalBoolOp build() {
                return new LogicalBoolOp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** Interface defining a dynamic boolean type. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public interface DynamicBool extends DynamicType {
        /** Get the protocol buffer representation of this object, without fingerprint. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        DynamicProto.DynamicBool toDynamicBoolProto();

        /** Get the protocol buffer representation of this object, with or without fingerprint. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        DynamicProto.DynamicBool toDynamicBoolProto(boolean withFingerprint);

        /**
         * Creates a {@link DynamicBool} from a byte array generated by {@link
         * #toDynamicBoolByteArray()}.
         *
         * @throws IllegalArgumentException if the byte array does not contain a valid serialization
         */
        @NonNull
        static DynamicBool fromByteArray(@NonNull byte[] byteArray) {
            return fromByteArray(byteArray, 0, byteArray.length);
        }

        /**
         * Creates a {@link DynamicBool} from the provided byte array at the provided offset and
         * length, that was generated by one of the {@link #toDynamicBoolByteArray} overloads.
         *
         * @throws IllegalArgumentException if the byte array does not contain a valid serialization
         *     in the provided offset and length
         */
        @NonNull
        static DynamicBool fromByteArray(@NonNull byte[] byteArray, int offset, int length) {
            try {
                return dynamicBoolFromProto(
                        DynamicProto.DynamicBool.parseFrom(
                                CodedInputStream.newInstance(byteArray, offset, length),
                                ExtensionRegistryLite.getEmptyRegistry()));
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        "Byte array could not be parsed into DynamicBool", e);
            }
        }

        /**
         * Serializes the {@link DynamicBool} into a new byte array that can later be used with
         * {@link #fromByteArray(byte[])}.
         */
        @NonNull
        default byte[] toDynamicBoolByteArray() {
            return toDynamicBoolProto(/* withFingerprint= */ true).toByteArray();
        }

        /**
         * Serializes the {@link DynamicBool} into the provided byte array, returning the amount of
         * bytes written, that can later be used with {@code DynamicBool.fromByteArray(byteArray, 0,
         * bytesWritten)}.
         *
         * @throws IllegalArgumentException if the byte array is too small
         */
        default int toDynamicBoolByteArray(@NonNull byte[] byteArray) {
            return toDynamicBoolByteArray(byteArray, 0, byteArray.length);
        }

        /**
         * Serializes the {@link DynamicBool} into the provided byte array, returning the amount of
         * bytes written, limited by the provided offset and length, that can later be used with
         * {@code DynamicBool.fromByteArray(byteArray, offset, bytesWritten)}.
         *
         * @throws IllegalArgumentException if the byte array is too small
         */
        default int toDynamicBoolByteArray(@NonNull byte[] byteArray, int offset, int length) {
            CodedOutputStream stream = CodedOutputStream.newInstance(byteArray, offset, length);
            try {
                toDynamicBoolProto(/* withFingerprint= */ true).writeTo(stream);
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        "Provided byte array not large enough to contain this DynamicBool", e);
            }
            return stream.getTotalBytesWritten();
        }

        /** Creates a constant-valued {@link DynamicBool}. */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        static DynamicBool constant(boolean constant) {
            return new FixedBool.Builder().setValue(constant).build();
        }

        /**
         * Creates a {@link DynamicBool} that is bound to the value of an item of the State.
         *
         * @param dynamicDataKey The key to a {@link DynamicDataValue} with a boolean value.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        static DynamicBool from(@NonNull DynamicDataKey<DynamicBool> dynamicDataKey) {
            return new StateBoolSource.Builder()
                    .setSourceKey(dynamicDataKey.getKey())
                    .setSourceNamespace(dynamicDataKey.getNamespace())
                    .build();
        }

        /**
         * Returns a {@link DynamicBool} that has the opposite value of this {@link DynamicBool}.
         * i.e. {code result = !this}
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicBool negate() {
            return new NotBoolOp.Builder().setInput(this).build();
        }

        /**
         * Returns a {@link DynamicBool} that is true if this {@link DynamicBool} and {@code input}
         * are both true, otherwise it is false. i.e. {@code boolean result = this && input}
         *
         * @param input The right hand operand of the "and" operation.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicBool and(@NonNull DynamicBool input) {
            return new LogicalBoolOp.Builder()
                    .setInputLhs(this)
                    .setInputRhs(input)
                    .setOperationType(DynamicBuilders.LOGICAL_OP_TYPE_AND)
                    .build();
        }

        /**
         * Returns a {@link DynamicBool} that is true if this {@link DynamicBool} or {@code input}
         * are true, otherwise it is false. i.e. {@code boolean result = this || input}
         *
         * @param input The right hand operand of the "or" operation.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicBool or(@NonNull DynamicBool input) {
            return new LogicalBoolOp.Builder()
                    .setInputLhs(this)
                    .setInputRhs(input)
                    .setOperationType(DynamicBuilders.LOGICAL_OP_TYPE_OR)
                    .build();
        }

        /**
         * Returns a {@link DynamicBool} that is true if the value of this {@link DynamicBool} and
         * {@code other} are equal, otherwise it's false.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicBool eq(@NonNull DynamicBool other) {
            return new LogicalBoolOp.Builder()
                    .setInputLhs(this)
                    .setInputRhs(other)
                    .setOperationType(DynamicBuilders.LOGICAL_OP_TYPE_EQUAL)
                    .build();
        }

        /**
         * Returns a {@link DynamicBool} that is true if the value of this {@link DynamicBool} and
         * {@code other} are not equal, otherwise it's false.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicBool ne(@NonNull DynamicBool other) {
            return new LogicalBoolOp.Builder()
                    .setInputLhs(this)
                    .setInputRhs(other)
                    .setOperationType(DynamicBuilders.LOGICAL_OP_TYPE_NOT_EQUAL)
                    .build();
        }

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        Fingerprint getFingerprint();

        /** Builder to create {@link DynamicBool} objects. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull
            DynamicBool build();
        }
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static DynamicBool dynamicBoolFromProto(
            @NonNull DynamicProto.DynamicBool proto, @Nullable Fingerprint fingerprint) {
        if (proto.hasFixed()) {
            return FixedBool.fromProto(proto.getFixed(), fingerprint);
        }
        if (proto.hasStateSource()) {
            return StateBoolSource.fromProto(proto.getStateSource(), fingerprint);
        }
        if (proto.hasInt32Comparison()) {
            return ComparisonInt32Op.fromProto(proto.getInt32Comparison(), fingerprint);
        }
        if (proto.hasNotOp()) {
            return NotBoolOp.fromProto(proto.getNotOp(), fingerprint);
        }
        if (proto.hasLogicalOp()) {
            return LogicalBoolOp.fromProto(proto.getLogicalOp(), fingerprint);
        }
        if (proto.hasFloatComparison()) {
            return ComparisonFloatOp.fromProto(proto.getFloatComparison(), fingerprint);
        }
        throw new IllegalStateException("Proto was not a recognised instance of DynamicBool");
    }

    /**
     * Creates a new wrapper instance from the proto. Intended for testing purposes only. An object
     * created using this method can't be added to any other wrapper.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static DynamicBool dynamicBoolFromProto(@NonNull DynamicProto.DynamicBool proto) {
        return dynamicBoolFromProto(proto, new Fingerprint(proto.getFingerprint()));
    }

    /** A dynamic Color which sources its data from the tile's state. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class StateColorSource implements DynamicColor {
        private final DynamicProto.StateColorSource mImpl;
        @Nullable private final Fingerprint mFingerprint;

        StateColorSource(DynamicProto.StateColorSource impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the key in the state to bind to. */
        @NonNull
        public String getSourceKey() {
            return mImpl.getSourceKey();
        }

        /** Gets the namespace for the state key. */
        @NonNull
        public String getSourceNamespace() {
            return mImpl.getSourceNamespace();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static StateColorSource fromProto(
                @NonNull DynamicProto.StateColorSource proto, @Nullable Fingerprint fingerprint) {
            return new StateColorSource(proto, fingerprint);
        }

        @NonNull
        static StateColorSource fromProto(@NonNull DynamicProto.StateColorSource proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.StateColorSource toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicColor toDynamicColorProto() {
            return DynamicProto.DynamicColor.newBuilder().setStateSource(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicColor toDynamicColorProto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicColor.newBuilder()
                        .setStateSource(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicColorProto();
        }

        @Override
        @NonNull
        public String toString() {
            return "StateColorSource{"
                    + "sourceKey="
                    + getSourceKey()
                    + ", sourceNamespace="
                    + getSourceNamespace()
                    + "}";
        }

        /** Builder for {@link StateColorSource}. */
        public static final class Builder implements DynamicColor.Builder {
            private final DynamicProto.StateColorSource.Builder mImpl =
                    DynamicProto.StateColorSource.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1981221690);

            public Builder() {}

            /** Sets the key in the state to bind to. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setSourceKey(@NonNull String sourceKey) {
                mImpl.setSourceKey(sourceKey);
                mFingerprint.recordPropertyUpdate(1, sourceKey.hashCode());
                return this;
            }

            /** Sets the name space for the state key. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setSourceNamespace(@NonNull String sourceNamespace) {
                mImpl.setSourceNamespace(sourceNamespace);
                mFingerprint.recordPropertyUpdate(2, sourceNamespace.hashCode());
                return this;
            }

            @Override
            @NonNull
            public StateColorSource build() {
                return new StateColorSource(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A static interpolation node, between two fixed color values. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class AnimatableFixedColor implements DynamicColor {
        private final DynamicProto.AnimatableFixedColor mImpl;
        @Nullable private final Fingerprint mFingerprint;

        AnimatableFixedColor(
                DynamicProto.AnimatableFixedColor impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the color value (in ARGB format) to start animating from. */
        @ColorInt
        public int getFromArgb() {
            return mImpl.getFromArgb();
        }

        /** Gets the color value (in ARGB format) to animate to. */
        @ColorInt
        public int getToArgb() {
            return mImpl.getToArgb();
        }

        /** Gets the animation parameters for duration, delay, etc. */
        @Nullable
        public AnimationSpec getAnimationSpec() {
            if (mImpl.hasAnimationSpec()) {
                return AnimationSpec.fromProto(mImpl.getAnimationSpec());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static AnimatableFixedColor fromProto(
                @NonNull DynamicProto.AnimatableFixedColor proto,
                @Nullable Fingerprint fingerprint) {
            return new AnimatableFixedColor(proto, fingerprint);
        }

        @NonNull
        static AnimatableFixedColor fromProto(@NonNull DynamicProto.AnimatableFixedColor proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.AnimatableFixedColor toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicColor toDynamicColorProto() {
            return DynamicProto.DynamicColor.newBuilder().setAnimatableFixed(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicColor toDynamicColorProto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicColor.newBuilder()
                        .setAnimatableFixed(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicColorProto();
        }

        @Override
        @NonNull
        public String toString() {
            return "AnimatableFixedColor{"
                    + "fromArgb="
                    + getFromArgb()
                    + ", toArgb="
                    + getToArgb()
                    + ", animationSpec="
                    + getAnimationSpec()
                    + "}";
        }

        /** Builder for {@link AnimatableFixedColor}. */
        public static final class Builder implements DynamicColor.Builder {
            private final DynamicProto.AnimatableFixedColor.Builder mImpl =
                    DynamicProto.AnimatableFixedColor.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(2051778294);

            public Builder() {}

            /** Sets the color value (in ARGB format) to start animating from. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setFromArgb(@ColorInt int fromArgb) {
                mImpl.setFromArgb(fromArgb);
                mFingerprint.recordPropertyUpdate(1, fromArgb);
                return this;
            }

            /** Sets the color value (in ARGB format) to animate to. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setToArgb(@ColorInt int toArgb) {
                mImpl.setToArgb(toArgb);
                mFingerprint.recordPropertyUpdate(2, toArgb);
                return this;
            }

            /** Sets the animation parameters for duration, delay, etc. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setAnimationSpec(@NonNull AnimationSpec animationSpec) {
                mImpl.setAnimationSpec(animationSpec.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(animationSpec.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public AnimatableFixedColor build() {
                return new AnimatableFixedColor(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A dynamic interpolation node. This will watch the value of its input and, when the first
     * update arrives, immediately emit that value. On subsequent updates, it will animate between
     * the old and new values.
     *
     * <p>If this node receives an invalid value (e.g. as a result of an upstream node having no
     * value), then it will emit a single invalid value, and forget its "stored" value. The next
     * valid value that arrives is then used as the "first" value again.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class AnimatableDynamicColor implements DynamicColor {
        private final DynamicProto.AnimatableDynamicColor mImpl;
        @Nullable private final Fingerprint mFingerprint;

        AnimatableDynamicColor(
                DynamicProto.AnimatableDynamicColor impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value to watch, and animate when it changes. */
        @Nullable
        public DynamicColor getInput() {
            if (mImpl.hasInput()) {
                return DynamicBuilders.dynamicColorFromProto(mImpl.getInput());
            } else {
                return null;
            }
        }

        /** Gets the animation parameters for duration, delay, etc. */
        @Nullable
        public AnimationSpec getAnimationSpec() {
            if (mImpl.hasAnimationSpec()) {
                return AnimationSpec.fromProto(mImpl.getAnimationSpec());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static AnimatableDynamicColor fromProto(
                @NonNull DynamicProto.AnimatableDynamicColor proto,
                @Nullable Fingerprint fingerprint) {
            return new AnimatableDynamicColor(proto, fingerprint);
        }

        @NonNull
        static AnimatableDynamicColor fromProto(
                @NonNull DynamicProto.AnimatableDynamicColor proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.AnimatableDynamicColor toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicColor toDynamicColorProto() {
            return DynamicProto.DynamicColor.newBuilder().setAnimatableDynamic(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicColor toDynamicColorProto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicColor.newBuilder()
                        .setAnimatableDynamic(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicColorProto();
        }

        @Override
        @NonNull
        public String toString() {
            return "AnimatableDynamicColor{"
                    + "input="
                    + getInput()
                    + ", animationSpec="
                    + getAnimationSpec()
                    + "}";
        }

        /** Builder for {@link AnimatableDynamicColor}. */
        public static final class Builder implements DynamicColor.Builder {
            private final DynamicProto.AnimatableDynamicColor.Builder mImpl =
                    DynamicProto.AnimatableDynamicColor.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-193597422);

            public Builder() {}

            /** Sets the value to watch, and animate when it changes. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setInput(@NonNull DynamicColor input) {
                mImpl.setInput(input.toDynamicColorProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(input.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the animation parameters for duration, delay, etc. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setAnimationSpec(@NonNull AnimationSpec animationSpec) {
                mImpl.setAnimationSpec(animationSpec.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(animationSpec.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public AnimatableDynamicColor build() {
                return new AnimatableDynamicColor(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A conditional operator which yields a color depending on the boolean operand. This
     * implements:
     *
     * <pre>{@code
     * color result = condition ? value_if_true : value_if_false
     * }</pre>
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class ConditionalColorOp implements DynamicColor {
        private final DynamicProto.ConditionalColorOp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        ConditionalColorOp(
                DynamicProto.ConditionalColorOp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the condition to use. */
        @Nullable
        public DynamicBool getCondition() {
            if (mImpl.hasCondition()) {
                return DynamicBuilders.dynamicBoolFromProto(mImpl.getCondition());
            } else {
                return null;
            }
        }

        /** Gets the color to yield if condition is true. */
        @Nullable
        public DynamicColor getValueIfTrue() {
            if (mImpl.hasValueIfTrue()) {
                return DynamicBuilders.dynamicColorFromProto(mImpl.getValueIfTrue());
            } else {
                return null;
            }
        }

        /** Gets the color to yield if condition is false. */
        @Nullable
        public DynamicColor getValueIfFalse() {
            if (mImpl.hasValueIfFalse()) {
                return DynamicBuilders.dynamicColorFromProto(mImpl.getValueIfFalse());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static ConditionalColorOp fromProto(
                @NonNull DynamicProto.ConditionalColorOp proto, @Nullable Fingerprint fingerprint) {
            return new ConditionalColorOp(proto, fingerprint);
        }

        @NonNull
        static ConditionalColorOp fromProto(@NonNull DynamicProto.ConditionalColorOp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.ConditionalColorOp toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicColor toDynamicColorProto() {
            return DynamicProto.DynamicColor.newBuilder().setConditionalOp(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicColor toDynamicColorProto(boolean withFringerprint) {
            if (withFringerprint) {
                return DynamicProto.DynamicColor.newBuilder()
                        .setConditionalOp(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicColorProto();
        }

        @Override
        @NonNull
        public String toString() {
            return "ConditionalColorOp{"
                    + "condition="
                    + getCondition()
                    + ", valueIfTrue="
                    + getValueIfTrue()
                    + ", valueIfFalse="
                    + getValueIfFalse()
                    + "}";
        }

        /** Builder for {@link ConditionalColorOp}. */
        public static final class Builder implements DynamicColor.Builder {
            private final DynamicProto.ConditionalColorOp.Builder mImpl =
                    DynamicProto.ConditionalColorOp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1961850082);

            public Builder() {}

            /** Sets the condition to use. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setCondition(@NonNull DynamicBool condition) {
                mImpl.setCondition(condition.toDynamicBoolProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(condition.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the color to yield if condition is true. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setValueIfTrue(@NonNull DynamicColor valueIfTrue) {
                mImpl.setValueIfTrue(valueIfTrue.toDynamicColorProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(valueIfTrue.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the color to yield if condition is false. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setValueIfFalse(@NonNull DynamicColor valueIfFalse) {
                mImpl.setValueIfFalse(valueIfFalse.toDynamicColorProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(valueIfFalse.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public ConditionalColorOp build() {
                return new ConditionalColorOp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** Interface defining a dynamic color type. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public interface DynamicColor extends DynamicType {
        /** Get the protocol buffer representation of this object, without fingerprint. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        DynamicProto.DynamicColor toDynamicColorProto();

        /** Get the protocol buffer representation of this object, with or without fingerprint. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        DynamicProto.DynamicColor toDynamicColorProto(boolean withFingerprint);


        /**
         * Creates a {@link DynamicColor} from a byte array generated by {@link
         * #toDynamicColorByteArray()}.
         *
         * @throws IllegalArgumentException if the byte array does not contain a valid serialization
         */
        @NonNull
        static DynamicColor fromByteArray(@NonNull byte[] byteArray) {
            return fromByteArray(byteArray, 0, byteArray.length);
        }

        /**
         * Creates a {@link DynamicColor} from the provided byte array at the provided offset and
         * length, that was generated by one of the {@link #toDynamicColorByteArray} overloads.
         *
         * @throws IllegalArgumentException if the byte array does not contain a valid serialization
         *     in the provided offset and length
         */
        @NonNull
        static DynamicColor fromByteArray(@NonNull byte[] byteArray, int offset, int length) {
            try {
                return dynamicColorFromProto(
                        DynamicProto.DynamicColor.parseFrom(
                                CodedInputStream.newInstance(byteArray, offset, length),
                                ExtensionRegistryLite.getEmptyRegistry()));
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        "Byte array could not be parsed into DynamicColor", e);
            }
        }

        /**
         * Serializes the {@link DynamicColor} into a new byte array that can later be used with
         * {@link #fromByteArray(byte[])}.
         */
        @NonNull
        default byte[] toDynamicColorByteArray() {
            return toDynamicColorProto(/* withFingerprint= */ true).toByteArray();
        }

        /**
         * Serializes the {@link DynamicColor} into the provided byte array, returning the amount of
         * bytes written, that can later be used with {@code DynamicColor.fromByteArray(byteArray,
         * 0, bytesWritten)}.
         *
         * @throws IllegalArgumentException if the byte array is too small
         */
        default int toDynamicColorByteArray(@NonNull byte[] byteArray) {
            return toDynamicColorByteArray(byteArray, 0, byteArray.length);
        }

        /**
         * Serializes the {@link DynamicColor} into the provided byte array, returning the amount of
         * bytes written, limited by the provided offset and length, that can later be used with
         * {@code DynamicColor.fromByteArray(byteArray, offset, bytesWritten)}.
         *
         * @throws IllegalArgumentException if the byte array is too small
         */
        default int toDynamicColorByteArray(@NonNull byte[] byteArray, int offset, int length) {
            CodedOutputStream stream = CodedOutputStream.newInstance(byteArray, offset, length);
            try {
                toDynamicColorProto(/* withFingerprint= */ true).writeTo(stream);
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        "Provided byte array not large enough to contain this DynamicColor", e);
            }
            return stream.getTotalBytesWritten();
        }

        /** Creates a constant-valued {@link DynamicColor}. */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        static DynamicColor constant(@ColorInt int constant) {
            return new FixedColor.Builder().setArgb(constant).build();
        }

        /**
         * Creates a {@link DynamicColor} that is bound to the value of an item of the State.
         *
         * @param dynamicDataKey The source key to a {@link DynamicDataValue} with a color value.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        static DynamicColor from(@NonNull DynamicDataKey<DynamicColor> dynamicDataKey) {
            return new StateColorSource.Builder()
                    .setSourceKey(dynamicDataKey.getKey())
                    .setSourceNamespace(dynamicDataKey.getNamespace())
                    .build();
        }

        /**
         * Creates a {@link DynamicColor} which will animate over the range of colors from {@code
         * start} to {@code end}.
         *
         * @param start The start value of the range.
         * @param end The end value of the range.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        static DynamicColor animate(@ColorInt int start, @ColorInt int end) {
            return new AnimatableFixedColor.Builder().setFromArgb(start).setToArgb(end).build();
        }

        /**
         * Creates a {@link DynamicColor} which will animate over the range of colors from {@code
         * start} to {@code end} with the given animation parameters.
         *
         * @param start The start value of the range.
         * @param end The end value of the range.
         * @param animationSpec The animation parameters.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        static DynamicColor animate(
                @ColorInt int start, @ColorInt int end, @NonNull AnimationSpec animationSpec) {
            return new AnimatableFixedColor.Builder()
                    .setFromArgb(start)
                    .setToArgb(end)
                    .setAnimationSpec(animationSpec)
                    .build();
        }

        /**
         * Creates a {@link DynamicColor} that is bound to the value of an item of the State. Every
         * time the state value changes, this {@link DynamicColor} will animate from its current
         * value to the new value (from the state).
         *
         * @param dynamicDataKey The source key to a {@link DynamicDataValue} with a color value.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        static DynamicColor animate(@NonNull DynamicDataKey<DynamicColor> dynamicDataKey) {
            return new AnimatableDynamicColor.Builder().setInput(from(dynamicDataKey)).build();
        }

        /**
         * Creates a {@link DynamicColor} that is bound to the value of an item of the State. Every
         * time the state value changes, this {@link DynamicColor} will animate from its current
         * value to the new value (from the state).
         *
         * @param dynamicDataKey The source key to a {@link DynamicDataValue} with a color value.
         * @param animationSpec The animation parameters.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        static DynamicColor animate(
                @NonNull DynamicDataKey<DynamicColor> dynamicDataKey,
                @NonNull AnimationSpec animationSpec) {
            return new AnimatableDynamicColor.Builder()
                    .setInput(from(dynamicDataKey))
                    .setAnimationSpec(animationSpec)
                    .build();
        }

        /**
         * Returns a {@link DynamicColor} that is bound to the value of this {@link DynamicColor}
         * and every time its value is changing, it animates from its current value to the new
         * value.
         *
         * @param animationSpec The animation parameters.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicColor animate(@NonNull AnimationSpec animationSpec) {
            return new AnimatableDynamicColor.Builder()
                    .setInput(this)
                    .setAnimationSpec(animationSpec)
                    .build();
        }

        /**
         * Returns a {@link DynamicColor} that is bound to the value of this {@link DynamicColor}
         * and every time its value is changing, it animates from its current value to the new
         * value.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicColor animate() {
            return new AnimatableDynamicColor.Builder().setInput(this).build();
        }

        /**
         * Bind the value of this {@link DynamicColor} to the result of a conditional expression.
         * This will use the value given in either {@link ConditionScope#use} or {@link
         * ConditionScopes.IfTrueScope#elseUse} depending on the value yielded from {@code
         * condition}.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        static ConditionScope<DynamicColor, Integer> onCondition(@NonNull DynamicBool condition) {
            return new ConditionScopes.ConditionScope<>(
                    (trueValue, falseValue) ->
                            new ConditionalColorOp.Builder()
                                    .setCondition(condition)
                                    .setValueIfTrue(trueValue)
                                    .setValueIfFalse(falseValue)
                                    .build(),
                    DynamicColor::constant);
        }

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        Fingerprint getFingerprint();

        /** Builder to create {@link DynamicColor} objects. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull
            DynamicColor build();
        }
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static DynamicColor dynamicColorFromProto(
            @NonNull DynamicProto.DynamicColor proto, @Nullable Fingerprint fingerprint) {
        if (proto.hasFixed()) {
            return FixedColor.fromProto(proto.getFixed(), fingerprint);
        }
        if (proto.hasStateSource()) {
            return StateColorSource.fromProto(proto.getStateSource(), fingerprint);
        }
        if (proto.hasAnimatableFixed()) {
            return AnimatableFixedColor.fromProto(proto.getAnimatableFixed(), fingerprint);
        }
        if (proto.hasAnimatableDynamic()) {
            return AnimatableDynamicColor.fromProto(proto.getAnimatableDynamic(), fingerprint);
        }
        if (proto.hasConditionalOp()) {
            return ConditionalColorOp.fromProto(proto.getConditionalOp(), fingerprint);
        }
        throw new IllegalStateException("Proto was not a recognised instance of DynamicColor");
    }

    /**
     * Creates a new wrapper instance from the proto. Intended for testing purposes only. An object
     * created using this method can't be added to any other wrapper.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static DynamicColor dynamicColorFromProto(@NonNull DynamicProto.DynamicColor proto) {
        return dynamicColorFromProto(proto, new Fingerprint(proto.getFingerprint()));
    }

    /** A dynamic time instant that sources its value from the platform. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class PlatformTimeSource implements DynamicInstant {
        private final DynamicProto.PlatformTimeSource mImpl;
        @Nullable private final Fingerprint mFingerprint;

        PlatformTimeSource(
                DynamicProto.PlatformTimeSource impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static PlatformTimeSource fromProto(
                @NonNull DynamicProto.PlatformTimeSource proto, @Nullable Fingerprint fingerprint) {
            return new PlatformTimeSource(proto, fingerprint);
        }

        @NonNull
        static PlatformTimeSource fromProto(@NonNull DynamicProto.PlatformTimeSource proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.PlatformTimeSource toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicInstant toDynamicInstantProto() {
            return DynamicProto.DynamicInstant.newBuilder().setPlatformSource(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicInstant toDynamicInstantProto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicInstant.newBuilder()
                        .setPlatformSource(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicInstantProto();
        }

        @Override
        @NonNull
        public String toString() {
            return "PlatformTimeSource";
        }

        /** Builder for {@link PlatformTimeSource}. */
        public static final class Builder implements DynamicInstant.Builder {
            private final DynamicProto.PlatformTimeSource.Builder mImpl =
                    DynamicProto.PlatformTimeSource.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1895976938);

            public Builder() {}

            @Override
            @NonNull
            public PlatformTimeSource build() {
                return new PlatformTimeSource(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A conditional operator which yields a instant depending on the boolean operand. This
     * implements:
     *
     * <pre>{@code
     * instant result = condition ? value_if_true : value_if_false
     * }</pre>
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class ConditionalInstantOp implements DynamicInstant {
        private final DynamicProto.ConditionalInstantOp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        ConditionalInstantOp(
                DynamicProto.ConditionalInstantOp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the condition to use. */
        @Nullable
        public DynamicBool getCondition() {
            if (mImpl.hasCondition()) {
                return DynamicBuilders.dynamicBoolFromProto(mImpl.getCondition());
            } else {
                return null;
            }
        }

        /** Gets the instant to yield if condition is true. */
        @Nullable
        public DynamicInstant getValueIfTrue() {
            if (mImpl.hasValueIfTrue()) {
                return DynamicBuilders.dynamicInstantFromProto(mImpl.getValueIfTrue());
            } else {
                return null;
            }
        }

        /** Gets the instant to yield if condition is false. */
        @Nullable
        public DynamicInstant getValueIfFalse() {
            if (mImpl.hasValueIfFalse()) {
                return DynamicBuilders.dynamicInstantFromProto(mImpl.getValueIfFalse());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static ConditionalInstantOp fromProto(
                @NonNull DynamicProto.ConditionalInstantOp proto,
                @Nullable Fingerprint fingerprint) {
            return new ConditionalInstantOp(proto, fingerprint);
        }

        @NonNull
        static ConditionalInstantOp fromProto(@NonNull DynamicProto.ConditionalInstantOp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.ConditionalInstantOp toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicInstant toDynamicInstantProto() {
            return DynamicProto.DynamicInstant.newBuilder().setConditionalOp(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicInstant toDynamicInstantProto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicInstant.newBuilder()
                        .setConditionalOp(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }

            return toDynamicInstantProto();
        }

        @Override
        @NonNull
        public String toString() {
            return "ConditionalInstantOp{"
                    + "condition="
                    + getCondition()
                    + ", valueIfTrue="
                    + getValueIfTrue()
                    + ", valueIfFalse="
                    + getValueIfFalse()
                    + "}";
        }

        /** Builder for {@link ConditionalInstantOp}. */
        public static final class Builder implements DynamicInstant.Builder {
            private final DynamicProto.ConditionalInstantOp.Builder mImpl =
                    DynamicProto.ConditionalInstantOp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1479466239);

            public Builder() {}

            /** Sets the condition to use. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setCondition(@NonNull DynamicBool condition) {
                mImpl.setCondition(condition.toDynamicBoolProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(condition.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the instant to yield if condition is true. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setValueIfTrue(@NonNull DynamicInstant valueIfTrue) {
                mImpl.setValueIfTrue(valueIfTrue.toDynamicInstantProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(valueIfTrue.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the instant to yield if condition is false. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setValueIfFalse(@NonNull DynamicInstant valueIfFalse) {
                mImpl.setValueIfFalse(valueIfFalse.toDynamicInstantProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(valueIfFalse.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public ConditionalInstantOp build() {
                return new ConditionalInstantOp(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * Interface defining a dynamic time instant type.
     *
     * <p>{@link DynamicInstant} precision is seconds. Thus, any time or duration operation will
     * operate on that precision level.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public interface DynamicInstant extends DynamicType {
        /** Get the protocol buffer representation of this object, without fingerprint. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        DynamicProto.DynamicInstant toDynamicInstantProto();

        /** Get the protocol buffer representation of this object, with or without fingerprint. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        DynamicProto.DynamicInstant toDynamicInstantProto(boolean withFingerprint);

        /**
         * Creates a {@link DynamicInstant} from a byte array generated by {@link
         * #toDynamicInstantByteArray()}.
         *
         * @throws IllegalArgumentException if the byte array does not contain a valid serialization
         */
        @NonNull
        static DynamicInstant fromByteArray(@NonNull byte[] byteArray) {
            return fromByteArray(byteArray, 0, byteArray.length);
        }

        /**
         * Creates a {@link DynamicInstant} from the provided byte array at the provided offset and
         * length, that was generated by one of the {@link #toDynamicInstantByteArray} overloads.
         *
         * @throws IllegalArgumentException if the byte array does not contain a valid serialization
         *     in the provided offset and length
         */
        @NonNull
        static DynamicInstant fromByteArray(@NonNull byte[] byteArray, int offset, int length) {
            try {
                return dynamicInstantFromProto(
                        DynamicProto.DynamicInstant.parseFrom(
                                CodedInputStream.newInstance(byteArray, offset, length),
                                ExtensionRegistryLite.getEmptyRegistry()));
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        "Byte array could not be parsed into DynamicInstant", e);
            }
        }

        /**
         * Serializes the {@link DynamicInstant} into a new byte array that can later be used with
         * {@link #fromByteArray(byte[])}.
         */
        @NonNull
        default byte[] toDynamicInstantByteArray() {
            return toDynamicInstantProto(/* withFingerprint= */ true).toByteArray();
        }

        /**
         * Serializes the {@link DynamicInstant} into the provided byte array, returning the amount
         * of bytes written, that can later be used with {@code DynamicInstant.fromByteArray(
         * byteArray, 0, bytesWritten)}.
         *
         * @throws IllegalArgumentException if the byte array is too small
         */
        default int toDynamicInstantByteArray(@NonNull byte[] byteArray) {
            return toDynamicInstantByteArray(byteArray, 0, byteArray.length);
        }

        /**
         * Serializes the {@link DynamicInstant} into the provided byte array, returning the amount
         * of bytes written, limited by the provided offset and length, that can later be used with
         * {@code DynamicInstant.fromByteArray(byteArray, offset, bytesWritten)}.
         *
         * @throws IllegalArgumentException if the byte array is too small
         */
        default int toDynamicInstantByteArray(@NonNull byte[] byteArray, int offset, int length) {
            CodedOutputStream stream = CodedOutputStream.newInstance(byteArray, offset, length);
            try {
                toDynamicInstantProto(/* withFingerprint= */ true).writeTo(stream);
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        "Provided byte array not large enough to contain this DynamicInstant", e);
            }
            return stream.getTotalBytesWritten();
        }

        /**
         * Creates a {@link DynamicInstant} that is bound to the value of an item of the State.
         *
         * @param dynamicDataKey The source key to a {@link DynamicDataValue} with an {@link
         *     Instant} value.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 300)
        static DynamicInstant from(@NonNull DynamicDataKey<DynamicInstant> dynamicDataKey) {
            return new StateInstantSource.Builder()
                    .setSourceKey(dynamicDataKey.getKey())
                    .setSourceNamespace(dynamicDataKey.getNamespace())
                    .build();
        }

        /**
         * Creates a constant-valued {@link DynamicInstant} from an {@link Instant}. If {@link
         * Instant} precision is greater than seconds, then any excess precision information will be
         * dropped.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        static DynamicInstant withSecondsPrecision(@NonNull Instant instant) {
            return new FixedInstant.Builder().setEpochSeconds(instant.getEpochSecond()).build();
        }

        /**
         * Creates a {@link DynamicInstant} that updates its value periodically from the system
         * time.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        static DynamicInstant platformTimeWithSecondsPrecision() {
            return new PlatformTimeSource.Builder().build();
        }

        /**
         * Returns duration between the two {@link DynamicInstant} instances as a {@link
         * DynamicDuration}. The resulted duration is inclusive of the start instant and exclusive
         * of the end; As an example, the following expression yields a duration object representing
         * 10 seconds:
         *
         * <pre>
         *   DynamicInstant.withSecondsPrecision(Instant.ofEpochSecond(10L))
         *      .durationUntil(DynamicInstant.withSecondsPrecision(Instant.ofEpochSecond(20L)));
         * </pre>
         *
         * @return a new instance of {@link DynamicDuration} containing the result of the operation.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicDuration durationUntil(@NonNull DynamicInstant to) {
            return new BetweenDuration.Builder()
                    .setStartInclusive(this)
                    .setEndExclusive(to)
                    .build();
        }

        /**
         * Returns the year field following the ISO-8601 calendar system; As an example, the
         * following is equal to {@code DynamicInt32.constant(1970)}:
         *
         * <pre>
         *   DynamicInstant.withSecondsPrecision(Instant.ofEpochSecond(8410))
         *      .getYear(ZoneId.of("Europe/London"));
         * </pre>
         */
        @RequiresSchemaVersion(major=1,minor=300)
        @NonNull
        default DynamicInt32 getYear(@NonNull ZoneId zoneId) {
            return this.atZone(zoneId).getYear();
        }

        /**
         * Returns the month-of-year field from 1 to 12 following the ISO-8601 calendar system; As
         * an example, the following is equal to {@code DynamicInt32.constant(1)}:
         *
         * <pre>
         *   DynamicInstant.withSecondsPrecision(Instant.ofEpochSecond(8410))
         *      .getMonth(ZoneId.of("Europe/London"));
         * </pre>
         */
        @RequiresSchemaVersion(major=1,minor=300)
        @NonNull
        default DynamicInt32 getMonth(@NonNull ZoneId zoneId) {
            return this.atZone(zoneId).getMonth();
        }

        /**
         * Returns the day-of-month field from 1 to 31 following the ISO-8601 calendar system; As an
         * example, the following is equal to {@code DynamicInt32.constant(1)}:
         *
         * <pre>
         *   DynamicInstant.withSecondsPrecision(Instant.ofEpochSecond(8410))
         *      .getDayOfMonth(ZoneId.of("Europe/London"));
         * </pre>
         */
        @RequiresSchemaVersion(major=1,minor=300)
        @NonNull
        default DynamicInt32 getDayOfMonth(@NonNull ZoneId zoneId) {
            return this.atZone(zoneId).getDayOfMonth();
        }

        /**
         * Returns the day-of-week field going from MONDAY (1) to SUNDAY (7) following the ISO-8601
         * calendar system; As an example, the following is equal to {@code
         * DynamicInt32.constant(4)}:
         *
         * <pre>
         *   DynamicInstant.withSecondsPrecision(Instant.ofEpochSecond(8410))
         *      .getDayOfWeek(ZoneId.of("Europe/London"));
         * </pre>
         */
        @RequiresSchemaVersion(major=1,minor=300)
        @NonNull
        default DynamicInt32 getDayOfWeek(@NonNull ZoneId zoneId) {
            return this.atZone(zoneId).getDayOfWeek();
        }

        /**
         * Returns the hour-of-day field from 0 to 23 following the ISO-8601 calendar system; As an
         * example, the following is equal to {@code DynamicInt32.constant(3)}:
         *
         * <pre>
         *   DynamicInstant.withSecondsPrecision(Instant.ofEpochSecond(8410))
         *      .getHour(ZoneId.of("Europe/London"));
         * </pre>
         */
        @RequiresSchemaVersion(major=1,minor=300)
        @NonNull
        default DynamicInt32 getHour(@NonNull ZoneId zoneId) {
            return this.atZone(zoneId).getHour();
        }

        /**
         * Returns the minute-of-hour field from 0 to 59 following the ISO-8601 calendar system; As
         * an example, the following is equal to {@code DynamicInt32.constant(20)}:
         *
         * <pre>
         *   DynamicInstant.withSecondsPrecision(Instant.ofEpochSecond(8410))
         *      .getMinute(ZoneId.of("Europe/London"));
         * </pre>
         */
        @RequiresSchemaVersion(major=1,minor=300)
        @NonNull
        default DynamicInt32 getMinute(@NonNull ZoneId zoneId) {
            return this.atZone(zoneId).getMinute();
        }

        /**
         * Returns the second-of-minute field from 0 to 59 following the ISO-8601 calendar system;
         * As an example, the following is equal to {@code DynamicInt32.constant(10)}:
         *
         * <pre>
         *   DynamicInstant.withSecondsPrecision(Instant.ofEpochSecond(8410))
         *      .getSecond(ZoneId.of("Europe/London"));
         * </pre>
         */
        @RequiresSchemaVersion(major=1,minor=300)
        @NonNull
        default DynamicInt32 getSecond(@NonNull ZoneId zoneId) {
            return this.atZone(zoneId).getSecond();
        }

        /**
         * Returns a {@link DynamicZonedDateTime} instance representing this Instant in the
         * specified time-zone. As an example, the following expression yields a {@link
         * DynamicZonedDateTime} instance representing platform time in Europe/London time-zone:
         *
         * <pre>
         *   DynamicInstant.platformTimeWithSecondsPrecision()
         *      .atZone(ZoneId.of("Europe/London"));
         * </pre>
         *
         * @return a new instance of {@link DynamicZonedDateTime} representing this {@link
         *     DynamicInstant} in the specified time-zone.
         */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 300)
        default DynamicZonedDateTime atZone(@NonNull ZoneId zoneId) {
            return new InstantToZonedDateTimeOp.Builder()
                    .setInstant(this)
                    .setZoneId(zoneId.getId())
                    .build();
        }

        /**
         * Bind the value of this {@link DynamicInstant} to the result of a conditional expression.
         * This will use the value given in either {@link ConditionScope#use} or {@link
         * ConditionScopes.IfTrueScope#elseUse} depending on the value yielded from {@code
         * condition}.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        static ConditionScope<DynamicInstant, Instant> onCondition(@NonNull DynamicBool condition) {
            return new ConditionScopes.ConditionScope<>(
                    (trueValue, falseValue) ->
                            new ConditionalInstantOp.Builder()
                                    .setCondition(condition)
                                    .setValueIfTrue(trueValue)
                                    .setValueIfFalse(falseValue)
                                    .build(),
                    DynamicInstant::withSecondsPrecision);
        }

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        Fingerprint getFingerprint();

        /** Builder to create {@link DynamicInstant} objects. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull
            DynamicInstant build();
        }
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static DynamicInstant dynamicInstantFromProto(
            @NonNull DynamicProto.DynamicInstant proto, @Nullable Fingerprint fingerprint) {
        if (proto.hasFixed()) {
            return FixedInstant.fromProto(proto.getFixed(), fingerprint);
        }
        if (proto.hasPlatformSource()) {
            return PlatformTimeSource.fromProto(proto.getPlatformSource(), fingerprint);
        }
        if (proto.hasConditionalOp()) {
            return ConditionalInstantOp.fromProto(proto.getConditionalOp(), fingerprint);
        }
        if (proto.hasStateSource()) {
            return StateInstantSource.fromProto(proto.getStateSource(), fingerprint);
        }
        throw new IllegalStateException("Proto was not a recognised instance of DynamicInstant");
    }

    /**
     * Creates a new wrapper instance from the proto. Intended for testing purposes only. An object
     * created using this method can't be added to any other wrapper.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static DynamicInstant dynamicInstantFromProto(
            @NonNull DynamicProto.DynamicInstant proto) {
        return dynamicInstantFromProto(proto, new Fingerprint(proto.getFingerprint()));
    }

    /**
     * Interface defining a dynamic zoned date-time type.
     *
     * <p>{@link DynamicZonedDateTime} precision is seconds. Thus, any related time operation will
     * operate on that precision level.
     */
    @RequiresSchemaVersion(major = 1, minor = 300)
    interface DynamicZonedDateTime extends DynamicType {
        /** Get the protocol buffer representation of this object. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        DynamicProto.DynamicZonedDateTime toDynamicZonedDateTimeProto();

        /**
         * Creates a {@link DynamicZonedDateTime} from a byte array generated by {@link
         * #toDynamicZonedDateTimeByteArray()}.
         *
         * @throws IllegalArgumentException if the byte array does not contain a valid serialization
         */
        @NonNull
        static DynamicZonedDateTime fromByteArray(@NonNull byte[] byteArray) {
            return fromByteArray(byteArray, 0, byteArray.length);
        }

        /**
         * Creates a {@link DynamicZonedDateTime} from the provided byte array at the provided
         * offset and length, that was generated by one of the {@link
         * #toDynamicZonedDateTimeByteArray} overloads.
         *
         * @throws IllegalArgumentException if the byte array does not contain a valid serialization
         *     in the provided offset and length
         */
        @NonNull
        static DynamicZonedDateTime fromByteArray(
                @NonNull byte[] byteArray, int offset, int length) {
            try {
                return dynamicZonedDateTimeFromProto(
                        DynamicProto.DynamicZonedDateTime.parseFrom(
                                CodedInputStream.newInstance(byteArray, offset, length),
                                ExtensionRegistryLite.getEmptyRegistry()));
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        "Byte array could not be parsed into DynamicZonedDateTime", e);
            }
        }

        /**
         * Serializes the {@link DynamicZonedDateTime} into a new byte array that can later be used
         * with {@link #fromByteArray(byte[])}.
         */
        @NonNull
        default byte[] toDynamicZonedDateTimeByteArray() {
            return toDynamicZonedDateTimeProto().toByteArray();
        }

        /**
         * Serializes the {@link DynamicZonedDateTime} into the provided byte array, returning the
         * amount of bytes written, that can later be used with {@code
         * DynamicZonedDateTime.fromByteArray(byteArray, 0, bytesWritten)}.
         *
         * @throws IllegalArgumentException if the byte array is too small
         */
        default int toDynamicZonedDateTimeByteArray(@NonNull byte[] byteArray) {
            return toDynamicZonedDateTimeByteArray(byteArray, 0, byteArray.length);
        }

        /**
         * Serializes the {@link DynamicZonedDateTime} into the provided byte array, returning the
         * amount of bytes written, limited by the provided offset and length, that can later be
         * used with {@code DynamicZonedDateTime.fromByteArray(byteArray, offset, bytesWritten)}.
         *
         * @throws IllegalArgumentException if the byte array is too small
         */
        default int toDynamicZonedDateTimeByteArray(
                @NonNull byte[] byteArray, int offset, int length) {
            CodedOutputStream stream = CodedOutputStream.newInstance(byteArray, offset, length);
            try {
                toDynamicZonedDateTimeProto().writeTo(stream);
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        "Provided byte array not large enough to contain this DynamicZonedDateTime",
                        e);
            }
            return stream.getTotalBytesWritten();
        }

        /**
         * Returns the year field following the ISO-8601 calendar system; As an example, the
         * following is equal to {@code DynamicInt32.constant(1970)}:
         *
         * <pre>
         *   DynamicInstant.withSecondsPrecision(Instant.ofEpochSecond(8410))
         *      .atZone(ZoneId.of("Europe/London"))
         *      .getYear();
         * </pre>
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 300)
        default DynamicInt32 getYear() {
            return new GetZonedDateTimePartOp.Builder()
                    .setInput(this)
                    .setPartType(ZONED_DATE_TIME_PART_YEAR)
                    .build();
        }

        /**
         * Returns the month-of-year field from 1 to 12 following the ISO-8601 calendar system; As
         * an example, the following is equal to {@code DynamicInt32.constant(1)}:
         *
         * <pre>
         *   DynamicInstant.withSecondsPrecision(Instant.ofEpochSecond(8410))
         *      .atZone(ZoneId.of("Europe/London"))
         *      .getMonth();
         * </pre>
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 300)
        default DynamicInt32 getMonth() {
            return new GetZonedDateTimePartOp.Builder()
                    .setInput(this)
                    .setPartType(ZONED_DATE_TIME_PART_MONTH)
                    .build();
        }

        /**
         * Returns the day-of-month field from 1 to 31 following the ISO-8601 calendar system; As an
         * example, the following is equal to {@code DynamicInt32.constant(1)}:
         *
         * <pre>
         *   DynamicInstant.withSecondsPrecision(Instant.ofEpochSecond(8410))
         *      .atZone(ZoneId.of("Europe/London"))
         *      .getDayOfMonth();
         * </pre>
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 300)
        default DynamicInt32 getDayOfMonth() {
            return new GetZonedDateTimePartOp.Builder()
                    .setInput(this)
                    .setPartType(ZONED_DATE_TIME_PART_DAY_OF_MONTH)
                    .build();
        }

        /**
         * Returns the day-of-week field going from MONDAY (1) to SUNDAY (7) following the ISO-8601
         * calendar system; As an example, the following is equal to {@code
         * DynamicInt32.constant(4)}:
         *
         * <pre>
         *   DynamicInstant.withSecondsPrecision(Instant.ofEpochSecond(8410))
         *      .atZone(ZoneId.of("Europe/London"))
         *      .getDayOfWeek();
         * </pre>
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 300)
        default DynamicInt32 getDayOfWeek() {
            return new GetZonedDateTimePartOp.Builder()
                    .setInput(this)
                    .setPartType(ZONED_DATE_TIME_PART_DAY_OF_WEEK)
                    .build();
        }

        /**
         * Returns the hour-of-day field from 0 to 23 following the ISO-8601 calendar system; As an
         * example, the following is equal to {@code DynamicInt32.constant(3)}:
         *
         * <pre>
         *   DynamicInstant.withSecondsPrecision(Instant.ofEpochSecond(8410))
         *      .atZone(ZoneId.of("Europe/London"))
         *      .getHour();
         * </pre>
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 300)
        default DynamicInt32 getHour() {
            return new GetZonedDateTimePartOp.Builder()
                    .setInput(this)
                    .setPartType(ZONED_DATE_TIME_PART_HOUR_24H)
                    .build();
        }

        /**
         * Returns the minute-of-hour field from 0 to 59 following the ISO-8601 calendar system; As
         * an example, the following is equal to {@code DynamicInt32.constant(20)}:
         *
         * <pre>
         *   DynamicInstant.withSecondsPrecision(Instant.ofEpochSecond(8410))
         *      .atZone(ZoneId.of("Europe/London"))
         *      .getMinute();
         * </pre>
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 300)
        default DynamicInt32 getMinute() {
            return new GetZonedDateTimePartOp.Builder()
                    .setInput(this)
                    .setPartType(ZONED_DATE_TIME_PART_MINUTE)
                    .build();
        }

        /**
         * Returns the second-of-minute field from 0 to 59 following the ISO-8601 calendar system;
         * As an example, the following is equal to {@code DynamicInt32.constant(10)}:
         *
         * <pre>
         *   DynamicInstant.withSecondsPrecision(Instant.ofEpochSecond(8410))
         *      .atZone(ZoneId.of("Europe/London"))
         *      .getSecond();
         * </pre>
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 300)
        default DynamicInt32 getSecond() {
            return new GetZonedDateTimePartOp.Builder()
                    .setInput(this)
                    .setPartType(ZONED_DATE_TIME_PART_SECOND)
                    .build();
        }

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        Fingerprint getFingerprint();

        /** Builder to create {@link DynamicZonedDateTime} objects. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull
            DynamicZonedDateTime build();
        }
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static DynamicZonedDateTime dynamicZonedDateTimeFromProto(
            @NonNull DynamicProto.DynamicZonedDateTime proto, @Nullable Fingerprint fingerprint) {
        if (proto.hasInstantToZonedDateTime()) {
            return InstantToZonedDateTimeOp.fromProto(
                    proto.getInstantToZonedDateTime(), fingerprint);
        }
        throw new IllegalStateException(
                "Proto was not a recognised instance of DynamicZonedDateTime");
    }

    /**
     * Creates a new wrapper instance from the proto. Intended for testing purposes only. An object
     * created using this method can't be added to any other wrapper.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static DynamicZonedDateTime dynamicZonedDateTimeFromProto(
            @NonNull DynamicProto.DynamicZonedDateTime proto) {
        return dynamicZonedDateTimeFromProto(proto, null);
    }

    /** Converts a {@link DynamicInstant} into a {@link DynamicZonedDateTime}. */
    @RequiresSchemaVersion(major = 1, minor = 300)
    static final class InstantToZonedDateTimeOp implements DynamicZonedDateTime {
        private final DynamicProto.InstantToZonedDateTimeOp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        InstantToZonedDateTimeOp(
                DynamicProto.InstantToZonedDateTimeOp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the instant to convert. */
        @Nullable
        public DynamicInstant getInstant() {
            if (mImpl.hasInstant()) {
                return DynamicBuilders.dynamicInstantFromProto(mImpl.getInstant());
            } else {
                return null;
            }
        }

        /** Gets the ZoneId following the time-zone ID format used by java {@link ZoneId}. */
        @NonNull
        public String getZoneId() {
            return mImpl.getZoneId();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static InstantToZonedDateTimeOp fromProto(
                @NonNull DynamicProto.InstantToZonedDateTimeOp proto,
                @Nullable Fingerprint fingerprint) {
            return new InstantToZonedDateTimeOp(proto, fingerprint);
        }

        @NonNull
        static InstantToZonedDateTimeOp fromProto(
                @NonNull DynamicProto.InstantToZonedDateTimeOp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.InstantToZonedDateTimeOp toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicZonedDateTime toDynamicZonedDateTimeProto() {
            return DynamicProto.DynamicZonedDateTime.newBuilder()
                    .setInstantToZonedDateTime(mImpl)
                    .build();
        }

        @Override
        @NonNull
        public String toString() {
            return "InstantToZonedDateTimeOp{"
                    + "instant="
                    + getInstant()
                    + ", zoneId="
                    + getZoneId()
                    + "}";
        }

        /** Builder for {@link InstantToZonedDateTimeOp}. */
        public static final class Builder implements DynamicZonedDateTime.Builder {
            private final DynamicProto.InstantToZonedDateTimeOp.Builder mImpl =
                    DynamicProto.InstantToZonedDateTimeOp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1382070867);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Sets the instant to convert. */
            @RequiresSchemaVersion(major = 1, minor = 300)
            @NonNull
            public Builder setInstant(@NonNull DynamicInstant instant) {
                mImpl.setInstant(instant.toDynamicInstantProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(instant.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the ZoneId following the time-zone ID format used by java {@link ZoneId}. */
            @RequiresSchemaVersion(major = 1, minor = 300)
            @NonNull
            public Builder setZoneId(@NonNull String zoneId) {
                mImpl.setZoneId(zoneId);
                mFingerprint.recordPropertyUpdate(2, zoneId.hashCode());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @Override
            @NonNull
            public InstantToZonedDateTimeOp build() {
                return new InstantToZonedDateTimeOp(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * Retrieve the specified date-time part of a {@link DynamicZonedDateTime} instance as a {@link
     * DynamicInt32}.
     */
    @RequiresSchemaVersion(major = 1, minor = 300)
    static final class GetZonedDateTimePartOp implements DynamicInt32 {
        private final DynamicProto.GetZonedDateTimePartOp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        GetZonedDateTimePartOp(
                DynamicProto.GetZonedDateTimePartOp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the zoned date-time input. */
        @Nullable
        public DynamicZonedDateTime getInput() {
            if (mImpl.hasInput()) {
                return DynamicBuilders.dynamicZonedDateTimeFromProto(mImpl.getInput());
            } else {
                return null;
            }
        }

        /** Gets the date-time part to retrieve. */
        @ZonedDateTimePartType
        public int getPartType() {
            return mImpl.getPartType().getNumber();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static GetZonedDateTimePartOp fromProto(
                @NonNull DynamicProto.GetZonedDateTimePartOp proto,
                @Nullable Fingerprint fingerprint) {
            return new GetZonedDateTimePartOp(proto, fingerprint);
        }

        @NonNull
        static GetZonedDateTimePartOp fromProto(
                @NonNull DynamicProto.GetZonedDateTimePartOp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.GetZonedDateTimePartOp toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicInt32 toDynamicInt32Proto() {
            return DynamicProto.DynamicInt32.newBuilder().setZonedDateTimePart(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicInt32 toDynamicInt32Proto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicInt32.newBuilder()
                        .setZonedDateTimePart(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }

            return toDynamicInt32Proto();
        }

        @Override
        @NonNull
        public String toString() {
            return "GetZonedDateTimePartOp{"
                    + "input="
                    + getInput()
                    + ", partType="
                    + getPartType()
                    + "}";
        }

        /** Builder for {@link GetZonedDateTimePartOp}. */
        public static final class Builder implements DynamicInt32.Builder {
            private final DynamicProto.GetZonedDateTimePartOp.Builder mImpl =
                    DynamicProto.GetZonedDateTimePartOp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1073599249);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Sets the zoned date-time input. */
            @RequiresSchemaVersion(major = 1, minor = 300)
            @NonNull
            public Builder setInput(@NonNull DynamicZonedDateTime input) {
                mImpl.setInput(input.toDynamicZonedDateTimeProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(input.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the date-time part to retrieve. */
            @RequiresSchemaVersion(major = 1, minor = 300)
            @NonNull
            public Builder setPartType(@ZonedDateTimePartType int partType) {
                mImpl.setPartType(DynamicProto.ZonedDateTimePartType.forNumber(partType));
                mFingerprint.recordPropertyUpdate(2, partType);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @Override
            @NonNull
            public GetZonedDateTimePartOp build() {
                return new GetZonedDateTimePartOp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A dynamic duration type that represents the duration between two dynamic time instants. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class BetweenDuration implements DynamicDuration {
        private final DynamicProto.BetweenDuration mImpl;
        @Nullable private final Fingerprint mFingerprint;

        BetweenDuration(DynamicProto.BetweenDuration impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the time instant value marking the start of the duration. */
        @Nullable
        public DynamicInstant getStartInclusive() {
            if (mImpl.hasStartInclusive()) {
                return DynamicBuilders.dynamicInstantFromProto(mImpl.getStartInclusive());
            } else {
                return null;
            }
        }

        /** Gets the time instant value marking the end of the duration. */
        @Nullable
        public DynamicInstant getEndExclusive() {
            if (mImpl.hasEndExclusive()) {
                return DynamicBuilders.dynamicInstantFromProto(mImpl.getEndExclusive());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static BetweenDuration fromProto(
                @NonNull DynamicProto.BetweenDuration proto, @Nullable Fingerprint fingerprint) {
            return new BetweenDuration(proto, fingerprint);
        }

        @NonNull
        static BetweenDuration fromProto(@NonNull DynamicProto.BetweenDuration proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.BetweenDuration toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicDuration toDynamicDurationProto() {
            return DynamicProto.DynamicDuration.newBuilder().setBetween(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicDuration toDynamicDurationProto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicDuration.newBuilder()
                        .setBetween(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicDurationProto();
        }

        @Override
        @NonNull
        public String toString() {
            return "BetweenDuration{"
                    + "startInclusive="
                    + getStartInclusive()
                    + ", endExclusive="
                    + getEndExclusive()
                    + "}";
        }

        /** Builder for {@link BetweenDuration}. */
        public static final class Builder implements DynamicDuration.Builder {
            private final DynamicProto.BetweenDuration.Builder mImpl =
                    DynamicProto.BetweenDuration.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1615230958);

            public Builder() {}

            /** Sets the time instant value marking the start of the duration. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setStartInclusive(@NonNull DynamicInstant startInclusive) {
                mImpl.setStartInclusive(startInclusive.toDynamicInstantProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(startInclusive.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the time instant value marking the end of the duration. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setEndExclusive(@NonNull DynamicInstant endExclusive) {
                mImpl.setEndExclusive(endExclusive.toDynamicInstantProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(endExclusive.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public BetweenDuration build() {
                return new BetweenDuration(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A conditional operator which yields a duration depending on the boolean operand. This
     * implements:
     *
     * <pre>{@code
     * duration result = condition ? value_if_true : value_if_false
     * }</pre>
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class ConditionalDurationOp implements DynamicDuration {
        private final DynamicProto.ConditionalDurationOp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        ConditionalDurationOp(
                DynamicProto.ConditionalDurationOp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the condition to use. */
        @Nullable
        public DynamicBool getCondition() {
            if (mImpl.hasCondition()) {
                return DynamicBuilders.dynamicBoolFromProto(mImpl.getCondition());
            } else {
                return null;
            }
        }

        /** Gets the duration to yield if condition is true. */
        @Nullable
        public DynamicDuration getValueIfTrue() {
            if (mImpl.hasValueIfTrue()) {
                return DynamicBuilders.dynamicDurationFromProto(mImpl.getValueIfTrue());
            } else {
                return null;
            }
        }

        /** Gets the duration to yield if condition is false. */
        @Nullable
        public DynamicDuration getValueIfFalse() {
            if (mImpl.hasValueIfFalse()) {
                return DynamicBuilders.dynamicDurationFromProto(mImpl.getValueIfFalse());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static ConditionalDurationOp fromProto(
                @NonNull DynamicProto.ConditionalDurationOp proto,
                @Nullable Fingerprint fingerprint) {
            return new ConditionalDurationOp(proto, fingerprint);
        }

        @NonNull
        static ConditionalDurationOp fromProto(@NonNull DynamicProto.ConditionalDurationOp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.ConditionalDurationOp toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicDuration toDynamicDurationProto() {
            return DynamicProto.DynamicDuration.newBuilder().setConditionalOp(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicDuration toDynamicDurationProto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicDuration.newBuilder()
                        .setConditionalOp(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicDurationProto();
        }

        @Override
        @NonNull
        public String toString() {
            return "ConditionalDurationOp{"
                    + "condition="
                    + getCondition()
                    + ", valueIfTrue="
                    + getValueIfTrue()
                    + ", valueIfFalse="
                    + getValueIfFalse()
                    + "}";
        }

        /** Builder for {@link ConditionalDurationOp}. */
        public static final class Builder implements DynamicDuration.Builder {
            private final DynamicProto.ConditionalDurationOp.Builder mImpl =
                    DynamicProto.ConditionalDurationOp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(905401559);

            public Builder() {}

            /** Sets the condition to use. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setCondition(@NonNull DynamicBool condition) {
                mImpl.setCondition(condition.toDynamicBoolProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(condition.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the duration to yield if condition is true. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setValueIfTrue(@NonNull DynamicDuration valueIfTrue) {
                mImpl.setValueIfTrue(valueIfTrue.toDynamicDurationProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(valueIfTrue.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the duration to yield if condition is false. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setValueIfFalse(@NonNull DynamicDuration valueIfFalse) {
                mImpl.setValueIfFalse(valueIfFalse.toDynamicDurationProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(valueIfFalse.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public ConditionalDurationOp build() {
                return new ConditionalDurationOp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** Interface defining a dynamic duration type. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public interface DynamicDuration extends DynamicType {
        /** Get the protocol buffer representation of this object, without fingerprint. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        DynamicProto.DynamicDuration toDynamicDurationProto();

        /** Get the protocol buffer representation of this object, with or without fingerprint. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        DynamicProto.DynamicDuration toDynamicDurationProto(boolean withFingerprint);


        /**
         * Creates a {@link DynamicDuration} from a byte array generated by {@link
         * #toDynamicDurationByteArray()}.
         *
         * @throws IllegalArgumentException if the byte array does not contain a valid serialization
         */
        @NonNull
        static DynamicDuration fromByteArray(@NonNull byte[] byteArray) {
            return fromByteArray(byteArray, 0, byteArray.length);
        }

        /**
         * Creates a {@link DynamicDuration} from the provided byte array at the provided offset and
         * length, that was generated by one of the {@link #toDynamicDurationByteArray} overloads.
         *
         * @throws IllegalArgumentException if the byte array does not contain a valid serialization
         *     in the provided offset and length
         */
        @NonNull
        static DynamicDuration fromByteArray(@NonNull byte[] byteArray, int offset, int length) {
            try {
                return dynamicDurationFromProto(
                        DynamicProto.DynamicDuration.parseFrom(
                                CodedInputStream.newInstance(byteArray, offset, length),
                                ExtensionRegistryLite.getEmptyRegistry()));
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        "Byte array could not be parsed into DynamicDuration", e);
            }
        }

        /**
         * Serializes the {@link DynamicDuration} into a new byte array that can later be used with
         * {@link #fromByteArray(byte[])}.
         */
        @NonNull
        default byte[] toDynamicDurationByteArray() {
            return toDynamicDurationProto(/* withFingerprint= */ true).toByteArray();
        }

        /**
         * Serializes the {@link DynamicDuration} into the provided byte array, returning the amount
         * of bytes written, that can later be used with {@code DynamicDuration.fromByteArray(
         * byteArray, 0, bytesWritten)}.
         *
         * @throws IllegalArgumentException if the byte array is too small
         */
        default int toDynamicDurationByteArray(@NonNull byte[] byteArray) {
            return toDynamicDurationByteArray(byteArray, 0, byteArray.length);
        }

        /**
         * Serializes the {@link DynamicDuration} into the provided byte array, returning the amount
         * of bytes written, limited by the provided offset and length, that can later be used with
         * {@code DynamicDuration.fromByteArray(byteArray, offset, bytesWritten)}.
         *
         * @throws IllegalArgumentException if the byte array is too small
         */
        default int toDynamicDurationByteArray(@NonNull byte[] byteArray, int offset, int length) {
            CodedOutputStream stream = CodedOutputStream.newInstance(byteArray, offset, length);
            try {
                toDynamicDurationProto(/* withFingerprint= */ true).writeTo(stream);
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        "Provided byte array not large enough to contain this DynamicDuration", e);
            }
            return stream.getTotalBytesWritten();
        }

        /**
         * Creates a {@link DynamicDuration} that is bound to the value of an item of the State.
         *
         * @param dynamicDataKey The source key to a {@link DynamicDataValue} with an {@link
         *     Duration} value.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 300)
        static DynamicDuration from(@NonNull DynamicDataKey<DynamicDuration> dynamicDataKey) {
            return new StateDurationSource.Builder()
                    .setSourceKey(dynamicDataKey.getKey())
                    .setSourceNamespace(dynamicDataKey.getNamespace())
                    .build();
        }

        /**
         * Creates a constant-valued {@link DynamicDuration} from a {@link Duration}. If {@link
         * Duration} precision is greater than seconds, then any excess precision information will
         * be dropped.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        static DynamicDuration withSecondsPrecision(@NonNull Duration duration) {
            return new FixedDuration.Builder().setSeconds(duration.getSeconds()).build();
        }

        /**
         * Returns the total number of days in a {@link DynamicDuration} as a {@link DynamicInt32}.
         * The fraction part of the result will be truncated. This is based on the standard
         * definition of a day as 24 hours. As an example, the following is equal to {@code
         * DynamicInt32.constant(1)}
         *
         * <pre>
         *   DynamicInstant.withSecondsPrecision(Instant.EPOCH)
         *      .durationUntil(DynamicInstant.withSecondsPrecision(Instant.ofEpochSecond(123456L)))
         *      .toIntDays();
         * </pre>
         *
         * @return a new instance of {@link DynamicInt32} containing the result of the operation.
         *     Integer overflow can occur if the result of the operation is larger than {@link
         *     Integer#MAX_VALUE}.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicInt32 toIntDays() {
            return new GetDurationPartOp.Builder()
                    .setInput(this)
                    .setDurationPart(DURATION_PART_TYPE_TOTAL_DAYS)
                    .build();
        }

        /**
         * Returns the total number of hours in a {@link DynamicDuration} as a {@link DynamicInt32}.
         * The fraction part of the result will be truncated. As an example, the following is equal
         * to {@code DynamicInt32.constant(34)}
         *
         * <pre>
         *   DynamicInstant.withSecondsPrecision(Instant.EPOCH)
         *      .durationUntil(DynamicInstant.withSecondsPrecision(Instant.ofEpochSecond(123456L)))
         *      .toIntHours();
         * </pre>
         *
         * @return a new instance of {@link DynamicInt32} containing the result of the operation.
         *     Integer overflow can occur if the result of the operation is larger than {@link
         *     Integer#MAX_VALUE}.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicInt32 toIntHours() {
            return new GetDurationPartOp.Builder()
                    .setInput(this)
                    .setDurationPart(DURATION_PART_TYPE_TOTAL_HOURS)
                    .build();
        }

        /**
         * Returns the total number of minutes in a {@link DynamicDuration} as a {@link
         * DynamicInt32}. The fraction part of the result will be truncated. As an example, the
         * following is equal to {@code DynamicInt32.constant(2057)}
         *
         * <pre>
         *   DynamicInstant.withSecondsPrecision(Instant.EPOCH)
         *      .durationUntil(DynamicInstant.withSecondsPrecision(Instant.ofEpochSecond(123456L)))
         *      .toIntMinutes();
         * </pre>
         *
         * @return a new instance of {@link DynamicInt32} containing the result of the operation.
         *     Integer overflow can occur if the result of the operation is larger than {@link
         *     Integer#MAX_VALUE}.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicInt32 toIntMinutes() {
            return new GetDurationPartOp.Builder()
                    .setInput(this)
                    .setDurationPart(DURATION_PART_TYPE_TOTAL_MINUTES)
                    .build();
        }

        /**
         * Returns the total number of seconds in a {@link DynamicDuration} as a {@link
         * DynamicInt32}. As an example, the following is equal to {@code
         * DynamicInt32.constant(123456)}
         *
         * <pre>
         *   DynamicInstant.withSecondsPrecision(Instant.EPOCH)
         *      .durationUntil(DynamicInstant.withSecondsPrecision(Instant.ofEpochSecond(123456L)))
         *      .toIntSeconds();
         * </pre>
         *
         * @return a new instance of {@link DynamicInt32} containing the result of the operation.
         *     Integer overflow can occur if the result of the operation is larger than {@link
         *     Integer#MAX_VALUE}.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicInt32 toIntSeconds() {
            return new GetDurationPartOp.Builder()
                    .setInput(this)
                    .setDurationPart(DURATION_PART_TYPE_TOTAL_SECONDS)
                    .build();
        }

        /**
         * Returns the total number of days in a duration as a {@link DynamicInt32}. This represents
         * the absolute value of the total number of days in the duration based on the 24 hours day
         * definition. The fraction part of the result will be truncated; As an example, the
         * following is equal to {@code DynamicInt32.constant(1)}
         *
         * <pre>
         *   DynamicInstant.withSecondsPrecision(Instant.EPOCH)
         *      .durationUntil(DynamicInstant.withSecondsPrecision(Instant.ofEpochSecond(123456L)))
         *      .getIntDaysPart();
         * </pre>
         *
         * @return a new instance of {@link DynamicInt32} containing the result of the operation.
         *     Integer overflow can occur if the result of the operation is larger than {@link
         *     Integer#MAX_VALUE}.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicInt32 getIntDaysPart() {
            return new GetDurationPartOp.Builder()
                    .setInput(this)
                    .setDurationPart(DURATION_PART_TYPE_DAYS)
                    .build();
        }

        /**
         * Returns the number of hours part in the duration as a {@link DynamicInt32}. This
         * represents the absolute value of remaining hours when dividing total hours by hours in a
         * day (24 hours); As an example, the following is equal to {@code
         * DynamicInt32.constant(10)}
         *
         * <pre>
         *   DynamicInstant.withSecondsPrecision(Instant.EPOCH)
         *      .durationUntil(DynamicInstant.withSecondsPrecision(Instant.ofEpochSecond(123456L)))
         *      .getHoursPart();
         * </pre>
         *
         * @return a new instance of {@link DynamicInt32} containing the result of the operation.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicInt32 getHoursPart() {
            return new GetDurationPartOp.Builder()
                    .setInput(this)
                    .setDurationPart(DURATION_PART_TYPE_HOURS)
                    .build();
        }

        /**
         * Returns the number of minutes part in the duration as a {@link DynamicInt32}. This
         * represents the absolute value of remaining minutes when dividing total minutes by minutes
         * in an hour (60 minutes). As an example, the following is equal to {@code
         * DynamicInt32.constant(17)}
         *
         * <pre>
         *   DynamicInstant.withSecondsPrecision(Instant.EPOCH)
         *      .durationUntil(DynamicInstant.withSecondsPrecision(Instant.ofEpochSecond(123456L)))
         *      .getMinutesPart();
         * </pre>
         *
         * @return a new instance of {@link DynamicInt32} containing the result of the operation.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicInt32 getMinutesPart() {
            return new GetDurationPartOp.Builder()
                    .setInput(this)
                    .setDurationPart(DURATION_PART_TYPE_MINUTES)
                    .build();
        }

        /**
         * Returns the number of seconds part in the duration as a {@link DynamicInt32}. This
         * represents the absolute value of remaining seconds when dividing total seconds by seconds
         * in a minute (60 seconds); As an example, the following is equal to {@code
         * DynamicInt32.constant(36)}
         *
         * <pre>
         *   DynamicInstant.withSecondsPrecision(Instant.EPOCH)
         *      .durationUntil(DynamicInstant.withSecondsPrecision(Instant.ofEpochSecond(123456L)))
         *      .getSecondsPart();
         * </pre>
         *
         * @return a new instance of {@link DynamicInt32} containing the result of the operation.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        default DynamicInt32 getSecondsPart() {
            return new GetDurationPartOp.Builder()
                    .setInput(this)
                    .setDurationPart(DURATION_PART_TYPE_SECONDS)
                    .build();
        }

        /**
         * Bind the value of this {@link DynamicDuration} to the result of a conditional expression.
         * This will use the value given in either {@link ConditionScope#use} or {@link
         * ConditionScopes.IfTrueScope#elseUse} depending on the value yielded from {@code
         * condition}.
         */
        @NonNull
        @RequiresSchemaVersion(major = 1, minor = 200)
        static ConditionScope<DynamicDuration, Duration> onCondition(
                @NonNull DynamicBool condition) {
            return new ConditionScopes.ConditionScope<>(
                    (trueValue, falseValue) ->
                            new ConditionalDurationOp.Builder()
                                    .setCondition(condition)
                                    .setValueIfTrue(trueValue)
                                    .setValueIfFalse(falseValue)
                                    .build(),
                    DynamicDuration::withSecondsPrecision);
        }

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        Fingerprint getFingerprint();

        /** Builder to create {@link DynamicDuration} objects. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull
            DynamicDuration build();
        }
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static DynamicDuration dynamicDurationFromProto(
            @NonNull DynamicProto.DynamicDuration proto, @Nullable Fingerprint fingerprint) {
        if (proto.hasBetween()) {
            return BetweenDuration.fromProto(proto.getBetween(), fingerprint);
        }
        if (proto.hasFixed()) {
            return FixedDuration.fromProto(proto.getFixed(), fingerprint);
        }
        if (proto.hasConditionalOp()) {
            return ConditionalDurationOp.fromProto(proto.getConditionalOp(), fingerprint);
        }
        if (proto.hasStateSource()) {
            return StateDurationSource.fromProto(proto.getStateSource(), fingerprint);
        }
        throw new IllegalStateException("Proto was not a recognised instance of DynamicDuration");
    }

    /**
     * Creates a new wrapper instance from the proto. Intended for testing purposes only. An object
     * created using this method can't be added to any other wrapper.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static DynamicDuration dynamicDurationFromProto(
            @NonNull DynamicProto.DynamicDuration proto) {
        return dynamicDurationFromProto(proto, new Fingerprint(proto.getFingerprint()));
    }

    /**
     * Retrieve the specified duration part of a {@link DynamicDuration} instance as a {@link
     * DynamicInt32}.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class GetDurationPartOp implements DynamicInt32 {
        private final DynamicProto.GetDurationPartOp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        GetDurationPartOp(DynamicProto.GetDurationPartOp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the duration input. */
        @Nullable
        public DynamicDuration getInput() {
            if (mImpl.hasInput()) {
                return DynamicBuilders.dynamicDurationFromProto(mImpl.getInput());
            } else {
                return null;
            }
        }

        /** Gets the duration part to retrieve. */
        @DurationPartType
        public int getDurationPart() {
            return mImpl.getDurationPart().getNumber();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static GetDurationPartOp fromProto(
                @NonNull DynamicProto.GetDurationPartOp proto, @Nullable Fingerprint fingerprint) {
            return new GetDurationPartOp(proto, fingerprint);
        }

        @NonNull
        static GetDurationPartOp fromProto(@NonNull DynamicProto.GetDurationPartOp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.GetDurationPartOp toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicInt32 toDynamicInt32Proto() {
            return DynamicProto.DynamicInt32.newBuilder().setDurationPart(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicInt32 toDynamicInt32Proto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicInt32.newBuilder()
                        .setDurationPart(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicInt32Proto();
        }

        @Override
        @NonNull
        public String toString() {
            return "GetDurationPartOp{"
                    + "input="
                    + getInput()
                    + ", durationPart="
                    + getDurationPart()
                    + "}";
        }

        /** Builder for {@link GetDurationPartOp}. */
        public static final class Builder implements DynamicInt32.Builder {
            private final DynamicProto.GetDurationPartOp.Builder mImpl =
                    DynamicProto.GetDurationPartOp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-225941123);

            public Builder() {}

            /** Sets the duration input. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setInput(@NonNull DynamicDuration input) {
                mImpl.setInput(input.toDynamicDurationProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(input.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the duration part to retrieve. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setDurationPart(@DurationPartType int durationPart) {
                mImpl.setDurationPart(DynamicProto.DurationPartType.forNumber(durationPart));
                mFingerprint.recordPropertyUpdate(2, durationPart);
                return this;
            }

            @Override
            @NonNull
            public GetDurationPartOp build() {
                return new GetDurationPartOp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A dynamic Instant which sources its data from the a state entry. */
    @RequiresSchemaVersion(major = 1, minor = 300)
    static final class StateInstantSource implements DynamicInstant {
        private final DynamicProto.StateInstantSource mImpl;
        @Nullable private final Fingerprint mFingerprint;

        StateInstantSource(
                DynamicProto.StateInstantSource impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the key in the state to bind to. */
        @NonNull
        public String getSourceKey() {
            return mImpl.getSourceKey();
        }

        /** Gets the namespace for the state key. */
        @NonNull
        public String getSourceNamespace() {
            return mImpl.getSourceNamespace();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static StateInstantSource fromProto(
                @NonNull DynamicProto.StateInstantSource proto, @Nullable Fingerprint fingerprint) {
            return new StateInstantSource(proto, fingerprint);
        }

        @NonNull
        static StateInstantSource fromProto(@NonNull DynamicProto.StateInstantSource proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.StateInstantSource toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicInstant toDynamicInstantProto() {
            return DynamicProto.DynamicInstant.newBuilder().setStateSource(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicInstant toDynamicInstantProto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicInstant.newBuilder()
                        .setStateSource(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicInstantProto();
        }

        @Override
        @NonNull
        public String toString() {
            return "StateInstantSource{"
                    + "sourceKey="
                    + getSourceKey()
                    + ", sourceNamespace="
                    + getSourceNamespace()
                    + "}";
        }

        /** Builder for {@link StateInstantSource}. */
        public static final class Builder implements DynamicInstant.Builder {
            private final DynamicProto.StateInstantSource.Builder mImpl =
                    DynamicProto.StateInstantSource.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-694732886);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Sets the key in the state to bind to. */
            @RequiresSchemaVersion(major = 1, minor = 300)
            @NonNull
            public Builder setSourceKey(@NonNull String sourceKey) {
                mImpl.setSourceKey(sourceKey);
                mFingerprint.recordPropertyUpdate(1, sourceKey.hashCode());
                return this;
            }

            /** Sets the namespace for the state key. */
            @RequiresSchemaVersion(major = 1, minor = 300)
            @NonNull
            public Builder setSourceNamespace(@NonNull String sourceNamespace) {
                mImpl.setSourceNamespace(sourceNamespace);
                mFingerprint.recordPropertyUpdate(2, sourceNamespace.hashCode());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @Override
            @NonNull
            public StateInstantSource build() {
                return new StateInstantSource(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A dynamic Duration which sources its data from the a state entry. */
    @RequiresSchemaVersion(major = 1, minor = 300)
    static final class StateDurationSource implements DynamicDuration {
        private final DynamicProto.StateDurationSource mImpl;
        @Nullable private final Fingerprint mFingerprint;

        StateDurationSource(
                DynamicProto.StateDurationSource impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the key in the state to bind to. */
        @NonNull
        public String getSourceKey() {
            return mImpl.getSourceKey();
        }

        /** Gets the namespace for the state key. */
        @NonNull
        public String getSourceNamespace() {
            return mImpl.getSourceNamespace();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static StateDurationSource fromProto(
                @NonNull DynamicProto.StateDurationSource proto,
                @Nullable Fingerprint fingerprint) {
            return new StateDurationSource(proto, fingerprint);
        }

        @NonNull
        static StateDurationSource fromProto(@NonNull DynamicProto.StateDurationSource proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        DynamicProto.StateDurationSource toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicDuration toDynamicDurationProto() {
            return DynamicProto.DynamicDuration.newBuilder().setStateSource(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DynamicProto.DynamicDuration toDynamicDurationProto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicDuration.newBuilder()
                        .setStateSource(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicDurationProto();
        }

        @Override
        @NonNull
        public String toString() {
            return "StateDurationSource{"
                    + "sourceKey="
                    + getSourceKey()
                    + ", sourceNamespace="
                    + getSourceNamespace()
                    + "}";
        }

        /** Builder for {@link StateDurationSource}. */
        public static final class Builder implements DynamicDuration.Builder {
            private final DynamicProto.StateDurationSource.Builder mImpl =
                    DynamicProto.StateDurationSource.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1860268194);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Sets the key in the state to bind to. */
            @RequiresSchemaVersion(major = 1, minor = 300)
            @NonNull
            public Builder setSourceKey(@NonNull String sourceKey) {
                mImpl.setSourceKey(sourceKey);
                mFingerprint.recordPropertyUpdate(1, sourceKey.hashCode());
                return this;
            }

            /** Sets the namespace for the state key. */
            @RequiresSchemaVersion(major = 1, minor = 300)
            @NonNull
            public Builder setSourceNamespace(@NonNull String sourceNamespace) {
                mImpl.setSourceNamespace(sourceNamespace);
                mFingerprint.recordPropertyUpdate(2, sourceNamespace.hashCode());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @Override
            @NonNull
            public StateDurationSource build() {
                return new StateDurationSource(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * Interface to be used as a base type for all other dynamic types. This is not consumed by any
     * Tile elements, it exists just as a marker interface for use internally in the Tiles library.
     */
    public interface DynamicType {}
}
