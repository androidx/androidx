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

package androidx.work.impl.model;

import static androidx.work.BackoffPolicy.EXPONENTIAL;
import static androidx.work.BackoffPolicy.LINEAR;
import static androidx.work.State.BLOCKED;
import static androidx.work.State.CANCELLED;
import static androidx.work.State.ENQUEUED;
import static androidx.work.State.FAILED;
import static androidx.work.State.RUNNING;
import static androidx.work.State.SUCCEEDED;

import android.arch.persistence.room.TypeConverter;

import androidx.work.BackoffPolicy;
import androidx.work.NetworkType;
import androidx.work.State;

/**
 * TypeConverters for enums.
 */

public class EnumTypeConverters {

    /**
     * Integer identifiers that map to {@link State}.
     */
    public interface StateIds {
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
     * TypeConverter for a State to an int.
     *
     * @param state The input State
     * @return The associated int constant
     */
    @TypeConverter
    public static int stateToInt(State state) {
        switch (state) {
            case ENQUEUED:
                return StateIds.ENQUEUED;

            case RUNNING:
                return StateIds.RUNNING;

            case SUCCEEDED:
                return StateIds.SUCCEEDED;

            case FAILED:
                return StateIds.FAILED;

            case BLOCKED:
                return StateIds.BLOCKED;

            case CANCELLED:
                return StateIds.CANCELLED;

            default:
                throw new IllegalArgumentException(
                        "Could not convert " + state + " to int");
        }
    }

    /**
     * TypeConverter for an int to a State.
     *
     * @param value The input integer
     * @return The associated State enum value
     */
    @TypeConverter
    public static State intToState(int value) {
        switch (value) {
            case StateIds.ENQUEUED:
                return ENQUEUED;

            case StateIds.RUNNING:
                return RUNNING;

            case StateIds.SUCCEEDED:
                return SUCCEEDED;

            case StateIds.FAILED:
                return FAILED;

            case StateIds.BLOCKED:
                return BLOCKED;

            case StateIds.CANCELLED:
                return CANCELLED;

            default:
                throw new IllegalArgumentException(
                        "Could not convert " + value + " to State");
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
