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

package androidx.room.processor

import androidx.room.ext.findKotlinDefaultImpl
import androidx.room.ext.hasAnyOf
import androidx.room.vo.TransactionMethod
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier.ABSTRACT
import javax.lang.model.element.Modifier.DEFAULT
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.type.DeclaredType

class TransactionMethodProcessor(baseContext: Context,
                                 val containing: DeclaredType,
                                 val executableElement: ExecutableElement) {

    val context = baseContext.fork(executableElement)

    fun process(): TransactionMethod {
        val kotlinDefaultImpl =
                executableElement.findKotlinDefaultImpl(context.processingEnv.typeUtils)
        context.checker.check(
                !executableElement.hasAnyOf(PRIVATE, FINAL)
                        && (!executableElement.hasAnyOf(ABSTRACT) || kotlinDefaultImpl != null),
                executableElement, ProcessorErrors.TRANSACTION_METHOD_MODIFIERS)

        val callType = when {
            executableElement.hasAnyOf(DEFAULT) ->
                TransactionMethod.CallType.DEFAULT_JAVA8
            kotlinDefaultImpl != null ->
                TransactionMethod.CallType.DEFAULT_KOTLIN
            else ->
                TransactionMethod.CallType.CONCRETE
        }

        return TransactionMethod(
                element = executableElement,
                name = executableElement.simpleName.toString(),
                callType = callType)
    }
}
