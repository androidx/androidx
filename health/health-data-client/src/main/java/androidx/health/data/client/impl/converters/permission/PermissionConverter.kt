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
package androidx.health.data.client.impl.converters.permission

import androidx.health.data.client.impl.converters.datatype.toDataTypeKClass
import androidx.health.data.client.impl.converters.datatype.toDataTypeName
import androidx.health.data.client.permission.AccessType
import androidx.health.data.client.permission.Permission
import androidx.health.platform.client.proto.DataProto
import androidx.health.platform.client.proto.PermissionProto

fun AccessType.toProto(): PermissionProto.AccessType {
    return when (this) {
        AccessType.WRITE -> PermissionProto.AccessType.ACCESS_TYPE_WRITE
        AccessType.READ -> PermissionProto.AccessType.ACCESS_TYPE_READ
        else -> PermissionProto.AccessType.ACCESS_TYPE_UNKNOWN
    }
}

fun PermissionProto.AccessType.toAccessType(): AccessType {
    return when (this) {
        PermissionProto.AccessType.ACCESS_TYPE_WRITE -> AccessType.WRITE
        PermissionProto.AccessType.ACCESS_TYPE_READ -> AccessType.READ
        else -> AccessType.UNKNOWN
    }
}

fun Permission.toProtoPermission(): PermissionProto.Permission {
    val dataType = DataProto.DataType.newBuilder().setName(this.recordType.toDataTypeName()).build()
    return PermissionProto.Permission.newBuilder()
        .setDataType(dataType)
        .setAccessType(this.accessType.toProto())
        .build()
}

fun PermissionProto.Permission.toJetpackPermission(): Permission {
    val dataTypeKClass = dataType.name.toDataTypeKClass()
    return Permission(dataTypeKClass, accessType.toAccessType())
}
