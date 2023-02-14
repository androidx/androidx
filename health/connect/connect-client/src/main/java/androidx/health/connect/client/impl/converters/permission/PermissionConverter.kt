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
@file:RestrictTo(RestrictTo.Scope.LIBRARY)

package androidx.health.connect.client.impl.converters.permission

import androidx.annotation.RestrictTo
import androidx.health.connect.client.impl.converters.datatype.toDataType
import androidx.health.connect.client.impl.converters.datatype.toDataTypeKClass
import androidx.health.connect.client.permission.AccessTypes
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.platform.client.proto.PermissionProto

private fun toAccessTypeProto(accessType: Int): PermissionProto.AccessType {
    return when (accessType) {
        AccessTypes.WRITE -> PermissionProto.AccessType.ACCESS_TYPE_WRITE
        AccessTypes.READ -> PermissionProto.AccessType.ACCESS_TYPE_READ
        else -> PermissionProto.AccessType.ACCESS_TYPE_UNKNOWN
    }
}

private fun PermissionProto.AccessType.toAccessType(): Int {
    return when (this) {
        PermissionProto.AccessType.ACCESS_TYPE_WRITE -> AccessTypes.WRITE
        PermissionProto.AccessType.ACCESS_TYPE_READ -> AccessTypes.READ
        else -> throw IllegalStateException("Unknown access type")
    }
}

fun HealthPermission.toProtoPermission(): PermissionProto.Permission =
    PermissionProto.Permission.newBuilder()
        .setDataType(recordType.toDataType())
        .setAccessType(toAccessTypeProto(accessType))
        .build()

fun PermissionProto.Permission.toJetpackPermission(): HealthPermission {
    val dataTypeKClass = dataType.name.toDataTypeKClass()
    return HealthPermission(dataTypeKClass, accessType.toAccessType())
}
