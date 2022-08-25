/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.health.services.client.data

import androidx.health.services.client.proto.DataProto.PassiveGoal as PassiveGoalProto
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.health.services.client.data.PassiveGoal.TriggerFrequency.Companion.toProto

/** Defines an passive goal that will be triggered when the specified condition is met. */
@Suppress("ParcelCreator")
class PassiveGoal(
    /** [DataTypeCondition] which must be met for the passive goal to be triggered. */
    val dataTypeCondition: DataTypeCondition<out Number, out DeltaDataType<out Number, *>>,
    /** Frequency this goal should trigger, which is expected to be a  */
    @TriggerFrequency val triggerFrequency: Int,
) {

    internal constructor(
        proto: PassiveGoalProto
    ) : this(
        DataTypeCondition.deltaFromProto(proto.condition),
        TriggerFrequency.fromProto(proto.triggerFrequency)
    )

    internal val proto: PassiveGoalProto =
        PassiveGoalProto.newBuilder().setCondition(dataTypeCondition.proto)
            .setTriggerFrequency(triggerFrequency.toProto()).build()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PassiveGoal) return false
        if (dataTypeCondition != other.dataTypeCondition) return false
        if (triggerFrequency != other.triggerFrequency) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dataTypeCondition.hashCode()
        result = 31 * result + triggerFrequency
        return result
    }

    override fun toString(): String =
        "PassiveGoal(dataTypeCondition=$dataTypeCondition, triggerFrequency=$triggerFrequency)"

    /**
     * The frequency at which passive goals should be triggered.
     *
     * @hide
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        TriggerFrequency.ONCE,
        TriggerFrequency.REPEATED,
    )
    annotation class TriggerFrequency {

        companion object {
            /** TriggerFrequency is an unknown or unexpected value. */
            const val UNKNOWN: Int = 0

            /** The passive goal will trigger the first time the specified conditions are met. */
            const val ONCE: Int = 1

            /**
             * The passive goal will trigger *each time* the specified conditions become met.
             * Repeated goals on daily metrics will trigger once per day.
             */
            const val REPEATED: Int = 2

            /** @hide */
            @RestrictTo(RestrictTo.Scope.LIBRARY)
            internal fun @receiver:TriggerFrequency
            Int.toProto(): PassiveGoalProto.TriggerFrequency =
                PassiveGoalProto.TriggerFrequency.forNumber(this)
                    ?: PassiveGoalProto.TriggerFrequency.TRIGGER_FREQUENCY_UNKNOWN

            /** @hide */
            @RestrictTo(RestrictTo.Scope.LIBRARY)
            @TriggerFrequency
            @Suppress("WrongConstant")
            fun fromProto(proto: PassiveGoalProto.TriggerFrequency): Int = proto.number
        }
    }
}
