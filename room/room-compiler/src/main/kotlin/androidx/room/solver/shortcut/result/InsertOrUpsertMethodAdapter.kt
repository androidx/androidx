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

package androidx.room.solver.shortcut.result

import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.isArray
import androidx.room.compiler.processing.isKotlinUnit
import androidx.room.compiler.processing.isLong
import androidx.room.compiler.processing.isVoid
import androidx.room.compiler.processing.isVoidObject
import androidx.room.ext.KotlinTypeNames
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.T
import androidx.room.ext.typeName
import androidx.room.processor.Context
import androidx.room.processor.ProcessorErrors
import androidx.room.solver.CodeGenScope
import androidx.room.vo.ShortcutQueryParameter
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName

class InsertOrUpsertMethodAdapter private constructor(private val methodType: MethodType) {
    companion object {
        fun createInsert(
            context: Context,
            returnType: XType,
            params: List<ShortcutQueryParameter>
        ): InsertOrUpsertMethodAdapter? {
            return createMethod(
                context,
                returnType,
                params,
                ::InsertMethodType,
                ProcessorErrors.INSERT_MULTI_PARAM_SINGLE_RETURN_MISMATCH,
                ProcessorErrors.INSERT_SINGLE_PARAM_MULTI_RETURN_MISMATCH
            )
        }

        fun createUpsert(
            context: Context,
            returnType: XType,
            params: List<ShortcutQueryParameter>
        ): InsertOrUpsertMethodAdapter? {
            return createMethod(
                context,
                returnType,
                params,
                ::UpsertMethodType,
                ProcessorErrors.UPSERT_MULTI_PARAM_SINGLE_RETURN_MISMATCH,
                ProcessorErrors.UPSERT_SINGLE_PARAM_MULTI_RETURN_MISMATCH
            )
        }

        private fun createMethod(
            context: Context,
            returnType: XType,
            params: List<ShortcutQueryParameter>,
            methodTypeClass: (returnType: ReturnType) -> MethodType,
            multiParamSingleReturnError: String,
            singleParamMultiReturnError: String
        ): InsertOrUpsertMethodAdapter? {
            val methodReturnType = getReturnType(returnType)
            if (methodReturnType != null &&
                isReturnValid(
                    context,
                    methodReturnType,
                    params,
                    multiParamSingleReturnError,
                    singleParamMultiReturnError
                )
            ) {
                val methodType = methodTypeClass(methodReturnType)
                return InsertOrUpsertMethodAdapter(methodType)
            }
            return null
        }

        private fun isReturnValid(
            context: Context,
            returnType: ReturnType,
            params: List<ShortcutQueryParameter>,
            multiParamSingleReturnError: String,
            singleParamMultiReturnError: String
        ): Boolean {
            if (params.isEmpty() || params.size > 1) {
                return returnType == ReturnType.VOID ||
                    returnType == ReturnType.UNIT
            }
            if (params.first().isMultiple) {
                val isValid = returnType in MULTIPLE_ITEM_SET
                if (!isValid) {
                    context.logger.e(multiParamSingleReturnError)
                }
                return isValid
            } else {
                val isValid = (returnType == ReturnType.VOID ||
                    returnType == ReturnType.VOID_OBJECT ||
                    returnType == ReturnType.UNIT ||
                    returnType == ReturnType.SINGLE_ID)
                if (!isValid) {
                    context.logger.e(singleParamMultiReturnError)
                }
                return isValid
            }
        }

        private val MULTIPLE_ITEM_SET by lazy {
            setOf(
                ReturnType.VOID,
                ReturnType.VOID_OBJECT,
                ReturnType.UNIT,
                ReturnType.ID_ARRAY,
                ReturnType.ID_ARRAY_BOX,
                ReturnType.ID_LIST
            )
        }

        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        private fun getReturnType(returnType: XType): ReturnType? {
            return if (returnType.isVoid()) {
                ReturnType.VOID
            } else if (returnType.isVoidObject()) {
                ReturnType.VOID_OBJECT
            } else if (returnType.isKotlinUnit()) {
                ReturnType.UNIT
            } else if (returnType.isArray()) {
                val param = returnType.componentType
                if (param.isLong()) {
                    if (param.typeName == TypeName.LONG) {
                        ReturnType.ID_ARRAY
                    } else {
                        ReturnType.ID_ARRAY_BOX
                    }
                } else {
                    null
                }
            } else if (returnType.isList()) {
                val param = returnType.typeArguments.first()
                if (param.isLong()) {
                    ReturnType.ID_LIST
                } else {
                    null
                }
            } else if (returnType.isLong()) {
                ReturnType.SINGLE_ID
            } else {
                null
            }
        }
    }

