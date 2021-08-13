/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.work;

import static androidx.work.NetworkType.NOT_REQUIRED;

import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.room.ColumnInfo;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * A specification of the requirements that need to be met before a {@link WorkRequest} can run.  By
 * default, WorkRequests do not have any requirements and can run immediately.  By adding
 * requirements, you can make sure that work only runs in certain situations - for example, when you
 * have an unmetered network and are charging.
 */

public final class Constraints {

    /**
     * Represents a Constraints object with no requirements.
     */
    public static final Constraints NONE = new Constraints.Builder().build();

    // NOTE: this is effectively a @NonNull, but changing the annotation would result in a really
    // annoying database migration that we can deal with later.
    @ColumnInfo(name = "required_network_type")
    private NetworkType mRequiredNetworkType = NOT_REQUIRED;

    @ColumnInfo(name = "requires_charging")
    private boolean mRequiresCharging;

    @ColumnInfo(name = "requires_device_idle")
    private boolean mRequiresDeviceIdle;

    @ColumnInfo(name = "requires_battery_not_low")
    private boolean mRequiresBatteryNotLow;

    @ColumnInfo(name = "requires_storage_not_low")
    private boolean mRequiresStorageNotLow;

    @ColumnInfo(name = "trigger_content_update_delay")
    private long mTriggerContentUpdateDelay = -1;

    @ColumnInfo(name = "trigger_max_content_delay")
    private long  mTriggerMaxContentDelay = -1;

    // NOTE: this is effectively a @NonNull, but changing the annotation would result in a really
    // annoying database migration that we can deal with later.
    @ColumnInfo(name = "content_uri_triggers")
    private ContentUriTriggers mContentUriTriggers = new ContentUriTriggers();

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public Constraints() { // stub required for room
    }

    Constraints(Builder builder) {
        mRequiresCharging = builder.mRequiresCharging;
        mRequiresDeviceIdle = Build.VERSION.SDK_INT >= 23 && builder.mRequiresDeviceIdle;
        mRequiredNetworkType = builder.mRequiredNetworkType;
        mRequiresBatteryNotLow = builder.mRequiresBatteryNotLow;
        mRequiresStorageNotLow = builder.mRequiresStorageNotLow;
        if (Build.VERSION.SDK_INT >= 24) {
            mContentUriTriggers = builder.mContentUriTriggers;
            mTriggerContentUpdateDelay = builder.mTriggerContentUpdateDelay;
            mTriggerMaxContentDelay = builder.mTriggerContentMaxDelay;
        }
    }

    public Constraints(@NonNull Constraints other) {
        mRequiresCharging = other.mRequiresCharging;
        mRequiresDeviceIdle = other.mRequiresDeviceIdle;
        mRequiredNetworkType = other.mRequiredNetworkType;
        mRequiresBatteryNotLow = other.mRequiresBatteryNotLow;
        mRequiresStorageNotLow = other.mRequiresStorageNotLow;
        mContentUriTriggers = other.mContentUriTriggers;
    }

    public @NonNull NetworkType getRequiredNetworkType() {
        return mRequiredNetworkType;
    }

    /**
     * Needed by Room.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void setRequiredNetworkType(@NonNull NetworkType requiredNetworkType) {
        mRequiredNetworkType = requiredNetworkType;
    }

    /**
     * @return {@code true} if the work should only execute while the device is charging
     */
    public boolean requiresCharging() {
        return mRequiresCharging;
    }

    /**
     * Needed by Room.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void setRequiresCharging(boolean requiresCharging) {
        mRequiresCharging = requiresCharging;
    }

    /**
     * @return {@code true} if the work should only execute while the device is idle
     */
    @RequiresApi(23)
    public boolean requiresDeviceIdle() {
        return mRequiresDeviceIdle;
    }

    /**
     * Needed by Room.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @RequiresApi(23)
    public void setRequiresDeviceIdle(boolean requiresDeviceIdle) {
        mRequiresDeviceIdle = requiresDeviceIdle;
    }

    /**
     * @return {@code true} if the work should only execute when the battery isn't low
     */
    public boolean requiresBatteryNotLow() {
        return mRequiresBatteryNotLow;
    }

    /**
     * Needed by Room.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void setRequiresBatteryNotLow(boolean requiresBatteryNotLow) {
        mRequiresBatteryNotLow = requiresBatteryNotLow;
    }

    /**
     * @return {@code true} if the work should only execute when the storage isn't low
     */
    public boolean requiresStorageNotLow() {
        return mRequiresStorageNotLow;
    }

