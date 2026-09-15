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

package androidx.camera.video;

import android.location.Location;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** A fake implementation of {@link OutputOptions}. */
public class FakeOutputOptions extends OutputOptions {

    private FakeOutputOptions(@NonNull FakeOutputOptionsInternal fakeOutputOptionsInternal) {
        super(fakeOutputOptionsInternal);
    }

    /** The builder of the {@link FakeOutputOptions} object. */
    public static final class Builder extends OutputOptions.Builder<FakeOutputOptions, Builder> {

        /** Creates a builder of the {@link FakeOutputOptions}. */
        public Builder() {
            super(new FakeOutputOptionsInternal.Builder());
        }

        /** Builds the {@link FakeOutputOptions} instance. */
        @Override
        public @NonNull FakeOutputOptions build() {
            return new FakeOutputOptions(
                    ((FakeOutputOptionsInternal.Builder) mRootInternalBuilder).build());
        }
    }

    static class FakeOutputOptionsInternal extends OutputOptions.OutputOptionsInternal {
        private final long mFileSizeLimit;
        private final long mDurationLimitMillis;
        private final Location mLocation;

        FakeOutputOptionsInternal(
                long fileSizeLimit,
                long durationLimitMillis,
                @Nullable Location location) {
            mFileSizeLimit = fileSizeLimit;
            mDurationLimitMillis = durationLimitMillis;
            mLocation = location;
        }

        @Override
        long getFileSizeLimit() {
            return mFileSizeLimit;
        }

        @Override
        long getDurationLimitMillis() {
            return mDurationLimitMillis;
        }

        @Override
        @Nullable Location getLocation() {
            return mLocation;
        }

        static class Builder extends OutputOptions.OutputOptionsInternal.Builder<Builder> {
            private long mFileSizeLimit = OutputOptions.FILE_SIZE_UNLIMITED;
            private long mDurationLimitMillis = OutputOptions.DURATION_UNLIMITED;
            private Location mLocation = null;

            @Override
            @NonNull Builder setFileSizeLimit(long fileSizeLimitBytes) {
                mFileSizeLimit = fileSizeLimitBytes;
                return this;
            }

            @Override
            @NonNull Builder setDurationLimitMillis(long durationLimitMillis) {
                mDurationLimitMillis = durationLimitMillis;
                return this;
            }

            @Override
            @NonNull Builder setLocation(@Nullable Location location) {
                mLocation = location;
                return this;
            }

            @Override
            @NonNull FakeOutputOptionsInternal build() {
                return new FakeOutputOptionsInternal(
                        mFileSizeLimit, mDurationLimitMillis, mLocation);
            }
        }
    }
}
