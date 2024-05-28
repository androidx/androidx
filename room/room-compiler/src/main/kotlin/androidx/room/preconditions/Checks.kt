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

import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.isTypeVariable
import androidx.room.log.RLog
import kotlin.contracts.contract
import kotlin.reflect.KClass

/**
 * Similar to preconditions but element bound and just logs the error instead of throwing an
 * exception.
 *
 * <p>
 * It is important for processing to continue when some errors happen so that we can generate as
 * much code as possible, leaving only the errors in javac output.
 */
class Checks(private val logger: RLog) {

    fun check(predicate: Boolean, element: XElement, errorMsg: String, vararg args: Any): Boolean {
        if (!predicate) {
            logger.e(element, errorMsg, args)
        }
        return predicate
    }

    fun check(predicate: Boolean, element: XElement, errorMsgProducer: () -> String): Boolean {
        if (!predicate) {
            logger.e(element, errorMsgProducer.invoke())
        }
        return predicate
    }

    fun hasAnnotation(
        element: XElement,
        annotation: KClass<out Annotation>,
        errorMsg: String,
        vararg args: Any
    ): Boolean {
        return if (!element.hasAnnotation(annotation)) {
            logger.e(element, errorMsg, args)
            false
        } else {
            true
        }
    }

    fun notUnbound(type: XType, element: XElement, errorMsg: String, vararg args: Any): Boolean {
        // TODO support bounds cases like <T extends Foo> T bar()
        val failed = check(!type.isTypeVariable(), element, errorMsg, args)
        if (type.typeArguments.isNotEmpty()) {
            val nestedFailure = type.typeArguments.any { notUnbound(it, element, errorMsg, args) }
            return !(failed || nestedFailure)
        }
        return !failed
    }

    fun notBlank(value: String?, element: XElement, msg: String, vararg args: Any): Boolean {
        return check(value != null && value.isNotBlank(), element, msg, args)
    }
}

/**
 * Check if the input is of type [T] returning the input if it is or null if it is not.
 *
 * @param obj the object to be checked
 * @param T the expect type
 */
internal inline fun <reified T> checkTypeOrNull(obj: Any): T? {
    contract { returns() implies (obj is T) }
    return if (obj is T) {
        obj
    } else {
        null
    }
}
