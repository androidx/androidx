package androidx.health.services.client.impl.event

import android.os.Parcelable
import androidx.health.services.client.data.ProtoParcelable
import androidx.health.services.client.impl.response.PassiveMonitoringUpdateResponse
import androidx.health.services.client.proto.EventsProto.PassiveCallbackEvent as EventProto

/**
 * An event representing a `PassiveMonitoringCallback` invocation.
 *
 * @hide
 */
public class PassiveCallbackEvent(public override val proto: EventProto) :
    ProtoParcelable<EventProto>() {

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<PassiveCallbackEvent> = newCreator {
            PassiveCallbackEvent(EventProto.parseFrom(it))
        }

        @JvmStatic
        public fun createPassiveUpdateResponse(
            response: PassiveMonitoringUpdateResponse
        ): PassiveCallbackEvent =
            PassiveCallbackEvent(
                EventProto.newBuilder().setPassiveUpdateResponse(response.proto).build()
            )
    }
}
