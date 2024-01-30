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

import androidx.camera.integration.camera2.pipe.CameraMetadataKey.LENS_FOCUS_DISTANCE
import androidx.camera.integration.camera2.pipe.CameraMetadataKey.SCALAR_CROP_REGION
import androidx.camera.integration.camera2.pipe.dataholders.GraphDataHolderStateImpl
import androidx.camera.integration.camera2.pipe.dataholders.GraphDataHolderValueImpl
import androidx.camera.integration.camera2.pipe.dataholders.GraphDataPoint
import androidx.camera.integration.camera2.pipe.dataholders.KeyValueDataHolder
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DataManagerTest {

    @Test
    fun initializeDataHolders() {
        val dataManager = DataManager(VisualizationDefaults)
        dataManager.initializeDataHolders()
        val keyValueDataHolders = dataManager.keyValueDataHolders
        val graphDataHolders = dataManager.graphDataHolders

        VisualizationDefaults.keysVisualizedAsKeyValuePair.forEach {
            assertThat(keyValueDataHolders.containsKey(it)).isTrue()
            assertThat(keyValueDataHolders[it] is KeyValueDataHolder).isTrue()
        }

        VisualizationDefaults.keysVisualizedAsValueGraph.forEach {
            assertThat(graphDataHolders.containsKey(it)).isTrue()
            assertThat(graphDataHolders[it] is GraphDataHolderValueImpl).isTrue()
        }

        VisualizationDefaults.keysVisualizedAsStateGraph.forEach {
            assertThat(graphDataHolders.containsKey(it)).isTrue()
            assertThat(graphDataHolders[it] is GraphDataHolderStateImpl).isTrue()
        }
    }

    @Test
    fun updateGraphDataHolder_keyDoesNotExistInGraphDataHolders() {
        val dataManager = DataManager(VisualizationDefaults)
        val dataPoint =
            GraphDataPoint(
                10,
                10,
                10,
                9
            )

        dataManager.initializeDataHolders()
        dataManager.updateGraphDataHolder(LENS_FOCUS_DISTANCE, dataPoint)

        val graphDataHolder = dataManager.graphDataHolders[LENS_FOCUS_DISTANCE]
        assertThat(graphDataHolder).isNotNull()
        val points = graphDataHolder!!.graphData.toList()
        assertThat(points.size).isEqualTo(1)
        assertThat(points[0]).isEqualTo(dataPoint)
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateGraphDataHolder_addGraphDataPoint() {
        val dataManager = DataManager(VisualizationDefaults)
        val dataPoint =
            GraphDataPoint(
                10,
                10,
                10,
                9
            )

        dataManager.initializeDataHolders()
        dataManager.updateGraphDataHolder(SCALAR_CROP_REGION, dataPoint)
    }
}
