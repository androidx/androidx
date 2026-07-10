/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.processing.XAnnotationValue
import androidx.room.compiler.processing.XType

/** An instance of the `@Document.EmbeddingProperty` annotation. */
data class EmbeddingPropertyAnnotation(
    override val name: String,
    override val isRequired: Boolean,

    /** Specifies how a property should be indexed. */
    val indexingType: Int,

    /** Specifies whether the embedding vectors in this property should be quantized. */
    val quantizationType: Int,
) :
    DataPropertyAnnotation(
        className = CLASS_NAME,
        configClassName = CONFIG_CLASS,
        genericDocGetterName = "getPropertyEmbedding",
        genericDocArrayGetterName = "getPropertyEmbeddingArray",
        genericDocSetterName = "setPropertyEmbedding",
    ) {
    companion object {
        val CLASS_NAME: XClassName =
            IntrospectionHelper.DOCUMENT_ANNOTATION_CLASS.nestedClass("EmbeddingProperty")

        @JvmField
        val CONFIG_CLASS: XClassName =
            IntrospectionHelper.APPSEARCH_SCHEMA_CLASS.nestedClass("EmbeddingPropertyConfig")

        /**
         * @param defaultName The name to use for the annotated property in case the annotation
         *   params do not mention an explicit name.
         */
        fun parse(
            annotationParams: Map<String, XAnnotationValue>,
            defaultName: String,
        ): EmbeddingPropertyAnnotation {
            val name = annotationParams["name"]?.value as? String
            return EmbeddingPropertyAnnotation(
                name = if (name.isNullOrEmpty()) defaultName else name,
                isRequired = annotationParams.getValue("required").asBoolean(),
                indexingType = annotationParams.getValue("indexingType").asInt(),
                quantizationType = annotationParams.getValue("quantizationType").asInt(),
            )
        }
    }

    override val dataPropertyKind
        get() = Kind.EMBEDDING_PROPERTY

    override fun getUnderlyingTypeWithinGenericDoc(helper: IntrospectionHelper): XType =
        helper.embeddingType
}
