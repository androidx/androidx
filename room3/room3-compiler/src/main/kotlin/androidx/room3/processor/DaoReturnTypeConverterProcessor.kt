/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.room3.Dao
import androidx.room3.DaoReturnTypeConverter
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Database
import androidx.room3.OperationType
import androidx.room3.compiler.codegen.asClassName
import androidx.room3.compiler.processing.XElement
import androidx.room3.compiler.processing.XExecutableElement
import androidx.room3.compiler.processing.XExecutableParameterElement
import androidx.room3.compiler.processing.XMethodElement
import androidx.room3.compiler.processing.XNullability
import androidx.room3.compiler.processing.XRawType
import androidx.room3.compiler.processing.XType
import androidx.room3.compiler.processing.XTypeElement
import androidx.room3.compiler.processing.isArray
import androidx.room3.compiler.processing.isBoolean
import androidx.room3.compiler.processing.isKotlinUnit
import androidx.room3.compiler.processing.isSuspendFunction
import androidx.room3.ext.CommonTypeNames
import androidx.room3.ext.KotlinTypeNames.NO_ARG_LAMBDA
import androidx.room3.ext.KotlinTypeNames.NO_ARG_SUSPEND_LAMBDA
import androidx.room3.ext.KotlinTypeNames.SINGLE_ARG_LAMBDA
import androidx.room3.ext.KotlinTypeNames.SINGLE_ARG_SUSPEND_LAMBDA
import androidx.room3.ext.RoomTypeNames
import androidx.room3.ext.getRequiredFunctionParamTypes
import androidx.room3.processor.ProcessorErrors.DAO_RETURN_TYPE_CONVERTER_ANNOTATION_MUST_HAVE_OPERATION_TYPE
import androidx.room3.processor.ProcessorErrors.DAO_RETURN_TYPE_CONVERTER_EMPTY_CLASS
import androidx.room3.processor.ProcessorErrors.DAO_RETURN_TYPE_CONVERTER_FUNCTIONS_MUST_HAVE_AT_MOST_ONE_TYPE_PARAMETER
import androidx.room3.processor.ProcessorErrors.DAO_RETURN_TYPE_CONVERTER_FUNCTIONS_WITHOUT_TYPE_PARAM_SHOULD_RETURN_UNIT
import androidx.room3.processor.ProcessorErrors.DAO_RETURN_TYPE_CONVERTER_LAMBDA_MUST_BE_LAST_PARAM
import androidx.room3.processor.ProcessorErrors.DAO_RETURN_TYPE_CONVERTER_MUST_CONTAIN_AN_ANNOTATED_FUNCTION
import androidx.room3.processor.ProcessorErrors.DAO_RETURN_TYPE_CONVERTER_MUST_HAVE_ONE_LAMBDA_PARAM_THAT_IS_SUSPEND
import androidx.room3.processor.ProcessorErrors.FOUND_DAO_TYPE_CONVERTER_WITH_NON_SUSPEND_LAMBDA
import androidx.room3.processor.ProcessorErrors.daoReturnTypeConverterFunctionsWithATypeParamShouldHaveReturnTypeContainingTheSameTypeArg
import androidx.room3.processor.ProcessorErrors.daoReturnTypeFunctionForOpWithBadParam
import androidx.room3.processor.ProcessorErrors.daoReturnTypeFunctionWithBadParam
import androidx.room3.solver.types.DaoReturnTypeConverter.OptionalParam
import androidx.room3.solver.types.DaoReturnTypeConverterWrapper
import androidx.room3.vo.CustomDaoReturnTypeConverter
import androidx.room3.vo.ExecuteAndReturnLambda

