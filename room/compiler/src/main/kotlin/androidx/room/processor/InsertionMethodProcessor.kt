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

import androidx.annotation.VisibleForTesting
import androidx.room.Insert
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.vo.InsertionMethod
import androidx.room.vo.InsertionMethod.Type
import androidx.room.vo.ShortcutQueryParameter
import com.google.auto.common.MoreTypes
import com.squareup.javapoet.TypeName
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeKind.LONG
import javax.lang.model.type.TypeKind.VOID
import javax.lang.model.type.TypeMirror

class InsertionMethodProcessor(baseContext: Context,
                               val containing: DeclaredType,
                               val executableElement: ExecutableElement) {
    val context = baseContext.fork(executableElement)
    fun process(): InsertionMethod {
        val delegate = ShortcutMethodProcessor(context, containing, executableElement)
        val annotation = delegate.extractAnnotation(Insert::class,
                ProcessorErrors.MISSING_INSERT_ANNOTATION)

        val onConflict = OnConflictProcessor.extractFrom(annotation)
        context.checker.check(onConflict <= IGNORE && onConflict >= REPLACE,
                executableElement, ProcessorErrors.INVALID_ON_CONFLICT_VALUE)

        val returnType = delegate.extractReturnType()
        val returnTypeName = TypeName.get(returnType)
        context.checker.notUnbound(returnTypeName, executableElement,
                ProcessorErrors.CANNOT_USE_UNBOUND_GENERICS_IN_INSERTION_METHODS)

        val (entities, params) = delegate.extractParams(
                missingParamError = ProcessorErrors
                        .INSERTION_DOES_NOT_HAVE_ANY_PARAMETERS_TO_INSERT
        )

        // TODO we can support more types
        var insertionType = getInsertionType(returnType)
        context.checker.check(insertionType != null, executableElement,
                ProcessorErrors.INVALID_INSERTION_METHOD_RETURN_TYPE)

        if (insertionType != null) {
            val acceptable = acceptableTypes(params)
            if (insertionType !in acceptable) {
                context.logger.e(executableElement,
                        ProcessorErrors.insertionMethodReturnTypeMismatch(
                                insertionType.returnTypeName,
                                acceptable.map { it.returnTypeName }))
                // clear it, no reason to generate code for it.
                insertionType = null
            }
        }
        return InsertionMethod(
                element = executableElement,
                name = executableElement.simpleName.toString(),
                returnType = returnType,
                entities = entities,
                parameters = params,
                onConflict = onConflict,
                insertionType = insertionType
        )
    }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private fun getInsertionType(returnType: TypeMirror): InsertionMethod.Type? {
        // TODO we need to support more types here.
        fun isLongPrimitiveType(typeMirror: TypeMirror) = typeMirror.kind == LONG

        fun isLongBoxType(typeMirror: TypeMirror) =
                MoreTypes.isType(typeMirror) &&
                        MoreTypes.isTypeOf(java.lang.Long::class.java, typeMirror)

        fun isLongType(typeMirror: TypeMirror) =
                isLongPrimitiveType(typeMirror) || isLongBoxType(typeMirror)

        return if (returnType.kind == VOID) {
            Type.INSERT_VOID
        } else if (returnType.kind == TypeKind.ARRAY) {
            val arrayType = MoreTypes.asArray(returnType)
            val param = arrayType.componentType
            if (isLongPrimitiveType(param)) {
                Type.INSERT_ID_ARRAY
            } else if (isLongBoxType(param)) {
                Type.INSERT_ID_ARRAY_BOX
            } else {
                null
            }
        } else if (MoreTypes.isType(returnType)
                && MoreTypes.isTypeOf(List::class.java, returnType)) {
            val declared = MoreTypes.asDeclared(returnType)
            val param = declared.typeArguments.first()
            if (isLongBoxType(param)) {
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

    companion object {
        @VisibleForTesting
        val VOID_SET by lazy { setOf(Type.INSERT_VOID) }
        @VisibleForTesting
        val SINGLE_ITEM_SET by lazy { setOf(Type.INSERT_VOID, Type.INSERT_SINGLE_ID) }
        @VisibleForTesting
        val MULTIPLE_ITEM_SET by lazy {
            setOf(Type.INSERT_VOID, Type.INSERT_ID_ARRAY, Type.INSERT_ID_ARRAY_BOX,
                    Type.INSERT_ID_LIST)
        }
        fun acceptableTypes(params: List<ShortcutQueryParameter>): Set<InsertionMethod.Type> {
            if (params.isEmpty()) {
                return VOID_SET
            }
            if (params.size > 1) {
                return VOID_SET
            }
            if (params.first().isMultiple) {
                return MULTIPLE_ITEM_SET
            } else {
                return SINGLE_ITEM_SET
            }
        }
    }
}
