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

import com.android.support.room.Entity
import com.android.support.room.ext.hasAnnotation
import com.android.support.room.vo.ShortcutQueryParameter
import com.google.auto.common.MoreTypes
import javax.lang.model.element.VariableElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror

/**
 * Processes parameters of methods that are annotated with Insert, Delete.
 */
class ShortcutParameterProcessor(baseContext : Context,
                                 val containing: DeclaredType,
                                 val element: VariableElement) {
    val context = baseContext.fork(element)
    fun process(): ShortcutQueryParameter {
        val asMember = MoreTypes.asMemberOf(context.processingEnv.typeUtils, containing, element)
        val name = element.simpleName.toString()
        context.checker.check(!name.startsWith("_"), element,
                ProcessorErrors.QUERY_PARAMETERS_CANNOT_START_WITH_UNDERSCORE)

        val (entityType, isMultiple) = extractEntityType(asMember)
        context.checker.check(entityType != null, element,
                ProcessorErrors.CANNOT_FIND_ENTITY_FOR_SHORTCUT_QUERY_PARAMETER)

        return ShortcutQueryParameter(
                name = name,
                type = asMember,
                entityType = entityType,
                isMultiple = isMultiple
        )
    }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    fun extractEntityType(typeMirror: TypeMirror) : Pair<TypeMirror?, Boolean> {
        fun verifyAndPair(entityType: TypeMirror, isMultiple : Boolean)
                : Pair<TypeMirror?, Boolean> {
            if (!MoreTypes.isType(entityType)) {
                return Pair(null, isMultiple)
            }
            val entityElement = MoreTypes.asElement(entityType)
            return if (entityElement.hasAnnotation(Entity::class)) {
                Pair(entityType, isMultiple)
            } else {
                Pair(null, isMultiple)
            }
        }

        if (MoreTypes.isType(typeMirror)
                && (MoreTypes.isTypeOf(java.util.List::class.java, typeMirror)
                || MoreTypes.isTypeOf(java.util.Set::class.java, typeMirror))) {
            val declared = MoreTypes.asDeclared(typeMirror)
            val entity = declared.typeArguments.first()
            return verifyAndPair(entity, true)
        }
        if (typeMirror is ArrayType) {
            val entity = typeMirror.componentType
            return verifyAndPair(entity, true)
        }
        return verifyAndPair(typeMirror, false)
    }
}
