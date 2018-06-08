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
import javax.lang.model.type.TypeMirror

/**
 * A code generator that can convert from 1 type to another
 */
abstract class TypeConverter(val from: TypeMirror, val to: TypeMirror) {
    abstract fun convert(inputVarName: String, outputVarName: String, scope: CodeGenScope)
}
