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

package androidx.room.vo

import androidx.room.ext.L
import androidx.room.compiler.processing.XType
import com.squareup.javapoet.CodeBlock

data class FieldSetter(val jvmName: String, val type: XType, val callType: CallType) {
    fun writeSet(ownerVar: String, inVar: String, builder: CodeBlock.Builder) {
        val stmt = when (callType) {
            CallType.FIELD -> "$L.$L = $L"
            CallType.METHOD -> "$L.$L($L)"
            CallType.CONSTRUCTOR -> null
        }
        stmt?.let {
            builder.addStatement(stmt, ownerVar, jvmName, inVar)
        }
    }
}
