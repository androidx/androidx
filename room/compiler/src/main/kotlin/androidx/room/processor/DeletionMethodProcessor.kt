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
package androidx.room.processor

import androidx.room.Delete
import androidx.room.vo.DeletionMethod
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.DeclaredType

class DeletionMethodProcessor(
    baseContext: Context,
    val containing: DeclaredType,
    val executableElement: ExecutableElement
) {
    val context = baseContext.fork(executableElement)

    fun process(): DeletionMethod {
        val delegate = ShortcutMethodProcessor(context, containing, executableElement)
        val annotation = delegate
            .extractAnnotation(Delete::class, ProcessorErrors.MISSING_DELETE_ANNOTATION)

        val returnType = delegate.extractReturnType()

        val methodBinder = delegate.findDeleteOrUpdateMethodBinder(returnType)

        context.checker.check(
                methodBinder.adapter != null,
                executableElement,
                ProcessorErrors.CANNOT_FIND_DELETE_RESULT_ADAPTER
        )

        val (entities, params) = delegate.extractParams(
            targetEntityType = annotation?.getAsTypeMirror("entity"),
            missingParamError = ProcessorErrors.DELETION_MISSING_PARAMS,
            onValidatePartialEntity = { _, _ -> }
        )

        return DeletionMethod(
                element = delegate.executableElement,
                name = delegate.executableElement.simpleName.toString(),
                entities = entities,
                parameters = params,
                methodBinder = methodBinder
        )
    }
}
