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

package androidx.camera.camera2.pipe.testing

import android.hardware.camera2.params.MultiResolutionStreamInfo
import androidx.camera.camera2.pipe.CameraMultiResolutionStreamConfigurationMap
import androidx.camera.camera2.pipe.StreamFormat
import kotlin.reflect.KClass

public class FakeCameraMultiResolutionStreamConfigurationMap(
    private val outputFormats: List<StreamFormat> = emptyList(),
    private val inputFormats: List<StreamFormat> = emptyList(),
    private val outputMultiResStreamFormatsByFormat:
        Map<StreamFormat, List<MultiResolutionStreamInfo>> =
        emptyMap(),
    private val inputMultiResStreamFormatsByFormat:
        Map<StreamFormat, List<MultiResolutionStreamInfo>> =
        emptyMap(),
) : CameraMultiResolutionStreamConfigurationMap {
    override fun getOutputFormats(): List<StreamFormat> = outputFormats.distinct()

    override fun getInputFormats(): List<StreamFormat> = inputFormats.distinct()

    override fun getOutputInfo(format: StreamFormat): List<MultiResolutionStreamInfo> =
        outputMultiResStreamFormatsByFormat[format] ?: emptyList()

    override fun getInputInfo(format: StreamFormat): List<MultiResolutionStreamInfo> =
        inputMultiResStreamFormatsByFormat[format] ?: emptyList()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? {
        return when (type) {
            FakeCameraMultiResolutionStreamConfigurationMap::class -> this as T
            else -> null
        }
    }
}
