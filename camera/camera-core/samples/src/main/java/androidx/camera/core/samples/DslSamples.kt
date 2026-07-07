/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.core.samples

import android.util.Range
import android.view.Surface
import androidx.annotation.Sampled
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.imageAnalysis
import androidx.camera.core.imageCapture
import androidx.camera.core.preview
import androidx.camera.core.resolutionselector.ResolutionSelector

@Sampled
fun useCaseDslSample() {
    // 1. Using the new Kotlin DSL to configure UseCases in a clean and idiomatic way
    val previewUseCase = preview {
        targetName = "preview_dsl"
        targetRotation = Surface.ROTATION_90
        resolutionSelector = ResolutionSelector.Builder().build()
        isPreviewStabilizationEnabled = true
        targetFrameRate = Range(30, 30)
    }

    val imageCaptureUseCase = imageCapture {
        targetName = "capture_dsl"
        captureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
        flashMode = ImageCapture.FLASH_MODE_AUTO
        jpegQuality = 95
    }

    val imageAnalysisUseCase = imageAnalysis {
        targetName = "analysis_dsl"
        backpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
        imageQueueDepth = 5
    }
}
