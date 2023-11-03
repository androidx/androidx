/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.expression.proto.VersionProto;

/** Builders for the schema version information of a layout (or an expression). */
public final class VersionBuilders {
    private VersionBuilders() {}

    /**
     * Version information. This is used to encode the schema version of a payload (e.g. inside of a
     * layout).
     *
     * @since 1.0
     */
    public static final class VersionInfo {
        private final VersionProto.VersionInfo mImpl;
        @Nullable private final Fingerprint mFingerprint;

        VersionInfo(VersionProto.VersionInfo impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets major version. Incremented on breaking changes (i.e. compatibility is not guaranteed
         * across major versions).
         *
         * @since 1.0
         */
        public int getMajor() {
            return mImpl.getMajor();
        }

        /**
         * Gets minor version. Incremented on non-breaking changes (e.g. schema additions). Anything
         * consuming a payload can safely consume anything with a lower minor version.
         *
         * @since 1.0
         */
        public int getMinor() {
            return mImpl.getMinor();
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
        public static VersionInfo fromProto(
                @NonNull VersionProto.VersionInfo proto, @Nullable Fingerprint fingerprint) {
            return new VersionInfo(proto, fingerprint);
        }

        /**
         * Creates a new wrapper instance from the proto. Intended for testing purposes only. An
         * object created using this method can't be added to any other wrapper.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static VersionInfo fromProto(@NonNull VersionProto.VersionInfo proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public VersionProto.VersionInfo toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "VersionInfo{" + "major=" + getMajor() + ", minor=" + getMinor() + "}";
        }

        /** Builder for {@link VersionInfo} */
        public static final class Builder {
            private final VersionProto.VersionInfo.Builder mImpl =
                    VersionProto.VersionInfo.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(77091996);

            public Builder() {}

            /**
             * Sets major version. Incremented on breaking changes (i.e. compatibility is not
             * guaranteed across major versions).
             *
             * @since 1.0
             */
            @NonNull
            public Builder setMajor(int major) {
                mImpl.setMajor(major);
                mFingerprint.recordPropertyUpdate(1, major);
                return this;
            }

            /**
             * Sets minor version. Incremented on non-breaking changes (e.g. schema additions).
             * Anything consuming a payload can safely consume anything with a lower minor version.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setMinor(int minor) {
                mImpl.setMinor(minor);
                mFingerprint.recordPropertyUpdate(2, minor);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public VersionInfo build() {
                return new VersionInfo(mImpl.build(), mFingerprint);
            }
        }
    }
}
