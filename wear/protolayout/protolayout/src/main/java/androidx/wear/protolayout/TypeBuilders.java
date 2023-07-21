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
import androidx.wear.protolayout.expression.Fingerprint;
import androidx.wear.protolayout.proto.AlignmentProto;
import androidx.wear.protolayout.proto.TypesProto;

/** Builders for extensible primitive types used by layout elements. */
public final class TypeBuilders {
    private TypeBuilders() {}

    /**
     * An int32 type.
     *
     * @since 1.0
     */
    public static final class Int32Prop {
        private final TypesProto.Int32Prop mImpl;
        @Nullable private final Fingerprint mFingerprint;

        Int32Prop(TypesProto.Int32Prop impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the value.
         *
         * @since 1.0
         */
        public int getValue() {
            return mImpl.getValue();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static Int32Prop fromProto(@NonNull TypesProto.Int32Prop proto) {
            return new Int32Prop(proto, null);
        }

        @NonNull
        TypesProto.Int32Prop toProto() {
            return mImpl;
        }

        /** Builder for {@link Int32Prop} */
        public static final class Builder {
            private final TypesProto.Int32Prop.Builder mImpl = TypesProto.Int32Prop.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1360212989);

            public Builder() {}

            /**
             * Sets the value.
             *
             * @since 1.0
             */
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

    /**
     * A string type.
     *
     * @since 1.0
     */
    public static final class StringProp {
        private final TypesProto.StringProp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        StringProp(TypesProto.StringProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the static value.
         *
         * @since 1.0
         */
        @NonNull
        public String getValue() {
            return mImpl.getValue();
        }

        /**
         * Gets the dynamic value.
         *
         * @since 1.2
         */
        @Nullable
        public DynamicBuilders.DynamicString getDynamicValue() {
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

        @NonNull
        static StringProp fromProto(@NonNull TypesProto.StringProp proto) {
            return new StringProp(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public TypesProto.StringProp toProto() {
            return mImpl;
        }

        /** Builder for {@link StringProp} */
        public static final class Builder {
            private final TypesProto.StringProp.Builder mImpl = TypesProto.StringProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(327834307);

            /**
             * Creates an instance of {@link Builder}.
             *
             * @deprecated use {@link Builder(String)}
             */
            @Deprecated
            public Builder() {}

            /**
             * Creates an instance of {@link Builder}.
             *
             * @param staticValue the static value.
             */
            public Builder(@NonNull String staticValue) {
                setValue(staticValue);
            }

            /**
             * Sets the static value. If a dynamic value is also set and the renderer supports
             * dynamic values for the corresponding field, this static value will be ignored.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setValue(@NonNull String value) {
                mImpl.setValue(value);
                mFingerprint.recordPropertyUpdate(1, value.hashCode());
                return this;
            }

            /**
             * Sets the dynamic value. Note that when setting this value, the static value is still
             * required to be set to support older renderers that only read the static value.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setDynamicValue(@NonNull DynamicBuilders.DynamicString dynamicValue) {
                mImpl.setDynamicValue(dynamicValue.toDynamicStringProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(dynamicValue.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public StringProp build() {
                if (mImpl.hasDynamicValue() && !mImpl.hasValue()) {
                    throw new IllegalStateException("Static value is missing.");
                }
                return new StringProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A type for specifying layout constraints when using {@link StringProp} on a data bindable
     * layout element.
     *
     * @since 1.2
     */
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
         *
         * @since 1.2
         */
        @NonNull
        public String getPatternForLayout() {
            return mImpl.getValueForLayout();
        }

        /**
         * Gets angular alignment of the actual content within the space reserved by value.
         *
         * @since 1.2
         */
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
             * @since 1.2
             */
            public Builder(@NonNull String patternForLayout) {
                setValue(patternForLayout);
            }

            /**
             * Sets the text string to use as the pattern for the largest text that can be laid out.
             * Used to ensure that the layout is of a known size during the layout pass.
             *
             * @since 1.2
             */
            @NonNull
            private Builder setValue(@NonNull String patternForLayout) {
                mImpl.setValueForLayout(patternForLayout);
                mFingerprint.recordPropertyUpdate(3, patternForLayout.hashCode());
                return this;
            }

            /**
             * Sets alignment of the actual text within the space reserved by patternForLayout. If
             * not specified, defaults to center alignment.
             *
             * @since 1.2
             */
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

    /**
     * A float type.
     *
     * @since 1.0
     */
    public static final class FloatProp {
        private final TypesProto.FloatProp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        FloatProp(TypesProto.FloatProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the value.
         *
         * @since 1.0
         */
        public float getValue() {
            return mImpl.getValue();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static FloatProp fromProto(@NonNull TypesProto.FloatProp proto) {
            return new FloatProp(proto, null);
        }

        @NonNull
        TypesProto.FloatProp toProto() {
            return mImpl;
        }

        /** Builder for {@link FloatProp} */
        public static final class Builder {
            private final TypesProto.FloatProp.Builder mImpl = TypesProto.FloatProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-641088370);

            public Builder() {}

            /**
             * Sets the value.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setValue(float value) {
                mImpl.setValue(value);
                mFingerprint.recordPropertyUpdate(1, Float.floatToIntBits(value));
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public FloatProp build() {
                return new FloatProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A boolean type.
     *
     * @since 1.0
     */
    public static final class BoolProp {
        private final TypesProto.BoolProp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        BoolProp(TypesProto.BoolProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the value.
         *
         * @since 1.0
         */
        public boolean getValue() {
            return mImpl.getValue();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static BoolProp fromProto(@NonNull TypesProto.BoolProp proto) {
            return new BoolProp(proto, null);
        }

        @NonNull
        TypesProto.BoolProp toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "BoolProp{" + "value=" + getValue() + "}";
        }

        /** Builder for {@link BoolProp} */
        public static final class Builder {
            private final TypesProto.BoolProp.Builder mImpl = TypesProto.BoolProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1691257528);

            public Builder() {}

            /**
             * Sets the value.
             *
             * @since 1.0
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setValue(boolean value) {
                mImpl.setValue(value);
                mFingerprint.recordPropertyUpdate(1, Boolean.hashCode(value));
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public BoolProp build() {
                return new BoolProp(mImpl.build(), mFingerprint);
            }
        }
    }
}
