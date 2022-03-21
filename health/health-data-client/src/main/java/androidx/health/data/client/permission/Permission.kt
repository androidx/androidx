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
package androidx.health.data.client.permission

import androidx.annotation.RestrictTo
import androidx.health.data.client.records.Record
import kotlin.reflect.KClass

/**
 * Class to represent a permission which consists of a [KClass] representing a data type and a
 * [AccessTypes] enum representing an access type.
 *
 * @property recordType type of [Record] the permission gives access for
 * @property accessType whether read or write access
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class Permission(
    internal val recordType: KClass<out Record>,
    @property:AccessType internal val accessType: Int,
) {
    companion object {
        /**
         * Creates a permission of the given [accessType] for record type [T].
         *
         * @param T type of [Record]
         * @param accessType whether read or write access
         * @return Permission for given [accessType] for record type [T]
         */
        public inline fun <reified T : Record> create(@AccessType accessType: Int): Permission {
            return Permission(T::class, accessType)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Permission) return false

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
