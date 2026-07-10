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
import androidx.glance.wear.parcel.WearWidgetEventBatchParcel
import androidx.glance.wear.proto.WearWidgetEventBatchProto
import androidx.glance.wear.proto.WearWidgetEventProto
import androidx.glance.wear.proto.WearWidgetVisibleEventProto
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.util.Objects

/** Represents an interaction event for a Wear Widget. */
public interface WearWidgetEvent

/**
 * An event where the widget became visible on screen.
 *
 * @property instanceId The instance id of the widget that was visible.
 * @property startTime The instant when the widget instance became visible.
 * @property duration The duration for which the widget instance was visible. After this duration,
 *   the widget was no longer visible.
 */
public class WearWidgetVisibleEvent
@RestrictTo(RestrictTo.Scope.LIBRARY)
constructor(
    public val instanceId: WidgetInstanceId,
    public val startTime: Instant,
    public val duration: Duration,
) : WearWidgetEvent {

    override fun equals(other: Any?): Boolean =
        when {
            this === other -> true
            other !is WearWidgetVisibleEvent -> false
            else ->
                instanceId == other.instanceId &&
                    startTime == other.startTime &&
                    duration == other.duration
        }

    override fun hashCode(): Int = Objects.hash(instanceId, startTime, duration)

    internal fun toProto(): WearWidgetVisibleEventProto =
        WearWidgetVisibleEventProto(
            id = instanceId.id,
            id_namespace = instanceId.namespace,
            start_time_millis = startTime.toEpochMilli(),
            duration_millis = duration.toMillis(),
        )

    internal companion object {
        fun fromProto(proto: WearWidgetVisibleEventProto): WearWidgetVisibleEvent =
            WearWidgetVisibleEvent(
                instanceId = WidgetInstanceId(id = proto.id, namespace = proto.id_namespace),
                startTime = Instant.ofEpochMilli(proto.start_time_millis),
                duration = Duration.ofMillis(proto.duration_millis),
            )
    }
}

/** A batch of [WearWidgetEvent]s. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class WearWidgetEventBatch(public val events: List<WearWidgetEvent>) {

    /** Convert this request to [WearWidgetEventBatchParcel]. */
    public fun toParcel(): WearWidgetEventBatchParcel {
        val protoEvents =
            events.mapNotNull { event ->
                when (event) {
                    is WearWidgetVisibleEvent ->
                        WearWidgetEventProto(visible_event = event.toProto())
                    else -> null
                }
            }
        val batchProto = WearWidgetEventBatchProto(events = protoEvents)
        return WearWidgetEventBatchParcel().apply { payload = batchProto.encode() }
    }

    public companion object {
        @Throws(IOException::class)
        public fun fromParcel(batchEventData: WearWidgetEventBatchParcel): WearWidgetEventBatch {
            val batchProto = WearWidgetEventBatchProto.ADAPTER.decode(batchEventData.payload)
            return WearWidgetEventBatch(
                events =
                    batchProto.events.mapNotNull { event ->
                        event.visible_event?.let(WearWidgetVisibleEvent::fromProto)
                    }
            )
        }
    }
}
