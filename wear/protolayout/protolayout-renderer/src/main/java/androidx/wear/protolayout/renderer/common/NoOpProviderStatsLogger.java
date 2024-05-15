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

package androidx.wear.protolayout.renderer.common;

import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.wear.protolayout.proto.StateProto.State;
import androidx.wear.protolayout.renderer.common.Constants.UpdateRequestReason;

/** A No-Op implementation of {@link ProviderStatsLogger}. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class NoOpProviderStatsLogger implements ProviderStatsLogger {
    private static final String TAG = "NoOpProviderStatsLogger";

    /** Creates an instance of {@link NoOpProviderStatsLogger}. */
    public NoOpProviderStatsLogger(@NonNull String reason) {
        Log.i(TAG, "Instance used because " + reason);
    }

    /** No-op method. */
    @Override
    public void logLayoutSchemaVersion(int major, int minor) {}

    /** No-op method. */
    @Override
    public void logStateStructure(@NonNull State state, boolean isInitialState) {}

    /** No-op method. */
    @Override
    public void logIgnoredFailure(int failure) {}

    /** No-op method. */
    @Override
    public void logInflationFailed(@InflationFailureReason int failureReason) {}

    /** No-op method. */
    @Override
    @NonNull
    public InflaterStatsLogger createInflaterStatsLogger() {
        return new NoOpInflaterStatsLogger();
    }

    /** No-op method. */
    @Override
    public void logInflationFinished(@NonNull InflaterStatsLogger inflaterStatsLogger) {}

    /** No-op method. */
    @Override
    public void logTileRequestReason(@UpdateRequestReason int updateRequestReason) {}

    /** A No-Op implementation of {@link InflaterStatsLogger}. */
    public static class NoOpInflaterStatsLogger implements InflaterStatsLogger {

        private NoOpInflaterStatsLogger() {}

        @Override
        public void logMutationChangedNodes(int changedNodesCount) {}

        @Override
        public void logTotalNodeCount(int totalNodesCount) {}

        /** No-op method. */
        @Override
        public void logDrawableUsage(@NonNull Drawable drawable) {}

        /** No-op method. */
        @Override
        public void logIgnoredFailure(@IgnoredFailure int failure) {}

        /** No-op method. */
        @Override
        public void logInflationFailed(@InflationFailureReason int failureReason) {}
    }
}
