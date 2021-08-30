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
import static androidx.work.WorkInfo.State.BLOCKED;
import static androidx.work.WorkInfo.State.CANCELLED;
import static androidx.work.WorkInfo.State.ENQUEUED;
import static androidx.work.WorkInfo.State.FAILED;
import static androidx.work.WorkInfo.State.RUNNING;
import static androidx.work.WorkInfo.State.SUCCEEDED;

import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.room.TypeConverter;
import androidx.work.BackoffPolicy;
import androidx.work.ContentUriTriggers;
import androidx.work.NetworkType;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkInfo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * TypeConverters for WorkManager enums and classes.
 */

public class WorkTypeConverters {

    /**
     * Integer identifiers that map to {@link WorkInfo.State}.
     */
    public interface StateIds {
        int ENQUEUED = 0;
        int RUNNING = 1;
        int SUCCEEDED = 2;
        int FAILED = 3;
        int BLOCKED = 4;
        int CANCELLED = 5;

        String COMPLETED_STATES = "(" + SUCCEEDED + ", " + FAILED + ", " + CANCELLED + ")";
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
        int TEMPORARILY_UNMETERED = 5;
    }

    /**
     * Integer identifiers that map to {@link OutOfQuotaPolicy}.
     */
    public interface OutOfPolicyIds {
        int RUN_AS_NON_EXPEDITED_WORK_REQUEST = 0;
        int DROP_WORK_REQUEST = 1;
    }

    /**
     * TypeConverter for a State to an int.
     *
     * @param state The input State
     * @return The associated int constant
     */
    @TypeConverter
    public static int stateToInt(WorkInfo.State state) {
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
    public static WorkInfo.State intToState(int value) {
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
                if (Build.VERSION.SDK_INT >= 30
                        && networkType == NetworkType.TEMPORARILY_UNMETERED) {
                    return NetworkTypeIds.TEMPORARILY_UNMETERED;
                }
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
                if (Build.VERSION.SDK_INT >= 30 && value == NetworkTypeIds.TEMPORARILY_UNMETERED) {
                    return NetworkType.TEMPORARILY_UNMETERED;
                }
                throw new IllegalArgumentException(
                        "Could not convert " + value + " to NetworkType");
        }
    }

    /**
     * Converts a {@link OutOfQuotaPolicy} to an int.
     *
     * @param policy The {@link OutOfQuotaPolicy} policy being used
     * @return the corresponding int representation.
     */
    @TypeConverter
    public static int outOfQuotaPolicyToInt(@NonNull OutOfQuotaPolicy policy) {
        switch (policy) {
            case RUN_AS_NON_EXPEDITED_WORK_REQUEST:
                return OutOfPolicyIds.RUN_AS_NON_EXPEDITED_WORK_REQUEST;
            case DROP_WORK_REQUEST:
                return OutOfPolicyIds.DROP_WORK_REQUEST;
            default:
                throw new IllegalArgumentException(
                        "Could not convert " + policy + " to int");
        }
    }

    /**
     * Converter from an int to a {@link OutOfQuotaPolicy}.
     *
     * @param value The input integer
     * @return An {@link OutOfQuotaPolicy}
     */
    @TypeConverter
    @NonNull
    public static OutOfQuotaPolicy intToOutOfQuotaPolicy(int value) {
        switch (value) {
            case OutOfPolicyIds.RUN_AS_NON_EXPEDITED_WORK_REQUEST:
                return OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST;
            case OutOfPolicyIds.DROP_WORK_REQUEST:
                return OutOfQuotaPolicy.DROP_WORK_REQUEST;
            default:
                throw new IllegalArgumentException(
                        "Could not convert " + value + " to OutOfQuotaPolicy");
        }
    }

    /**
     * Converts a list of {@link ContentUriTriggers.Trigger}s to byte array representation
     * @param triggers the list of {@link ContentUriTriggers.Trigger}s to convert
     * @return corresponding byte array representation
     */
    @TypeConverter
    @SuppressWarnings("CatchAndPrintStackTrace")
    public static byte[] contentUriTriggersToByteArray(ContentUriTriggers triggers) {
        if (triggers.size() == 0) {
            return null;
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = null;
        try {
            objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeInt(triggers.size());
            for (ContentUriTriggers.Trigger trigger : triggers.getTriggers()) {
                objectOutputStream.writeUTF(trigger.getUri().toString());
                objectOutputStream.writeBoolean(trigger.shouldTriggerForDescendants());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (objectOutputStream != null) {
                try {
                    objectOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return outputStream.toByteArray();
    }

    /**
     * Converts a byte array to list of {@link ContentUriTriggers.Trigger}s
     * @param bytes byte array representation to convert
     * @return list of {@link ContentUriTriggers.Trigger}s
     */
    @TypeConverter
    @SuppressWarnings("CatchAndPrintStackTrace")
    public static ContentUriTriggers byteArrayToContentUriTriggers(byte[] bytes) {
        ContentUriTriggers triggers = new ContentUriTriggers();
        if (bytes == null) {
            // bytes will be null if there are no Content Uri Triggers
            return triggers;
        }
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        ObjectInputStream objectInputStream = null;
        try {
            objectInputStream = new ObjectInputStream(inputStream);
            for (int i = objectInputStream.readInt(); i > 0; i--) {
                Uri uri = Uri.parse(objectInputStream.readUTF());
                boolean triggersForDescendants = objectInputStream.readBoolean();
                triggers.add(uri, triggersForDescendants);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (objectInputStream != null) {
                try {
                    objectInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return triggers;
    }

    private WorkTypeConverters() {
    }
}
