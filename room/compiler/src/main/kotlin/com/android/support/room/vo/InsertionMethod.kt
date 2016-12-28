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

package com.android.support.room.vo

import com.android.support.room.Insert
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.TypeMirror

data class InsertionMethod(val element: ExecutableElement, val name: String,
                           @Insert.OnConflict val onConflict: Int,
                           val entity: Entity?, val returnType: TypeMirror,
                           val insertionType: Type?,
                           val parameters: List<ShortcutQueryParameter>) {
    companion object {
        val INVALID_ON_CONFLICT = 1
    }
    val onConflictText by lazy {
        when(onConflict) {
            Insert.REPLACE -> "REPLACE"
            Insert.ABORT -> "ABORT"
            Insert.FAIL -> "FAIL"
            Insert.IGNORE -> "IGNORE"
            Insert.ROLLBACK -> "ROLLBACK"
            else -> "BAD_CONFLICT_CONSTRAINT"
        }
    }

    fun insertMethodTypeFor(param : ShortcutQueryParameter) : Type {
        return if (insertionType == Type.INSERT_VOID || insertionType == null) {
            Type.INSERT_VOID
        } else if (!param.isMultiple) {
            Type.INSERT_SINGLE_ID
        } else {
            insertionType
        }
    }

    enum class Type(
            // methodName matches EntityInsertionAdapter methods
            val methodName : String) {
        INSERT_VOID("insert"), // return void
        INSERT_SINGLE_ID("insertAndReturnId"), // return long
        INSERT_ID_ARRAY("insertAndReturnIdsArray"), // return long[]
        INSERT_ID_LIST("insertAndReturnIdsList") // return List<Long>
    }
}
