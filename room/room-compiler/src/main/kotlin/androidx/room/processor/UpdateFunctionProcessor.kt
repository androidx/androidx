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

import androidx.room.OnConflictStrategy
import androidx.room.Update
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XType
import androidx.room.vo.UpdateFunction
import androidx.room.vo.findPropertyByColumnName

class UpdateFunctionProcessor(
    baseContext: Context,
    val containing: XType,
    val executableElement: XMethodElement
) {
    val context = baseContext.fork(executableElement)

    fun process(): UpdateFunction {
        val delegate = ShortcutFunctionProcessor(context, containing, executableElement)
        val annotation =
            delegate.extractAnnotation(Update::class, ProcessorErrors.MISSING_UPDATE_ANNOTATION)

        val onConflict = annotation?.get("onConflict")?.asInt() ?: OnConflictStrategy.ABORT
        context.checker.check(
            onConflict in OnConflictStrategy.NONE..OnConflictStrategy.IGNORE,
            executableElement,
            ProcessorErrors.INVALID_ON_CONFLICT_VALUE
        )

        val (entities, params) =
            delegate.extractParams(
                targetEntityType = annotation?.get("entity")?.asType(),
                missingParamError = ProcessorErrors.UPDATE_MISSING_PARAMS,
                onValidatePartialEntity = { entity, pojo ->
                    val missingPrimaryKeys =
                        entity.primaryKey.properties.filter {
                            pojo.findPropertyByColumnName(it.columnName) == null
                        }
                    context.checker.check(
                        missingPrimaryKeys.isEmpty(),
                        executableElement,
                        ProcessorErrors.missingPrimaryKeysInPartialEntityForUpdate(
                            partialEntityName = pojo.typeName.toString(context.codeLanguage),
                            primaryKeyNames = missingPrimaryKeys.map { it.columnName }
                        )
                    )
                }
            )

        val returnType = delegate.extractReturnType()
        val functionBinder = delegate.findDeleteOrUpdateFunctionBinder(returnType)

        context.checker.check(
            functionBinder.adapter != null,
            executableElement,
            ProcessorErrors.CANNOT_FIND_UPDATE_RESULT_ADAPTER
        )

        return UpdateFunction(
            element = delegate.executableElement,
            entities = entities,
            onConflictStrategy = onConflict,
            functionBinder = functionBinder,
            parameters = params
        )
    }
}
