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

import androidx.room.solver.CodeGenScope

/** combines 2 type converters */
class CompositeTypeConverter(val conv1: TypeConverter, val conv2: TypeConverter) :
    TypeConverter(from = conv1.from, to = conv2.to, cost = conv1.cost + conv2.cost) {
    override fun doConvert(inputVarName: String, outputVarName: String, scope: CodeGenScope) {
        val conv1Output = conv1.convert(inputVarName, scope)
        conv2.convert(inputVarName = conv1Output, outputVarName = outputVarName, scope = scope)
    }

    override fun doConvert(inputVarName: String, scope: CodeGenScope): String {
        val conv1Output = conv1.convert(inputVarName = inputVarName, scope = scope)
        return conv2.convert(inputVarName = conv1Output, scope = scope)
    }
}
