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
@Document
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

    @Document.TtlMillis
    private final long mTtlMillis;

    @Document.StringProperty
    private final String mRingtone;

    @Document.StringProperty(indexingType = StringPropertyConfig.INDEXING_TYPE_PREFIXES)
    private final String mName;

    @Document.LongProperty
    private final long mDurationMillis;

    @Document.BooleanProperty
    private final boolean mVibrate;

    @Document.LongProperty
    private final long mRemainingTimeMillis;

    @Document.LongProperty
    private final int mTimerStatus;

    @Document.LongProperty
    private final long mExpireTimeMillis;

    Timer(String namespace, String id, int score, long ttlMillis, String ringtone,
            String name, long durationMillis, boolean vibrate, long remainingTimeMillis,
            int timerStatus, long expireTimeMillis) {
        mNamespace = namespace;
        mId = id;
        mScore = score;
        mTtlMillis = ttlMillis;
        mRingtone = ringtone;
        mName = name;
        mDurationMillis = durationMillis;
        mVibrate = vibrate;
        mRemainingTimeMillis = remainingTimeMillis;
        mTimerStatus = timerStatus;
        mExpireTimeMillis = expireTimeMillis;
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
     * Returns the TTL for the {@link Timer} document in milliseconds using the
     * {@link System#currentTimeMillis()} time base.
     *
     * <p>The {@link Timer} document will be automatically deleted when the TTL expires.
     */
    public long getTtlMillis() {
        return mTtlMillis;
    }

    /**
     * Returns the ringtone of the {@link Timer} as a content URI to be played, or
     * {@link android.provider.AlarmClock#VALUE_RINGTONE_SILENT} if no ringtone will be played.
     */
    @Nullable
    public String getRingtone() {
        return mRingtone;
    }

    /** Returns the name associated with the {@link Timer}. */
    @Nullable
    public String getName() {
        return mName;
    }

    /**
     * Returns the total duration of the {@link Timer} when it was first created, in milliseconds
     * using the {@link System#currentTimeMillis()} time base.
     */
    public long getDurationMillis() {
        return mDurationMillis;
    }

    /** Returns whether or not to activate the device vibrator when the {@link Timer} expires. */
    public boolean isVibrate() {
        return mVibrate;
    }

    /**
     * Returns the amount of time remaining when the {@link Timer} was started or paused, in
     * milliseconds using the {@link System#currentTimeMillis()} time base.
     */
    public long getRemainingTimeMillis() {
        return mRemainingTimeMillis;
    }

    /**
     * Returns the current status of the {@link Timer}.
     *
     * <p>Status can be {@link Timer#STATUS_UNKNOWN}, {@link Timer#STATUS_STARTED},
     * {@link Timer#STATUS_PAUSED}, {@link Timer#STATUS_EXPIRED}, {@link Timer#STATUS_MISSED}, or
     * {@link Timer#STATUS_RESET}.
     */
    @Status
    public int getTimerStatus() {
        return mTimerStatus;
    }

    /**
     * Returns the time at which the {@link Timer} will, or did expire in milliseconds since
     * epoch.
     *
     * <p>Unlike {@link Timer#getTtlMillis()}, the {@link Timer} document will not be
     * automatically deleted when the expire time is reached.
     */
    public long getExpireTimeMillis() {
        return mExpireTimeMillis;
    }

    /** Builder for {@link Timer}. */
    public static final class Builder {
        private final String mNamespace;
        private final String mId;
        private int mScore;
        private long mTtlMillis;
        private String mRingtone;
        private String mName;
        private long mDurationMillis;
        private boolean mVibrate;
        private long mRemainingTimeMillis;
        private int mTimerStatus;
        private long mExpireTimeMillis;

        /**
         * Constructor for {@link Timer.Builder}.
         *
         * @param id Unique identifier for the {@link Timer} Document. See {@link Document.Id}.
         * @param namespace Namespace for the {@link Timer} Document. See
         * {@link Document.Namespace}.
         */
        public Builder(@NonNull String namespace, @NonNull String id) {
            mNamespace = Preconditions.checkNotNull(namespace);
            mId = Preconditions.checkNotNull(id);
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
         * Sets the TTL for the {@link Timer} document in milliseconds using the
         * {@link System#currentTimeMillis()} time base.
         *
         * <p>The {@link Timer} document will be automatically deleted when the TTL expires.
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

        /**
         * Sets the content URI for the ringtone to be played, or
         * {@link android.provider.AlarmClock#VALUE_RINGTONE_SILENT} if no ringtone will be played.
         */
        @NonNull
        public Builder setRingtone(@Nullable String ringtone) {
            mRingtone = ringtone;
            return this;
        }

        /** Sets the name. */
        @NonNull
        public Builder setName(@Nullable String name) {
            mName = name;
            return this;
        }

        /**
         * Sets the total duration of the {@link Timer} when it was first created in milliseconds
         * using the {@link System#currentTimeMillis()} time base.
         */
        @NonNull
        public Builder setDurationMillis(long durationMillis) {
            mDurationMillis = durationMillis;
            return this;
        }

        /** Sets whether or not to activate the device vibrator when the {@link Timer} expires. */
        @NonNull
        public Builder setVibrate(boolean vibrate) {
            mVibrate = vibrate;
            return this;
        }

        /**
         * Sets the amount of time remaining when the {@link Timer} was started or paused, in
         * milliseconds using the {@link System#currentTimeMillis()} time base.
         */
        @NonNull
        public Builder setRemainingTimeMillis(long remainingTimeMillis) {
            mRemainingTimeMillis = remainingTimeMillis;
            return this;
        }

        /**
         * Sets the current status of the {@link Timer}.
         *
         * @param timerStatus Can be {@link Timer#STATUS_UNKNOWN}, {@link Timer#STATUS_STARTED},
         * {@link Timer#STATUS_PAUSED}, {@link Timer#STATUS_EXPIRED}, {@link Timer#STATUS_MISSED}
         *                    , or {@link Timer#STATUS_RESET}.
         */
        @NonNull
        public Builder setTimerStatus(@Status int timerStatus) {
            mTimerStatus = timerStatus;
            return this;
        }

        /**
         * Sets the time at which the {@link Timer} will, or did expire in milliseconds since epoch.
         *
         * <p>If set to 0, then the {@link Timer} will never expire.
         *
         * <p>Unlike {@link Builder#setTtlMillis(long)}, the {@link Timer} document will not be
         * automatically deleted when the expire time is reached.
         */
        @NonNull
        public Builder setExpireTimeMillis(long expireTimeMillis) {
            mExpireTimeMillis = expireTimeMillis;
            return this;
        }

        /** Builds the {@link Timer}. */
        @NonNull
        public Timer build() {
            Preconditions.checkNotNull(mId);
            Preconditions.checkNotNull(mNamespace);

            return new Timer(mNamespace, mId, mScore, mTtlMillis, mRingtone, mName,
                    mDurationMillis, mVibrate, mRemainingTimeMillis, mTimerStatus,
                    mExpireTimeMillis);
        }
    }
}