    /**
     * Needed by Room.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void setRequiresStorageNotLow(boolean requiresStorageNotLow) {
        mRequiresStorageNotLow = requiresStorageNotLow;
    }

    /**
     * Needed by Room.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public long getTriggerContentUpdateDelay() {
        return mTriggerContentUpdateDelay;
    }

    /**
     * Needed by Room.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void setTriggerContentUpdateDelay(long triggerContentUpdateDelay) {
        mTriggerContentUpdateDelay = triggerContentUpdateDelay;
    }

    /**
     * Needed by Room.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public long getTriggerMaxContentDelay() {
        return mTriggerMaxContentDelay;
    }

    /**
     * Needed by Room.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void setTriggerMaxContentDelay(long triggerMaxContentDelay) {
        mTriggerMaxContentDelay = triggerMaxContentDelay;
    }

    /**
     * Needed by Room.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @RequiresApi(24)
    public void setContentUriTriggers(@Nullable ContentUriTriggers mContentUriTriggers) {
        this.mContentUriTriggers = mContentUriTriggers;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @RequiresApi(24)
    public @NonNull ContentUriTriggers getContentUriTriggers() {
        return mContentUriTriggers;
    }

    /**
     * @return {@code true} if {@link ContentUriTriggers} is not empty
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @RequiresApi(24)
    public boolean hasContentUriTriggers() {
        return mContentUriTriggers.size() > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Constraints that = (Constraints) o;

        if (mRequiresCharging != that.mRequiresCharging) return false;
        if (mRequiresDeviceIdle != that.mRequiresDeviceIdle) return false;
        if (mRequiresBatteryNotLow != that.mRequiresBatteryNotLow) return false;
        if (mRequiresStorageNotLow != that.mRequiresStorageNotLow) return false;
        if (mTriggerContentUpdateDelay != that.mTriggerContentUpdateDelay) return false;
        if (mTriggerMaxContentDelay != that.mTriggerMaxContentDelay) return false;
        if (mRequiredNetworkType != that.mRequiredNetworkType) return false;
        return mContentUriTriggers.equals(that.mContentUriTriggers);
    }

    @Override
    public int hashCode() {
        int result = mRequiredNetworkType.hashCode();
        result = 31 * result + (mRequiresCharging ? 1 : 0);
        result = 31 * result + (mRequiresDeviceIdle ? 1 : 0);
        result = 31 * result + (mRequiresBatteryNotLow ? 1 : 0);
        result = 31 * result + (mRequiresStorageNotLow ? 1 : 0);
        result = 31 * result + (int) (mTriggerContentUpdateDelay ^ (mTriggerContentUpdateDelay
                >>> 32));
        result = 31 * result + (int) (mTriggerMaxContentDelay ^ (mTriggerMaxContentDelay >>> 32));
        result = 31 * result + mContentUriTriggers.hashCode();
        return result;
    }

    /**
     * A Builder for a {@link Constraints} object.
     */
    public static final class Builder {
        boolean mRequiresCharging = false;
        boolean mRequiresDeviceIdle = false;
        NetworkType mRequiredNetworkType = NOT_REQUIRED;
        boolean mRequiresBatteryNotLow = false;
        boolean mRequiresStorageNotLow = false;
        // Same defaults as JobInfo
        long mTriggerContentUpdateDelay = -1;
        long mTriggerContentMaxDelay = -1;
        ContentUriTriggers mContentUriTriggers = new ContentUriTriggers();

        public Builder() {
            // default public constructor
        }

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public Builder(@NonNull Constraints constraints) {
            mRequiresCharging = constraints.requiresCharging();
            mRequiresDeviceIdle = Build.VERSION.SDK_INT >= 23 && constraints.requiresDeviceIdle();
            mRequiredNetworkType = constraints.getRequiredNetworkType();
            mRequiresBatteryNotLow = constraints.requiresBatteryNotLow();
            mRequiresStorageNotLow = constraints.requiresStorageNotLow();
            if (Build.VERSION.SDK_INT >= 24) {
                mTriggerContentUpdateDelay = constraints.getTriggerContentUpdateDelay();
                mTriggerContentMaxDelay = constraints.getTriggerMaxContentDelay();
                mContentUriTriggers = constraints.getContentUriTriggers();
            }
        }

        /**
         * Sets whether device should be charging for the {@link WorkRequest} to run.  The
         * default value is {@code false}.
         *
         * @param requiresCharging {@code true} if device must be charging for the work to run
         * @return The current {@link Builder}
         */
        public @NonNull Builder setRequiresCharging(boolean requiresCharging) {
            this.mRequiresCharging = requiresCharging;
            return this;
        }

        /**
         * Sets whether device should be idle for the {@link WorkRequest} to run.  The default
         * value is {@code false}.
         *
         * @param requiresDeviceIdle {@code true} if device must be idle for the work to run
         * @return The current {@link Builder}
         */
        @RequiresApi(23)
        public @NonNull Builder setRequiresDeviceIdle(boolean requiresDeviceIdle) {
            this.mRequiresDeviceIdle = requiresDeviceIdle;
            return this;
        }

        /**
         * Sets whether device should have a particular {@link NetworkType} for the
         * {@link WorkRequest} to run.  The default value is {@link NetworkType#NOT_REQUIRED}.
         *
         * @param networkType The type of network required for the work to run
         * @return The current {@link Builder}
         */
        public @NonNull Builder setRequiredNetworkType(@NonNull NetworkType networkType) {
            this.mRequiredNetworkType = networkType;
            return this;
        }

