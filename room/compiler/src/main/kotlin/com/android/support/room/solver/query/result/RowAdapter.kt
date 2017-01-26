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

package com.android.support.room.solver.query.result

import com.android.support.room.processor.Context
import com.android.support.room.solver.CodeGenScope
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror

/**
 * Converts a row of a cursor result into an Entity or a primitive.
 * <p>
 * An instance of this is created for each usage so that it can keep local variables.
 */
abstract class RowAdapter(val out : TypeMirror) {
    /**
     * Receives this at the beginning of the conversion. Can declare variables etc to access later.
     * It should return a function that handles the conversion in the given scope.
     */
    abstract fun init(cursorVarName: String, scope : CodeGenScope) : RowConverter

    abstract fun reportErrors(context: Context, element: Element, suppressedWarnings: Set<String>)

    interface RowConverter {
        fun convert(outVarName : String, cursorVarName : String)
    }
}
