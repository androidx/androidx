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
import java.util.ArrayList;
import java.util.List;

/**
 * Represents statistics for vm initialization. When initializing the vm, it is possible that there
 * are multiple vm start attempts (retries) due to some transient errors. Therefore, a single {@link
 * VmInitializationStats} may contain multiple {@link VmStartAttemptStats} objects.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class VmInitializationStats {
    /**
     * Vm initialization types.
     *
     * <p>Needs to be kept consistent with AppSearchVmInitializationStatsReported in
     * frameworks/proto_logging/stats/atoms/appsearch/appsearch_extension_atoms.proto
     */
    @IntDef(
            value = {
                    VM_INIT_TYPE_UNKNOWN,
                    VM_INIT_TYPE_BOOTING,
                    VM_INIT_TYPE_RECONNECTING,
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface VmInitType {}

    /** Represents an unknown type for vm initialization. */
    public static final int VM_INIT_TYPE_UNKNOWN = 0;

    /** Represents device booting for vm initialization. */
    public static final int VM_INIT_TYPE_BOOTING = 1;

    /** Represents reconnecting for vm initialization. */
    public static final int VM_INIT_TYPE_RECONNECTING = 2;

    /** The vm initialization type. */
    @VmInitType private final int mVmInitType;

    /** The list of all vm start attempt stats objects. */
    private final @NonNull List<VmStartAttemptStats> mVmStartAttemptsStats;

    /**
     * Constructs a new {@link VmInitializationStats} object using the provided builder.
     *
     * @param builder The builder containing the statistics.
     */
    VmInitializationStats(@NonNull Builder builder) {
        mVmInitType = builder.mVmInitType;
        mVmStartAttemptsStats = builder.mVmStartAttemptsStats;
    }

    /** Returns the vm initialization type. */
    @VmInitType
    public int getVmInitType() {
        return mVmInitType;
    }

    /** Returns the list of {@link VmStartAttemptStats} for this vm initialization. */
    public @NonNull List<VmStartAttemptStats> getVmStartAttemptsStats() {
        return mVmStartAttemptsStats;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("VmInitializationStats {\n")
                .append(String.format("  vmInitType=%d,\n", mVmInitType))
                .append("  vmStartAttemptsStats=[\n");
        for (int i = 0; i < mVmStartAttemptsStats.size(); i++) {
            sb.append("    ").append(
                    String.valueOf(mVmStartAttemptsStats.get(i)).replace("\n", "\n    ")).append(
                    ",\n");
        }
        sb.append("  ]\n").append("}");
        return sb.toString();
    }

    /** Builder for {@link VmInitializationStats}. */
    public static class Builder {
        @VmInitType final int mVmInitType;

        private @NonNull List<VmStartAttemptStats> mVmStartAttemptsStats = new ArrayList<>();

        private boolean mBuilt = false;

        /** Constructs a new {@link Builder} with the specified initialization type. */
        public Builder(@VmInitType int vmInitType) {
            mVmInitType = vmInitType;
        }

        /** Adds one {@link VmStartAttemptStats} object. */
        public @NonNull Builder addStartAttemptStats(
                @NonNull VmStartAttemptStats vmStartAttemptStats) {
            resetIfBuilt();
            mVmStartAttemptsStats.add(vmStartAttemptStats);
            return this;
        }

        /**
         * If built, make a copy of previous data for every field so that the builder can be reused.
         */
        private void resetIfBuilt() {
            if (mBuilt) {
                mVmStartAttemptsStats = new ArrayList<>(mVmStartAttemptsStats);
                mBuilt = false;
            }
        }

        /** Builds a new {@link VmInitializationStats} object. */
        public @NonNull VmInitializationStats build() {
            mBuilt = true;
            return new VmInitializationStats(/* builder= */ this);
        }
    }
}
