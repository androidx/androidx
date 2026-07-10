/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.appsearch.compiler

import androidx.appsearch.compiler.AnnotatedGetterOrField.ElementTypeCategory
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XExecutableElement
import androidx.room.compiler.processing.XFieldElement
import androidx.room.compiler.processing.XHasModifiers
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.isConstructor
import androidx.room.compiler.processing.isMethod
import androidx.room.compiler.processing.isTypeElement
import java.util.Locale
import java.util.stream.Collectors

/**
 * Info about how to construct a class annotated with `@Document`, aka the document class.
 *
 * This has two components:
 * 1. A constructor/static [CreationMethod] that takes in **N** params, each corresponding to an
 *    [AnnotatedGetterOrField] and returns either the document class or a builder.
 * 2. A set of **M** setters/fields on the object returned by the [CreationMethod].
 *
 * Note: Fields only apply if [CreationMethod.returnsDocumentClass] since it is assumed that
 * builders cannot have fields. When [CreationMethod.returnsBuilder], this only contains setters.
 *
 * **N + M** collectively encompass all of the annotated getters/fields in the document class.
 *
 * For example:
 * <pre>
 * @Document
 * class DocumentClass {
 *     public DocumentClass(String id, String namespace, int someProp) {...}
 * //         ^^^^^^^^^^^^^
 * //       Creation method
 *
 *     @Document.Id
 *     public String getId() {...}
 *
 *     @Document.Namespace
 *     public String getNamespace() {...}
 *
 *     @Document.LongProperty
 *     public int getSomeProp() {...}
 *
 *     @Document.StringProperty
 *     public String getOtherProp() {...}
 *     public void setOtherProp(String otherProp) {...}
 * //              ^^^^^^^^^^^^
 * //                 setter
 *
 *     @Document.BooleanProperty
 *     public boolean mYetAnotherProp;
 * //                 ^^^^^^^^^^^^^^^
 * //                      field
 * }
 * </pre>
 */
