/*
 * Copyright (C) 2017 The Android Open Source Project
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

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement

/** Generated when we parse a method annotated with TypeConverter. */
data class CustomTypeConverter(
    val enclosingClass: XTypeElement,
    val isEnclosingClassKotlinObject: Boolean,
    val method: XMethodElement,
    val from: XType,
    val to: XType,
    val isProvidedConverter: Boolean
) {
    val className: XClassName by lazy { enclosingClass.asClassName() }
    val fromTypeName: XTypeName by lazy { from.asTypeName() }
    val toTypeName: XTypeName by lazy { to.asTypeName() }
    val isStatic by lazy { method.isStatic() }

    fun getMethodName(lang: CodeLanguage) =
        when (lang) {
            CodeLanguage.JAVA -> method.jvmName
            CodeLanguage.KOTLIN -> method.name
        }
}
