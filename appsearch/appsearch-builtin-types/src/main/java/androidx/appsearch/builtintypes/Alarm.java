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

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig;
import androidx.appsearch.utils.DateTimeFormatValidator;
import androidx.core.util.Preconditions;

import java.util.Calendar;

/**
 * AppSearch document representing an {@link Alarm} entity.
 */
@Document(name = "builtin:Alarm")
public final class Alarm {
    @Document.Namespace
    private final String mNamespace;

    @Document.Id
    private final String mId;

    @Document.Score
    private final int mDocumentScore;

    @Document.CreationTimestampMillis
    private final long mCreationTimestampMillis;

    @Document.TtlMillis
    private final long mDocumentTtlMillis;

    @Document.StringProperty(indexingType = StringPropertyConfig.INDEXING_TYPE_PREFIXES)
    private final String mName;

    @Document.BooleanProperty
    private final boolean mEnabled;

    @Document.LongProperty
    private final int[] mDaysOfWeek;

    @Document.LongProperty
    private final int mHour;

    @Document.LongProperty
    private final int mMinute;

    @Document.StringProperty
    private final String mBlackoutPeriodStartDate;

    @Document.StringProperty
    private final String mBlackoutPeriodEndDate;

    @Document.StringProperty
    private final String mRingtone;

    @Document.BooleanProperty
    private final boolean mShouldVibrate;

    @Document.DocumentProperty
    private final AlarmInstance mPreviousInstance;

    @Document.DocumentProperty
    private final AlarmInstance mNextInstance;

    Alarm(@NonNull String namespace, @NonNull String id, int documentScore,
            long creationTimestampMillis, long documentTtlMillis, @Nullable String name,
            boolean enabled, @Nullable int[] daysOfWeek, int hour, int minute,
            @Nullable String blackoutPeriodStartDate, @Nullable String blackoutPeriodEndDate,
            @Nullable String ringtone, boolean shouldVibrate,
            @Nullable AlarmInstance previousInstance, @Nullable AlarmInstance nextInstance) {
        mNamespace = Preconditions.checkNotNull(namespace);
        mId = Preconditions.checkNotNull(id);
        mDocumentScore = documentScore;
        mCreationTimestampMillis = creationTimestampMillis;
        mDocumentTtlMillis = documentTtlMillis;
        mName = name;
        mEnabled = enabled;
        mDaysOfWeek = daysOfWeek;
        mHour = hour;
        mMinute = minute;
        mBlackoutPeriodStartDate = blackoutPeriodStartDate;
        mBlackoutPeriodEndDate = blackoutPeriodEndDate;
        mRingtone = ringtone;
        mShouldVibrate = shouldVibrate;
        mPreviousInstance = previousInstance;
        mNextInstance = nextInstance;
    }

    /** Returns the namespace. */
    @NonNull
    public String getNamespace() {
        return mNamespace;
    }

    /** Returns the unique identifier. */
    @NonNull
    public String getId() {
        return mId;
    }

    /**
     * Returns the user-provided opaque document score of the current AppSearch document, which can
     * be used for ranking using
     * {@link androidx.appsearch.app.SearchSpec.RankingStrategy#RANKING_STRATEGY_DOCUMENT_SCORE}.
     *
     * <p>See {@link Document.Score} for more information on score.
     */
    public int getDocumentScore() {
        return mDocumentScore;
    }

    /**
     * Returns the creation timestamp for the current AppSearch entity, in milliseconds using the
     * {@link System#currentTimeMillis()} time base.
     *
     * <p>This timestamp refers to the creation time of the AppSearch entity, not when the
     * document is written into AppSearch.
     *
     * <p>If not set, then the current timestamp will be used.
     *
     * <p>See {@link androidx.appsearch.annotation.Document.CreationTimestampMillis} for more
     * information on creation timestamp.
     */
    public long getCreationTimestampMillis() {
        return mCreationTimestampMillis;
    }

    /**
     * Returns the time-to-live (TTL) for the current AppSearch document as a duration in
     * milliseconds.
     *
     * <p>The document will be automatically deleted when the TTL expires.
     *
     * <p>See {@link Document.TtlMillis} for more information on TTL.
     */
    public long getDocumentTtlMillis() {
        return mDocumentTtlMillis;
    }

    /** Returns the name. */
    @Nullable
    public String getName() {
        return mName;
    }

    /** Returns whether or not the {@link Alarm} is active. */
    public boolean isEnabled() {
        return mEnabled;
    }

    /**
     * Returns the scheduled days for repeating.
     *
     * <p>Days of the week can be {@link java.util.Calendar#MONDAY},
     * {@link java.util.Calendar#TUESDAY}, {@link java.util.Calendar#WEDNESDAY},
     * {@link java.util.Calendar#THURSDAY}, {@link java.util.Calendar#FRIDAY},
     * {@link java.util.Calendar#SATURDAY}, or {@link java.util.Calendar#SUNDAY}.
     *
     * <p>If null, or if the list is empty, then the {@link Alarm} does not repeat.
     */
    @Nullable
    public int[] getDaysOfWeek() {
        return mDaysOfWeek;
    }

