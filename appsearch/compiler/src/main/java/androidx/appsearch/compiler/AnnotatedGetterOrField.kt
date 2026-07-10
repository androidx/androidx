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

import androidx.appsearch.compiler.annotationwrapper.DataPropertyAnnotation
import androidx.appsearch.compiler.annotationwrapper.DataPropertyAnnotation.Companion.tryParse
import androidx.appsearch.compiler.annotationwrapper.LongPropertyAnnotation
import androidx.appsearch.compiler.annotationwrapper.MetadataPropertyAnnotation
import androidx.appsearch.compiler.annotationwrapper.PropertyAnnotation
import androidx.appsearch.compiler.annotationwrapper.SerializerClass
import androidx.appsearch.compiler.annotationwrapper.StringPropertyAnnotation
import androidx.room.compiler.processing.ExperimentalProcessingApi
import androidx.room.compiler.processing.XAnnotation
import androidx.room.compiler.processing.XArrayType
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XFieldElement
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.isArray
import androidx.room.compiler.processing.isField
import androidx.room.compiler.processing.isMethod
import java.util.Locale
import java.util.stream.Collectors

/**
 * A getter or field annotated with a [PropertyAnnotation] annotation. For example,
 * <pre>
 * @Document("MyEntity")
 * public final class Entity {
 *     @Document.Id
 *     public String mId;
 * //                ^^^
 *
 * // OR
 *
 *     @Document.StringProperty
 *     public String getName() {...}
 * //                ^^^^^^^
 * }
 * </pre>
 */
