/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.resourceinspection.processor

import com.google.auto.common.AnnotationMirrors.getAnnotationValue
import com.google.auto.common.BasicAnnotationProcessor
import com.google.auto.common.GeneratedAnnotationSpecs.generatedAnnotationSpec
import com.google.auto.common.MoreElements.asExecutable
import com.google.auto.common.MoreElements.asType
import com.google.auto.common.Visibility
import com.google.auto.common.Visibility.effectiveVisibilityOfElement
import com.google.common.collect.ImmutableSetMultimap
import com.squareup.javapoet.AnnotationSpec
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic

/** Processing step for generating layout inspection companions from `Attribute` annotations. */
internal class LayoutInspectionStep(
    private val processingEnv: ProcessingEnvironment,
    processorClass: Class<out Processor>
) : BasicAnnotationProcessor.Step {
    private val generatedAnnotation: AnnotationSpec? = generatedAnnotationSpec(
        processingEnv.elementUtils,
        processingEnv.sourceVersion,
        processorClass
    ).orElse(null)

    override fun annotations(): Set<String> {
        return setOf(ATTRIBUTE)
    }

    override fun process(
        elementsByAnnotation: ImmutableSetMultimap<String, Element>
    ): Set<Element> {
        // TODO(b/180039277): Validate that linked APIs (e.g. InspectionCompanion) are present
        elementsByAnnotation[ATTRIBUTE]
            .map { asExecutable(it) }
            .groupBy { asType(it.enclosingElement) }
            .forEach { (type, getters) ->
                parseView(type, getters)?.let { view ->
                    generateInspectionCompanion(view, generatedAnnotation)
                        .writeTo(processingEnv.filer)
                }
            }
        return emptySet()
    }

    /** Parse the annotated getters of a view class into a [View]. */
    private fun parseView(type: TypeElement, getters: Iterable<ExecutableElement>): View? {
        if (!type.asType().isAssignableTo("android.view.View")) {
            getters.forEach { getter ->
                printError(
                    "@Attribute must only annotate subclasses of android.view.View",
                    getter,
                    getter.getAnnotationMirror(ATTRIBUTE)
                )
            }
            return null
        }

        val attributes = getters.map(::parseAttribute)

        val duplicateAttributes = attributes
            .filterNotNull()
            .groupBy { it.qualifiedName }
            .values
            .filter { it.size > 1 }

        if (duplicateAttributes.any()) {
            duplicateAttributes.forEach { duplicates ->
                duplicates.forEach { attribute ->
                    val qualifiedName = attribute.qualifiedName
                    val otherGetters = duplicates
                        .filter { it.invocation != attribute.invocation }
                        .joinToString { it.invocation }

                    printError(
                        "Duplicate attribute $qualifiedName is also present on $otherGetters",
                        (attribute as? GetterAttribute)?.getter,
                        (attribute as? GetterAttribute)?.annotation
                    )
                }
            }
            return null
        }

        if (attributes.any { it == null }) {
            return null
        }

        return View(type, attributes = attributes.filterNotNull().sortedBy { it.qualifiedName })
    }

    /** Get an [Attribute] from a method known to have an `Attribute` annotation. */
    private fun parseAttribute(getter: ExecutableElement): Attribute? {
        val annotation = getter.getAnnotationMirror(ATTRIBUTE)!!
        val annotationValue = getAnnotationValue(annotation, "value")
        val value = annotationValue.value as String

        if (getter.parameters.isNotEmpty() || getter.returnType.kind == TypeKind.VOID) {
            printError("@Attribute must annotate a getter", getter, annotation)
            return null
        }

        if (effectiveVisibilityOfElement(getter) != Visibility.PUBLIC) {
            printError("@Attribute getter must be public", getter, annotation)
            return null
        }

        val match = ATTRIBUTE_VALUE.matchEntire(value)

        return if (match != null) {
            val (namespace, name) = match.destructured
            val intMapping = parseIntMapping(annotation)
            val type = inferAttributeType(getter, intMapping)

            // TODO(b/180041203): Verify attribute ID or at least existence of R files
            // TODO(b/180041633): Validate consistency of int mapping

            GetterAttribute(getter, annotation, namespace, name, type, intMapping)
        } else if (!value.contains(':')) {
            printError("Attribute name must include namespace", getter, annotation, annotationValue)
            null
        } else {
            printError("Invalid attribute name", getter, annotation, annotationValue)
            null
        }
    }

    /** Parse `Attribute.intMapping`. */
    private fun parseIntMapping(annotation: AnnotationMirror): List<IntMap> {
        return (getAnnotationValue(annotation, "intMapping").value as List<*>).map { entry ->
            val intMapAnnotation = (entry as AnnotationValue).value as AnnotationMirror

            IntMap(
                name = getAnnotationValue(intMapAnnotation, "name").value as String,
                value = getAnnotationValue(intMapAnnotation, "value").value as Int,
                mask = getAnnotationValue(intMapAnnotation, "mask").value as Int,
            )
        }.sortedBy { it.value }
    }

    /** Map the getter's annotations and return type to the internal attribute type. */
    private fun inferAttributeType(
        getter: ExecutableElement,
        intMapping: List<IntMap>
    ): AttributeType {
        return when (getter.returnType.kind) {
            TypeKind.BOOLEAN -> AttributeType.BOOLEAN
            TypeKind.BYTE -> AttributeType.BYTE
            TypeKind.CHAR -> AttributeType.CHAR
            TypeKind.DOUBLE -> AttributeType.DOUBLE
            TypeKind.FLOAT -> AttributeType.FLOAT
            TypeKind.SHORT -> AttributeType.SHORT
            TypeKind.INT -> when {
                getter.isAnnotationPresent(COLOR_INT) -> AttributeType.COLOR
                getter.isAnnotationPresent(GRAVITY_INT) -> AttributeType.GRAVITY
                getter.hasResourceIdAnnotation() -> AttributeType.RESOURCE_ID
                intMapping.any { it.mask != 0 } -> AttributeType.INT_FLAG
                intMapping.isNotEmpty() -> AttributeType.INT_ENUM
                else -> AttributeType.INT
            }
            TypeKind.LONG ->
                if (getter.isAnnotationPresent(COLOR_LONG)) {
                    AttributeType.COLOR
                } else {
                    AttributeType.LONG
                }
            TypeKind.DECLARED, TypeKind.ARRAY ->
                if (getter.returnType.isAssignableTo("android.graphics.Color")) {
                    AttributeType.COLOR
                } else {
                    // TODO(b/180041034): Validate object types and unbox primitives
                    AttributeType.OBJECT
                }
            else -> throw IllegalArgumentException("Unexpected attribute type")
        }
    }

    private fun Element.hasResourceIdAnnotation(): Boolean {
        return this.annotationMirrors.any {
            asType(it.annotationType.asElement()).qualifiedName matches RESOURCE_ID_ANNOTATION
        }
    }

    private fun TypeMirror.isAssignableTo(typeName: String): Boolean {
        val assignableType = requireNotNull(processingEnv.elementUtils.getTypeElement(typeName)) {
            "Expected $typeName to exist"
        }
        return processingEnv.typeUtils.isAssignable(this, assignableType.asType())
    }

    /** Convenience wrapper for [javax.annotation.processing.Messager.printMessage]. */
    private fun printError(
        message: String,
        element: Element?,
        annotation: AnnotationMirror? = null,
        value: AnnotationValue? = null
    ) {
        processingEnv.messager.printMessage(
            Diagnostic.Kind.ERROR,
            message,
            element,
            annotation,
            value
        )
    }

    /** Find an annotation mirror by its qualified name */
    private fun Element.getAnnotationMirror(qualifiedName: String): AnnotationMirror? {
        return this.annotationMirrors.firstOrNull { annotation ->
            asType(annotation.annotationType.asElement())
                .qualifiedName.contentEquals(qualifiedName)
        }
    }

    /** True if the supplied annotation name is present on the element */
    private fun Element.isAnnotationPresent(qualifiedName: String): Boolean {
        return getAnnotationMirror(qualifiedName) != null
    }

    private companion object {
        /** Regex for validating and parsing attribute name and namespace. */
        val ATTRIBUTE_VALUE = """(\w+(?:\.\w+)*):(\w+)""".toRegex()

        /** Regex for matching resource ID annotations. */
        val RESOURCE_ID_ANNOTATION = """androidx?\.annotation\.[A-Z]\w+Res""".toRegex()

        /** Fully qualified name of the `Attribute` annotation` */
        const val ATTRIBUTE = "androidx.resourceinspection.annotation.Attribute"

        /** Fully qualified name of `ColorInt` */
        const val COLOR_INT = "androidx.annotation.ColorInt"

        /** Fully qualified name of `ColorLong` */
        const val COLOR_LONG = "androidx.annotation.ColorLong"

        /** Fully qualified name of `GravityInt` */
        const val GRAVITY_INT = "androidx.annotation.GravityInt"
    }
}
