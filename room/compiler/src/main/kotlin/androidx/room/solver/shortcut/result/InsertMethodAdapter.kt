/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.room.solver.shortcut.result

import androidx.room.ext.KotlinTypeNames
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.T
import androidx.room.ext.typeName
import androidx.room.solver.CodeGenScope
import androidx.room.vo.ShortcutQueryParameter
import androidx.room.writer.DaoWriter
import com.google.auto.common.MoreTypes
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

/**
 * Class that knows how to generate an insert method body.
 */
class InsertMethodAdapter private constructor(private val insertionType: InsertionType) {
    companion object {
        fun create(
            returnType: TypeMirror,
            params: List<ShortcutQueryParameter>
        ): InsertMethodAdapter? {
            val insertionType = getInsertionType(returnType)
            if (insertionType != null && isInsertValid(insertionType, params)) {
                return InsertMethodAdapter(insertionType)
            }
            return null
        }

        private fun isInsertValid(
            insertionType: InsertionType?,
            params: List<ShortcutQueryParameter>
        ): Boolean {
            if (insertionType == null) {
                return false
            }
            if (params.isEmpty() || params.size > 1) {
                return insertionType == InsertionType.INSERT_VOID ||
                        insertionType == InsertionType.INSERT_UNIT
            }
            return if (params.first().isMultiple) {
                insertionType in MULTIPLE_ITEM_SET
            } else {
                insertionType == InsertionType.INSERT_VOID ||
                        insertionType == InsertionType.INSERT_VOID_OBJECT ||
                        insertionType == InsertionType.INSERT_UNIT ||
                        insertionType == InsertionType.INSERT_SINGLE_ID
            }
        }

        private val MULTIPLE_ITEM_SET by lazy {
            setOf(InsertionType.INSERT_VOID,
                    InsertionType.INSERT_VOID_OBJECT,
                    InsertionType.INSERT_UNIT,
                    InsertionType.INSERT_ID_ARRAY,
                    InsertionType.INSERT_ID_ARRAY_BOX,
                    InsertionType.INSERT_ID_LIST)
        }

        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        private fun getInsertionType(returnType: TypeMirror): InsertionType? {

            fun isLongPrimitiveType(typeMirror: TypeMirror) = typeMirror.kind == TypeKind.LONG

            fun isLongBoxType(typeMirror: TypeMirror) =
                    MoreTypes.isType(typeMirror) &&
                            MoreTypes.isTypeOf(java.lang.Long::class.java, typeMirror)

            fun isLongType(typeMirror: TypeMirror) =
                    isLongPrimitiveType(typeMirror) || isLongBoxType(typeMirror)

            fun isList(typeMirror: TypeMirror) = MoreTypes.isType(typeMirror) &&
                    MoreTypes.isTypeOf(List::class.java, typeMirror)

            fun isVoidObject(typeMirror: TypeMirror) = MoreTypes.isType(typeMirror) &&
                    MoreTypes.isTypeOf(Void::class.java, typeMirror)

            fun isKotlinUnit(typeMirror: TypeMirror) = MoreTypes.isType(typeMirror) &&
                    MoreTypes.isTypeOf(Unit::class.java, typeMirror)

            return if (returnType.kind == TypeKind.VOID) {
                InsertionType.INSERT_VOID
            } else if (isVoidObject(returnType)) {
                InsertionType.INSERT_VOID_OBJECT
            } else if (isKotlinUnit(returnType)) {
                InsertionType.INSERT_UNIT
            } else if (returnType.kind == TypeKind.ARRAY) {
                val arrayType = MoreTypes.asArray(returnType)
                val param = arrayType.componentType
                when {
                    isLongPrimitiveType(param) -> InsertionType.INSERT_ID_ARRAY
                    isLongBoxType(param) -> InsertionType.INSERT_ID_ARRAY_BOX
                    else -> null
                }
            } else if (isList(returnType)) {
                val declared = MoreTypes.asDeclared(returnType)
                val param = declared.typeArguments.first()
                if (isLongBoxType(param)) {
                    InsertionType.INSERT_ID_LIST
                } else {
                    null
                }
            } else if (isLongType(returnType)) {
                InsertionType.INSERT_SINGLE_ID
            } else {
                null
            }
        }
    }

    fun createInsertionMethodBody(
        parameters: List<ShortcutQueryParameter>,
        insertionAdapters: Map<String, Pair<FieldSpec, TypeSpec>>,
        scope: CodeGenScope
    ) {
        scope.builder().apply {
            // TODO assert thread
            // TODO collect results
            addStatement("$N.beginTransaction()", DaoWriter.dbField)
            val needsResultVar = insertionType != InsertionType.INSERT_VOID &&
                    insertionType != InsertionType.INSERT_VOID_OBJECT &&
                    insertionType != InsertionType.INSERT_UNIT
            val resultVar = if (needsResultVar) {
                scope.getTmpVar("_result")
            } else {
                null
            }

            beginControlFlow("try").apply {
                parameters.forEach { param ->
                    val insertionAdapter = insertionAdapters[param.name]?.first
                    if (needsResultVar) {
                        // if it has more than 1 parameter, we would've already printed the error
                        // so we don't care about re-declaring the variable here
                        addStatement("$T $L = $N.$L($L)",
                                insertionType.returnTypeName, resultVar,
                                insertionAdapter, insertionType.methodName,
                                param.name)
                    } else {
                        addStatement("$N.$L($L)", insertionAdapter, insertionType.methodName,
                                param.name)
                    }
                }
                addStatement("$N.setTransactionSuccessful()",
                        DaoWriter.dbField)
                if (needsResultVar) {
                    addStatement("return $L", resultVar)
                } else if (insertionType == InsertionType.INSERT_VOID_OBJECT) {
                    addStatement("return null")
                } else if (insertionType == InsertionType.INSERT_UNIT) {
                    addStatement("return $T.INSTANCE", KotlinTypeNames.UNIT)
                }
            }
            nextControlFlow("finally").apply {
                addStatement("$N.endTransaction()",
                        DaoWriter.dbField)
            }
            endControlFlow()
        }
    }

    enum class InsertionType(
            // methodName matches EntityInsertionAdapter methods
        val methodName: String,
        val returnTypeName: TypeName
    ) {
        INSERT_VOID("insert", TypeName.VOID), // return void
        INSERT_VOID_OBJECT("insert", TypeName.VOID), // return void
        INSERT_UNIT("insert", KotlinTypeNames.UNIT), // return kotlin.Unit.INSTANCE
        INSERT_SINGLE_ID("insertAndReturnId", TypeName.LONG), // return long
        INSERT_ID_ARRAY("insertAndReturnIdsArray",
                ArrayTypeName.of(TypeName.LONG)), // return long[]
        INSERT_ID_ARRAY_BOX("insertAndReturnIdsArrayBox",
                ArrayTypeName.of(TypeName.LONG.box())), // return Long[]
        INSERT_ID_LIST("insertAndReturnIdsList", // return List<Long>
                ParameterizedTypeName.get(List::class.typeName(), TypeName.LONG.box())),
    }
}