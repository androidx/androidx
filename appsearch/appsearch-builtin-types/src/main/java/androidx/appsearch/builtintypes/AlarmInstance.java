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
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.Document;
import androidx.core.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Calendar;

/**
 * AppSearch document representing an AlarmInstance entity.
 *
 * <p>An {@link AlarmInstance} must be associated with an {@link Alarm}. It represents a
 * particular point in time for that Alarm. For example, if an Alarm is set to
 * repeat every Monday, then each AlarmInstance for it will be the exact Mondays that the Alarm
 * did trigger.
 *
 * <p>Year, month, day, hour, and minute are used over timestamp to ensure the
 * {@link AlarmInstance} remains unchanged across timezones. E.g. An AlarmInstance set to fire at
 * 7am GMT should also fire at 7am when the timezone is changed to PST.
 */
@Document(name = "builtin:AlarmInstance")
public class AlarmInstance {
    /** @hide */
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

    @Document.Namespace
    private final String mNamespace;

    @Document.Id
    private final String mId;

    @Document.Score
    private final int mScore;

    @Document.CreationTimestampMillis
    private final long mCreationTimestampMillis;

    @Document.TtlMillis
    private final long mTtlMillis;

    @Document.LongProperty
    private final int mYear;

    @Document.LongProperty
    private final int mMonth;

    @Document.LongProperty
    private final int mDay;

    @Document.LongProperty
    private final int mHour;

    @Document.LongProperty
    private final int mMinute;

    @Document.LongProperty
    private final int mStatus;

    @Document.LongProperty
    private final long mSnoozeDurationMillis;

    AlarmInstance(String namespace, String id, int score, long creationTimestampMillis,
            long ttlMillis, int year, int month, int day, int hour, int minute, int status,
            long snoozeDurationMillis) {
        mNamespace = namespace;
        mId = id;
        mScore = score;
        mCreationTimestampMillis = creationTimestampMillis;
        mTtlMillis = ttlMillis;
        mYear = year;
        mMonth = month;
        mDay = day;
        mHour = hour;
        mMinute = minute;
        mStatus = status;
        mSnoozeDurationMillis = snoozeDurationMillis;
    }

    /** Returns the namespace of the {@link AlarmInstance}. */
    @NonNull
    public String getNamespace() {
        return mNamespace;
    }

    /** Returns the unique identifier of the {@link AlarmInstance}. */
    @NonNull
    public String getId() {
        return mId;
    }

    /**
     * Returns the user-provided opaque document score of the {@link AlarmInstance}, which can be
     * used for ranking using
     * {@link androidx.appsearch.app.SearchSpec.RankingStrategy#RANKING_STRATEGY_DOCUMENT_SCORE}.
     */
    public int getScore() {
        return mScore;
    }

    /**
     * Returns the creation timestamp for the {@link AlarmInstance} document, in milliseconds
     * using the {@link System#currentTimeMillis()} time base.
     */
    public long getCreationTimestampMillis() {
        return mCreationTimestampMillis;
    }

    /**
     * Returns the time-to-live (TTL) for the {@link AlarmInstance} document in milliseconds using
     * the {@link System#currentTimeMillis()} time base.
     *
     * <p>The {@link AlarmInstance} document will be automatically deleted when the TTL expires.
     */
    public long getTtlMillis() {
        return mTtlMillis;
    }

    /** Returns the year {@link AlarmInstance} is scheduled to fire. */
    public int getYear() {
        return mYear;
    }

    /**
     * Returns the month {@link AlarmInstance} is scheduled to fire.
     *
     * <p>Month should range from {@link java.util.Calendar#JANUARY} to
     * {@link java.util.Calendar#DECEMBER}.
     */
    @IntRange(from = Calendar.JANUARY, to = Calendar.DECEMBER)
    public int getMonth() {
        return mMonth;
    }

    /**
     * Returns the day of the month {@link AlarmInstance} is scheduled to fire.
     *
     * <p>Days are specified by integers from 1 to 31.
     */
    @IntRange(from = 1, to = 31)
    public int getDay() {
        return mDay;
    }