    /**
     * Returns the hour the {@link Alarm} will fire.
     *
     * <p>Hours are specified by integers from 0 to 23.
     */
    @IntRange(from = 0, to = 23)
    public int getHour() {
        return mHour;
    }

    /**
     * Returns the minute the {@link Alarm} will fire.
     *
     * <p>Minutes are specified by integers from 0 to 59.
     */
    @IntRange(from = 0, to = 59)
    public int getMinute() {
        return mMinute;
    }

    /**
     * Returns the start date of the blackout period in ISO 8601 format.
     * E.g.: 2022-01-14
     *
     * <p>A blackout period means the {@link Alarm} will not fire during this period.
     *
     * <p>If not set, then it indicates that the blackout period has no start time.
     *
     * <p>If neither blackoutPeriodStartDate and blackoutPeriodEndDate are set, then
     * the blackout period is not defined.
     */
    @Nullable
    public String getBlackoutPeriodStartDate() {
        return mBlackoutPeriodStartDate;
    }

    /**
     * Returns the end time for the blackout period in ISO 8601 format.
     * E.g.: 2022-01-14
     *
     * <p>A blackout period means the {@link Alarm} will not fire during this period.
     *
     * <p>If not set, then it indicates that the blackout period has no end time.
     *
     * <p>If neither blackoutPeriodStartDate and blackoutPeriodEndDate are set, then
     * the blackout period is not defined.
     */
    @Nullable
    public String getBlackoutPeriodEndDate() {
        return mBlackoutPeriodEndDate;
    }

    /**
     * Returns the ringtone as a content URI to be played, or
     * {@link android.provider.AlarmClock#VALUE_RINGTONE_SILENT} if no ringtone will be played.
     */
    @Nullable
    public String getRingtone() {
        return mRingtone;
    }

    /** Returns whether or not to activate the device vibrator when the {@link Alarm} fires. */
    public boolean shouldVibrate() {
        return mShouldVibrate;
    }

    /**
     * Returns the previous {@link AlarmInstance}.
     *
     * <p>The previous {@link AlarmInstance} is most recent past instance that was fired. If
     * there are no past instances, then null will be returned.
     *
     * <p>See {@link AlarmInstance}.
     */
    @Nullable
    public AlarmInstance getPreviousInstance() {
        return mPreviousInstance;
    }

    /**
     * Returns the next {@link AlarmInstance}.
     *
     * <p>The next {@link AlarmInstance} is the immediate future instance that is scheduled to fire.
     * If there are no future instances, then null will be returned.
     *
     * <p>See {@link AlarmInstance}.
     */
    @Nullable
    public AlarmInstance getNextInstance() {
        return mNextInstance;
    }

    /** Builder for {@link Alarm}. */
    public static final class Builder extends BaseBuiltinTypeBuilder<Builder> {
        private String mName;
        private boolean mEnabled;
        private int[] mDaysOfWeek;
        private int mHour;
        private int mMinute;
        private String mBlackoutPeriodStartDate;
        private String mBlackoutPeriodEndDate;
        private String mRingtone;
        private boolean mShouldVibrate;
        private AlarmInstance mPreviousInstance;
        private AlarmInstance mNextInstance;

        /**
         * Constructor for {@link Alarm.Builder}.
         *
         * @param namespace Namespace for the Document. See
         * {@link Document.Namespace}.
         * @param id Unique identifier for the Document. See {@link Document.Id}.
         */
        public Builder(@NonNull String namespace, @NonNull String id) {
            super(namespace, id);
        }

        /**
         * Constructor with all the existing values.
         */
        public Builder(@NonNull Alarm alarm) {
            this(alarm.getNamespace(), alarm.getId());
            mDocumentScore = alarm.getDocumentScore();
            mCreationTimestampMillis = alarm.getCreationTimestampMillis();
            mDocumentTtlMillis = alarm.getDocumentTtlMillis();
            mName = alarm.getName();
            mEnabled = alarm.isEnabled();
            mDaysOfWeek = alarm.getDaysOfWeek();
            mHour = alarm.getHour();
            mMinute = alarm.getMinute();
            mBlackoutPeriodStartDate = alarm.getBlackoutPeriodStartDate();
            mBlackoutPeriodEndDate = alarm.getBlackoutPeriodEndDate();
            mRingtone = alarm.getRingtone();
            mShouldVibrate = alarm.shouldVibrate();
            mPreviousInstance = alarm.getPreviousInstance();
            mNextInstance = alarm.getNextInstance();
        }

        /** Sets the name. */
        @NonNull
        public Builder setName(@Nullable String name) {
            mName = name;
            return this;
        }

        /** Sets whether or not the {@link Alarm} is active. */
        @NonNull
        public Builder setEnabled(boolean enabled) {
            mEnabled = enabled;
            return this;
        }

