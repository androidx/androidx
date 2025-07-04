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
import androidx.room.ext.isNotKotlinUnit
import androidx.room.ext.isNotVoid
import androidx.room.ext.isNotVoidObject
import androidx.room.processor.Context
import androidx.room.processor.ProcessorErrors
import androidx.room.solver.CodeGenScope
import androidx.room.vo.ShortcutQueryParameter

class InsertOrUpsertFunctionAdapter private constructor(private val functionInfo: FunctionInfo) {
    internal val returnType = functionInfo.returnType

    companion object {
        fun createInsert(
            context: Context,
            returnType: XType,
            params: List<ShortcutQueryParameter>,
        ): InsertOrUpsertFunctionAdapter? {
            return createFunction(
                context = context,
                returnType = returnType,
                params = params,
                functionInfoClass = ::InsertFunctionInfo,
                multiParamSingleReturnError =
                    ProcessorErrors.INSERT_MULTI_PARAM_SINGLE_RETURN_MISMATCH,
                singleParamMultiReturnError =
                    ProcessorErrors.INSERT_SINGLE_PARAM_MULTI_RETURN_MISMATCH,
            )
        }

        fun createUpsert(
            context: Context,
            returnType: XType,
            params: List<ShortcutQueryParameter>,
        ): InsertOrUpsertFunctionAdapter? {
            return createFunction(
                context = context,
                returnType = returnType,
                params = params,
                functionInfoClass = ::UpsertFunctionInfo,
                multiParamSingleReturnError =
                    ProcessorErrors.UPSERT_MULTI_PARAM_SINGLE_RETURN_MISMATCH,
                singleParamMultiReturnError =
                    ProcessorErrors.UPSERT_SINGLE_PARAM_MULTI_RETURN_MISMATCH,
            )
        }

        private fun createFunction(
            context: Context,
            returnType: XType,
            params: List<ShortcutQueryParameter>,
            functionInfoClass: (returnInfo: ReturnInfo, returnType: XType) -> FunctionInfo,
            multiParamSingleReturnError: String,
            singleParamMultiReturnError: String,
        ): InsertOrUpsertFunctionAdapter? {
            val functionReturnType = getReturnType(returnType)
            if (
                functionReturnType != null &&
                    isReturnValid(
                        context,
                        functionReturnType,
                        params,
                        multiParamSingleReturnError,
                        singleParamMultiReturnError,
                    )
            ) {
                val functionInfo = functionInfoClass(functionReturnType, returnType)
                return InsertOrUpsertFunctionAdapter(functionInfo = functionInfo)
            }
            return null
        }

        private fun isReturnValid(
            context: Context,
            returnInfo: ReturnInfo,
            params: List<ShortcutQueryParameter>,
            multiParamSingleReturnError: String,
            singleParamMultiReturnError: String,
        ): Boolean {
            if (params.isEmpty() || params.size > 1) {
                return returnInfo == ReturnInfo.VOID ||
                    returnInfo == ReturnInfo.UNIT ||
                    returnInfo == ReturnInfo.VOID_OBJECT
            }
            if (params.first().isMultiple) {
                val isValid = returnInfo in MULTIPLE_ITEM_SET
                if (!isValid) {
                    context.logger.e(multiParamSingleReturnError)
                }
                return isValid
            } else {
                val isValid =
                    (returnInfo == ReturnInfo.VOID ||
                        returnInfo == ReturnInfo.VOID_OBJECT ||
                        returnInfo == ReturnInfo.UNIT ||
                        returnInfo == ReturnInfo.SINGLE_ID)
                if (!isValid) {
                    context.logger.e(singleParamMultiReturnError)
                }
                return isValid
            }
        }

        private val MULTIPLE_ITEM_SET by lazy {
            setOf(
                ReturnInfo.VOID,
                ReturnInfo.VOID_OBJECT,
                ReturnInfo.UNIT,
                ReturnInfo.ID_ARRAY,
                ReturnInfo.ID_ARRAY_BOX,
                ReturnInfo.ID_LIST,
            )
        }

        private fun getReturnType(returnType: XType): ReturnInfo? {
            return if (returnType.isVoid()) {
                ReturnInfo.VOID
            } else if (returnType.isVoidObject()) {
                ReturnInfo.VOID_OBJECT
            } else if (returnType.isKotlinUnit()) {
                ReturnInfo.UNIT
            } else if (returnType.isArray()) {
                val param = returnType.componentType
                if (param.isLong()) {
                    if (param.asTypeName() == XTypeName.PRIMITIVE_LONG) {
                        ReturnInfo.ID_ARRAY
                    } else {
                        ReturnInfo.ID_ARRAY_BOX
                    }
                } else {
                    null
                }
            } else if (returnType.isList()) {
                val param = returnType.typeArguments.first()
                if (param.isLong()) {
                    ReturnInfo.ID_LIST
                } else {
                    null
                }
            } else if (returnType.isLong()) {
                ReturnInfo.SINGLE_ID
            } else {
                null
            }
        }
    }

