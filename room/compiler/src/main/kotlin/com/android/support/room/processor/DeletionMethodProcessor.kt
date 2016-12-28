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
package com.android.support.room.processor

import com.android.support.room.Delete
import com.android.support.room.ext.typeName
import com.android.support.room.vo.DeletionMethod
import com.squareup.javapoet.TypeName
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.DeclaredType

class DeletionMethodProcessor(val context: Context) {
    val entityProcessor = EntityProcessor(context)

    fun parse(containing: DeclaredType, executableElement: ExecutableElement): DeletionMethod {
        val delegate = ShortcutMethodProcessor(context, containing, executableElement,
                entityProcessor)
        delegate.extractAnnotation(Delete::class, ProcessorErrors.MISSING_DELETE_ANNOTATION)

        val returnTypeName = delegate.extractReturnType().typeName()
        context.checker.check(
                returnTypeName == TypeName.VOID || returnTypeName == TypeName.INT,
                executableElement,
                ProcessorErrors.DELETION_METHODS_MUST_RETURN_VOID_OR_INT
        )

        val (entity, params) = delegate.extractParams(
                missingParamError = ProcessorErrors
                        .DELETION_DOES_NOT_HAVE_ANY_PARAMETERS_TO_DELETE,
                multipleEntitiesError = ProcessorErrors
                        .DELETION_METHOD_PARAMETERS_MUST_HAVE_THE_SAME_ENTITY_TYPE
        )

        return DeletionMethod(
                element = delegate.executableElement,
                name = delegate.executableElement.simpleName.toString(),
                entity = entity,
                returnCount = returnTypeName == TypeName.INT,
                parameters = params
        )
    }
}
