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
package androidx.health.platform.client.impl.converters.permission

import androidx.health.data.client.impl.converters.permission.toJetpackPermission
import androidx.health.data.client.impl.converters.permission.toProtoPermission
import androidx.health.data.client.permission.AccessTypes
import androidx.health.data.client.permission.Permission
import androidx.health.data.client.records.Steps
import androidx.health.platform.client.proto.DataProto
import androidx.health.platform.client.proto.PermissionProto
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionConverterTest {
    @Test
    fun jetpackToProtoPermission() {
        val protoPermission = Permission(Steps::class, AccessTypes.WRITE).toProtoPermission()

        assertThat(protoPermission)
            .isEqualTo(
                PermissionProto.Permission.newBuilder()
                    .setDataType(DataProto.DataType.newBuilder().setName("Steps").build())
                    .setAccessType(PermissionProto.AccessType.ACCESS_TYPE_WRITE)
                    .build()
            )
    }

    @Test
    fun jetpackToProtoPermissions() {
        val protoPermissions =
            setOf(Permission(Steps::class, AccessTypes.READ))
                .asSequence()
                .map { it.toProtoPermission() }
                .toSet()

        assertThat(protoPermissions)
            .isEqualTo(
                setOf(
                    PermissionProto.Permission.newBuilder()
                        .setDataType(DataProto.DataType.newBuilder().setName("Steps").build())
                        .setAccessType(PermissionProto.AccessType.ACCESS_TYPE_READ)
                        .build()
                )
            )
    }

    @Test
    fun protoToJetpackPermission() {
        val jetpackPermission =
            PermissionProto.Permission.newBuilder()
                .setDataType(DataProto.DataType.newBuilder().setName("Steps").build())
                .setAccessType(PermissionProto.AccessType.ACCESS_TYPE_WRITE)
                .build()
                .toJetpackPermission()

        assertThat(jetpackPermission).isEqualTo(Permission(Steps::class, AccessTypes.WRITE))
    }

    @Test
    fun protoToJetpackPermissions() {
        val jetpackPermissions =
            setOf(
                    PermissionProto.Permission.newBuilder()
                        .setDataType(DataProto.DataType.newBuilder().setName("Steps").build())
                        .setAccessType(PermissionProto.AccessType.ACCESS_TYPE_READ)
                        .build()
                )
                .asSequence()
                .map { it.toJetpackPermission() }
                .toSet()

        assertThat(jetpackPermissions).isEqualTo(setOf(Permission(Steps::class, AccessTypes.READ)))
    }
}
