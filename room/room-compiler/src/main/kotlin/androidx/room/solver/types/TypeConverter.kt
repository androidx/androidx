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

package androidx.room.solver.types

import androidx.room.compiler.processing.XType
import androidx.room.ext.L
import androidx.room.ext.T
import androidx.room.solver.CodeGenScope

/**
 * A code generator that can convert from 1 type to another
 */
abstract class TypeConverter(val from: XType, val to: XType) {
    /**
     * Should generate the code that will covert [inputVarName] of type [from] to [outputVarName]
     * of type [to]. This method *should not* declare the [outputVarName] as it is already
     * declared by the caller.
     */
    protected abstract fun doConvert(
        inputVarName: String,
        outputVarName: String,
        scope: CodeGenScope
    )

    /**
     * A type converter can optionally override this method if they can handle the case where
     * they don't need a temporary output variable (e.g. no op conversion or null checks).
     *
     * @return The variable name where the result is saved.
     */
    protected open fun doConvert(
        inputVarName: String,
        scope: CodeGenScope
    ): String {
        val outVarName = scope.getTmpVar()
        scope.builder().apply {
            addStatement("final $T $L", to.typeName, outVarName)
        }
        doConvert(
            inputVarName = inputVarName,
            outputVarName = outVarName,
            scope = scope
        )
        return outVarName
    }

    fun convert(
        inputVarName: String,
        scope: CodeGenScope
    ): String = doConvert(inputVarName, scope)

    fun convert(
        inputVarName: String,
        outputVarName: String,
        scope: CodeGenScope
    ) {
        doConvert(inputVarName, outputVarName, scope)
    }
}
