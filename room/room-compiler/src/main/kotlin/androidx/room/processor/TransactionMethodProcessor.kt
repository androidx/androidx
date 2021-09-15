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
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.ext.KotlinTypeNames
import androidx.room.vo.TransactionMethod

class TransactionMethodProcessor(
    baseContext: Context,
    val containingElement: XTypeElement,
    val containingType: XType,
    val executableElement: XMethodElement
) {

    val context = baseContext.fork(executableElement)

    fun process(): TransactionMethod {
        val delegate = MethodProcessorDelegate.createFor(context, containingType, executableElement)
        val hasKotlinDefaultImpl = executableElement.hasKotlinDefaultImpl()
        context.checker.check(
            executableElement.isOverrideableIgnoringContainer() &&
                (!executableElement.isAbstract() || hasKotlinDefaultImpl),
            executableElement, ProcessorErrors.TRANSACTION_METHOD_MODIFIERS
        )

        val returnType = delegate.extractReturnType()
        val rawReturnType = returnType.rawType

        DEFERRED_TYPES.firstOrNull { className ->
            context.processingEnv.findType(className)?.let {
                rawReturnType.isAssignableFrom(it)
            } ?: false
        }?.let { returnTypeName ->
            context.logger.e(
                ProcessorErrors.transactionMethodAsync(returnTypeName.toString()),
                executableElement
            )
        }

        val callType = when {
            executableElement.isJavaDefault() ->
                if (containingElement.isInterface()) {
                    // if the dao is an interface, call via the Dao interface
                    TransactionMethod.CallType.DEFAULT_JAVA8
                } else {
                    // if the dao is an abstract class, call via the class itself
                    TransactionMethod.CallType.INHERITED_DEFAULT_JAVA8
                }
            hasKotlinDefaultImpl ->
                TransactionMethod.CallType.DEFAULT_KOTLIN
            else ->
                TransactionMethod.CallType.CONCRETE
        }

        return TransactionMethod(
            element = executableElement,
            returnType = returnType,
            parameterNames = delegate.extractParams().map { it.name },
            callType = callType,
            methodBinder = delegate.findTransactionMethodBinder(callType)
        )
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
            GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE,
            KotlinTypeNames.FLOW
        )
    }
}
