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

package androidx.health.services.client.data.event

import android.os.Parcel
import android.os.Parcelable
import androidx.health.services.client.data.DataPoint
import androidx.health.services.client.data.DataTypeCondition

/** Defines an event that will be triggered when the specified condition is met. */
public data class Event(
    /** [DataTypeCondition] which must be met for the event to be triggered. */
    val dataTypeCondition: DataTypeCondition,
    val triggerType: TriggerType,
) : Parcelable {

    /** Whether or not repeated events should be triggered. */
    public enum class TriggerType(public val id: Int) {
        /** The event will trigger the first time the specified conditions are met. */
        ONCE(1),

        /** The event will trigger each time the specified conditions *become* met. */
        REPEATED(2);

        public companion object {
            @JvmStatic
            public fun fromId(id: Int): TriggerType? = values().firstOrNull { it.id == id }
        }
    }

    /**
     * Does the provided [DataPoint] satisfy the event condition.
     *
     * @throws IllegalArgumentException if the provided data point is not of the same data type as
     * the condition itself.
     */
    public fun isTriggered(dataPoint: DataPoint): Boolean {
        return dataTypeCondition.isSatisfied(dataPoint)
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(dataTypeCondition, flags)
        dest.writeInt(triggerType.id)
    }

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<Event> =
            object : Parcelable.Creator<Event> {
                override fun createFromParcel(source: Parcel): Event? {
                    return Event(
                        source.readParcelable(DataTypeCondition::class.java.classLoader)
                            ?: return null,
                        TriggerType.fromId(source.readInt()) ?: return null
                    )
                }

                override fun newArray(size: Int): Array<Event?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
