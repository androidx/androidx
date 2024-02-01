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

package androidx.wear.protolayout;

import static androidx.wear.protolayout.expression.Preconditions.checkNotNull;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.expression.DynamicBuilders;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicBool;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString;
import androidx.wear.protolayout.expression.Fingerprint;
import androidx.wear.protolayout.expression.RequiresSchemaVersion;
import androidx.wear.protolayout.proto.AlignmentProto;
import androidx.wear.protolayout.proto.TypesProto;

/** Builders for extensible primitive types used by layout elements. */
public final class TypeBuilders {
    private TypeBuilders() {}

    /**
     * A type for specifying layout constraints when using {@link StringProp} on a data bindable
     * layout element.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public static final class StringLayoutConstraint {
        private final TypesProto.StringProp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        StringLayoutConstraint(TypesProto.StringProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the text string to use as the pattern for the largest text that can be laid out.
         * Used to ensure that the layout is of a known size during the layout pass.
         */
        @NonNull
        public String getPatternForLayout() {
            return mImpl.getValueForLayout();
        }

        /** Gets angular alignment of the actual content within the space reserved by value. */
        @LayoutElementBuilders.TextAlignment
        public int getAlignment() {
            return mImpl.getTextAlignmentForLayoutValue();
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public TypesProto.StringProp toProto() {
            return mImpl;
        }

        @NonNull
        static StringLayoutConstraint fromProto(@NonNull TypesProto.StringProp proto) {
            return new StringLayoutConstraint(proto, null);
        }

        /** Builder for {@link StringLayoutConstraint}. */
        public static final class Builder {
            private final TypesProto.StringProp.Builder mImpl = TypesProto.StringProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1927567664);

            /**
             * Creates a new builder for {@link StringLayoutConstraint}.
             *
             * @param patternForLayout Sets the text string to use as the pattern for the largest
             *     text that can be laid out. Used to ensure that the layout is of a known size
             *     during the layout pass.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public Builder(@NonNull String patternForLayout) {
                setValue(patternForLayout);
            }

            /**
             * Sets the text string to use as the pattern for the largest text that can be laid out.
             * Used to ensure that the layout is of a known size during the layout pass.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            private Builder setValue(@NonNull String patternForLayout) {
                mImpl.setValueForLayout(patternForLayout);
                mFingerprint.recordPropertyUpdate(3, patternForLayout.hashCode());
                return this;
            }

            /**
             * Sets alignment of the actual text within the space reserved by patternForLayout. If
             * not specified, defaults to center alignment.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setAlignment(@LayoutElementBuilders.TextAlignment int alignment) {
                mImpl.setTextAlignmentForLayout(AlignmentProto.TextAlignment.forNumber(alignment));
                mFingerprint.recordPropertyUpdate(4, alignment);
                return this;
            }

            /** Builds an instance of {@link StringLayoutConstraint}. */
            @NonNull
            public StringLayoutConstraint build() {
                return new StringLayoutConstraint(mImpl.build(), mFingerprint);
            }
        }
    }

    /** An int32 type. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class Int32Prop {
        private final TypesProto.Int32Prop mImpl;
        @Nullable private final Fingerprint mFingerprint;

        Int32Prop(TypesProto.Int32Prop impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the static value. */
        public int getValue() {
            return mImpl.getValue();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Int32Prop fromProto(
                @NonNull TypesProto.Int32Prop proto, @Nullable Fingerprint fingerprint) {
            return new Int32Prop(proto, fingerprint);
        }

        @NonNull
        static Int32Prop fromProto(@NonNull TypesProto.Int32Prop proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public TypesProto.Int32Prop toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "Int32Prop{" + "value=" + getValue() + "}";
        }

        /** Builder for {@link Int32Prop} */
        public static final class Builder {
            private final TypesProto.Int32Prop.Builder mImpl = TypesProto.Int32Prop.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1360212989);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Sets the static value. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            @NonNull
            public Builder setValue(int value) {
                mImpl.setValue(value);
                mFingerprint.recordPropertyUpdate(1, value);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public Int32Prop build() {
                return new Int32Prop(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A string type. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class StringProp {
        private final TypesProto.StringProp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        StringProp(TypesProto.StringProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the static value. If a dynamic value is also set and the renderer supports dynamic
         * values for the corresponding field, this static value will be ignored. If the static
         * value is not specified, {@code null} will be used instead.
         */
        @NonNull
        public String getValue() {
            return mImpl.getValue();
        }

        /**
         * Gets the dynamic value. Note that when setting this value, the static value is still
         * required to be set to support older renderers that only read the static value. If {@code
         * dynamicValue} has an invalid result, the provided static value will be used instead.
         */
        @Nullable
        public DynamicString getDynamicValue() {
            if (mImpl.hasDynamicValue()) {
                return DynamicBuilders.dynamicStringFromProto(mImpl.getDynamicValue());
            } else {
                return null;
            }
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static StringProp fromProto(
                @NonNull TypesProto.StringProp proto, @Nullable Fingerprint fingerprint) {
            return new StringProp(proto, fingerprint);
        }

        @NonNull
        static StringProp fromProto(@NonNull TypesProto.StringProp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public TypesProto.StringProp toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "StringProp{"
                    + "value="
                    + getValue()
                    + ", dynamicValue="
                    + getDynamicValue()
                    + "}";
        }

        /** Builder for {@link StringProp} */
        public static final class Builder {
            private final TypesProto.StringProp.Builder mImpl = TypesProto.StringProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(327834307);

            /**
             * Creates an instance of {@link Builder} from the given static value. {@link
             * #setDynamicValue(DynamicString)} can be used to provide a dynamic value.
             */
            public Builder(@NonNull String staticValue) {
                setValue(staticValue);
            }

            /**
             * Creates an instance of {@link Builder}.
             *
             * @deprecated use {@link #Builder(String)}
             */
            @Deprecated
            public Builder() {}

            /**
             * Sets the static value. If a dynamic value is also set and the renderer supports
             * dynamic values for the corresponding field, this static value will be ignored. If the
             * static value is not specified, {@code null} will be used instead.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            @NonNull
            public Builder setValue(@NonNull String value) {
                mImpl.setValue(value);
                mFingerprint.recordPropertyUpdate(1, value.hashCode());
                return this;
            }

            /**
             * Sets the dynamic value. Note that when setting this value, the static value is still
             * required to be set to support older renderers that only read the static value. If
             * {@code dynamicValue} has an invalid result, the provided static value will be used
             * instead.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setDynamicValue(@NonNull DynamicString dynamicValue) {
                mImpl.setDynamicValue(dynamicValue.toDynamicStringProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(dynamicValue.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Builds an instance from accumulated values.
             *
             * @throws IllegalStateException if a dynamic value is set using {@link
             *     #setDynamicValue(DynamicString)} but neither {@link #Builder(String)} nor {@link
             *     #setValue(String)} is used to provide a static value.
             */
            @NonNull
            public StringProp build() {
                if (mImpl.hasDynamicValue() && !mImpl.hasValue()) {
                    throw new IllegalStateException("Static value is missing.");
                }
                return new StringProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A float type. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class FloatProp {
        private final TypesProto.FloatProp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        FloatProp(TypesProto.FloatProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the static value. If a dynamic value is also set and the renderer supports dynamic
         * values for the corresponding field, this static value will be ignored. If the static
         * value is not specified, zero will be used instead.
         */
        public float getValue() {
            return mImpl.getValue();
        }

        /**
         * Gets the dynamic value. Note that when setting this value, the static value is still
         * required to be set to support older renderers that only read the static value. If {@code
         * dynamicValue} has an invalid result, the provided static value will be used instead.
         */
        @Nullable
        public DynamicFloat getDynamicValue() {
            if (mImpl.hasDynamicValue()) {
                return DynamicBuilders.dynamicFloatFromProto(mImpl.getDynamicValue());
            } else {
                return null;
            }
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static FloatProp fromProto(
                @NonNull TypesProto.FloatProp proto, @Nullable Fingerprint fingerprint) {
            return new FloatProp(proto, fingerprint);
        }

        @NonNull
        static FloatProp fromProto(@NonNull TypesProto.FloatProp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public TypesProto.FloatProp toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "FloatProp{"
                    + "value="
                    + getValue()
                    + ", dynamicValue="
                    + getDynamicValue()
                    + "}";
        }

        /** Builder for {@link FloatProp} */
        public static final class Builder {
            private final TypesProto.FloatProp.Builder mImpl = TypesProto.FloatProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-641088370);

            /**
             * Creates an instance of {@link Builder} from the given static value. {@link
             * #setDynamicValue(DynamicFloat)} can be used to provide a dynamic value.
             */
            public Builder(float staticValue) {
                setValue(staticValue);
            }

            /**
             * Creates an instance of {@link Builder}.
             *
             * @deprecated use {@link #Builder(float)}
             */
            @Deprecated
            public Builder() {}

            /**
             * Sets the static value. If a dynamic value is also set and the renderer supports
             * dynamic values for the corresponding field, this static value will be ignored. If the
             * static value is not specified, zero will be used instead.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            @NonNull
            public Builder setValue(float value) {
                mImpl.setValue(value);
                mFingerprint.recordPropertyUpdate(1, Float.floatToIntBits(value));
                return this;
            }

            /**
             * Sets the dynamic value. Note that when setting this value, the static value is still
             * required to be set (with either {@link #Builder(float)} or {@link #setValue(float)})
             * to support older renderers that only read the static value. If {@code dynamicValue }
             * has an invalid result, the provided static value will be used instead.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setDynamicValue(@NonNull DynamicFloat dynamicValue) {
                mImpl.setDynamicValue(dynamicValue.toDynamicFloatProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(dynamicValue.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Builds an instance from accumulated values.
             *
             * @throws IllegalStateException if a dynamic value is set using {@link
             *     #setDynamicValue(DynamicFloat)} but neither {@link #Builder(float)} nor {@link
             *     #setValue(float)} is used to provide a static value.
             */
            @NonNull
            public FloatProp build() {
                if (mImpl.hasDynamicValue() && !mImpl.hasValue()) {
                    throw new IllegalStateException("Static value is missing.");
                }
                return new FloatProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A boolean type. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class BoolProp {
        private final TypesProto.BoolProp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        BoolProp(TypesProto.BoolProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the static value. If a dynamic value is also set and the renderer supports dynamic
         * values for the corresponding field, this static value will be ignored. If the static
         * value is not specified, false will be used instead.
         */
        public boolean getValue() {
            return mImpl.getValue();
        }

        /**
         * Gets the dynamic value. Note that when setting this value, the static value is still
         * required to be set to support older renderers that only read the static value. If {@code
         * dynamicValue} has an invalid result, the provided static value will be used instead.
         */
        @Nullable
        public DynamicBool getDynamicValue() {
            if (mImpl.hasDynamicValue()) {
                return DynamicBuilders.dynamicBoolFromProto(mImpl.getDynamicValue());
            } else {
                return null;
            }
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static BoolProp fromProto(
                @NonNull TypesProto.BoolProp proto, @Nullable Fingerprint fingerprint) {
            return new BoolProp(proto, fingerprint);
        }

        @NonNull
        static BoolProp fromProto(@NonNull TypesProto.BoolProp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public TypesProto.BoolProp toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "BoolProp{"
                    + "value="
                    + getValue()
                    + ", dynamicValue="
                    + getDynamicValue()
                    + "}";
        }

        /** Builder for {@link BoolProp} */
        public static final class Builder {
            private final TypesProto.BoolProp.Builder mImpl = TypesProto.BoolProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1691257528);

            /**
             * Creates an instance of {@link Builder} from the given static value. {@link
             * #setDynamicValue(DynamicBool)} can be used to provide a dynamic value.
             */
            public Builder(boolean staticValue) {
                setValue(staticValue);
            }

            /**
             * Creates an instance of {@link Builder}.
             *
             * @deprecated use {@link #Builder(boolean)}
             */
            @Deprecated
            public Builder() {}

            /**
             * Sets the static value. If a dynamic value is also set and the renderer supports
             * dynamic values for the corresponding field, this static value will be ignored. If the
             * static value is not specified, false will be used instead.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setValue(boolean value) {
                mImpl.setValue(value);
                mFingerprint.recordPropertyUpdate(1, Boolean.hashCode(value));
                return this;
            }

            /**
             * Sets the dynamic value. Note that when setting this value, the static value is still
             * required to be set to support older renderers that only read the static value. If
             * {@code dynamicValue} has an invalid result, the provided static value will be used
             * instead.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @NonNull
            public Builder setDynamicValue(@NonNull DynamicBool dynamicValue) {
                mImpl.setDynamicValue(dynamicValue.toDynamicBoolProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(dynamicValue.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Builds an instance from accumulated values.
             *
             * @throws IllegalStateException if a dynamic value is set using {@link
             *     #setDynamicValue(DynamicBool)} but neither {@link #Builder(boolean)} nor {@link
             *     #setValue(boolean)} is used to provide a static value.
             */
            @NonNull
            public BoolProp build() {
                if (mImpl.hasDynamicValue() && !mImpl.hasValue()) {
                    throw new IllegalStateException("Static value is missing.");
                }
                return new BoolProp(mImpl.build(), mFingerprint);
            }
        }
    }
}
