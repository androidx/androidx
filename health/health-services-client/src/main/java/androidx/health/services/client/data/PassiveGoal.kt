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
import androidx.health.services.client.proto.DataProto.PassiveGoal as PassiveGoalProto

/** Defines an passive goal that will be triggered when the specified condition is met. */
@Suppress("ParcelCreator")
public class PassiveGoal(
    /** [DataTypeCondition] which must be met for the passive goal to be triggered. */
    public val dataTypeCondition: DataTypeCondition,
    public val triggerType: TriggerType,
) : ProtoParcelable<PassiveGoalProto>() {

    internal constructor(
        proto: PassiveGoalProto
    ) : this(
        DataTypeCondition(proto.condition),
        TriggerType.fromProto(proto.triggerType)
            ?: throw IllegalStateException("Invalid TriggerType ${proto.triggerType}")
    )

    /** @hide */
    override val proto: PassiveGoalProto by lazy {
        PassiveGoalProto.newBuilder()
            .setCondition(dataTypeCondition.proto)
            .setTriggerType(triggerType.toProto())
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
        "PassiveGoal(dataTypeCondition=$dataTypeCondition, triggerType=$triggerType)"

    /** Whether or not repeated passive goals should be triggered. */
    public enum class TriggerType(public val id: Int) {
        /** The passive goal will trigger the first time the specified conditions are met. */
        ONCE(1),

        /**
         * The passive goal will trigger each time the specified conditions *become* met. Repeated
         * goals on daily metrics will trigger once per day.
         */
        REPEATED(2);

        /** @hide */
        public fun toProto(): PassiveGoalProto.TriggerType =
            PassiveGoalProto.TriggerType.forNumber(id)
                ?: PassiveGoalProto.TriggerType.TRIGGER_TYPE_UNKNOWN

        public companion object {
            @JvmStatic
            public fun fromId(id: Int): TriggerType? = values().firstOrNull { it.id == id }

            /** @hide */
            public fun fromProto(proto: PassiveGoalProto.TriggerType): TriggerType? =
                fromId(proto.number)
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
         * Creates a [PassiveGoal] from an [Intent]. Returns null if no [PassiveGoal] is stored in
         * the given intent.
         */
        @JvmStatic
        public fun fromIntent(intent: Intent): PassiveGoal? = intent.getParcelableExtra(EXTRA_KEY)
    }
}
