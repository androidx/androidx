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

    /** Check if a [DataType] will be aggregated as a statistical value. */
    @JvmStatic
    public fun isStatisticalDataType(dataType: DataType): Boolean =
        dataType.timeType == DataType.TimeType.SAMPLE &&
            dataType.format != Value.FORMAT_DOUBLE_ARRAY

    /**
     * Check if a [DataType] will be aggregated as a cumulative value.
     *
     * Note: [DataType.SWIMMING_LAP_COUNT] already represents the total lap count, so it is not
     * considered a cumulative data type.
     */
    @JvmStatic
    public fun isCumulativeDataType(dataType: DataType): Boolean =
        dataType.timeType == DataType.TimeType.INTERVAL &&
            dataType.format != Value.FORMAT_DOUBLE_ARRAY &&
            dataType != DataType.SWIMMING_LAP_COUNT
}
