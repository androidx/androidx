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

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.UiThread;
import androidx.wear.protolayout.proto.StateProto.State;
import androidx.wear.protolayout.renderer.common.Constants.UpdateRequestReason;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Logger used for collecting metrics. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface ProviderStatsLogger {

    /** Failures that doesn't cause the inflation to fail. */
    @IntDef({
        IGNORED_FAILURE_UNKNOWN,
        IGNORED_FAILURE_APPLY_MUTATION_EXCEPTION,
        IGNORED_FAILURE_ANIMATION_QUOTA_EXCEEDED,
        IGNORED_FAILURE_DIFFING_FAILURE
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface IgnoredFailure {}

    /** Unknown failure. */
    int IGNORED_FAILURE_UNKNOWN = 0;

    /** Failure applying the diff mutation. */
    int IGNORED_FAILURE_APPLY_MUTATION_EXCEPTION = 1;

    /** Failure caused by exceeding animation quota. */
    int IGNORED_FAILURE_ANIMATION_QUOTA_EXCEEDED = 2;

    /** Failure diffing the layout. */
    int IGNORED_FAILURE_DIFFING_FAILURE = 3;

    /** Failures that causes the inflation to fail. */
    @IntDef({
        INFLATION_FAILURE_REASON_UNKNOWN,
        INFLATION_FAILURE_REASON_LAYOUT_DEPTH_EXCEEDED,
        INFLATION_FAILURE_REASON_EXPRESSION_NODE_COUNT_EXCEEDED
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface InflationFailureReason {}

    /** Unknown failure. */
    int INFLATION_FAILURE_REASON_UNKNOWN = 0;

    /** Failure caused by exceeding maximum layout depth. */
    int INFLATION_FAILURE_REASON_LAYOUT_DEPTH_EXCEEDED = 1;

    /** Failure caused by exceeding maximum expression node count. */
    int INFLATION_FAILURE_REASON_EXPRESSION_NODE_COUNT_EXCEEDED = 2;

    /** Log the schema version of the received layout. */
    void logLayoutSchemaVersion(int major, int minor);

    /** Log Protolayout state structure. */
    void logStateStructure(@NonNull State state, boolean isInitialState);

    /** Log the occurrence of an ignored failure. */
    @UiThread
    void logIgnoredFailure(@IgnoredFailure int failure);

    /** Log the reason for inflation failure. */
    @UiThread
    void logInflationFailed(@InflationFailureReason int failureReason);

    /**
     * Creates an {@link InflaterStatsLogger} and marks the start of inflation. The atoms will be
     * logged to statsd only when {@link #logInflationFinished} is called.
     */
    @UiThread
    @NonNull
    InflaterStatsLogger createInflaterStatsLogger();

    /** Makes the end of inflation and log the inflation results. */
    @UiThread
    void logInflationFinished(@NonNull InflaterStatsLogger inflaterStatsLogger);

    /** Log tile request reason. */
    void logTileRequestReason(@UpdateRequestReason int updateRequestReason);

    /** Logger used for logging inflation stats. */
    interface InflaterStatsLogger {
        /** log the mutation changed nodes count for the ongoing inflation. */
        @UiThread
        void logMutationChangedNodes(int changedNodesCount);

        /** Log the total nodes count for the ongoing inflation. */
        @UiThread
        void logTotalNodeCount(int totalNodesCount);

        /**
         * Log the usage of a drawable. This method should be called between {@link
         * #createInflaterStatsLogger()} and {@link #logInflationFinished(InflaterStatsLogger)}.
         */
        @UiThread
        void logDrawableUsage(@NonNull Drawable drawable);

        /**
         * Log the occurrence of an ignored failure. The usage of this method is not restricted to
         * inflation start or end.
         */
        @UiThread
        void logIgnoredFailure(@IgnoredFailure int failure);

        /**
         * Log the reason for inflation failure. This will make any future call {@link
         * #logInflationFinished(InflaterStatsLogger)} a Noop.
         */
        @UiThread
        void logInflationFailed(@InflationFailureReason int failureReason);
    }
}
