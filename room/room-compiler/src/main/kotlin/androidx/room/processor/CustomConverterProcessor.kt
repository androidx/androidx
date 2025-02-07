/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.room.processor

import androidx.room.BuiltInTypeConverters
import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.compiler.codegen.asClassName
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.isVoid
import androidx.room.processor.ProcessorErrors.INNER_CLASS_TYPE_CONVERTER_MUST_BE_STATIC
import androidx.room.processor.ProcessorErrors.TYPE_CONVERTER_BAD_RETURN_TYPE
import androidx.room.processor.ProcessorErrors.TYPE_CONVERTER_EMPTY_CLASS
import androidx.room.processor.ProcessorErrors.TYPE_CONVERTER_MISSING_NOARG_CONSTRUCTOR
import androidx.room.processor.ProcessorErrors.TYPE_CONVERTER_MUST_BE_PUBLIC
import androidx.room.processor.ProcessorErrors.TYPE_CONVERTER_MUST_RECEIVE_1_PARAM
import androidx.room.processor.ProcessorErrors.TYPE_CONVERTER_UNBOUND_GENERIC
import androidx.room.solver.types.CustomTypeConverterWrapper
import androidx.room.vo.BuiltInConverterFlags
import androidx.room.vo.CustomTypeConverter

/** Processes classes that are referenced in TypeConverters annotations. */
class CustomConverterProcessor(val context: Context, val element: XTypeElement) {
    companion object {
        private fun XType.isInvalidReturnType() = isError() || isVoid() || isNone()

        fun findConverters(context: Context, element: XElement): ProcessResult {
            if (!element.hasAnnotation(TypeConverters::class)) {
                return ProcessResult.EMPTY
            }
            if (!element.validate()) {
                context.reportMissingTypeReference(element.toString())
                return ProcessResult.EMPTY
            }
            val annotation = element.requireAnnotation(TypeConverters::class.asClassName())
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
                                )
                            )
                            null
                        } else {
                            typeElement
                        }
                    }
                    .associateWith {
                        CustomConverterProcessor(context, it)
                            .process()
                            .map(::CustomTypeConverterWrapper)
                    }
            reportDuplicates(
                context,
                typeElementToWrappers.values.flatMap { wrappers -> wrappers.map { it.custom } }
            )
            val builtInStates =
                annotation["builtInTypeConverters"]?.asAnnotation()?.let { builtInAnnotation ->
                    fun getState(name: String) =
                        builtInAnnotation[name]?.asEnum()?.name?.let {
                            BuiltInTypeConverters.State.valueOf(it)
                        } ?: BuiltInTypeConverters.State.INHERITED
                    BuiltInConverterFlags(
                        enums = getState("enums"),
                        uuid = getState("uuid"),
                        byteBuffer = getState("byteBuffer"),
                    )
                } ?: BuiltInConverterFlags.DEFAULT
            return ProcessResult(
                typeElementToWrappers = typeElementToWrappers,
                builtInConverterFlags = builtInStates
            )
        }

        private fun reportDuplicates(context: Context, converters: List<CustomTypeConverter>) {
            converters
                .groupBy { it.from.asTypeName() to it.to.asTypeName() }
                .filterValues { it.size > 1 }
                .values
                .forEach { possiblyDuplicateConverters ->
                    possiblyDuplicateConverters.forEach { converter ->
                        val duplicates =
                            possiblyDuplicateConverters.filter { duplicate ->
                                duplicate !== converter &&
                                    duplicate.from.isSameType(converter.from) &&
                                    duplicate.to.isSameType(converter.to)
                            }
                        if (duplicates.isNotEmpty()) {
                            context.logger.e(
                                converter.function,
                                ProcessorErrors.duplicateTypeConverters(duplicates)
                            )
                        }
                    }
                }
        }
    }

    fun process(): List<CustomTypeConverter> {
        if (!element.validate()) {
            context.reportMissingTypeReference(element.qualifiedName)
        }
        val functions = element.getAllMethods()
        val converterFunctions =
            functions.filter { it.hasAnnotation(TypeConverter::class) }.toList()
        val isProvidedConverter = element.hasAnnotation(ProvidedTypeConverter::class)
        context.checker.check(converterFunctions.isNotEmpty(), element, TYPE_CONVERTER_EMPTY_CLASS)
        val allStatic = converterFunctions.all { it.isStatic() }
        val constructors = element.getConstructors()
        val isKotlinObjectDeclaration = element.isKotlinObject()
        if (!isProvidedConverter) {
            context.checker.check(
                element.enclosingTypeElement == null || element.isStatic(),
                element,
                INNER_CLASS_TYPE_CONVERTER_MUST_BE_STATIC
            )
            context.checker.check(
                isKotlinObjectDeclaration ||
                    allStatic ||
                    constructors.isEmpty() ||
                    constructors.any { it.parameters.isEmpty() },
                element,
                TYPE_CONVERTER_MISSING_NOARG_CONSTRUCTOR
            )
        }
        return converterFunctions.mapNotNull {
            processFunction(
                container = element,
                isContainerKotlinObject = isKotlinObjectDeclaration,
                functionElement = it,
                isProvidedConverter = isProvidedConverter
            )
        }
    }

    private fun processFunction(
        container: XTypeElement,
        functionElement: XMethodElement,
        isContainerKotlinObject: Boolean,
        isProvidedConverter: Boolean
    ): CustomTypeConverter? {
        val asMember = functionElement.asMemberOf(container.type)
        val returnType = asMember.returnType
        val invalidReturnType = returnType.isInvalidReturnType()
        context.checker.check(
            functionElement.isPublic(),
            functionElement,
            TYPE_CONVERTER_MUST_BE_PUBLIC
        )
        if (invalidReturnType) {
            context.logger.e(functionElement, TYPE_CONVERTER_BAD_RETURN_TYPE)
            return null
        }
        context.checker.notUnbound(returnType, functionElement, TYPE_CONVERTER_UNBOUND_GENERIC)
        val params = functionElement.parameters
        if (params.size != 1) {
            context.logger.e(functionElement, TYPE_CONVERTER_MUST_RECEIVE_1_PARAM)
            return null
        }
        val param = params.map { it.asMemberOf(container.type) }.first()
        context.checker.notUnbound(param, params[0], TYPE_CONVERTER_UNBOUND_GENERIC)
        return CustomTypeConverter(
            enclosingClass = container,
            isEnclosingClassKotlinObject = isContainerKotlinObject,
            function = functionElement,
            from = param,
            to = returnType,
            isProvidedConverter = isProvidedConverter
        )
    }

    /** Order of classes is important hence they are a LinkedHashSet not a set. */
    data class ProcessResult(
        private val typeElementToWrappers: Map<XTypeElement, List<CustomTypeConverterWrapper>>,
        val builtInConverterFlags: BuiltInConverterFlags
    ) {
        companion object {
            val EMPTY =
                ProcessResult(
                    typeElementToWrappers = LinkedHashMap(),
                    builtInConverterFlags = BuiltInConverterFlags.DEFAULT
                )
        }

        val classes: Set<XTypeElement>
            get() = typeElementToWrappers.keys

        val converters: List<CustomTypeConverterWrapper>
            get() = typeElementToWrappers.flatMap { it.value }

        operator fun plus(other: ProcessResult): ProcessResult {
            val newMap = LinkedHashMap<XTypeElement, List<CustomTypeConverterWrapper>>()
            newMap.putAll(typeElementToWrappers)
            other.typeElementToWrappers.forEach { (typeElement, converters) ->
                if (!newMap.contains(typeElement)) {
                    newMap[typeElement] = converters
                }
            }
            return ProcessResult(
                typeElementToWrappers = newMap,
                builtInConverterFlags = other.builtInConverterFlags.withNext(builtInConverterFlags)
            )
        }
    }
}
