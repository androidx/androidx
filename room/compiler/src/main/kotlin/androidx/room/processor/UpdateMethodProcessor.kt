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
import androidx.room.vo.UpdateMethod
import androidx.room.vo.findFieldByColumnName
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.DeclaredType

class UpdateMethodProcessor(
    baseContext: Context,
    val containing: DeclaredType,
    val executableElement: ExecutableElement
) {
    val context = baseContext.fork(executableElement)

    fun process(): UpdateMethod {
        val delegate = ShortcutMethodProcessor(context, containing, executableElement)
        val annotation = delegate
                .extractAnnotation(Update::class, ProcessorErrors.MISSING_UPDATE_ANNOTATION)

        val onConflict = annotation?.value?.onConflict ?: OnConflictProcessor.INVALID_ON_CONFLICT
        context.checker.check(onConflict in REPLACE..IGNORE,
                executableElement, ProcessorErrors.INVALID_ON_CONFLICT_VALUE)

        val (entities, params) = delegate.extractParams(
            targetEntityType = annotation?.getAsTypeMirror("entity"),
            missingParamError = ProcessorErrors.UPDATE_MISSING_PARAMS,
            onValidatePartialEntity = { entity, pojo ->
                val missingPrimaryKeys = entity.primaryKey.fields.filter {
                    pojo.findFieldByColumnName(it.columnName) == null
                }
                context.checker.check(missingPrimaryKeys.isEmpty(), executableElement,
                    ProcessorErrors.missingPrimaryKeysInPartialEntityForUpdate(
                        partialEntityName = pojo.typeName.toString(),
                        primaryKeyNames = missingPrimaryKeys.map { it.columnName })
                )
            }
        )

        val returnType = delegate.extractReturnType()
        val methodBinder = delegate.findDeleteOrUpdateMethodBinder(returnType)

        context.checker.check(
                methodBinder.adapter != null,
                executableElement,
                ProcessorErrors.CANNOT_FIND_UPDATE_RESULT_ADAPTER
        )

        return UpdateMethod(
                element = delegate.executableElement,
                name = delegate.executableElement.simpleName.toString(),
                entities = entities,
                onConflictStrategy = onConflict,
                methodBinder = methodBinder,
                parameters = params
        )
    }
}
