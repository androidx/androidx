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

import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Update
import androidx.room.ext.typeName
import androidx.room.vo.UpdateMethod
import com.squareup.javapoet.TypeName
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.DeclaredType

class UpdateMethodProcessor(
        baseContext: Context,
        val containing: DeclaredType,
        val executableElement: ExecutableElement) {
    val context = baseContext.fork(executableElement)

    fun process(): UpdateMethod {
        val delegate = ShortcutMethodProcessor(context, containing, executableElement)
        val annotation = delegate
                .extractAnnotation(Update::class, ProcessorErrors.MISSING_UPDATE_ANNOTATION)

        val onConflict = OnConflictProcessor.extractFrom(annotation)
        context.checker.check(onConflict <= IGNORE && onConflict >= REPLACE,
                executableElement, ProcessorErrors.INVALID_ON_CONFLICT_VALUE)

        val returnTypeName = delegate.extractReturnType().typeName()
        context.checker.check(
                returnTypeName == TypeName.VOID || returnTypeName == TypeName.INT,
                executableElement,
                ProcessorErrors.UPDATE_METHODS_MUST_RETURN_VOID_OR_INT
        )

        val (entities, params) = delegate.extractParams(
                missingParamError = ProcessorErrors
                        .UPDATE_MISSING_PARAMS
        )

        return UpdateMethod(
                element = delegate.executableElement,
                name = delegate.executableElement.simpleName.toString(),
                entities = entities,
                onConflictStrategy = onConflict,
                returnCount = returnTypeName == TypeName.INT,
                parameters = params
        )
    }
}
