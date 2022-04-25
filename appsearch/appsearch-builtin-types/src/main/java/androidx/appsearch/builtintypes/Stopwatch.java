/*
 * Copyright 2022 The Android Open Source Project
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

import android.content.Context;
import android.os.Build;
import android.os.SystemClock;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig;
import androidx.appsearch.utils.BootCountUtil;
import androidx.core.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.List;

/**
 * An AppSearch document representing a {@link Stopwatch} entity.
 *
 * <p>A stopwatch is used to count time up, starting from 0, and can be paused and resumed at will.
 */
@Document(name = "builtin:Stopwatch")
public final class Stopwatch {
    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({STATUS_UNKNOWN, STATUS_RESET, STATUS_RUNNING, STATUS_PAUSED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Status {}

    /** The {@link Stopwatch} status is unknown */
    public static final int STATUS_UNKNOWN = 0;
    /** The {@link Stopwatch} is reset. */
    public static final int STATUS_RESET = 1;
    /** The {@link Stopwatch} is running. */
    public static final int STATUS_RUNNING = 2;
    /** The {@link Stopwatch} is paused. */
    public static final int STATUS_PAUSED = 3;

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

    @Document.LongProperty
    private final long mBaseTimeMillis;

    @Document.LongProperty
    private final long mBaseTimeMillisInElapsedRealtime;

    @Document.LongProperty
    private final int mBootCount;

    @Document.LongProperty
    private final int mStatus;

    @Document.LongProperty
    private final long mAccumulatedTimeMillis;

    @Document.DocumentProperty
    private final List<StopwatchLap> mLaps;

    Stopwatch(@NonNull String namespace, @NonNull String id, int documentScore,
            long creationTimestampMillis,
            long documentTtlMillis, @Nullable String name, long baseTimeMillis,
            long baseTimeMillisInElapsedRealtime, int bootCount, int status,
            long accumulatedTimeMillis, @NonNull List<StopwatchLap> laps) {
        mNamespace = Preconditions.checkNotNull(namespace);
        mId = Preconditions.checkNotNull(id);
        mDocumentScore = documentScore;
        mCreationTimestampMillis = creationTimestampMillis;
        mDocumentTtlMillis = documentTtlMillis;
        mName = name;
        mBaseTimeMillis = baseTimeMillis;
        mBaseTimeMillisInElapsedRealtime = baseTimeMillisInElapsedRealtime;
        mBootCount = bootCount;
        mStatus = status;
        mAccumulatedTimeMillis = accumulatedTimeMillis;
        mLaps = Preconditions.checkNotNull(laps);
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

    /**
     * Returns the point in time that the {@link Stopwatch} counts up from. In milliseconds using
     * the {@link System#currentTimeMillis()} time base.
     *
     * <p>Use {@link #calculateBaseTimeMillis(Context)} to get a more accurate base time that
     * accounts for the current boot count of the device.
     */
    public long getBaseTimeMillis() {
        return mBaseTimeMillis;
    }

    /**
     * Returns the point in time that the {@link Stopwatch} counts up from. In milliseconds using
     * the {@link android.os.SystemClock#elapsedRealtime()} time base.
     *
     * <p>ElapsedRealtime should only be used if the {@link #getBootCount()} matches the
     * bootCount of the current device.
     */
    public long getBaseTimeMillisInElapsedRealtime() {
        return mBaseTimeMillisInElapsedRealtime;
    }

    /**
     * Returns the boot count of the device when this document is last updated.
     *
     * <p>The boot count of the device can be accessed from Global Settings. See
     * {@link android.provider.Settings.Global#BOOT_COUNT}.
     *
     * <p>On older APIs where boot count is not available, this value should not be used.
     */
    public int getBootCount() {
        return mBootCount;
    }

    /**
     * Returns the current status.
     *
     * <p>Status can be {@link Stopwatch#STATUS_UNKNOWN}, {@link Stopwatch#STATUS_RESET},
     * {@link Stopwatch#STATUS_RUNNING}, or {@link Stopwatch#STATUS_PAUSED}.
     */
    @Status
    public int getStatus() {
        return mStatus;
    }

    /**
     * Returns the total duration in milliseconds accumulated by the {@link Stopwatch}.
     *
     * <p>Use this method to get the static accumulated time stored in the document. Use
     * {@link #calculateCurrentAccumulatedTimeMillis(Context)} to calculate the accumulated time
     * in real time.
     */
    public long getAccumulatedTimeMillis() {
        return mAccumulatedTimeMillis;
    }

    /** Returns all the {@link StopwatchLap} instances. */
    @NonNull
    public List<StopwatchLap> getLaps() {
        return mLaps;
    }

    /**
     * Calculates the base time in milliseconds using the {@link System#currentTimeMillis()} time
     * base.
     *
     * <p>If the boot count retrieved from the context matches {@link #getBootCount()}, then
     * {@link #getBaseTimeMillisInElapsedRealtime()} will be used to calculate the base time
     * in the {@link System#currentTimeMillis()} time base. Otherwise return
     * {@link #getBaseTimeMillis()}.
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public long calculateBaseTimeMillis(@NonNull Context context) {
        int currentBootCount = BootCountUtil.getCurrentBootCount(context);
        if (currentBootCount == -1 || currentBootCount != mBootCount) {
            // Boot count doesn't exist, or it doesn't match the current device boot count.
            // Therefore return the wall clock time since elapsed realtime is not valid.
            return mBaseTimeMillis;
        } else {
            // Boot count matches the current device boot count. Therefore calculate the wall
            // clock base time using elapsed realtime.
            long elapsedTime = SystemClock.elapsedRealtime() - mBaseTimeMillisInElapsedRealtime;
            return System.currentTimeMillis() - elapsedTime;
        }
    }

    /**
     * Calculates the current accumulated time in milliseconds.
     *
     * <p>Use this method to calculate the accumulated time in real time. Use
     * {@link #getAccumulatedTimeMillis()} to get the static accumulated time stored in the
     * document.
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public long calculateCurrentAccumulatedTimeMillis(@NonNull Context context) {
        if (mStatus == STATUS_PAUSED || mStatus == STATUS_RESET) {
            return mAccumulatedTimeMillis;
        }

        return System.currentTimeMillis() - calculateBaseTimeMillis(context)
                + mAccumulatedTimeMillis;
    }

    /** Builder for {@link Stopwatch}. */
    public static final class Builder extends BaseBuiltinTypeBuilder<Builder> {
        private String mName;
        private long mBaseTimeMillis;
        private long mBaseTimeMillisInElapsedRealtime;
        private int mBootCount;
        private int mStatus;
        private long mAccumulatedTimeMillis;
        private List<StopwatchLap> mLaps;

        /**
         * Constructor for {@link Stopwatch.Builder}.
         *
         * @param namespace Namespace for the Document. See {@link Document.Namespace}.
         * @param id Unique identifier for the Document. See {@link Document.Id}.
         */
        public Builder(@NonNull String namespace, @NonNull String id) {
            super(namespace, id);

            // Default empty laps
            mLaps = Collections.emptyList();
        }

        /**
         * Constructor for {@link Stopwatch.Builder} with all the existing values.
         */
        public Builder(@NonNull Stopwatch stopwatch) {
            this(stopwatch.getNamespace(), stopwatch.getId());

            mDocumentScore = stopwatch.getDocumentScore();
            mCreationTimestampMillis = stopwatch.getCreationTimestampMillis();
            mDocumentTtlMillis = stopwatch.getDocumentTtlMillis();
            mName = stopwatch.getName();
            mBaseTimeMillis = stopwatch.getBaseTimeMillis();
            mBaseTimeMillisInElapsedRealtime =
                    stopwatch.getBaseTimeMillisInElapsedRealtime();
            mBootCount = stopwatch.getBootCount();
            mStatus = stopwatch.getStatus();
            mAccumulatedTimeMillis = stopwatch.getAccumulatedTimeMillis();
            mLaps = stopwatch.getLaps();
        }

        /** Sets the name. */
        @NonNull
        public Builder setName(@Nullable String name) {
            mName = name;
            return this;
        }

        /**
         * Sets the point in time that the {@link Stopwatch} counts up from.
         *
         * <p>Base time should be sampled in both the {@link System#currentTimeMillis()} and
         * {@link android.os.SystemClock#elapsedRealtime()} time base. In addition, the boot
         * count of the device is needed to check if the
         * {@link android.os.SystemClock#elapsedRealtime()} time base is valid.
         *
         * @param baseTimeMillis The base time in milliseconds using the
         * {@link System#currentTimeMillis()} time base.
         * @param baseTimeMillisInElapsedRealtime The base time in milliseconds using the
         * {@link android.os.SystemClock#elapsedRealtime()} time base.
         * @param bootCount The current boot count of the device. See
         * {@link android.provider.Settings.Global#BOOT_COUNT}.
         */
        @NonNull
        public Builder setBaseTimeMillis(long baseTimeMillis,
                long baseTimeMillisInElapsedRealtime, int bootCount) {
            mBaseTimeMillis = baseTimeMillis;
            mBaseTimeMillisInElapsedRealtime = baseTimeMillisInElapsedRealtime;
            mBootCount = bootCount;
            return this;
        }

        /**
         * Sets the point in time that the {@link Stopwatch} counts up from.
         *
         * <p>See {@link #setBaseTimeMillis(long, long, int)}.
         *
         * @param context The app context used to fetch boot count.
         * @param baseTimeMillis The base time in milliseconds using the
         * {@link System#currentTimeMillis()} time base.
         * @param baseTimeMillisInElapsedRealtime The base time in milliseconds using the
         * {@link android.os.SystemClock#elapsedRealtime()} time base.
         */
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
        @NonNull
        public Builder setBaseTimeMillis(@NonNull Context context, long baseTimeMillis,
                long baseTimeMillisInElapsedRealtime) {
            int bootCount = BootCountUtil.getCurrentBootCount(context);
            return setBaseTimeMillis(baseTimeMillis, baseTimeMillisInElapsedRealtime, bootCount);
        }

        /**
         * Sets the current status.
         *
         * <p>Status can be {@link Stopwatch#STATUS_UNKNOWN}, {@link Stopwatch#STATUS_RESET},
         * {@link Stopwatch#STATUS_RUNNING}, or {@link Stopwatch#STATUS_PAUSED}.
         */
        @NonNull
        public Builder setStatus(@Status int status) {
            mStatus = Preconditions.checkArgumentInRange(status, STATUS_UNKNOWN, STATUS_PAUSED,
                    "status");
            return this;
        }

        /**
         * Sets the total duration in milliseconds accumulated by the {@link Stopwatch}.
         */
        @NonNull
        public Builder setAccumulatedTimeMillis(long accumulatedTimeMillis) {
            mAccumulatedTimeMillis = accumulatedTimeMillis;
            return this;
        }

        /** Sets all the {@link StopwatchLap} instances. */
        @NonNull
        public Builder setLaps(@NonNull List<StopwatchLap> laps) {
            mLaps = Preconditions.checkNotNull(laps);
            return this;
        }

        /** Builds the {@link Stopwatch}. */
        @NonNull
        public Stopwatch build() {
            return new Stopwatch(mNamespace, mId, mDocumentScore,
                    mCreationTimestampMillis, mDocumentTtlMillis, mName, mBaseTimeMillis,
                    mBaseTimeMillisInElapsedRealtime, mBootCount, mStatus,
                    mAccumulatedTimeMillis, mLaps);
        }
    }
}
