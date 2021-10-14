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

import android.os.Parcelable
import androidx.health.services.client.proto.DataProto
import androidx.health.services.client.proto.DataProto.ExerciseTypeCapabilities.SupportedGoalEntry
import androidx.health.services.client.proto.DataProto.ExerciseTypeCapabilities.SupportedMilestoneEntry

/** Provides exercise specific capabilities data. */
@Suppress("ParcelCreator")
public class ExerciseTypeCapabilities(
    public val supportedDataTypes: Set<DataType>,
    public val supportedGoals: Map<DataType, Set<ComparisonType>>,
    public val supportedMilestones: Map<DataType, Set<ComparisonType>>,
    public val supportsAutoPauseAndResume: Boolean,
    public val supportsLaps: Boolean,
) : ProtoParcelable<DataProto.ExerciseTypeCapabilities>() {

    internal constructor(
        proto: DataProto.ExerciseTypeCapabilities
    ) : this(
        proto.supportedDataTypesList.map { DataType(it) }.toSet(),
        proto
            .supportedGoalsList
            .map { entry ->
                DataType(entry.dataType) to
                    entry.comparisonTypesList.mapNotNull { ComparisonType.fromProto(it) }.toSet()
            }
            .toMap(),
        proto
            .supportedMilestonesList
            .map { entry ->
                DataType(entry.dataType) to
                    entry.comparisonTypesList.mapNotNull { ComparisonType.fromProto(it) }.toSet()
            }
            .toMap(),
        supportsAutoPauseAndResume = proto.isAutoPauseAndResumeSupported,
        supportsLaps = proto.isLapsSupported
    )

    /** @hide */
    override val proto: DataProto.ExerciseTypeCapabilities by lazy {
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
            .setIsLapsSupported(supportsLaps)
            .build()
    }

    override fun toString(): String =
        "ExerciseTypeCapabilities(" +
            "supportedDataTypes=$supportedDataTypes, " +
            "supportedGoals=$supportedGoals, " +
            "supportedMilestones=$supportedMilestones, " +
            "supportsAutoPauseAndResume=$supportsAutoPauseAndResume, " +
            "supportsLaps=$supportsLaps)"

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<ExerciseTypeCapabilities> = newCreator { bytes ->
            val proto = DataProto.ExerciseTypeCapabilities.parseFrom(bytes)
            ExerciseTypeCapabilities(proto)
        }
    }
}
