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

import android.os.SystemClock;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig;
import androidx.core.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * AppSearch document representing a Timer entity.
 */
@Document(name = "builtin:Timer")
public class Timer {
    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({STATUS_UNKNOWN, STATUS_STARTED, STATUS_PAUSED, STATUS_EXPIRED, STATUS_MISSED,
            STATUS_RESET})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Status {}

    /** The {@link Timer} is in an unknown error state. */
    public static final int STATUS_UNKNOWN = 0;
    /** The {@link Timer} is started. */
    public static final int STATUS_STARTED = 1;
    /** The {@link Timer} is paused. */
    public static final int STATUS_PAUSED = 2;
    /** The {@link Timer} is expired. */
    public static final int STATUS_EXPIRED = 3;
    /** The {@link Timer} is missed. */
    public static final int STATUS_MISSED = 4;
    /** The {@link Timer} is reset to its initial value. */
    public static final int STATUS_RESET = 5;

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

    @Document.LongProperty
    private final long mDurationMillis;

    @Document.LongProperty
    private final long mStartTimeMillis;

    @Document.LongProperty
    private final long mStartTimeMillisInElapsedRealtime;

    @Document.LongProperty
    private final long mRemainingTimeMillis;

    @Document.StringProperty
    private final String mRingtone;

    @Document.LongProperty
    private final int mStatus;

    @Document.BooleanProperty
    private final boolean mVibrate;

    Timer(String namespace, String id, int score, long creationTimestampMillis, long ttlMillis,
            String name, long durationMillis, long startTimeMillis,
            long startTimeMillisInElapsedRealtime, long remainingTimeMillis, String ringtone,
            int status, boolean vibrate) {
        mNamespace = namespace;
        mId = id;
        mScore = score;
        mCreationTimestampMillis = creationTimestampMillis;
        mTtlMillis = ttlMillis;
        mName = name;
        mDurationMillis = durationMillis;
        mStartTimeMillis = startTimeMillis;
        mStartTimeMillisInElapsedRealtime = startTimeMillisInElapsedRealtime;
        mRemainingTimeMillis = remainingTimeMillis;
        mRingtone = ringtone;
        mStatus = status;
        mVibrate = vibrate;
    }

    /** Returns the namespace of the {@link Timer}. */
    @NonNull
    public String getNamespace() {
        return mNamespace;
    }

    /** Returns the unique identifier of the {@link Timer}. */
    @NonNull
    public String getId() {
        return mId;
    }

    /**
     * Returns the user-provided opaque document score of the {@link Timer}, which can be used for
     * ranking using
     * {@link androidx.appsearch.app.SearchSpec.RankingStrategy#RANKING_STRATEGY_DOCUMENT_SCORE}
     */
    public int getScore() {
        return mScore;
    }

    /**
     * Returns the creation timestamp for the {@link Timer} document, in milliseconds using the
     * {@link System#currentTimeMillis()} time base.
     */
    public long getCreationTimestampMillis() {
        return mCreationTimestampMillis;
    }

    /**
     * Returns the time-to-live (TTL) for the {@link Timer} document in milliseconds using the
     * {@link System#currentTimeMillis()} time base.
     *
     * <p>The {@link Timer} document will be automatically deleted when the TTL expires.
     */
    public long getTtlMillis() {
        return mTtlMillis;
    }

    /** Returns the name associated with the {@link Timer}. */
    @Nullable
    public String getName() {
        return mName;
    }

    /**
     * Returns the total duration of the {@link Timer}, in milliseconds.
     */
    public long getDurationMillis() {
        return mDurationMillis;
    }

    /**
     * Returns the time at which the {@link Timer} was started in milliseconds using the
     * {@link System#currentTimeMillis()} time base.
     *
     *
     * <p>If the {@link Timer} is in a {@link Timer#STATUS_STARTED} state, then its expire time
     * can be calculated using:
     * <pre>{@code
     * long expireTime = timer.getStartTimeMillis + timer.getRemainingTimeMillis();
     * }</pre>
     *
     * <p>See {@link #getStartTimeMillisInElapsedRealtime()} to see how startTimeMillis and
     * startTimeMillisInElapsedRealtime should be used.
     */
    public long getStartTimeMillis() {
        return mStartTimeMillis;
    }

