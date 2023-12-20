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

package androidx.camera.integration.camera2.pipe

import androidx.camera.integration.camera2.pipe.dataholders.GraphDataHolder
import androidx.camera.integration.camera2.pipe.dataholders.GraphDataHolderStateImpl
import androidx.camera.integration.camera2.pipe.dataholders.GraphDataHolderValueImpl
import androidx.camera.integration.camera2.pipe.dataholders.GraphDataPoint
import androidx.camera.integration.camera2.pipe.dataholders.GraphDataSortedRingBuffer
import androidx.camera.integration.camera2.pipe.dataholders.KeyValueDataHolder

/** Manages all the data holders for different types of visualizations */
class DataManager(private val visualizationDefaults: VisualizationDefaults) {

    val keyValueDataHolders: HashMap<CameraMetadataKey, KeyValueDataHolder> = hashMapOf()
    val graphDataHolders: HashMap<CameraMetadataKey, GraphDataHolder> = hashMapOf()

    fun initializeDataHolders() {
        visualizationDefaults.keysVisualizedAsKeyValuePair.forEach {
            keyValueDataHolders[it] = KeyValueDataHolder()
        }

        visualizationDefaults.keysVisualizedAsValueGraph.forEach { key ->
            ValueRanges.absoluteRanges[key]?.let { range ->
                graphDataHolders[key] =
                    GraphDataHolderValueImpl(
                        absoluteMin = range.first,
                        absoluteMax = range.second,
                        graphData = GraphDataSortedRingBuffer()
                    )

                /** Because the top row of any graph view is a key value view*/
                if (!keyValueDataHolders.containsKey(key))
                    keyValueDataHolders[key] = KeyValueDataHolder()
            }
        }

        visualizationDefaults.keysVisualizedAsStateGraph.forEach { key ->
            StateDetails.intToStringMap[key]?.let { map ->
                graphDataHolders[key] = GraphDataHolderStateImpl(map, GraphDataSortedRingBuffer())

                /** Because the top row of any graph view is a key value view*/
                if (!keyValueDataHolders.containsKey(key))
                    keyValueDataHolders[key] = KeyValueDataHolder()
            }
        }
    }

    fun updateKeyValueDataHolder(key: CameraMetadataKey, value: String?) {
        val dataHolder = keyValueDataHolders[key]
        if (dataHolder != null) keyValueDataHolders[key]?.updateValue(value)
        else throw IllegalArgumentException("A key value data holder for $key does not exist.")
    }

    fun updateGraphDataHolder(key: CameraMetadataKey, dataPoint: GraphDataPoint) {
        val dataHolder = graphDataHolders[key]
        if (dataHolder != null) graphDataHolders[key]?.addPoint(dataPoint)
        else throw IllegalArgumentException("A graph data holder for $key does not exist.")
    }
}
