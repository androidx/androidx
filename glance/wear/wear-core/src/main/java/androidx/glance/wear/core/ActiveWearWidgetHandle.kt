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

import android.content.ComponentName
import androidx.annotation.RestrictTo
import androidx.glance.wear.parcel.ActiveWearWidgetHandleParcel
import androidx.glance.wear.proto.ActiveWearWidgetHandleProto
import java.util.Objects

/**
 * Identifies a unique instance of a widget or tile that is active in the host.
 *
 * A widget or a tile instance is active when it was added by the user to a widget or tile surface.
 *
 * @property provider The name of the provider associated with the widget instance.
 * @property instanceId The id of the widget instance.
 * @property containerType The container type of the widget instance.
 */
public class ActiveWearWidgetHandle
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public constructor(
    public val provider: ComponentName,
    public val instanceId: WidgetInstanceId,
    @get:ContainerInfo.ContainerType
    @param:ContainerInfo.ContainerType
    public val containerType: Int,
) {
    override fun equals(other: Any?): Boolean =
        when {
            this === other -> true
            other !is ActiveWearWidgetHandle -> false
            else ->
                this.provider == other.provider &&
                    this.instanceId == other.instanceId &&
                    this.containerType == other.containerType
        }

    override fun hashCode(): Int = Objects.hash(provider, instanceId, containerType)

    override fun toString(): String =
        "ActiveWidget{${provider.className}:$instanceId type=${ContainerInfo.Companion.containerTypeToString(containerType)}}"

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun toParcel(): ActiveWearWidgetHandleParcel {
        val handleProto =
            ActiveWearWidgetHandleProto(
                id = instanceId.id,
                id_namespace = instanceId.namespace,
                container_type = containerType,
            )
        return ActiveWearWidgetHandleParcel().apply { payload = handleProto.encode() }
    }

    public companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun fromParcel(
            handleParcel: ActiveWearWidgetHandleParcel,
            provider: ComponentName,
        ): ActiveWearWidgetHandle {
            val handleProto =
                ActiveWearWidgetHandleProto.Companion.ADAPTER.decode(handleParcel.payload)
            return ActiveWearWidgetHandle(
                provider = provider,
                instanceId = WidgetInstanceId(handleProto.id_namespace, handleProto.id),
                containerType = handleProto.container_type,
            )
        }
    }
}
