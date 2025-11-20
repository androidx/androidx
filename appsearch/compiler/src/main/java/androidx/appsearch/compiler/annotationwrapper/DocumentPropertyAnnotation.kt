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
import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.processing.XAnnotationValue
import androidx.room.compiler.processing.XType

/** An instance of the `@Document.DocumentProperty` annotation. */
data class DocumentPropertyAnnotation(
    override val name: String,
    override val isRequired: Boolean,

    /** Specifies whether fields in the nested document should be indexed. */
    val shouldIndexNestedProperties: Boolean,

    /**
     * The list of nested properties to index for the nested document other than the properties
     * inherited from the type's parent.
     */
    val indexableNestedPropertiesList: List<String>,

    /**
     * Specifies whether to inherit the parent class's definition for the indexable nested
     * properties list.
     */
    val shouldInheritIndexableNestedPropertiesFromSuperClass: Boolean,
) :
    DataPropertyAnnotation(
        className = CLASS_NAME,
        configClassName = CONFIG_CLASS,
        genericDocGetterName = "getPropertyDocument",
        genericDocArrayGetterName = "getPropertyDocumentArray",
        genericDocSetterName = "setPropertyDocument",
    ) {
    companion object {
        val CLASS_NAME: XClassName =
            IntrospectionHelper.DOCUMENT_ANNOTATION_CLASS.nestedClass("DocumentProperty")

        @JvmField
        val CONFIG_CLASS: XClassName =
            IntrospectionHelper.APPSEARCH_SCHEMA_CLASS.nestedClass("DocumentPropertyConfig")

        /**
         * @param defaultName The name to use for the annotated property in case the annotation
         *   params do not mention an explicit name.
         */
        fun parse(
            annotationParams: Map<String, XAnnotationValue>,
            defaultName: String,
        ): DocumentPropertyAnnotation {
            val name = annotationParams["name"]?.value as? String
            val indexableNestedPropertiesList =
                annotationParams.getValue("indexableNestedPropertiesList").asStringList()
            return DocumentPropertyAnnotation(
                name = if (name.isNullOrEmpty()) defaultName else name,
                isRequired = annotationParams.getValue("required").asBoolean(),
                shouldIndexNestedProperties =
                    annotationParams.getValue("indexNestedProperties").asBoolean(),
                indexableNestedPropertiesList = indexableNestedPropertiesList,
                shouldInheritIndexableNestedPropertiesFromSuperClass =
                    annotationParams
                        .getValue("inheritIndexableNestedPropertiesFromSuperclass")
                        .asBoolean(),
            )
        }
    }

    override val dataPropertyKind
        get() = Kind.DOCUMENT_PROPERTY

    override fun getUnderlyingTypeWithinGenericDoc(helper: IntrospectionHelper): XType =
        helper.genericDocumentType
}