    fun createMethodBody(
        parameters: List<ShortcutQueryParameter>,
        adapters: Map<String, Pair<FieldSpec, Any>>,
        dbField: FieldSpec,
        scope: CodeGenScope
    ) {

        scope.builder().apply {
            val methodName = methodType.methodName
            val methodReturnType = methodType.returnType

            // TODO assert thread
            // TODO collect results
            addStatement("$N.beginTransaction()", dbField)
            val needsResultVar = methodReturnType != ReturnType.VOID &&
                methodReturnType != ReturnType.VOID_OBJECT &&
                methodReturnType != ReturnType.UNIT
            val resultVar = if (needsResultVar) {
                scope.getTmpVar("_result")
            } else {
                null
            }

            beginControlFlow("try").apply {
                parameters.forEach { param ->
                    val upsertionAdapter = adapters[param.name]?.first
                    if (needsResultVar) {
                        // if it has more than 1 parameter, we would've already printed the error
                        // so we don't care about re-declaring the variable here
                        addStatement(
                            "$T $L = $N.$L($L)",
                            methodReturnType.returnTypeName, resultVar,
                            upsertionAdapter, methodName,
                            param.name
                        )
                    } else {
                        addStatement(
                            "$N.$L($L)", upsertionAdapter, methodName,
                            param.name
                        )
                    }
                }
                addStatement("$N.setTransactionSuccessful()", dbField)
                if (needsResultVar) {
                    addStatement("return $L", resultVar)
                } else if (methodReturnType == ReturnType.VOID_OBJECT) {
                    addStatement("return null")
                } else if (methodReturnType == ReturnType.UNIT) {
                    addStatement("return $T.INSTANCE", KotlinTypeNames.UNIT)
                }
            }
            nextControlFlow("finally").apply {
                addStatement("$N.endTransaction()", dbField)
            }
            endControlFlow()
        }
    }

    sealed class MethodType(
        val returnType: ReturnType
    ) {
        abstract val methodName: String
    }

    class InsertMethodType(returnType: ReturnType) : MethodType(returnType) {
        override val methodName = "insert" + returnType.methodSuffix
    }

    class UpsertMethodType(returnType: ReturnType) : MethodType(returnType) {
        override val methodName = "upsert" + returnType.methodSuffix
    }

    enum class ReturnType(
        val methodSuffix: String,
        val returnTypeName: TypeName
    ) {
        VOID("", TypeName.VOID), // return void
        VOID_OBJECT("", TypeName.VOID), // return void
        UNIT("", KotlinTypeNames.UNIT), // return kotlin.Unit.INSTANCE
        SINGLE_ID("AndReturnId", TypeName.LONG), // return long
        ID_ARRAY(
            "AndReturnIdsArray",
            ArrayTypeName.of(TypeName.LONG)
        ), // return long[]
        ID_ARRAY_BOX(
            "AndReturnIdsArrayBox",
            ArrayTypeName.of(TypeName.LONG.box())
        ), // return Long[]
        ID_LIST(
            "AndReturnIdsList",
            ParameterizedTypeName.get(List::class.typeName, TypeName.LONG.box())
        ), // return List<Long>
    }
}