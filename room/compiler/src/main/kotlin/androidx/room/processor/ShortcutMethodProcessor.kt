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

import androidx.room.ext.AnnotationBox
import androidx.room.ext.isEntityElement
import androidx.room.ext.toAnnotationBox
import androidx.room.vo.Entity
import androidx.room.vo.Pojo
import androidx.room.vo.ShortcutEntity
import androidx.room.vo.ShortcutQueryParameter
import androidx.room.vo.findFieldByColumnName
import asTypeElement
import com.google.auto.common.MoreTypes
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import kotlin.reflect.KClass

/**
 * Common functionality for shortcut method processors
 */
class ShortcutMethodProcessor(
    baseContext: Context,
    val containing: DeclaredType,
    val executableElement: ExecutableElement
) {
    val context = baseContext.fork(executableElement)
    private val delegate = MethodProcessorDelegate.createFor(context, containing, executableElement)

    fun <T : Annotation> extractAnnotation(klass: KClass<T>, errorMsg: String): AnnotationBox<T>? {
        val annotation = executableElement.toAnnotationBox(klass)
        context.checker.check(annotation != null, executableElement, errorMsg)
        return annotation
    }

    fun extractReturnType() = delegate.extractReturnType()

    fun extractParams(
        targetEntityType: TypeMirror?,
        missingParamError: String,
        onValidatePartialEntity: (Entity, Pojo) -> Unit
    ): Pair<Map<String, ShortcutEntity>, List<ShortcutQueryParameter>> {
        val params = delegate.extractParams().map {
            ShortcutParameterProcessor(
                baseContext = context,
                containing = containing,
                element = it).process()
        }
        context.checker.check(params.isNotEmpty(), executableElement, missingParamError)

        val targetEntity = if (targetEntityType != null &&
            !MoreTypes.isTypeOf(Any::class.java, targetEntityType)) {
            processEntity(
                element = targetEntityType.asTypeElement(),
                onInvalid = {
                    context.logger.e(executableElement,
                        ProcessorErrors.INVALID_TARGET_ENTITY_IN_SHORTCUT_METHOD)
                    return emptyMap<String, ShortcutEntity>() to emptyList()
                })
        } else {
            null
        }

        val entities = params.filter { it.pojoType != null }.let {
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
        onValidatePartialEntity: (Entity, Pojo) -> Unit
    ) = params.associateBy({ it.name }, { param ->
        if (context.processingEnv.typeUtils.isSameType(targetEntity.type, param.pojoType)) {
            ShortcutEntity(entity = targetEntity, partialEntity = null)
        } else {
            // Target entity and pojo param are not the same, process and validate partial entity.
            val pojo = PojoProcessor.createFor(
                context = context,
                element = param.pojoType!!.asTypeElement(),
                bindingScope = FieldProcessor.BindingScope.BIND_TO_STMT,
                parent = null
            ).process().also { pojo ->
                pojo.fields.filter { targetEntity.findFieldByColumnName(it.columnName) == null }
                    .forEach { context.logger.e(it.element,
                        ProcessorErrors.cannotFindAsEntityField(targetEntity.typeName.toString())) }

                if (pojo.relations.isNotEmpty()) {
                    // TODO: Support Pojos with relations.
                    context.logger.e(pojo.element,
                        ProcessorErrors.INVALID_RELATION_IN_PARTIAL_ENTITY)
                }
                onValidatePartialEntity(targetEntity, pojo)
            }
            ShortcutEntity(entity = targetEntity, partialEntity = pojo)
        }
    })

    private fun extractEntities(params: List<ShortcutQueryParameter>) =
        params.mapNotNull {
            val entity = processEntity(
                element = it.pojoType!!.asTypeElement(),
                onInvalid = {
                    context.logger.e(it.element,
                        ProcessorErrors.CANNOT_FIND_ENTITY_FOR_SHORTCUT_QUERY_PARAMETER)
                    return@mapNotNull null
                })
            it.name to ShortcutEntity(entity = entity!!, partialEntity = null)
        }.toMap()

    private inline fun processEntity(element: TypeElement, onInvalid: () -> Unit) =
        if (element.isEntityElement()) {
            EntityProcessor(
                context = context,
                element = element).process()
        } else {
            onInvalid()
            null
        }

    fun findInsertMethodBinder(
        returnType: TypeMirror,
        params: List<ShortcutQueryParameter>
    ) = delegate.findInsertMethodBinder(returnType, params)

    fun findDeleteOrUpdateMethodBinder(returnType: TypeMirror) =
        delegate.findDeleteOrUpdateMethodBinder(returnType)
}
