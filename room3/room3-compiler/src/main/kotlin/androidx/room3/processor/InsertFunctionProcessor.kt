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

package androidx.room3.processor

import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.compiler.processing.XMethodElement
import androidx.room3.compiler.processing.XType
import androidx.room3.solver.shortcut.binder.InstantInsertOrUpsertFunctionBinder
import androidx.room3.vo.InsertFunction
import androidx.room3.vo.findPropertyByColumnName

class InsertFunctionProcessor(
    baseContext: Context,
    val containing: XType,
    val executableElement: XMethodElement,
) {
    val context = baseContext.fork(executableElement)

    fun process(): InsertFunction {
        val delegate = ShortcutFunctionProcessor(context, containing, executableElement)
        val annotation =
            delegate.extractAnnotation(Insert::class, ProcessorErrors.MISSING_INSERT_ANNOTATION)

        val onConflict = annotation?.get("onConflict")?.asInt() ?: OnConflictStrategy.ABORT
        context.checker.check(
            onConflict in OnConflictStrategy.NONE..OnConflictStrategy.IGNORE,
            executableElement,
            ProcessorErrors.INVALID_ON_CONFLICT_VALUE,
        )

        val returnType = delegate.extractReturnType()
        context.checker.notUnbound(
            returnType,
            executableElement,
            ProcessorErrors.CANNOT_USE_UNBOUND_GENERICS_IN_INSERT_FUNCTIONS,
        )

        val (entities, params) =
            delegate.extractParams(
                targetEntityType = annotation?.get("entity")?.asType(),
                missingParamError = ProcessorErrors.INSERT_DOES_NOT_HAVE_ANY_PARAMETERS_TO_INSERT,
                onValidatePartialEntity = { entity, dataClass ->
                    val missingPrimaryKeys =
                        entity.primaryKey.properties.any {
                            dataClass.findPropertyByColumnName(it.columnName) == null
                        }
                    context.checker.check(
                        entity.primaryKey.autoGenerateId || !missingPrimaryKeys,
                        executableElement,
                        ProcessorErrors.missingPrimaryKeysInPartialEntityForInsert(
                            partialEntityName = dataClass.typeName.toString(context.codeLanguage),
                            primaryKeyNames = entity.primaryKey.properties.columnNames,
                        ),
                    )

                    // Verify all non null columns without a default value are in the data class
                    // otherwise the INSERT will fail with a NOT NULL constraint.
                    val missingRequiredProperties =
                        (entity.properties - entity.primaryKey.properties).filter {
                            it.nonNull &&
                                it.defaultValue == null &&
                                dataClass.findPropertyByColumnName(it.columnName) == null
                        }
                    context.checker.check(
                        missingRequiredProperties.isEmpty(),
                        executableElement,
                        ProcessorErrors.missingRequiredColumnsInPartialEntity(
                            partialEntityName = dataClass.typeName.toString(context.codeLanguage),
                            missingColumnNames = missingRequiredProperties.map { it.columnName },
                        ),
                    )
                },
            )

        val functionBinder = delegate.findInsertFunctionBinder(returnType, params)
        if (
            functionBinder is InstantInsertOrUpsertFunctionBinder && !context.isAndroidOnlyTarget()
        ) {
            // A blocking function that does not return a deferred return type is not allowed
            // if the target platforms include non-Android targets.
            context.logger.e(
                executableElement,
                ProcessorErrors.INVALID_BLOCKING_DAO_FUNCTION_NON_ANDROID,
            )
        }

        context.checker.check(
            functionBinder.adapter != null,
            executableElement,
            ProcessorErrors.CANNOT_FIND_INSERT_RESULT_ADAPTER,
        )

        return InsertFunction(
            element = executableElement,
            returnType = returnType,
            entities = entities,
            parameters = params,
            onConflict = onConflict,
            functionBinder = functionBinder,
        )
    }
}
