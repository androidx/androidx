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
import androidx.room.solver.CodeGenScope
import javax.lang.model.type.TypeMirror

/**
 * Yes, we need this when user input is the same as the desired output.
 * <p>
 * Each query parameter receives an adapter that converts it into a String (or String[]). This
 * TypeAdapter basically serves as a wrapper for converting String parameter into the String[] of
 * the query. Not having this would require us to special case handle String, String[], List<String>
 * etc.
 */
class NoOpConverter(type: TypeMirror) : TypeConverter(
        type, type) {
    override fun convert(inputVarName: String, outputVarName: String, scope: CodeGenScope) {
        scope.builder()
                .addStatement("$L = $L", outputVarName, inputVarName)
    }
}
