/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.health.connect.client.permission

import androidx.health.connect.client.records.Record
import kotlin.reflect.KClass

/**
 * A Permission either to read or write data associated with a [Record] type.
 *
 * @see androidx.health.connect.client.PermissionController
 */
public class HealthPermission
internal constructor(
    /** type of [Record] the permission gives access for. */
    internal val recordType: KClass<out Record>,
    /** whether read or write access. */
    @property:AccessType internal val accessType: Int,
) {
    companion object {
        /**
         * Creates [HealthPermission] to read provided [recordType], such as `Steps::class`.
         *
         * @return Permission object to use with
         * [androidx.health.connect.client.PermissionController].
         */
        @JvmStatic
        public fun createReadPermission(recordType: KClass<out Record>): HealthPermission {
            return HealthPermission(recordType, AccessTypes.READ)
        }

        /**
         * Creates [HealthPermission] to write provided [recordType], such as `Steps::class`.
         *
         * @return Permission object to use with
         * [androidx.health.connect.client.PermissionController].
         */
        @JvmStatic
        public fun createWritePermission(recordType: KClass<out Record>): HealthPermission {
            return HealthPermission(recordType, AccessTypes.WRITE)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HealthPermission) return false

        if (recordType != other.recordType) return false
        if (accessType != other.accessType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = recordType.hashCode()
        result = 31 * result + accessType
        return result
    }
}
