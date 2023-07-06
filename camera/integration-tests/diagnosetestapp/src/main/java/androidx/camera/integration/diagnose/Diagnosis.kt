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

package androidx.camera.integration.diagnose

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.camera.view.LifecycleCameraController
import java.io.File

/**
 * Diagnosis object that runs diagnosis test on device and save result to a file.
 */
class Diagnosis {

    @WorkerThread
    suspend fun diagnose(
        context: Context,
        tasks: List<DiagnosisTask>,
        cameraController: LifecycleCameraController,
        isAggregated: Boolean
    ): File? {

        if (tasks.isEmpty()) {
            return null
        }

        val zipFileName = "diagnose_report_${System.currentTimeMillis()}.zip"
        val dataStore = DataStore(context, zipFileName)

        tasks.forEach { task ->
            task.runDiagnosisTask(cameraController, dataStore, context)
            if (!isAggregated) {
                dataStore.flushTextToTextFile("${task.getTaskName()}")
            }
        }
        if (isAggregated) {
            dataStore.flushTextToTextFile("text_report")
        }

        return dataStore.flushZip()
    }
    companion object {
        private const val TAG = "Diagnosis"
    }
}
