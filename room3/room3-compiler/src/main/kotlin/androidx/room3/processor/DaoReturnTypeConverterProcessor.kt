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
import androidx.room3.compiler.processing.isKotlinUnit
import androidx.room3.compiler.processing.isSuspendFunction
import androidx.room3.ext.KotlinTypeNames.NO_ARG_SUSPEND_LAMBDA
import androidx.room3.ext.KotlinTypeNames.SINGLE_ARG_SUSPEND_LAMBDA
import androidx.room3.ext.getRequiredFunctionParamTypes
import androidx.room3.processor.ProcessorErrors.DAO_RETURN_TYPE_CONVERTER_EMPTY_CLASS
import androidx.room3.processor.ProcessorErrors.DAO_RETURN_TYPE_CONVERTER_FUNCTIONS_MUST_HAVE_AT_MOST_ONE_TYPE_PARAMETER
import androidx.room3.processor.ProcessorErrors.DAO_RETURN_TYPE_CONVERTER_FUNCTIONS_WITHOUT_TYPE_PARAM_SHOULD_RETURN_UNIT
import androidx.room3.processor.ProcessorErrors.DAO_RETURN_TYPE_CONVERTER_LAMBDA_MUST_BE_LAST_PARAM
import androidx.room3.processor.ProcessorErrors.DAO_RETURN_TYPE_CONVERTER_MUST_CONTAIN_AN_ANNOTATED_FUNCTION
import androidx.room3.processor.ProcessorErrors.DAO_RETURN_TYPE_CONVERTER_MUST_HAVE_ONE_LAMBDA_PARAM_THAT_IS_SUSPEND
import androidx.room3.processor.ProcessorErrors.daoReturnTypeConverterFunctionsWithATypeParamShouldHaveReturnTypeContainingTheSameTypeArg
import androidx.room3.solver.types.DaoReturnTypeConverterWrapper
import androidx.room3.vo.CustomDaoReturnTypeConverter

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
        val requiredFunctionNonLambdaParamTypes = function.getRequiredFunctionParamTypes()
        val suspendLambdaParam =
            findAndValidateLambdaParams(
                function,
                lambdaTypes,
                requiredFunctionNonLambdaParamTypes.lastIndex + 1,
            )
        if (suspendLambdaParam == null) {
            context.logger.e(
                element = function,
                msg = DAO_RETURN_TYPE_CONVERTER_MUST_HAVE_ONE_LAMBDA_PARAM_THAT_IS_SUSPEND,
            )
            return null
        } else {
            val rowAdapterPosition =
                findRowAdapterTypeArgPosition(functionType, to, function, suspendLambdaParam)

            val lambaParam = function.parameters.last().type.typeArguments.last()
            val functionParamType =
                function.executableType.typeVariables.singleOrNull()?.upperBounds?.singleOrNull()
            val functionReturnType =
                if (rowAdapterPosition > -1) {
                    to.typeArguments[rowAdapterPosition]
                } else {
                    to
                }
            val hasNullableLambdaReturnType =
                (lambaParam.nullability != XNullability.NONNULL) &&
                    (functionParamType == null ||
                        functionParamType.nullability == XNullability.NONNULL &&
                            functionReturnType.nullability == XNullability.NONNULL)

            return DaoReturnTypeConverterWrapper(
                CustomDaoReturnTypeConverter(
                    to = to,
                    enclosingClass = containerTypeElement,
                    isEnclosingClassKotlinObject = false,
                    function = function,
                    isProvidedConverter = false,
                    hasNullableLambdaReturnType = hasNullableLambdaReturnType,
                    rowAdapterTypeArgPosition = rowAdapterPosition,
                    requiredFunctionParamTypes = requiredFunctionNonLambdaParamTypes,
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

        val suspendLambdaParamCandidates = function.parameters.filter { it.isSuspendFunction() }
        val suspendLambdaParam = suspendLambdaParamCandidates.singleOrNull()

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
    )
}

private fun reportDuplicates(context: Context, converters: List<CustomDaoReturnTypeConverter>) {
    val reportedConverters = mutableSetOf<CustomDaoReturnTypeConverter>()
    converters
        .groupBy { it.to.asTypeName() }
        .filterValues { it.size > 1 }
        .values
        .forEach { possiblyDuplicateConverters ->
            possiblyDuplicateConverters.forEach { converter ->
                if (reportedConverters.contains(converter)) {
                    return@forEach
                }
                val duplicates =
                    possiblyDuplicateConverters.filter { duplicate ->
                        duplicate !== converter &&
                            duplicate.function.isSuspendFunction() ==
                                converter.function.isSuspendFunction()
                    }

                if (duplicates.isNotEmpty()) {
                    context.logger.e(
                        converter.function,
                        ProcessorErrors.duplicateDaoReturnTypeConverters(duplicates),
                    )
                    reportedConverters.add(converter)
                    reportedConverters.addAll(duplicates)
                }
            }
        }
}