        /**
         * Sets the scheduled days for repeating.
         *
         * <p>Days of the week can be {@link java.util.Calendar#MONDAY},
         * {@link java.util.Calendar#TUESDAY}, {@link java.util.Calendar#WEDNESDAY},
         * {@link java.util.Calendar#THURSDAY}, {@link java.util.Calendar#FRIDAY},
         * {@link java.util.Calendar#SATURDAY}, or {@link java.util.Calendar#SUNDAY}.
         *
         * <p>If not set, or if the list is empty, then the {@link Alarm} does not repeat.
         */
        @NonNull
        public Builder setDaysOfWeek(
                @Nullable
                @IntRange(from = Calendar.SUNDAY, to = Calendar.SATURDAY) int... daysOfWeek) {
            if (daysOfWeek != null) {
                for (int day : daysOfWeek) {
                    Preconditions.checkArgumentInRange(day, Calendar.SUNDAY, Calendar.SATURDAY,
                            "daysOfWeek");
                }
            }
            mDaysOfWeek = daysOfWeek;
            return this;
        }

        /**
         * Sets the hour the {@link Alarm} will fire.
         *
         * <p>Hours are specified by integers from 0 to 23.
         */
        @NonNull
        public Builder setHour(@IntRange(from = 0, to = 23) int hour) {
            mHour = Preconditions.checkArgumentInRange(hour, 0, 23, "hour");
            return this;
        }

        /**
         * Sets the minute the {@link Alarm} will fire.
         *
         * <p>Minutes are specified by integers from 0 to 59.
         */
        @NonNull
        public Builder setMinute(@IntRange(from = 0, to = 59) int minute) {
            mMinute = Preconditions.checkArgumentInRange(minute, 0, 59, "minute");
            return this;
        }

        /**
         * Sets the start date for the blackout period in ISO 8601 format.
         * E.g.: 2022-01-14
         *
         * <p>A blackout period means the {@link Alarm} will not fire during this period.
         *
         * <p>If not set, then it indicates that the blackout period has no start time.
         *
         * <p>If neither blackoutPeriodStartDate and blackoutPeriodEndDate are set, then
         * the blackout period is not defined.
         */
        @NonNull
        public Builder setBlackoutPeriodStartDate(
                @Nullable String blackoutPeriodStartDate) {
            if (blackoutPeriodStartDate != null) {
                Preconditions.checkArgument(
                        DateTimeFormatValidator.validateISO8601Date(blackoutPeriodStartDate),
                        "blackoutPeriodStartDate must be in the format: yyyy-MM-dd");
            }
            mBlackoutPeriodStartDate = blackoutPeriodStartDate;
            return this;
        }

        /**
         * Sets the end time for the blackout period in ISO 8601 format.
         * E.g.: 2022-01-14
         *
         * <p>A blackout period means the {@link Alarm} will not fire during this period.
         *
         * <p>If not set, then it indicates that the blackout period has no end time.
         *
         * <p>If neither blackoutPeriodStartDate and blackoutPeriodEndDate are set, then
         * the blackout period is not defined.
         */
        @NonNull
        public Builder setBlackoutPeriodEndDate(@Nullable String blackoutPeriodEndDate) {
            if (blackoutPeriodEndDate != null) {
                Preconditions.checkArgument(
                        DateTimeFormatValidator.validateISO8601Date(blackoutPeriodEndDate),
                        "blackoutPeriodEndDate must be in the format: yyyy-MM-dd");
            }
            mBlackoutPeriodEndDate = blackoutPeriodEndDate;
            return this;
        }

        /**
         * Sets the content URI for the ringtone to be played, or
         * {@link android.provider.AlarmClock#VALUE_RINGTONE_SILENT} if no ringtone will be played.
         */
        @NonNull
        public Builder setRingtone(@Nullable String ringtone) {
            mRingtone = ringtone;
            return this;
        }

        /** Sets whether or not to activate the device vibrator when the {@link Alarm} fires. */
        @NonNull
        public Builder setShouldVibrate(boolean shouldVibrate) {
            mShouldVibrate = shouldVibrate;
            return this;
        }

        /**
         * Sets the previous {@link AlarmInstance}.
         *
         * <p>The previous {@link AlarmInstance} is most recent past instance that was fired. If
         * not set, then there are no past instances.
         *
         * <p>See {@link AlarmInstance}.
         */
        @NonNull
        public Builder setPreviousInstance(@Nullable AlarmInstance previousInstance) {
            mPreviousInstance = previousInstance;
            return this;
        }

        /**
         * Sets the next {@link AlarmInstance}.
         *
         * <p>The next {@link AlarmInstance} is the immediate future instance that is scheduled
         * to fire. If not set, then there are no future instances.
         *
         * <p>See {@link AlarmInstance}.
         */
        @NonNull
        public Builder setNextInstance(@Nullable AlarmInstance nextInstance) {
            mNextInstance = nextInstance;
            return this;
        }

        /** Builds the {@link Alarm}. */
        @NonNull
        public Alarm build() {
            return new Alarm(mNamespace, mId, mDocumentScore, mCreationTimestampMillis,
                    mDocumentTtlMillis, mName, mEnabled, mDaysOfWeek, mHour, mMinute,
                    mBlackoutPeriodStartDate, mBlackoutPeriodEndDate, mRingtone,
                    mShouldVibrate, mPreviousInstance, mNextInstance);
        }
    }
}
