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
import java.util.Locale
import java.util.stream.Collectors
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier

/**
 * The public/package-private accessor for an [AnnotatedGetterOrField].
 *
 * The accessor itself may be a getter or a field.
 *
 * May be the [AnnotatedGetterOrField] itself or some completely different method in case the
 * [AnnotatedGetterOrField] is private. For example:
 * <pre>
 * @Document("MyEntity")
 * class Entity {
 *     @Document.StringProperty
 *     private String mName;
 *
 *     public String getName();
 *     //            ^^^^^^^
 * }
 * </pre>
 */
data class PropertyAccessor(
    /** The getter/field element. */
    val element: Element
) {
    companion object {
        /**
         * Infers the [PropertyAccessor] for a given [AnnotatedGetterOrField].
         *
         * @param neighboringMethods The surrounding methods in the same class as the field. In case
         *   the field is private, an appropriate non-private getter can be picked from this list.
         */
        @Throws(ProcessingException::class)
        @JvmStatic
        fun infer(
            getterOrField: AnnotatedGetterOrField,
            neighboringMethods: Collection<ExecutableElement>,
            helper: IntrospectionHelper,
        ): PropertyAccessor {
            if (!getterOrField.element.modifiers.contains(Modifier.PRIVATE)) {
                // Accessible as-is
                return PropertyAccessor(getterOrField.element)
            }

            if (getterOrField.isGetter) {
                throw ProcessingException(
                    "Annotated getter must not be private",
                    getterOrField.element,
                )
            }

            return PropertyAccessor(
                findCorrespondingGetter(getterOrField, neighboringMethods, helper)
            )
        }

        @Throws(ProcessingException::class)
        private fun findCorrespondingGetter(
            privateField: AnnotatedGetterOrField,
            neighboringMethods: Collection<ExecutableElement>,
            helper: IntrospectionHelper,
        ): ExecutableElement {
            val getterNames = getAcceptableGetterNames(privateField, helper)
            val potentialGetters: List<ExecutableElement> =
                neighboringMethods
                    .stream()
                    .filter { getterNames.contains(it.simpleName.toString()) }
                    .toList()

            // Start building the exception for the case where we don't find a suitable getter
            val potentialSignatures =
                getterNames
                    .stream()
                    .map { "[public] ${privateField.jvmType} $it()" }
                    .collect(Collectors.joining(" OR "))
            val processingException =
                ProcessingException(
                    ("Field '${privateField.jvmName}' cannot be read: it is private and has no " +
                        "suitable getters $potentialSignatures"),
                    privateField.element,
                )

            for (method in potentialGetters) {
                val errors = helper.validateIsGetterThatReturns(method, privateField.jvmType)
                if (errors.isNotEmpty()) {
                    processingException.addWarnings(errors)
                    continue
                }
                // found one!
                return method
            }

            throw processingException
        }

        private fun getAcceptableGetterNames(
            privateField: AnnotatedGetterOrField,
            helper: IntrospectionHelper,
        ): Set<String> {
            // String mMyField -> {myField, getMyField}
            // boolean mMyField -> {myField, getMyField, isMyField}
            val normalizedName = privateField.normalizedName
            val getterNames = mutableSetOf<String>(normalizedName)
            val upperCamelCase =
                normalizedName.substring(0, 1).uppercase(Locale.getDefault()) +
                    normalizedName.substring(1)
            getterNames.add("get$upperCamelCase")
            val isBooleanField =
                helper.isFieldOfExactType(
                    privateField.element,
                    helper.booleanPrimitiveType,
                    helper.booleanBoxType,
                )
            if (isBooleanField && privateField.elementTypeCategory == ElementTypeCategory.SINGLE) {
                getterNames.add("is$upperCamelCase")
            }
            return getterNames
        }
    }

    /** Whether the accessor is a getter. */
    val isGetter: Boolean
        get() = element.kind == ElementKind.METHOD

    /** Whether the accessor is a field. */
    val isField: Boolean
        get() = element.kind == ElementKind.FIELD
}