@OptIn(ExperimentalProcessingApi::class)
data class AnnotatedGetterOrField(
    /** The annotation that the getter or field is annotated with. */
    val annotation: PropertyAnnotation,

    /** The annotated getter or field. */
    val element: XElement,

    /**
     * The type-category of the getter or field.
     *
     * Note: `byte[]` as treated specially as documented in [ElementTypeCategory].
     */
    val elementTypeCategory: ElementTypeCategory,

    /**
     * The field/getter's return type if non-repeated, else the underlying element type.
     *
     * For example, `String` for a field `String mName` and `int` for a field `int[] mNums`.
     *
     * The one exception to this is `byte[]` where:
     * <pre>
     * @BytesProperty bytes[] mField; // componentType: byte[]
     * @BytesProperty bytes[][] mField; // componentType: byte[]
     * @BytesProperty List<bytes[]> mField; // componentType: byte[]
     * </pre>
     */
    val componentType: XType,

    /**
     * The normalized/stemmed [.getJvmName].
     *
     * For example,
     * <pre>
     * getName -> name
     * mName -> name
     * _name -> name
     * name_ -> name
     * isAwesome -> awesome
     * </pre>
     */
    val normalizedName: String,
) {
    /**
     * Specifies whether the getter/field is assigned a collection or array or a single type.
     *
     * Note: `byte[]` are treated specially such that
     * * `byte[]` is a primitive in icing and is treated as [.SINGLE].
     * * `Collection<byte[]>` is treated as a [.COLLECTION].
     * * `byte[][]` is treated as an [.ARRAY].
     *
     * The boxed [Byte] type is not supported by the AppSearch compiler at all.
     */
    enum class ElementTypeCategory {
        SINGLE,
        COLLECTION,
        ARRAY,
    }

    companion object {
        /**
         * Creates a [AnnotatedGetterOrField] if the element is annotated with some
         * [PropertyAnnotation]. Otherwise returns null.
         */
        @Throws(XProcessingException::class)
        @JvmStatic
        fun tryCreateFor(element: XElement, env: XProcessingEnv): AnnotatedGetterOrField? {
            val annotation: XAnnotation = getSingleAppSearchAnnotation(element) ?: return null

            val metadataPropertyAnnotation = MetadataPropertyAnnotation.tryParse(annotation)
            if (metadataPropertyAnnotation != null) {
                return create(metadataPropertyAnnotation, element, env)
            }

            val normalizedName: String = inferNormalizedName(element, env) // e.g. mField -> field
            val dataPropertyAnnotation =
                tryParse(annotation, /* defaultName= */ normalizedName, IntrospectionHelper(env))
            if (dataPropertyAnnotation != null) {
                return create(dataPropertyAnnotation, element, env)
            }

            return null
        }

        /**
         * Creates a [AnnotatedGetterOrField] for a `getterOrField` annotated with the specified
         * `annotation`.
         */
        @Throws(XProcessingException::class)
        private fun create(
            annotation: PropertyAnnotation,
            getterOrField: XElement,
            env: XProcessingEnv,
        ): AnnotatedGetterOrField {
            requireIsGetterOrField(getterOrField)

            val typeCategory: ElementTypeCategory = inferTypeCategory(getterOrField, env)
            val annotatedGetterOrField =
                AnnotatedGetterOrField(
                    annotation = annotation,
                    element = getterOrField,
                    elementTypeCategory = typeCategory,
                    componentType = inferComponentType(getterOrField, typeCategory),
                    normalizedName = inferNormalizedName(getterOrField, env),
                )

            requireTypeMatchesAnnotation(annotatedGetterOrField, env)

            return annotatedGetterOrField
        }

        @Throws(XProcessingException::class)
        private fun requireIsGetterOrField(element: XElement) {
            when {
                element.isField() -> return
                element.isMethod() -> {
                    val errors = IntrospectionHelper.validateIsGetter(element)
                    if (errors.isNotEmpty()) {
                        val err =
                            XProcessingException(
                                "Failed to find a suitable getter for element " +
                                    "\"${element.name}\"",
                                element,
                            )
                        err.addWarnings(errors)
                        throw err
                    }
                }
                else -> {}
            }
        }

        /**
         * Infers whether the getter/field returns a collection or array or neither.
         *
         * Note: `byte[]` are treated specially as documented in [ElementTypeCategory].
         */
        private fun inferTypeCategory(
            getterOrField: XElement,
            env: XProcessingEnv,
        ): ElementTypeCategory {
            val jvmType: XType = IntrospectionHelper.getPropertyType(getterOrField)
            val helper = IntrospectionHelper(env)
            return if (helper.collectionType.rawType.isAssignableFrom(jvmType)) {
                ElementTypeCategory.COLLECTION
            } else if (
                jvmType.isArray() &&
                    !jvmType.isSameType(helper.bytePrimitiveArrayType) &&
                    !jvmType.isSameType(helper.byteBoxArrayType)
            ) {
                // byte[] has a native representation in Icing and should be considered a
                // primitive by itself.
                //
                // byte[][], however, is considered repeated (ARRAY).
                ElementTypeCategory.ARRAY
            } else {
                ElementTypeCategory.SINGLE
            }
        }

        /**
         * Infers the getter/field's return type if non-repeated, else the underlying element type.
         *
         * For example, `String mField -> String` and `List<String> mField -> String`.
         */
        @Throws(XProcessingException::class)
        private fun inferComponentType(
            getterOrField: XElement,
            typeCategory: ElementTypeCategory,
        ): XType {
            val jvmType = IntrospectionHelper.getPropertyType(getterOrField)
            return when (typeCategory) {
                ElementTypeCategory.SINGLE -> jvmType
                ElementTypeCategory.COLLECTION -> {
                    // e.g. List<T>
                    //           ^
                    val typeArguments = jvmType.typeArguments
                    if (typeArguments.isEmpty()) {
                        throw XProcessingException(
                            "Property is repeated but has no generic rawType",
                            getterOrField,
                        )
                    }
                    typeArguments.first()
                }
                ElementTypeCategory.ARRAY -> (jvmType as XArrayType).componentType
            }
        }

        private fun inferNormalizedName(element: XElement, env: XProcessingEnv): String {
            return if (element.isMethod()) {
                inferNormalizedMethodName(element, env)
            } else {
                inferNormalizedFieldName(element)
            }
        }

        /**
         * Makes sure the getter/field's JVM type matches the type expected by the
         * [PropertyAnnotation].
         */
        @Throws(XProcessingException::class)
        private fun requireTypeMatchesAnnotation(
            getterOrField: AnnotatedGetterOrField,
            env: XProcessingEnv,
        ) {
            val annotation = getterOrField.annotation
            when (annotation.propertyKind) {
                PropertyAnnotation.Kind.METADATA_PROPERTY ->
                    requireTypeMatchesMetadataPropertyAnnotation(
                        getterOrField,
                        annotation as MetadataPropertyAnnotation,
                        env,
                    )
                PropertyAnnotation.Kind.DATA_PROPERTY ->
                    requireTypeMatchesDataPropertyAnnotation(
                        getterOrField,
                        annotation as DataPropertyAnnotation,
                        env,
                    )
            }
        }

        /**
         * Returns the only `@Document.*` annotation on the element e.g. `@Document.StringProperty`.
         *
         * Returns null if no such annotation exists on the element.
         *
         * @throws XProcessingException If the element is annotated with more than one of such
         *   annotations.
         */
        @Throws(XProcessingException::class)
        private fun getSingleAppSearchAnnotation(element: XElement): XAnnotation? {
            // @Document.* annotation
            val annotations =
                element
                    .getAllAnnotations()
                    .stream()
                    .filter {
                        it.qualifiedName.startsWith(
                            IntrospectionHelper.DOCUMENT_ANNOTATION_CLASS.canonicalName
                        )
                    }
                    .toList()
            if (annotations.isEmpty()) {
                return null
            }
            if (annotations.size > 1) {
                throw XProcessingException("Cannot use multiple @Document.* annotations", element)
            }
            return annotations.first()
        }

        private fun inferNormalizedMethodName(method: XElement, env: XProcessingEnv): String {
            val methodName = method.name
            val helper = IntrospectionHelper(env)
            // String getName() -> name
            if (
                methodName.startsWith("get") &&
                    methodName.length > 3 &&
                    Character.isUpperCase(methodName[3])
            ) {
                return methodName.substring(3, 4).lowercase(Locale.getDefault()) +
                    methodName.substring(4)
            }

            // String isAwesome() -> awesome
            if (
                helper.isFieldOfBooleanType(method) &&
                    methodName.startsWith("is") &&
                    methodName.length > 2
            ) {
                return methodName.substring(2, 3).lowercase(Locale.getDefault()) +
                    methodName.substring(3)
            }

            // Assume the method's name is the normalized name as well: String name() -> name
            return methodName
        }

        private fun inferNormalizedFieldName(field: XElement): String {
            val fieldName = field.name
            if (fieldName.length < 2) {
                return fieldName
            }

            // String mName -> name
            if (fieldName[0] == 'm' && Character.isUpperCase(fieldName[1])) {
                return fieldName.substring(1, 2).lowercase(Locale.getDefault()) +
                    fieldName.substring(2)
            }

            // String _name -> name
            if (fieldName[0] == '_' && fieldName[1] != '_' && Character.isLowerCase(fieldName[1])) {
                return fieldName.substring(1)
            }

            // String name_ -> name
            if (fieldName[fieldName.length - 1] == '_' && fieldName[fieldName.length - 2] != '_') {
                return fieldName.substring(0, fieldName.length - 1)
            }

            // Assume the field's name is the normalize name as well: String name -> name
            return fieldName
        }

        /**
         * Makes sure the getter/field's JVM type matches the type expected by the
         * [MetadataPropertyAnnotation].
         *
         * For example, fields annotated with `@Document.Score` must be of type `int` or [Integer].
         */
        @Throws(XProcessingException::class)
        private fun requireTypeMatchesMetadataPropertyAnnotation(
            getterOrField: AnnotatedGetterOrField,
            annotation: MetadataPropertyAnnotation,
            env: XProcessingEnv,
        ) {
            val helper = IntrospectionHelper(env)
            when (annotation) {
                MetadataPropertyAnnotation.ID,
                MetadataPropertyAnnotation.NAMESPACE ->
                    requireTypeIsOneOf(
                        getterOrField,
                        listOf(helper.stringType),
                        allowRepeated = false,
                    )
                MetadataPropertyAnnotation.TTL_MILLIS,
                MetadataPropertyAnnotation.CREATION_TIMESTAMP_MILLIS ->
                    requireTypeIsOneOf(
                        getterOrField,
                        listOf(
                            helper.longPrimitiveType,
                            helper.intPrimitiveType,
                            helper.longBoxType,
                            helper.integerBoxType,
                        ),
                        allowRepeated = false,
                    )
                MetadataPropertyAnnotation.SCORE ->
                    requireTypeIsOneOf(
                        getterOrField,
                        listOf(helper.intPrimitiveType, helper.integerBoxType),
                        allowRepeated = false,
                    )
            }
        }

        /**
         * Makes sure the getter/field's JVM type matches the type expected by the
         * [DataPropertyAnnotation].
         *
         * For example, fields annotated with [StringPropertyAnnotation] must be of type [String] or
         * a collection or array of [String]s.
         */
        @Throws(XProcessingException::class)
        private fun requireTypeMatchesDataPropertyAnnotation(
            getterOrField: AnnotatedGetterOrField,
            annotation: DataPropertyAnnotation,
            env: XProcessingEnv,
        ) {
            val helper = IntrospectionHelper(env)
            when (annotation.dataPropertyKind) {
                DataPropertyAnnotation.Kind.STRING_PROPERTY -> {
                    val stringSerializer = (annotation as StringPropertyAnnotation).customSerializer
                    if (stringSerializer != null) {
                        requireComponentTypeMatchesWithSerializer(getterOrField, stringSerializer)
                    } else {
                        requireTypeIsOneOf(
                            getterOrField,
                            listOf(helper.stringType),
                            allowRepeated = true,
                        )
                    }
                }
                DataPropertyAnnotation.Kind.DOCUMENT_PROPERTY ->
                    requireTypeIsSomeDocumentClass(getterOrField)
                DataPropertyAnnotation.Kind.LONG_PROPERTY -> {
                    val longSerializer = (annotation as LongPropertyAnnotation).customSerializer
                    if (longSerializer != null) {
                        requireComponentTypeMatchesWithSerializer(getterOrField, longSerializer)
                    } else {
                        requireTypeIsOneOf(
                            getterOrField,
                            listOf(
                                helper.longPrimitiveType,
                                helper.intPrimitiveType,
                                helper.longBoxType,
                                helper.integerBoxType,
                            ),
                            allowRepeated = true,
                        )
                    }
                }
                DataPropertyAnnotation.Kind.DOUBLE_PROPERTY ->
                    requireTypeIsOneOf(
                        getterOrField,
                        listOf(
                            helper.doublePrimitiveType,
                            helper.floatPrimitiveType,
                            helper.doubleBoxType,
                            helper.floatBoxType,
                        ),
                        allowRepeated = true,
                    )
                DataPropertyAnnotation.Kind.BOOLEAN_PROPERTY ->
                    requireTypeIsOneOf(
                        getterOrField,
                        listOf(helper.booleanPrimitiveType, helper.booleanBoxType),
                        allowRepeated = true,
                    )
                DataPropertyAnnotation.Kind.BYTES_PROPERTY ->
                    requireTypeIsOneOf(
                        getterOrField,
                        listOf(helper.bytePrimitiveArrayType),
                        allowRepeated = true,
                    )
                DataPropertyAnnotation.Kind.EMBEDDING_PROPERTY ->
                    requireTypeIsOneOf(
                        getterOrField,
                        listOf(helper.embeddingType),
                        allowRepeated = true,
                    )
                DataPropertyAnnotation.Kind.BLOB_HANDLE_PROPERTY ->
                    requireTypeIsOneOf(
                        getterOrField,
                        listOf(helper.blobHandleType),
                        allowRepeated = true,
                    )
            }
        }

        /**
         * Makes sure the getter/field's type is one of the expected types.
         *
         * If `allowRepeated` is true, also allows the getter/field's type to be an array or
         * collection of any of the expected types.
         */
        @Throws(XProcessingException::class)
        private fun requireTypeIsOneOf(
            getterOrField: AnnotatedGetterOrField,
            expectedTypes: Collection<XType>,
            allowRepeated: Boolean,
        ) {
            val target = if (allowRepeated) getterOrField.componentType else getterOrField.jvmType
            val isValid =
                expectedTypes.stream().anyMatch { expectedType: XType ->
                    expectedType.isSameType(target)
                }
            if (!isValid) {
                val error =
                    ("@${getterOrField.annotation.className.simpleName}" +
                        " must only be placed on a getter/field of type " +
                        (if (allowRepeated) "or array or collection of " else "") +
                        expectedTypes
                            .stream()
                            .map(XType::toString)
                            .collect(Collectors.joining("|")))
                throw XProcessingException(error, getterOrField.element)
            }
        }

        /**
         * Makes sure the getter/field's component type is consistent with the serializer class.
         *
         * @throws XProcessingException If the getter/field is of a different type than what the
         *   serializer class serializes to/from.
         */
        @Throws(XProcessingException::class)
        private fun requireComponentTypeMatchesWithSerializer(
            getterOrField: AnnotatedGetterOrField,
            serializerClass: SerializerClass,
        ) {
            // The component type must exactly match the type for which we have a serializer.
            // Subtypes do not work e.g.
            // @StringProperty(serializer = ParentSerializer.class) Child mField;
            // because ParentSerializer.deserialize(String) would return a Parent, which we won't be
            // able to assign to mField.
            if (!getterOrField.componentType.isSameType(serializerClass.customType)) {
                throw XProcessingException(
                    ("@%s with serializer = %s must only be placed on a getter/field of type or " +
                            "array or collection of %s")
                        .format(
                            getterOrField.annotation.className.simpleName,
                            serializerClass.element.name,
                            serializerClass.customType,
                        ),
                    getterOrField.element,
                )
            }
        }

        /**
         * Makes sure the getter/field is assigned a type annotated with `@Document`.
         *
         * Allows for arrays and collections of such a type as well.
         */
        @Throws(XProcessingException::class)
        private fun requireTypeIsSomeDocumentClass(annotatedGetterOrField: AnnotatedGetterOrField) {
            val componentTypeElement = annotatedGetterOrField.componentType.typeElement
            if (
                componentTypeElement == null ||
                    IntrospectionHelper.getDocumentAnnotation(componentTypeElement) == null
            ) {
                throw XProcessingException(
                    "Invalid type for @DocumentProperty. Must be another class " +
                        "annotated with @Document (or collection or array of)",
                    annotatedGetterOrField.element,
                )
            }
        }
    }

    /** The field/getter's return type. */
    val jvmType: XType
        get() =
            if (isGetter) {
                (element as XMethodElement).returnType
            } else {
                (element as XFieldElement).type
            }

    /** The getter/field's jvm name e.g. `mId` or `getName`. */
    val jvmName: String
        get() = element.name

    /** Whether the [.getElement] is a getter. */
    val isGetter: Boolean
        get() = element.isMethod()

    /** Whether the [.getElement] is a field. */
    val isField: Boolean
        get() = element.isField()
}
