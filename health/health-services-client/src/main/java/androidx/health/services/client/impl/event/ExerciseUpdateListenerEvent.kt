package androidx.health.services.client.impl.event

import android.os.Parcelable
import androidx.health.services.client.data.ProtoParcelable
import androidx.health.services.client.impl.response.AvailabilityResponse
import androidx.health.services.client.impl.response.ExerciseEventResponse
import androidx.health.services.client.impl.response.ExerciseLapSummaryResponse
import androidx.health.services.client.impl.response.ExerciseUpdateResponse
import androidx.health.services.client.proto.EventsProto.ExerciseUpdateListenerEvent as ListenerProto

/** An event representing an [ExerciseUpdateListener] invocation. */
internal class ExerciseUpdateListenerEvent(override val proto: ListenerProto) :
    ProtoParcelable<ListenerProto>() {

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<ExerciseUpdateListenerEvent> = newCreator {
            ExerciseUpdateListenerEvent(ListenerProto.parseFrom(it))
        }

        @JvmStatic
        public fun createExerciseUpdateEvent(
            exerciseUpdate: ExerciseUpdateResponse
        ): ExerciseUpdateListenerEvent =
            ExerciseUpdateListenerEvent(
                ListenerProto.newBuilder().setExerciseUpdateResponse(exerciseUpdate.proto).build()
            )

        @JvmStatic
        public fun createLapSummaryEvent(
            lapSummary: ExerciseLapSummaryResponse
        ): ExerciseUpdateListenerEvent =
            ExerciseUpdateListenerEvent(
                ListenerProto.newBuilder().setLapSummaryResponse(lapSummary.proto).build()
            )

        @JvmStatic
        public fun createAvailabilityUpdateEvent(
            availability: AvailabilityResponse
        ): ExerciseUpdateListenerEvent =
            ExerciseUpdateListenerEvent(
                ListenerProto.newBuilder().setAvailabilityResponse(availability.proto).build()
            )

        @JvmStatic
        public fun createExerciseEventUpdateEvent(
            exerciseEvent: ExerciseEventResponse
        ): ExerciseUpdateListenerEvent =
            ExerciseUpdateListenerEvent(
                ListenerProto.newBuilder().setExerciseEventResponse(exerciseEvent.proto).build()
            )
    }
}
