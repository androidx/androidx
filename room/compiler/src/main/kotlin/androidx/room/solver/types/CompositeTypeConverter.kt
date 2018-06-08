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

import androidx.room.ext.L
import androidx.room.ext.T
import androidx.room.ext.typeName
import androidx.room.solver.CodeGenScope

/**
 * combines 2 type converters
 */
class CompositeTypeConverter(val conv1: TypeConverter, val conv2: TypeConverter) : TypeConverter(
        conv1.from, conv2.to) {
    override fun convert(inputVarName: String, outputVarName: String, scope: CodeGenScope) {
        scope.builder().apply {
            val tmp = scope.getTmpVar()
            addStatement("final $T $L", conv1.to.typeName(), tmp)
            conv1.convert(inputVarName, tmp, scope)
            conv2.convert(tmp, outputVarName, scope)
        }
    }
}
