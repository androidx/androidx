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

package androidx.camera.integration.camera2.pipe.transformations

import androidx.camera.integration.camera2.pipe.CameraMetadataKey
import androidx.camera.integration.camera2.pipe.StateDetails

object DataTransformationsKeyValue {
    fun convert(key: CameraMetadataKey, keyData: Any?): String? {

        specificTransformationMap[key]?.let {
            return it(keyData)
        }

        return typeTransformation(
            key,
            keyData
        )
    }

    private fun stateTransformation(key: CameraMetadataKey, keyData: Any?): String? {
        val stateMap = StateDetails.intToStringMap[key]
        return if (stateMap != null) stateMap[keyData]
        else nullOrInvalid(
            key,
            keyData
        )
    }

    private val specificTransformationMap: HashMap<CameraMetadataKey, (Any?) -> String?> =
        hashMapOf(
            CameraMetadataKey.CONTROL_AE_MODE to { keyData: Any? ->
                stateTransformation(
                    CameraMetadataKey.CONTROL_AE_MODE,
                    keyData
                )
            },
            CameraMetadataKey.CONTROL_AF_MODE to { keyData: Any? ->
                stateTransformation(
                    CameraMetadataKey.CONTROL_AF_MODE,
                    keyData
                )
            },
            CameraMetadataKey.CONTROL_AWB_MODE to { keyData: Any? ->
                stateTransformation(
                    CameraMetadataKey.CONTROL_AWB_MODE,
                    keyData
                )
            },
            CameraMetadataKey.COLOR_CORRECTION_ABERRATION_MODE to { keyData: Any? ->
                stateTransformation(CameraMetadataKey.COLOR_CORRECTION_ABERRATION_MODE, keyData)
            }
        )

    private fun typeTransformation(key: CameraMetadataKey, keyData: Any?): String? {
        return when (keyData) {
            is Number -> keyData.toString()
            is Boolean -> keyData.toString().uppercase()
            else -> nullOrInvalid(
                key,
                keyData
            )
        }
    }

    private fun nullOrInvalid(key: CameraMetadataKey, keyData: Any?): String? {
        if (keyData == null) return "MISSING DATA"
        throw IllegalArgumentException(
            "keyData of type ${keyData::class.simpleName} for $key is" +
                " not supported"
        )
    }
}