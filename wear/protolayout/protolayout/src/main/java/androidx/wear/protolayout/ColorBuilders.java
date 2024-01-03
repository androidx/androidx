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

import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.expression.DynamicBuilders;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicColor;
import androidx.wear.protolayout.expression.Fingerprint;
import androidx.wear.protolayout.proto.ColorProto;

/** Builders for color utilities for layout elements. */
public final class ColorBuilders {
    private ColorBuilders() {}

    /**
     * Shortcut for building a {@link ColorProp} using an ARGB value.
     *
     * @since 1.0
     */
    @NonNull
    public static ColorProp argb(@ColorInt int colorArgb) {
        return new ColorProp.Builder().setArgb(colorArgb).build();
    }

    /**
     * A property defining a color.
     *
     * @since 1.0
     */
    public static final class ColorProp {
        private final ColorProto.ColorProp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        ColorProp(ColorProto.ColorProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the static color value, in ARGB format. If a dynamic value is also set and the
         * renderer supports dynamic values for the corresponding field, this static value will be
         * ignored. If the static value is not specified, zero (equivalent to {@link
         * Color#TRANSPARENT}) will be used instead.
         *
         * @since 1.0
         */
        @ColorInt
        public int getArgb() {
            return mImpl.getArgb();
        }

        /**
         * Gets the dynamic value. Note that when setting this value, the static value is still
         * required to be set to support older renderers that only read the static value. If {@code
         * dynamicValue} has an invalid result, the provided static value will be used instead.
         *
         * @since 1.2
         */
        @Nullable
        public DynamicColor getDynamicValue() {
            if (mImpl.hasDynamicValue()) {
                return DynamicBuilders.dynamicColorFromProto(mImpl.getDynamicValue());
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
        public static ColorProp fromProto(
                @NonNull ColorProto.ColorProp proto, @Nullable Fingerprint fingerprint) {
            return new ColorProp(proto, fingerprint);
        }

        @NonNull
        static ColorProp fromProto(@NonNull ColorProto.ColorProp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ColorProto.ColorProp toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "ColorProp{" + "argb=" + getArgb() + ", dynamicValue=" + getDynamicValue() + "}";
        }

        /** Builder for {@link ColorProp} */
        public static final class Builder {
            private final ColorProto.ColorProp.Builder mImpl = ColorProto.ColorProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1955659823);

            /**
             * @deprecated Use {@link #Builder(int)} instead.
             */
            @Deprecated
            public Builder() {}

            public Builder(@ColorInt int argb) {
                setArgb(argb);
            }

            /**
             * Sets the static color value, in ARGB format. If a dynamic value is also set and the
             * renderer supports dynamic values for the corresponding field, this static value will
             * be ignored. If the static value is not specified, zero (equivalent to {@link
             * Color#TRANSPARENT}) will be used instead.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setArgb(@ColorInt int argb) {
                mImpl.setArgb(argb);
                mFingerprint.recordPropertyUpdate(1, argb);
                return this;
            }

            /**
             * Sets the dynamic value. Note that when setting this value, the static value is still
             * required to be set to support older renderers that only read the static value. If
             * {@code dynamicValue} has an invalid result, the provided static value will be used
             * instead.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setDynamicValue(@NonNull DynamicColor dynamicValue) {
                mImpl.setDynamicValue(dynamicValue.toDynamicColorProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(dynamicValue.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Builds an instance from accumulated values.
             *
             * @throws IllegalStateException if a dynamic value is set using {@link
             *     #setDynamicValue(DynamicColor)} but neither {@link #Builder(int)} nor {@link
             *     #setArgb(int)} is used to provide a static value.
             */
            @NonNull
            public ColorProp build() {
                if (mImpl.hasDynamicValue() && !mImpl.hasArgb()) {
                    throw new IllegalStateException("Static value is missing.");
                }
                return new ColorProp(mImpl.build(), mFingerprint);
            }
        }
    }
}
