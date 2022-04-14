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
package androidx.work.impl.model

import android.net.Uri
import android.os.Build
import androidx.room.TypeConverter
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Constraints.ContentUriTrigger
import androidx.work.NetworkType
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.lang.IllegalArgumentException

/**
 * TypeConverters for WorkManager enums and classes.
 */
object WorkTypeConverters {
    /**
     * Integer identifiers that map to [WorkInfo.State].
     */
    object StateIds {
        const val ENQUEUED = 0
        const val RUNNING = 1
        const val SUCCEEDED = 2
        const val FAILED = 3
        const val BLOCKED = 4
        const val CANCELLED = 5
        const val COMPLETED_STATES = "($SUCCEEDED, $FAILED, $CANCELLED)"
    }

    /**
     * Integer identifiers that map to [BackoffPolicy].
     */
    private object BackoffPolicyIds {
        const val EXPONENTIAL = 0
        const val LINEAR = 1
    }

    /**
     * Integer identifiers that map to [NetworkType].
     */
    private object NetworkTypeIds {
        const val NOT_REQUIRED = 0
        const val CONNECTED = 1
        const val UNMETERED = 2
        const val NOT_ROAMING = 3
        const val METERED = 4
        const val TEMPORARILY_UNMETERED = 5
    }

    /**
     * Integer identifiers that map to [OutOfQuotaPolicy].
     */
    private object OutOfPolicyIds {
        const val RUN_AS_NON_EXPEDITED_WORK_REQUEST = 0
        const val DROP_WORK_REQUEST = 1
    }

    /**
     * TypeConverter for a State to an int.
     *
     * @param state The input State
     * @return The associated int constant
     */
    @JvmStatic
    @TypeConverter
    fun stateToInt(state: WorkInfo.State): Int {
        return when (state) {
            WorkInfo.State.ENQUEUED -> StateIds.ENQUEUED
            WorkInfo.State.RUNNING -> StateIds.RUNNING
            WorkInfo.State.SUCCEEDED -> StateIds.SUCCEEDED
            WorkInfo.State.FAILED -> StateIds.FAILED
            WorkInfo.State.BLOCKED -> StateIds.BLOCKED
            WorkInfo.State.CANCELLED -> StateIds.CANCELLED
        }
    }

    /**
     * TypeConverter for an int to a State.
     *
     * @param value The input integer
     * @return The associated State enum value
     */
    @JvmStatic
    @TypeConverter
    fun intToState(value: Int): WorkInfo.State {
        return when (value) {
            StateIds.ENQUEUED -> WorkInfo.State.ENQUEUED
            StateIds.RUNNING -> WorkInfo.State.RUNNING
            StateIds.SUCCEEDED -> WorkInfo.State.SUCCEEDED
            StateIds.FAILED -> WorkInfo.State.FAILED
            StateIds.BLOCKED -> WorkInfo.State.BLOCKED
            StateIds.CANCELLED -> WorkInfo.State.CANCELLED
            else -> throw IllegalArgumentException("Could not convert $value to State")
        }
    }

    /**
     * TypeConverter for a BackoffPolicy to an int.
     *
     * @param backoffPolicy The input BackoffPolicy
     * @return The associated int constant
     */
    @JvmStatic
    @TypeConverter
    fun backoffPolicyToInt(backoffPolicy: BackoffPolicy): Int {
        return when (backoffPolicy) {
            BackoffPolicy.EXPONENTIAL -> BackoffPolicyIds.EXPONENTIAL
            BackoffPolicy.LINEAR -> BackoffPolicyIds.LINEAR
        }
    }

    /**
     * TypeConverter for an int to a BackoffPolicy.
     *
     * @param value The input integer
     * @return The associated BackoffPolicy enum value
     */
    @JvmStatic
    @TypeConverter
    fun intToBackoffPolicy(value: Int): BackoffPolicy {
        return when (value) {
            BackoffPolicyIds.EXPONENTIAL -> BackoffPolicy.EXPONENTIAL
            BackoffPolicyIds.LINEAR -> BackoffPolicy.LINEAR
            else -> throw IllegalArgumentException("Could not convert $value to BackoffPolicy")
        }
    }

