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

import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.processing.XType

data class FieldGetter(val jvmName: String, val type: XType, val callType: CallType) {
    fun writeGet(ownerVar: String, outVar: String, builder: XCodeBlock.Builder) {
        val stmt = when (callType) {
            CallType.FIELD -> "%L.%L"
            CallType.METHOD -> "%L.%L()"
            CallType.CONSTRUCTOR -> null
        }
        if (stmt != null) {
            builder.addLocalVariable(
                name = outVar,
                typeName = type.asTypeName(),
                assignExpr = XCodeBlock.of(builder.language, stmt, ownerVar, jvmName)
            )
        }
    }
}
