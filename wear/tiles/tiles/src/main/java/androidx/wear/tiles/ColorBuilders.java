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

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.expression.Fingerprint;
import androidx.wear.protolayout.proto.ColorProto;

/**
 * Builders for color utilities for layout elements.
 *
 * @deprecated Use {@link androidx.wear.protolayout.ColorBuilders} instead.
 */
@Deprecated
public final class ColorBuilders {
    private ColorBuilders() {}

    /** Shortcut for building a {@link ColorProp} using an ARGB value. */
    @NonNull
    public static ColorProp argb(@ColorInt int colorArgb) {
        return new ColorProp.Builder().setArgb(colorArgb).build();
    }

    /** A property defining a color. */
    public static final class ColorProp {
        private final ColorProto.ColorProp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        ColorProp(ColorProto.ColorProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the color value, in ARGB format. Intended for testing purposes only. */
        @ColorInt
        public int getArgb() {
            return mImpl.getArgb();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static ColorProp fromProto(@NonNull ColorProto.ColorProp proto) {
            return new ColorProp(proto, null);
        }

        @NonNull
        ColorProto.ColorProp toProto() {
            return mImpl;
        }

        /** Builder for {@link ColorProp} */
        public static final class Builder {
            private final ColorProto.ColorProp.Builder mImpl = ColorProto.ColorProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1332287496);

            public Builder() {}

            /** Sets the color value, in ARGB format. */
            @NonNull
            public Builder setArgb(@ColorInt int argb) {
                mImpl.setArgb(argb);
                mFingerprint.recordPropertyUpdate(1, argb);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public ColorProp build() {
                return new ColorProp(mImpl.build(), mFingerprint);
            }
        }
    }
}
