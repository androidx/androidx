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

import androidx.health.services.client.proto.DataProto
import androidx.health.services.client.proto.DataProto.ExerciseTypeCapabilities.SupportedGoalEntry
import androidx.health.services.client.proto.DataProto.ExerciseTypeCapabilities.SupportedMilestoneEntry

/** Provides exercise specific capabilities data. */
@Suppress("ParcelCreator")
public class ExerciseTypeCapabilities
@JvmOverloads
constructor(
    /** Supported [DataType]s for a given exercise. */
    public val supportedDataTypes: Set<DataType<*, *>>,
    /** Map from supported goals [DataType]s to a set of compatible [ComparisonType]s. */
    public val supportedGoals: Map<AggregateDataType<*, *>, Set<ComparisonType>>,
    /** Map from supported milestone [DataType]s to a set of compatible [ComparisonType]s. */
    public val supportedMilestones: Map<AggregateDataType<*, *>, Set<ComparisonType>>,
    /** Returns `true` if the given exercise supports auto pause and resume. */
    public val supportsAutoPauseAndResume: Boolean,
    /** Map from [ExerciseEventType]s to their [ExerciseEventCapabilities]. */
    internal val exerciseEventCapabilities: Map<ExerciseEventType<*>, ExerciseEventCapabilities> =
    emptyMap(),
) {

    internal constructor(
        proto: DataProto.ExerciseTypeCapabilities
    ) : this(
        proto.supportedDataTypesList.map { DataType.deltaAndAggregateFromProto(it) }
            .flatten()
            .toSet(),
        proto
            .supportedGoalsList
            .map { entry ->
                DataType.aggregateFromProto(entry.dataType) to
                    entry
                        .comparisonTypesList
                        .map { ComparisonType.fromProto(it) }
                        .filter { it != ComparisonType.UNKNOWN }
                        .toSet()
            }
            .toMap(),
        proto
            .supportedMilestonesList
            .map { entry ->
                DataType.aggregateFromProto(entry.dataType) to
                    entry
                        .comparisonTypesList
                        .map { ComparisonType.fromProto(it) }
                        .filter { it != ComparisonType.UNKNOWN }
                        .toSet()
            }
            .toMap(),
        supportsAutoPauseAndResume = proto.isAutoPauseAndResumeSupported,
        exerciseEventCapabilities = proto.supportedExerciseEventsList
            .filter { ExerciseEventCapabilities.fromProto(it) != null }.associate { entry ->
                ExerciseEventType.fromProto(entry.exerciseEventType) to
                    ExerciseEventCapabilities.fromProto(entry)!!
            },
    )

    internal val proto: DataProto.ExerciseTypeCapabilities =
        DataProto.ExerciseTypeCapabilities.newBuilder()
            .addAllSupportedDataTypes(supportedDataTypes.map { it.proto })
            .addAllSupportedGoals(
                supportedGoals
                    .map { entry ->
                        SupportedGoalEntry.newBuilder()
                            .setDataType(entry.key.proto)
                            .addAllComparisonTypes(entry.value.map { it.toProto() })
                            .build()
                    }
                    .sortedBy { it.dataType.name } // Sorting to ensure equals() works
            )
            .addAllSupportedMilestones(
                supportedMilestones
                    .map { entry ->
                        SupportedMilestoneEntry.newBuilder()
                            .setDataType(entry.key.proto)
                            .addAllComparisonTypes(entry.value.map { it.toProto() })
                            .build()
                    }
                    .sortedBy { it.dataType.name } // Sorting to ensure equals() works
            )
            .setIsAutoPauseAndResumeSupported(supportsAutoPauseAndResume)
            .addAllSupportedExerciseEvents(exerciseEventCapabilities.map { it.value.toProto() })
            .build()

    /** Returns the set of supported [ExerciseEventType]s on this device. */
    public val supportedExerciseEvents: Set<ExerciseEventType<*>>
        get() = this.exerciseEventCapabilities.keys

    /** Returns the [ExerciseEventCapabilities] for a requested [ExerciseEventType]. */
    public fun <C : ExerciseEventCapabilities> getExerciseEventCapabilityDetails(
        exerciseEventType: ExerciseEventType<C>
    ): C? {
        @Suppress("UNCHECKED_CAST") // Map's keys' and values' types will match
        return exerciseEventCapabilities[exerciseEventType] as C?
    }

    override fun toString(): String =
        "ExerciseTypeCapabilities(" +
            "supportedDataTypes=$supportedDataTypes, " +
            "supportedGoals=$supportedGoals, " +
            "supportedMilestones=$supportedMilestones, " +
            "supportsAutoPauseAndResume=$supportsAutoPauseAndResume, "
}