data class DocumentClassCreationInfo(
    /** The creation method. */
    val creationMethod: CreationMethod,

    /**
     * Maps an annotated getter/field to the corresponding setter/field on the object returned by
     * the [CreationMethod].
     */
    val settersAndFields: Map<AnnotatedGetterOrField, SetterOrField>,
) {
    companion object {
        /** Infers the [DocumentClassCreationInfo] for a specified document class. */
        @Throws(XProcessingException::class)
        @JvmStatic
        fun infer(
            documentClass: XTypeElement,
            annotatedGettersAndFields: Set<AnnotatedGetterOrField>,
            helper: IntrospectionHelper,
        ): DocumentClassCreationInfo {
            val builderProducer = BuilderProducer.tryCreate(documentClass, helper)

            val settersAndFields = LinkedHashMap<AnnotatedGetterOrField, SetterOrField>()
            val setterNotFoundErrors = mutableListOf<XProcessingException>()
            for (getterOrField in annotatedGettersAndFields) {
                if (
                    builderProducer == null &&
                        getterOrField.isField &&
                        fieldCanBeSetDirectly((getterOrField.element as XFieldElement))
                ) {
                    // annotated field on the document class itself
                    settersAndFields[getterOrField] = SetterOrField(getterOrField.element)
                } else {
                    // Annotated getter|annotated private field|must use builder pattern
                    try {
                        val targetClass: XTypeElement =
                            if (builderProducer == null) {
                                documentClass
                            } else {
                                builderProducer.builderType.typeElement!!
                            }
                        val setter = findSetter(targetClass, getterOrField, helper)
                        settersAndFields[getterOrField] = SetterOrField(setter)
                    } catch (e: XProcessingException) {
                        setterNotFoundErrors.add(e)
                    }
                }
            }

            val potentialCreationMethods =
                extractPotentialCreationMethods(
                    documentClass,
                    annotatedGettersAndFields,
                    builderProducer,
                    helper,
                )

            // Start building the exception in case we don't find a suitable creation method
            val remainingGettersAndFields = annotatedGettersAndFields - settersAndFields.keys
            val exception =
                XProcessingException(
                    ("Could not find a suitable %s for \"%s\" that covers properties: [%s]. " +
                            "See the warnings for more details.")
                        .format(
                            if (builderProducer == null) {
                                "constructor/factory method"
                            } else {
                                "builder producer"
                            },
                            documentClass.qualifiedName,
                            getCommaSeparatedJvmNames(remainingGettersAndFields),
                        ),
                    documentClass,
                )
            exception.addWarnings(setterNotFoundErrors)

            // Pick the first creation method that covers the annotated getters/fields that we don't
            // already have setters/fields for
            for (creationMethod in potentialCreationMethods) {
                val missingParams = remainingGettersAndFields - creationMethod.paramAssociations
                if (missingParams.isNotEmpty()) {
                    exception.addWarning(
                        XProcessingException(
                            ("Cannot use this %s to construct the class: \"%s\". " +
                                    "No parameters for the properties: [%s]")
                                .format(
                                    if (creationMethod.isConstructor) {
                                        "constructor"
                                    } else {
                                        "creation method"
                                    },
                                    documentClass.qualifiedName,
                                    getCommaSeparatedJvmNames(missingParams),
                                ),
                            creationMethod.element,
                        )
                    )
                    continue
                }
                // found one!
                // This creation method may cover properties that we already have setters for.
                // If so, forget those setters.
                for (getterOrField in creationMethod.paramAssociations) {
                    settersAndFields.remove(getterOrField)
                }
                return DocumentClassCreationInfo(creationMethod, settersAndFields)
            }

            throw exception
        }

        /**
         * Finds a setter corresponding to the getter/field within the specified class.
         *
         * @throws XProcessingException if no suitable setter was found within the specified class.
         */
        @Throws(XProcessingException::class)
        private fun findSetter(
            clazz: XTypeElement,
            getterOrField: AnnotatedGetterOrField,
            helper: IntrospectionHelper,
        ): XExecutableElement {
            val setterNames = getAcceptableSetterNames(getterOrField)
            // Start building the exception in case we don't find a suitable setter
            val setterSignatures =
                setterNames
                    .stream()
                    .map { "[public] void $it(${getterOrField.jvmType})" }
                    .collect(Collectors.joining("|"))
            val exception =
                XProcessingException(
                    "Could not find any of the setter(s): $setterSignatures",
                    getterOrField.element,
                )

            val potentialSetters =
                helper
                    .getAllMethods(clazz)
                    .filter { method: IntrospectionHelper.MethodTypeAndElement ->
                        setterNames.contains(method.element.name)
                    }
                    .toList()
            for (method in potentialSetters) {
                if (method.element.isPrivate()) {
                    exception.addWarning(
                        XProcessingException(
                            "Setter cannot be used: private visibility",
                            method.element,
                        )
                    )
                    continue
                }
                if (method.element.isStatic()) {
                    exception.addWarning(
                        XProcessingException("Setter cannot be used: static method", method.element)
                    )
                    continue
                }
                if (method.element.parameters.size != 1) {
                    exception.addWarning(
                        XProcessingException(
                            ("Setter cannot be used: takes ${method.element.parameters.size} " +
                                "parameters instead of 1"),
                            method.element,
                        )
                    )
                    continue
                }
                // found one!
                return method.element
            }

            throw exception
        }

        private fun getAcceptableSetterNames(getterOrField: AnnotatedGetterOrField): Set<String> {
            // String mField -> {field(String), setField(String)}
            // String getProp() -> {prop(String), setProp(String)}
            // List<String> getProps() -> {props(List), setProps(List), addProps(List)}
            val setterNames = mutableSetOf<String>()
            val normalizedName = getterOrField.normalizedName
            setterNames.add(normalizedName)
            val pascalCase =
                normalizedName.substring(0, 1).uppercase(Locale.getDefault()) +
                    normalizedName.substring(1)
            setterNames.add("set$pascalCase")
            when (getterOrField.elementTypeCategory) {
                ElementTypeCategory.SINGLE -> {}
                ElementTypeCategory.COLLECTION,
                ElementTypeCategory.ARRAY -> setterNames.add("add$pascalCase")
            }
            return setterNames
        }

        private fun fieldCanBeSetDirectly(field: XFieldElement): Boolean {
            return !field.isPrivate() && !field.isFinal()
        }

        /**
         * Extracts potential creation methods for the document class.
         *
         * Returns creation methods corresponding to the [BuilderProducer], when it is not null.
         *
         * @throws XProcessingException if no viable creation methods could be extracted.
         */
        @Throws(XProcessingException::class)
        private fun extractPotentialCreationMethods(
            documentClass: XTypeElement,
            annotatedGettersAndFields: Set<AnnotatedGetterOrField>,
            builderProducer: BuilderProducer?,
            helper: IntrospectionHelper,
        ): List<CreationMethod> {
            val potentialMethods: List<XExecutableElement> =
                if (builderProducer != null && builderProducer.isStaticMethod) {
                    listOf(builderProducer.element as XExecutableElement)
                } else {
                    // Use the constructors & factory methods on the document class or builder class
                    // itself
                    val targetClass =
                        if (builderProducer == null) {
                            documentClass
                        } else {
                            builderProducer.element as XTypeElement
                        }
                    targetClass
                        .getEnclosedElements()
                        .stream()
                        .filter { element: XElement ->
                            element.isConstructor() || helper.isStaticFactoryMethod(element)
                        }
                        .map { element: XElement -> element as XExecutableElement }
                        .toList()
                }

            // Start building an exception in case none of the candidates are suitable
            val exception =
                XProcessingException("Could not find a suitable creation method", documentClass)

            val creationMethods = mutableListOf<CreationMethod>()
            for (candidate in potentialMethods) {
                try {
                    creationMethods.add(
                        CreationMethod.inferParamAssociationsAndCreate(
                            candidate,
                            annotatedGettersAndFields,
                            /* returnsDocumentClass= */ builderProducer == null,
                        )
                    )
                } catch (e: XProcessingException) {
                    exception.addWarning(e)
                }
            }

            if (creationMethods.isEmpty()) {
                throw exception
            }

            return creationMethods
        }

        private fun getCommaSeparatedJvmNames(
            gettersAndFields: Collection<AnnotatedGetterOrField>
        ): String {
            return gettersAndFields
                .stream()
                .map(AnnotatedGetterOrField::jvmName)
                .collect(Collectors.joining(", "))
        }
    }

    /**
     * Represents a static method/nested class within a document class annotated with
     * `@Document.BuilderProducer`. For example:
     * <pre>
     * @Document
     * public class MyEntity {
     *     @Document.BuilderProducer
     *     public static Builder newBuilder();
     *
     *     // This class may directly be annotated with @Document.BuilderProducer instead
     *     public static class Builder {...}
     * }
     * </pre>
     */
    private class BuilderProducer(
        /** The static method/nested class annotated with `@Document.BuilderProducer`. */
        val element: XElement,

        /** The return type of the annotated method or the annotated builder class. */
        val builderType: XType,
    ) {
        companion object {
            @Throws(XProcessingException::class)
            fun tryCreate(
                documentClass: XTypeElement,
                helper: IntrospectionHelper,
            ): BuilderProducer? {
                val annotatedElements: List<XElement> =
                    documentClass
                        .getEnclosedElements()
                        .stream()
                        .filter(::isAnnotatedWithBuilderProducer)
                        .toList()
                if (annotatedElements.isEmpty()) {
                    return null
                } else if (annotatedElements.size > 1) {
                    throw XProcessingException("Found duplicated builder producer", documentClass)
                }

                val annotatedElement = annotatedElements[0]
                requireBuilderProducerAccessible(annotatedElement)
                // Since @Document.BuilderProducer is configured with
                // @Target({ElementType.METHOD, ElementType.TYPE}), this should never throw in
                // practice.
                requireBuilderProducerIsMethodOrClass(annotatedElement)

                val builderType: XType =
                    if (annotatedElement.isMethod()) {
                        val method = annotatedElement as XMethodElement
                        requireIsDeclaredTypeWithBuildMethod(
                            method.returnType,
                            documentClass,
                            annotatedElement,
                            helper,
                        )
                        method.returnType
                    } else {
                        // A class is annotated with @Document.BuilderProducer. Use its constructors
                        // as the creation methods.
                        val builderClass = annotatedElement as XTypeElement
                        requireIsDeclaredTypeWithBuildMethod(
                            builderClass.type,
                            documentClass,
                            annotatedElement,
                            helper,
                        )
                        annotatedElement.type
                    }

                return BuilderProducer(annotatedElement, builderType)
            }

            private fun isAnnotatedWithBuilderProducer(element: XElement): Boolean {
                return !IntrospectionHelper.getAnnotations(
                        element,
                        IntrospectionHelper.BUILDER_PRODUCER_CLASS,
                    )
                    .isEmpty()
            }

            /** Makes sure the annotated element is a builder/class. */
            @Throws(XProcessingException::class)
            private fun requireBuilderProducerIsMethodOrClass(annotatedElement: XElement) {
                if (!annotatedElement.isMethod() && !annotatedElement.isTypeElement()) {
                    throw XProcessingException(
                        "Builder producer must be a method or a class",
                        annotatedElement,
                    )
                }
            }

            /** Makes sure the annotated element is static and not private. */
            @Throws(XProcessingException::class)
            private fun requireBuilderProducerAccessible(annotatedElement: XElement) {
                annotatedElement as XHasModifiers
                if (!annotatedElement.isStatic()) {
                    throw XProcessingException("Builder producer must be static", annotatedElement)
                }
                if (annotatedElement.isPrivate()) {
                    throw XProcessingException(
                        "Builder producer cannot be private",
                        annotatedElement,
                    )
                }
            }

            /**
             * Makes sure the builder type is a non-private & non-static method of the form
             * `DocumentClass build()`.
             *
             * @param annotatedElement The method/class annotated with `@Document.BuilderProducer`.
             * @throws XProcessingException on the annotated element if the conditions are not met.
             */
            @Throws(XProcessingException::class)
            private fun requireIsDeclaredTypeWithBuildMethod(
                builderType: XType,
                documentClass: XTypeElement,
                annotatedElement: XElement,
                helper: IntrospectionHelper,
            ) {
                val exception =
                    XProcessingException(
                        ("Invalid builder producer: $builderType does not have a method " +
                            "$documentClass build()"),
                        annotatedElement,
                    )
                val typeElement = builderType.typeElement
                if (typeElement == null) {
                    throw exception
                }
                var hasBuildMethod = false
                for (method: IntrospectionHelper.MethodTypeAndElement in
                    helper.getAllMethods(typeElement)) {
                    if (
                        method.element.name == "build" &&
                            !method.element.isStatic() &&
                            !method.element.isPrivate() &&
                            documentClass.type.isAssignableFrom(method.type.returnType) &&
                            method.type.parameterTypes.isEmpty()
                    ) {
                        hasBuildMethod = true
                        break
                    }
                }
                if (!hasBuildMethod) {
                    throw exception
                }
            }
        }

        val isStaticMethod: Boolean
            get() = element.isMethod()
    }
}
