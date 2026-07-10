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
import androidx.room.compiler.processing.XAnnotationValue
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement

/** An instance of the `@Document.LongProperty` annotation. */
data class LongPropertyAnnotation(
    override val name: String,
    override val isRequired: Boolean,

    /** Specifies how a property should be indexed. */
    val indexingType: Int,

    /**
     * An optional [androidx.appsearch.app.LongSerializer].
     *
     * This is specified in the annotation when the annotated getter/field is of some custom type
     * that should boil down to a long in the database.
     *
     * @see androidx.appsearch.annotation.Document.LongProperty.serializer
     */
    val customSerializer: SerializerClass?,
) :
    DataPropertyAnnotation(
        className = CLASS_NAME,
        configClassName = CONFIG_CLASS,
        genericDocGetterName = "getPropertyLong",
        genericDocArrayGetterName = "getPropertyLongArray",
        genericDocSetterName = "setPropertyLong",
    ) {
    companion object {
        val CLASS_NAME: XClassName =
            IntrospectionHelper.DOCUMENT_ANNOTATION_CLASS.nestedClass("LongProperty")

        @JvmField
        val CONFIG_CLASS: XClassName =
            IntrospectionHelper.APPSEARCH_SCHEMA_CLASS.nestedClass("LongPropertyConfig")

        private val DEFAULT_SERIALIZER_CLASS: XClassName =
            CLASS_NAME.nestedClass("DefaultSerializer")

        /**
         * @param defaultName The name to use for the annotated property in case the annotation
         *   params do not mention an explicit name.
         * @throws XProcessingException If the annotation points to an Illegal serializer class.
         */
        @Throws(XProcessingException::class)
        fun parse(
            annotationParams: Map<String, XAnnotationValue>,
            defaultName: String,
        ): LongPropertyAnnotation {
            val name = annotationParams["name"]?.value as? String
            val serializerInAnnotation = annotationParams.getValue("serializer").asType()
            val typeName = serializerInAnnotation.toString()
            val customSerializer: SerializerClass? =
                if (typeName == DEFAULT_SERIALIZER_CLASS.canonicalName) {
                    null
                } else {
                    SerializerClass.create(
                        serializerInAnnotation.typeElement as XTypeElement,
                        SerializerClass.Kind.LONG_SERIALIZER,
                    )
                }
            return LongPropertyAnnotation(
                name = if (name.isNullOrEmpty()) defaultName else name,
                isRequired = annotationParams.getValue("required").asBoolean(),
                indexingType = annotationParams.getValue("indexingType").asInt(),
                customSerializer = customSerializer,
            )
        }
    }

    override val dataPropertyKind
        get() = Kind.LONG_PROPERTY

    override fun getUnderlyingTypeWithinGenericDoc(helper: IntrospectionHelper): XType =
        helper.longPrimitiveType
}
