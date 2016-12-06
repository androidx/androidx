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

package com.android.support.room.processor

import com.android.support.room.Insert
import com.android.support.room.Insert.REPLACE
import com.android.support.room.ext.typeName
import com.android.support.room.vo.InsertionMethod
import com.android.support.room.vo.InsertionMethod.Type
import com.google.auto.common.AnnotationMirrors
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.squareup.javapoet.TypeName
import java.util.List
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeKind.LONG
import javax.lang.model.type.TypeKind.VOID
import javax.lang.model.type.TypeMirror

class InsertionMethodProcessor(val context: Context) {
    val parameterProcessor = InsertionParameterProcessor(context)
    val entityProcessor = EntityProcessor(context)
    fun parse(containing: DeclaredType, executableElement: ExecutableElement): InsertionMethod {
        val asMember = context.processingEnv.typeUtils.asMemberOf(containing, executableElement)
        val executableType = MoreTypes.asExecutable(asMember)

        val annotation = MoreElements.getAnnotationMirror(executableElement,
                Insert::class.java).orNull()
        context.checker.check(annotation != null, executableElement,
                ProcessorErrors.MISSING_INSERT_ANNOTATION)

        val onConflictValue = AnnotationMirrors
                .getAnnotationValue(annotation, "onConflict")
                .value
        val onConflict = try {
            onConflictValue.toString().toInt()
        } catch (ex : NumberFormatException) {
            -1
        }
        context.checker.check(onConflict <= Insert.IGNORE && onConflict >= REPLACE,
                executableElement, ProcessorErrors.INVALID_ON_CONFLICT_VALUE)

        val returnTypeName = TypeName.get(executableType.returnType)
        context.checker.notUnbound(returnTypeName, executableElement,
                ProcessorErrors.CANNOT_USE_UNBOUND_GENERICS_IN_INSERTION_METHODS)

        val params = executableElement.parameters
                .map { parameterProcessor.parse(containing, it) }
        context.checker.check(params.isNotEmpty(), executableElement,
                ProcessorErrors.INSERTION_DOES_NOT_HAVE_ANY_PARAMETERS_TO_INSERT)

        val distinctTypes = params
                .map { it.entityType }
                .filterNotNull()
                .distinctBy { it.typeName() } // TypeName implement equals
        context.checker.check(distinctTypes.size < 2, executableElement,
                ProcessorErrors.INSERTION_METHOD_PARAMETERS_MUST_HAVE_THE_SAME_ENTITY_TYPE)
        val entityTypeMirror = distinctTypes.firstOrNull()
        val entity = if (entityTypeMirror == null) {
            null
        } else {
            entityProcessor.parse(MoreTypes.asTypeElement(entityTypeMirror))
        }
        val returnType = executableType.returnType
        // TODO we can support more types
        val insertionType = getInsertionType(returnType)
        context.checker.check(insertionType != null, executableElement,
                ProcessorErrors.INVALID_INSERTION_METHOD_RETURN_TYPE)
        return InsertionMethod(
                element = executableElement,
                name = executableElement.simpleName.toString(),
                returnType = returnType,
                entity = entity,
                parameters = params,
                onConflict = onConflict,
                insertionType = insertionType
        )
    }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private fun getInsertionType(returnType: TypeMirror): InsertionMethod.Type? {
        // TODO we need to support more types here.
        fun isLongType(typeMirror: TypeMirror) : Boolean {
            return typeMirror.kind == LONG || (MoreTypes.isType(typeMirror) &&
                    MoreTypes.isTypeOf(java.lang.Long::class.java, typeMirror))
        }
        return if (returnType.kind == VOID) {
            Type.INSERT_VOID
        } else if (returnType.kind == TypeKind.ARRAY) {
            val arrayType = MoreTypes.asArray(returnType)
            val param = arrayType.componentType
            if (isLongType(param)) {
                Type.INSERT_ID_ARRAY
            } else {
                null
            }
        } else if (MoreTypes.isType(returnType)
                && MoreTypes.isTypeOf(List::class.java, returnType)) {
            val declared = MoreTypes.asDeclared(returnType)
            val param = declared.typeArguments.first()
            if (isLongType(param)) {
                Type.INSERT_ID_LIST
            } else {
                null
            }
        } else if (isLongType(returnType)) {
            Type.INSERT_SINGLE_ID
        } else {
            null
        }
    }
}
