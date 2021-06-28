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
import com.google.auto.common.MoreElements.getPackage
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
        return setOf(ATTRIBUTE, APP_COMPAT_SHADOWED_ATTRIBUTES)
    }

    override fun process(
        elementsByAnnotation: ImmutableSetMultimap<String, Element>
    ): Set<Element> {
        // TODO(b/180039277): Validate that linked APIs (e.g. InspectionCompanion) are present

        val views = mergeViews(
            elementsByAnnotation[ATTRIBUTE]
                .groupBy({ asType(it.enclosingElement) }, { asExecutable(it) }),
            elementsByAnnotation[APP_COMPAT_SHADOWED_ATTRIBUTES]
                .mapTo(mutableSetOf()) { asType(it) }
        )
        val filer = processingEnv.filer

        views.forEach { generateInspectionCompanion(it, generatedAnnotation).writeTo(filer) }

        // We don't defer elements for later rounds in this processor
        return emptySet()
    }

    /** Merge shadowed and regular attributes into [View] models. */
    private fun mergeViews(
        viewsWithGetters: Map<TypeElement, List<ExecutableElement>>,
        viewsWithShadowedAttributes: Set<TypeElement>
    ): List<View> {
        return (viewsWithGetters.keys + viewsWithShadowedAttributes).mapNotNull { viewType ->
            val getterAttributes = viewsWithGetters[viewType].orEmpty().map(::parseGetter)

            if (viewType in viewsWithShadowedAttributes) {
                inferShadowedAttributes(viewType)?.let { shadowedAttributes ->
                    createView(viewType, getterAttributes + shadowedAttributes)
                }
            } else {
                createView(viewType, getterAttributes)
            }
        }
    }

    /** Parse the annotated getters of a view class into a [View]. */
    private fun createView(type: TypeElement, attributes: Collection<Attribute?>): View? {
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

        if (attributes.isEmpty() || attributes.any { it == null }) {
            return null
        }

        return View(type, attributes = attributes.filterNotNull().sortedBy { it.qualifiedName })
    }

    /** Get an [Attribute] from a method known to have an `Attribute` annotation. */
    private fun parseGetter(getter: ExecutableElement): Attribute? {
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

        if (!getter.enclosingElement.asType().isAssignableTo(VIEW)) {
            printError("@Attribute must be on a subclass of android.view.View", getter, annotation)
            return null
        }

        val match = ATTRIBUTE_VALUE.matchEntire(value)

        if (match == null) {
            if (!value.contains(':')) {
                printError("@Attribute must include namespace", getter, annotation, annotationValue)
            } else {
                printError("Invalid attribute name", getter, annotation, annotationValue)
            }
            return null
        }

        val (namespace, name) = match.destructured
        val intMapping = parseIntMapping(annotation)
        val type = inferAttributeType(getter, intMapping)

        if (!isAttributeInRFile(namespace, name)) {
            printError("Attribute $namespace:$name not found", getter, annotation)
            return null
        }

        // TODO(b/180041633): Validate consistency of int mapping

        return GetterAttribute(getter, annotation, namespace, name, type, intMapping)
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
                if (getter.returnType.isAssignableTo(COLOR)) {
                    AttributeType.COLOR
                } else {
                    AttributeType.OBJECT
                }
            else -> throw IllegalArgumentException("Unexpected attribute type")
        }
    }

    /** Determines shadowed attributes based on interfaces present on the view. */
    private fun inferShadowedAttributes(viewType: TypeElement): List<ShadowedAttribute>? {
        if (!viewType.asType().isAssignableTo(VIEW)) {
            printError(
                "@AppCompatShadowedAttributes must be on a subclass of android.view.View",
                viewType,
                viewType.getAnnotationMirror(APP_COMPAT_SHADOWED_ATTRIBUTES)
            )
            return null
        }

        if (!getPackage(viewType).qualifiedName.startsWith("androidx.appcompat.")) {
            printError(
                "@AppCompatShadowedAttributes is only supported in the androidx.appcompat package",
                viewType,
                viewType.getAnnotationMirror(APP_COMPAT_SHADOWED_ATTRIBUTES)
            )
            return null
        }

        val attributes = viewType.interfaces.flatMap {
            APP_COMPAT_INTERFACE_MAP[it.toString()].orEmpty()
        }

        if (attributes.isEmpty()) {
            printError(
                "@AppCompatShadowedAttributes is present on this view, but it does not implement " +
                    "any interfaces that indicate it has shadowed attributes.",
                viewType,
                viewType.getAnnotationMirror(APP_COMPAT_SHADOWED_ATTRIBUTES)
            )
            return null
        }

        return attributes
    }

    /** Check if an R.java file exists for [namespace] and that it contains attribute [name] */
    private fun isAttributeInRFile(namespace: String, name: String): Boolean {
        return processingEnv.elementUtils.getTypeElement("$namespace.R")
            ?.enclosedElements?.find { it.simpleName.contentEquals("attr") }
            ?.enclosedElements?.find { it.simpleName.contentEquals(name) } != null
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

        /** Fully qualified name of the `Attribute` annotation */
        const val ATTRIBUTE = "androidx.resourceinspection.annotation.Attribute"

        /** Fully qualified name of the `AppCompatShadowedAttributes` annotation */
        const val APP_COMPAT_SHADOWED_ATTRIBUTES =
            "androidx.resourceinspection.annotation.AppCompatShadowedAttributes"

        /** Fully qualified name of the platform's Color class */
        const val COLOR = "android.graphics.Color"

        /** Fully qualified name of `ColorInt` */
        const val COLOR_INT = "androidx.annotation.ColorInt"

        /** Fully qualified name of `ColorLong` */
        const val COLOR_LONG = "androidx.annotation.ColorLong"

        /** Fully qualified name of `GravityInt` */
        const val GRAVITY_INT = "androidx.annotation.GravityInt"

        /** Fully qualified name of the platform's View class */
        const val VIEW = "android.view.View"

        /**
         * Map of compat interface names in `androidx.core` to the AppCompat attributes they
         * shadow. These virtual attributes are added to the inspection companion for views within
         * AppCompat with the `@AppCompatShadowedAttributes` annotation.
         *
         * As you can tell, this is brittle. The good news is these are established platform APIs
         * from API <= 29 (the minimum for app inspection) and are unlikely to change in the
         * future. If you update this list, please update the documentation comment in
         * [androidx.resourceinspection.annotation.AppCompatShadowedAttributes] as well.
         */
        val APP_COMPAT_INTERFACE_MAP: Map<String, List<ShadowedAttribute>> = mapOf(
            "androidx.core.view.TintableBackgroundView" to listOf(
                ShadowedAttribute("backgroundTint", "getBackgroundTintList()"),
                ShadowedAttribute("backgroundTintMode", "getBackgroundTintMode()")
            ),
            "androidx.core.widget.AutoSizeableTextView" to listOf(
                ShadowedAttribute(
                    "autoSizeTextType",
                    "getAutoSizeTextType()",
                    AttributeType.INT_ENUM,
                    listOf(
                        IntMap("none", 0 /* TextView.AUTO_SIZE_TEXT_TYPE_NONE */),
                        IntMap("uniform", 1 /* TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM */),
                    )
                ),
                ShadowedAttribute(
                    "autoSizeStepGranularity", "getAutoSizeStepGranularity()", AttributeType.INT
                ),
                ShadowedAttribute(
                    "autoSizeMinTextSize", "getAutoSizeMinTextSize()", AttributeType.INT
                ),
                ShadowedAttribute(
                    "autoSizeMaxTextSize", "getAutoSizeMaxTextSize()", AttributeType.INT
                )
            ),
            "androidx.core.widget.TintableCheckedTextView" to listOf(
                ShadowedAttribute("checkMarkTint", "getCheckMarkTintList()"),
                ShadowedAttribute("checkMarkTintMode", "getCheckMarkTintMode()")
            ),
            "androidx.core.widget.TintableCompoundButton" to listOf(
                ShadowedAttribute("buttonTint", "getButtonTintList()"),
                ShadowedAttribute("buttonTintMode", "getButtonTintMode()")
            ),
            "androidx.core.widget.TintableCompoundDrawablesView" to listOf(
                ShadowedAttribute("drawableTint", "getCompoundDrawableTintList()"),
                ShadowedAttribute("drawableTintMode", "getCompoundDrawableTintMode()")
            ),
            "androidx.core.widget.TintableImageSourceView" to listOf(
                ShadowedAttribute("tint", "getImageTintList()"),
                ShadowedAttribute("tintMode", "getImageTintMode()"),
            )
        )
    }
}
