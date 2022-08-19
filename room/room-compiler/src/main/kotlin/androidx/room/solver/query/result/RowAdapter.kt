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

package androidx.room.solver.query.result

import androidx.room.compiler.processing.XType
import androidx.room.solver.CodeGenScope
import androidx.room.vo.ColumnIndexVar

/**
 * Converts a row of a cursor result into an object or a primitive.
 *
 * An instance of this is created for each usage so that it can keep local variables.
 */
abstract class RowAdapter(val out: XType) {

    /**
     * Called when cursor variable along with column indices variables are ready.
     *
     * @param indices the list of index variables to use when getting columns from the cursor to
     * convert the row.
     * @param cursorVarName the name of the cursor local variable
     */
    open fun onCursorReady(
        cursorVarName: String,
        scope: CodeGenScope,
        indices: List<ColumnIndexVar> =
            getDefaultIndexAdapter().apply { onCursorReady(cursorVarName, scope) }.getIndexVars(),
    ) {
    }

    /**
     * Called to convert a single row.
     */
    abstract fun convert(outVarName: String, cursorVarName: String, scope: CodeGenScope)

    /**
     * Gets the default index adapter for the implementation
     */
    abstract fun getDefaultIndexAdapter(): IndexAdapter
}
