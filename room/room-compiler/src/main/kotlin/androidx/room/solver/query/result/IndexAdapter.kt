/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.room.vo.ColumnIndexVar

/**
 * Creates the index variables used by [RowAdapter]s from a cursor.
 *
 * Most row adapters know how to find the indices needed and provide an implementation of this
 * interface via [RowAdapter.getDefaultIndexAdapter].
 */
interface IndexAdapter {

    /**
     * Called when the cursor variable is ready.
     */
    fun onCursorReady(cursorVarName: String, scope: CodeGenScope)

    fun getIndexVars(): List<ColumnIndexVar>
}
