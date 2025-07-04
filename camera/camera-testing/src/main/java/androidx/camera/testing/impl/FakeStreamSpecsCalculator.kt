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

package androidx.camera.testing.impl

import android.util.Range
import android.util.Size
import androidx.camera.core.DynamicRange
import androidx.camera.core.Logger
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CameraConfig
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED
import androidx.camera.core.impl.stabilization.StabilizationMode
import androidx.camera.core.internal.StreamSpecQueryResult
import androidx.camera.core.internal.StreamSpecsCalculator

/** A fake [StreamSpecsCalculator] implementation for testing purposes. */
public class FakeStreamSpecsCalculator : StreamSpecsCalculator {
    private val supportedStreamSpecs = mutableListOf<ExtendedStreamSpec>()

    /**
     * Adds an [ExtendedStreamSpec] as a supported stream spec.
     *
     * The added stream specs are matched with provided use cases to determine the supported stream
     * specs map when [calculateSuggestedStreamSpecs] is called.
     */
    public fun addSupportedStreamSpecs(vararg streamSpecs: ExtendedStreamSpec) {
        supportedStreamSpecs.addAll(streamSpecs)
    }

    override fun calculateSuggestedStreamSpecs(
        cameraMode: Int,
        cameraInfoInternal: CameraInfoInternal,
        newUseCases: List<UseCase>,
        attachedUseCases: List<UseCase>,
        cameraConfig: CameraConfig,
        sessionType: Int,
        targetFrameRate: Range<Int>,
        isFeatureComboInvocation: Boolean,
        findMaxSupportedFrameRate: Boolean,
    ): StreamSpecQueryResult {
        Logger.d(TAG, "calculateSuggestedStreamSpecs: supportedStreamSpecs = $supportedStreamSpecs")

        val streamSpecs = mutableMapOf<UseCase, ExtendedStreamSpec>()

        (attachedUseCases + newUseCases).forEach { useCase ->
            val useCaseConfig = useCase.mergeConfigs(cameraInfoInternal, null, null)

            val supportedSpec =
                supportedStreamSpecs.find { streamSpec ->
                    if (streamSpec.dynamicRange != useCaseConfig.dynamicRange) {
                        return@find false
                    }

                    if (
                        streamSpec.expectedFrameRateRange !=
                            useCaseConfig.getTargetFrameRate(FRAME_RATE_RANGE_UNSPECIFIED)
                    ) {
                        return@find false
                    }

                    if (streamSpec.imageFormat != useCaseConfig.inputFormat) {
                        return@find false
                    }

                    if (
                        streamSpec.previewStabilizationMode !=
                            useCaseConfig.previewStabilizationMode
                    ) {
                        return@find false
                    }

                    // TODO: Check other options like zslDisabled, implementationOptions etc.

                    return@find true
                }

            if (supportedSpec == null) {
                val logMsg =
                    "No supported stream spec found for useCase = $useCase with " +
                        "dynamicRange = ${useCaseConfig.dynamicRange}" +
                        ", fpsRange = " +
                        "${useCaseConfig.getTargetFrameRate(FRAME_RATE_RANGE_UNSPECIFIED)}" +
                        ", imageFormat = ${useCaseConfig.inputFormat}" +
                        ", previewStabilizationMode = ${useCaseConfig.previewStabilizationMode}"

                Logger.d(TAG, logMsg)
                throw IllegalArgumentException(logMsg)
            } else {
                streamSpecs[useCase] = supportedSpec
            }
        }

        return StreamSpecQueryResult(streamSpecs, MAX_SUPPORTED_FRAME_RATE)
    }

    /**
     * A [StreamSpec] that is extended with additional properties for testing purposes.
     *
     * @property dynamicRange The dynamic range of the stream spec.
     * @property expectedFrameRateRange The expected frame rate range of the stream spec.
     * @property imageFormat The image format of the stream spec, e.g.
     *   [android.graphics.ImageFormat.JPEG].
     * @property previewStabilizationMode The preview stabilization mode of the stream spec, based
     *   on [StabilizationMode.Mode].
     */
    public data class ExtendedStreamSpec(
        private val dynamicRange: DynamicRange,
        private val expectedFrameRateRange: Range<Int>,
        public val imageFormat: Int,
        @StabilizationMode.Mode public val previewStabilizationMode: Int,
    ) : StreamSpec() {
        override fun getResolution(): Size {
            return resolution
        }

        override fun getOriginalConfiguredResolution(): Size {
            return resolution
        }

        override fun getDynamicRange(): DynamicRange {
            return dynamicRange
        }

        override fun getSessionType(): Int {
            return sessionType
        }

        override fun getExpectedFrameRateRange(): Range<Int> {
            return expectedFrameRateRange
        }

        override fun getImplementationOptions(): Config? {
            // TODO: Add implementation options to include the StreamUseCase option
            return null
        }

        override fun getZslDisabled(): Boolean {
            // TODO: Add ZSL disabled option
            return false
        }

        override fun toBuilder(): Builder {
            return builder(resolution).apply {
                setDynamicRange(dynamicRange)
                setExpectedFrameRateRange(expectedFrameRateRange)
                setOriginalConfiguredResolution(resolution)
                setSessionType(sessionType)
                setZslDisabled(zslDisabled)

                val implementationOptions = implementationOptions // snapshot
                if (implementationOptions != null) {
                    setImplementationOptions(implementationOptions)
                }
            }
        }
    }

    public companion object {
        private const val TAG = "FakeStreamSpecsCalculator"
        private const val MAX_SUPPORTED_FRAME_RATE = Int.MAX_VALUE
    }
}
