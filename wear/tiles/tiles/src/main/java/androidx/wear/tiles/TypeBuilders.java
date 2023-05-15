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

package androidx.wear.tiles;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.proto.TypesProto;

/**
 * Builders for extensible primitive types used by layout elements.
 *
 * @deprecated Use {@link androidx.wear.protolayout.TypeBuilders} instead.
 */
@Deprecated
public final class TypeBuilders {
    private TypeBuilders() {}

    /** An int32 type. */
    public static final class Int32Prop {
        private final TypesProto.Int32Prop mImpl;

        private Int32Prop(TypesProto.Int32Prop impl) {
            this.mImpl = impl;
        }

        /** Gets the value. Intended for testing purposes only. */
        public int getValue() {
            return mImpl.getValue();
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Int32Prop fromProto(@NonNull TypesProto.Int32Prop proto) {
            return new Int32Prop(proto);
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public TypesProto.Int32Prop toProto() {
            return mImpl;
        }

        /** Builder for {@link Int32Prop} */
        public static final class Builder {
            private final TypesProto.Int32Prop.Builder mImpl = TypesProto.Int32Prop.newBuilder();

            public Builder() {}

            /** Sets the value. */
            @NonNull
            public Builder setValue(int value) {
                mImpl.setValue(value);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public Int32Prop build() {
                return Int32Prop.fromProto(mImpl.build());
            }
        }
    }

    /** A string type. */
    public static final class StringProp {
        private final TypesProto.StringProp mImpl;

        private StringProp(TypesProto.StringProp impl) {
            this.mImpl = impl;
        }

        /** Gets the value. Intended for testing purposes only. */
        @NonNull
        public String getValue() {
            return mImpl.getValue();
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static StringProp fromProto(@NonNull TypesProto.StringProp proto) {
            return new StringProp(proto);
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public TypesProto.StringProp toProto() {
            return mImpl;
        }

        /** Builder for {@link StringProp} */
        public static final class Builder {
            private final TypesProto.StringProp.Builder mImpl = TypesProto.StringProp.newBuilder();

            public Builder() {}

            /** Sets the value. */
            @NonNull
            public Builder setValue(@NonNull String value) {
                mImpl.setValue(value);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public StringProp build() {
                return StringProp.fromProto(mImpl.build());
            }
        }
    }

    /** A float type. */
    public static final class FloatProp {
        private final TypesProto.FloatProp mImpl;

        private FloatProp(TypesProto.FloatProp impl) {
            this.mImpl = impl;
        }

        /** Gets the value. Intended for testing purposes only. */
        public float getValue() {
            return mImpl.getValue();
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static FloatProp fromProto(@NonNull TypesProto.FloatProp proto) {
            return new FloatProp(proto);
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public TypesProto.FloatProp toProto() {
            return mImpl;
        }

        /** Builder for {@link FloatProp} */
        public static final class Builder {
            private final TypesProto.FloatProp.Builder mImpl = TypesProto.FloatProp.newBuilder();

            public Builder() {}

            /** Sets the value. */
            @NonNull
            public Builder setValue(float value) {
                mImpl.setValue(value);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public FloatProp build() {
                return FloatProp.fromProto(mImpl.build());
            }
        }
    }

    /** A boolean type. */
    public static final class BoolProp {
        private final TypesProto.BoolProp mImpl;

        private BoolProp(TypesProto.BoolProp impl) {
            this.mImpl = impl;
        }

        /** Gets the value. Intended for testing purposes only. */
        public boolean getValue() {
            return mImpl.getValue();
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static BoolProp fromProto(@NonNull TypesProto.BoolProp proto) {
            return new BoolProp(proto);
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public TypesProto.BoolProp toProto() {
            return mImpl;
        }

        /** Builder for {@link BoolProp} */
        public static final class Builder {
            private final TypesProto.BoolProp.Builder mImpl = TypesProto.BoolProp.newBuilder();

            public Builder() {}

            /** Sets the value. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setValue(boolean value) {
                mImpl.setValue(value);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public BoolProp build() {
                return BoolProp.fromProto(mImpl.build());
            }
        }
    }
}
