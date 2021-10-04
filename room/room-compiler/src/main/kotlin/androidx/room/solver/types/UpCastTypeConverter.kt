/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.room.solver.CodeGenScope

/**
 * We have a special class for upcasting types in type converters. (e.g. Int to Number)
 * It is used in the pathfinding to be more expensive than exactly matching calls to prioritize
 * exact matches.
 */
class UpCastTypeConverter(
    upCastFrom: XType,
    upCastTo: XType
) : TypeConverter(
    from = upCastFrom,
    to = upCastTo,
    cost = Cost.UP_CAST
) {
    override fun doConvert(inputVarName: String, outputVarName: String, scope: CodeGenScope) {
        scope.builder().apply {
            addStatement("$L = $L", outputVarName, inputVarName)
        }
    }

    override fun doConvert(inputVarName: String, scope: CodeGenScope): String {
        // normally, we don't need to generate any code here but if the upcast is converting from
        // a primitive to boxed; we need to. Otherwise, output value won't become an object and
        // that might break the rest of the code generation (e.g. checking nullable on primitive)
        return if (to.typeName.isBoxedPrimitive && from.typeName.isPrimitive) {
            super.doConvert(inputVarName, scope)
        } else {
            inputVarName
        }
    }
}