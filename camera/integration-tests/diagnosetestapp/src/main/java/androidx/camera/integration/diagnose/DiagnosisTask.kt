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
import androidx.camera.view.LifecycleCameraController

/**
 * Abstract DiagnosisTask for running custom task through diagnose in {@link Diagnosis} on device
 * and store results to a {@link DataStore}.
 */
abstract class DiagnosisTask(private val name: String) {
    fun getTaskName(): String {
        return name
    }

    /**
     *  Called by diagnose function in {@link Diagnosis} to execute tasks implemented by children.
     *  Children can use {@link LifecycleCameraController} to access CameraX features on the device
     *  and use @{link DataStore} to save results to a report file.
     */
    abstract suspend fun runDiagnosisTask(
        cameraController: LifecycleCameraController,
        dataStore: DataStore,
        context: Context
    )
}
