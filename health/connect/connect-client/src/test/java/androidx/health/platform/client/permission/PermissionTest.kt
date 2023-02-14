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

import android.os.Parcel
import androidx.health.platform.client.proto.DataProto
import androidx.health.platform.client.proto.PermissionProto
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

private val HEART_RATE_DATA_TYPE = DataProto.DataType.newBuilder().setName("HEART_RATE").build()

@RunWith(AndroidJUnit4::class)
class PermissionTest {

    @Test
    fun writeToParcel() {
        val permission =
            Permission(
                PermissionProto.Permission.newBuilder()
                    .setDataType(HEART_RATE_DATA_TYPE)
                    .setAccessType(PermissionProto.AccessType.ACCESS_TYPE_READ)
                    .build()
            )

        val parcel = Parcel.obtain()
        parcel.writeParcelable(permission, 0)
        parcel.setDataPosition(0)
        @Suppress("Deprecation") // readParcelable deprecated in T and introduced new methods
        val parcelOut = parcel.readParcelable<Permission>(Permission::class.java.classLoader)
        assertThat(parcelOut).isEqualTo(permission)
    }
}
