package androidx.health.services.client.impl.response

import android.os.Parcelable
import androidx.health.services.client.data.ExerciseEvent
import androidx.health.services.client.data.ProtoParcelable
import androidx.health.services.client.proto.ResponsesProto

/** Response containing [ExerciseEvent] when it's sent. */
internal class ExerciseEventResponse(public val exerciseEvent: ExerciseEvent) :
  ProtoParcelable<ResponsesProto.ExerciseEventResponse>() {
  override val proto: ResponsesProto.ExerciseEventResponse by lazy {
    ResponsesProto.ExerciseEventResponse.newBuilder()
      .setExerciseEvent(exerciseEvent.toProto())
      .build()
  }

  public companion object {
    @JvmField
    public val CREATOR: Parcelable.Creator<ExerciseEventResponse> = newCreator { bytes ->
      val proto = ResponsesProto.ExerciseEventResponse.parseFrom(bytes)
      ExerciseEventResponse(ExerciseEvent.fromProto(proto.exerciseEvent))
    }
  }
}
