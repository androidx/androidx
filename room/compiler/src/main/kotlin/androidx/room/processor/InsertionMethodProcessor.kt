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

@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package androidx.room.processor

import androidx.room.Insert
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.ext.typeName
import androidx.room.vo.InsertionMethod
import androidx.room.vo.findFieldByColumnName
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.DeclaredType

class InsertionMethodProcessor(
    baseContext: Context,
    val containing: DeclaredType,
    val executableElement: ExecutableElement
) {
    val context = baseContext.fork(executableElement)
    fun process(): InsertionMethod {
        val delegate = ShortcutMethodProcessor(context, containing, executableElement)
        val annotation = delegate.extractAnnotation(Insert::class,
                ProcessorErrors.MISSING_INSERT_ANNOTATION)

        val onConflict = annotation?.value?.onConflict ?: OnConflictProcessor.INVALID_ON_CONFLICT
        context.checker.check(onConflict in REPLACE..IGNORE,
                executableElement, ProcessorErrors.INVALID_ON_CONFLICT_VALUE)

        val returnType = delegate.extractReturnType()
        val returnTypeName = returnType.typeName()
        context.checker.notUnbound(returnTypeName, executableElement,
                ProcessorErrors.CANNOT_USE_UNBOUND_GENERICS_IN_INSERTION_METHODS)

        val (entities, params) = delegate.extractParams(
            targetEntityType = annotation?.getAsTypeMirror("entity"),
            missingParamError = ProcessorErrors.INSERTION_DOES_NOT_HAVE_ANY_PARAMETERS_TO_INSERT,
            onValidatePartialEntity = { entity, pojo ->
                val missingPrimaryKeys = entity.primaryKey.fields.any {
                    pojo.findFieldByColumnName(it.columnName) == null
                }
                context.checker.check(
                    entity.primaryKey.autoGenerateId || !missingPrimaryKeys,
                    executableElement,
                    ProcessorErrors.missingPrimaryKeysInPartialEntityForInsert(
                        partialEntityName = pojo.typeName.toString(),
                        primaryKeyNames = entity.primaryKey.fields.columnNames)
                )

                // Verify all non null columns without a default value are in the POJO otherwise
                // the INSERT will fail with a NOT NULL constraint.
                val missingRequiredFields = (entity.fields - entity.primaryKey.fields).filter {
                    it.nonNull && it.defaultValue == null &&
                            pojo.findFieldByColumnName(it.columnName) == null
                }
                context.checker.check(
                    missingRequiredFields.isEmpty(),
                    executableElement,
                    ProcessorErrors.missingRequiredColumnsInPartialEntity(
                        partialEntityName = pojo.typeName.toString(),
                        missingColumnNames = missingRequiredFields.map { it.columnName })
                )
            }
        )

        val methodBinder = delegate.findInsertMethodBinder(returnType, params)

        context.checker.check(
                methodBinder.adapter != null,
                executableElement,
                ProcessorErrors.CANNOT_FIND_INSERT_RESULT_ADAPTER
        )

        return InsertionMethod(
                element = executableElement,
                name = executableElement.simpleName.toString(),
                returnType = returnType,
                entities = entities,
                parameters = params,
                onConflict = onConflict,
                methodBinder = methodBinder
        )
    }
}
