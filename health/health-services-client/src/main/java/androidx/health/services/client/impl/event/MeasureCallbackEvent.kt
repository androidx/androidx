package androidx.health.services.client.impl.event

import android.os.Parcelable
import androidx.health.services.client.data.ProtoParcelable
import androidx.health.services.client.impl.response.AvailabilityResponse
import androidx.health.services.client.impl.response.DataPointsResponse
import androidx.health.services.client.proto.EventsProto.MeasureCallbackEvent as ListenerProto

/**
 * An event representing a `MeasureCallback` invocation.
 *
 * @hide
 */
public class MeasureCallbackEvent(public override val proto: ListenerProto) :
    ProtoParcelable<ListenerProto>() {

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<MeasureCallbackEvent> = newCreator {
            MeasureCallbackEvent(ListenerProto.parseFrom(it))
        }

        @JvmStatic
        public fun createDataPointsUpdateEvent(
            dataPointsResponse: DataPointsResponse
        ): MeasureCallbackEvent =
            MeasureCallbackEvent(
                ListenerProto.newBuilder().setDataPointResponse(dataPointsResponse.proto).build()
            )

        @JvmStatic
        public fun createAvailabilityUpdateEvent(
            availability: AvailabilityResponse
        ): MeasureCallbackEvent =
            MeasureCallbackEvent(
                ListenerProto.newBuilder().setAvailabilityResponse(availability.proto).build()
            )
    }
}
