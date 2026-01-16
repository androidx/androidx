/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.core.impl.utils

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Logger
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.core.impl.stabilization.StabilizationMode
import androidx.camera.core.impl.stabilization.VideoStabilization

public object UseCaseUtil {
    private const val TAG = "UseCaseUtil"

    /** Checks if the receiver [UseCase] list contains a video capture use case instance. */
    @JvmStatic
    public fun Collection<UseCase?>.containsVideoCapture(): Boolean {
        forEach { useCase ->
            if (useCase?.isVideoCapture() == true) {
                return true
            }
        }
        return false
    }

    /** Checks if the receiver [UseCase] is a video capture use case instance. */
    @JvmStatic
    public fun UseCase.isVideoCapture(): Boolean {
        if (currentConfig.containsOption(UseCaseConfig.OPTION_CAPTURE_TYPE)) {
            return currentConfig.captureType == UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE
        } else {
            Logger.e(TAG, "$this UseCase does not have capture type.")
        }
        return false
    }

    @JvmStatic
    public fun Collection<UseCase>.getVideoStabilization(
        configProvider: (UseCase) -> UseCaseConfig<*> = { it.currentConfig }
    ): VideoStabilization =
        VideoStabilization.from(
            previewStabilizationMode = getPreviewStabilizationMode(configProvider),
            videoStabilizationMode = getVideoStabilizationMode(configProvider),
        )

    private fun Collection<UseCase>.getPreviewStabilizationMode(
        configProvider: (UseCase) -> UseCaseConfig<*>
    ): Int {
        var previewStabilizationMode = StabilizationMode.UNSPECIFIED

        forEach {
            val useCasePreviewStabilization = configProvider(it).previewStabilizationMode

            if (useCasePreviewStabilization != StabilizationMode.UNSPECIFIED) {
                if (
                    previewStabilizationMode != useCasePreviewStabilization &&
                        previewStabilizationMode != StabilizationMode.UNSPECIFIED
                ) {
                    Logger.w(
                        TAG,
                        "Unexpected configurations: Overwriting current" +
                            " previewStabilizationMode($previewStabilizationMode) with" +
                            " useCasePreviewStabilization($useCasePreviewStabilization)!",
                    )
                }

                previewStabilizationMode = useCasePreviewStabilization
            }
        }

        return previewStabilizationMode
    }

    private fun Collection<UseCase>.getVideoStabilizationMode(
        configProvider: (UseCase) -> UseCaseConfig<*>
    ): Int {
        var videoStabilizationMode = StabilizationMode.UNSPECIFIED

        forEach {
            val useCaseVideoStabilization = configProvider(it).videoStabilizationMode

            if (useCaseVideoStabilization != StabilizationMode.UNSPECIFIED) {
                if (
                    videoStabilizationMode != useCaseVideoStabilization &&
                        videoStabilizationMode != StabilizationMode.UNSPECIFIED
                ) {
                    Logger.w(
                        TAG,
                        "Unexpected configurations: Overwriting current" +
                            " videoStabilizationMode($videoStabilizationMode) with" +
                            " useCaseVideoStabilization($useCaseVideoStabilization)!",
                    )
                }

                videoStabilizationMode = useCaseVideoStabilization
            }
        }

        return videoStabilizationMode
    }

    /**
     * Finds and returns the [androidx.camera.video.VideoCapture] in the given [UseCase] list.
     *
     * @return a UseCase type instance for the VideoCapture when it is found. Returns UseCase
     *   because VideoCapture is not visible in the core module.. Returns `null` when VideoCapture
     *   can't be found.
     */
    @JvmStatic
    public fun Collection<UseCase>.findVideoCapture(): UseCase? = firstOrNull {
        it.isVideoCapture()
    }

    /**
     * Finds and returns the [Preview] in the given UseCase list.
     *
     * @return a [Preview] instance if it can be found. Otherwise, return `null`.
     */
    @JvmStatic
    public fun Collection<UseCase>.findPreview(): Preview? =
        firstOrNull { it is Preview } as? Preview

    /**
     * Finds and returns the [ImageCapture] in the given UseCase list.
     *
     * @return a [ImageCapture] instance if it can be found. Otherwise, return `null`.
     */
    @JvmStatic
    public fun Collection<UseCase>.findImageCapture(): ImageCapture? =
        firstOrNull { it is ImageCapture } as? ImageCapture

    /**
     * Finds and returns the [ImageAnalysis] in the given UseCase list.
     *
     * @return a [ImageAnalysis] instance if it can be found. Otherwise, return `null`.
     */
    @JvmStatic
    public fun Collection<UseCase>.findImageAnalysis(): ImageAnalysis? =
        firstOrNull { it is ImageAnalysis } as? ImageAnalysis
}
