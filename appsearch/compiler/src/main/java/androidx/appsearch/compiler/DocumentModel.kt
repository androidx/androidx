/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.annotation.RestrictTo
import androidx.appsearch.compiler.AnnotatedGetterOrField.Companion.tryCreateFor
import androidx.appsearch.compiler.DocumentClassCreationInfo.Companion.infer
import androidx.appsearch.compiler.IntrospectionHelper.Companion.generateClassHierarchy
import androidx.appsearch.compiler.IntrospectionHelper.Companion.getDocumentAnnotation
import androidx.appsearch.compiler.annotationwrapper.DataPropertyAnnotation
import androidx.appsearch.compiler.annotationwrapper.MetadataPropertyAnnotation
import androidx.appsearch.compiler.annotationwrapper.PropertyAnnotation
import androidx.room.compiler.processing.ExperimentalProcessingApi
import androidx.room.compiler.processing.XAnnotation
import androidx.room.compiler.processing.XAnnotationValue
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.compat.XConverters.toJavac
import java.util.Objects
import java.util.function.Predicate
import java.util.stream.Collectors
import javax.lang.model.element.Element

/**
 * Processes @Document annotations.
 *
 * @see AnnotatedGetterAndFieldAccumulator for the DocumentModel's invariants with regards to its
 *   getter and field definitions.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@OptIn(ExperimentalProcessingApi::class)
internal class DocumentModel
private constructor(
    private val env: XProcessingEnv,
    clazz: XTypeElement,
    generatedAutoValueElement: XTypeElement?,
) {
    val classElement: XTypeElement

    /**
     * The name of the original class annotated with @Document
     *
     * @return the class name
     */
    // The name of the original class annotated with @Document
    val qualifiedDocumentClassName: String

    val schemaName: String

    /** Returns the set of parent classes specified in @Document via the "parent" parameter. */
    val parentTypes: LinkedHashSet<XTypeElement>

    /**
     * Returns all getters/fields (declared or inherited) annotated with some [PropertyAnnotation].
     */
    val annotatedGettersAndFields: LinkedHashSet<AnnotatedGetterOrField>

    /** Returns the getter/field annotated with `@Document.Id`. */
    val idAnnotatedGetterOrField: AnnotatedGetterOrField

    /** Returns the getter/field annotated with `@Document.Namespace`. */
    val namespaceAnnotatedGetterOrField: AnnotatedGetterOrField

    val documentClassCreationInfo: DocumentClassCreationInfo

    private val helper = IntrospectionHelper(env)
    private val accessors: Map<AnnotatedGetterOrField, PropertyAccessor>

    init {
        if (clazz.isPrivate()) {
            throw XProcessingException("@Document annotated class is private", clazz)
        }
        classElement = clazz
        qualifiedDocumentClassName = generatedAutoValueElement?.qualifiedName ?: clazz.qualifiedName
        parentTypes = getParentSchemaTypes(clazz)

        val classHierarchy: List<XTypeElement> = generateClassHierarchy(clazz)
        schemaName = computeSchemaName(classHierarchy)
        annotatedGettersAndFields = scanAnnotatedGettersAndFields(classHierarchy)

        requireNoDuplicateMetadataProperties()
        idAnnotatedGetterOrField =
            requireGetterOrFieldMatchingPredicate(
                { getterOrField: AnnotatedGetterOrField ->
                    getterOrField.annotation === MetadataPropertyAnnotation.ID
                },
                errorMessage =
                    "All @Document classes must have exactly one field annotated with @Id",
            )
        namespaceAnnotatedGetterOrField =
            requireGetterOrFieldMatchingPredicate(
                { getterOrField: AnnotatedGetterOrField ->
                    getterOrField.annotation === MetadataPropertyAnnotation.NAMESPACE
                },
                errorMessage =
                    "All @Document classes must have exactly one field annotated with @Namespace",
            )

        val allMethods =
            helper
                .getAllMethods(clazz)
                .stream()
                .map(IntrospectionHelper.MethodTypeAndElement::element)
                .toList()
        accessors = inferPropertyAccessors(annotatedGettersAndFields, allMethods, helper)
        documentClassCreationInfo = infer(clazz, annotatedGettersAndFields, helper)
    }

    @Throws(XProcessingException::class)
    private fun scanAnnotatedGettersAndFields(
        hierarchy: List<XTypeElement>
    ): LinkedHashSet<AnnotatedGetterOrField> {
        /*
         * XProcessing seems to change the order of enclosed elements, since getEnclosedElements is:
         * mutableListOf<XElement>().apply {
         *   addAll(getEnclosedTypeElements())
         *   addAll(getDeclaredFields())
         *   addAll(getConstructors())
         *   addAll(getDeclaredMethods())
         * }
         *
         * Thus, we will use the javac version of getEnclosedElements to manage the order.
         */
        val javacElementToXElement = mutableMapOf<Element, XElement>()
        for (type in hierarchy) {
            for (enclosedElement in type.getEnclosedElements()) {
                javacElementToXElement.put(enclosedElement.toJavac(), enclosedElement)
            }
        }

        val accumulator = AnnotatedGetterAndFieldAccumulator()
        for (type in hierarchy) {
            for (enclosedElementJavac in type.toJavac().enclosedElements) {
                val enclosedElement: XElement =
                    javacElementToXElement.getValue(enclosedElementJavac)
                val getterOrField = tryCreateFor(enclosedElement, env)
                if (getterOrField == null) {
                    continue
                }
                accumulator.add(getterOrField)
            }
        }
        return accumulator.accumulatedGettersAndFields
    }

    /**
     * Makes sure [.mAnnotatedGettersAndFields] does not contain two getters/fields annotated with
     * the same metadata annotation e.g. it doesn't make sense for a document to have two
     * `@Document.Id`s.
     */
    @Throws(XProcessingException::class)
    private fun requireNoDuplicateMetadataProperties() {
        val annotationToGettersAndFields =
            annotatedGettersAndFields
                .stream()
                .filter { getterOrField: AnnotatedGetterOrField ->
                    getterOrField.annotation.propertyKind ==
                        PropertyAnnotation.Kind.METADATA_PROPERTY
                }
                .collect(
                    Collectors.groupingBy { getterOrField: AnnotatedGetterOrField ->
                        getterOrField.annotation as MetadataPropertyAnnotation
                    }
                )
        for (entry in annotationToGettersAndFields.entries) {
            val annotation: MetadataPropertyAnnotation = entry.key
            val gettersAndFields: List<AnnotatedGetterOrField> = entry.value
            if (gettersAndFields.size > 1) {
                // Can show the error on any of the duplicates. Just pick the first first.
                throw XProcessingException(
                    "Duplicate member annotated with @" + annotation.className.simpleName,
                    gettersAndFields[0].element,
                )
            }
        }
    }

    /**
     * Makes sure [.mAnnotatedGettersAndFields] contains a getter/field that matches the predicate.
     *
     * @return The matched getter/field.
     * @throws XProcessingException with the error message if no match.
     */
    @Throws(XProcessingException::class)
    private fun requireGetterOrFieldMatchingPredicate(
        predicate: Predicate<AnnotatedGetterOrField>,
        errorMessage: String,
    ): AnnotatedGetterOrField {
        return annotatedGettersAndFields.stream().filter(predicate).findFirst().orElseThrow {
            XProcessingException(errorMessage, classElement)
        }
    }

    /**
     * Returns the public/package-private accessor for an annotated getter/field (may be private).
     */
    fun getAccessor(getterOrField: AnnotatedGetterOrField): PropertyAccessor {
        val accessor: PropertyAccessor = accessors.getValue(getterOrField)
        requireNotNull(accessor) {
            "No such getter/field belongs to this DocumentModel: $getterOrField"
        }
        return accessor
    }

    /** Returns the parent types mentioned within the `@Document` annotation. */
    @Throws(XProcessingException::class)
    private fun getParentSchemaTypes(documentClass: XTypeElement): LinkedHashSet<XTypeElement> {
        val documentAnnotation =
            Objects.requireNonNull<XAnnotation>(getDocumentAnnotation(documentClass))
        val params: Map<String, XAnnotationValue> = helper.getAnnotationParams(documentAnnotation)
        val parentsSchemaTypes = LinkedHashSet<XTypeElement>()
        val parentsParam = params.getValue("parent").value
        if (parentsParam is List<*>) {
            for (parent in parentsParam) {
                val parentClassName =
                    if (parent is String) {
                        parent
                    } else {
                        val parentType = (parent as XAnnotationValue).asType()
                        parentType.typeElement!!.qualifiedName
                    }
                parentsSchemaTypes.add(env.findTypeElement(parentClassName)!!)
            }
        }
        if (!parentsSchemaTypes.isEmpty() && params.getValue("name").asString().isEmpty()) {
            throw XProcessingException(
                "All @Document classes with a parent must explicitly provide a name",
                classElement,
            )
        }
        return parentsSchemaTypes
    }

    /**
     * Computes the schema name for a Document class given its hierarchy of parent @Document
     * classes.
     *
     * The schema name will be the most specific Document class that has an explicit schema name, to
     * allow the schema name to be manually set with the "name" annotation. If no such Document
     * class exists, use the name of the root Document class, so that performing a query on the base
     * \@Document class will also return child @Document classes.
     *
     * @param hierarchy List of classes annotated with \@Document, with the root class at the
     *   beginning and the final class at the end
     * @return the final schema name for the class at the end of the hierarchy
     */
    private fun computeSchemaName(hierarchy: List<XTypeElement>): String {
        for (i in hierarchy.indices.reversed()) {
            val documentAnnotation = getDocumentAnnotation(hierarchy[i])
            if (documentAnnotation == null) {
                continue
            }
            val params: Map<String, XAnnotationValue> =
                helper.getAnnotationParams(documentAnnotation)
            val name = params.getValue("name").asString()
            if (!name.isEmpty()) {
                return name
            }
        }
        // Nobody had a name annotation -- use the class name of the root document in the hierarchy
        val rootDocumentClass = hierarchy[0]
        val rootDocumentAnnotation = getDocumentAnnotation(rootDocumentClass)
        if (rootDocumentAnnotation == null) {
            return classElement.name
        }
        // Documents don't need an explicit name annotation, can use the class name
        return rootDocumentClass.name
    }

    /**
     * Accumulates and de-duplicates [AnnotatedGetterOrField]s within a class hierarchy and ensures
     * all of the following:
     * 1. The same getter/field doesn't appear in the class hierarchy with different annotation
     *    types e.g.
     * <pre>
     * `class Parent {
     *
     * public String getProp();
     * }
     *
     *
     * class Child extends Parent {
     *
     * public String getProp();
     * }
     * ` *
     * </pre> *
     * 1. The same getter/field doesn't appear twice with different serialized names e.g.
     * <pre>
     * `class Parent {
     *
     * public String getProp();
     * }
     *
     *
     * class Child extends Parent {
     *
     * public String getProp();
     * }
     * ` *
     * </pre> *
     * 1. The same serialized name doesn't appear on two separate getters/fields e.g.
     * <pre>
     * `class Gift {
     *
     * String mField;
     *
     *
     * Long getProp();
     * }
     * ` *
     * </pre> *
     * 1. Two annotated element do not have the same normalized name because this hinders with
     *    downstream logic that tries to infer [CreationMethod]s e.g.
     * <pre>
     * `class Gift {
     *
     * String mFoo;
     *
     *
     * String getFoo() {...}
     * void setFoo(String value) {...}
     * }
     * ` *
     * </pre> *
     *
     * @see CreationMethod.inferParamAssociationsAndCreate
     */
    private class AnnotatedGetterAndFieldAccumulator {
        private val jvmNameToGetterOrField = LinkedHashMap<String, AnnotatedGetterOrField>()
        private val serializedNameToGetterOrField = mutableMapOf<String, AnnotatedGetterOrField>()
        private val normalizedNameToGetterOrField = mutableMapOf<String, AnnotatedGetterOrField>()

        /**
         * Adds the [AnnotatedGetterOrField] to the accumulator.
         *
         * [AnnotatedGetterOrField] that appear again are considered to be overridden versions and
         * replace the older ones.
         *
         * Hence, this method should be called with [AnnotatedGetterOrField]s from the least
         * specific types to the most specific type.
         */
        @Throws(XProcessingException::class)
        fun add(getterOrField: AnnotatedGetterOrField) {
            val jvmName = getterOrField.jvmName
            val existingGetterOrField = jvmNameToGetterOrField[jvmName]

            if (existingGetterOrField == null) {
                // First time we're seeing this getter or field
                jvmNameToGetterOrField.put(jvmName, getterOrField)

                requireUniqueNormalizedName(getterOrField)
                normalizedNameToGetterOrField.put(getterOrField.normalizedName, getterOrField)

                if (hasDataPropertyAnnotation(getterOrField)) {
                    requireSerializedNameNeverSeenBefore(getterOrField)
                    serializedNameToGetterOrField.put(
                        getSerializedName(getterOrField),
                        getterOrField,
                    )
                }
            } else {
                // Seen this getter or field before. It showed up again because of overriding.
                requireAnnotationTypeIsConsistent(existingGetterOrField, getterOrField)
                // Replace the old entries
                jvmNameToGetterOrField.put(jvmName, getterOrField)
                normalizedNameToGetterOrField.put(getterOrField.normalizedName, getterOrField)

                if (hasDataPropertyAnnotation(getterOrField)) {
                    requireSerializedNameIsConsistent(existingGetterOrField, getterOrField)
                    // Replace the old entry
                    serializedNameToGetterOrField.put(
                        getSerializedName(getterOrField),
                        getterOrField,
                    )
                }
            }
        }

        val accumulatedGettersAndFields: LinkedHashSet<AnnotatedGetterOrField>
            get() = LinkedHashSet(jvmNameToGetterOrField.values)

        /**
         * Makes sure the getter/field's normalized name either never appeared before, or if it did,
         * did so for the same getter/field and re-appeared likely because of overriding.
         */
        @Throws(XProcessingException::class)
        fun requireUniqueNormalizedName(getterOrField: AnnotatedGetterOrField) {
            val existingGetterOrField = normalizedNameToGetterOrField[getterOrField.normalizedName]
            if (existingGetterOrField == null) {
                // Never seen this normalized name before
                return
            }
            if (existingGetterOrField.jvmName == getterOrField.jvmName) {
                // Same getter/field appeared again (likely because of overriding). Ok.
                return
            }
            throw XProcessingException(
                "Normalized name \"${getterOrField.normalizedName}\" is already taken up by " +
                    "pre-existing ${createSignatureString(existingGetterOrField)}. " +
                    "Please rename this getter/field to something else.",
                getterOrField.element,
            )
        }

        /**
         * Makes sure a new getter/field is never annotated with a serialized name that is already
         * given to some other getter/field.
         *
         * Assumes the getter/field is annotated with a [DataPropertyAnnotation].
         */
        @Throws(XProcessingException::class)
        fun requireSerializedNameNeverSeenBefore(getterOrField: AnnotatedGetterOrField) {
            val serializedName = getSerializedName(getterOrField)
            val existingGetterOrField = serializedNameToGetterOrField[serializedName]
            if (existingGetterOrField != null) {
                throw XProcessingException(
                    "Cannot give property the name '$serializedName' because it is already used " +
                        "for ${existingGetterOrField.jvmName}",
                    getterOrField.element,
                )
            }
        }

        companion object {
            /**
             * Returns the serialized name that should be used for the property in the database.
             *
             * Assumes the getter/field is annotated with a [DataPropertyAnnotation].
             */
            private fun getSerializedName(getterOrField: AnnotatedGetterOrField): String {
                val annotation = getterOrField.annotation as DataPropertyAnnotation
                return annotation.name
            }

            private fun hasDataPropertyAnnotation(getterOrField: AnnotatedGetterOrField): Boolean {
                val annotation = getterOrField.annotation
                return annotation.propertyKind == PropertyAnnotation.Kind.DATA_PROPERTY
            }

            /**
             * Makes sure the annotation type didn't change when overriding e.g. `@StringProperty
             * -> @Id`.
             */
            @Throws(XProcessingException::class)
            private fun requireAnnotationTypeIsConsistent(
                existingGetterOrField: AnnotatedGetterOrField,
                overriddenGetterOfField: AnnotatedGetterOrField,
            ) {
                val existingAnnotation = existingGetterOrField.annotation
                val overriddenAnnotation = overriddenGetterOfField.annotation
                if (existingAnnotation.className != overriddenAnnotation.className) {
                    throw XProcessingException(
                        "Property type must stay consistent when overriding annotated members " +
                            "but changed from @${existingAnnotation.className.simpleName} -> " +
                            "@${overriddenAnnotation.className.simpleName}",
                        overriddenGetterOfField.element,
                    )
                }
            }

            /**
             * Makes sure the serialized name didn't change when overriding.
             *
             * Assumes the getter/field is annotated with a [DataPropertyAnnotation].
             */
            @Throws(XProcessingException::class)
            private fun requireSerializedNameIsConsistent(
                existingGetterOrField: AnnotatedGetterOrField,
                overriddenGetterOrField: AnnotatedGetterOrField,
            ) {
                val existingSerializedName = getSerializedName(existingGetterOrField)
                val overriddenSerializedName = getSerializedName(overriddenGetterOrField)
                if (existingSerializedName != overriddenSerializedName) {
                    throw XProcessingException(
                        "Property name within the annotation must stay consistent when " +
                            "overriding annotated members but changed from " +
                            "'${existingSerializedName}' -> '${overriddenSerializedName}'",
                        overriddenGetterOrField.element,
                    )
                }
            }

            private fun createSignatureString(getterOrField: AnnotatedGetterOrField): String {
                return (getterOrField.jvmType.toString() +
                    " " +
                    getterOrField.element.enclosingElement!!.name +
                    "#" +
                    getterOrField.jvmName +
                    (if (getterOrField.isGetter) "()" else ""))
            }
        }
    }

    companion object {
        /**
         * Tries to create an [DocumentModel] from the given [Element].
         *
         * @throws XProcessingException if the @`Document`-annotated class is invalid.
         */
        @Throws(XProcessingException::class)
        fun createPojoModel(env: XProcessingEnv, clazz: XTypeElement): DocumentModel {
            return DocumentModel(env, clazz, null)
        }

        /**
         * Tries to create an [DocumentModel] from the given AutoValue [Element] and corresponding
         * generated class.
         *
         * @throws XProcessingException if the @`Document`-annotated class is invalid.
         */
        @Throws(XProcessingException::class)
        fun createAutoValueModel(
            env: XProcessingEnv,
            clazz: XTypeElement,
            generatedAutoValueElement: XTypeElement,
        ): DocumentModel {
            return DocumentModel(env, clazz, generatedAutoValueElement)
        }

        /**
         * Infers the [PropertyAccessor] for each of the [AnnotatedGetterOrField].
         *
         * Each accessor may be the [AnnotatedGetterOrField] itself or some other non-private
         * getter.
         */
        @Throws(XProcessingException::class)
        private fun inferPropertyAccessors(
            annotatedGettersAndFields: Collection<AnnotatedGetterOrField>,
            allMethods: Collection<XMethodElement>,
            helper: IntrospectionHelper,
        ): Map<AnnotatedGetterOrField, PropertyAccessor> {
            val accessors = mutableMapOf<AnnotatedGetterOrField, PropertyAccessor>()
            for (getterOrField in annotatedGettersAndFields) {
                accessors.put(
                    getterOrField,
                    PropertyAccessor.infer(getterOrField, allMethods, helper),
                )
            }
            return accessors
        }
    }
}
