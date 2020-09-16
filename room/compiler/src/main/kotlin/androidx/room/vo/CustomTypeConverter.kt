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

import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XType
import com.squareup.javapoet.TypeName

/**
 * Generated when we parse a method annotated with TypeConverter.
 */
data class CustomTypeConverter(
    val enclosingClass: XType,
    val isEnclosingClassKotlinObject: Boolean,
    val method: XMethodElement,
    val from: XType,
    val to: XType,
    val isProvidedConverter: Boolean
) {
    val typeName: TypeName by lazy { enclosingClass.typeName }
    val fromTypeName: TypeName by lazy { from.typeName }
    val toTypeName: TypeName by lazy { to.typeName }
    val methodName by lazy { method.name }
    val isStatic by lazy { method.isStatic() }
}
