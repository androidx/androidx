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
package androidx.health.platform.client.permission

import android.os.Parcelable
import androidx.health.platform.client.impl.data.ProtoParcelable
import androidx.health.platform.client.proto.DataProto
import androidx.health.platform.client.proto.PermissionProto

/** Pair of DataType and AccessType. */
class Permission(
    val dataType: DataProto.DataType,
    val accessType: AccessType,
) : ProtoParcelable<PermissionProto.Permission>() {

    internal constructor(
        proto: PermissionProto.Permission
    ) : this(proto.dataType, AccessType.fromProto(proto.accessType))

    enum class AccessType(public val id: Int) {
        UNKNOWN(0),
        READ(1),
        WRITE(2);

        internal fun toProto(): PermissionProto.AccessType =
            PermissionProto.AccessType.forNumber(id)
                ?: PermissionProto.AccessType.ACCESS_TYPE_UNKNOWN

        companion object {
            fun fromProto(proto: PermissionProto.AccessType): AccessType =
                values().firstOrNull { it.id == proto.number } ?: UNKNOWN
        }
    }

    override val proto: PermissionProto.Permission by lazy {
        PermissionProto.Permission.newBuilder()
            .setDataType(dataType)
            .setAccessType(accessType.toProto())
            .build()
    }

    override fun toString(): String = "Permission(dataType=$dataType, accessType=$accessType)"
    override fun equals(other: Any?): Boolean =
        other is Permission && other.dataType == dataType && other.accessType === accessType

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Permission> = newCreator {
            val proto = PermissionProto.Permission.parseFrom(it)
            Permission(proto)
        }
    }
}
