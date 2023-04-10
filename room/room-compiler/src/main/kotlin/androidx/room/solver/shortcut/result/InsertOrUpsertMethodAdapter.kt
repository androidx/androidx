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

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.isArray
import androidx.room.compiler.processing.isKotlinUnit
import androidx.room.compiler.processing.isLong
import androidx.room.compiler.processing.isVoid
import androidx.room.compiler.processing.isVoidObject
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.KotlinTypeNames
import androidx.room.ext.isList
import androidx.room.processor.Context
import androidx.room.processor.ProcessorErrors
import androidx.room.solver.CodeGenScope
import androidx.room.vo.ShortcutQueryParameter

class InsertOrUpsertMethodAdapter private constructor(private val methodType: MethodType) {
    companion object {
        fun createInsert(
            context: Context,
            returnType: XType,
            params: List<ShortcutQueryParameter>
        ): InsertOrUpsertMethodAdapter? {
            return createMethod(
                context = context,
                returnType = returnType,
                params = params,
                methodTypeClass = ::InsertMethodType,
                multiParamSingleReturnError =
                    ProcessorErrors.INSERT_MULTI_PARAM_SINGLE_RETURN_MISMATCH,
                singleParamMultiReturnError =
                    ProcessorErrors.INSERT_SINGLE_PARAM_MULTI_RETURN_MISMATCH
            )
        }

        fun createUpsert(
            context: Context,
            returnType: XType,
            params: List<ShortcutQueryParameter>
        ): InsertOrUpsertMethodAdapter? {
            return createMethod(
                context = context,
                returnType = returnType,
                params = params,
                methodTypeClass = ::UpsertMethodType,
                multiParamSingleReturnError =
                    ProcessorErrors.UPSERT_MULTI_PARAM_SINGLE_RETURN_MISMATCH,
                singleParamMultiReturnError =
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
                return returnType == ReturnType.VOID || returnType == ReturnType.UNIT
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
                    if (param.asTypeName() == XTypeName.PRIMITIVE_LONG) {
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
        adapters: Map<String, Pair<XPropertySpec, Any>>,
        dbProperty: XPropertySpec,
        scope: CodeGenScope
    ) {
        scope.builder.apply {
            val methodName = methodType.methodName
            val methodReturnType = methodType.returnType

            // TODO assert thread
            // TODO collect results
            addStatement("%N.beginTransaction()", dbProperty)
            val resultVar = when (methodReturnType) {
                ReturnType.VOID, ReturnType.VOID_OBJECT, ReturnType.UNIT -> null
                else -> scope.getTmpVar("_result")
            }

            beginControlFlow("try").apply {
                parameters.forEach { param ->
                    val upsertionAdapter = adapters.getValue(param.name).first
                    if (resultVar != null) {
                        // if it has more than 1 parameter, we would've already printed the error
                        // so we don't care about re-declaring the variable here
                        addLocalVariable(
                            name = resultVar,
                            typeName = methodReturnType.returnTypeName,
                            assignExpr = XCodeBlock.of(
                                language,
                                "%L.%L(%L)",
                                upsertionAdapter.name,
                                methodName,
                                param.name
                            )
                        )
                    } else {
                        addStatement(
                            "%L.%L(%L)",
                            upsertionAdapter.name,
                            methodName,
                            param.name
                        )
                    }
                }
                addStatement("%N.setTransactionSuccessful()", dbProperty)
                if (resultVar != null) {
                    addStatement("return %L", resultVar)
                } else if (methodReturnType == ReturnType.VOID_OBJECT) {
                    addStatement("return null")
                } else if (methodReturnType == ReturnType.UNIT && language == CodeLanguage.JAVA) {
                    addStatement("return %T.INSTANCE", KotlinTypeNames.UNIT)
                }
            }
            nextControlFlow("finally").apply {
                addStatement("%N.endTransaction()", dbProperty)
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
        val returnTypeName: XTypeName
    ) {
        VOID("", XTypeName.UNIT_VOID), // return void
        VOID_OBJECT("", CommonTypeNames.VOID), // return Void
        UNIT("", XTypeName.UNIT_VOID), // return kotlin.Unit.INSTANCE
        SINGLE_ID("AndReturnId", XTypeName.PRIMITIVE_LONG), // return long
        ID_ARRAY(
            "AndReturnIdsArray",
            XTypeName.getArrayName(XTypeName.PRIMITIVE_LONG)
        ), // return long[]
        ID_ARRAY_BOX(
            "AndReturnIdsArrayBox",
            XTypeName.getArrayName(XTypeName.BOXED_LONG)
        ), // return Long[]
        ID_LIST(
            "AndReturnIdsList",
            CommonTypeNames.LIST.parametrizedBy(XTypeName.BOXED_LONG)
        ), // return List<Long>
    }
}