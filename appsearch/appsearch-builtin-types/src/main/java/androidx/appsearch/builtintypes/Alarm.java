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
import androidx.core.util.Preconditions;

import java.util.Calendar;

/**
 * AppSearch document representing an Alarm entity.
 */
@Document(name = "builtin:Alarm")
public class Alarm {
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

    @Document.LongProperty
    private final long mBlackoutStartTimeMillis;

    @Document.LongProperty
    private final long mBlackoutEndTimeMillis;

    @Document.StringProperty
    private final String mRingtone;

    @Document.BooleanProperty
    private final boolean mVibrate;

    @Document.DocumentProperty
    private final AlarmInstance mPreviousInstance;

    @Document.DocumentProperty
    private final AlarmInstance mNextInstance;

    Alarm(String namespace, String id, int score, long creationTimestampMillis, long ttlMillis,
            String name, boolean enabled, int[] daysOfWeek, int hour, int minute,
            long blackoutStartTimeMillis, long blackoutEndTimeMillis, String ringtone,
            boolean vibrate, AlarmInstance previousInstance, AlarmInstance nextInstance) {
        mNamespace = namespace;
        mId = id;
        mScore = score;
        mCreationTimestampMillis = creationTimestampMillis;
        mTtlMillis = ttlMillis;
        mName = name;
        mEnabled = enabled;
        mDaysOfWeek = daysOfWeek;
        mHour = hour;
        mMinute = minute;
        mBlackoutStartTimeMillis = blackoutStartTimeMillis;
        mBlackoutEndTimeMillis = blackoutEndTimeMillis;
        mRingtone = ringtone;
        mVibrate = vibrate;
        mPreviousInstance = previousInstance;
        mNextInstance = nextInstance;
    }

    /** Returns the namespace of the {@link Alarm}. */
    @NonNull
    public String getNamespace() {
        return mNamespace;
    }

    /** Returns the unique identifier of the {@link Alarm}. */
    @NonNull
    public String getId() {
        return mId;
    }

    /**
     * Returns the user-provided opaque document score of the {@link Alarm}, which can be
     * used for ranking using
     * {@link androidx.appsearch.app.SearchSpec.RankingStrategy#RANKING_STRATEGY_DOCUMENT_SCORE}.
     */
    public int getScore() {
        return mScore;
    }

    /**
     * Returns the creation timestamp for the {@link Alarm} document, in milliseconds using the
     * {@link System#currentTimeMillis()} time base.
     */
    public long getCreationTimestampMillis() {
        return mCreationTimestampMillis;
    }

    /**
     * Returns the time-to-live (TTL) for the {@link Alarm} document in milliseconds using the
     * {@link System#currentTimeMillis()} time base.
     *
     * <p>The {@link Alarm} document will be automatically deleted when the TTL expires.
     */
    public long getTtlMillis() {
        return mTtlMillis;
    }

    /** Returns the name associated with the {@link Alarm}. */
    @Nullable
    public String getName() {
        return mName;
    }

    /** Returns whether or not the {@link Alarm} is active. */
    public boolean isEnabled() {
        return mEnabled;
    }

    /**
     * Returns the scheduled days for repeating the {@link Alarm}.
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
     * Returns the start time for the {@link Alarm} blackout period in milliseconds using the
     * {@link System#currentTimeMillis()} time base.
     *
     * <p>A blackout period means the {@link Alarm} will not fire during this period.
     *
     * <p>The value {@code 0} indicates that the blackout period has no start time.
     *
     * <p>If both blackoutStartTime and blackoutEndTime are {@code 0}, then the blackout period
     * is not defined for this {@link Alarm}.
     */
    public long getBlackoutStartTimeMillis() {
        return mBlackoutStartTimeMillis;
    }

