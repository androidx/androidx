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

package androidx.room.preconditions

import androidx.room.ext.hasAnnotation
import androidx.room.log.RLog
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeVariableName
import javax.lang.model.element.Element
import kotlin.reflect.KClass

/**
 * Similar to preconditions but element bound and just logs the error instead of throwing an
 * exception.
 * <p>
 * It is important for processing to continue when some errors happen so that we can generate as
 * much code as possible, leaving only the errors in javac output.
 */
class Checks(private val logger: RLog) {

    fun check(predicate: Boolean, element: Element, errorMsg: String, vararg args: Any): Boolean {
        if (!predicate) {
            logger.e(element, errorMsg, args)
        }
        return predicate
    }

    fun hasAnnotation(element: Element, annotation: KClass<out Annotation>, errorMsg: String,
                      vararg args: Any): Boolean {
        return if (!element.hasAnnotation(annotation)) {
            logger.e(element, errorMsg, args)
            false
        } else {
            true
        }
    }

    fun notUnbound(typeName: TypeName, element: Element, errorMsg: String,
                   vararg args: Any): Boolean {
        // TODO support bounds cases like <T extends Foo> T bar()
        val failed = check(typeName !is TypeVariableName, element, errorMsg, args)
        if (typeName is ParameterizedTypeName) {
            val nestedFailure = typeName.typeArguments
                    .any { notUnbound(it, element, errorMsg, args) }
            return !(failed || nestedFailure)
        }
        return !failed
    }

    fun notBlank(value: String?, element: Element, msg: String, vararg args: Any): Boolean {
        return check(value != null && value.isNotBlank(), element, msg, args)
    }
}
