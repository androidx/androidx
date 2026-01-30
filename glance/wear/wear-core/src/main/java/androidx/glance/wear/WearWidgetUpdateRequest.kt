/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.glance.wear

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.glance.wear.parcel.WearWidgetUpdateRequestParcel
import androidx.glance.wear.proto.WearWidgetUpdateRequestProto

/** Describes the necessary information to request or push an update to the Host. */
@RestrictTo(LIBRARY_GROUP)
public class WearWidgetUpdateRequest(public val instanceId: WidgetInstanceId) {

    /** Convert to the parcelable [WearWidgetUpdateRequestParcel]. */
    @RestrictTo(LIBRARY_GROUP)
    public fun toParcel(): WearWidgetUpdateRequestParcel {
        val requestProto =
            WearWidgetUpdateRequestProto(id = instanceId.id, id_namespace = instanceId.namespace)
        return WearWidgetUpdateRequestParcel().apply { payload = requestProto.encode() }
    }

    @RestrictTo(LIBRARY_GROUP)
    public companion object {
        public fun fromParcel(
            contentParcel: WearWidgetUpdateRequestParcel
        ): WearWidgetUpdateRequest {
            val requestProto = WearWidgetUpdateRequestProto.ADAPTER.decode(contentParcel.payload)
            return WearWidgetUpdateRequest(
                instanceId =
                    WidgetInstanceId(namespace = requestProto.id_namespace, id = requestProto.id)
            )
        }
    }
}