    /**
     * Returns the end time for the {@link Alarm} blackout period in milliseconds using the
     * {@link System#currentTimeMillis()} time base.
     *
     * <p>A blackout period means the {@link Alarm} will not fire during this period.
     *
     * <p>The value {@code 0} indicates that the blackout period has no end time.
     *
     * <p>If both blackoutStartTime and blackoutEndTime are {@code 0}, then the blackout period
     * is not defined for this {@link Alarm}.
     */
    public long getBlackoutEndTimeMillis() {
        return mBlackoutEndTimeMillis;
    }

    /**
     * Returns the ringtone of the {@link Alarm} as a content URI to be played, or
     * {@link android.provider.AlarmClock#VALUE_RINGTONE_SILENT} if no ringtone will be played.
     */
    @Nullable
    public String getRingtone() {
        return mRingtone;
    }

    /** Returns whether or not to activate the device vibrator when the {@link Alarm} fires. */
    public boolean isVibrate() {
        return mVibrate;
    }

    /**
     * Returns the previous {@link AlarmInstance} associated with the {@link Alarm}.
     *
     * <p>The previous {@link AlarmInstance} is most recent past instance that was fired. If the
     * {@link Alarm} has no past instances, then null will be returned.
     *
     * <p>See {@link AlarmInstance}.
     */
    @Nullable
    public AlarmInstance getPreviousInstance() {
        return mPreviousInstance;
    }

    /**
     * Returns the next {@link AlarmInstance} associated with the {@link Alarm}.
     *
     * <p>The next {@link AlarmInstance} is the immediate future instance that is scheduled to fire.
     * If the {@link Alarm} has no future instances, then null will be returned.
     *
     * <p>See {@link AlarmInstance}.
     */
    @Nullable
    public AlarmInstance getNextInstance() {
        return mNextInstance;
    }

    /** Builder for {@link Alarm}. */
    public static final class Builder {
        private final String mNamespace;
        private final String mId;
        private int mScore;
        private long mCreationTimestampMillis;
        private long mTtlMillis;
        private String mName;
        private boolean mEnabled;
        private int[] mDaysOfWeek;
        private int mHour;
        private int mMinute;
        private long mBlackoutStartTimeMillis;
        private long mBlackoutEndTimeMillis;
        private String mRingtone;
        private boolean mVibrate;
        private AlarmInstance mPreviousInstance;
        private AlarmInstance mNextInstance;

        /**
         * Constructor for {@link Alarm.Builder}.
         *
         * @param namespace Namespace for the {@link Alarm} Document. See
         * {@link Document.Namespace}.
         * @param id Unique identifier for the {@link Alarm} Document. See {@link Document.Id}.
         */
        public Builder(@NonNull String namespace, @NonNull String id) {
            mNamespace = Preconditions.checkNotNull(namespace);
            mId = Preconditions.checkNotNull(id);

            // Default for unset creationTimestampMillis. AppSearch will internally convert this
            // to current time when creating the GenericDocument.
            mCreationTimestampMillis = -1;
        }

        /**
         * Constructor for {@link Alarm.Builder} with all the existing values of an {@link Alarm}.
         */
        public Builder(@NonNull Alarm alarm) {
            this(alarm.getNamespace(), alarm.getId());
            mScore = alarm.getScore();
            mCreationTimestampMillis = alarm.getCreationTimestampMillis();
            mTtlMillis = alarm.getTtlMillis();
            mName = alarm.getName();
            mEnabled = alarm.isEnabled();
            mDaysOfWeek = alarm.getDaysOfWeek();
            mHour = alarm.getHour();
            mMinute = alarm.getMinute();
            mBlackoutStartTimeMillis = alarm.getBlackoutStartTimeMillis();
            mBlackoutEndTimeMillis = alarm.getBlackoutEndTimeMillis();
            mRingtone = alarm.getRingtone();
            mVibrate = alarm.isVibrate();
            mPreviousInstance = alarm.getPreviousInstance();
            mNextInstance = alarm.getNextInstance();
        }

        /**
         * Sets the opaque document score of the {@link Alarm}, which can be used for
         * ranking using
         * {@link androidx.appsearch.app.SearchSpec.RankingStrategy#RANKING_STRATEGY_DOCUMENT_SCORE}
         *
         * <p>See {@link Document.Score}
         */
        @NonNull
        public Alarm.Builder setScore(int score) {
            mScore = score;
            return this;
        }

