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
package androidx.health.connect.client.records.metadata

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.health.connect.client.records.Record
import java.time.Instant

/** Set of shared metadata fields for [Record]. */
@SuppressWarnings("NewApi") // Temporary until we can enable java8 desugaring effectively.
public class Metadata(
    /**
     * Unique identifier of this data, assigned by the Android Health Platform at insertion time.
     * When [Record] is created before insertion, this takes a sentinel value, any assigned value
     * will be ignored.
     */
    public val id: String = EMPTY_ID,

    /**
     * Where the data comes from, such as application information originally generated this data.
     * When [Record] is created before insertion, this contains a sentinel value, any assigned value
     * will be ignored. After insertion, this will be populated with inserted application.
     */
    public val dataOrigin: DataOrigin = DataOrigin(""),

    /**
     * Automatically populated to when data was last modified (or originally created). When [Record]
     * is created before inserted, this contains a sentinel value, any assigned value will be
     * ignored.
     */
    public val lastModifiedTime: Instant = Instant.EPOCH,

    /**
     * Optional client supplied record unique data identifier associated with the data.
     *
     * There is guaranteed a single entry for any type of data with same client provided identifier
     * for a given client. Any new insertions with the same client provided identifier will either
     * replace or be ignored depending on associated [clientRecordVersion].
     *
     * @see clientRecordVersion
     */
    public val clientRecordId: String? = null,

    /**
     * Optional client supplied version associated with the data.
     *
     * This determines conflict resolution outcome when there are multiple insertions of the same
     * [clientRecordId]. Data with the highest [clientRecordVersion] takes precedence.
     * [clientRecordVersion] starts with 0.
     *
     * @see clientRecordId
     */
    public val clientRecordVersion: Long = 0,

    /** Optional client supplied device information associated with the data. */
    public val device: Device? = null,

    /**
     * Optional client supplied data recording method to help to understand how the data was
     * recorded.
     *
     * It should be one of the following: [RECORDING_METHOD_UNKNOWN],
     * [RECORDING_METHOD_ACTIVELY_RECORDED], [RECORDING_METHOD_AUTOMATICALLY_RECORDED] and
     * [RECORDING_METHOD_MANUAL_ENTRY].
     */
    @param:RecordingMethod
    @property:RecordingMethod
    @get:RecordingMethod
    val recordingMethod: Int = RECORDING_METHOD_UNKNOWN,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Metadata) return false

        if (id != other.id) return false
        if (dataOrigin != other.dataOrigin) return false
        if (lastModifiedTime != other.lastModifiedTime) return false
        if (clientRecordId != other.clientRecordId) return false
        if (clientRecordVersion != other.clientRecordVersion) return false
        if (device != other.device) return false
        if (recordingMethod != other.recordingMethod) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + dataOrigin.hashCode()
        result = 31 * result + lastModifiedTime.hashCode()
        result = 31 * result + (clientRecordId?.hashCode() ?: 0)
        result = 31 * result + clientRecordVersion.hashCode()
        result = 31 * result + (device?.hashCode() ?: 0)
        result = 31 * result + recordingMethod.hashCode()
        return result
    }

    internal companion object {
        internal const val EMPTY_ID: String = ""

        /** A default instance of metadata with no fields initialised. */
        @JvmField internal val EMPTY = Metadata()

        /** Unknown recording method. */
        const val RECORDING_METHOD_UNKNOWN = 0

        /**
         * For actively recorded data by the user.
         *
         * For e.g. An exercise session actively recorded by the user using a phone or a watch
         * device.
         */
        const val RECORDING_METHOD_ACTIVELY_RECORDED = 1

        /**
         * For passively recorded data by the app.
         *
         * For e.g. Steps data recorded by a watch or phone without the user starting a session.
         */
        const val RECORDING_METHOD_AUTOMATICALLY_RECORDED = 2

        /**
         * For manually entered data by the user.
         *
         * For e.g. Nutrition or weight data entered by the user.
         */
        const val RECORDING_METHOD_MANUAL_ENTRY = 3

        /** List of possible Recording method for the [Record]. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @IntDef(
            RECORDING_METHOD_UNKNOWN,
            RECORDING_METHOD_ACTIVELY_RECORDED,
            RECORDING_METHOD_AUTOMATICALLY_RECORDED,
            RECORDING_METHOD_MANUAL_ENTRY
        )
        @Retention(AnnotationRetention.SOURCE)
        annotation class RecordingMethod
    }
}
