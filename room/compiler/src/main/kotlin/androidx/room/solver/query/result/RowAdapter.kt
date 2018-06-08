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

import androidx.room.solver.CodeGenScope
import javax.lang.model.type.TypeMirror

/**
 * Converts a row of a cursor result into an Entity or a primitive.
 * <p>
 * An instance of this is created for each usage so that it can keep local variables.
 */
abstract class RowAdapter(val out: TypeMirror) {
    /**
     * Called when cursor variable is ready, good place to put initialization code.
     */
    open fun onCursorReady(cursorVarName: String, scope: CodeGenScope) {}

    /**
     * Called to convert a single row.
     */
    abstract fun convert(outVarName: String, cursorVarName: String, scope: CodeGenScope)

    /**
     * Called when the cursor is finished. It is important to return null if no operation is
     * necessary so that caller can understand that we can do lazy loading.
     */
    open fun onCursorFinished(): ((scope: CodeGenScope) -> Unit)? = null
}