/** Processes classes that are referenced in [DaoReturnTypeConverters] annotations. */
class DaoReturnTypeConverterProcessor(
    val context: Context,
    val containerTypeElement: XTypeElement,
) {
    private val lambdaTypesConstants =
        LambdaTypeConstants(
            noArgSuspendLambda = context.processingEnv.requireType(NO_ARG_SUSPEND_LAMBDA).rawType,
            singleArgSuspendLambda =
                context.processingEnv.requireType(SINGLE_ARG_SUSPEND_LAMBDA).rawType,
            noArgNonSuspendLambda = context.processingEnv.requireType(NO_ARG_LAMBDA).rawType,
            singleArgNonSuspendLambda = context.processingEnv.requireType(SINGLE_ARG_LAMBDA).rawType,
        )

    private val paramTypesConstants =
        ParamTypeConstants(
            roomDb = context.processingEnv.requireType(RoomTypeNames.ROOM_DB),
            rawQuery = context.processingEnv.requireType(RoomTypeNames.RAW_QUERY),
            string = context.processingEnv.requireType(CommonTypeNames.STRING),
            listOfString =
                context.processingEnv.getDeclaredType(
                    context.processingEnv.requireTypeElement(CommonTypeNames.LIST),
                    context.processingEnv.requireType(CommonTypeNames.STRING),
                ),
        )

    fun process(): List<DaoReturnTypeConverterWrapper> {
        val memberFunctions = containerTypeElement.getDeclaredMethods()
        context.checker.check(
            predicate = memberFunctions.isNotEmpty(),
            element = containerTypeElement,
            errorMsg = DAO_RETURN_TYPE_CONVERTER_EMPTY_CLASS,
        )
        if (memberFunctions.isEmpty()) {
            return emptyList()
        }

        val isKotlinObjectDeclaration = containerTypeElement.isKotlinObject()

        val convertersList =
            memberFunctions
                .filter { it.hasAnnotation(DaoReturnTypeConverter::class) }
                .mapNotNull { processFunction(it, isKotlinObjectDeclaration) }

        val classContainsAtLeastOneConverterFunction =
            memberFunctions.any { it.hasAnnotation(DaoReturnTypeConverter::class) }
        context.checker.check(
            predicate = classContainsAtLeastOneConverterFunction,
            element = containerTypeElement,
            errorMsg = DAO_RETURN_TYPE_CONVERTER_MUST_CONTAIN_AN_ANNOTATED_FUNCTION,
        )
        return convertersList
    }

    private fun processFunction(
        function: XMethodElement,
        isContainerKotlinObject: Boolean,
    ): DaoReturnTypeConverterWrapper? {
        val functionType = function.executableType
        val to =
            if (functionType.isSuspendFunction()) {
                functionType.getSuspendFunctionReturnType()
            } else {
                functionType.returnType
            }

        if (functionType.typeVariables.size > 1) {
            context.logger.e(
                element = function,
                msg = DAO_RETURN_TYPE_CONVERTER_FUNCTIONS_MUST_HAVE_AT_MOST_ONE_TYPE_PARAMETER,
            )
            return null
        }
        val requiredFunctionParamTypes = function.getRequiredFunctionParamTypes()
        val suspendLambdaParam =
            findAndValidateLambdaParameter(function, requiredFunctionParamTypes.lastIndex)
        if (suspendLambdaParam == null) {
            context.logger.e(
                element = function,
                msg = DAO_RETURN_TYPE_CONVERTER_MUST_HAVE_ONE_LAMBDA_PARAM_THAT_IS_SUSPEND,
            )
            return null
        }

        val operationTypes =
            function
                .requireAnnotation(DaoReturnTypeConverter::class)
                .getAsEnumList("operations")
                .map { OperationType.valueOf(it.name) }
        context.checker.check(
            predicate = operationTypes.isNotEmpty(),
            element = function,
            errorMsg = DAO_RETURN_TYPE_CONVERTER_ANNOTATION_MUST_HAVE_OPERATION_TYPE,
        )

        val requiredParameters =
            processOptionalParameters(
                operationTypes,
                function,
                requiredFunctionParamTypes.minus(suspendLambdaParam.type),
            ) ?: return null

        val executeAndReturnLambda =
            processExecuteAndReturnLambda(
                to = to,
                convertFunction = function,
                suspendLambdaParam = suspendLambdaParam,
            ) ?: return null

        return DaoReturnTypeConverterWrapper(
            converter =
                CustomDaoReturnTypeConverter(
                    to = to,
                    enclosingClass = containerTypeElement,
                    isEnclosingClassKotlinObject = isContainerKotlinObject,
                    function = function,
                    isProvidedConverter = false,
                    requiredParameters = requiredParameters,
                    operationTypes = operationTypes,
                    executeAndReturnLambda = executeAndReturnLambda,
                )
        )
    }

    /**
     * A functional / lambda parameter in DAO return type converters is required, it must be the
     * last parameter and must be a suspend function.
     */
    private fun findAndValidateLambdaParameter(
        convertFunction: XExecutableElement,
        expectedIndexOfLambdaParam: Int,
    ): XExecutableParameterElement? {
        fun XExecutableParameterElement.isSuspendFunction(): Boolean =
            lambdaTypesConstants.noArgSuspendLambda.isAssignableFrom(this.type.rawType) ||
                lambdaTypesConstants.singleArgSuspendLambda.isAssignableFrom(this.type.rawType)

        fun XExecutableParameterElement.isNonSuspendFunction(): Boolean =
            lambdaTypesConstants.noArgNonSuspendLambda.isAssignableFrom(this.type.rawType) ||
                lambdaTypesConstants.singleArgNonSuspendLambda.isAssignableFrom(this.type.rawType)

        val suspendLambdaParamCandidates =
            convertFunction.parameters.filter { it.isSuspendFunction() }
        val suspendLambdaParam = suspendLambdaParamCandidates.singleOrNull()
        if (suspendLambdaParam == null) {
            val hasNonSuspendLambdaParam =
                convertFunction.parameters.any { it.isNonSuspendFunction() }
            context.checker.check(
                predicate = !hasNonSuspendLambdaParam,
                element = convertFunction,
                errorMsg = FOUND_DAO_TYPE_CONVERTER_WITH_NON_SUSPEND_LAMBDA,
            )
        }

        val actualIndexOfLambdaParam = convertFunction.parameters.indexOf(suspendLambdaParam)
        context.checker.check(
            predicate =
                suspendLambdaParam == null ||
                    actualIndexOfLambdaParam == (expectedIndexOfLambdaParam),
            element = convertFunction,
            errorMsg = DAO_RETURN_TYPE_CONVERTER_LAMBDA_MUST_BE_LAST_PARAM,
        )
        return suspendLambdaParam
    }

    /**
     * If a converter function has a type parameter, then the return type of the converter function
     * must have a matching type variable and similarly the lambda parameter in the convert function
     * must also have a matching type variable.
     */
    private fun findRowAdapterTypeArgPosition(
        to: XType,
        convertFunction: XMethodElement,
        suspendLambdaReturnType: XType,
    ): Int {
        if (convertFunction.typeParameters.isEmpty()) {
            // Converter function has no type param, lambda should return Unit
            context.checker.check(
                predicate = suspendLambdaReturnType.isKotlinUnit(),
                element = convertFunction,
                errorMsg = DAO_RETURN_TYPE_CONVERTER_FUNCTIONS_WITHOUT_TYPE_PARAM_SHOULD_RETURN_UNIT,
            )
            return -1
        }

        val functionTypeVar = convertFunction.executableType.typeVariables.single()
        val typeVarIndex = to.typeArguments.indexOf(functionTypeVar)
        context.checker.check(
            predicate = typeVarIndex != -1,
            element = convertFunction,
            errorMsg =
                daoReturnTypeConverterFunctionsWithATypeParamShouldHaveReturnTypeContainingTheSameTypeArg(
                    functionTypeVar.asTypeName().toString(context.codeLanguage),
                    to.typeArguments.joinToString(separator = ", ") {
                        it.asTypeName().toString(context.codeLanguage)
                    },
                ),
        )
        return typeVarIndex
    }

    /**
     * The functional / lambda parameter (usually called `executeAndReturn`) in DAO return type
     * converters must have exactly one parameter of type `RoomRawQuery` or none. It must return
     * either `Unit`, a type variable whose name matches the name of the type parameter of the
     * convert function or one of the supported collections containing the previous type variable.
     */
    private fun processExecuteAndReturnLambda(
        to: XType,
        convertFunction: XMethodElement,
        suspendLambdaParam: XExecutableParameterElement,
    ): ExecuteAndReturnLambda? {
        // Wrap the lambda return type in a collection if needed, e.g. a List for PagingSource.
        // Returns the original type argument XType if wrapping is not needed, or a match is
        // not found.
        val lambdaReturnType = suspendLambdaParam.type.typeArguments.last()
        val adjustToResultAdapterType: (XType) -> XType = { arg ->
            val env = context.processingEnv
            if (lambdaReturnType.isArray()) {
                env.getArrayType(arg)
            } else if (lambdaReturnType.isTypeOf(List::class)) {
                env.getDeclaredType(env.requireTypeElement(List::class), arg)
            } else {
                arg
            }
        }

        val rowAdapterPosition =
            findRowAdapterTypeArgPosition(to, convertFunction, lambdaReturnType)
        val functionParamType =
            convertFunction.executableType.typeVariables.singleOrNull()?.upperBounds?.singleOrNull()
        val functionReturnType =
            if (rowAdapterPosition > -1) {
                to.typeArguments[rowAdapterPosition]
            } else {
                to
            }
        val hasNullableReturnType =
            (lambdaReturnType.nullability != XNullability.NONNULL) &&
                (functionParamType == null ||
                    functionParamType.nullability == XNullability.NONNULL &&
                        functionReturnType.nullability == XNullability.NONNULL)

        val lambdaParameterTypes = suspendLambdaParam.type.typeArguments.dropLast(1)
        if (lambdaParameterTypes.size > 1) {
            context.logger.e(
                element = convertFunction,
                msg = DAO_RETURN_TYPE_CONVERTER_MUST_HAVE_ONE_LAMBDA_PARAM_THAT_IS_SUSPEND,
            )
            return null
        } else if (lambdaParameterTypes.size == 1) {
            context.checker.check(
                predicate = lambdaParameterTypes.single().isSameType(paramTypesConstants.rawQuery),
                element = convertFunction,
                errorMsg = DAO_RETURN_TYPE_CONVERTER_MUST_HAVE_ONE_LAMBDA_PARAM_THAT_IS_SUSPEND,
            )
        }

        return ExecuteAndReturnLambda(
            returnType = lambdaReturnType,
            adjustToResultAdapterType = adjustToResultAdapterType,
            hasNullableReturnType = hasNullableReturnType,
            rowAdapterTypeArgPosition = rowAdapterPosition,
            hasRawQueryParam = lambdaParameterTypes.isNotEmpty(),
        )
    }

    /**
     * A functional / lambda parameter in DAO return type converters is required. All other params
     * are optional, but are limited to:
     * * `Boolean` representing `inTransaction`
     * * `List<String>` or `Array<String>` representing `tableNames`
     * * `RoomRawQuery` representing `rawQuery`
     *
     * We need to have a way to check if [1] any / which of these parameters have been defined in
     * the convert function we have, [2] the order in which they have been supplied.
     */
    private fun processOptionalParameters(
        operationTypes: List<OperationType>,
        convertFunction: XMethodElement,
        paramTypes: List<XType>,
    ): List<OptionalParam>? =
        paramTypes.map {
            if (it.isSameType(paramTypesConstants.roomDb)) {
                OptionalParam.ROOM_DB
            } else if (it.isSameType(paramTypesConstants.rawQuery)) {
                if (OperationType.WRITE in operationTypes) {
                    context.logger.e(
                        element = convertFunction,
                        msg =
                            daoReturnTypeFunctionForOpWithBadParam(
                                op = OperationType.WRITE.name,
                                paramTypeName = it.asTypeName().toString(context.codeLanguage),
                            ),
                    )
                    return null
                }
                OptionalParam.RAW_QUERY
            } else if (it.isSameType(paramTypesConstants.listOfString)) {
                if (OperationType.WRITE in operationTypes) {
                    context.logger.e(
                        element = convertFunction,
                        msg =
                            daoReturnTypeFunctionForOpWithBadParam(
                                op = OperationType.WRITE.name,
                                paramTypeName = it.asTypeName().toString(context.codeLanguage),
                            ),
                    )
                    return null
                }
                OptionalParam.TABLE_NAMES_LIST
            } else if (it.isArray() && it.componentType.isSameType(paramTypesConstants.string)) {
                if (OperationType.WRITE in operationTypes) {
                    context.logger.e(
                        element = convertFunction,
                        msg =
                            daoReturnTypeFunctionForOpWithBadParam(
                                op = OperationType.WRITE.name,
                                paramTypeName = it.asTypeName().toString(context.codeLanguage),
                            ),
                    )
                    return null
                }
                OptionalParam.TABLE_NAMES_ARRAY
            } else if (it.isBoolean()) {
                OptionalParam.IN_TRANSACTION
            } else {
                context.logger.e(
                    element = convertFunction,
                    msg =
                        daoReturnTypeFunctionWithBadParam(
                            paramTypeName = it.asTypeName().toString(context.codeLanguage)
                        ),
                )
                return null
            }
        }

    companion object {
        fun findConverters(context: Context, element: XElement): ProcessResult {
            if (!element.hasAnnotation(Database::class) && !element.hasAnnotation(Dao::class)) {
                return ProcessResult.EMPTY
            }
            if (!element.hasAnnotation(DaoReturnTypeConverters::class)) {
                return ProcessResult.EMPTY
            }
            if (!element.validate()) {
                context.reportMissingTypeReference(element.toString())
                return ProcessResult.EMPTY
            }
            val annotation = element.requireAnnotation(DaoReturnTypeConverters::class.asClassName())
            val classes = annotation.getAsTypeList("value").mapTo(LinkedHashSet()) { it }
            val typeElementToWrappers =
                classes
                    .mapNotNull {
                        val typeElement = it.typeElement
                        if (typeElement == null) {
                            context.logger.e(
                                element,
                                ProcessorErrors.typeConverterMustBeDeclared(
                                    it.asTypeName().toString(context.codeLanguage)
                                ),
                            )
                            null
                        } else {
                            typeElement
                        }
                    }
                    .associateWith { DaoReturnTypeConverterProcessor(context, it).process() }
            reportDuplicates(
                context = context,
                converters =
                    typeElementToWrappers.values.flatMap { wrappers ->
                        wrappers.map { it.converter }
                    },
            )
            return ProcessResult(typeElementToWrappers = typeElementToWrappers)
        }
    }

    /** Order of classes is important hence they are a LinkedHashSet not a set. */
    data class ProcessResult(
        private val typeElementToWrappers: Map<XTypeElement, List<DaoReturnTypeConverterWrapper>>
    ) {
        companion object {
            val EMPTY = ProcessResult(typeElementToWrappers = LinkedHashMap())
        }

        val classes: Set<XTypeElement>
            get() = typeElementToWrappers.keys

        val converters: List<DaoReturnTypeConverterWrapper>
            get() = typeElementToWrappers.flatMap { it.value }

        operator fun plus(other: ProcessResult): ProcessResult {
            val newMap = LinkedHashMap<XTypeElement, List<DaoReturnTypeConverterWrapper>>()
            newMap.putAll(typeElementToWrappers)
            other.typeElementToWrappers.forEach { (typeElement, converters) ->
                if (!newMap.contains(typeElement)) {
                    newMap[typeElement] = converters
                }
            }
            return ProcessResult(typeElementToWrappers = newMap)
        }
    }

    private class LambdaTypeConstants(
        val noArgSuspendLambda: XRawType,
        val singleArgSuspendLambda: XRawType,
        val noArgNonSuspendLambda: XRawType,
        val singleArgNonSuspendLambda: XRawType,
    )

    private class ParamTypeConstants(
        val roomDb: XType,
        val rawQuery: XType,
        val string: XType,
        val listOfString: XType,
    )
}

