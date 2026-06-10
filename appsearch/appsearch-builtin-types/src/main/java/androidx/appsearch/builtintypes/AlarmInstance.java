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

package androidx.appsearch.builtintypes;

import androidx.annotation.IntDef;
import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.utils.DateTimeFormatValidator;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * AppSearch document representing an {@link AlarmInstance} entity.
 *
 * <p>An {@link AlarmInstance} must be associated with an {@link Alarm}. It represents a
 * particular point in time for that Alarm. For example, if an Alarm is set to
 * repeat every Monday, then each {@link AlarmInstance} for it will be the exact Mondays that the
 * Alarm did trigger.
 *
 * <p>The scheduled time should be timezone independent so that it remains unchanged across
 * timezones. E.g.: An {@link AlarmInstance} set to fire at 7am GMT should also fire at 7am when
 * the timezone is changed to PST.
 */
@Document(name = "builtin:AlarmInstance")
public class AlarmInstance extends Thing {
    /** @exportToFramework:hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({STATUS_UNKNOWN, STATUS_SCHEDULED, STATUS_FIRING, STATUS_DISMISSED, STATUS_SNOOZED,
            STATUS_MISSED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Status {}

    /** The {@link AlarmInstance} is in an unknown error state. */
    public static final int STATUS_UNKNOWN = 0;
    /** The {@link AlarmInstance} is scheduled to fire at some point in the future. */
    public static final int STATUS_SCHEDULED = 1;
    /** The {@link AlarmInstance} is firing. */
    public static final int STATUS_FIRING = 2;
    /** The {@link AlarmInstance} has been dismissed. */
    public static final int STATUS_DISMISSED = 3;
    /** The {@link AlarmInstance} has been snoozed. */
    public static final int STATUS_SNOOZED = 4;
    /** The {@link AlarmInstance} has been missed. */
    public static final int STATUS_MISSED = 5;

    @Document.StringProperty
    private final String mScheduledTime;

    @Document.LongProperty
    private final int mStatus;

    @Document.LongProperty
    private final long mSnoozeDurationMillis;

    /**
     * Constructor for {@link AlarmInstance}.
     *
     * @param builder The builder to construct the {@link AlarmInstance} from.
     */
    @ExperimentalAppSearchApi
    public AlarmInstance(@NonNull BuilderBase<?> builder) {
        super(builder);
        mScheduledTime = builder.mScheduledTime;
        mStatus = builder.mStatus;
        mSnoozeDurationMillis = builder.mSnoozeDurationMillis;
    }

    /**
     * Returns the time that this is expected to fire in ISO 8601 format.
     * E.g.: 2022-01-14T00:00:00
     *
     * <p>The scheduledTime is a timezone independent date time. When used, it should be
     * converted into a date time in the local timezone.
     */
    public @NonNull String getScheduledTime() {
        return mScheduledTime;
    }

    /**
     * Returns the current status.
     *
     * <p>Status can be either {@link #STATUS_UNKNOWN}, {@link #STATUS_SCHEDULED},
     * {@link #STATUS_FIRING}, {@link #STATUS_DISMISSED}, {@link #STATUS_SNOOZED}, or
     * {@link #STATUS_MISSED}.
     */
    @Status
    public int getStatus() {
        return mStatus;
    }

    /**
     * Returns the length of time in milliseconds the {@link AlarmInstance} will remain snoozed
     * before it fires again, or -1 if it does not support snoozing.
     */
    public long getSnoozeDurationMillis() {
        return mSnoozeDurationMillis;
    }

    /** Builder for {@link AlarmInstance}. */
    @Document.BuilderProducer
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    public static final class Builder extends BuilderBase<Builder> {
        /**
         * Constructor for {@link AlarmInstance.Builder}.
         *
         * @param namespace Namespace for the Document. See {@link Document.Namespace}.
         * @param id Unique identifier for the Document. See {@link Document.Id}.
         * @param scheduledTime The time that this is expected to fire in
         *                      ISO 8601 format. E.g.: 2022-01-14T00:00:00. Scheduled time should
         *                      be timezone independent.
         */
        public Builder(@NonNull String namespace, @NonNull String id,
                @NonNull String scheduledTime) {
            super(namespace, id, scheduledTime);
        }

        /**
         * Constructor for {@link AlarmInstance.Builder} with all the existing values.
         */
        public Builder(@NonNull AlarmInstance alarmInstance) {
            super(alarmInstance);
        }
    }

    @SuppressWarnings("unchecked")
    @ExperimentalAppSearchApi
    public static class BuilderBase<T extends AlarmInstance.BuilderBase<T>> extends
            Thing.BuilderBase<T> {
        private final String mScheduledTime;
        private int mStatus;
        private long mSnoozeDurationMillis;

        /**
         * Constructor for {@link AlarmInstance.BuilderBase}.
         *
         * @param namespace Namespace for the Document. See {@link Document.Namespace}.
         * @param id Unique identifier for the Document. See {@link Document.Id}.
         * @param scheduledTime The time that this is expected to fire in
         *                      ISO 8601 format. E.g.: 2022-01-14T00:00:00. Scheduled time should
         *                      be timezone independent.
         */
        public BuilderBase(@NonNull String namespace, @NonNull String id,
                @NonNull String scheduledTime) {
            super(namespace, id);
            Preconditions.checkNotNull(scheduledTime);
            Preconditions.checkArgument(
                    DateTimeFormatValidator.validateISO8601DateTime(scheduledTime),
                    "scheduledTime must be in the format: yyyy-MM-ddTHH:mm:ss");

            mScheduledTime = scheduledTime;

            // default for snooze length. Indicates no snoozing.
            mSnoozeDurationMillis = -1;
        }

        /**
         * Constructor for {@link AlarmInstance.BuilderBase} with all the existing values.
         *
         * @param alarmInstance The existing {@link AlarmInstance} to copy values from.
         */
        public BuilderBase(@NonNull AlarmInstance alarmInstance) {
            super(alarmInstance);
            mScheduledTime = alarmInstance.getScheduledTime();
            mStatus = alarmInstance.getStatus();
            mSnoozeDurationMillis = alarmInstance.getSnoozeDurationMillis();
        }

        /**
         * Sets the current status.
         *
         * <p>Status can be either {@link #STATUS_UNKNOWN}, {@link #STATUS_SCHEDULED},
         * {@link #STATUS_FIRING}, {@link #STATUS_DISMISSED}, {@link #STATUS_SNOOZED}, or
         * {@link #STATUS_MISSED}.
         */
        public @NonNull T setStatus(@Status int status) {
            mStatus = status;
            return (T) this;
        }

        /**
         * Sets the length of time in milliseconds the {@link AlarmInstance} will remain snoozed
         * before it fires again.
         *
         * <p>If not set, or set to -1, then it does not support snoozing.
         */
        public @NonNull T setSnoozeDurationMillis(long snoozeDurationMillis) {
            mSnoozeDurationMillis = snoozeDurationMillis;
            return (T) this;
        }

        /** Builds the {@link AlarmInstance}. */
        @Override
        public @NonNull AlarmInstance build() {
            return new AlarmInstance(this);
        }
    }
}
