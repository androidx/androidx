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

import androidx.camera.integration.camera2.pipe.dataholders.GraphDataPoint
import androidx.camera.integration.camera2.pipe.transformations.DataTransformations1D
import androidx.camera.integration.camera2.pipe.transformations.DataTransformationsKeyValue

/** Implementation of the per CameraPipe global listener for CaptureResults */
class DataListener(
    private val dataManager: DataManager,
    private val dataTransformationsKeyValue: DataTransformationsKeyValue,
    private val dataTransformations1D: DataTransformations1D,
    private val dataGenerationBeginTime: Long
) {

    /** Receives CaptureResults and metadata from CameraPipe */
    //    override fun onCompleted(
    //        requestMetadata: RequestMetadata,
    //        frameNumber: FrameNumber,
    //        totalCaptureResult: TotalCaptureResult
    //    ) {
    //
    //          call functions below for each metadata key
    //
    //    }

    /** Calls for keyValueDataHolder update given the new data (will be private in future) */
    fun newKeyValueData(key: CameraMetadataKey, keyData: Any?) {
        transformKeyValueData(key, keyData)?.let { transformedData ->
            dataManager.updateKeyValueDataHolder(key, transformedData)
        }
    }

    /** Calls for graphDataHolder update given the new data (will be private in future) */
    fun newGraphData(
        key: CameraMetadataKey,
        frameNumber: Long,
        timestampNanos: Long,
        keyData: Any?
    ) {
        val timeArrivedNanos = System.nanoTime() - dataGenerationBeginTime

        transformGraphData(key, keyData)?.let { transformedData ->
            val dataPoint =
                GraphDataPoint(frameNumber, timestampNanos, timeArrivedNanos, transformedData)
            dataManager.updateGraphDataHolder(key, dataPoint)
        }
    }

    private fun transformKeyValueData(key: CameraMetadataKey, keyData: Any?) =
        dataTransformationsKeyValue.convert(key, keyData)

    private fun transformGraphData(key: CameraMetadataKey, keyData: Any?) =
        dataTransformations1D.convert(key, keyData)
}