    fun generateFunctionBody(
        scope: CodeGenScope,
        connectionVar: String,
        parameters: List<ShortcutQueryParameter>,
        adapters: Map<String, Pair<XPropertySpec, Any>>,
    ) {
        scope.builder.apply {
            val hasReturnValue =
                returnType.isNotVoid() &&
                    returnType.isNotVoidObject() &&
                    returnType.isNotKotlinUnit()
            val resultVar =
                if (hasReturnValue) {
                    scope.getTmpVar("_result")
                } else {
                    null
                }
            parameters.forEach { param ->
                val upsertAdapter = adapters.getValue(param.name).first
                val resultFormat =
                    XCodeBlock.of(
                            "%L.%L(%L, %L)",
                            upsertAdapter.name,
                            functionInfo.functionName,
                            connectionVar,
                            param.name,
                        )
                        .let {
                            if (
                                scope.language == CodeLanguage.KOTLIN &&
                                    functionInfo.returnInfo == ReturnInfo.ID_ARRAY_BOX &&
                                    functionInfo.returnType.asTypeName() ==
                                        functionInfo.returnInfo.typeName
                            ) {
                                XCodeBlock.ofCast(
                                    typeName = functionInfo.returnInfo.typeName,
                                    expressionBlock = it,
                                )
                            } else {
                                it
                            }
                        }
                when (scope.language) {
                    CodeLanguage.JAVA -> {
                        when (functionInfo.returnInfo) {
                            ReturnInfo.VOID,
                            ReturnInfo.VOID_OBJECT -> {
                                if (param == parameters.last()) {
                                    addStatement("%L", resultFormat)
                                    addStatement("return null")
                                } else {
                                    addStatement("%L", resultFormat)
                                }
                            }
                            ReturnInfo.UNIT -> {
                                if (param == parameters.last()) {
                                    addStatement("%L", resultFormat)
                                    addStatement("return %T.INSTANCE", KotlinTypeNames.UNIT)
                                } else {
                                    addStatement("%L", resultFormat)
                                }
                            }
                            else -> addStatement("return %L", resultFormat)
                        }
                    }
                    CodeLanguage.KOTLIN -> {
                        if (resultVar != null) {
                            // if it has more than 1 parameter, we would've already printed the
                            // error
                            // so we don't care about re-declaring the variable here
                            addLocalVariable(
                                name = resultVar,
                                typeName = returnType.asTypeName(),
                                assignExpr = resultFormat,
                            )
                        } else {
                            addStatement("%L", resultFormat)
                        }
                    }
                }
            }
            if (scope.language == CodeLanguage.KOTLIN && resultVar != null) {
                addStatement("%L", resultVar)
            }
        }
    }

    sealed class FunctionInfo(val returnInfo: ReturnInfo, val returnType: XType) {
        abstract val functionName: String
    }

    class InsertFunctionInfo(returnInfo: ReturnInfo, returnType: XType) :
        FunctionInfo(returnInfo, returnType) {
        override val functionName: String = "insert${returnInfo.functionSuffix}"
    }

    class UpsertFunctionInfo(returnInfo: ReturnInfo, returnType: XType) :
        FunctionInfo(returnInfo, returnType) {
        override val functionName: String = "upsert${returnInfo.functionSuffix}"
    }

    enum class ReturnInfo(val functionSuffix: String, val typeName: XTypeName) {
        VOID("", XTypeName.UNIT_VOID), // return void
        VOID_OBJECT("", CommonTypeNames.VOID), // return Void
        UNIT("", XTypeName.UNIT_VOID), // return kotlin.Unit.INSTANCE
        SINGLE_ID("AndReturnId", XTypeName.PRIMITIVE_LONG), // return long
        ID_ARRAY(
            "AndReturnIdsArray",
            XTypeName.getArrayName(XTypeName.PRIMITIVE_LONG),
        ), // return long[]
        ID_ARRAY_BOX(
            "AndReturnIdsArrayBox",
            XTypeName.getArrayName(XTypeName.BOXED_LONG),
        ), // return Long[]
        ID_LIST(
            "AndReturnIdsList",
            CommonTypeNames.LIST.parametrizedBy(XTypeName.BOXED_LONG),
        ), // return List<Long>
    }
}
