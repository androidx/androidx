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

/** Helper class to facilitate working with [DataTypes] [DataType]. */
public object DataTypes {
    private val AGGREGATE_TYPE_TO_RAW_TYPE =
        DataTypeBiMap(
            DataType.AGGREGATE_DISTANCE to DataType.DISTANCE,
            DataType.AGGREGATE_CALORIES_EXPENDED to DataType.TOTAL_CALORIES,
            DataType.AGGREGATE_STEP_COUNT to DataType.STEPS,
            DataType.AGGREGATE_ELEVATION to DataType.ELEVATION,
            DataType.AGGREGATE_FLOORS to DataType.FLOORS,
            DataType.AGGREGATE_SWIMMING_STROKE_COUNT to DataType.SWIMMING_STROKES
        )

    private val MAX_TYPE_TO_RAW_TYPE =
        DataTypeBiMap(
            DataType.MAX_HEART_RATE_BPM to DataType.HEART_RATE_BPM,
            DataType.MAX_PACE to DataType.PACE,
            DataType.MAX_SPEED to DataType.SPEED
        )

    private val AVERAGE_TYPE_TO_RAW_TYPE =
        DataTypeBiMap(
            DataType.AVERAGE_HEART_RATE_BPM to DataType.HEART_RATE_BPM,
            DataType.AVERAGE_PACE to DataType.PACE,
            DataType.AVERAGE_SPEED to DataType.SPEED
        )

    /** Check if a [DataType] represents aggregate value of a collection of non-aggregate data. */
    @JvmStatic
    public fun isAggregateDataType(dataType: DataType): Boolean =
        AGGREGATE_TYPE_TO_RAW_TYPE.map.containsKey(dataType)

    /** Check if a [DataType] represents the maximum value of a collection of non-aggregate data. */
    @JvmStatic
    public fun isStatisticalMaxDataType(dataType: DataType): Boolean =
        MAX_TYPE_TO_RAW_TYPE.map.containsKey(dataType)

    /** Check if a [DataType] represents the average value of a collection of non-aggregate data. */
    @JvmStatic
    public fun isStatisticalAverageDataType(dataType: DataType): Boolean =
        AVERAGE_TYPE_TO_RAW_TYPE.map.containsKey(dataType)

    /**
     * Check if a [DataType] represents raw data value, i.e., neither aggregate value nor
     * statistical value.
     */
    @JvmStatic
    public fun isRawType(dataType: DataType): Boolean =
        !isAggregateDataType(dataType) &&
            !isStatisticalMaxDataType(dataType) &&
            !isStatisticalAverageDataType(dataType)

    /** Get the aggregate [DataType] from a raw [DataType], or null if it doesn't exist. */
    @JvmStatic
    public fun getAggregateTypeFromRawType(rawType: DataType): DataType? =
        AGGREGATE_TYPE_TO_RAW_TYPE.inverse[rawType]

    /** Get the raw [DataType] from an aggregate [DataType], or null if it doesn't exist. */
    @JvmStatic
    public fun getRawTypeFromAggregateType(aggregateType: DataType): DataType? =
        AGGREGATE_TYPE_TO_RAW_TYPE.map[aggregateType]

    /** Get the max [DataType] from a raw [DataType], or null if it doesn't exist. */
    @JvmStatic
    public fun getMaxTypeFromRawType(rawType: DataType): DataType? =
        MAX_TYPE_TO_RAW_TYPE.inverse[rawType]

    /** Get the raw [DataType] from a max [DataType], or null if it doesn't exist. */
    @JvmStatic
    public fun getRawTypeFromMaxType(maxType: DataType): DataType? =
        MAX_TYPE_TO_RAW_TYPE.map[maxType]

    /** Get the average [DataType] from a raw [DataType], or null if it doesn't exist. */
    @JvmStatic
    public fun getAverageTypeFromRawType(rawType: DataType): DataType? =
        AVERAGE_TYPE_TO_RAW_TYPE.inverse[rawType]

    /** Get the raw [DataType] from an average [DataType], or null if it doesn't exist. */
    @JvmStatic
    public fun getRawTypeFromAverageType(averageType: DataType): DataType? =
        AVERAGE_TYPE_TO_RAW_TYPE.map[averageType]

    /** Get the aggregate, average, and max [DataType] from a raw [DataType] if they exist. */
    @JvmStatic
    public fun getAggregatedDataTypesFromRawType(rawType: DataType): Set<DataType> {
        val allDataTypes = HashSet<DataType>()

        getAggregateTypeFromRawType(rawType)?.let { allDataTypes.add(it) }
        getMaxTypeFromRawType(rawType)?.let { allDataTypes.add(it) }
        getAverageTypeFromRawType(rawType)?.let { allDataTypes.add(it) }

        return allDataTypes
    }

    private class DataTypeBiMap(vararg pairs: Pair<DataType, DataType>) {
        val map = mapOf(*pairs)
        val inverse = pairs.map { it.second to it.first }.toMap()
    }
}
