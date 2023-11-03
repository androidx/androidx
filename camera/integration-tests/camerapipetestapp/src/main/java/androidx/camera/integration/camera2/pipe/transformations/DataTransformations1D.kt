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

import android.hardware.camera2.params.Face
import androidx.camera.integration.camera2.pipe.CameraMetadataKey
import androidx.camera.integration.camera2.pipe.CameraMetadataKey.CONTROL_AE_MODE
import androidx.camera.integration.camera2.pipe.CameraMetadataKey.LENS_FOCUS_DISTANCE
import androidx.camera.integration.camera2.pipe.CameraMetadataKey.STATISTICS_FACES

/**
 * Transforms camera metadata into numbers to be visualized as 1D graphics
 */
object DataTransformations1D {

    /**
     * Tries to apply a specific transformation. If one exists, the resulting Number is returned.
     * If none exist, we try to apply a type based transformation. If one exists, the resulting
     * Number is returned. Otherwise, an error expressing the transformation is not supported is
     * thrown.
     */
    fun convert(key: CameraMetadataKey, keyData: Any?): Number? {

        specificTransformationMap[key]?.let {
            return it(keyData)
        }

        return typeTransformation(key, keyData)
    }

    /**
     * When converting data, this is the first transformation we will try to apply. It maps specific
     * keys to a lambda function that can transform the corresponding data into a number
     */
    private val specificTransformationMap: HashMap<CameraMetadataKey, (Any?) -> Number?> =
        hashMapOf(
            /**
             * LENS_FOCUS_DISTANCE has a specific transformation that maps Float inputs to their
             * reciprocals, maps null input to null, and throws an IllegalArgumentException if the input
             * is not of the expected type Float?
             */
            LENS_FOCUS_DISTANCE to { keyData: Any? ->
                when (keyData) {
                    is Float -> 1 / keyData
                    else -> nullOrInvalid(LENS_FOCUS_DISTANCE, keyData)
                }
            },

            /**
             * CONTROL_AE_MODE has a specific transformation that upon an Int input returns that input
             * as is since each Int corresponds to a mode (The strings describing these modes will be
             * passed as a map into the data source for the visualization directly). Upon a null input,
             * null will be returned, and upon any other input, an exception will be thrown
             */
            CONTROL_AE_MODE to { keyData: Any? ->
                when (keyData) {
                    is Int -> keyData
                    else -> nullOrInvalid(CONTROL_AE_MODE, keyData)
                }
            },

            /**
             * STATISTICS_FACES has a specific transformation that maps Array<Face> inputs to array
             * size of that input, maps null input to null, and throws an IllegalArgumentException if
             * the input is not an Array, or is an Array of the wrong type
             */
            STATISTICS_FACES to { keyData: Any? ->
                when (keyData) {
                    is Array<*> -> {
                        if (keyData.isArrayOf<Face>()) {
                            keyData.size
                        } else {
                            throw IllegalArgumentException(
                                "keyData for $STATISTICS_FACES expected " +
                                    "to be Array<Face>, but was ${keyData::class.simpleName}"
                            )
                        }
                    }
                    else -> nullOrInvalid(STATISTICS_FACES, keyData)
                }
            }
        )

    /**
     * When converting data, this function will be called only if the camera metadata key has no
     * specific transformation defined in the specificTransformationMap above. This function
     * converts keyData based on its type, and throws an exception if the type is not supported
     */
    private fun typeTransformation(key: CameraMetadataKey, keyData: Any?): Number? {
        return when (keyData) {
            is Number -> keyData
            else -> nullOrInvalid(key, keyData)
        }
    }

    /**
     * This function is called when keyData doesn't match any valid type. If keyData is null,
     * null is returned, but if keyData is some other type, we throw an Illegal Argument
     * Exception explaining transformation is not supported for the keyData's type
     */
    private fun nullOrInvalid(key: CameraMetadataKey, keyData: Any?): Number? {
        if (keyData == null) return null
        throw IllegalArgumentException(
            "keyData of type ${keyData::class.simpleName} for $key is" +
                " not supported"
        )
    }
}
