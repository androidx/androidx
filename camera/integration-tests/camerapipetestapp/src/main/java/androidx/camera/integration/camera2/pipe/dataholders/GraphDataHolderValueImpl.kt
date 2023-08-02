/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.integration.camera2.pipe.dataholders

import androidx.camera.integration.camera2.pipe.extensions.compareTo
import androidx.camera.integration.camera2.pipe.extensions.minus

/** Data source for continuous value graph visualizations. */
data class GraphDataHolderValueImpl(
    /**
     * Defines the hard upper and lower bound of the value. Absolute is specified to leave room to
     * keep track of current min and max values as points are added in the future
     */
    private val absoluteMin: Number,
    private val absoluteMax: Number,
    override var graphData: GraphDataSortedRingBuffer
) : GraphDataHolder {

    private val absoluteRange: Number

    init {
        if (absoluteMax::class != absoluteMin::class) throw IllegalArgumentException(
            "Min and max" +
                " values must be of the same type"
        )
        if (absoluteMax <= absoluteMin) throw IllegalArgumentException(
            "Max value must be greater" +
                " than min value"
        )
        this.absoluteRange = absoluteMax - absoluteMin
    }

    fun getMin(): Number = absoluteMin

    fun getMax(): Number = absoluteMax

    fun getRange(): Number = absoluteRange
}
