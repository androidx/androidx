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
package androidx.appsearch.compiler.annotationwrapper

import androidx.appsearch.compiler.IntrospectionHelper
import androidx.appsearch.compiler.XProcessingException
import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.processing.XAnnotation

/**
 * An instance of an annotation for a data property e.g. `@Document.StringProperty`.
 *
 * Is one of:
 * * [StringPropertyAnnotation]
 * * [DocumentPropertyAnnotation]
 * * [LongPropertyAnnotation]
 * * [DoublePropertyAnnotation]
 * * [BooleanPropertyAnnotation]
 * * [BytesPropertyAnnotation]
 * * [EmbeddingPropertyAnnotation]
 * * [BlobHandlePropertyAnnotation]
 */
abstract class DataPropertyAnnotation
protected constructor(
    override val className: XClassName,

    /**
     * The class used to configure data properties of this kind.
     *
     * For example, [androidx.appsearch.app.AppSearchSchema.StringPropertyConfig] for
     * [StringPropertyAnnotation].
     */
    val configClassName: XClassName,
    override val genericDocGetterName: String,

    /**
     * The corresponding getter within [androidx.appsearch.app.GenericDocument] that returns an
     * array.
     *
     * For example, `getPropertyStringArray` for a [StringPropertyAnnotation].
     */
    val genericDocArrayGetterName: String,
    override val genericDocSetterName: String,
) : PropertyAnnotation {
    enum class Kind {
        STRING_PROPERTY,
        DOCUMENT_PROPERTY,
        LONG_PROPERTY,
        DOUBLE_PROPERTY,
        BOOLEAN_PROPERTY,
        BYTES_PROPERTY,
        EMBEDDING_PROPERTY,
        BLOB_HANDLE_PROPERTY,
    }

    companion object {
        /**
         * Attempts to parse an [XAnnotation] into a [DataPropertyAnnotation], or null.
         *
         * @param defaultName The name to use for the annotated property in case the annotation
         *   params do not mention an explicit name.
         * @throws XProcessingException If the [XAnnotation] is a valid [DataPropertyAnnotation] but
         *   its params are malformed e.g. point to an illegal serializer class etc.
         */
        @Throws(XProcessingException::class)
        @JvmStatic
        fun tryParse(
            annotation: XAnnotation,
            defaultName: String,
            helper: IntrospectionHelper,
        ): DataPropertyAnnotation? {
            val annotationParams = helper.getAnnotationParams(annotation)
            val qualifiedClassName = annotation.qualifiedName
            return when (qualifiedClassName) {
                BooleanPropertyAnnotation.CLASS_NAME.canonicalName ->
                    BooleanPropertyAnnotation.parse(annotationParams, defaultName)
                BytesPropertyAnnotation.CLASS_NAME.canonicalName ->
                    BytesPropertyAnnotation.parse(annotationParams, defaultName)
                DocumentPropertyAnnotation.CLASS_NAME.canonicalName ->
                    DocumentPropertyAnnotation.parse(annotationParams, defaultName)
                DoublePropertyAnnotation.CLASS_NAME.canonicalName ->
                    DoublePropertyAnnotation.parse(annotationParams, defaultName)
                LongPropertyAnnotation.CLASS_NAME.canonicalName ->
                    LongPropertyAnnotation.parse(annotationParams, defaultName)
                StringPropertyAnnotation.CLASS_NAME.canonicalName ->
                    StringPropertyAnnotation.parse(annotationParams, defaultName)
                EmbeddingPropertyAnnotation.CLASS_NAME.canonicalName ->
                    EmbeddingPropertyAnnotation.parse(annotationParams, defaultName)
                BlobHandlePropertyAnnotation.CLASS_NAME.canonicalName ->
                    BlobHandlePropertyAnnotation.parse(annotationParams, defaultName)
                else -> return null
            }
        }
    }

    /** The serialized name for the property in the database. */
    abstract val name: String

    /** Denotes whether this property must be specified for the document to be valid. */
    abstract val isRequired: Boolean

    /** The [Kind] of [DataPropertyAnnotation]. */
    abstract val dataPropertyKind: Kind

    override val propertyKind
        get() = PropertyAnnotation.Kind.DATA_PROPERTY
}