private fun reportDuplicates(context: Context, converters: List<CustomDaoReturnTypeConverter>) {
    // Group by raw type first to narrow down the search space efficiently
    converters
        .groupBy { it.to.rawType.asTypeName() }
        .filterValues { it.size > 1 }
        .values
        .forEach { possibleDuplicates ->
            val reportedIndices = mutableSetOf<Int>()

            possibleDuplicates.forEachIndexed { index, converter ->
                if (index in reportedIndices) return@forEachIndexed

                // Use subList to get a view of remaining items without allocating a new list
                val duplicates =
                    possibleDuplicates.subList(index + 1, possibleDuplicates.size).filterIndexed {
                        subIndex,
                        other ->
                        val isMatch = converter.checkIfMatches(other)
                        if (isMatch) {
                            // Mark the absolute index in possibleDuplicates to avoid double
                            // reporting
                            reportedIndices.add(index + 1 + subIndex)
                        }
                        isMatch
                    }

                if (duplicates.isNotEmpty()) {
                    val converterNames =
                        (listOf(converter) + duplicates).map {
                            it.className.toString(context.codeLanguage) + "." + it.function.name
                        }
                    context.logger.e(
                        converter.function,
                        ProcessorErrors.duplicateDaoReturnTypeConverters(converterNames),
                    )
                }
            }
        }
}

private fun CustomDaoReturnTypeConverter.checkIfMatches(
    other: CustomDaoReturnTypeConverter
): Boolean {
    // Raw type is already handled by the groupBy, but kept for logic completeness if used elsewhere
    if (this.function.isSuspendFunction() != other.function.isSuspendFunction()) return false
    if (this.requiredParameters.size != other.requiredParameters.size) return false

    val rowPos = this.executeAndReturnLambda.rowAdapterTypeArgPosition
    if (rowPos != other.executeAndReturnLambda.rowAdapterTypeArgPosition) return false

    // Check if type arguments match, skipping the row adapter position
    return this.to.typeArguments.indices.all { pos ->
        pos == rowPos || this.to.typeArguments[pos].isAssignableFrom(other.to.typeArguments[pos])
    }
}
