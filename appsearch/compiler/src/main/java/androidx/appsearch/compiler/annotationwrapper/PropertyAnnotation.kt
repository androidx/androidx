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
import androidx.room.compiler.processing.XType

/**
 * An instance of an AppSearch property annotation.
 *
 * Is one of:
 * * [MetadataPropertyAnnotation] e.g. `@Document.Id`
 * * [DataPropertyAnnotation] e.g. `@Document.StringProperty`
 */
interface PropertyAnnotation {
    enum class Kind {
        METADATA_PROPERTY,
        DATA_PROPERTY,
    }

    /**
     * The annotation class' name.
     *
     * For example, `androidx.appsearch.annotation.Document.StringProperty` for a
     * [StringPropertyAnnotation].
     */
    val className: XClassName

    /** The [Kind] of [PropertyAnnotation]. */
    val propertyKind: Kind

    /**
     * The corresponding getter within [androidx.appsearch.app.GenericDocument].
     *
     * For example, `getPropertyString` for a [StringPropertyAnnotation].
     */
    val genericDocGetterName: String

    /**
     * The corresponding setter within [androidx.appsearch.app.GenericDocument.Builder].
     *
     * For example, `setPropertyString` for a [StringPropertyAnnotation].
     */
    val genericDocSetterName: String

    /**
     * The underlying type that the property is stored as within a
     * [androidx.appsearch.app.GenericDocument].
     *
     * For example, [String] for [StringPropertyAnnotation].
     */
    fun getUnderlyingTypeWithinGenericDoc(helper: IntrospectionHelper): XType
}
