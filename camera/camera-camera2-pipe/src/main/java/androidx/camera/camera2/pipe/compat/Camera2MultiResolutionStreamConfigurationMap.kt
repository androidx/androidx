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

package androidx.camera.camera2.pipe.compat

import android.hardware.camera2.params.MultiResolutionStreamConfigurationMap
import android.hardware.camera2.params.MultiResolutionStreamInfo
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraMultiResolutionStreamConfigurationMap
import androidx.camera.camera2.pipe.StreamFormat
import kotlin.reflect.KClass

/**
 * Implementation of the MultiResolutionStreamConfigurationMap interface using the Camera2 library.
 *
 * This class wraps an instance of
 * [android.hardware.camera2.params.MultiResolutionStreamConfigurationMap].
 *
 * @see CameraMultiResolutionStreamConfigurationMap
 */
@RequiresApi(Build.VERSION_CODES.S)
internal class Camera2MultiResolutionStreamConfigurationMap(
    private val multiResolutionStreamConfigurationMap: MultiResolutionStreamConfigurationMap
) : CameraMultiResolutionStreamConfigurationMap {

    override fun getOutputFormats(): List<StreamFormat> {
        return multiResolutionStreamConfigurationMap.outputFormats.map { StreamFormat(it) }
    }

    override fun getInputFormats(): List<StreamFormat> {
        return multiResolutionStreamConfigurationMap.inputFormats.map { StreamFormat(it) }
    }

    override fun getOutputInfo(format: StreamFormat): List<MultiResolutionStreamInfo> {
        return multiResolutionStreamConfigurationMap.getOutputInfo(format.value).toList()
    }

    override fun getInputInfo(format: StreamFormat): List<MultiResolutionStreamInfo> {
        return multiResolutionStreamConfigurationMap.getInputInfo(format.value).toList()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? {
        return when (type) {
            MultiResolutionStreamConfigurationMap::class ->
                multiResolutionStreamConfigurationMap as T
            Camera2MultiResolutionStreamConfigurationMap::class -> this as T
            else -> null
        }
    }
}
