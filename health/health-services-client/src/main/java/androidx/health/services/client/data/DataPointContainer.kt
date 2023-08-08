/*
 * Copyright 2022 The Android Open Source Project
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

/**
 * Container that provides ease of use methods to access [DataPoint]s in a type safe way.
 *
 * Example:
 * ```
 * dataPointContainer.getData(DataType.LOCATION).forEach { location ->
 *   Log.d(TAG, "location = ${location.latitude}, ${location.longitude}")
 * }
 * ```
 */
class DataPointContainer(
    internal val dataPoints: Map<DataType<*, *>, List<DataPoint<*>>>
) {

    /** Constructs a [DataPointContainer] using a list of [DataPoint]s. */
    constructor(dataPointList: List<DataPoint<*>>) : this(
        dataPointList.groupBy { it.dataType }
    )

    /** Set of [DataType]s contained within this [DataPointContainer]. */
    val dataTypes: Set<DataType<*, *>> = dataPoints.keys

    /** Returns all [SampleDataPoint]s contained in this update. */
    val sampleDataPoints: List<SampleDataPoint<*>> get() {
        return dataPoints.flatMap { it.value }.filterIsInstance(SampleDataPoint::class.java)
            .toList()
    }

    /** Returns all [IntervalDataPoint]s contained in this update. */
    val intervalDataPoints: List<IntervalDataPoint<*>> get() {
        return dataPoints.flatMap { it.value }.filterIsInstance(IntervalDataPoint::class.java)
            .toList()
    }

    /** Returns all [CumulativeDataPoint]s contained in this update. */
    val cumulativeDataPoints: List<CumulativeDataPoint<*>> get() {
        return dataPoints.flatMap { it.value }.filterIsInstance(CumulativeDataPoint::class.java)
            .toList()
    }

    /** Returns all [StatisticalDataPoint]s contained in this update. */
    val statisticalDataPoints: List<StatisticalDataPoint<*>> get() {
        return dataPoints.flatMap { it.value }.filterIsInstance(StatisticalDataPoint::class.java)
            .toList()
    }

    /** Returns all [DataPoint] objects with a matching delta [type]. */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any, D : DataPoint<T>> getData(type: DeltaDataType<T, D>): List<D> {
        return dataPoints[type] as? List<D> ?: emptyList()
    }

    /**
     * Returns the [DataPoint] object with a matching aggregate [type], otherwise `null` if exist in
     * this [DataPointContainer].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Number, D : DataPoint<T>> getData(type: AggregateDataType<T, D>): D? {
        return (dataPoints[type] as? List<D>)?.lastOrNull()
    }
}
