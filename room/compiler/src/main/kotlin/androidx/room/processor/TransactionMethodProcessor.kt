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

import androidx.room.ext.GuavaUtilConcurrentTypeNames
import androidx.room.ext.LifecyclesTypeNames
import androidx.room.ext.RxJava2TypeNames
import androidx.room.ext.RxJava3TypeNames
import androidx.room.ext.findKotlinDefaultImpl
import androidx.room.ext.findTypeMirror
import androidx.room.ext.isAbstract
import androidx.room.ext.isJavaDefault
import androidx.room.ext.isOverrideableIgnoringContainer
import androidx.room.ext.name
import androidx.room.vo.TransactionMethod
import erasure
import isAssignableFrom
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.DeclaredType

class TransactionMethodProcessor(
    baseContext: Context,
    val containing: DeclaredType,
    val executableElement: ExecutableElement
) {

    val context = baseContext.fork(executableElement)

    fun process(): TransactionMethod {
        val delegate = MethodProcessorDelegate.createFor(context, containing, executableElement)
        val typeUtils = context.processingEnv.typeUtils
        val kotlinDefaultImpl = executableElement.findKotlinDefaultImpl(typeUtils)
        context.checker.check(
                executableElement.isOverrideableIgnoringContainer() &&
                        (!executableElement.isAbstract() || kotlinDefaultImpl != null),
                executableElement, ProcessorErrors.TRANSACTION_METHOD_MODIFIERS)

        val returnType = delegate.extractReturnType()
        val erasureReturnType = returnType.erasure(typeUtils)

        DEFERRED_TYPES.firstOrNull { className ->
            context.processingEnv.findTypeMirror(className)?.let {
                erasureReturnType.isAssignableFrom(typeUtils, it)
            } ?: false
        }?.let { returnTypeName ->
            context.logger.e(
                ProcessorErrors.transactionMethodAsync(returnTypeName.toString()),
                executableElement
            )
        }

        val callType = when {
            executableElement.isJavaDefault() ->
                TransactionMethod.CallType.DEFAULT_JAVA8
            kotlinDefaultImpl != null ->
                TransactionMethod.CallType.DEFAULT_KOTLIN
            else ->
                TransactionMethod.CallType.CONCRETE
        }

        return TransactionMethod(
                element = executableElement,
                returnType = returnType,
                parameterNames = delegate.extractParams().map { it.name },
                callType = callType,
                methodBinder = delegate.findTransactionMethodBinder(callType))
    }

    companion object {
        val DEFERRED_TYPES = listOf(
            LifecyclesTypeNames.LIVE_DATA,
            RxJava2TypeNames.FLOWABLE,
            RxJava2TypeNames.OBSERVABLE,
            RxJava2TypeNames.MAYBE,
            RxJava2TypeNames.SINGLE,
            RxJava2TypeNames.COMPLETABLE,
            RxJava3TypeNames.FLOWABLE,
            RxJava3TypeNames.OBSERVABLE,
            RxJava3TypeNames.MAYBE,
            RxJava3TypeNames.SINGLE,
            RxJava3TypeNames.COMPLETABLE,
            GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE)
    }
}
