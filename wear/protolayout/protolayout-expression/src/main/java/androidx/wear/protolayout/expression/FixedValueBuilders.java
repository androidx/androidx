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
import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.expression.proto.DynamicDataProto;
import androidx.wear.protolayout.expression.proto.DynamicProto;
import androidx.wear.protolayout.expression.proto.FixedProto;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;

/**
 * Builders for fixed value primitive types that can be used in dynamic expressions and in for state
 * state values.
 */
final class FixedValueBuilders {
    private FixedValueBuilders() {}

    /** A fixed int32 type. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class FixedInt32
            implements DynamicBuilders.DynamicInt32,
                    DynamicDataBuilders.DynamicDataValue<DynamicBuilders.DynamicInt32> {
        private final FixedProto.FixedInt32 mImpl;
        private final @Nullable Fingerprint mFingerprint;

        FixedInt32(FixedProto.FixedInt32 impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. */
        public int getValue() {
            return mImpl.getValue();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull FixedInt32 fromProto(
                FixedProto.@NonNull FixedInt32 proto, @Nullable Fingerprint fingerprint) {
            return new FixedInt32(proto, fingerprint);
        }

        static @NonNull FixedInt32 fromProto(FixedProto.@NonNull FixedInt32 proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        FixedProto.@NonNull FixedInt32 toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DynamicProto.@NonNull DynamicInt32 toDynamicInt32Proto() {
            return DynamicProto.DynamicInt32.newBuilder().setFixed(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DynamicProto.@NonNull DynamicInt32 toDynamicInt32Proto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicInt32.newBuilder()
                        .setFixed(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicInt32Proto();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DynamicDataProto.@NonNull DynamicDataValue toDynamicDataValueProto() {
            return DynamicDataProto.DynamicDataValue.newBuilder().setInt32Val(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "FixedInt32{" + "value=" + getValue() + "}";
        }

        /**
         * Returns true if the {@link DynamicDataBuilders.DynamicDataValue} contains an int value.
         * Otherwise returns false.
         */
        @Override
        public boolean hasIntValue() {
            return true;
        }

        /**
         * Returns the int value stored in this {@link DynamicDataBuilders.DynamicDataValue }.
         *
         * @throws IllegalStateException if the {@link DynamicDataBuilders.DynamicDataValue }
         *     doesn't contain an int value.
         */
        @Override
        public int getIntValue() {
            return mImpl.getValue();
        }

        /**
         * Returns true if the {@link DynamicDataBuilders.DynamicDataValue} contains a value of the
         * specified {@code type}. Otherwise returns false.
         */
        @Override
        public boolean hasValueOfType(@NonNull Class<?> type) {
            return DynamicBuilders.DynamicInt32.class.isAssignableFrom(type)
                    || type == int.class
                    || type == Integer.class;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(mImpl.getValue());
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof FixedInt32)) {
                return false;
            }
            FixedInt32 that = (FixedInt32) obj;
            return this.mImpl.getValue() == that.mImpl.getValue();
        }

        /** Builder for {@link FixedInt32}. */
        public static final class Builder
                implements DynamicBuilders.DynamicInt32.Builder,
                        DynamicDataBuilders.DynamicDataValue.Builder<DynamicBuilders.DynamicInt32> {
            private final FixedProto.FixedInt32.Builder mImpl = FixedProto.FixedInt32.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(974881783);

            @RequiresSchemaVersion(major = 1, minor = 200)
            public Builder() {}

            /** Sets the value. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setValue(int value) {
                mImpl.setValue(value);
                mFingerprint.recordPropertyUpdate(1, value);
                return this;
            }

            @Override
            public @NonNull FixedInt32 build() {
                return new FixedInt32(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A fixed string type. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class FixedString
            implements DynamicBuilders.DynamicString,
                    DynamicDataBuilders.DynamicDataValue<DynamicBuilders.DynamicString> {
        private final FixedProto.FixedString mImpl;
        private final @Nullable Fingerprint mFingerprint;

        FixedString(FixedProto.FixedString impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. */
        public @NonNull String getValue() {
            return mImpl.getValue();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull FixedString fromProto(
                FixedProto.@NonNull FixedString proto, @Nullable Fingerprint fingerprint) {
            return new FixedString(proto, fingerprint);
        }

        static @NonNull FixedString fromProto(FixedProto.@NonNull FixedString proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        FixedProto.@NonNull FixedString toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DynamicProto.@NonNull DynamicString toDynamicStringProto() {
            return DynamicProto.DynamicString.newBuilder().setFixed(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DynamicProto.@NonNull DynamicString toDynamicStringProto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicString.newBuilder()
                        .setFixed(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicStringProto();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DynamicDataProto.@NonNull DynamicDataValue toDynamicDataValueProto() {
            return DynamicDataProto.DynamicDataValue.newBuilder().setStringVal(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "FixedString{" + "value=" + getValue() + "}";
        }

        /**
         * Returns true if the {@link DynamicDataBuilders.DynamicDataValue} contains a String value.
         * Otherwise returns false.
         */
        @Override
        public boolean hasStringValue() {
            return true;
        }

        /**
         * Returns the String value stored in this {@link DynamicDataBuilders.DynamicDataValue }.
         *
         * @throws IllegalStateException if the {@link DynamicDataBuilders.DynamicDataValue }
         *     doesn't contain a String value.
         */
        @Override
        public @NonNull String getStringValue() {
            return mImpl.getValue();
        }

        /**
         * Returns true if the {@link DynamicDataBuilders.DynamicDataValue} contains a value of the
         * specified {@code type}. Otherwise returns false.
         */
        @Override
        public boolean hasValueOfType(@NonNull Class<?> type) {
            return DynamicBuilders.DynamicString.class.isAssignableFrom(type)
                    || type == String.class;
        }

        @Override
        public int hashCode() {
            return mImpl.getValue().hashCode();
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof FixedString)) {
                return false;
            }
            FixedString that = (FixedString) obj;
            return this.mImpl.getValue().equals(that.mImpl.getValue());
        }

        /** Builder for {@link FixedString}. */
        public static final class Builder
                implements DynamicBuilders.DynamicString.Builder,
                        DynamicDataBuilders.DynamicDataValue.Builder<
                                DynamicBuilders.DynamicString> {
            private final FixedProto.FixedString.Builder mImpl =
                    FixedProto.FixedString.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1963352072);

            @RequiresSchemaVersion(major = 1, minor = 200)
            public Builder() {}

            /** Sets the value. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setValue(@NonNull String value) {
                mImpl.setValue(value);
                mFingerprint.recordPropertyUpdate(1, value.hashCode());
                return this;
            }

            @Override
            public @NonNull FixedString build() {
                return new FixedString(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A fixed float type. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class FixedFloat
            implements DynamicBuilders.DynamicFloat,
                    DynamicDataBuilders.DynamicDataValue<DynamicBuilders.DynamicFloat> {
        private final FixedProto.FixedFloat mImpl;
        private final @Nullable Fingerprint mFingerprint;

        FixedFloat(FixedProto.FixedFloat impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the value. Note that a NaN value is considered invalid and any expression with this
         * node will have an invalid value delivered via {@link
         * DynamicTypeValueReceiver<T>#onInvalidate()}.
         */
        public float getValue() {
            return mImpl.getValue();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull FixedFloat fromProto(
                FixedProto.@NonNull FixedFloat proto, @Nullable Fingerprint fingerprint) {
            return new FixedFloat(proto, fingerprint);
        }

        static @NonNull FixedFloat fromProto(FixedProto.@NonNull FixedFloat proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        FixedProto.@NonNull FixedFloat toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DynamicProto.@NonNull DynamicFloat toDynamicFloatProto() {
            return DynamicProto.DynamicFloat.newBuilder().setFixed(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DynamicProto.@NonNull DynamicFloat toDynamicFloatProto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicFloat.newBuilder()
                        .setFixed(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicFloatProto();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DynamicDataProto.@NonNull DynamicDataValue toDynamicDataValueProto() {
            return DynamicDataProto.DynamicDataValue.newBuilder().setFloatVal(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "FixedFloat{" + "value=" + getValue() + "}";
        }

        /**
         * Returns true if the {@link DynamicDataBuilders.DynamicDataValue} contains a float value.
         * Otherwise returns false.
         */
        @Override
        public boolean hasFloatValue() {
            return true;
        }

        /**
         * Returns the float value stored in this {@link DynamicDataBuilders.DynamicDataValue }.
         *
         * @throws IllegalStateException if the {@link DynamicDataBuilders.DynamicDataValue }
         *     doesn't contain a float value.
         */
        @Override
        public float getFloatValue() {
            return mImpl.getValue();
        }

        /**
         * Returns true if the {@link DynamicDataBuilders.DynamicDataValue} contains a value of the
         * specified {@code type}. Otherwise returns false.
         */
        @Override
        public boolean hasValueOfType(@NonNull Class<?> type) {
            return DynamicBuilders.DynamicFloat.class.isAssignableFrom(type)
                    || type == float.class
                    || type == Float.class;
        }

        @Override
        public int hashCode() {
            return Float.hashCode(mImpl.getValue());
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof FixedFloat)) {
                return false;
            }
            FixedFloat that = (FixedFloat) obj;
            return this.mImpl.getValue() == that.mImpl.getValue();
        }

        /** Builder for {@link FixedFloat}. */
        public static final class Builder
                implements DynamicBuilders.DynamicFloat.Builder,
                        DynamicDataBuilders.DynamicDataValue.Builder<DynamicBuilders.DynamicFloat> {
            private final FixedProto.FixedFloat.Builder mImpl = FixedProto.FixedFloat.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-144724541);

            @RequiresSchemaVersion(major = 1, minor = 200)
            public Builder() {}

            /**
             * Sets the value. Note that a NaN value is considered invalid and any expression with
             * this node will have an invalid value delivered via {@link
             * DynamicTypeValueReceiver<T>#onInvalidate()}.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setValue(float value) {
                mImpl.setValue(value);
                mFingerprint.recordPropertyUpdate(1, Float.floatToIntBits(value));
                return this;
            }

            @Override
            public @NonNull FixedFloat build() {
                return new FixedFloat(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A fixed boolean type. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class FixedBool
            implements DynamicBuilders.DynamicBool,
                    DynamicDataBuilders.DynamicDataValue<DynamicBuilders.DynamicBool> {
        private final FixedProto.FixedBool mImpl;
        private final @Nullable Fingerprint mFingerprint;

        FixedBool(FixedProto.FixedBool impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value. */
        public boolean getValue() {
            return mImpl.getValue();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull FixedBool fromProto(
                FixedProto.@NonNull FixedBool proto, @Nullable Fingerprint fingerprint) {
            return new FixedBool(proto, fingerprint);
        }

        static @NonNull FixedBool fromProto(FixedProto.@NonNull FixedBool proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        FixedProto.@NonNull FixedBool toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DynamicProto.@NonNull DynamicBool toDynamicBoolProto() {
            return DynamicProto.DynamicBool.newBuilder().setFixed(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DynamicProto.@NonNull DynamicBool toDynamicBoolProto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicBool.newBuilder()
                        .setFixed(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicBoolProto();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DynamicDataProto.@NonNull DynamicDataValue toDynamicDataValueProto() {
            return DynamicDataProto.DynamicDataValue.newBuilder().setBoolVal(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "FixedBool{" + "value=" + getValue() + "}";
        }

        /**
         * Returns true if the {@link DynamicDataBuilders.DynamicDataValue} contains a boolean
         * value. Otherwise returns false.
         */
        @Override
        public boolean hasBoolValue() {
            return true;
        }

        /**
         * Returns the boolean value stored in this {@link DynamicDataBuilders.DynamicDataValue }.
         *
         * @throws IllegalStateException if the {@link DynamicDataBuilders.DynamicDataValue }
         *     doesn't contain a boolean value.
         */
        @Override
        public boolean getBoolValue() {
            return mImpl.getValue();
        }

        /**
         * Returns true if the {@link DynamicDataBuilders.DynamicDataValue} contains a value of the
         * specified {@code type}. Otherwise returns false.
         */
        @Override
        public boolean hasValueOfType(@NonNull Class<?> type) {
            return DynamicBuilders.DynamicBool.class.isAssignableFrom(type)
                    || type == boolean.class
                    || type == Boolean.class;
        }

        @Override
        public int hashCode() {
            return Boolean.hashCode(mImpl.getValue());
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof FixedBool)) {
                return false;
            }
            FixedBool that = (FixedBool) obj;
            return this.mImpl.getValue() == that.mImpl.getValue();
        }

        /** Builder for {@link FixedBool}. */
        public static final class Builder
                implements DynamicBuilders.DynamicBool.Builder,
                        DynamicDataBuilders.DynamicDataValue.Builder<DynamicBuilders.DynamicBool> {
            private final FixedProto.FixedBool.Builder mImpl = FixedProto.FixedBool.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-665116398);

            @RequiresSchemaVersion(major = 1, minor = 200)
            public Builder() {}

            /** Sets the value. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @SuppressLint("MissingGetterMatchingBuilder")
            public @NonNull Builder setValue(boolean value) {
                mImpl.setValue(value);
                mFingerprint.recordPropertyUpdate(1, Boolean.hashCode(value));
                return this;
            }

            @Override
            public @NonNull FixedBool build() {
                return new FixedBool(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A fixed color type. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class FixedColor
            implements DynamicBuilders.DynamicColor,
                    DynamicDataBuilders.DynamicDataValue<DynamicBuilders.DynamicColor> {
        private final FixedProto.FixedColor mImpl;
        private final @Nullable Fingerprint mFingerprint;

        FixedColor(FixedProto.FixedColor impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the color value, in ARGB format. */
        @ColorInt
        public int getArgb() {
            return mImpl.getArgb();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull FixedColor fromProto(
                FixedProto.@NonNull FixedColor proto, @Nullable Fingerprint fingerprint) {
            return new FixedColor(proto, fingerprint);
        }

        static @NonNull FixedColor fromProto(FixedProto.@NonNull FixedColor proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        FixedProto.@NonNull FixedColor toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DynamicProto.@NonNull DynamicColor toDynamicColorProto() {
            return DynamicProto.DynamicColor.newBuilder().setFixed(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DynamicProto.@NonNull DynamicColor toDynamicColorProto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicColor.newBuilder()
                        .setFixed(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicColorProto();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DynamicDataProto.@NonNull DynamicDataValue toDynamicDataValueProto() {
            return DynamicDataProto.DynamicDataValue.newBuilder().setColorVal(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "FixedColor{" + "argb=" + getArgb() + "}";
        }

        /**
         * Returns true if the {@link DynamicDataBuilders.DynamicDataValue} contains a color value.
         * Otherwise returns false.
         */
        @Override
        public boolean hasColorValue() {
            return true;
        }

        /**
         * Returns the color value stored in this {@link DynamicDataBuilders.DynamicDataValue }.
         *
         * @throws IllegalStateException if the {@link DynamicDataBuilders.DynamicDataValue }
         *     doesn't contain a color value.
         */
        @Override
        public @ColorInt int getColorValue() {
            return mImpl.getArgb();
        }

        /**
         * Returns true if the {@link DynamicDataBuilders.DynamicDataValue} contains a value of the
         * specified {@code type}. Otherwise returns false.
         */
        @Override
        public boolean hasValueOfType(@NonNull Class<?> type) {
            return DynamicBuilders.DynamicColor.class.isAssignableFrom(type) || type == Color.class;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(mImpl.getArgb());
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof FixedColor)) {
                return false;
            }
            FixedColor that = (FixedColor) obj;
            return this.mImpl.getArgb() == that.mImpl.getArgb();
        }

        /** Builder for {@link FixedColor}. */
        public static final class Builder
                implements DynamicBuilders.DynamicColor.Builder,
                        DynamicDataBuilders.DynamicDataValue.Builder<DynamicBuilders.DynamicColor> {
            private final FixedProto.FixedColor.Builder mImpl = FixedProto.FixedColor.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1895809356);

            @RequiresSchemaVersion(major = 1, minor = 200)
            public Builder() {}

            /** Sets the color value, in ARGB format. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setArgb(@ColorInt int argb) {
                mImpl.setArgb(argb);
                mFingerprint.recordPropertyUpdate(1, argb);
                return this;
            }

            @Override
            public @NonNull FixedColor build() {
                return new FixedColor(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A fixed time instant type. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class FixedInstant
            implements DynamicBuilders.DynamicInstant,
                    DynamicDataBuilders.DynamicDataValue<DynamicBuilders.DynamicInstant> {
        private final FixedProto.FixedInstant mImpl;
        private final @Nullable Fingerprint mFingerprint;

        FixedInstant(FixedProto.FixedInstant impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the number of seconds that have elapsed since 00:00:00 UTC on 1 January 1970. */
        public long getEpochSeconds() {
            return mImpl.getEpochSeconds();
        }

        /**
         * Returns true if the {@link DynamicDataBuilders.DynamicDataValue} contains an {@link
         * Instant} value. Otherwise returns false.
         */
        @Override
        public boolean hasInstantValue() {
            return true;
        }

        /**
         * Returns the {@link Instant} value stored in this {@link
         * DynamicDataBuilders.DynamicDataValue }.
         *
         * @throws IllegalStateException if the {@link DynamicDataBuilders.DynamicDataValue }
         *     doesn't contain an {@link Instant} value.
         */
        @Override
        public @NonNull Instant getInstantValue() {
            return Instant.ofEpochSecond(mImpl.getEpochSeconds());
        }

        /**
         * Returns true if the {@link DynamicDataBuilders.DynamicDataValue} contains a value of the
         * specified {@code type}. Otherwise returns false.
         */
        @Override
        public boolean hasValueOfType(@NonNull Class<?> type) {
            return DynamicBuilders.DynamicInstant.class.isAssignableFrom(type)
                    || type == Instant.class;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(mImpl.getEpochSeconds());
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof FixedInstant)) {
                return false;
            }
            FixedInstant that = (FixedInstant) obj;
            return this.mImpl.getEpochSeconds() == that.mImpl.getEpochSeconds();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull FixedInstant fromProto(
                FixedProto.@NonNull FixedInstant proto, @Nullable Fingerprint fingerprint) {
            return new FixedInstant(proto, fingerprint);
        }

        static @NonNull FixedInstant fromProto(FixedProto.@NonNull FixedInstant proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        FixedProto.@NonNull FixedInstant toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DynamicProto.@NonNull DynamicInstant toDynamicInstantProto() {
            return DynamicProto.DynamicInstant.newBuilder().setFixed(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DynamicProto.@NonNull DynamicInstant toDynamicInstantProto(boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicInstant.newBuilder()
                        .setFixed(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicInstantProto();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DynamicDataProto.@NonNull DynamicDataValue toDynamicDataValueProto() {
            return DynamicDataProto.DynamicDataValue.newBuilder().setInstantVal(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "FixedInstant{" + "epochSeconds=" + getEpochSeconds() + "}";
        }

        /** Builder for {@link FixedInstant}. */
        public static final class Builder implements DynamicBuilders.DynamicInstant.Builder {
            private final FixedProto.FixedInstant.Builder mImpl =
                    FixedProto.FixedInstant.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1986552556);

            @RequiresSchemaVersion(major = 1, minor = 200)
            public Builder() {}

            /**
             * Sets the number of seconds that have elapsed since 00:00:00 UTC on 1 January 1970.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setEpochSeconds(long epochSeconds) {
                mImpl.setEpochSeconds(epochSeconds);
                mFingerprint.recordPropertyUpdate(1, Long.hashCode(epochSeconds));
                return this;
            }

            @Override
            public @NonNull FixedInstant build() {
                return new FixedInstant(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A fixed duration type. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class FixedDuration
            implements DynamicBuilders.DynamicDuration,
                    DynamicDataBuilders.DynamicDataValue<DynamicBuilders.DynamicDuration> {
        private final FixedProto.FixedDuration mImpl;
        private final @Nullable Fingerprint mFingerprint;

        FixedDuration(FixedProto.FixedDuration impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets duration in seconds. */
        public long getSeconds() {
            return mImpl.getSeconds();
        }

        /**
         * Returns true if the {@link DynamicDataBuilders.DynamicDataValue} contains a {@link
         * Duration} value. Otherwise returns false.
         */
        @Override
        public boolean hasDurationValue() {
            return true;
        }

        /**
         * Returns the {@link Duration} value stored in this {@link
         * DynamicDataBuilders.DynamicDataValue }.
         *
         * @throws IllegalStateException if the {@link DynamicDataBuilders.DynamicDataValue }
         *     doesn't contain a {@link Duration} value.
         */
        @Override
        public @NonNull Duration getDurationValue() {
            return Duration.ofSeconds(mImpl.getSeconds());
        }

        /**
         * Returns true if the {@link DynamicDataBuilders.DynamicDataValue} contains a value of the
         * specified {@code type}. Otherwise returns false.
         */
        @Override
        public boolean hasValueOfType(@NonNull Class<?> type) {
            return DynamicBuilders.DynamicDuration.class.isAssignableFrom(type)
                    || type == Duration.class;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(mImpl.getSeconds());
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof FixedDuration)) {
                return false;
            }
            FixedDuration that = (FixedDuration) obj;
            return this.mImpl.getSeconds() == that.mImpl.getSeconds();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull FixedDuration fromProto(
                FixedProto.@NonNull FixedDuration proto, @Nullable Fingerprint fingerprint) {
            return new FixedDuration(proto, fingerprint);
        }

        static @NonNull FixedDuration fromProto(FixedProto.@NonNull FixedDuration proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        FixedProto.@NonNull FixedDuration toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DynamicProto.@NonNull DynamicDuration toDynamicDurationProto() {
            return DynamicProto.DynamicDuration.newBuilder().setFixed(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DynamicProto.@NonNull DynamicDuration toDynamicDurationProto(
                boolean withFingerprint) {
            if (withFingerprint) {
                return DynamicProto.DynamicDuration.newBuilder()
                        .setFixed(mImpl)
                        .setFingerprint(checkNotNull(mFingerprint).toProto())
                        .build();
            }
            return toDynamicDurationProto();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DynamicDataProto.@NonNull DynamicDataValue toDynamicDataValueProto() {
            return DynamicDataProto.DynamicDataValue.newBuilder().setDurationVal(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "FixedDuration{" + "seconds=" + getSeconds() + "}";
        }

        /** Builder for {@link FixedDuration}. */
        public static final class Builder implements DynamicBuilders.DynamicDuration.Builder {
            private final FixedProto.FixedDuration.Builder mImpl =
                    FixedProto.FixedDuration.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(9029504);

            @RequiresSchemaVersion(major = 1, minor = 200)
            public Builder() {}

            /** Sets duration in seconds. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setSeconds(long seconds) {
                mImpl.setSeconds(seconds);
                mFingerprint.recordPropertyUpdate(1, Long.hashCode(seconds));
                return this;
            }

            @Override
            public @NonNull FixedDuration build() {
                return new FixedDuration(mImpl.build(), mFingerprint);
            }
        }
    }
}
