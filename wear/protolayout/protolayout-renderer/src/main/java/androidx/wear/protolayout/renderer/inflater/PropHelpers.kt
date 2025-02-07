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

package androidx.wear.protolayout.renderer.inflater

import android.util.Log
import androidx.wear.protolayout.proto.ColorProto.ColorProp
import androidx.wear.protolayout.proto.DimensionProto.DegreesProp
import androidx.wear.protolayout.proto.DimensionProto.DpProp
import androidx.wear.protolayout.proto.TypesProto.BoolProp
import androidx.wear.protolayout.proto.TypesProto.FloatProp
import androidx.wear.protolayout.proto.TypesProto.StringProp
import androidx.wear.protolayout.renderer.dynamicdata.ProtoLayoutDynamicDataPipeline.PipelineMaker
import java.util.Locale
import java.util.Optional
import java.util.function.Consumer

/** Helpers for handling Prop classes' static and dynamic values. */
internal object PropHelpers {
    const val TAG = "ProtolayoutPropHelpers"

    /** Handles a StringProp. */
    @JvmStatic
    fun handleProp(
        stringProp: StringProp,
        locale: Locale,
        consumer: Consumer<String>,
        posId: String,
        pipelineMaker: Optional<PipelineMaker>,
    ) {
        if (stringProp.hasDynamicValue() && pipelineMaker.isPresent()) {
            try {
                pipelineMaker
                    .get()
                    .addPipelineFor(
                        stringProp.dynamicValue,
                        stringProp.value,
                        locale,
                        posId,
                        consumer
                    )
            } catch (ex: RuntimeException) {
                Log.e(TAG, "Error building pipeline", ex)
                consumer.accept(stringProp.value)
            }
        } else {
            consumer.accept(stringProp.value)
        }
    }

    /** Handles a DegreesProp. */
    @JvmStatic
    fun handleProp(
        degreesProp: DegreesProp,
        consumer: Consumer<Float>,
        posId: String,
        pipelineMaker: Optional<PipelineMaker>,
    ) {
        if (degreesProp.hasDynamicValue() && pipelineMaker.isPresent()) {
            try {
                pipelineMaker.get().addPipelineFor(degreesProp, degreesProp.value, posId, consumer)
            } catch (ex: RuntimeException) {
                Log.e(TAG, "Error building pipeline", ex)
                consumer.accept(degreesProp.value)
            }
        } else {
            consumer.accept(degreesProp.value)
        }
    }

    /** Handles a DpProp. */
    @JvmStatic
    fun handleProp(
        dpProp: DpProp,
        consumer: Consumer<Float>,
        posId: String,
        pipelineMaker: Optional<PipelineMaker>,
    ) = handleProp(dpProp, consumer, consumer, posId, pipelineMaker)

    /** Handles a DpProp. */
    @JvmStatic
    fun handleProp(
        dpProp: DpProp,
        staticValueConsumer: Consumer<Float>,
        dynamicValueConsumer: Consumer<Float>,
        posId: String,
        pipelineMaker: Optional<PipelineMaker>,
    ) {
        if (dpProp.hasDynamicValue() && pipelineMaker.isPresent()) {
            try {
                pipelineMaker
                    .get()
                    .addPipelineFor(dpProp, dpProp.value, posId, dynamicValueConsumer)
            } catch (ex: RuntimeException) {
                Log.e(TAG, "Error building pipeline", ex)
                staticValueConsumer.accept(dpProp.value)
            }
        } else {
            staticValueConsumer.accept(dpProp.value)
        }
    }

    /** Handles a ColorProp. */
    @JvmStatic
    fun handleProp(
        colorProp: ColorProp,
        consumer: Consumer<Int>,
        posId: String,
        pipelineMaker: Optional<PipelineMaker>,
    ) {
        if (colorProp.hasDynamicValue() && pipelineMaker.isPresent()) {
            try {
                pipelineMaker.get().addPipelineFor(colorProp, colorProp.argb, posId, consumer)
            } catch (ex: RuntimeException) {
                Log.e(TAG, "Error building pipeline", ex)
                consumer.accept(colorProp.argb)
            }
        } else {
            consumer.accept(colorProp.argb)
        }
    }

    /** Handles a BoolProp. */
    @JvmStatic
    fun handleProp(
        boolProp: BoolProp,
        consumer: Consumer<Boolean>,
        posId: String,
        pipelineMaker: Optional<PipelineMaker>,
    ) {
        if (boolProp.hasDynamicValue() && pipelineMaker.isPresent()) {
            try {
                pipelineMaker.get().addPipelineFor(boolProp, boolProp.value, posId, consumer)
            } catch (ex: RuntimeException) {
                Log.e(TAG, "Error building pipeline", ex)
                consumer.accept(boolProp.value)
            }
        } else {
            consumer.accept(boolProp.value)
        }
    }

    /** Handles a FloatProp. */
    @JvmStatic
    fun handleProp(
        floatProp: FloatProp,
        consumer: Consumer<Float>,
        posId: String,
        pipelineMaker: Optional<PipelineMaker>,
    ) = handleProp(floatProp, consumer, consumer, posId, pipelineMaker)

    /** Handles a FloatProp. */
    @JvmStatic
    fun handleProp(
        floatProp: FloatProp,
        staticValueConsumer: Consumer<Float>,
        dynamicValueconsumer: Consumer<Float>,
        posId: String,
        pipelineMaker: Optional<PipelineMaker>,
    ) {
        if (floatProp.hasDynamicValue() && pipelineMaker.isPresent()) {
            try {
                pipelineMaker
                    .get()
                    .addPipelineFor(
                        floatProp.dynamicValue,
                        floatProp.value,
                        posId,
                        dynamicValueconsumer
                    )
            } catch (ex: RuntimeException) {
                Log.e(TAG, "Error building pipeline", ex)
                staticValueConsumer.accept(floatProp.value)
            }
        } else {
            staticValueConsumer.accept(floatProp.value)
        }
    }
}
