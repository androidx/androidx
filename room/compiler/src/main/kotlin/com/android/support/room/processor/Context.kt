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

import com.android.support.room.ext.LifecyclesTypeNames
import com.android.support.room.ext.RoomTypeNames
import com.android.support.room.log.RLog
import com.android.support.room.preconditions.Checks
import com.android.support.room.solver.TypeAdapterStore
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.type.TypeMirror

data class Context(val processingEnv: ProcessingEnvironment) {
    val logger = RLog(processingEnv)
    val checker = Checks(logger)
    val COMMON_TYPES = CommonTypes(processingEnv)
    val typeAdapterStore by lazy { TypeAdapterStore(this) }

    class CommonTypes(val processingEnv: ProcessingEnvironment) {
        val STRING by lazy {
            processingEnv.elementUtils.getTypeElement("java.lang.String").asType()
        }
        val LIVE_DATA: TypeMirror? by lazy {
            processingEnv.elementUtils.getTypeElement(LifecyclesTypeNames.LIVE_DATA.toString())
                    ?.asType()
        }
        val COMPUTABLE_LIVE_DATA : TypeMirror? by lazy {
            processingEnv.elementUtils.getTypeElement(LifecyclesTypeNames.COMPUTABLE_LIVE_DATA
                    .toString())?.asType()
        }
    }
}