        /**
         * Sets the Creation Timestamp of the {@link Alarm} document, in milliseconds using the
         * {@link System#currentTimeMillis()} time base.
         *
         * <p>If not set, then the current timestamp will be used.
         *
         * <p>See {@link Document.CreationTimestampMillis}
         */
        @NonNull
        public Alarm.Builder setCreationTimestampMillis(long creationTimestampMillis) {
            mCreationTimestampMillis = creationTimestampMillis;
            return this;
        }

        /**
         * Sets the time-to-live (TTL) for the {@link Alarm} document in milliseconds using the
         * {@link System#currentTimeMillis()} time base.
         *
         * <p>The {@link Alarm} document will be automatically deleted when the TTL expires.
         *
         * <p>If set to 0, then the document will never expire.
         *
         * <p>See {@link Document.TtlMillis}
         */
        @NonNull
        public Alarm.Builder setTtlMillis(long ttlMillis) {
            mTtlMillis = ttlMillis;
            return this;
        }

        /** Sets the name of the {@link Alarm}. */
        @NonNull
        public Alarm.Builder setName(@Nullable String name) {
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
         * Sets the scheduled days for repeating the {@link Alarm}.
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
         * Sets the start time for the {@link Alarm} blackout period in milliseconds using the
         * {@link System#currentTimeMillis()} time base.
         *
         * <p>A blackout period means the {@link Alarm} will not fire during this period.
         *
         * <p>If not set, or set to 0, then the blackout period has no start time.
         *
         * <p>If neither blackoutStartTime nor blackoutEndTime are set, or if they are both set
         * to 0, then the {@link Alarm} has no blackout period.
         */
        @NonNull
        public Builder setBlackoutStartTimeMillis(long blackoutStartTimeMillis) {
            mBlackoutStartTimeMillis = blackoutStartTimeMillis;
            return this;
        }

        /**
         * Sets the end time for the {@link Alarm} blackout period in milliseconds using the
         * {@link System#currentTimeMillis()} time base.
         *
         * <p>A blackout period means the {@link Alarm} will not fire during this period.
         *
         * <p>If not set, or set to 0, then the blackout period has no end time.
         *
         * <p>If neither blackoutStartTime nor blackoutEndTime are set, or if they are both set
         * to 0, then the {@link Alarm} has no blackout period.
         */
        @NonNull
        public Builder setBlackoutEndTimeMillis(long blackoutEndTimeMillis) {
            mBlackoutEndTimeMillis = blackoutEndTimeMillis;
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
        public Builder setVibrate(boolean vibrate) {
            mVibrate = vibrate;
            return this;
        }

        /**
         * Sets the previous {@link AlarmInstance} associated with the {@link Alarm}.
         *
         * <p>The previous {@link AlarmInstance} is most recent past instance that was fired. If
         * not set, then the {@link Alarm} has no past instances.
         *
         * <p>See {@link AlarmInstance}.
         */
        @NonNull
        public Builder setPreviousInstance(@Nullable AlarmInstance previousInstance) {
            mPreviousInstance = previousInstance;
            return this;
        }

        /**
         * Sets the next {@link AlarmInstance} associated with the {@link Alarm}.
         *
         * <p>The next {@link AlarmInstance} is the immediate future instance that is scheduled
         * to fire. If not set, then the {@link Alarm} has no future instances.
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
            Preconditions.checkNotNull(mId);
            Preconditions.checkNotNull(mNamespace);

            return new Alarm(mNamespace, mId, mScore, mCreationTimestampMillis, mTtlMillis, mName,
                    mEnabled, mDaysOfWeek, mHour, mMinute, mBlackoutStartTimeMillis,
                    mBlackoutEndTimeMillis, mRingtone, mVibrate, mPreviousInstance, mNextInstance);
        }
    }
}
