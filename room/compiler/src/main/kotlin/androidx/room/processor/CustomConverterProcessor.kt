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

import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.ext.asDeclaredType
import androidx.room.ext.asMemberOf
import androidx.room.ext.getAllMethods
import androidx.room.ext.getConstructors
import androidx.room.ext.hasAnnotation
import androidx.room.ext.isPublic
import androidx.room.ext.isStatic
import androidx.room.ext.toAnnotationBox
import androidx.room.ext.typeName
import androidx.room.kotlin.KotlinMetadataElement
import androidx.room.processor.ProcessorErrors.TYPE_CONVERTER_BAD_RETURN_TYPE
import androidx.room.processor.ProcessorErrors.TYPE_CONVERTER_EMPTY_CLASS
import androidx.room.processor.ProcessorErrors.TYPE_CONVERTER_MISSING_NOARG_CONSTRUCTOR
import androidx.room.processor.ProcessorErrors.TYPE_CONVERTER_MUST_BE_PUBLIC
import androidx.room.processor.ProcessorErrors.TYPE_CONVERTER_MUST_RECEIVE_1_PARAM
import androidx.room.processor.ProcessorErrors.TYPE_CONVERTER_UNBOUND_GENERIC
import androidx.room.solver.types.CustomTypeConverterWrapper
import androidx.room.vo.CustomTypeConverter
import asTypeElement
import isError
import isNone
import isType
import isVoid
import java.util.LinkedHashSet
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror

/**
 * Processes classes that are referenced in TypeConverters annotations.
 */
class CustomConverterProcessor(val context: Context, val element: TypeElement) {
    companion object {
        private fun TypeMirror.isInvalidReturnType() =
            isError() || isVoid() || isNone()

        fun findConverters(context: Context, element: Element): ProcessResult {
            val annotation = element.toAnnotationBox(TypeConverters::class)
            return annotation?.let {
                val classes = it.getAsTypeMirrorList("value")
                    .filter { it.isType() }
                    .mapTo(LinkedHashSet()) { it }
                val converters = classes.flatMap {
                    CustomConverterProcessor(context, it.asTypeElement()).process()
                }
                reportDuplicates(context, converters)
                ProcessResult(classes, converters.map(::CustomTypeConverterWrapper))
            } ?: ProcessResult.EMPTY
        }

        private fun reportDuplicates(context: Context, converters: List<CustomTypeConverter>) {
            converters
                .groupBy { it.from.typeName() to it.to.typeName() }
                .filterValues { it.size > 1 }
                .values.forEach {
                    it.forEach { converter ->
                        context.logger.e(
                            converter.method, ProcessorErrors
                                .duplicateTypeConverters(it.minus(converter))
                        )
                    }
                }
        }
    }

    fun process(): List<CustomTypeConverter> {
        // using element utils instead of MoreElements to include statics.
        val methods = element.getAllMethods(context.processingEnv)
        val declaredType = element.asDeclaredType()
        val converterMethods = methods.filter {
            it.hasAnnotation(TypeConverter::class)
        }
        context.checker.check(converterMethods.isNotEmpty(), element, TYPE_CONVERTER_EMPTY_CLASS)
        val allStatic = converterMethods.all { it.isStatic() }
        val constructors = element.getConstructors()
        val kotlinMetadata = KotlinMetadataElement.createFor(context, element)
        val isKotlinObjectDeclaration = kotlinMetadata?.isObject() == true
        context.checker.check(
            isKotlinObjectDeclaration || allStatic || constructors.isEmpty() ||
                    constructors.any {
                        it.parameters.isEmpty()
                    }, element, TYPE_CONVERTER_MISSING_NOARG_CONSTRUCTOR
        )
        return converterMethods.mapNotNull {
            processMethod(
                container = declaredType,
                isContainerKotlinObject = isKotlinObjectDeclaration,
                methodElement = it
            )
        }
    }

    private fun processMethod(
        container: DeclaredType,
        methodElement: ExecutableElement,
        isContainerKotlinObject: Boolean
    ): CustomTypeConverter? {
        val typeUtils = context.processingEnv.typeUtils
        val asMember = methodElement.asMemberOf(typeUtils, container)
        val returnType = asMember.returnType
        val invalidReturnType = returnType.isInvalidReturnType()
        context.checker.check(
            methodElement.isPublic(), methodElement, TYPE_CONVERTER_MUST_BE_PUBLIC
        )
        if (invalidReturnType) {
            context.logger.e(methodElement, TYPE_CONVERTER_BAD_RETURN_TYPE)
            return null
        }
        val returnTypeName = returnType.typeName()
        context.checker.notUnbound(
            returnTypeName, methodElement,
            TYPE_CONVERTER_UNBOUND_GENERIC
        )
        val params = methodElement.parameters
        if (params.size != 1) {
            context.logger.e(methodElement, TYPE_CONVERTER_MUST_RECEIVE_1_PARAM)
            return null
        }
        val param = params.map {
            it.asMemberOf(typeUtils, container)
        }.first()
        context.checker.notUnbound(param.typeName(), params[0], TYPE_CONVERTER_UNBOUND_GENERIC)
        return CustomTypeConverter(
            enclosingClass = container,
            isEnclosingClassKotlinObject = isContainerKotlinObject,
            method = methodElement,
            from = param,
            to = returnType
        )
    }

    /**
     * Order of classes is important hence they are a LinkedHashSet not a set.
     */
    open class ProcessResult(
        val classes: LinkedHashSet<TypeMirror>,
        val converters: List<CustomTypeConverterWrapper>
    ) {
        object EMPTY : ProcessResult(LinkedHashSet(), emptyList())

        operator fun plus(other: ProcessResult): ProcessResult {
            val newClasses = LinkedHashSet<TypeMirror>()
            newClasses.addAll(classes)
            newClasses.addAll(other.classes)
            return ProcessResult(newClasses, converters + other.converters)
        }
    }
}
