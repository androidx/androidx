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

package android.arch.background.workmanager.impl.model;

import static android.arch.background.workmanager.BackoffPolicy.EXPONENTIAL;
import static android.arch.background.workmanager.BackoffPolicy.LINEAR;
import static android.arch.background.workmanager.WorkStatus.BLOCKED;
import static android.arch.background.workmanager.WorkStatus.CANCELLED;
import static android.arch.background.workmanager.WorkStatus.ENQUEUED;
import static android.arch.background.workmanager.WorkStatus.FAILED;
import static android.arch.background.workmanager.WorkStatus.RUNNING;
import static android.arch.background.workmanager.WorkStatus.SUCCEEDED;

import android.arch.background.workmanager.BackoffPolicy;
import android.arch.background.workmanager.NetworkType;
import android.arch.background.workmanager.WorkStatus;
import android.arch.persistence.room.TypeConverter;

/**
 * TypeConverters for enums.
 */

public class EnumTypeConverters {

    /**
     * Integer identifiers that map to {@link WorkStatus}.
     */
    public interface StatusIds {
        int ENQUEUED = 0;
        int RUNNING = 1;
        int SUCCEEDED = 2;
        int FAILED = 3;
        int BLOCKED = 4;
        int CANCELLED = 5;
    }

    /**
     * Integer identifiers that map to {@link BackoffPolicy}.
     */
    public interface BackoffPolicyIds {
        int EXPONENTIAL = 0;
        int LINEAR = 1;
    }

    /**
     * Integer identifiers that map to {@link NetworkType}.
     */
    public interface NetworkTypeIds {
        int NOT_REQUIRED = 0;
        int CONNECTED = 1;
        int UNMETERED = 2;
        int NOT_ROAMING = 3;
        int METERED = 4;
    }

    /**
     * TypeConverter for a WorkStatus to an int.
     *
     * @param workStatus The input WorkStatus
     * @return The associated int constant
     */
    @TypeConverter
    public static int workStatusToInt(WorkStatus workStatus) {
        switch (workStatus) {
            case ENQUEUED:
                return StatusIds.ENQUEUED;

            case RUNNING:
                return StatusIds.RUNNING;

            case SUCCEEDED:
                return StatusIds.SUCCEEDED;

            case FAILED:
                return StatusIds.FAILED;

            case BLOCKED:
                return StatusIds.BLOCKED;

            case CANCELLED:
                return StatusIds.CANCELLED;

            default:
                throw new IllegalArgumentException(
                        "Could not convert " + workStatus + " to int");
        }
    }

    /**
     * TypeConverter for an int to a WorkStatus.
     *
     * @param value The input integer
     * @return The associated WorkStatus enum value
     */
    @TypeConverter
    public static WorkStatus intToWorkStatus(int value) {
        switch (value) {
            case StatusIds.ENQUEUED:
                return ENQUEUED;

            case StatusIds.RUNNING:
                return RUNNING;

            case StatusIds.SUCCEEDED:
                return SUCCEEDED;

            case StatusIds.FAILED:
                return FAILED;

            case StatusIds.BLOCKED:
                return BLOCKED;

            case StatusIds.CANCELLED:
                return CANCELLED;

            default:
                throw new IllegalArgumentException(
                        "Could not convert " + value + " to WorkStatus");
        }
    }

    /**
     * TypeConverter for a BackoffPolicy to an int.
     *
     * @param backoffPolicy The input BackoffPolicy
     * @return The associated int constant
     */
    @TypeConverter
    public static int backoffPolicyToInt(BackoffPolicy backoffPolicy) {
        switch (backoffPolicy) {
            case EXPONENTIAL:
                return BackoffPolicyIds.EXPONENTIAL;

            case LINEAR:
                return BackoffPolicyIds.LINEAR;

            default:
                throw new IllegalArgumentException(
                        "Could not convert " + backoffPolicy + " to int");
        }
    }

    /**
     * TypeConverter for an int to a BackoffPolicy.
     *
     * @param value The input integer
     * @return The associated BackoffPolicy enum value
     */
    @TypeConverter
    public static BackoffPolicy intToBackoffPolicy(int value) {
        switch (value) {
            case BackoffPolicyIds.EXPONENTIAL:
                return EXPONENTIAL;

            case BackoffPolicyIds.LINEAR:
                return LINEAR;

            default:
                throw new IllegalArgumentException(
                        "Could not convert " + value + " to BackoffPolicy");
        }
    }

    /**
     * TypeConverter for a NetworkType to an int.
     *
     * @param networkType The input NetworkType
     * @return The associated int constant
     */
    @TypeConverter
    public static int networkTypeToInt(NetworkType networkType) {
        switch (networkType) {
            case NOT_REQUIRED:
                return NetworkTypeIds.NOT_REQUIRED;

            case CONNECTED:
                return NetworkTypeIds.CONNECTED;

            case UNMETERED:
                return NetworkTypeIds.UNMETERED;

            case NOT_ROAMING:
                return NetworkTypeIds.NOT_ROAMING;

            case METERED:
                return NetworkTypeIds.METERED;

            default:
                throw new IllegalArgumentException(
                        "Could not convert " + networkType + " to int");
        }
    }

    /**
     * TypeConverter for an int to a NetworkType.
     *
     * @param value The input integer
     * @return The associated NetworkType enum value
     */
    @TypeConverter
    public static NetworkType intToNetworkType(int value) {
        switch (value) {
            case NetworkTypeIds.NOT_REQUIRED:
                return NetworkType.NOT_REQUIRED;

            case NetworkTypeIds.CONNECTED:
                return NetworkType.CONNECTED;

            case NetworkTypeIds.UNMETERED:
                return NetworkType.UNMETERED;

            case NetworkTypeIds.NOT_ROAMING:
                return NetworkType.NOT_ROAMING;

            case NetworkTypeIds.METERED:
                return NetworkType.METERED;

            default:
                throw new IllegalArgumentException(
                        "Could not convert " + value + " to NetworkType");
        }
    }

    private EnumTypeConverters() {
    }
}