    /**
     * Returns the time at which the {@link Timer} was started in milliseconds using the
     * {@link android.os.SystemClock#elapsedRealtime()} time base, or -1 if not present.
     *
     * <p>If present, startTimeMillisInElapsedRealtime should be the preferred value used to do
     * accurate time keeping in {@link Timer}.
     *
     * <p>If not present, or if {@link SystemClock#elapsedRealtime()} is unreliable, for example
     * after a device reboot, or the {@link Timer} document is moved to a different device, then
     * startTimeMillis should be used instead for time keeping.
     *
     * <p>If the {@link Timer} is in a {@link Timer#STATUS_STARTED} state, then its expire time
     * can be calculated using:
     * <pre>{@code
     * long elapsedTime = SystemClock.elapsedRealtime() -
     *   timer.getStartTimeMillisInElapsedRealtime();
     * long expireTime = System.currentTimeMillis() + timer.getRemainingTimeMillis() - elapsedTime;
     * }</pre>
     */
    public long getStartTimeMillisInElapsedRealtime() {
        return mStartTimeMillisInElapsedRealtime;
    }

    /**
     * Returns the amount of time remaining when the {@link Timer} was started, paused or reset,
     * in milliseconds.
     *
     * <p>The current remaining time can also be calculate using either
     * {@link #getStartTimeMillis()} or {@link #getStartTimeMillisInElapsedRealtime()}:
     * <pre>{@code
     * long elapsedTime = System.currentTimeMillis() - timer.getStartTimeMillis();
     * long currentRemainingTime = timer.getRemainingTimeMillis() - elapsedTime;
     * }</pre>
     * <pre>{@code
     * long elapsedTime = SystemClock.elapsedRealtime() -
     *   timer.getStartTimeMillisInElapsedRealtime();
     * long currentRemainingTime = timer.getRemainingTimeMillis() - elapsedTime;
     * }</pre>
     */
    public long getRemainingTimeMillis() {
        return mRemainingTimeMillis;
    }

    /**
     * Returns the ringtone of the {@link Timer} as a content URI to be played, or
     * {@link android.provider.AlarmClock#VALUE_RINGTONE_SILENT} if no ringtone will be played.
     */
    @Nullable
    public String getRingtone() {
        return mRingtone;
    }

    /**
     * Returns the current status of the {@link Timer}.
     *
     * <p>Status can be {@link Timer#STATUS_UNKNOWN}, {@link Timer#STATUS_STARTED},
     * {@link Timer#STATUS_PAUSED}, {@link Timer#STATUS_EXPIRED}, {@link Timer#STATUS_MISSED}, or
     * {@link Timer#STATUS_RESET}.
     */
    @Status
    public int getStatus() {
        return mStatus;
    }

    /** Returns whether or not to activate the device vibrator when the {@link Timer} expires. */
    public boolean isVibrate() {
        return mVibrate;
    }

    /** Builder for {@link Timer}. */
    public static final class Builder {
        private final String mNamespace;
        private final String mId;
        private int mScore;
        private long mCreationTimestampMillis;
        private long mTtlMillis;
        private String mName;
        private long mDurationMillis;
        private long mStartTimeMillis;
        private long mStartTimeMillisInElapsedRealtime;
        private long mRemainingTimeMillis;
        private String mRingtone;
        private int mStatus;
        private boolean mVibrate;

        /**
         * Constructor for {@link Timer.Builder}.
         *
         * @param namespace Namespace for the {@link Timer} Document. See
         * {@link Document.Namespace}.
         * @param id Unique identifier for the {@link Timer} Document. See {@link Document.Id}.
         */
        public Builder(@NonNull String namespace, @NonNull String id) {
            mNamespace = Preconditions.checkNotNull(namespace);
            mId = Preconditions.checkNotNull(id);

            // Default for unset creationTimestampMillis. AppSearch will internally convert this
            // to current time when creating the GenericDocument.
            mCreationTimestampMillis = -1;
            // Default for unset startTimeMillisInElapsedRealtime
            mStartTimeMillisInElapsedRealtime = -1;
        }

