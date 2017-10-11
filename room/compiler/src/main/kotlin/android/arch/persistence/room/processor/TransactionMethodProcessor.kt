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

package android.arch.persistence.room.processor

import android.arch.persistence.room.ext.hasAnyOf
import android.arch.persistence.room.vo.TransactionMethod
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier.ABSTRACT
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.type.DeclaredType

class TransactionMethodProcessor(baseContext: Context,
                                 val containing: DeclaredType,
                                 val executableElement: ExecutableElement) {

    val context = baseContext.fork(executableElement)

    fun process(): TransactionMethod {
        context.checker.check(!executableElement.hasAnyOf(PRIVATE, FINAL, ABSTRACT),
                executableElement, ProcessorErrors.TRANSACTION_METHOD_MODIFIERS)

        return TransactionMethod(
                element = executableElement,
                name = executableElement.simpleName.toString())
    }
}
