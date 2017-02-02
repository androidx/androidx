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

package com.android.support.room.solver.types

import com.android.support.room.ext.L
import com.android.support.room.solver.CodeGenScope
import javax.annotation.processing.ProcessingEnvironment

/**
 * int to boolean adapter.
 */
class BoxedBooleanToBoxedIntConverter(processingEnvironment: ProcessingEnvironment) : TypeConverter(
        from = processingEnvironment.elementUtils.getTypeElement("java.lang.Boolean").asType(),
        to = processingEnvironment.elementUtils.getTypeElement("java.lang.Integer").asType()) {
    override fun convertForward(inputVarName: String, outputVarName: String,
                                scope: CodeGenScope) {
        scope.builder().addStatement("$L = $L == null ? null : ($L ? 1 : 0)",
                outputVarName, inputVarName, inputVarName)
    }

    override fun convertBackward(inputVarName: String, outputVarName: String,
                                 scope: CodeGenScope) {
        scope.builder().addStatement("$L = $L == null ? null : $L != 0", outputVarName,
                inputVarName, inputVarName)
    }
}
