/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.support.room.processor

import com.android.support.room.log.RLog
import com.android.support.room.preconditions.Checks
import com.android.support.room.solver.TypeAdapterStore
import com.android.support.room.solver.types.TypeConverter
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror

data class Context private constructor(val processingEnv: ProcessingEnvironment,
                   val logger : RLog, val typeConverters: List<TypeConverter>) {
    val checker : Checks = Checks(logger)
    val COMMON_TYPES : Context.CommonTypes = Context.CommonTypes(processingEnv)

    val typeAdapterStore by lazy { TypeAdapterStore(this, typeConverters) }

    constructor(processingEnv: ProcessingEnvironment) : this(processingEnv,
            RLog(processingEnv, emptySet(), null), emptyList()) {
    }

    class CommonTypes(val processingEnv: ProcessingEnvironment) {
        val STRING: TypeMirror by lazy {
            processingEnv.elementUtils.getTypeElement("java.lang.String").asType()
        }
    }

    fun fork(element: Element) : Context {
        val suppressedWarnings = SuppressWarningProcessor.getSuppressedWarnings(element)
        val converters = CustomConverterProcessor.findConverters(this, element)
        return Context(processingEnv,
                RLog(processingEnv, logger.suppressedWarnings + suppressedWarnings, element),
                converters + this.typeConverters)
    }
}