    /**
     * Returns the hour {@link AlarmInstance} is scheduled to fire.
     *
     * <p>Hours are specified by integers from 0 to 23.
     */
    @IntRange(from = 0, to = 23)
    public int getHour() {
        return mHour;
    }

    /**
     * Returns the minute {@link AlarmInstance} is scheduled to fire.
     *
     * <p>Minutes are specified by integers from 0 to 59.
     */
    @IntRange(from = 0, to = 59)
    public int getMinute() {
        return mMinute;
    }

    /**
     * Returns the current status of the {@link AlarmInstance}.
     *
     * <p>Status can be either {@link AlarmInstance#STATUS_UNKNOWN},
     * {@link AlarmInstance#STATUS_SCHEDULED}, {@link AlarmInstance#STATUS_FIRING},
     * {@link AlarmInstance#STATUS_DISMISSED}, {@link AlarmInstance#STATUS_SNOOZED}, or
     * {@link AlarmInstance#STATUS_MISSED}.
     */
    @Status
    public int getStatus() {
        return mStatus;
    }

    /**
     * Returns the length of time in milliseconds the {@link AlarmInstance} will remain snoozed
     * before it fires again, or -1 if the {@link AlarmInstance} does not support snoozing.
     */
    public long getSnoozeDurationMillis() {
        return mSnoozeDurationMillis;
    }

    /** Builder for {@link AlarmInstance}. */
    public static final class Builder {
        private final String mNamespace;
        private final String mId;
        private int mScore;
        private long mCreationTimestampMillis;
        private long mTtlMillis;
        private int mYear;
        private int mMonth;
        private int mDay;
        private int mHour;
        private int mMinute;
        private int mStatus;
        private long mSnoozeDurationMillis;

        /**
         * Constructor for {@link AlarmInstance.Builder}.
         *
         * @param namespace Namespace for the {@link AlarmInstance} Document. See
         * {@link Document.Namespace}.
         * @param id Unique identifier for the {@link AlarmInstance} Document. See
         * {@link Document.Id}.
         */
        public Builder(@NonNull String namespace, @NonNull String id) {
            mNamespace = Preconditions.checkNotNull(namespace);
            mId = Preconditions.checkNotNull(id);

            // Default for unset creationTimestampMillis. AppSearch will internally convert this
            // to current time when creating the GenericDocument.
            mCreationTimestampMillis = -1;
            // default for snooze length. Indicates no snoozing.
            mSnoozeDurationMillis = -1;
        }

        /**
         * Constructor for {@link AlarmInstance.Builder} with all the existing values of an
         * {@link AlarmInstance}.
         */
        public Builder(@NonNull AlarmInstance alarmInstance) {
            this(alarmInstance.getNamespace(), alarmInstance.getId());
            mScore = alarmInstance.getScore();
            mCreationTimestampMillis = alarmInstance.getCreationTimestampMillis();
            mTtlMillis = alarmInstance.getTtlMillis();
            mYear = alarmInstance.getYear();
            mMonth = alarmInstance.getMonth();
            mDay = alarmInstance.getDay();
            mHour = alarmInstance.getHour();
            mMinute = alarmInstance.getMinute();
            mStatus = alarmInstance.getStatus();
            mSnoozeDurationMillis = alarmInstance.getSnoozeDurationMillis();
        }

        /**
         * Sets the opaque document score of the {@link AlarmInstance}, which can be used for
         * ranking using
         * {@link androidx.appsearch.app.SearchSpec.RankingStrategy#RANKING_STRATEGY_DOCUMENT_SCORE}
         *
         * <p>See {@link Document.Score}
         */
        @NonNull
        public Builder setScore(int score) {
            mScore = score;
            return this;
        }