        /**
         * Sets whether device battery should be at an acceptable level for the
         * {@link WorkRequest} to run.  The default value is {@code false}.
         *
         * @param requiresBatteryNotLow {@code true} if the battery should be at an acceptable level
         *                              for the work to run
         * @return The current {@link Builder}
         */
        public @NonNull Builder setRequiresBatteryNotLow(boolean requiresBatteryNotLow) {
            this.mRequiresBatteryNotLow = requiresBatteryNotLow;
            return this;
        }

        /**
         * Sets whether the device's available storage should be at an acceptable level for the
         * {@link WorkRequest} to run.  The default value is {@code false}.
         *
         * @param requiresStorageNotLow {@code true} if the available storage should not be below a
         *                              a critical threshold for the work to run
         * @return The current {@link Builder}
         */
        public @NonNull Builder setRequiresStorageNotLow(boolean requiresStorageNotLow) {
            this.mRequiresStorageNotLow = requiresStorageNotLow;
            return this;
        }

        /**
         * Sets whether the {@link WorkRequest} should run when a local {@code content:} {@link Uri}
         * is updated.  This functionality is identical to the one found in {@code JobScheduler} and
         * is described in
         * {@code JobInfo.Builder#addTriggerContentUri(android.app.job.JobInfo.TriggerContentUri)}.
         *
         * @param uri The local {@code content:} Uri to observe
         * @param triggerForDescendants {@code true} if any changes in descendants cause this
         *                              {@link WorkRequest} to run
         * @return The current {@link Builder}
         */
        @RequiresApi(24)
        public @NonNull Builder addContentUriTrigger(
                @NonNull Uri uri,
                boolean triggerForDescendants) {
            mContentUriTriggers.add(uri, triggerForDescendants);
            return this;
        }

        /**
         * Sets the delay that is allowed from the time a {@code content:} {@link Uri}
         * change is detected to the time when the {@link WorkRequest} is scheduled.  If there are
         * more changes during this time, the delay will be reset to the start of the most recent
         * change. This functionality is identical to the one found in {@code JobScheduler} and
         * is described in {@code JobInfo.Builder#setTriggerContentUpdateDelay(long)}.
         *
         * @param duration The length of the delay in {@code timeUnit} units
         * @param timeUnit The units of time for {@code duration}
         * @return The current {@link Builder}
         */
        @RequiresApi(24)
        @NonNull
        public Builder setTriggerContentUpdateDelay(
                long duration,
                @NonNull TimeUnit timeUnit) {
            mTriggerContentUpdateDelay = timeUnit.toMillis(duration);
            return this;
        }

        /**
         * Sets the delay that is allowed from the time a {@code content:} {@link Uri} change
         * is detected to the time when the {@link WorkRequest} is scheduled.  If there are more
         * changes during this time, the delay will be reset to the start of the most recent change.
         * This functionality is identical to the one found in {@code JobScheduler} and
         * is described in {@code JobInfo.Builder#setTriggerContentUpdateDelay(long)}.
         *
         * @param duration The length of the delay
         * @return The current {@link Builder}
         */
        @RequiresApi(26)
        @NonNull
        public Builder setTriggerContentUpdateDelay(Duration duration) {
            mTriggerContentUpdateDelay = duration.toMillis();
            return this;
        }

        /**
         * Sets the maximum delay that is allowed from the first time a {@code content:}
         * {@link Uri} change is detected to the time when the {@link WorkRequest} is scheduled.
         * This functionality is identical to the one found in {@code JobScheduler} and
         * is described in {@code JobInfo.Builder#setTriggerContentMaxDelay(long)}.
         *
         * @param duration The length of the delay in {@code timeUnit} units
         * @param timeUnit The units of time for {@code duration}
         * @return The current {@link Builder}
         */
        @RequiresApi(24)
        @NonNull
        public Builder setTriggerContentMaxDelay(
                long duration,
                @NonNull TimeUnit timeUnit) {
            mTriggerContentMaxDelay = timeUnit.toMillis(duration);
            return this;
        }

        /**
         * Sets the maximum delay that is allowed from the first time a {@code content:} {@link Uri}
         * change is detected to the time when the {@link WorkRequest} is scheduled. This
         * functionality is identical to the one found in {@code JobScheduler} and is described
         * in {@code JobInfo.Builder#setTriggerContentMaxDelay(long)}.
         *
         * @param duration The length of the delay
         * @return The current {@link Builder}
         */
        @RequiresApi(26)
        @NonNull
        public Builder setTriggerContentMaxDelay(Duration duration) {
            mTriggerContentMaxDelay = duration.toMillis();
            return this;
        }

        /**
         * Generates the {@link Constraints} from this Builder.
         *
         * @return The {@link Constraints} specified by this Builder
         */
        public @NonNull Constraints build() {
            return new Constraints(this);
        }
    }
}