    /**
     * TypeConverter for a NetworkType to an int.
     *
     * @param networkType The input NetworkType
     * @return The associated int constant
     */
    @JvmStatic
    @TypeConverter
    fun networkTypeToInt(networkType: NetworkType): Int {
        return when (networkType) {
            NetworkType.NOT_REQUIRED -> NetworkTypeIds.NOT_REQUIRED
            NetworkType.CONNECTED -> NetworkTypeIds.CONNECTED
            NetworkType.UNMETERED -> NetworkTypeIds.UNMETERED
            NetworkType.NOT_ROAMING -> NetworkTypeIds.NOT_ROAMING
            NetworkType.METERED -> NetworkTypeIds.METERED
            else -> {
                if (Build.VERSION.SDK_INT >= 30 && networkType == NetworkType.TEMPORARILY_UNMETERED)
                    NetworkTypeIds.TEMPORARILY_UNMETERED
                else
                    throw IllegalArgumentException("Could not convert $networkType to int")
            }
        }
    }

    /**
     * TypeConverter for an int to a NetworkType.
     *
     * @param value The input integer
     * @return The associated NetworkType enum value
     */
    @JvmStatic
    @TypeConverter
    fun intToNetworkType(value: Int): NetworkType {
        return when (value) {
            NetworkTypeIds.NOT_REQUIRED -> NetworkType.NOT_REQUIRED
            NetworkTypeIds.CONNECTED -> NetworkType.CONNECTED
            NetworkTypeIds.UNMETERED -> NetworkType.UNMETERED
            NetworkTypeIds.NOT_ROAMING -> NetworkType.NOT_ROAMING
            NetworkTypeIds.METERED -> NetworkType.METERED
            else -> {
                if (Build.VERSION.SDK_INT >= 30 && value == NetworkTypeIds.TEMPORARILY_UNMETERED) {
                    return NetworkType.TEMPORARILY_UNMETERED
                } else throw IllegalArgumentException("Could not convert $value to NetworkType")
            }
        }
    }

    /**
     * Converts a [OutOfQuotaPolicy] to an int.
     *
     * @param policy The [OutOfQuotaPolicy] policy being used
     * @return the corresponding int representation.
     */
    @JvmStatic
    @TypeConverter
    fun outOfQuotaPolicyToInt(policy: OutOfQuotaPolicy): Int {
        return when (policy) {
            OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST ->
                OutOfPolicyIds.RUN_AS_NON_EXPEDITED_WORK_REQUEST
            OutOfQuotaPolicy.DROP_WORK_REQUEST -> OutOfPolicyIds.DROP_WORK_REQUEST
        }
    }

    /**
     * Converter from an int to a [OutOfQuotaPolicy].
     *
     * @param value The input integer
     * @return An [OutOfQuotaPolicy]
     */
    @JvmStatic
    @TypeConverter
    fun intToOutOfQuotaPolicy(value: Int): OutOfQuotaPolicy {
        return when (value) {
            OutOfPolicyIds.RUN_AS_NON_EXPEDITED_WORK_REQUEST ->
                OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST
            OutOfPolicyIds.DROP_WORK_REQUEST -> OutOfQuotaPolicy.DROP_WORK_REQUEST
            else -> throw IllegalArgumentException("Could not convert $value to OutOfQuotaPolicy")
        }
    }

    /**
     * Converts a set of [Constraints.ContentUriTrigger]s to byte array representation
     * @param triggers the list of [Constraints.ContentUriTrigger]s to convert
     * @return corresponding byte array representation
     */
    @JvmStatic
    @TypeConverter
    fun setOfTriggersToByteArray(triggers: Set<ContentUriTrigger>): ByteArray {
        if (triggers.isEmpty()) {
            return ByteArray(0)
        }
        val outputStream = ByteArrayOutputStream()
        outputStream.use {
            ObjectOutputStream(outputStream).use { objectOutputStream ->
                objectOutputStream.writeInt(triggers.size)
                for (trigger in triggers) {
                    objectOutputStream.writeUTF(trigger.uri.toString())
                    objectOutputStream.writeBoolean(trigger.isTriggeredForDescendants)
                }
            }
        }
        return outputStream.toByteArray()
    }

    /**
     * Converts a byte array to set of [ContentUriTrigger]s
     * @param bytes byte array representation to convert
     * @return set of [ContentUriTrigger]
     */
    @JvmStatic
    @TypeConverter
    fun byteArrayToSetOfTriggers(bytes: ByteArray): Set<ContentUriTrigger> {
        val triggers = mutableSetOf<ContentUriTrigger>()
        if (bytes.isEmpty()) {
            // bytes will be null if there are no Content Uri Triggers
            return triggers
        }
        val inputStream = ByteArrayInputStream(bytes)
        inputStream.use {
            try {
                ObjectInputStream(inputStream).use { objectInputStream ->
                    repeat(objectInputStream.readInt()) {
                        val uri = Uri.parse(objectInputStream.readUTF())
                        val triggersForDescendants = objectInputStream.readBoolean()
                        triggers.add(ContentUriTrigger(uri, triggersForDescendants))
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return triggers
    }
}