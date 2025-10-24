/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appsearch.localstorage.stats;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents statistics for a vm start attempt. Usually it is reported under {@link
 * VmInitializationStats}.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class VmStartAttemptStats {
    /**
     * Status codes for vm start result.
     *
     * <p>Needs to be kept consistent with AppSearchVmStartAttempts in
     * frameworks/proto_logging/stats/atoms/appsearch/appsearch_extension_atoms.proto
     */
    @IntDef(
            value = {
                    VM_START_STATUS_UNKNOWN,
                    VM_START_STATUS_SUCCESS,
                    VM_START_STATUS_TIMEOUT,
                    VM_START_STATUS_ERROR,
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface VmStartStatus {}

    /** Represents an unknown status for vm start result. */
    public static final int VM_START_STATUS_UNKNOWN = 0;

    /** Represents success status for vm start result. */
    public static final int VM_START_STATUS_SUCCESS = 1;

    /** Represents timeout status for vm start result. */
    public static final int VM_START_STATUS_TIMEOUT = 2;

    /** Represents error status for vm start result. */
    public static final int VM_START_STATUS_ERROR = 3;

    /** The status code of vm start result. */
    @VmStartStatus private final int mStatusCode;

    /** The overall latency (in milliseconds) of vm start attempt. */
    private final long mLatencyMillis;

    /**
     * Constructs a new {@link VmStartAttemptStats} object using the provided builder.
     *
     * @param builder The builder containing the statistics.
     */
    VmStartAttemptStats(@NonNull Builder builder) {
        mStatusCode = builder.mStatusCode;
        mLatencyMillis = builder.mLatencyMillis;
    }

    /** Returns the status code of vm start result. */
    @VmStartStatus
    public int getStatusCode() {
        return mStatusCode;
    }

    /** Returns the overall latency (in milliseconds) of vm start attempt. */
    public long getLatencyMillis() {
        return mLatencyMillis;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(
                "VmStartAttemptStats {\n"
                        + "  statusCode=%d,\n"
                        + "  latencyMillis=%d\n"
                        + "}",
                mStatusCode,
                mLatencyMillis);
    }

    /** Builder for {@link VmStartAttemptStats}. */
    public static class Builder {
        @VmStartStatus private int mStatusCode;

        private long mLatencyMillis;

        /** Constructs a new {@link Builder}. */
        public Builder() {
            mStatusCode = VM_START_STATUS_UNKNOWN;
            mLatencyMillis = 0;
        }

        /** Sets the status code of vm start result. */
        public @NonNull Builder setStatusCode(int statusCode) {
            mStatusCode = statusCode;
            return this;
        }

        /** Sets the overall latency of vm start attempt. */
        public @NonNull Builder setLatencyMillis(long latencyMillis) {
            mLatencyMillis = latencyMillis;
            return this;
        }

        /** Builds a new {@link VmStartAttemptStats} object. */
        public @NonNull VmStartAttemptStats build() {
            return new VmStartAttemptStats(/* builder= */ this);
        }
    }
}
