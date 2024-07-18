/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.room.Upsert
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XType
import androidx.room.vo.UpsertionMethod
import androidx.room.vo.findFieldByColumnName

class UpsertionMethodProcessor(
    baseContext: Context,
    val containing: XType,
    val executableElement: XMethodElement
) {
    val context = baseContext.fork(executableElement)

    fun process(): UpsertionMethod {
        val delegate = ShortcutMethodProcessor(context, containing, executableElement)

        val annotation = delegate.extractAnnotation(
            Upsert::class,
            ProcessorErrors.MISSING_UPSERT_ANNOTATION
        )

        val returnType = delegate.extractReturnType()
        context.checker.notUnbound(
            returnType, executableElement,
            ProcessorErrors.CANNOT_USE_UNBOUND_GENERICS_IN_UPSERTION_METHODS
        )

        val (entities, params) = delegate.extractParams(
            targetEntityType = annotation?.getAsType("entity"),
            missingParamError = ProcessorErrors.UPSERTION_DOES_NOT_HAVE_ANY_PARAMETERS_TO_UPSERT,
            onValidatePartialEntity = { entity, pojo ->
                val missingPrimaryKeys = entity.primaryKey.fields.any {
                    pojo.findFieldByColumnName(it.columnName) == null
                }
                context.checker.check(
                    entity.primaryKey.autoGenerateId || !missingPrimaryKeys,
                    executableElement,
                    ProcessorErrors.missingPrimaryKeysInPartialEntityForUpsert(
                        partialEntityName = pojo.typeName.toString(context.codeLanguage),
                        primaryKeyNames = entity.primaryKey.fields.columnNames
                    )
                )

                // Verify all non null columns without a default value are in the POJO otherwise
                // the UPSERT will fail with a NOT NULL constraint.
                val missingRequiredFields = (entity.fields - entity.primaryKey.fields).filter {
                    it.nonNull && it.defaultValue == null &&
                        pojo.findFieldByColumnName(it.columnName) == null
                }
                context.checker.check(
                    missingRequiredFields.isEmpty(),
                    executableElement,
                    ProcessorErrors.missingRequiredColumnsInPartialEntity(
                        partialEntityName = pojo.typeName.toString(context.codeLanguage),
                        missingColumnNames = missingRequiredFields.map { it.columnName }
                    )
                )
            }
        )

        val methodBinder = delegate.findUpsertMethodBinder(returnType, params)

        context.checker.check(
            methodBinder.adapter != null,
            executableElement,
            ProcessorErrors.CANNOT_FIND_UPSERT_RESULT_ADAPTER
        )

        return UpsertionMethod(
            element = executableElement,
            returnType = returnType,
            entities = entities,
            parameters = params,
            methodBinder = methodBinder
        )
    }
}
