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

import android.app.PendingIntent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import java.util.Objects

/** Configs for the automatic exercise detection. */
public data class AutoExerciseConfig
@JvmOverloads
constructor(
    /**
     * Set of [ExerciseType] for WHS to detect from. If left empty, all possible types are used].
     */
    val exercisesToDetect: Set<ExerciseType> = emptySet(),
    /**
     * A [PendingIntent] that WHS will use to post state / data changes to the app if the app has no
     * active [androidx.health.services.client.ExerciseUpdateListener] registered at the moment. The
     * launchIntent will be fed with the updated exercise states collected by the automatic exercise
     * detection for the app to consume as soon as it re-starts.
     */
    val launchIntent: PendingIntent? = null,

    /** See [ExerciseConfig.exerciseParams]. */
    val exerciseParams: Bundle = Bundle(),
) : Parcelable {

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(exercisesToDetect.size)
        dest.writeIntArray(exercisesToDetect.map { it.id }.toIntArray())
        dest.writeParcelable(launchIntent, flags)
        dest.writeBundle(exerciseParams)
    }

    // TODO(b/180612514): Bundle doesn't have equals, so we need to override the data class default.
    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other is AutoExerciseConfig) {
            return (
                launchIntent == other.launchIntent &&
                    BundlesUtil.equals(exerciseParams, other.exerciseParams) &&
                    exercisesToDetect == other.exercisesToDetect
                )
        }
        return false
    }

    // TODO(b/180612514): Bundle doesn't have hashCode, so we need to override the data class
    // default.
    override fun hashCode(): Int {
        return Objects.hash(launchIntent, BundlesUtil.hashCode(exerciseParams), exercisesToDetect)
    }

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<AutoExerciseConfig> =
            object : Parcelable.Creator<AutoExerciseConfig> {
                override fun createFromParcel(source: Parcel): AutoExerciseConfig? {
                    val exercisesIntArray = IntArray(source.readInt())
                    source.readIntArray(exercisesIntArray)
                    val launchIntent =
                        source.readParcelable<PendingIntent>(PendingIntent::class.java.classLoader)
                    val exerciseParams =
                        source.readBundle(AutoExerciseConfig::class.java.classLoader) ?: return null

                    return AutoExerciseConfig(
                        exercisesIntArray.map { ExerciseType.fromId(it) }.toSet(),
                        launchIntent,
                        exerciseParams
                    )
                }

                override fun newArray(size: Int): Array<AutoExerciseConfig?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
