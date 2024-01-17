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

class InsertOrUpsertMethodAdapter private constructor(
    private val methodInfo: MethodInfo
) {
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
                methodInfoClass = ::InsertMethodInfo,
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
                methodInfoClass = ::UpsertMethodInfo,
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
            methodInfoClass: (returnInfo: ReturnInfo, returnType: XType) -> MethodInfo,
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
                val methodInfo = methodInfoClass(methodReturnType, returnType)
                return InsertOrUpsertMethodAdapter(
                    methodInfo = methodInfo
                )
            }
            return null
        }

        private fun isReturnValid(
            context: Context,
            returnInfo: ReturnInfo,
            params: List<ShortcutQueryParameter>,
            multiParamSingleReturnError: String,
            singleParamMultiReturnError: String
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
                val isValid = (returnInfo == ReturnInfo.VOID ||
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
                ReturnInfo.ID_LIST
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

    fun createMethodBody(
        parameters: List<ShortcutQueryParameter>,
        adapters: Map<String, Pair<XPropertySpec, Any>>,
        dbProperty: XPropertySpec,
        scope: CodeGenScope
    ) {
        scope.builder.apply {
            val methodName = methodInfo.methodName
            val methodReturnInfo = methodInfo.returnInfo

            // TODO assert thread
            // TODO collect results
            addStatement("%N.beginTransaction()", dbProperty)
            val resultVar = when (methodReturnInfo) {
                ReturnInfo.VOID, ReturnInfo.VOID_OBJECT, ReturnInfo.UNIT -> null
                else -> scope.getTmpVar("_result")
            }

            beginControlFlow("try").apply {
                parameters.forEach { param ->
                    val upsertionAdapter = adapters.getValue(param.name).first
                    // We want to keep the e.g. Array<out Long> generic type function signature, so
                    // need to do a cast.
                    val resultFormat = XCodeBlock.of(
                        language,
                        "%L.%L(%L)",
                        upsertionAdapter.name,
                        methodName,
                        param.name
                    ).let {
                        if (
                            language == CodeLanguage.KOTLIN &&
                            methodReturnInfo == ReturnInfo.ID_ARRAY_BOX &&
                            methodInfo.returnType.asTypeName() == methodReturnInfo.typeName
                        ) {
                            XCodeBlock.ofCast(
                                language = language,
                                typeName = methodReturnInfo.typeName,
                                expressionBlock = it
                            )
                        } else {
                            it
                        }
                    }

                    if (resultVar != null) {
                        // if it has more than 1 parameter, we would've already printed the error
                        // so we don't care about re-declaring the variable here
                        addLocalVariable(
                            name = resultVar,
                            typeName = methodInfo.returnType.asTypeName(),
                            assignExpr = resultFormat
                        )
                    } else {
                        addStatement("%L", resultFormat)
                    }
                }
                addStatement("%N.setTransactionSuccessful()", dbProperty)
                if (resultVar != null) {
                    addStatement("return %L", resultVar)
                } else if (methodReturnInfo == ReturnInfo.VOID_OBJECT) {
                    addStatement("return null")
                } else if (methodReturnInfo == ReturnInfo.UNIT && language == CodeLanguage.JAVA) {
                    addStatement("return %T.INSTANCE", KotlinTypeNames.UNIT)
                }
            }
            nextControlFlow("finally").apply {
                addStatement("%N.endTransaction()", dbProperty)
            }
            endControlFlow()
        }
    }

    sealed class MethodInfo(
        val returnInfo: ReturnInfo,
        val returnType: XType
    ) {
        abstract val methodName: String
    }

    class InsertMethodInfo(
        returnInfo: ReturnInfo,
        returnType: XType
    ) : MethodInfo(returnInfo, returnType) {
        override val methodName = "insert" + returnInfo.methodSuffix
    }

    class UpsertMethodInfo(
        returnInfo: ReturnInfo,
        returnType: XType
    ) : MethodInfo(returnInfo, returnType) {
        override val methodName = "upsert" + returnInfo.methodSuffix
    }

    enum class ReturnInfo(
        val methodSuffix: String,
        val typeName: XTypeName
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