        /**
         * Sets the Creation Timestamp of the {@link AlarmInstance} document, in milliseconds
         * using the {@link System#currentTimeMillis()} time base.
         *
         * <p>If not set, then the current timestamp will be used.
         *
         * <p>See {@link Document.CreationTimestampMillis}
         */
        @NonNull
        public Builder setCreationTimestampMillis(long creationTimestampMillis) {
            mCreationTimestampMillis = creationTimestampMillis;
            return this;
        }

        /**
         * Sets the time-to-live (TTL) for the {@link AlarmInstance} document in milliseconds using
         * the {@link System#currentTimeMillis()} time base.
         *
         * <p>The {@link AlarmInstance} document will be automatically deleted when the TTL expires.
         *
         * <p>If set to 0, then the document will never expire.
         *
         * <p>See {@link Document.TtlMillis}
         */
        @NonNull
        public Builder setTtlMillis(long ttlMillis) {
            mTtlMillis = ttlMillis;
            return this;
        }

        /** Sets the year {@link AlarmInstance} is scheduled to fire. */
        @NonNull
        public Builder setYear(int year) {
            mYear = year;
            return this;
        }

        /**
         * Sets the month {@link AlarmInstance} is scheduled to fire.
         *
         * <p>Month should range from {@link java.util.Calendar#JANUARY} to
         * {@link java.util.Calendar#DECEMBER}.
         */
        @NonNull
        public Builder setMonth(
                @IntRange(from = Calendar.JANUARY, to = Calendar.DECEMBER) int month) {
            mMonth = Preconditions.checkArgumentInRange(month, Calendar.JANUARY,
                    Calendar.DECEMBER, "month");
            return this;
        }

        /**
         * Sets the day of the month {@link AlarmInstance} is scheduled to fire.
         *
         * <p>Days are specified by integers from 1 to 31.
         */
        @NonNull
        public Builder setDay(@IntRange(from = 1, to = 31) int day) {
            mDay = Preconditions.checkArgumentInRange(day, 1, 31, "day");
            return this;
        }

        /**
         * Sets the hour {@link AlarmInstance} is scheduled to fire.
         *
         * <p>Hours are specified by integers from 0 to 23.
         */
        @NonNull
        public Builder setHour(@IntRange(from = 0, to = 23) int hour) {
            mHour = Preconditions.checkArgumentInRange(hour, 0, 23, "hour");
            return this;
        }

        /**
         * Sets the minute {@link AlarmInstance} is scheduled to fire.
         *
         * <p>Minutes are specified by integers from 0 to 59.
         */
        @NonNull
        public Builder setMinute(@IntRange(from = 0, to = 59) int minute) {
            mMinute = Preconditions.checkArgumentInRange(minute, 0, 59, "minute");
            return this;
        }

        /**
         * Sets the current status of the {@link AlarmInstance}.
         *
         * <p>Status can be either {@link AlarmInstance#STATUS_UNKNOWN},
         * {@link AlarmInstance#STATUS_SCHEDULED}, {@link AlarmInstance#STATUS_FIRING},
         * {@link AlarmInstance#STATUS_DISMISSED}, {@link AlarmInstance#STATUS_SNOOZED}, or
         * {@link AlarmInstance#STATUS_MISSED}.
         */
        @NonNull
        public Builder setStatus(@Status int status) {
            mStatus = status;
            return this;
        }

        /**
         * Sets the length of time in milliseconds the {@link AlarmInstance} will remain snoozed
         * before it fires again.
         *
         * <p>If not set, or set to -1, then the {@link AlarmInstance} does not support snoozing.
         */
        @NonNull
        public Builder setSnoozeDurationMillis(long snoozeDurationMillis) {
            mSnoozeDurationMillis = snoozeDurationMillis;
            return this;
        }

        /** Builds the {@link AlarmInstance}. */
        @NonNull
        public AlarmInstance build() {
            Preconditions.checkNotNull(mId);
            Preconditions.checkNotNull(mNamespace);

            return new AlarmInstance(mNamespace, mId, mScore, mCreationTimestampMillis, mTtlMillis,
                    mYear, mMonth, mDay, mHour, mMinute, mStatus, mSnoozeDurationMillis);
        }
    }
}
