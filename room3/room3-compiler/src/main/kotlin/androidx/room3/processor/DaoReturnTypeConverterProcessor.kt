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
import androidx.room3.compiler.processing.XMethodType
import androidx.room3.compiler.processing.XNullability
import androidx.room3.compiler.processing.XRawType
import androidx.room3.compiler.processing.XType
import androidx.room3.compiler.processing.XTypeElement
import androidx.room3.compiler.processing.isArray
import androidx.room3.compiler.processing.isKotlinUnit
import androidx.room3.compiler.processing.isSuspendFunction
import androidx.room3.ext.KotlinTypeNames.NO_ARG_LAMBDA
import androidx.room3.ext.KotlinTypeNames.NO_ARG_SUSPEND_LAMBDA
import androidx.room3.ext.KotlinTypeNames.SINGLE_ARG_LAMBDA
import androidx.room3.ext.KotlinTypeNames.SINGLE_ARG_SUSPEND_LAMBDA
import androidx.room3.ext.RoomTypeNames.RAW_QUERY
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
import androidx.room3.solver.types.DaoReturnTypeConverterWrapper
import androidx.room3.vo.CustomDaoReturnTypeConverter
import androidx.room3.vo.ExecuteAndReturnLambda

/** Processes classes that are referenced in ReturnTypeConverters annotations. */
class DaoReturnTypeConverterProcessor(
    val context: Context,
    val containerTypeElement: XTypeElement,
) {
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

        val lambdaTypes = resolveLambdaTypeConstants()

        val convertersList =
            memberFunctions
                .filter { it.hasAnnotation(DaoReturnTypeConverter::class) }
                .mapNotNull { processFunction(it, lambdaTypes) }

        val classContainsAtLeastOneConverterFunction =
            memberFunctions.any { it.hasAnnotation(DaoReturnTypeConverter::class) }
        context.checker.check(
            predicate = classContainsAtLeastOneConverterFunction,
            element = containerTypeElement,
            errorMsg = DAO_RETURN_TYPE_CONVERTER_MUST_CONTAIN_AN_ANNOTATED_FUNCTION,
        )
        return convertersList
    }

    private fun resolveLambdaTypeConstants(): LambdaTypeConstants =
        LambdaTypeConstants(
            noArgSuspendLambda = context.processingEnv.requireType(NO_ARG_SUSPEND_LAMBDA).rawType,
            singleArgSuspendLambda =
                context.processingEnv.requireType(SINGLE_ARG_SUSPEND_LAMBDA).rawType,
            noArgNonSuspendLambda = context.processingEnv.requireType(NO_ARG_LAMBDA).rawType,
            singleArgNonSuspendLambda = context.processingEnv.requireType(SINGLE_ARG_LAMBDA).rawType,
        )

    private fun processFunction(
        function: XMethodElement,
        lambdaTypes: LambdaTypeConstants,
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
            findAndValidateLambdaParams(function, lambdaTypes, requiredFunctionParamTypes.lastIndex)
        if (suspendLambdaParam == null) {
            context.logger.e(
                element = function,
                msg = DAO_RETURN_TYPE_CONVERTER_MUST_HAVE_ONE_LAMBDA_PARAM_THAT_IS_SUSPEND,
            )
            return null
        } else {
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
            return DaoReturnTypeConverterWrapper(
                customDaoReturnTypeConverter =
                    CustomDaoReturnTypeConverter(
                        to = to,
                        enclosingClass = containerTypeElement,
                        isEnclosingClassKotlinObject = false,
                        function = function,
                        isProvidedConverter = false,
                        requiredFunctionParamTypes = requiredFunctionParamTypes,
                        operationTypes = operationTypes,
                        executeAndReturnLambda =
                            processExecuteAndReturnLambda(
                                to = to,
                                functionType = functionType,
                                function = function,
                                suspendLambdaParam = suspendLambdaParam,
                                requiredFunctionParamTypes = requiredFunctionParamTypes,
                            ),
                    )
            )
        }
    }

    private fun findAndValidateLambdaParams(
        function: XExecutableElement,
        lambdaTypes: LambdaTypeConstants,
        expectedIndexOfLambdaParam: Int,
    ): XExecutableParameterElement? {
        fun XExecutableParameterElement.isSuspendFunction(): Boolean =
            lambdaTypes.noArgSuspendLambda.isAssignableFrom(this.type.rawType) ||
                lambdaTypes.singleArgSuspendLambda.isAssignableFrom(this.type.rawType)

        fun XExecutableParameterElement.isNonSuspendFunction(): Boolean =
            lambdaTypes.noArgNonSuspendLambda.isAssignableFrom(this.type.rawType) ||
                lambdaTypes.singleArgNonSuspendLambda.isAssignableFrom(this.type.rawType)

        val suspendLambdaParamCandidates = function.parameters.filter { it.isSuspendFunction() }
        val suspendLambdaParam = suspendLambdaParamCandidates.singleOrNull()
        if (suspendLambdaParam == null) {
            val hasNonSuspendLambdaParam = function.parameters.any { it.isNonSuspendFunction() }
            context.checker.check(
                predicate = !hasNonSuspendLambdaParam,
                element = function,
                errorMsg = FOUND_DAO_TYPE_CONVERTER_WITH_NON_SUSPEND_LAMBDA,
            )
        }

        val actualIndexOfLambdaParam = function.parameters.indexOf(suspendLambdaParam)
        context.checker.check(
            predicate =
                suspendLambdaParam == null ||
                    actualIndexOfLambdaParam == (expectedIndexOfLambdaParam),
            element = function,
            errorMsg = DAO_RETURN_TYPE_CONVERTER_LAMBDA_MUST_BE_LAST_PARAM,
        )
        return suspendLambdaParam
    }

    private fun findRowAdapterTypeArgPosition(
        functionType: XMethodType,
        to: XType,
        function: XMethodElement,
        suspendLambdaParam: XExecutableParameterElement,
    ): Int {
        return if (functionType.typeVariables.isEmpty()) {
            val paramTypeArgs = suspendLambdaParam.type.typeArguments
            context.checker.check(
                predicate = paramTypeArgs.first().isKotlinUnit(),
                element = suspendLambdaParam,
                errorMsg = DAO_RETURN_TYPE_CONVERTER_FUNCTIONS_WITHOUT_TYPE_PARAM_SHOULD_RETURN_UNIT,
            )
            -1
        } else {
            val functionTypeParam = functionType.typeVariables.single()
            context.checker.check(
                predicate = to.typeArguments.contains(functionTypeParam),
                element = function,
                errorMsg =
                    daoReturnTypeConverterFunctionsWithATypeParamShouldHaveReturnTypeContainingTheSameTypeArg(
                        functionTypeParam.toString(),
                        to.typeArguments.joinToString(separator = ", "),
                    ),
            )
            to.typeArguments.indexOf(functionTypeParam)
        }
    }

    private fun processExecuteAndReturnLambda(
        to: XType,
        functionType: XMethodType,
        function: XMethodElement,
        suspendLambdaParam: XExecutableParameterElement,
        requiredFunctionParamTypes: List<XType>,
    ): ExecuteAndReturnLambda {
        val rowAdapterPosition =
            findRowAdapterTypeArgPosition(functionType, to, function, suspendLambdaParam)

        val functionParamType =
            function.executableType.typeVariables.singleOrNull()?.upperBounds?.singleOrNull()
        val functionReturnType =
            if (rowAdapterPosition > -1) {
                to.typeArguments[rowAdapterPosition]
            } else {
                to
            }
        val lambdaReturnType = suspendLambdaParam.type.typeArguments.last()
        // Wrap the return type in a collection if needed, e.g. a List for PagingSource.
        // Returns the original type argument XType if wrapping is not needed, or a match is
        // not found.
        val adjustToResultAdapterType: (XType) -> XType = { arg ->
            val env = context.processingEnv
            val rawType = lambdaReturnType.rawType

            if (lambdaReturnType.typeArguments.isNotEmpty()) {
                when {
                    rawType.isAssignableFrom(env.requireType(List::class)) ->
                        env.getDeclaredType(env.requireTypeElement(List::class), arg)
                    else -> arg
                }
            } else if (lambdaReturnType.isArray()) {
                env.getArrayType(arg)
            } else {
                arg
            }
        }
        val hasNullableLambdaReturnType =
            (lambdaReturnType.nullability != XNullability.NONNULL) &&
                (functionParamType == null ||
                    functionParamType.nullability == XNullability.NONNULL &&
                        functionReturnType.nullability == XNullability.NONNULL)

        return ExecuteAndReturnLambda(
            returnType = lambdaReturnType,
            adjustToResultAdapterType = adjustToResultAdapterType,
            hasNullableReturnType = hasNullableLambdaReturnType,
            rowAdapterTypeArgPosition = rowAdapterPosition,
            hasRawQueryParam = requiredFunctionParamTypes.any { it.asTypeName() == RAW_QUERY },
            isParametrized = requiredFunctionParamTypes.last().typeArguments.size > 1,
        )
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
                        wrappers.map { it.customDaoReturnTypeConverter }
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

    private data class LambdaTypeConstants(
        val noArgSuspendLambda: XRawType,
        val singleArgSuspendLambda: XRawType,
        val noArgNonSuspendLambda: XRawType,
        val singleArgNonSuspendLambda: XRawType,
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
    if (this.requiredFunctionParamTypes.size != other.requiredFunctionParamTypes.size) return false

    val rowPos = this.executeAndReturnLambda.rowAdapterTypeArgPosition
    if (rowPos != other.executeAndReturnLambda.rowAdapterTypeArgPosition) return false

    // Check if type arguments match, skipping the row adapter position
    return this.to.typeArguments.indices.all { pos ->
        pos == rowPos || this.to.typeArguments[pos].isAssignableFrom(other.to.typeArguments[pos])
    }
}
