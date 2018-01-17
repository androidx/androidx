/*
 * Copyright 2017 The Android Open Source Project
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

package android.arch.background.workmanager;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.arch.persistence.room.ColumnInfo;
import android.net.Uri;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;

/**
 * The constraints that can be applied to one {@link BaseWork}.
 */
public class Constraints {
    @Retention(SOURCE)
    @IntDef({NETWORK_NOT_REQUIRED, NETWORK_CONNECTED, NETWORK_UNMETERED, NETWORK_NOT_ROAMING,
            NETWORK_METERED})
    public @interface NetworkType {
    }

    public static final Constraints NONE = new Constraints.Builder().build();

    public static final int NETWORK_NOT_REQUIRED = 0;
    public static final int NETWORK_CONNECTED = 1;
    public static final int NETWORK_UNMETERED = 2;
    public static final int NETWORK_NOT_ROAMING = 3;
    public static final int NETWORK_METERED = 4;

    @NetworkType
    @ColumnInfo(name = "required_network_type")
    int mRequiredNetworkType;

    @ColumnInfo(name = "requires_charging")
    boolean mRequiresCharging;

    @ColumnInfo(name = "requires_device_idle")
    boolean mRequiresDeviceIdle;

    @ColumnInfo(name = "requires_battery_not_low")
    boolean mRequiresBatteryNotLow;

    @ColumnInfo(name = "requires_storage_not_low")
    boolean mRequiresStorageNotLow;

    @ColumnInfo(name = "content_uri_triggers")
    ContentUriTriggers mContentUriTriggers;

    public Constraints() { // stub required for room
    }

    private Constraints(Builder builder) {
        mRequiresCharging = builder.mRequiresCharging;
        mRequiresDeviceIdle = builder.mRequiresDeviceIdle;
        mRequiredNetworkType = builder.mRequiredNetworkType;
        mRequiresBatteryNotLow = builder.mRequiresBatteryNotLow;
        mRequiresStorageNotLow = builder.mRequiresStorageNotLow;
        mContentUriTriggers = builder.mContentUriTriggers;
    }

    public int getRequiredNetworkType() {
        return mRequiredNetworkType;
    }

    public void setRequiredNetworkType(int requiredNetworkType) {
        mRequiredNetworkType = requiredNetworkType;
    }

    /**
     * @return If the constraints require charging.
     */
    public boolean requiresCharging() {
        return mRequiresCharging;
    }

    public void setRequiresCharging(boolean requiresCharging) {
        mRequiresCharging = requiresCharging;
    }

    /**
     * @return If the constraints require device idle.
     */
    public boolean requiresDeviceIdle() {
        return mRequiresDeviceIdle;
    }

    public void setRequiresDeviceIdle(boolean requiresDeviceIdle) {
        mRequiresDeviceIdle = requiresDeviceIdle;
    }

    /**
     * @return If the constraints require battery not low status.
     */
    public boolean requiresBatteryNotLow() {
        return mRequiresBatteryNotLow;
    }

    public void setRequiresBatteryNotLow(boolean requiresBatteryNotLow) {
        mRequiresBatteryNotLow = requiresBatteryNotLow;
    }

    /**
     * @return If the constraints require storage not low status.
     */
    public boolean requiresStorageNotLow() {
        return mRequiresStorageNotLow;
    }

    public void setRequiresStorageNotLow(boolean requiresStorageNotLow) {
        mRequiresStorageNotLow = requiresStorageNotLow;
    }

    public void setContentUriTriggers(ContentUriTriggers mContentUriTriggers) {
        this.mContentUriTriggers = mContentUriTriggers;
    }

    public ContentUriTriggers getContentUriTriggers() {
        return mContentUriTriggers;
    }

    /**
     * @return {@code true} if {@link ContentUriTriggers} is not empty
     */
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
                && (mContentUriTriggers != null ? mContentUriTriggers.equals(
                        other.mContentUriTriggers) : other.mContentUriTriggers == null);
    }

    @Override
    public int hashCode() {
        int result = mRequiredNetworkType;
        result = 31 * result + (mRequiresCharging ? 1 : 0);
        result = 31 * result + (mRequiresDeviceIdle ? 1 : 0);
        result = 31 * result + (mRequiresBatteryNotLow ? 1 : 0);
        result = 31 * result + (mRequiresStorageNotLow ? 1 : 0);
        result = 31 * result + (mContentUriTriggers != null ? mContentUriTriggers.hashCode() : 0);
        return result;
    }

    /**
     * Builder for {@link Constraints} class.
     */
    public static class Builder {
        private boolean mRequiresCharging = false;
        private boolean mRequiresDeviceIdle = false;
        private int mRequiredNetworkType = NETWORK_NOT_REQUIRED;
        private boolean mRequiresBatteryNotLow = false;
        private boolean mRequiresStorageNotLow = false;
        private ContentUriTriggers mContentUriTriggers = new ContentUriTriggers();

        /**
         * Specify whether device should be plugged in for {@link BaseWork} to run.
         * Default is false.
         *
         * @param requiresCharging true if device must be plugged in, false otherwise
         * @return current builder
         */
        public Builder setRequiresCharging(boolean requiresCharging) {
            this.mRequiresCharging = requiresCharging;
            return this;
        }

        /**
         * Specify whether device should be idle for {@link BaseWork} to run. Default is false.
         *
         * @param requiresDeviceIdle true if device must be idle, false otherwise
         * @return current builder
         */
        public Builder setRequiresDeviceIdle(boolean requiresDeviceIdle) {
            this.mRequiresDeviceIdle = requiresDeviceIdle;
            return this;
        }

        /**
         * Specify whether device should have a particular {@link NetworkType} for {@link BaseWork}
         * to run. Default is {@value #NETWORK_NOT_REQUIRED}
         *
         * @param networkType type of network required
         * @return current builder
         */
        public Builder setRequiredNetworkType(@NetworkType int networkType) {
            this.mRequiredNetworkType = networkType;
            return this;
        }

        /**
         * Specify whether device battery should not be below critical threshold for
         * {@link BaseWork} to run. Default is false.
         *
         * @param requiresBatteryNotLow true if battery should not be below critical threshold,
         *                              false otherwise
         * @return current builder
         */
        public Builder setRequiresBatteryNotLow(boolean requiresBatteryNotLow) {
            this.mRequiresBatteryNotLow = requiresBatteryNotLow;
            return this;
        }

        /**
         * Specify whether device available storage should not be below critical threshold for
         * {@link BaseWork} to run. Default is false.
         *
         * @param requiresStorageNotLow true if available storage should not be below critical
         *                              threshold, false otherwise
         * @return current builder
         */
        public Builder setRequiresStorageNotLow(boolean requiresStorageNotLow) {
            this.mRequiresStorageNotLow = requiresStorageNotLow;
            return this;
        }

        /**
         * Specify whether {@link BaseWork} should run when a content {@link android.net.Uri} is
         * updated
         * @param uri {@link android.net.Uri} to observe
         * @param triggerForDescendants {@code true} if any changes in descendants cause this
         *                              {@link BaseWork} to run
         * @return The current {@link Builder}
         */
        public Builder addContentUriTrigger(Uri uri, boolean triggerForDescendants) {
            mContentUriTriggers.add(uri, triggerForDescendants);
            return this;
        }

        /**
         * Generates the {@link Constraints} from this Builder.
         *
         * @return new {@link Constraints} which can be attached to a {@link BaseWork}
         */
        public Constraints build() {
            return new Constraints(this);
        }
    }
}
