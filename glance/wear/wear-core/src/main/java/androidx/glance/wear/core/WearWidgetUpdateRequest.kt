/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.glance.wear.core

import androidx.annotation.RestrictTo
import androidx.glance.wear.parcel.WearWidgetUpdateRequestParcel
import androidx.glance.wear.proto.WearWidgetUpdateRequestProto

/** Describes the necessary information to request or push an update to the Host. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WearWidgetUpdateRequest(public val instanceId: WidgetInstanceId) {

    /** Convert to the parcelable [androidx.glance.wear.parcel.WearWidgetUpdateRequestParcel]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun toParcel(): WearWidgetUpdateRequestParcel {
        val requestProto =
            WearWidgetUpdateRequestProto(id = instanceId.id, id_namespace = instanceId.namespace)
        return WearWidgetUpdateRequestParcel().apply { payload = requestProto.encode() }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        public fun fromParcel(
            contentParcel: WearWidgetUpdateRequestParcel
        ): WearWidgetUpdateRequest {
            val requestProto =
                WearWidgetUpdateRequestProto.Companion.ADAPTER.decode(contentParcel.payload)
            return WearWidgetUpdateRequest(
                instanceId =
                    WidgetInstanceId(namespace = requestProto.id_namespace, id = requestProto.id)
            )
        }
    }
}
