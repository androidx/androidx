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

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import java.util.Objects

/** Defines configuration for an exercise tracked using HealthServices. */
@Suppress("DataClassPrivateConstructor")
public data class ExerciseConfig
protected constructor(
    /**
     * [ExerciseType] the user is performing for this exercise.
     *
     * This information can be used to tune sensors, e.g. the calories estimate can take the MET
     * value into account.
     */
    val exerciseType: ExerciseType,
    val dataTypes: Set<DataType>,
    val autoPauseAndResume: Boolean,
    val exerciseGoals: List<ExerciseGoal>,
    val exerciseParams: Bundle,
) : Parcelable {
    init {
        require(dataTypes.isNotEmpty()) { "Must specify the desired data types." }
        require(exerciseType != ExerciseType.UNKNOWN) { "Must specify a valid exercise type." }
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(exerciseType.id)
        dest.writeInt(dataTypes.size)
        dest.writeTypedArray(dataTypes.toTypedArray(), flags)
        dest.writeInt(if (autoPauseAndResume) 1 else 0)
        dest.writeInt(exerciseGoals.size)
        dest.writeTypedArray(exerciseGoals.toTypedArray(), flags)
        dest.writeBundle(exerciseParams)
    }

    /** Builder for [ExerciseConfig] instances. */
    public class Builder {
        private var exerciseType: ExerciseType? = null
        private var dataTypes: Set<DataType>? = null
        private var autoPauseAndResume: Boolean = false
        private var exerciseGoals: List<ExerciseGoal> = emptyList()
        private var exerciseParams: Bundle = Bundle.EMPTY

        /**
         * Sets the active [ExerciseType] the user is performing for this exercise.
         *
         * Provide this parameter when tracking a workout to provide more accurate data. This
         * information can be used to tune sensors, e.g. the calories estimate can take the MET
         * value into account.
         */
        public fun setExerciseType(exerciseType: ExerciseType): Builder {
            this.exerciseType = exerciseType
            return this
        }

        /**
         * Sets the requested [DataType] s that should be tracked during this exercise. If not
         * explicitly called, a default set of [DataType] will be chosen based on the [ ].
         */
        public fun setDataTypes(dataTypes: Set<DataType>): Builder {
            this.dataTypes = dataTypes.toSet()
            return this
        }

        /**
         * Sets whether auto pause and auto resume are enabled for this exercise. If not set,
         * they're disabled by default.
         */
        public fun setAutoPauseAndResume(autoPauseAndResume: Boolean): Builder {
            this.autoPauseAndResume = autoPauseAndResume
            return this
        }

        /**
         * Sets [ExerciseGoal] s specified for this exercise.
         *
         * This is useful to have goals specified before the start of an exercise.
         */
        public fun setExerciseGoals(exerciseGoals: List<ExerciseGoal>): Builder {
            this.exerciseGoals = exerciseGoals.toList()
            return this
        }

        /**
         * Sets additional parameters for current exercise. Supported keys can be found in
         * [ExerciseConfig].
         */
        // TODO(b/180612514) expose keys on a per-OEM basis.
        public fun setExerciseParams(exerciseParams: Bundle): Builder {
            this.exerciseParams = exerciseParams
            return this
        }

        /** Returns the built `ExerciseConfig`. */
        public fun build(): ExerciseConfig {
            return ExerciseConfig(
                checkNotNull(exerciseType) { "No exercise type specified" },
                checkNotNull(dataTypes) { "No data types specified" },
                autoPauseAndResume,
                exerciseGoals,
                exerciseParams
            )
        }
    }

    // TODO(b/180612514): Bundle doesn't have equals, so we need to override the data class default.
    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other is ExerciseConfig) {
            return exerciseType == other.exerciseType &&
                dataTypes == other.dataTypes &&
                autoPauseAndResume == other.autoPauseAndResume &&
                exerciseGoals == other.exerciseGoals &&
                BundlesUtil.equals(exerciseParams, other.exerciseParams)
        }
        return false
    }

    // TODO(b/180612514): Bundle doesn't have hashCode, so we need to override the data class
    // default.
    override fun hashCode(): Int {
        return Objects.hash(
            exerciseType,
            dataTypes,
            autoPauseAndResume,
            exerciseGoals,
            BundlesUtil.hashCode(exerciseParams)
        )
    }

    public companion object {
        @JvmStatic public fun builder(): Builder = Builder()

        @JvmField
        public val CREATOR: Parcelable.Creator<ExerciseConfig> =
            object : Parcelable.Creator<ExerciseConfig> {
                override fun createFromParcel(source: Parcel): ExerciseConfig? {
                    val exerciseType = ExerciseType.fromId(source.readInt())

                    val dataTypesArray = Array<DataType?>(source.readInt()) { null }
                    source.readTypedArray(dataTypesArray, DataType.CREATOR)

                    val autoPauseAndResume = source.readInt() == 1

                    val exerciseGoals = Array<ExerciseGoal?>(source.readInt()) { null }
                    source.readTypedArray(exerciseGoals, ExerciseGoal.CREATOR)

                    val exerciseParams =
                        source.readBundle(ExerciseConfig::class.java.classLoader) ?: Bundle()

                    return ExerciseConfig(
                        exerciseType,
                        dataTypesArray.filterNotNull().toSet(),
                        autoPauseAndResume,
                        exerciseGoals.filterNotNull().toList(),
                        exerciseParams
                    )
                }

                override fun newArray(size: Int): Array<ExerciseConfig?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
