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

import android.arch.persistence.room.ColumnInfo;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;

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
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Constraints other = (Constraints) o;
        return mRequiredNetworkType == other.mRequiredNetworkType
                && mRequiresCharging == other.mRequiresCharging
                && mRequiresDeviceIdle == other.mRequiresDeviceIdle
                && mRequiresBatteryNotLow == other.mRequiresBatteryNotLow
                && mRequiresStorageNotLow == other.mRequiresStorageNotLow
                && mContentUriTriggers.equals(other.mContentUriTriggers);
    }

    @Override
    public int hashCode() {
        int result = mRequiredNetworkType.hashCode();
        result = 31 * result + (mRequiresCharging ? 1 : 0);
        result = 31 * result + (mRequiresDeviceIdle ? 1 : 0);
        result = 31 * result + (mRequiresBatteryNotLow ? 1 : 0);
        result = 31 * result + (mRequiresStorageNotLow ? 1 : 0);
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
        ContentUriTriggers mContentUriTriggers = new ContentUriTriggers();

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
         * Generates the {@link Constraints} from this Builder.
         *
         * @return The {@link Constraints} specified by this Builder
         */
        public @NonNull Constraints build() {
            return new Constraints(this);
        }
    }
}