        /**
         * Constructor for {@link Timer.Builder} with all the existing values of a {@link Timer}.
         */
        public Builder(@NonNull Timer timer) {
            this(timer.getNamespace(), timer.getId());
            mScore = timer.getScore();
            mCreationTimestampMillis = timer.getCreationTimestampMillis();
            mTtlMillis = timer.getTtlMillis();
            mName = timer.getName();
            mDurationMillis = timer.getDurationMillis();
            mStartTimeMillis = timer.getStartTimeMillis();
            mStartTimeMillisInElapsedRealtime = timer.getStartTimeMillisInElapsedRealtime();
            mRemainingTimeMillis = timer.getRemainingTimeMillis();
            mRingtone = timer.getRingtone();
            mStatus = timer.getStatus();
            mVibrate = timer.isVibrate();
        }

        /**
         * Sets the opaque document score of the {@link Timer}, which can be used for ranking using
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
         * Sets the creation timestamp of the {@link Timer} document, in milliseconds using the
         * {@link System#currentTimeMillis()} time base.
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
         * Sets the time-to-live (TTL) for the {@link Timer} document in milliseconds using the
         * {@link System#currentTimeMillis()} time base.
         *
         * <p>The {@link Timer} document will be automatically deleted when the TTL expires.
         *
         * <p>If not set, then the document will never expire.
         *
         * <p>See {@link Document.TtlMillis}
         */
        @NonNull
        public Builder setTtlMillis(long ttlMillis) {
            mTtlMillis = ttlMillis;
            return this;
        }

        /** Sets the name. */
        @NonNull
        public Builder setName(@Nullable String name) {
            mName = name;
            return this;
        }

        /**
         * Sets the total duration of the {@link Timer}, in milliseconds.
         */
        @NonNull
        public Builder setDurationMillis(long durationMillis) {
            mDurationMillis = durationMillis;
            return this;
        }

        /**
         * Sets the time at which the {@link Timer} was started in milliseconds using the
         * {@link System#currentTimeMillis()} time base.
         *
         * <p>See {@link #setStartTimeMillisInElapsedRealtime(long)} on how startTimeMillis and
         * startTimeMillisInElapsedRealtime should be used.
         */
        @NonNull
        public Builder setStartTimeMillis(long startTimeMillis) {
            mStartTimeMillis = startTimeMillis;
            return this;
        }

        /**
         * Sets the time at which the {@link Timer} was started in milliseconds using the
         * {@link android.os.SystemClock#elapsedRealtime()} time base.
         *
         * <p>startTimeMillis and startTimeMillisInElapsedRealtime should be sampled at
         * the same time, using {@link System#currentTimeMillis()} and
         * {@link android.os.SystemClock#elapsedRealtime()} respectively.
         *
         * <p>In situations where the reader cannot reliably use
         * {@link android.os.SystemClock#elapsedRealtime()}, for example if the reader is not on
         * the same device where the {@link Timer} document is written, then
         * startTimeMillisInElapsedRealtime should not be set.
         */
        @NonNull
        public Builder setStartTimeMillisInElapsedRealtime(long startTimeMillisInElapsedRealtime) {
            mStartTimeMillisInElapsedRealtime = startTimeMillisInElapsedRealtime;
            return this;
        }

        /**
         * Sets the amount of time remaining when the {@link Timer} was started, paused or reset,
         * in milliseconds.
         */
        @NonNull
        public Builder setRemainingTimeMillis(long remainingTimeMillis) {
            mRemainingTimeMillis = remainingTimeMillis;
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

        /**
         * Sets the current status of the {@link Timer}.
         *
         * <p>Status can be {@link Timer#STATUS_UNKNOWN}, {@link Timer#STATUS_STARTED},
         * {@link Timer#STATUS_PAUSED}, {@link Timer#STATUS_EXPIRED}, {@link Timer#STATUS_MISSED},
         * or {@link Timer#STATUS_RESET}.
         */
        @NonNull
        public Builder setStatus(@Status int status) {
            mStatus = status;
            return this;
        }

        /** Sets whether or not to activate the device vibrator when the {@link Timer} expires. */
        @NonNull
        public Builder setVibrate(boolean vibrate) {
            mVibrate = vibrate;
            return this;
        }

        /** Builds the {@link Timer}. */
        @NonNull
        public Timer build() {
            Preconditions.checkNotNull(mId);
            Preconditions.checkNotNull(mNamespace);

            return new Timer(mNamespace, mId, mScore, mCreationTimestampMillis, mTtlMillis, mName,
                    mDurationMillis, mStartTimeMillis, mStartTimeMillisInElapsedRealtime,
                    mRemainingTimeMillis, mRingtone, mStatus, mVibrate);
        }
    }
}
