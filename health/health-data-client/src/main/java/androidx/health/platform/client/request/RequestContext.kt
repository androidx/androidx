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
package androidx.health.platform.client.request

import android.os.Parcelable
import androidx.health.platform.client.impl.data.ProtoParcelable
import androidx.health.platform.client.proto.RequestProto

/** Data object holding context data for IPC calls. */
class RequestContext(
    val callingPackage: String,
    val sdkVersion: Int,
    val permissionToken: String?,
    val isInForeground: Boolean
) : ProtoParcelable<RequestProto.RequestContext>() {

    @Suppress("CheckResult")
    override val proto: RequestProto.RequestContext by lazy {
        val obj = this
        RequestProto.RequestContext.newBuilder()
            .setCallingPackage(obj.callingPackage)
            .setSdkVersion(obj.sdkVersion)
            .apply {
                obj.permissionToken?.let { setPermissionToken(it) }
            }
            .setIsInForeground(obj.isInForeground)
            .build()
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<RequestContext> = newCreator {
            RequestProto.RequestContext.parseFrom(it).run {
                RequestContext(callingPackage, sdkVersion, permissionToken, isInForeground)
            }
        }
    }
}
