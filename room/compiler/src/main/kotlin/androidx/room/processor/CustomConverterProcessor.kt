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
import androidx.room.ext.hasAnnotation
import androidx.room.ext.hasAnyOf
import androidx.room.ext.toListOfClassTypes
import androidx.room.ext.typeName
import androidx.room.processor.ProcessorErrors.TYPE_CONVERTER_BAD_RETURN_TYPE
import androidx.room.processor.ProcessorErrors.TYPE_CONVERTER_EMPTY_CLASS
import androidx.room.processor.ProcessorErrors
        .TYPE_CONVERTER_MISSING_NOARG_CONSTRUCTOR
import androidx.room.processor.ProcessorErrors.TYPE_CONVERTER_MUST_BE_PUBLIC
import androidx.room.processor.ProcessorErrors.TYPE_CONVERTER_MUST_RECEIVE_1_PARAM
import androidx.room.processor.ProcessorErrors.TYPE_CONVERTER_UNBOUND_GENERIC
import androidx.room.solver.types.CustomTypeConverterWrapper
import androidx.room.vo.CustomTypeConverter
import com.google.auto.common.AnnotationMirrors
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import java.util.LinkedHashSet
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.ElementFilter

/**
 * Processes classes that are referenced in TypeConverters annotations.
 */
class CustomConverterProcessor(val context: Context, val element: TypeElement) {
    companion object {
        private val INVALID_RETURN_TYPES = setOf(TypeKind.ERROR, TypeKind.VOID, TypeKind.NONE)
        fun findConverters(context: Context, element: Element): ProcessResult {
            val annotation = MoreElements.getAnnotationMirror(element,
                    TypeConverters::class.java).orNull()
            return annotation?.let {
                val classes = AnnotationMirrors.getAnnotationValue(annotation, "value")
                        ?.toListOfClassTypes()
                        ?.filter {
                            MoreTypes.isType(it)
                        }?.mapTo(LinkedHashSet(), { it }) ?: LinkedHashSet<TypeMirror>()
                val converters = classes
                        .flatMap {
                            CustomConverterProcessor(context, MoreTypes.asTypeElement(it))
                                    .process()
                        }
                converters.let {
                    reportDuplicates(context, converters)
                }
                ProcessResult(classes, converters.map(::CustomTypeConverterWrapper))
            } ?: ProcessResult.EMPTY
        }

        fun reportDuplicates(context: Context, converters: List<CustomTypeConverter>) {
            val groupedByFrom = converters.groupBy { it.from.typeName() }
            groupedByFrom.forEach {
                it.value.groupBy { it.to.typeName() }.forEach {
                    if (it.value.size > 1) {
                        it.value.forEach { converter ->
                            context.logger.e(converter.method, ProcessorErrors
                                    .duplicateTypeConverters(it.value.minus(converter)))
                        }
                    }
                }
            }
        }
    }

    fun process(): List<CustomTypeConverter> {
        // using element utils instead of MoreElements to include statics.
        val methods = ElementFilter
                .methodsIn(context.processingEnv.elementUtils.getAllMembers(element))
        val declaredType = MoreTypes.asDeclared(element.asType())
        val converterMethods = methods.filter {
            it.hasAnnotation(TypeConverter::class)
        }
        context.checker.check(converterMethods.isNotEmpty(), element, TYPE_CONVERTER_EMPTY_CLASS)
        val allStatic = converterMethods.all { it.modifiers.contains(Modifier.STATIC) }
        val constructors = ElementFilter.constructorsIn(
                context.processingEnv.elementUtils.getAllMembers(element))
        context.checker.check(allStatic || constructors.isEmpty() || constructors.any {
            it.parameters.isEmpty()
        }, element, TYPE_CONVERTER_MISSING_NOARG_CONSTRUCTOR)
        return converterMethods.mapNotNull { processMethod(declaredType, it) }
    }

    private fun processMethod(
            container: DeclaredType, methodElement: ExecutableElement): CustomTypeConverter? {
        val asMember = context.processingEnv.typeUtils.asMemberOf(container, methodElement)
        val executableType = MoreTypes.asExecutable(asMember)
        val returnType = executableType.returnType
        val invalidReturnType = INVALID_RETURN_TYPES.contains(returnType.kind)
        context.checker.check(methodElement.hasAnyOf(Modifier.PUBLIC), methodElement,
                TYPE_CONVERTER_MUST_BE_PUBLIC)
        if (invalidReturnType) {
            context.logger.e(methodElement, TYPE_CONVERTER_BAD_RETURN_TYPE)
            return null
        }
        val returnTypeName = returnType.typeName()
        context.checker.notUnbound(returnTypeName, methodElement,
                TYPE_CONVERTER_UNBOUND_GENERIC)
        val params = methodElement.parameters
        if (params.size != 1) {
            context.logger.e(methodElement, TYPE_CONVERTER_MUST_RECEIVE_1_PARAM)
            return null
        }
        val param = params.map {
            MoreTypes.asMemberOf(context.processingEnv.typeUtils, container, it)
        }.first()
        context.checker.notUnbound(param.typeName(), params[0], TYPE_CONVERTER_UNBOUND_GENERIC)
        return CustomTypeConverter(container, methodElement, param, returnType)
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
