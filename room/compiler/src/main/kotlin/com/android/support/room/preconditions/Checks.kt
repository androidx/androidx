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

package com.android.support.room.preconditions

import com.android.support.room.errors.ElementBoundException
import com.android.support.room.ext.hasAnnotation
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeVariableName
import javax.lang.model.element.Element
import kotlin.reflect.KClass

/**
 * Similar to preconditions but element bound.
 */
object Checks {
    fun check(predicate: Boolean, element: Element, errorMsg: String, vararg args: Any) {
        if (!predicate) {
            throw ElementBoundException(element, errorMsg.format(args))
        }
    }

    fun hasAnnotation(element : Element, annotation: KClass<out Annotation>, errorMsg: String,
                      vararg args: Any) {
        if (!element.hasAnnotation(annotation)) {
            throw ElementBoundException(element, errorMsg.format(args))
        }
    }

    fun notUnbound(typeName: TypeName, element: Element, errorMsg : String,
                   vararg args : Any) {
        // TODO support bounds cases like <T extends Foo> T bar()
        Checks.check(typeName !is TypeVariableName, element, errorMsg, args)
        if (typeName is ParameterizedTypeName) {
            typeName.typeArguments.forEach { notUnbound(it, element, errorMsg, args) }
        }
    }

    fun notBlank(value: String?, element: Element, msg: String, vararg args : Any) {
        Checks.check(value != null && value.isNotBlank(), element, msg, args)
    }
}
