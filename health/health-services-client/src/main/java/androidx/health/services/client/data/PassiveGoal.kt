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

import android.content.Intent
import android.os.Parcelable
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.health.services.client.data.PassiveGoal.TriggerFrequency.Companion.toProto
import androidx.health.services.client.proto.DataProto.PassiveGoal as PassiveGoalProto

/** Defines an passive goal that will be triggered when the specified condition is met. */
@Suppress("ParcelCreator")
public class PassiveGoal(
    /** [DataTypeCondition] which must be met for the passive goal to be triggered. */
    public val dataTypeCondition: DataTypeCondition,
    @TriggerFrequency public val triggerFrequency: Int,
) : ProtoParcelable<PassiveGoalProto>() {

    internal constructor(
        proto: PassiveGoalProto
    ) : this(DataTypeCondition(proto.condition), TriggerFrequency.fromProto(proto.triggerFrequency))

    /** @hide */
    override val proto: PassiveGoalProto by lazy {
        PassiveGoalProto.newBuilder()
            .setCondition(dataTypeCondition.proto)
            .setTriggerFrequency(triggerFrequency.toProto())
            .build()
    }

    /**
     * Puts the goal as an extra into a given [Intent]. The state can then be obtained from the
     * intent via [PassiveGoal.fromIntent].
     */
    public fun putToIntent(intent: Intent) {
        intent.putExtra(EXTRA_KEY, this)
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
    public annotation class TriggerFrequency {

        public companion object {
            /** TriggerFrequency is an unknown or unexpected value. */
            public const val UNKNOWN: Int = 0

            /** The passive goal will trigger the first time the specified conditions are met. */
            public const val ONCE: Int = 1

            /**
             * The passive goal will trigger *each time* the specified conditions become met.
             * Repeated goals on daily metrics will trigger once per day.
             */
            public const val REPEATED: Int = 2

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
            public fun fromProto(proto: PassiveGoalProto.TriggerFrequency): Int = proto.number
        }
    }

    /**
     * Does the provided [DataPoint] satisfy the passive goal condition.
     *
     * @throws IllegalArgumentException if the provided data point is not of the same data type as
     * the condition itself.
     */
    public fun isTriggered(dataPoint: DataPoint): Boolean {
        return dataTypeCondition.isSatisfied(dataPoint)
    }

    public companion object {
        private const val EXTRA_KEY = "hs.passive_goal"
        @Suppress("ActionValue") public const val ACTION_GOAL: String = "hs.passivemonitoring.GOAL"

        @JvmField
        public val CREATOR: Parcelable.Creator<PassiveGoal> = newCreator { bytes ->
            val proto = PassiveGoalProto.parseFrom(bytes)
            PassiveGoal(proto)
        }

        /**
         * Creates a [PassiveGoal] from an [Intent]. Returns `null` if no [PassiveGoal] is stored in
         * the given intent.
         */
        @Suppress("DEPRECATION")
        @JvmStatic
        public fun fromIntent(intent: Intent): PassiveGoal? = intent.getParcelableExtra(EXTRA_KEY)
    }
}
