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

import androidx.appsearch.compiler.XProcessingException
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.processing.XConstructorElement
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement

/**
 * Represents a class that can convert between some custom type and a property's actual type.
 *
 * @see androidx.appsearch.app.StringSerializer
 * @see androidx.appsearch.app.LongSerializer
 */
data class SerializerClass(
    /** The kind of serializer. */
    val kind: Kind,

    /** The serializer class element. */
    val element: XTypeElement,

    /** The zero-param constructor. Present on every serializer class. */
    val defaultConstructor: XConstructorElement,

    /** The custom type that can be serialized using the serializer class. */
    val customType: XType,
) {
    enum class Kind(
        /**
         * The actual type of the corresponding property within a `GenericDocument`.
         *
         * For example, a [.STRING_SERIALIZER] may only be used with a `@Document.StringProperty`
         * which, in turn, boils down to a [String] within a `GenericDocument`.
         */
        // TypeName is an immutable 3P type
        val actualTypeInGenericDoc: XTypeName
    ) {
        STRING_SERIALIZER(actualTypeInGenericDoc = XTypeName.STRING),
        LONG_SERIALIZER(actualTypeInGenericDoc = XTypeName.PRIMITIVE_LONG),
    }

    companion object {
        /**
         * Creates a serializer class given its [XTypeElement].
         *
         * @throws XProcessingException If the `clazz` does not have a zero-param constructor.
         */
        @Throws(XProcessingException::class)
        fun create(clazz: XTypeElement, kind: Kind): SerializerClass {
            val deserializeMethod: XMethodElement = findDeserializeMethod(clazz, kind)
            return SerializerClass(
                kind = kind,
                element = clazz,
                defaultConstructor = findDefaultConstructor(clazz),
                customType = deserializeMethod.returnType,
            )
        }

        /**
         * Returns the zero-param constructor in the `clazz`.
         *
         * @throws XProcessingException If no such constructor exists or it's private.
         */
        @Throws(XProcessingException::class)
        private fun findDefaultConstructor(clazz: XTypeElement): XConstructorElement {
            val constructor: XConstructorElement =
                clazz
                    .getConstructors()
                    .stream()
                    .filter { it.parameters.isEmpty() }
                    .findFirst()
                    .orElseThrow {
                        XProcessingException(
                            "Serializer ${clazz.qualifiedName} must have a zero-param " +
                                "constructor",
                            clazz,
                        )
                    }
            if (constructor.isPrivate()) {
                throw XProcessingException(
                    "The zero-param constructor of serializer ${clazz.qualifiedName} must not " +
                        "be private",
                    constructor,
                )
            }
            return constructor
        }

        /** Returns the `T deserialize(PropertyType)` method. */
        private fun findDeserializeMethod(clazz: XTypeElement, kind: Kind): XMethodElement {
            for (method in clazz.getDeclaredMethods()) {
                // The type-system enforces there is one method satisfying these constraints
                if (
                    method.name == "deserialize" &&
                        !method.isStatic()
                        // Direct equality check with the param's type should be sufficient.
                        // Don't need to allow for subtypes because mActualTypeInGenericDoc can
                        // only be a primitive type or String which is a final class.
                        &&
                        hasSingleParamOfExactType(method, kind.actualTypeInGenericDoc)
                ) {
                    return method
                }
            }
            throw IllegalStateException(
                "Couldn't find deserialize method in ${clazz.qualifiedName}"
            )
        }

        private fun hasSingleParamOfExactType(
            method: XMethodElement,
            expectedType: XTypeName,
        ): Boolean {
            if (method.parameters.size != 1) {
                return false
            }
            val firstParamType = method.parameters.first().type.asTypeName()
            return firstParamType == expectedType
        }
    }
}
