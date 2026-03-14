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
package androidx.room3.processor

import androidx.room3.compiler.processing.XAnnotation
import androidx.room3.compiler.processing.XMethodElement
import androidx.room3.compiler.processing.XType
import androidx.room3.compiler.processing.XTypeElement
import androidx.room3.ext.isEntityElement
import androidx.room3.vo.DataClass
import androidx.room3.vo.Entity
import androidx.room3.vo.ShortcutEntity
import androidx.room3.vo.ShortcutQueryParameter
import androidx.room3.vo.findPropertyByColumnName
import kotlin.reflect.KClass

/** Common functionality for shortcut function processors */
class ShortcutFunctionProcessor(
    baseContext: Context,
    val containing: XType,
    val executableElement: XMethodElement,
) {
    val context = baseContext.fork(executableElement)
    private val delegate =
        FunctionProcessorDelegate.createFor(context, containing, executableElement)

    fun <T : Annotation> extractAnnotation(klass: KClass<T>, errorMsg: String): XAnnotation? {
        val annotation = executableElement.getAnnotation(klass)
        context.checker.check(annotation != null, executableElement, errorMsg)
        return annotation
    }

    fun extractReturnType(): XType {
        val returnType = delegate.extractReturnType()
        val returnsDeferredType = delegate.returnsDeferredType()
        val isSuspendFunction = delegate.executableElement.isSuspendFunction()
        context.checker.check(
            !isSuspendFunction || !returnsDeferredType,
            executableElement,
            ProcessorErrors.suspendReturnsDeferredType(returnType.rawType.typeName.toString()),
        )
        return returnType
    }

    fun extractParams(
        targetEntityType: XType?,
        missingParamError: String,
        onValidatePartialEntity: (Entity, DataClass) -> Unit,
    ): Pair<Map<String, ShortcutEntity>, List<ShortcutQueryParameter>> {
        val params =
            delegate.extractParams().map {
                ShortcutParameterProcessor(
                        baseContext = context,
                        containing = containing,
                        element = it,
                    )
                    .process()
            }
        context.checker.check(params.isNotEmpty(), executableElement, missingParamError)

        val targetEntity =
            if (targetEntityType != null && !targetEntityType.isTypeOf(Any::class)) {
                val targetTypeElement = targetEntityType.typeElement
                if (targetTypeElement == null) {
                    context.logger.e(
                        executableElement,
                        ProcessorErrors.INVALID_TARGET_ENTITY_IN_SHORTCUT_FUNCTION,
                    )
                    null
                } else {
                    processEntity(
                        element = targetTypeElement,
                        onInvalid = {
                            context.logger.e(
                                executableElement,
                                ProcessorErrors.INVALID_TARGET_ENTITY_IN_SHORTCUT_FUNCTION,
                            )
                            return emptyMap<String, ShortcutEntity>() to emptyList()
                        },
                    )
                }
            } else {
                null
            }

        val entities =
            params
                .filter { it.dataClassType != null }
                .let {
                    if (targetEntity != null) {
                        extractPartialEntities(targetEntity, it, onValidatePartialEntity)
                    } else {
                        extractEntities(it)
                    }
                }

        return Pair(entities, params)
    }

    private fun extractPartialEntities(
        targetEntity: Entity,
        params: List<ShortcutQueryParameter>,
        onValidatePartialEntity: (Entity, DataClass) -> Unit,
    ) =
        params.associateBy(
            { it.name },
            { param ->
                if (targetEntity.type.isSameType(param.dataClassType!!)) {
                    ShortcutEntity(entity = targetEntity, partialEntity = null)
                } else {
                    // Target entity and data class param are not the same, process and validate
                    // partial entity.
                    val dataClassTypeElement = param.dataClassType.typeElement
                    val dataClass =
                        if (dataClassTypeElement == null) {
                            context.logger.e(
                                targetEntity.element,
                                ProcessorErrors.shortcutFunctionArgumentMustBeAClass(
                                    typeName =
                                        param.dataClassType
                                            .asTypeName()
                                            .toString(context.codeLanguage)
                                ),
                            )
                            null
                        } else {
                            DataClassProcessor.createFor(
                                    context = context,
                                    element = dataClassTypeElement,
                                    bindingScope = PropertyProcessor.BindingScope.BIND_TO_STMT,
                                    parent = null,
                                )
                                .process()
                                .also { dataClass ->
                                    dataClass.properties
                                        .filter {
                                            targetEntity.findPropertyByColumnName(it.columnName) ==
                                                null
                                        }
                                        .forEach {
                                            context.logger.e(
                                                it.element,
                                                ProcessorErrors.cannotFindAsEntityProperty(
                                                    targetEntity.typeName.toString(
                                                        context.codeLanguage
                                                    )
                                                ),
                                            )
                                        }

                                    if (dataClass.relations.isNotEmpty()) {
                                        // TODO: Support data classes with relations.
                                        context.logger.e(
                                            dataClass.element,
                                            ProcessorErrors.INVALID_RELATION_IN_PARTIAL_ENTITY,
                                        )
                                    }

                                    if (dataClass.properties.isEmpty()) {
                                        context.logger.e(
                                            executableElement,
                                            ProcessorErrors.noColumnsInPartialEntity(
                                                partialEntityName =
                                                    dataClass.typeName.toString(
                                                        context.codeLanguage
                                                    )
                                            ),
                                        )
                                    }
                                    onValidatePartialEntity(targetEntity, dataClass)
                                }
                        }
                    ShortcutEntity(entity = targetEntity, partialEntity = dataClass)
                }
            },
        )

    private fun extractEntities(params: List<ShortcutQueryParameter>) =
        params
            .mapNotNull {
                val entityTypeElement = it.dataClassType?.typeElement
                if (entityTypeElement == null) {
                    context.logger.e(
                        it.element,
                        ProcessorErrors.CANNOT_FIND_ENTITY_FOR_SHORTCUT_QUERY_PARAMETER,
                    )
                    null
                } else {
                    val entity =
                        processEntity(
                            element = entityTypeElement,
                            onInvalid = {
                                context.logger.e(
                                    it.element,
                                    ProcessorErrors.CANNOT_FIND_ENTITY_FOR_SHORTCUT_QUERY_PARAMETER,
                                )
                                return@mapNotNull null
                            },
                        )
                    it.name to ShortcutEntity(entity = entity!!, partialEntity = null)
                }
            }
            .toMap()

    private inline fun processEntity(element: XTypeElement, onInvalid: () -> Unit) =
        if (element.isEntityElement()) {
            EntityProcessor(context = context, element = element).process()
        } else {
            onInvalid()
            null
        }

    fun findInsertFunctionBinder(returnType: XType, params: List<ShortcutQueryParameter>) =
        delegate.findInsertFunctionBinder(returnType, params)

    fun findUpsertFunctionBinder(returnType: XType, params: List<ShortcutQueryParameter>) =
        delegate.findUpsertFunctionBinder(returnType, params)

    fun findDeleteOrUpdateFunctionBinder(returnType: XType) =
        delegate.findDeleteOrUpdateFunctionBinder(returnType)
}
